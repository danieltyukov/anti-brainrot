'use strict';
const test = require('node:test');
const assert = require('node:assert/strict');

require('../extension/lib/education.js');
const { education: E } = globalThis.AntiBrainrot;

test('category list is the 15 YouTube categories and defaults are a subset', () => {
  assert.equal(E.CATEGORIES.length, 15);
  for (const c of E.DEFAULT_ALLOWED) assert.ok(E.CATEGORIES.includes(c), c);
});

test('normalizeChannel strips handles, URLs and case', () => {
  assert.equal(E.normalizeChannel('@Veritasium '), 'veritasium');
  assert.equal(E.normalizeChannel('https://www.youtube.com/@3blue1brown'), '3blue1brown');
  assert.equal(E.normalizeChannel('https://www.youtube.com/@3blue1brown/videos'), '3blue1brown');
  assert.equal(E.normalizeChannel('youtube.com/channel/UCabc123'), 'ucabc123');
  assert.equal(E.normalizeChannel('https://youtube.com/c/SomeName?sub=1'), 'somename');
  assert.equal(E.normalizeChannel('UCabc'), 'ucabc');
  assert.equal(E.normalizeChannel('  Kurzgesagt In a Nutshell '), 'kurzgesagt in a nutshell');
  assert.equal(E.normalizeChannel(''), '');
  assert.equal(E.normalizeChannel(null), '');
});

test('parseChannelList splits on newlines and commas and dedupes', () => {
  assert.deepEqual(E.parseChannelList('a\nb, c\n\n@A '), ['a', 'b', 'c']);
  assert.deepEqual(E.parseChannelList(''), []);
  assert.deepEqual(E.parseChannelList(undefined), []);
});

const policy = { allowedCategories: ['Education', 'Science & Technology'], allowedChannels: ['@veritasium', 'UCallowed'] };

test('decide allows listed categories, case-insensitively', () => {
  assert.deepEqual(E.decide({ category: 'Education' }, policy), { allow: true, reason: 'category' });
  assert.deepEqual(E.decide({ category: 'science & technology' }, policy), { allow: true, reason: 'category' });
});

test('decide blocks other categories', () => {
  assert.deepEqual(E.decide({ category: 'Entertainment', channelName: 'MrBeast' }, policy), { allow: false, reason: 'blocked' });
});

test('decide allows allow-listed channels regardless of category', () => {
  assert.deepEqual(E.decide({ category: 'Entertainment', channelHandle: 'Veritasium' }, policy), { allow: true, reason: 'channel' });
  assert.deepEqual(E.decide({ category: 'Comedy', channelId: 'UCallowed' }, policy), { allow: true, reason: 'channel' });
  assert.deepEqual(E.decide({ category: 'Comedy', channelName: 'veritasium' }, policy), { allow: true, reason: 'channel' });
});

test('decide fails closed when the category is missing', () => {
  assert.deepEqual(E.decide({ category: '' }, policy), { allow: false, reason: 'unknown' });
  assert.deepEqual(E.decide({}, policy), { allow: false, reason: 'unknown' });
  assert.deepEqual(E.decide(null, policy), { allow: false, reason: 'unknown' });
});

test('decide copes with an empty policy', () => {
  assert.deepEqual(E.decide({ category: 'Education' }, { allowedCategories: [], allowedChannels: [] }), { allow: false, reason: 'blocked' });
  assert.deepEqual(E.decide({ category: 'Education' }, undefined), { allow: false, reason: 'blocked' });
});
