'use strict';
const test = require('node:test');
const assert = require('node:assert/strict');
const { installChromeMock, removeChromeMock } = require('./helpers/chrome-mock.js');

require('../extension/lib/features.js');
require('../extension/lib/settings.js');
const { settings: S, features } = globalThis.AntiBrainrot;

test('defaults match the feature registry', () => {
  const d = S.defaults();
  assert.equal(d.version, S.VERSION);
  assert.equal(d.focus.enabled, true);
  assert.equal(d.focus.unlockDelaySec, 300);
  assert.equal(d.theme, 'system');
  for (const f of features.FEATURES) assert.equal(d.features[f.id], f.default, f.id);
  assert.deepEqual(d.educational.allowedCategories, ['Education', 'Science & Technology', 'Howto & Style']);
  assert.deepEqual(d.educational.allowedChannels, []);
  assert.deepEqual(d.blocker, { blockedDomains: [], allowedDomains: [] });
  assert.equal(d.distractions.mode, 'pause');
  assert.deepEqual(d.distractions.presets, [...S.DEFAULT_PRESETS]);
  assert.equal(d.distractions.passMinutes, 5);
  assert.equal(d.distractions.pauseSeconds, 10);
  assert.equal(d.distractions.dailyBudgetMinutes, 30);
  assert.equal(d.distractions.cooldownMinutes, 15);
  assert.deepEqual(d.distractions.exceptions, []);
  assert.equal(d.distractions.grayscaleAlways, false);
  assert.equal(d.focus.lockUntil, 0);
  assert.equal(d.focus.reason, '');
  assert.deepEqual(d.schedule, { days: [1, 2, 3, 4, 5], start: '09:00', end: '17:00' });
});

test('ad hoc lock: isLockedNow, lockedUntilText and the update guard', async () => {
  const s = S.defaults();
  const now = new Date(2026, 8, 20, 10, 0);
  s.focus.lockUntil = now.getTime() + 2 * 3600 * 1000;
  assert.equal(S.isLockedNow(s, now), true);
  assert.equal(S.lockedUntilText(s, now), '12:00');
  s.focus.lockUntil = now.getTime() + 20 * 3600 * 1000;
  assert.equal(S.lockedUntilText(s, now), 'tomorrow 06:00');
  s.focus.lockUntil = now.getTime() - 1;
  assert.equal(S.isLockedNow(s, now), false);
  assert.equal(S.lockedUntilText(s, now), '');
  const sched = S.defaults();
  sched.features.schedule = true;
  sched.schedule = { days: [0, 1, 2, 3, 4, 5, 6], start: '09:00', end: '17:00' };
  assert.equal(S.lockedUntilText(sched, now), '17:00');
  sched.focus.lockUntil = now.getTime() + 9 * 3600 * 1000;
  assert.equal(S.lockedUntilText(sched, now), '19:00', 'the later of the two ends wins');

  installChromeMock();
  const locked = S.defaults();
  locked.focus.lockUntil = Date.now() + 3600 * 1000;
  await S.save(locked);
  await assert.rejects(S.update({ focus: { enabled: false } }), (err) => err instanceof S.LockedError && /Locked until/.test(err.message));
  await assert.rejects(S.update({ focus: { lockUntil: 0 } }), S.LockedError, 'shortening the lock is loosening');
  const longer = await S.update({ focus: { lockUntil: Date.now() + 7200 * 1000 } });
  assert.ok(longer.focus.lockUntil > locked.focus.lockUntil);
  removeChromeMock();
});

test('patch writes through the queue without guards', async () => {
  installChromeMock();
  const on = S.defaults();
  on.focus.enabled = true;
  await S.save(on);
  const r = await S.patch({ features: { comments: false } });
  assert.equal(r.features.comments, false);
  removeChromeMock();
});

