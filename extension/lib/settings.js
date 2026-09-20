// Settings: defaults, validation, persistence in chrome.storage.sync, and the
// mapping from settings to the data-unrot-* attributes that hide.css keys on.
// Requires lib/features.js to be loaded first.
(globalThis.Unrot ||= {}).settings = (() => {
  'use strict';

  const { FEATURES, byId } = globalThis.Unrot.features;

  const VERSION = 1;
  const KEY = 'settings';
  const DELAY_CHOICES = Object.freeze([0, 30, 60, 300, 600, 1800, 3600]);
  const DEFAULT_DELAY = 300;
  const THEMES = Object.freeze(['system', 'light', 'dark']);
  const DEFAULT_CATEGORIES = Object.freeze(['Education', 'Science & Technology', 'Howto & Style']);

  let warned = false;
  function warnOnce(msg) {
    if (warned) return;
    warned = true;
    console.warn('[unrot] ' + msg);
  }

  function defaults() {
    const features = {};
    for (const f of FEATURES) features[f.id] = f.default;
    return {
      version: VERSION,
      focus: { enabled: true, unlockDelaySec: DEFAULT_DELAY },
      theme: 'system',
      features,
      educational: {
        allowedCategories: [...DEFAULT_CATEGORIES],
        allowedChannels: [],
      },
    };
  }

  function isObject(v) {
    return v !== null && typeof v === 'object' && !Array.isArray(v);
  }

  function cleanStringList(v) {
    if (!Array.isArray(v)) return null;
    return v
      .filter((s) => typeof s === 'string')
      .map((s) => s.trim())
      .filter((s) => s.length > 0);
  }

  function normalize(raw) {
    const out = defaults();
    if (!isObject(raw)) return out;

    if (isObject(raw.focus)) {
      if (typeof raw.focus.enabled === 'boolean') out.focus.enabled = raw.focus.enabled;
      const d = Number(raw.focus.unlockDelaySec);
      if (DELAY_CHOICES.includes(d)) out.focus.unlockDelaySec = d;
    }

    if (THEMES.includes(raw.theme)) out.theme = raw.theme;

    if (isObject(raw.features)) {
      for (const f of FEATURES) {
        if (f.locked) continue;
        if (typeof raw.features[f.id] === 'boolean') out.features[f.id] = raw.features[f.id];
      }
    }

    if (isObject(raw.educational)) {
      const cats = cleanStringList(raw.educational.allowedCategories);
      if (cats) out.educational.allowedCategories = cats;
      const chans = cleanStringList(raw.educational.allowedChannels);
      if (chans) out.educational.allowedChannels = chans;
    }

    return out;
  }

  function area() {
    const c = globalThis.chrome;
    return c && c.storage && c.storage.sync ? c.storage.sync : null;
  }

  async function load() {
    const a = area();
    if (!a) {
      warnOnce('chrome.storage.sync unavailable, using defaults');
      return defaults();
    }
    try {
      const got = await a.get(KEY);
      return normalize(got ? got[KEY] : undefined);
    } catch (err) {
      warnOnce('failed to read settings: ' + (err && err.message));
      return defaults();
    }
  }

  async function save(settings) {
    const a = area();
    if (!a) return;
    await a.set({ [KEY]: normalize(settings) });
  }

  // Merges one level deep inside each section so callers can pass partial
  // objects like { focus: { enabled: false } } without clobbering siblings.
  function merge(current, patch) {
    const next = { ...current };
    if (!isObject(patch)) return next;
    for (const [k, v] of Object.entries(patch)) {
      if (isObject(v) && isObject(current[k])) next[k] = { ...current[k], ...v };
      else next[k] = v;
    }
    return next;
  }

  async function update(patch) {
    const merged = normalize(merge(await load(), patch));
    await save(merged);
    return merged;
  }

  function onChange(cb) {
    const c = globalThis.chrome;
    if (!c || !c.storage || !c.storage.onChanged) return;
    c.storage.onChanged.addListener((changes, areaName) => {
      if (areaName !== 'sync' || !changes[KEY]) return;
      cb(normalize(changes[KEY].newValue));
    });
  }

  function isActive(settings, id) {
    if (!settings || !settings.focus || !settings.focus.enabled) return false;
    const f = byId(id);
    if (!f) return false;
    if (f.locked) return true;
    if (!settings.features[id]) return false;
    if (f.parent) {
      const parentOn = Boolean(settings.features[f.parent]);
      if (f.mode === 'when-parent-off' && parentOn) return false;
      if (f.mode === 'when-parent-on' && !parentOn) return false;
    }
    return true;
  }

  function activeAttributes(settings) {
    const attrs = [];
    for (const f of FEATURES) {
      if (f.attr && isActive(settings, f.id)) attrs.push(f.attr);
    }
    return attrs;
  }

  return Object.freeze({
    VERSION,
    KEY,
    DELAY_CHOICES,
    THEMES,
    DEFAULT_CATEGORIES,
    defaults,
    normalize,
    load,
    save,
    update,
    onChange,
    isActive,
    activeAttributes,
  });
})();
