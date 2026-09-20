'use strict';
const test = require('node:test');
const assert = require('node:assert/strict');

require('../extension/lib/distractions.js');
const { distractions: D } = globalThis.AntiBrainrot;

test('presets have unique ids and valid patterns', () => {
  const ids = D.PRESETS.map((p) => p.id);
  assert.equal(new Set(ids).size, ids.length);
  for (const p of D.PRESETS) for (const s of p.patterns) assert.ok(D.parsePattern(s), `${p.id}: ${s}`);
});

test('parsePattern understands host, root-only and prefix forms', () => {
  assert.deepEqual(D.parsePattern('tiktok.com'), { host: 'tiktok.com', path: null });
  assert.deepEqual(D.parsePattern('https://www.Reddit.com/'), { host: 'reddit.com', path: '' });
  assert.deepEqual(D.parsePattern('instagram.com/reels/'), { host: 'instagram.com', path: '/reels' });
  assert.deepEqual(D.parsePattern('x.com/home?x=1'), { host: 'x.com', path: '/home' });
  assert.deepEqual(D.parsePattern('example.com/a/b*'), { host: 'example.com', path: '/a/b' });
  assert.equal(D.parsePattern('localhost'), null);
  assert.equal(D.parsePattern(''), null);
  assert.equal(D.parsePattern('not a host/x'), null);
  assert.equal(D.formatPattern(D.parsePattern('https://www.Reddit.com/')), 'reddit.com/');
  assert.equal(D.formatPattern(D.parsePattern('instagram.com/reels/')), 'instagram.com/reels');
  assert.deepEqual(D.parseList('tiktok.com\nTikTok.com, reddit.com/ , bad'), ['tiktok.com', 'reddit.com/']);
});

test('matchesUrl respects the three forms', () => {
  assert.equal(D.matchesUrl('tiktok.com', 'https://www.tiktok.com/@user/video/1'), true);
  assert.equal(D.matchesUrl('tiktok.com', 'https://m.tiktok.com/'), true);
  assert.equal(D.matchesUrl('tiktok.com', 'https://nottiktok.com/'), false);
  assert.equal(D.matchesUrl('reddit.com/', 'https://www.reddit.com/'), true);
  assert.equal(D.matchesUrl('reddit.com/', 'https://old.reddit.com/?feed=home'), true);
  assert.equal(D.matchesUrl('reddit.com/', 'https://www.reddit.com/r/programming/'), false);
  assert.equal(D.matchesUrl('reddit.com/r/popular', 'https://www.reddit.com/r/popular/'), true);
  assert.equal(D.matchesUrl('reddit.com/r/popular', 'https://www.reddit.com/r/popularity'), false);
  assert.equal(D.matchesUrl('instagram.com/reels', 'https://www.instagram.com/reels/abc/'), true);
  assert.equal(D.matchesUrl('instagram.com/reels', 'https://www.instagram.com/direct/inbox/'), false);
  assert.equal(D.matchesUrl('x.com/home', 'chrome-extension://abc/blocked/blocked.html'), false);
});

test('activePatterns merges presets and custom entries', () => {
  const patterns = D.activePatterns({ presets: ['tiktok', 'nope'], custom: ['Example.com/feed', 'tiktok.com'] });
  assert.deepEqual(patterns, ['tiktok.com', 'example.com/feed']);
  assert.equal(D.firstMatch(patterns, 'https://example.com/feed/x'), 'example.com/feed');
  assert.equal(D.firstMatch(patterns, 'https://example.com/jobs'), null);
  assert.deepEqual(D.hosts(patterns), ['tiktok.com', 'example.com']);
  assert.deepEqual(D.matchPatterns(['reddit.com/', 'reddit.com/r/all']), ['*://reddit.com/*', '*://*.reddit.com/*']);
});

