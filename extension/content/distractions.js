// Watcher for distracting sites, registered by the service worker only for
// the hosts in force. Network-level rules catch full page loads; this script
// catches in-app navigation (pushState, reported by distractions-main.js),
// applies the grayscale look, warns before a pass ends, and sends the tab
// back to the pause page when a pass runs out. The block page is listed
// under web_accessible_resources because a navigation started from a content
// script counts as web-initiated and Chrome refuses it otherwise.
(() => {
  'use strict';

  const { settings: S, distractions: D } = globalThis.AntiBrainrot;
  const WARN_BEFORE_MS = 30 * 1000;
  let settings = null;
  let passes = {};
  let lastHref = location.href;
  let warnTimer = null;

  function setGray(on) {
    document.documentElement.style.filter = on ? 'grayscale(1)' : '';
  }

  function onDistractionHost() {
    const hosts = D.hosts(D.activePatterns(settings.distractions));
    return hosts.some((h) => D.matchesUrl(h, location.href));
  }

  function showWarning(msLeft) {
    const existing = document.getElementById('abr-pass-warning');
    if (existing) existing.remove();
    const box = document.createElement('div');
    box.id = 'abr-pass-warning';
    box.setAttribute('role', 'status');
    box.textContent = `Anti-Brainrot: your pass ends in ${Math.max(1, Math.round(msLeft / 1000))} seconds.`;
    Object.assign(box.style, {
      position: 'fixed', top: '16px', right: '16px', zIndex: '2147483647', padding: '12px 16px',
      borderRadius: '12px', background: '#1a1a2e', color: '#eceade', font: '600 14px/1.4 Bahnschrift, "Segoe UI", sans-serif',
      boxShadow: '0 10px 30px rgba(10,18,32,0.35)',
    });
    (document.body || document.documentElement).appendChild(box);
    setTimeout(() => box.remove(), 8000);
  }

  function scheduleWarning(expiresAt) {
    clearTimeout(warnTimer);
    const delay = expiresAt - WARN_BEFORE_MS - Date.now();
    if (delay <= 0) return;
    warnTimer = setTimeout(() => showWarning(WARN_BEFORE_MS), delay);
  }

  function check() {
    // A blocked keyword in the address outranks everything, pass or not.
    if (settings && S.isActive(settings, 'keywords') && U.keywords.match(settings.keywords.blocked, location.href)) {
      location.replace(chrome.runtime.getURL('blocked/blocked.html') + '?kind=keyword&u=' + encodeURIComponent(location.href));
      return;
    }
    if (!settings || !S.isActive(settings, 'distractions')) {
      setGray(false);
      return;
    }
    const d = settings.distractions;
    const pattern = D.blockingPattern(d, location.href);
    if (!pattern) {
      setGray(Boolean(d.grayscaleAlways) && onDistractionHost());
      clearTimeout(warnTimer);
      return;
    }
    const pass = D.activePass(passes, pattern);
    if (pass) {
      setGray(Boolean(d.grayscalePass || d.grayscaleAlways));
      scheduleWarning(pass);
      return;
    }
    clearTimeout(warnTimer);
    const target = chrome.runtime.getURL('blocked/blocked.html')
      + '?kind=' + encodeURIComponent(d.mode)
      + '&u=' + encodeURIComponent(location.href);
    location.replace(target);
  }

  async function boot() {
    settings = await S.load();
    try {
      passes = (await chrome.storage.local.get('passes')).passes || {};
    } catch {
      passes = {};
    }
    check();
  }

  document.addEventListener('abr:navigate', () => {
    lastHref = location.href;
    check();
  });
  setInterval(() => {
    if (location.href !== lastHref) {
      lastHref = location.href;
      check();
    }
  }, 500);
  window.addEventListener('popstate', check);
  chrome.storage.onChanged.addListener((changes, area) => {
    if (area === 'local' && changes.passes) {
      passes = changes.passes.newValue || {};
      check();
    }
  });
  S.onChange((s) => {
    settings = s;
    check();
  });
  boot();
})();
