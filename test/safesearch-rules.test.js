'use strict';
const test = require('node:test');
const assert = require('node:assert/strict');
const fs = require('node:fs');
const path = require('node:path');

const RULES = path.join(__dirname, '..', 'extension', 'rules');
const safesearch = JSON.parse(fs.readFileSync(path.join(RULES, 'safesearch.json'), 'utf8'));
const restrict = JSON.parse(fs.readFileSync(path.join(RULES, 'youtube-restrict.json'), 'utf8'));

// The parameter every rule adds is the engine's own strict setting, the
// same one Chrome's ForceGoogleSafeSearch policy appends.
function addedParams(url) {
  const hits = [];
  for (const rule of safesearch) {
    if (new RegExp(rule.condition.regexFilter, 'i').test(url)) {
      hits.push(...rule.action.redirect.transform.queryTransform.addOrReplaceParams.map((p) => `${p.key}=${p.value}`));
    }
  }
  return hits;
}

test('safesearch.json is a list of query transform redirects with unique ids', () => {
  assert.ok(Array.isArray(safesearch) && safesearch.length >= 5);
  const ids = safesearch.map((r) => r.id);
  assert.equal(new Set(ids).size, ids.length);
  for (const rule of safesearch) {
    assert.equal(rule.action.type, 'redirect', `rule ${rule.id}`);
    const params = rule.action.redirect.transform.queryTransform.addOrReplaceParams;
    assert.ok(Array.isArray(params) && params.length >= 1, `rule ${rule.id}`);
    assert.ok(rule.condition.resourceTypes.includes('main_frame'), `rule ${rule.id}`);
    assert.doesNotThrow(() => new RegExp(rule.condition.regexFilter), `rule ${rule.id}`);
    assert.ok(rule.condition.regexFilter.startsWith('^https?://'), `rule ${rule.id} must anchor on the scheme`);
  }
});

test('search results on the big engines get the strict parameter', () => {
  assert.deepEqual(addedParams('https://www.google.com/search?q=feet&udm=2'), ['safe=active', 'ssui=on']);
  assert.deepEqual(addedParams('https://www.google.co.uk/search?q=x&tbm=isch'), ['safe=active', 'ssui=on']);
  assert.deepEqual(addedParams('https://google.com.au/search?q=x'), ['safe=active', 'ssui=on']);
  assert.deepEqual(addedParams('https://www.google.nl/search?q=x'), ['safe=active', 'ssui=on']);
  assert.deepEqual(addedParams('https://www.bing.com/images/search?q=x'), ['adlt=strict']);
  assert.deepEqual(addedParams('https://www.bing.com/videos/search?q=x'), ['adlt=strict']);
  assert.deepEqual(addedParams('https://www.bing.com/search?q=x'), ['adlt=strict']);
  assert.deepEqual(addedParams('https://www.bing.com/images/async?q=x&first=35'), ['adlt=strict']);
  assert.deepEqual(addedParams('https://duckduckgo.com/?q=x&ia=images&iax=images'), ['kp=1']);
  assert.deepEqual(addedParams('https://duckduckgo.com/i.js?q=x&o=json'), ['p=1']);
  assert.deepEqual(addedParams('https://html.duckduckgo.com/html/?q=x'), ['kp=1']);
  assert.deepEqual(addedParams('https://search.yahoo.com/search?p=x'), ['vm=r']);
  assert.deepEqual(addedParams('https://images.search.yahoo.com/search/images?p=x'), ['vm=r']);
  assert.deepEqual(addedParams('https://yandex.com/images/search?text=x'), ['family=yes']);
  assert.deepEqual(addedParams('https://yandex.ru/search/?text=x'), ['family=yes']);
  assert.deepEqual(addedParams('https://search.brave.com/images?q=x'), ['safesearch=strict']);
  assert.deepEqual(addedParams('https://search.brave.com/search?q=x'), ['safesearch=strict']);
});

test('pages that are not search results are left alone', () => {
  for (const url of [
    'https://www.google.com/',
    'https://www.google.com/maps/place/x',
    'https://mail.google.com/mail/u/0/',
    'https://www.google.com/complete/search?q=x',
    'https://docs.google.com/document/d/x',
    'https://www.googleapis.com/search?q=x',
    'https://www.bing.com/',
    'https://www.bing.com/maps',
    'https://duckduckgo.com/about',
    'https://www.yahoo.com/',
    'https://mail.yahoo.com/search?p=x',
    'https://yandex.com/',
    'https://search.brave.com/',
    'https://example.com/search?q=x',
  ]) {
    assert.deepEqual(addedParams(url), [], url);
  }
});

test('youtube-restrict.json sets the Restricted Mode header on every YouTube request', () => {
  assert.equal(restrict.length, 1);
  const [rule] = restrict;
  assert.equal(rule.action.type, 'modifyHeaders');
  assert.deepEqual(rule.action.requestHeaders, [{ header: 'YouTube-Restrict', operation: 'set', value: 'Strict' }]);
  assert.ok(rule.condition.requestDomains.includes('youtube.com'));
  assert.ok(rule.condition.resourceTypes.includes('main_frame'));
  assert.ok(rule.condition.resourceTypes.includes('xmlhttprequest'));
});