test('regexFor produces RE2-compatible filters that match the same URLs', () => {
  const cases = [
    ['tiktok.com', 'https://www.tiktok.com/foryou', true],
    ['tiktok.com', 'https://tiktok.com', true],
    ['tiktok.com', 'https://tiktok.com.evil.io/', false],
    ['reddit.com/', 'https://www.reddit.com/', true],
    ['reddit.com/', 'https://www.reddit.com', true],
    ['reddit.com/', 'https://www.reddit.com/?feed=home', true],
    ['reddit.com/', 'https://www.reddit.com/r/all', false],
    ['reddit.com/', 'https://www.reddit.com//', true],
    ['instagram.com/reels', 'https://www.instagram.com/reels//abc', true],
    ['instagram.com/reels', 'https://www.instagram.com/reels/', true],
    ['instagram.com/reels', 'https://www.instagram.com/reels/abc', true],
    ['instagram.com/reels', 'https://www.instagram.com/reelsy', false],
  ];
  for (const [pattern, url, expected] of cases) {
    const re = new RegExp(D.regexFor(pattern));
    assert.equal(re.test(url), expected, `${pattern} vs ${url}`);
    assert.equal(D.matchesUrl(pattern, url), expected, `matchesUrl ${pattern} vs ${url}`);
  }
});

test('redirectRules and passRule build rules with the block page target', () => {
  const rules = D.redirectRules(['tiktok.com', 'reddit.com/'], 'abc', 'pause', 3000);
  assert.equal(rules.length, 2);
  assert.deepEqual(rules.map((r) => r.id), [3000, 3001]);
  assert.equal(rules[0].action.redirect.regexSubstitution, 'chrome-extension://abc/blocked/blocked.html?kind=pause&u=\\0');
  assert.equal(rules[0].priority, 2);
  assert.deepEqual(rules[0].condition.resourceTypes, ['main_frame']);
  const pass = D.passRule('tiktok.com', 4000);
  assert.equal(pass.action.type, 'allow');
  assert.ok(pass.priority > rules[0].priority);
  assert.equal(pass.condition.regexFilter, rules[0].condition.regexFilter);
});

test('exceptions and blockingPattern', () => {
  const d = { presets: ['reddit'], custom: [], exceptions: ['reddit.com/r/programming', 'Reddit.com/message'] };
  assert.deepEqual(D.activeExceptions(d), ['reddit.com/r/programming', 'reddit.com/message']);
  assert.equal(D.blockingPattern(d, 'https://www.reddit.com/'), 'reddit.com');
  assert.equal(D.blockingPattern(d, 'https://www.reddit.com/r/programming/comments/1'), null);
  assert.equal(D.blockingPattern(d, 'https://www.reddit.com/r/pics'), 'reddit.com');
  assert.equal(D.blockingPattern(d, 'https://example.org/'), null);
  const rules = D.exceptionRules(D.activeExceptions(d), 3500);
  assert.deepEqual(rules.map((r) => [r.id, r.priority, r.action.type]), [[3500, 3, 'allow'], [3501, 3, 'allow']]);
});

test('cooldowns', () => {
  assert.equal(D.cooldownUntil({ 'x.com': 2000 }, 'x.com', 1000), 2000);
  assert.equal(D.cooldownUntil({ 'x.com': 2000 }, 'x.com', 3000), null);
  assert.equal(D.cooldownUntil({}, 'x.com', 0), null);
});

test('budget and passes', () => {
  const now = new Date(2026, 8, 20, 10, 0, 0);
  assert.equal(D.dayKey(now), '2026-09-20');
  const d = { mode: 'pause', passMinutes: 5, dailyBudgetMinutes: 30 };
  assert.equal(D.budgetLeft(null, d, now), 30);
  assert.equal(D.budgetLeft({ day: '2026-09-20', usedMinutes: 27 }, d, now), 3);
  assert.equal(D.budgetLeft({ day: '2026-09-19', usedMinutes: 27 }, d, now), 30, 'yesterday resets');
  assert.equal(D.canPass({ day: '2026-09-20', usedMinutes: 27 }, d, now), false);
  assert.equal(D.canPass({ day: '2026-09-20', usedMinutes: 25 }, d, now), true);
  assert.equal(D.canPass(null, { ...d, mode: 'block' }, now), false);
  assert.equal(D.canPass(null, { ...d, dailyBudgetMinutes: 0 }, now), false);
  assert.equal(D.activePass({ 'tiktok.com': 1000 }, 'tiktok.com', 500), 1000);
  assert.equal(D.activePass({ 'tiktok.com': 1000 }, 'tiktok.com', 1500), null);
  assert.equal(D.activePass({}, 'tiktok.com', 0), null);
});
