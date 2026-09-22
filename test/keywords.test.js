'use strict';
const test = require('node:test');
const assert = require('node:assert/strict');

require('../extension/lib/keywords.js');
const { keywords: K } = globalThis.AntiBrainrot;

test('normalize lowercases, trims, collapses spaces, drops punctuation and short or empty entries', () => {
  assert.equal(K.normalize('  Feet '), 'feet');
  assert.equal(K.normalize('Foot   Fetish'), 'foot fetish');
  assert.equal(K.normalize('nu"des!'), 'nudes');
  assert.equal(K.normalize('a'), null);
  assert.equal(K.normalize(''), null);
  assert.equal(K.normalize('   '), null);
  assert.equal(K.normalize(null), null);
  assert.equal(K.normalize('x'.repeat(50)), 'x'.repeat(40));
  assert.equal(K.normalize('Café'), 'café');
});

test('parseList splits on newlines and commas, dedupes and caps the list', () => {
  assert.deepEqual(K.parseList('feet\nFeet, nudes\n\nfoot fetish'), ['feet', 'nudes', 'foot fetish']);
  assert.deepEqual(K.parseList(''), []);
  assert.deepEqual(K.parseList(null), []);
  const many = K.parseList(Array.from({ length: 250 }, (_, i) => `word${i}`).join('\n'));
  assert.equal(many.length, K.MAX);
});

test('match finds a listed word in the path or query, whole words only, case-insensitively', () => {
  const list = ['feet', 'foot fetish', 'nudes'];
  assert.equal(K.match(list, 'https://www.google.com/search?q=feet+pics'), 'feet');
  assert.equal(K.match(list, 'https://www.google.com/search?q=FEET'), 'feet');
  assert.equal(K.match(list, 'https://www.reddit.com/r/feet/'), 'feet');
  assert.equal(K.match(list, 'https://www.youtube.com/results?search_query=foot+fetish'), 'foot fetish');
  assert.equal(K.match(list, 'https://x.com/search?q=foot%20fetish'), 'foot fetish');
  assert.equal(K.match(list, 'https://x.com/search?q=foot-fetish'), 'foot fetish');
  assert.equal(K.match(list, 'https://www.reddit.com/r/foot_fetish/'), 'foot fetish');
  assert.equal(K.match(list, 'https://x.com/search?q=barefeet'), null);
  assert.equal(K.match(list, 'https://x.com/search?q=feetwear'), null);
  assert.equal(K.match(list, 'https://feet.example/'), null, 'the host does not count');
  assert.equal(K.match(list, 'https://x.com/'), null);
  assert.equal(K.match([], 'https://x.com/?q=feet'), null);
  assert.equal(K.match(list, 'garbage'), null);
  assert.equal(K.match(list, 'https://x.com/?q=%E0%A4%A'), null, 'malformed encoding does not throw');
});

test('rules build a redirect for navigations and a block for the rest, a few keywords per regex', () => {
  const rules = K.rules(['feet', 'nudes', 'foot fetish'], 'abc');
  assert.equal(rules.length, 2);
  const [redirect, block] = rules;
  assert.equal(redirect.id, K.RULE_BASE);
  assert.equal(redirect.priority, 5, 'outranks passes and exceptions');
  assert.equal(redirect.action.type, 'redirect');
  assert.equal(redirect.action.redirect.regexSubstitution, 'chrome-extension://abc/blocked/blocked.html?kind=keyword&u=\\0');
  assert.deepEqual(redirect.condition.resourceTypes, ['main_frame']);
  assert.equal(redirect.condition.isUrlFilterCaseSensitive, false);
  assert.equal(block.id, K.RULE_BASE + 1);
  assert.equal(block.action.type, 'block');
  assert.deepEqual(block.condition.resourceTypes, ['sub_frame', 'xmlhttprequest', 'websocket', 'other']);
  assert.equal(block.condition.regexFilter, redirect.condition.regexFilter);
  assert.deepEqual(K.rules([], 'abc'), []);
  const many = K.rules(Array.from({ length: 12 }, (_, i) => `word${i}`), 'abc');
  assert.equal(many.length, 6, '12 keywords in groups of 5, two rules each');
  assert.equal(new Set(many.map((r) => r.id)).size, 6);
  assert.ok(many.every((r) => r.id >= K.RULE_BASE && r.id < K.RULE_BASE + 1000));
});

test('the rule regex matches the URLs the browser actually sends', () => {
  const [rule] = K.rules(['feet', 'foot fetish', 'café'], 'abc');
  const re = new RegExp(rule.condition.regexFilter, 'i');
  for (const url of [
    'https://www.google.com/search?q=feet&udm=2',
    'https://www.google.com/search?q=Feet+pics',
    'https://www.bing.com/images/search?q=foot+fetish',
    'https://duckduckgo.com/?q=foot%20fetish&ia=images',
    'https://www.reddit.com/r/feet/',
    'https://www.reddit.com/r/foot_fetish/',
    'https://www.youtube.com/results?search_query=feet',
    'https://x.com/search?q=caf%C3%A9',
    'https://example.com/feet',
    'https://example.com/a?b=c%20feet%20d',
  ]) assert.ok(re.test(url), url);
  for (const url of [
    'https://www.google.com/search?q=barefeet',
    'https://www.google.com/search?q=feetwear',
    'https://feet.example.com/',
    'https://www.google.com/',
    'https://www.reddit.com/r/football/',
  ]) assert.ok(!re.test(url), url);
});