test('normalize validates distractions and schedule', () => {
  const n = S.normalize({
    distractions: { mode: 'nuke', presets: ['tiktok', 7], custom: 'x', passMinutes: 7, pauseSeconds: 20, dailyBudgetMinutes: 60, grayscalePass: 'no', intention: false },
    schedule: { days: [5, 5, 9, '1'], start: '25:00', end: '18:30' },
  });
  assert.equal(n.distractions.mode, 'pause');
  assert.deepEqual(n.distractions.presets, ['tiktok']);
  assert.deepEqual(n.distractions.custom, []);
  assert.equal(n.distractions.passMinutes, 5, 'invalid choice keeps default');
  assert.equal(n.distractions.pauseSeconds, 20);
  assert.equal(n.distractions.dailyBudgetMinutes, 60);
  assert.equal(n.distractions.grayscalePass, true);
  assert.equal(n.distractions.intention, false);
  assert.deepEqual(n.schedule.days, [1, 5]);
  assert.equal(n.schedule.start, '09:00', 'invalid start keeps default');
  assert.equal(n.schedule.end, '18:30');
  const bad = S.normalize({ schedule: { start: '18:00', end: '09:00' } });
  assert.deepEqual([bad.schedule.start, bad.schedule.end], ['09:00', '17:00'], 'start must precede end');
});

test('isLoosening covers distractions and the schedule', () => {
  const base = S.defaults();
  base.features.distractions = true;
  base.features.schedule = true;
  const tweak = (fn) => { const n = S.normalize(base); fn(n); return S.isLoosening(base, n); };
  assert.equal(tweak(() => {}), false);
  assert.equal(tweak((n) => { n.distractions.mode = 'block'; }), false, 'pause to block tightens');
  const blockBase = S.normalize(base); blockBase.distractions.mode = 'block';
  const toPause = S.normalize(blockBase); toPause.distractions.mode = 'pause';
  assert.equal(S.isLoosening(blockBase, toPause), true, 'block to pause loosens');
  assert.equal(tweak((n) => { n.distractions.presets = n.distractions.presets.slice(1); }), true, 'preset removed');
  assert.equal(tweak((n) => { n.distractions.presets.push('twitch'); }), false, 'preset added');
  assert.equal(tweak((n) => { n.distractions.custom = ['example.com']; }), false, 'custom added');
  assert.equal(tweak((n) => { n.distractions.passMinutes = 10; }), true, 'longer pass');
  assert.equal(tweak((n) => { n.distractions.passMinutes = 2; }), false, 'shorter pass');
  assert.equal(tweak((n) => { n.distractions.pauseSeconds = 5; }), true, 'shorter pause');
  assert.equal(tweak((n) => { n.distractions.dailyBudgetMinutes = 60; }), true, 'bigger budget');
  assert.equal(tweak((n) => { n.distractions.grayscalePass = false; }), true);
  assert.equal(tweak((n) => { n.distractions.intention = false; }), true);
  assert.equal(tweak((n) => { n.distractions.exceptions = ['reddit.com/r/programming']; }), true, 'exception added');
  assert.equal(tweak((n) => { n.distractions.cooldownMinutes = 5; }), true, 'shorter cooldown');
  assert.equal(tweak((n) => { n.distractions.cooldownMinutes = 60; }), false, 'longer cooldown');
  assert.equal(tweak((n) => { n.distractions.grayscaleAlways = true; }), false, 'always grayscale tightens');
  assert.equal(tweak((n) => { n.schedule.days = [1, 2, 3, 4]; }), true, 'day removed');
  assert.equal(tweak((n) => { n.schedule.days = [0, 1, 2, 3, 4, 5, 6]; }), false, 'days added');
  assert.equal(tweak((n) => { n.schedule.start = '10:00'; }), true, 'later start');
  assert.equal(tweak((n) => { n.schedule.end = '16:00'; }), true, 'earlier end');
  assert.equal(tweak((n) => { n.schedule.start = '08:00'; n.schedule.end = '18:00'; }), false, 'wider window');
  const off = S.normalize(base); off.features.schedule = false; off.features.distractions = false;
  const offChanged = S.normalize(off); offChanged.schedule.days = [1]; offChanged.distractions.presets = [];
  assert.equal(S.isLoosening(off, offChanged), false, 'lists do not matter while those features are off');
});

