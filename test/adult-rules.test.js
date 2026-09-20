'use strict';
const test = require('node:test');
const assert = require('node:assert/strict');
const fs = require('node:fs');
const path = require('node:path');

const FILE = path.join(__dirname, '..', 'extension', 'rules', 'adult.json');
const BLOCK_PAGE = '/blocked/blocked.html?u=';

const text = fs.readFileSync(FILE, 'utf8');
const rules = JSON.parse(text);
const redirectRules = rules.filter((r) => r.action.type === 'redirect');
const allowRules = rules.filter((r) => r.action.type === 'allow');
const domainRule = rules[0];
const domains = domainRule.condition.requestDomains;
const keywordRegexes = redirectRules
  .filter((r) => r !== domainRule)
  .map((r) => new RegExp(r.condition.regexFilter, 'i'));

const BENIGN = [
  'google.com', 'youtube.com', 'wikipedia.org', 'github.com', 'reddit.com', 'twitter.com',
  'x.com', 'tumblr.com', 'imgur.com', 'discord.com', 'twitch.tv', 'amazon.com', 'bbc.co.uk',
  'cloudflare.com', 'akamaihd.net', 'facebook.com', 'instagram.com', 'tiktok.com',
  'pinterest.com', 'deviantart.com', 'patreon.com',
];

const ADULT = ['pornhub.com', 'xvideos.com', 'xnxx.com', 'xhamster.com', 'onlyfans.com'];

// True when a navigation to https://<hostname>/ would be redirected by the
// domain rule or by a keyword rule, and not excepted by an allow rule.
function covers(hostname) {
  const inList = (list) => {
    const labels = hostname.split('.');
    for (let i = 0; i < labels.length - 1; i++) {
      if (list.includes(labels.slice(i).join('.'))) return true;
    }
    return false;
  };
  for (const rule of allowRules) {
    if (inList(rule.condition.requestDomains)) return false;
  }
  if (inList(domains)) return true;
  const url = `https://${hostname}/`;
  return keywordRegexes.some((re) => re.test(url));
}

test('adult.json parses to a non-empty array of rules', () => {
  assert.ok(Array.isArray(rules));
  assert.ok(rules.length >= 2);
});

test('rule ids are unique integers starting at 1', () => {
  const ids = rules.map((r) => r.id);
  assert.ok(ids.every((id) => Number.isInteger(id) && id >= 1));
  assert.equal(new Set(ids).size, ids.length);
  assert.deepEqual([...ids].sort((a, b) => a - b), ids.map((_, i) => i + 1));
});

test('every rule targets main frames only and has a positive priority', () => {
  for (const rule of rules) {
    assert.deepEqual(rule.condition.resourceTypes, ['main_frame'], `rule ${rule.id}`);
    assert.ok(Number.isInteger(rule.priority) && rule.priority >= 1, `rule ${rule.id}`);
  }
});

test('redirect rules send the request to the block page with the original URL', () => {
  assert.ok(redirectRules.length >= 2);
  for (const rule of redirectRules) {
    const sub = rule.action.redirect.regexSubstitution;
    assert.ok(sub.includes(BLOCK_PAGE), `rule ${rule.id}`);
    assert.ok(sub.startsWith('chrome-extension://ibcicobbbpfmonjbhpmllnjgdkedneop/'), `rule ${rule.id}`);
    assert.ok(sub.endsWith('\\0'), `rule ${rule.id}`);
  }
});

test('allow rules outrank the redirect rules and only list domains', () => {
  const maxRedirect = Math.max(...redirectRules.map((r) => r.priority));
  for (const rule of allowRules) {
    assert.ok(rule.priority > maxRedirect, `rule ${rule.id}`);
    assert.equal(rule.condition.regexFilter, undefined, `rule ${rule.id}`);
    assert.ok(rule.condition.requestDomains.length >= 1, `rule ${rule.id}`);
  }
});

test('every regexFilter compiles and is short enough for RE2', () => {
  for (const rule of rules) {
    const filter = rule.condition.regexFilter;
    if (filter === undefined) continue;
    assert.doesNotThrow(() => new RegExp(filter), `rule ${rule.id}`);
    assert.ok(filter.length < 2000, `rule ${rule.id} regexFilter is too long`);
  }
});

test('the domain rule has a plausible number of clean domains', () => {
  assert.ok(Array.isArray(domains));
  assert.ok(domains.length >= 1000, `only ${domains.length} domains`);
  assert.ok(domains.length <= 15000, `${domains.length} domains`);
  assert.equal(new Set(domains).size, domains.length, 'duplicates present');
  const shape = /^[a-z0-9](?:[a-z0-9-]*[a-z0-9])?(?:\.[a-z0-9](?:[a-z0-9-]*[a-z0-9])?)+$/;
  for (const d of domains) {
    assert.equal(typeof d, 'string');
    assert.equal(d, d.toLowerCase(), d);
    assert.ok(!/\s/.test(d), `whitespace in ${d}`);
    assert.ok(!d.includes('://'), `protocol in ${d}`);
    assert.ok(!d.includes('/'), `path in ${d}`);
    assert.ok(shape.test(d), `invalid domain ${d}`);
  }
});

test('the domain list is sorted and has no entry covered by another entry', () => {
  const sorted = [...domains].sort();
  assert.deepEqual(domains, sorted);
  const set = new Set(domains);
  for (const d of domains) {
    const labels = d.split('.');
    for (let i = 1; i < labels.length - 1; i++) {
      assert.ok(!set.has(labels.slice(i).join('.')), `${d} is covered by a parent entry`);
    }
  }
});

test('benign domains are neither listed nor matched by keyword rules', () => {
  for (const d of BENIGN) {
    assert.ok(!domains.includes(d), `${d} is in the domain list`);
    assert.ok(!covers(d), `${d} is blocked`);
    assert.ok(!covers(`www.${d}`), `www.${d} is blocked`);
  }
  for (const d of ['aeromexico.com', 'beegees.com', 'xxxlutz.de', 'sussex.ac.uk', 'essex.gov.uk', 'adulteducation.org']) {
    assert.ok(!covers(d), `${d} is blocked`);
  }
});

test('well-known adult domains are covered', () => {
  for (const d of ADULT) {
    assert.ok(covers(d), `${d} is not covered`);
    assert.ok(covers(`www.${d}`), `www.${d} is not covered`);
  }
  assert.ok(covers('free-porn-site.example'));
  assert.ok(covers('erome.com'));
  assert.ok(covers('beeg.com'));
  assert.ok(covers('cam4.com'));
});

test('the file stays under 500 KB', () => {
  assert.ok(Buffer.byteLength(text) < 500 * 1024);
});
