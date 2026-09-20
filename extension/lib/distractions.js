// Distracting sites: presets, URL pattern grammar, matching, and the
// declarativeNetRequest rules that redirect them to the block page.
// Pure, no chrome.* calls.
//
// Pattern grammar (one per line in the options page):
//   host            the host and every subdomain, every path
//   host/           only the root page of that host (feeds usually live there)
//   host/path       every URL whose path is /path or starts with /path/
(globalThis.AntiBrainrot ||= {}).distractions = (() => {
  'use strict';

  const PRESETS = Object.freeze([
    { id: 'tiktok', label: 'TikTok', patterns: ['tiktok.com'] },
    { id: 'instagram-reels', label: 'Instagram feed, Reels, Explore and Stories', patterns: ['instagram.com/', 'instagram.com/reels', 'instagram.com/reel', 'instagram.com/explore', 'instagram.com/stories'] },
    { id: 'instagram', label: 'Instagram (everything)', patterns: ['instagram.com'] },
    { id: 'x-home', label: 'X home timeline, Explore, search and trends', patterns: ['x.com/', 'x.com/home', 'x.com/explore', 'x.com/search', 'x.com/i/trending', 'twitter.com/', 'twitter.com/home', 'twitter.com/explore', 'twitter.com/search', 'twitter.com/i/trending'] },
    { id: 'x', label: 'X (everything)', patterns: ['x.com', 'twitter.com'] },
    { id: 'reddit-home', label: 'Reddit home, popular and all', patterns: ['reddit.com/', 'reddit.com/r/popular', 'reddit.com/r/all', 'reddit.com/best', 'reddit.com/hot', 'reddit.com/new', 'reddit.com/top', 'reddit.com/rising'] },
    { id: 'reddit', label: 'Reddit (everything)', patterns: ['reddit.com'] },
    { id: 'facebook-feed', label: 'Facebook feed, Watch, Reels, Stories and Gaming', patterns: ['facebook.com/', 'facebook.com/home.php', 'facebook.com/watch', 'facebook.com/videos', 'facebook.com/reel', 'facebook.com/reels', 'facebook.com/stories', 'facebook.com/gaming', 'facebook.com/groups/feed'] },
    { id: 'facebook', label: 'Facebook (everything)', patterns: ['facebook.com'] },
    { id: 'threads', label: 'Threads', patterns: ['threads.net', 'threads.com'] },
    { id: 'linkedin-feed', label: 'LinkedIn feed', patterns: ['linkedin.com/', 'linkedin.com/feed'] },
    { id: 'bluesky-home', label: 'Bluesky home, feeds and search', patterns: ['bsky.app/', 'bsky.app/feeds', 'bsky.app/search', 'bsky.app/explore'] },
    { id: 'tumblr-dashboard', label: 'Tumblr dashboard, explore and tags', patterns: ['tumblr.com/', 'tumblr.com/dashboard', 'tumblr.com/explore', 'tumblr.com/tagged', 'tumblr.com/search'] },
    { id: 'pinterest', label: 'Pinterest', patterns: ['pinterest.com', 'pinterest.co.uk', 'pinterest.de', 'pinterest.fr', 'pinterest.es', 'pinterest.it', 'pinterest.nl', 'pinterest.ca', 'pinterest.com.au', 'pinterest.jp', 'pinterest.se', 'pinterest.ch', 'pinterest.at', 'pinterest.pt', 'pinterest.ie', 'pinterest.co.kr', 'pinterest.com.mx', 'pinterest.ru'] },
    { id: 'twitch', label: 'Twitch', patterns: ['twitch.tv'] },
    { id: 'kick', label: 'Kick', patterns: ['kick.com'] },
    { id: '9gag', label: '9GAG', patterns: ['9gag.com'] },
    { id: 'imgur-front', label: 'Imgur front page', patterns: ['imgur.com/'] },
    { id: 'snapchat', label: 'Snapchat Spotlight, Discover and Stories (chat stays open)', patterns: ['snapchat.com/spotlight', 'snapchat.com/discover', 'snapchat.com/stories', 'story.snapchat.com'] },
    { id: 'netflix-browse', label: 'Netflix browse page', patterns: ['netflix.com/browse'] },
  ]);

  const presetById = new Map(PRESETS.map((p) => [p.id, p]));

  function parsePattern(text) {
    if (typeof text !== 'string') return null;
    let s = text.trim().toLowerCase();
    if (!s) return null;
    s = s.replace(/^[a-z][a-z0-9+.-]*:\/\//, '');
    s = s.replace(/^www\./, '');
    const slash = s.indexOf('/');
    let host = slash < 0 ? s : s.slice(0, slash);
    let rest = slash < 0 ? null : s.slice(slash + 1);
    host = host.split(/[?#:]/)[0].replace(/\.$/, '');
    if (!host || !host.includes('.') || !/^[a-z0-9.-]+$/.test(host)) return null;
    if (host.split('.').some((l) => !l || l.startsWith('-') || l.endsWith('-'))) return null;
    if (rest === null) return { host, path: null };
    rest = rest.split(/[?#]/)[0].replace(/\*+$/, '').replace(/\/+$/, '');
    if (!rest) return { host, path: '' };
    if (!/^[a-z0-9._~!$&'()+,;=:@%/-]+$/i.test(rest)) return null;
    return { host, path: '/' + rest };
  }

  function formatPattern(p) {
    if (p.path === null) return p.host;
    return p.host + '/' + (p.path === '' ? '' : p.path.slice(1));
  }

  function parseList(text) {
    if (typeof text !== 'string') return [];
    const out = [];
    for (const raw of text.split(/[\n,]+/)) {
      const p = parsePattern(raw);
      if (!p) continue;
      const f = formatPattern(p);
      if (!out.includes(f)) out.push(f);
    }
    return out;
  }

  function hostMatches(patternHost, host) {
    const h = String(host || '').toLowerCase().replace(/^www\./, '');
    return h === patternHost || h.endsWith('.' + patternHost);
  }

  function pathMatches(patternPath, pathname) {
    if (patternPath === null) return true;
    const p = (pathname || '/').replace(/\/+$/, '') || '/';
    if (patternPath === '') return p === '/';
    return p === patternPath || p.startsWith(patternPath + '/');
  }

  function matchesUrl(pattern, url) {
    const p = typeof pattern === 'string' ? parsePattern(pattern) : pattern;
    if (!p) return false;
    let u;
    try {
      u = new URL(url);
    } catch {
      return false;
    }
    if (!/^https?:$/.test(u.protocol)) return false;
    return hostMatches(p.host, u.hostname) && pathMatches(p.path, u.pathname);
  }

  // Every pattern string in force for the given distractions settings.
  function activePatterns(distractions) {
    const d = distractions && typeof distractions === 'object' ? distractions : {};
    const out = [];
    for (const id of d.presets || []) {
      const preset = presetById.get(id);
      if (!preset) continue;
      for (const p of preset.patterns) if (!out.includes(p)) out.push(p);
    }
    for (const c of parseList((d.custom || []).join('\n'))) if (!out.includes(c)) out.push(c);
    return out;
  }

  function firstMatch(patterns, url) {
    for (const p of patterns) if (matchesUrl(p, url)) return p;
    return null;
  }

  function hosts(patterns) {
    const out = [];
    for (const s of patterns) {
      const p = parsePattern(s);
      if (p && !out.includes(p.host)) out.push(p.host);
    }
    return out;
  }

  // Match patterns for chrome.scripting.registerContentScripts.
  function matchPatterns(patterns) {
    const out = [];
    for (const h of hosts(patterns)) out.push(`*://${h}/*`, `*://*.${h}/*`);
    return out;
  }

  function escapeRegex(s) {
    return s.replace(/[.*+?^${}()|[\]\\/]/g, '\\$&');
  }

  // RE2 filter matching exactly the URLs the pattern covers. Chrome removes
  // dot segments before matching, so only repeated slashes need care.
  function regexFor(pattern) {
    const p = typeof pattern === 'string' ? parsePattern(pattern) : pattern;
    const host = `^https?://([^/]*\\.)?${escapeRegex(p.host)}`;
    if (p.path === null) return `${host}(/.*)?$`;
    if (p.path === '') return `${host}/*([?#].*)?$`;
    return `${host}${escapeRegex(p.path)}/*(/.*|[?#].*)?$`;
  }

  function redirectRules(patterns, extensionId, mode, startId) {
    const substitution = `chrome-extension://${extensionId}/blocked/blocked.html?kind=${mode}&u=\\0`;
    return patterns.map((pattern, i) => ({
      id: startId + i,
      priority: 2,
      action: { type: 'redirect', redirect: { regexSubstitution: substitution } },
      condition: { regexFilter: regexFor(pattern), resourceTypes: ['main_frame'] },
    }));
  }

  function passRule(pattern, id) {
    return {
      id,
      priority: 4,
      action: { type: 'allow' },
      condition: { regexFilter: regexFor(pattern), resourceTypes: ['main_frame'] },
    };
  }

  // Exceptions: URLs inside a blocked pattern that stay open, for example
  // reddit.com/r/programming under reddit.com. Priority 3 beats the redirect.
  function exceptionRules(patterns, startId) {
    return patterns.map((pattern, i) => ({
      id: startId + i,
      priority: 3,
      action: { type: 'allow' },
      condition: { regexFilter: regexFor(pattern), resourceTypes: ['main_frame'] },
    }));
  }

  function activeExceptions(distractions) {
    const d = distractions && typeof distractions === 'object' ? distractions : {};
    return parseList((d.exceptions || []).join('\n'));
  }

  // The pattern that blocks `url`, or null when nothing blocks it (no match,
  // or an exception covers it).
  function blockingPattern(distractions, url) {
    if (firstMatch(activeExceptions(distractions), url)) return null;
    return firstMatch(activePatterns(distractions), url);
  }

  // ---- passes and the daily budget

  function dayKey(now = new Date()) {
    const y = now.getFullYear();
    const m = String(now.getMonth() + 1).padStart(2, '0');
    const d = String(now.getDate()).padStart(2, '0');
    return `${y}-${m}-${d}`;
  }

  // budget = { day: 'YYYY-MM-DD', usedMinutes }
  function budgetLeft(budget, distractions, now = new Date()) {
    const limit = distractions && Number(distractions.dailyBudgetMinutes);
    if (!Number.isFinite(limit)) return 0;
    const used = budget && budget.day === dayKey(now) ? Number(budget.usedMinutes) || 0 : 0;
    return Math.max(0, limit - used);
  }

  function canPass(budget, distractions, now = new Date()) {
    const d = distractions || {};
    if (d.mode !== 'pause') return false;
    return budgetLeft(budget, d, now) >= Number(d.passMinutes || 0) && Number(d.passMinutes || 0) > 0;
  }

  // passes = { [pattern]: expiresAtMs }
  function activePass(passes, pattern, now = Date.now()) {
    const t = passes && Number(passes[pattern]);
    return Number.isFinite(t) && t > now ? t : null;
  }

  // cooldowns = { [pattern]: untilMs }. After a pass ends, the pause page
  // refuses a new pass for the same pattern until the cooldown is over.
  function cooldownUntil(cooldowns, pattern, now = Date.now()) {
    const t = cooldowns && Number(cooldowns[pattern]);
    return Number.isFinite(t) && t > now ? t : null;
  }

  return Object.freeze({
    PRESETS, parsePattern, formatPattern, parseList, matchesUrl, activePatterns, activeExceptions,
    blockingPattern, firstMatch, hosts, matchPatterns, regexFor, redirectRules, passRule,
    exceptionRules, dayKey, budgetLeft, canPass, activePass, cooldownUntil,
  });
})();
