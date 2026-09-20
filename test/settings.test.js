'use strict';
const test = require('node:test');
const assert = require('node:assert/strict');
const { installChromeMock, removeChromeMock } = require('./helpers/chrome-mock.js');

require('../extension/lib/features.js');
require('../extension/lib/settings.js');
const { settings: S, features } = globalThis.Unrot;

test('defaults match the feature registry', () => {
  const d = S.defaults();
  assert.equal(d.version, S.VERSION);
  assert.equal(d.focus.enabled, true);
  assert.equal(d.focus.unlockDelaySec, 300);
  assert.equal(d.theme, 'system');
  for (const f of features.FEATURES) assert.equal(d.features[f.id], f.default, f.id);
  assert.deepEqual(d.educational.allowedCategories, ['Education', 'Science & Technology', 'Howto & Style']);
  assert.deepEqual(d.educational.allowedChannels, []);
});

test('defaults returns a fresh object each time', () => {
  const a = S.defaults();
  a.features.comments = false;
  assert.equal(S.defaults().features.comments, true);
});

test('normalize fills gaps, coerces and drops junk', () => {
  assert.deepEqual(S.normalize({}), S.defaults());
  assert.deepEqual(S.normalize(undefined), S.defaults());
  assert.deepEqual(S.normalize('garbage'), S.defaults());

  const n = S.normalize({
    version: 1,
    focus: { enabled: false, unlockDelaySec: 42 },
    theme: 'purple',
    features: { comments: false, mixes: 'no', bogus: true, shorts: false },
    educational: { allowedCategories: [' Education ', 7, ''], allowedChannels: 'x' },
  });
  assert.equal(n.focus.enabled, false);
  assert.equal(n.focus.unlockDelaySec, 300, 'delay outside choices falls back');
  assert.equal(n.theme, 'system');
  assert.equal(n.features.comments, false);
  assert.equal(n.features.mixes, true, 'non-boolean values are ignored');
  assert.equal('bogus' in n.features, false);
  assert.equal(n.features.shorts, true, 'locked features are always on');
  assert.deepEqual(n.educational.allowedCategories, ['Education']);
  assert.deepEqual(n.educational.allowedChannels, []);
});

test('normalize keeps valid delay choices', () => {
  for (const sec of S.DELAY_CHOICES) {
    assert.equal(S.normalize({ focus: { unlockDelaySec: sec } }).focus.unlockDelaySec, sec);
  }
});

test('isActive is false for everything while the filter is off', () => {
  const s = S.defaults();
  s.focus.enabled = false;
  for (const f of features.FEATURES) assert.equal(S.isActive(s, f.id), false, f.id);
});

test('isActive respects parent modes', () => {
  const s = S.defaults();
  assert.equal(S.isActive(s, 'sidebar'), false);
  assert.equal(S.isActive(s, 'sidebarRecommended'), true);
  s.features.sidebar = true;
  assert.equal(S.isActive(s, 'sidebar'), true);
  assert.equal(S.isActive(s, 'sidebarRecommended'), false, 'parent covers child');

  assert.equal(S.isActive(s, 'redirectHome'), true);
  s.features.homeFeed = false;
  assert.equal(S.isActive(s, 'redirectHome'), false, 'redirect needs the home feed hidden');

  assert.equal(S.isActive(s, 'unknownFeature'), false);
});

test('activeAttributes lists attributes of active features in registry order', () => {
  assert.deepEqual(S.activeAttributes(S.defaults()), [
    'home-feed', 'sidebar-recommended', 'live-chat', 'fundraiser', 'end-screen-feed',
    'end-screen-cards', 'comments', 'mixes', 'merch', 'notifications', 'inapt-search',
    'explore', 'more-from-youtube', 'annotations',
  ]);
  const off = S.defaults();
  off.focus.enabled = false;
  assert.deepEqual(S.activeAttributes(off), []);
});

test('load without chrome returns defaults instead of throwing', async () => {
  removeChromeMock();
  assert.deepEqual(await S.load(), S.defaults());
});

test('load, save and update round trip through chrome.storage.sync', async () => {
  const { store } = installChromeMock();
  assert.deepEqual(await S.load(), S.defaults());

  const custom = S.defaults();
  custom.features.comments = false;
  await S.save(custom);
  assert.equal(store.settings.features.comments, false);
  assert.deepEqual(await S.load(), custom);

  const updated = await S.update({ focus: { enabled: false } });
  assert.equal(updated.focus.enabled, false);
  assert.equal(updated.focus.unlockDelaySec, 300, 'update merges one level deep');
  assert.equal(updated.features.comments, false, 'update keeps other sections');

  const again = await S.update({ features: { mixes: false } });
  assert.equal(again.features.comments, false);
  assert.equal(again.features.mixes, false);
  removeChromeMock();
});

test('onChange fires with normalized settings for the settings key only', async () => {
  const { listeners } = installChromeMock();
  const seen = [];
  S.onChange((s) => seen.push(s));
  assert.equal(listeners.length, 1);
  await chrome.storage.sync.set({ other: 1 });
  assert.equal(seen.length, 0);
  await chrome.storage.sync.set({ settings: { theme: 'dark' } });
  assert.equal(seen.length, 1);
  assert.equal(seen[0].theme, 'dark');
  assert.equal(seen[0].focus.enabled, true);
  removeChromeMock();
});
