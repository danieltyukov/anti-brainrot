'use strict';
const test = require('node:test');
const assert = require('node:assert/strict');

require('../extension/lib/features.js');
const { features } = globalThis.AntiBrainrot;

test('every feature id is unique', () => {
  const ids = features.FEATURES.map((f) => f.id);
  assert.equal(new Set(ids).size, ids.length);
});

test('every attribute is unique and kebab-case', () => {
  const attrs = features.FEATURES.map((f) => f.attr).filter(Boolean);
  assert.equal(new Set(attrs).size, attrs.length);
  for (const a of attrs) assert.match(a, /^[a-z]+(-[a-z]+)*$/);
});

test('every parent exists, precedes its children, and children declare a mode', () => {
  const seen = new Set();
  for (const f of features.FEATURES) {
    if (f.parent) {
      assert.ok(seen.has(f.parent), `${f.id} parent ${f.parent} must come first`);
      assert.ok(['when-parent-off', 'when-parent-on'].includes(f.mode), `${f.id} needs a mode`);
    }
    seen.add(f.id);
  }
});

test('roots keep registry order and exclude children', () => {
  assert.deepEqual(
    features.roots().map((f) => f.id),
    [
      'homeFeed', 'sidebar', 'endScreenFeed', 'endScreenCards', 'shorts', 'comments',
      'mixes', 'merch', 'videoInfo', 'topHeader', 'inaptSearch', 'explore',
      'moreFromYouTube', 'subscriptions', 'autoplay', 'annotations', 'educational', 'adultSites',
    ],
  );
});

test('children of sidebar are the four sidebar parts in order', () => {
  assert.deepEqual(
    features.children('sidebar').map((f) => f.id),
    ['sidebarRecommended', 'liveChat', 'playlist', 'fundraiser'],
  );
  assert.deepEqual(features.children('homeFeed').map((f) => f.id), ['redirectHome']);
  assert.deepEqual(features.children('topHeader').map((f) => f.id), ['notifications']);
  assert.deepEqual(features.children('comments').map((f) => f.id), ['commentAvatars']);
  assert.deepEqual(features.children('videoInfo').map((f) => f.id), ['videoButtons', 'videoChannel', 'videoDescription']);
  assert.deepEqual(features.children('mixes'), []);
});

test('shorts is locked on and has no attribute', () => {
  const shorts = features.byId('shorts');
  assert.equal(shorts.locked, true);
  assert.equal(shorts.default, true);
  assert.equal(shorts.attr, null);
});

test('adultSites is the only web-section feature', () => {
  assert.deepEqual(features.FEATURES.filter((f) => f.section === 'web').map((f) => f.id), ['adultSites']);
});

test('byId returns undefined for unknown ids', () => {
  assert.equal(features.byId('nope'), undefined);
});