test('isLockedNow and the update guard during locked hours', async () => {
  const s = S.defaults();
  s.features.schedule = true;
  s.schedule = { days: [1, 2, 3, 4, 5], start: '09:00', end: '17:00' };
  const monday10 = new Date(2026, 8, 21, 10, 0);
  const monday18 = new Date(2026, 8, 21, 18, 0);
  const saturday10 = new Date(2026, 8, 19, 10, 0);
  assert.equal(S.isLockedNow(s, monday10), true);
  assert.equal(S.isLockedNow(s, monday18), false);
  assert.equal(S.isLockedNow(s, saturday10), false);
  s.features.schedule = false;
  assert.equal(S.isLockedNow(s, monday10), false);

  installChromeMock();
  const locked = S.defaults();
  locked.features.schedule = true;
  locked.schedule = { days: [0, 1, 2, 3, 4, 5, 6], start: '00:00', end: '23:59' };
  await S.save(locked);
  await assert.rejects(S.update({ focus: { enabled: false } }), (err) => err instanceof S.LockedError && /Locked until 23:59/.test(err.message));
  const stillOn = await S.load();
  assert.equal(stillOn.focus.enabled, true);
  removeChromeMock();
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
  assert.equal(n.features.shorts, false, 'shorts is a setting like any other');
  assert.equal(S.normalize({}).features.shorts, true, 'and it defaults to on');
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
    'end-screen-cards', 'shorts', 'comments', 'mixes', 'merch', 'notifications', 'inapt-search',
    'explore', 'more-from-youtube', 'autoplay', 'annotations', 'chips', 'rich-sections',
  ]);
  const noShorts = S.defaults();
  noShorts.features.shorts = false;
  assert.equal(S.activeAttributes(noShorts).includes('shorts'), false);
  assert.equal(S.isActive(noShorts, 'shorts'), false);
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

function on() {
  const s = S.defaults();
  s.features.educational = true;
  s.features.adultSites = true;
  s.features.keywords = true;
  s.blocker.blockedDomains = ['x.com'];
  s.keywords.blocked = ['feet', 'nudes'];
  return s;
}

test('keywords: defaults empty, normalised and capped, removing one is loosening while on', () => {
  assert.deepEqual(S.defaults().keywords, { blocked: [] });
  const n = S.normalize({ keywords: { blocked: [' feet ', '', 7, 'nudes'] } });
  assert.deepEqual(n.keywords.blocked, ['feet', 'nudes']);
  assert.equal(S.normalize({ keywords: { blocked: Array.from({ length: 300 }, (_, i) => `w${i}`) } }).keywords.blocked.length, 200);
  assert.deepEqual(S.normalize({ keywords: 'nope' }).keywords, { blocked: [] });
  const base = on();
  let next = on(); next.keywords.blocked = ['feet'];
  assert.equal(S.isLoosening(base, next), true, 'removing a keyword');
  next = on(); next.keywords.blocked = ['feet', 'nudes', 'more'];
  assert.equal(S.isLoosening(base, next), false, 'adding a keyword');
  next = on(); next.keywords.blocked = ['Feet', 'NUDES'];
  assert.equal(S.isLoosening(base, next), false, 'case does not count');
  const off = on(); off.features.keywords = false;
  next = on(); next.features.keywords = false; next.keywords.blocked = [];
  assert.equal(S.isLoosening(off, next), false, 'the list does not matter while the feature is off');
});

