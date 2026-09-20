'use strict';
const test = require('node:test');
const assert = require('node:assert/strict');

require('../extension/lib/blocker.js');
const { blocker: B } = globalThis.AntiBrainrot;

test('normalizeDomain reduces URLs and hosts to a bare lowercase domain', () => {
  assert.equal(B.normalizeDomain('https://www.Example.com/x?y=1'), 'example.com');
  assert.equal(B.normalizeDomain('sub.example.co.uk'), 'sub.example.co.uk');
  assert.equal(B.normalizeDomain(' Example.ORG. '), 'example.org');
  assert.equal(B.normalizeDomain('http://münchen.de'), 'xn--mnchen-3ya.de');
  assert.equal(B.normalizeDomain('not a domain'), null);
  assert.equal(B.normalizeDomain('localhost'), null);
  assert.equal(B.normalizeDomain('127.0.0.1'), null);
  assert.equal(B.normalizeDomain(''), null);
  assert.equal(B.normalizeDomain(undefined), null);
});

test('parseDomainList splits on newlines, commas and spaces and dedupes', () => {
  assert.deepEqual(B.parseDomainList('a.com\nA.com, b.org  c.net\n\nhttps://d.io/path'), ['a.com', 'b.org', 'c.net', 'd.io']);
  assert.deepEqual(B.parseDomainList(''), []);
  assert.deepEqual(B.parseDomainList(null), []);
});

test('dynamicRules builds a redirect rule and a higher priority allow rule', () => {
  const rules = B.dynamicRules({ blockedDomains: ['x.com', 'https://Y.com/'], allowedDomains: ['z.com'] }, 'abc');
  assert.equal(rules.length, 2);
  const [block, allow] = rules;
  assert.equal(block.id, 1000);
  assert.equal(block.action.type, 'redirect');
  assert.equal(block.action.redirect.regexSubstitution, 'chrome-extension://abc/blocked/blocked.html?u=\\0');
  assert.deepEqual(block.condition.requestDomains, ['x.com', 'y.com']);
  assert.deepEqual(block.condition.resourceTypes, ['main_frame']);
  assert.equal(allow.id, 2000);
  assert.equal(allow.action.type, 'allow');
  assert.ok(allow.priority > block.priority);
  assert.ok(block.priority >= 2, 'must beat the bundled allow rule in rules/adult.json');
  assert.deepEqual(allow.condition.requestDomains, ['z.com']);
});

test('dynamicRules skips empty lists', () => {
  assert.deepEqual(B.dynamicRules({ blockedDomains: [], allowedDomains: [] }, 'abc'), []);
  assert.deepEqual(B.dynamicRules(undefined, 'abc'), []);
  assert.equal(B.dynamicRules({ blockedDomains: ['a.com'], allowedDomains: [] }, 'abc').length, 1);
});

test('blockedUrlFrom reads the original URL back from the block page URL', () => {
  assert.equal(B.blockedUrlFrom('chrome-extension://id/blocked/blocked.html?u=https://x.com/a?b=1&c=2'), 'https://x.com/a?b=1&c=2');
  assert.equal(B.blockedUrlFrom('chrome-extension://id/blocked/blocked.html'), null);
  assert.equal(B.hostOf('https://www.x.com/a?b=1'), 'www.x.com');
  assert.equal(B.hostOf('garbage'), null);
});
