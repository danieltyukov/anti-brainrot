// Main-world helper for distracting sites: single-page apps navigate with
// history.pushState, which no event in the isolated world reports. This
// wraps the two history methods and raises a DOM event the watcher listens
// to. It touches nothing else.
(() => {
  'use strict';
  if (window.__abrHistoryPatched) return;
  window.__abrHistoryPatched = true;
  const fire = () => document.dispatchEvent(new CustomEvent('abr:navigate'));
  for (const method of ['pushState', 'replaceState']) {
    const original = history[method];
    if (typeof original !== 'function') continue;
    history[method] = function patched(...args) {
      const result = original.apply(this, args);
      fire();
      return result;
    };
  }
})();
