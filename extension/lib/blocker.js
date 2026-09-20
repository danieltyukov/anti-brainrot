// Adult site blocker helpers: domain normalisation, dynamic rule builders for
// the user's own lists, and block page helpers. Pure, no chrome.* calls.
(globalThis.AntiBrainrot ||= {}).blocker = (() => {
  'use strict';

  const BLOCK_PAGE = '/blocked/blocked.html';
  const DYNAMIC_BLOCK_ID = 1000;
  const DYNAMIC_ALLOW_ID = 2000;

  function normalizeDomain(entry) {
    if (typeof entry !== 'string') return null;
    let s = entry.trim().toLowerCase();
    if (!s) return null;
    if (!/^[a-z][a-z0-9+.-]*:\/\//.test(s)) s = 'http://' + s;
    let host;
    try {
      host = new URL(s).hostname;
    } catch {
      return null;
    }
    host = host.replace(/^www\./, '').replace(/\.$/, '');
    if (!host.includes('.')) return null;
    if (/^[\d.]+$/.test(host) || host.startsWith('[')) return null;
    if (!/^[a-z0-9.-]+$/.test(host)) return null;
    if (host.split('.').some((label) => !label || label.startsWith('-') || label.endsWith('-'))) return null;
    return host;
  }

  function parseDomainList(text) {
    if (typeof text !== 'string') return [];
    const out = [];
    for (const raw of text.split(/[\s,]+/)) {
      const d = normalizeDomain(raw);
      if (d && !out.includes(d)) out.push(d);
    }
    return out;
  }

  function redirectAction(extensionId) {
    return {
      type: 'redirect',
      redirect: { regexSubstitution: `chrome-extension://${extensionId}${BLOCK_PAGE}?u=\\0` },
    };
  }

  function dynamicRules(blocker, extensionId) {
    const lists = blocker && typeof blocker === 'object' ? blocker : {};
    const blocked = parseDomainList((lists.blockedDomains || []).join('\n'));
    const allowed = parseDomainList((lists.allowedDomains || []).join('\n'));
    const rules = [];
    if (blocked.length > 0) {
      // Priority 2 so a user's own entry beats the bundled allow rule for
      // benign hostnames that match a keyword (rules/adult.json rule 7).
      rules.push({
        id: DYNAMIC_BLOCK_ID,
        priority: 2,
        action: redirectAction(extensionId),
        condition: { requestDomains: blocked, regexFilter: '^https?://.*', resourceTypes: ['main_frame'] },
      });
    }
    if (allowed.length > 0) {
      rules.push({
        id: DYNAMIC_ALLOW_ID,
        priority: 3,
        action: { type: 'allow' },
        condition: { requestDomains: allowed, resourceTypes: ['main_frame'] },
      });
    }
    return rules;
  }

  function blockedUrlFrom(href) {
    if (typeof href !== 'string') return null;
    const i = href.indexOf('?u=');
    return i < 0 ? null : href.slice(i + 3);
  }

  function hostOf(url) {
    try {
      return new URL(url).hostname;
    } catch {
      return null;
    }
  }

  return Object.freeze({ BLOCK_PAGE, DYNAMIC_BLOCK_ID, DYNAMIC_ALLOW_ID, normalizeDomain, parseDomainList, dynamicRules, blockedUrlFrom, hostOf });
})();