test('isLoosening detects every way of allowing more', () => {
  const base = on();
  assert.equal(S.isLoosening(base, base), false);

  let next = on(); next.features.comments = false;
  assert.equal(S.isLoosening(base, next), true, 'feature off');
  next = on(); next.features.playlist = true;
  assert.equal(S.isLoosening(base, next), false, 'feature on is tightening');
  next = on(); next.features.shorts = false;
  assert.equal(S.isLoosening(base, next), true, 'shorts off is loosening like any other feature');
  next = on(); next.features.preventRemoval = true;
  assert.equal(S.isLoosening(base, next), false, 'prevent removal on is tightening');
  const guarded = on(); guarded.features.preventRemoval = true;
  next = on(); next.features.preventRemoval = false;
  assert.equal(S.isLoosening(guarded, next), true, 'prevent removal off is loosening');

  next = on(); next.focus.unlockDelaySec = 60;
  assert.equal(S.isLoosening(base, next), true, 'shorter delay');
  next = on(); next.focus.unlockDelaySec = 3600;
  assert.equal(S.isLoosening(base, next), false, 'longer delay');

  next = on(); next.educational.allowedCategories.push('Gaming');
  assert.equal(S.isLoosening(base, next), true, 'extra category');
  next = on(); next.educational.allowedCategories = ['Education'];
  assert.equal(S.isLoosening(base, next), false, 'fewer categories');
  next = on(); next.educational.allowedChannels = ['@someone'];
  assert.equal(S.isLoosening(base, next), true, 'extra channel');

  next = on(); next.blocker.blockedDomains = [];
  assert.equal(S.isLoosening(base, next), true, 'blocked domain removed');
  next = on(); next.blocker.blockedDomains = ['x.com', 'y.com'];
  assert.equal(S.isLoosening(base, next), false, 'blocked domain added');
  next = on(); next.blocker.allowedDomains = ['z.com'];
  assert.equal(S.isLoosening(base, next), true, 'allowed domain added');

  const eduOff = on(); eduOff.features.educational = false;
  next = on(); next.features.educational = false; next.educational.allowedCategories.push('Gaming');
  assert.equal(S.isLoosening(eduOff, next), false, 'category lists do not matter while educational mode is off');

  next = on(); next.theme = 'dark';
  assert.equal(S.isLoosening(base, next), false, 'theme is free');
});

test('update refuses loosening changes while the filter is on', async () => {
  installChromeMock();
  await S.save(on());
  await assert.rejects(S.update({ features: { comments: false } }), S.LockedError);
  const tightened = await S.update({ features: { playlist: true } });
  assert.equal(tightened.features.playlist, true);
  const off = await S.update({ focus: { enabled: false } });
  assert.equal(off.focus.enabled, false);
  const loosened = await S.update({ features: { comments: false } });
  assert.equal(loosened.features.comments, false, 'anything goes while off');
  const backOn = await S.update({ focus: { enabled: true } });
  assert.equal(backOn.features.comments, false);
  removeChromeMock();
});

test('overlapping updates are serialised and both land', async () => {
  const { store } = installChromeMock();
  const off = S.defaults();
  off.focus.enabled = false;
  await S.save(off);
  await Promise.all([
    S.update({ features: { comments: false } }),
    S.update({ features: { mixes: false } }),
    S.update({ theme: 'dark' }),
  ]);
  assert.equal(store.settings.features.comments, false);
  assert.equal(store.settings.features.mixes, false);
  assert.equal(store.settings.theme, 'dark');

  // A refused update must not break the queue for the next one.
  await S.update({ focus: { enabled: true } });
  const results = await Promise.allSettled([
    S.update({ features: { comments: true } }),
    S.update({ features: { mixes: true } }),
    S.update({ features: { playlist: false } }),
  ]);
  assert.deepEqual(results.map((r) => r.status), ['fulfilled', 'fulfilled', 'fulfilled']);
  const after = await S.load();
  assert.equal(after.features.comments, true);
  assert.equal(after.features.mixes, true);
  await assert.rejects(S.update({ features: { comments: false } }), S.LockedError);
  assert.equal((await S.update({ theme: 'light' })).theme, 'light', 'queue survives a LockedError');
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
