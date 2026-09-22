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

test('dynamicRules builds a redirect rule, a subresource block rule and a higher priority allow rule', () => {
  const rules = B.dynamicRules({ blockedDomains: ['x.com', 'https://Y.com/'], allowedDomains: ['z.com'] }, 'abc');
  assert.equal(rules.length, 3);
  const [redirect, block, allow] = rules;
  assert.equal(redirect.id, 1000);
  assert.equal(redirect.action.type, 'redirect');
  assert.equal(redirect.action.redirect.regexSubstitution, 'chrome-extension://abc/blocked/blocked.html?u=\\0');
  assert.deepEqual(redirect.condition.requestDomains, ['x.com', 'y.com']);
  assert.deepEqual(redirect.condition.resourceTypes, ['main_frame']);
  // Images, video, frames and scripts from a blocked site are blocked on
  // every page, not only navigations to it.
  assert.equal(block.id, 1001);
  assert.equal(block.action.type, 'block');
  assert.deepEqual(block.condition.requestDomains, ['x.com', 'y.com']);
  assert.deepEqual(block.condition.excludedResourceTypes, ['main_frame']);
  assert.equal(block.condition.resourceTypes, undefined);
  assert.equal(block.priority, redirect.priority);
  assert.equal(allow.id, 2000);
  assert.equal(allow.action.type, 'allow');
  assert.ok(allow.priority > redirect.priority);
  assert.ok(redirect.priority >= 2, 'must beat the bundled allow rule in rules/adult.json');
  assert.deepEqual(allow.condition.requestDomains, ['z.com']);
  assert.deepEqual(allow.condition.resourceTypes, B.ALL_RESOURCE_TYPES);
  assert.ok(B.ALL_RESOURCE_TYPES.includes('main_frame') && B.ALL_RESOURCE_TYPES.includes('image') && B.ALL_RESOURCE_TYPES.includes('media'));
});

test('dynamicRules skips empty lists', () => {
  assert.deepEqual(B.dynamicRules({ blockedDomains: [], allowedDomains: [] }, 'abc'), []);
  assert.deepEqual(B.dynamicRules(undefined, 'abc'), []);
  assert.equal(B.dynamicRules({ blockedDomains: ['a.com'], allowedDomains: [] }, 'abc').length, 2);
  assert.equal(B.dynamicRules({ blockedDomains: [], allowedDomains: ['a.com'] }, 'abc').length, 1);
});

test('isYouTube recognises YouTube hosts only', () => {
  assert.equal(B.isYouTube('https://www.youtube.com/shorts/abc'), true);
  assert.equal(B.isYouTube('https://m.youtube.com/'), true);
  assert.equal(B.isYouTube('https://youtube.com/feed/subscriptions'), true);
  assert.equal(B.isYouTube('https://xvideos.com/'), false);
  assert.equal(B.isYouTube('https://notyoutube.com/'), false);
  assert.equal(B.isYouTube('https://youtube.com.evil.example/'), false);
  assert.equal(B.isYouTube(null), false);
});

test('blockedUrlFrom reads the original URL back from the block page URL', () => {
  assert.equal(B.blockedUrlFrom('chrome-extension://id/blocked/blocked.html?u=https://x.com/a?b=1&c=2'), 'https://x.com/a?b=1&c=2');
  assert.equal(B.blockedUrlFrom('chrome-extension://id/blocked/blocked.html?kind=pause&u=https://x.com/a?b=1&c=2'), 'https://x.com/a?b=1&c=2');
  assert.equal(B.blockedUrlFrom('chrome-extension://id/blocked/blocked.html?kind=block&u=https%3A%2F%2Fx.com%2Fa%3Fb%3D1'), 'https://x.com/a?b=1');
  assert.equal(B.blockedUrlFrom('chrome-extension://id/blocked/blocked.html'), null);
  assert.equal(B.kindFrom('chrome-extension://id/blocked/blocked.html?kind=pause&u=https://x.com/?kind=block'), 'pause');
  assert.equal(B.kindFrom('chrome-extension://id/blocked/blocked.html?u=https://x.com/'), 'adult');
  assert.equal(B.kindFrom('chrome-extension://id/blocked/blocked.html?kind=weird&u=https://x.com/'), 'adult');
  assert.equal(B.kindFrom('chrome-extension://id/blocked/blocked.html?kind=guard'), 'guard');
  assert.equal(B.kindFrom('chrome-extension://id/blocked/blocked.html?kind=keyword&u=https://x.com/?q=feet'), 'keyword');
  assert.equal(B.blockedUrlFrom('chrome-extension://id/blocked/blocked.html?kind=guard'), null);
  assert.equal(B.hostOf('https://www.x.com/a?b=1'), 'www.x.com');
  assert.equal(B.hostOf('garbage'), null);
});
