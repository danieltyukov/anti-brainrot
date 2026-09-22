// Blocked keywords: a list of words the user never wants to see in a web
// address. Any navigation whose path or query contains one, as a whole word,
// goes to the block page; frames and fetches with one are blocked. The host
// does not count, that is the adult list's job. Pure, no chrome.* calls.
(globalThis.AntiBrainrot ||= {}).keywords = (() => {
  'use strict';

  const BLOCK_PAGE = '/blocked/blocked.html?kind=keyword&u=';
  const RULE_BASE = 5000; // dynamic ids 5000..5999
  const PRIORITY = 5; // above passes (4) and exceptions (3)
  const PER_RULE = 5; // keywords per regex, kept small for RE2's limits
  const MAX = 200;
  const MAX_LENGTH = 40;
  // Letters and digits in any script; everything else is a separator.
  const WORD = /[\p{L}\p{N}]/u;
  const NOT_WORD = /[^\p{L}\p{N} ]/gu;

  function normalize(entry) {
    if (typeof entry !== 'string') return null;
    const s = entry.toLowerCase().replace(NOT_WORD, '').replace(/\s+/g, ' ').trim().slice(0, MAX_LENGTH).trim();
    if (s.length < 2 || !WORD.test(s)) return null;
    return s;
  }

  function parseList(text) {
    if (typeof text !== 'string') return [];
    const out = [];
    for (const raw of text.split(/[\n,]+/)) {
      const k = normalize(raw);
      if (k && !out.includes(k)) out.push(k);
      if (out.length >= MAX) break;
    }
    return out;
  }

  function escapeRegex(s) {
    return s.replace(/[.*+?^${}()|[\]\\]/g, '\\$&');
  }

  // The part of a URL a keyword is looked for in: path and query, decoded,
  // with plus signs as spaces the way search engines send them.
  function searchable(url) {
    let u;
    try {
      u = new URL(url);
    } catch {
      return null;
    }
    if (u.protocol !== 'http:' && u.protocol !== 'https:') return null;
    const raw = (u.pathname + u.search).replace(/\+/g, ' ');
    try {
      return decodeURIComponent(raw);
    } catch {
      return raw;
    }
  }

  const matchers = new Map();
  function matcher(keyword) {
    let re = matchers.get(keyword);
    if (!re) {
      const words = keyword.split(' ').map(escapeRegex).join('[\\s_.-]+');
      re = new RegExp(`(?:^|[^\\p{L}\\p{N}])${words}(?:$|[^\\p{L}\\p{N}])`, 'iu');
      matchers.set(keyword, re);
    }
    return re;
  }

  // The first listed keyword found in the URL, or null.
  function match(list, url) {
    if (!Array.isArray(list) || list.length === 0) return null;
    const text = searchable(url);
    if (!text) return null;
    for (const keyword of list) {
      const k = normalize(keyword);
      if (k && matcher(k).test(text)) return k;
    }
    return null;
  }

  // RE2 pattern for one keyword as it appears in a request URL: each word
  // percent-encoded the way the browser sends it, words joined by a plus,
  // %20, a dash, a dot or an underscore.
  function pattern(keyword) {
    return keyword.split(' ').map((w) => escapeRegex(encodeURIComponent(w))).join('(?:\\+|%20|[-_.])+');
  }

  // Matches after the host only. A word boundary in a URL is any character
  // that is not a letter or digit, or a percent-encoded byte.
  function regexFilter(group) {
    return `^https?://[^/]+/(?:.*[^a-z0-9]|.*%[0-9a-f]{2})?(?:${group.map(pattern).join('|')})(?:[^a-z0-9]|%[0-9a-f]{2}|$)`;
  }

  function rules(list, extensionId) {
    const keywords = parseList((Array.isArray(list) ? list : []).join('\n'));
    const out = [];
    for (let i = 0; i < keywords.length; i += PER_RULE) {
      const filter = regexFilter(keywords.slice(i, i + PER_RULE));
      out.push({
        id: RULE_BASE + out.length,
        priority: PRIORITY,
        action: { type: 'redirect', redirect: { regexSubstitution: `chrome-extension://${extensionId}${BLOCK_PAGE}\\0` } },
        condition: { regexFilter: filter, isUrlFilterCaseSensitive: false, resourceTypes: ['main_frame'] },
      });
      out.push({
        id: RULE_BASE + out.length,
        priority: PRIORITY,
        action: { type: 'block' },
        condition: { regexFilter: filter, isUrlFilterCaseSensitive: false, resourceTypes: ['sub_frame', 'xmlhttprequest', 'websocket', 'other'] },
      });
    }
    return out;
  }

  return Object.freeze({ RULE_BASE, MAX, MAX_LENGTH, normalize, parseList, match, rules });
})();
