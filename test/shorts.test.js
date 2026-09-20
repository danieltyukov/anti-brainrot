'use strict';
const test = require('node:test');
const assert = require('node:assert/strict');

require('../extension/lib/shorts.js');
const { shorts } = globalThis.AntiBrainrot;

test('videoIdFromPath extracts the id from a shorts path', () => {
  assert.equal(shorts.videoIdFromPath('/shorts/AbC-12_xyz'), 'AbC-12_xyz');
  assert.equal(shorts.videoIdFromPath('/shorts/AbC-12_xyz/'), 'AbC-12_xyz');
  assert.equal(shorts.videoIdFromPath('/shorts/'), null);
  assert.equal(shorts.videoIdFromPath('/shorts'), null);
  assert.equal(shorts.videoIdFromPath('/watch'), null);
  assert.equal(shorts.videoIdFromPath('/feed/shorts/abc'), null);
  assert.equal(shorts.videoIdFromPath(''), null);
  assert.equal(shorts.videoIdFromPath(undefined), null);
});

test('rewriteUrl turns shorts URLs into watch URLs on the same host', () => {
  assert.equal(
    shorts.rewriteUrl('https://www.youtube.com/shorts/x1?feature=share'),
    'https://www.youtube.com/watch?v=x1',
  );
  assert.equal(shorts.rewriteUrl('https://m.youtube.com/shorts/x1'), 'https://m.youtube.com/watch?v=x1');
  assert.equal(shorts.rewriteUrl('https://youtube.com/shorts/x1#frag'), 'https://youtube.com/watch?v=x1');
  assert.equal(shorts.rewriteUrl('https://www.youtube.com/watch?v=x1'), null);
  assert.equal(shorts.rewriteUrl('https://example.com/shorts/x1'), null);
  assert.equal(shorts.rewriteUrl('not a url'), null);
});

test('isShortsPath', () => {
  assert.equal(shorts.isShortsPath('/shorts/abc'), true);
  assert.equal(shorts.isShortsPath('/watch?v=abc'), false);
});
