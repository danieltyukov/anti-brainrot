// Settings: defaults, validation, persistence in chrome.storage.sync, and the
// mapping from settings to the data-abr-* attributes that hide.css keys on.
// Requires lib/features.js to be loaded first.
(globalThis.AntiBrainrot ||= {}).settings = (() => {
  'use strict';

  const { FEATURES, byId } = globalThis.AntiBrainrot.features;

  const VERSION = 1;
  const KEY = 'settings';
  const DELAY_CHOICES = Object.freeze([0, 30, 60, 300, 600, 1800, 3600]);
  const DEFAULT_DELAY = 300;
  const THEMES = Object.freeze(['system', 'light', 'dark']);
  const DEFAULT_CATEGORIES = Object.freeze(['Education', 'Science & Technology', 'Howto & Style']);
  const MODES = Object.freeze(['block', 'pause']);
  const PASS_CHOICES = Object.freeze([1, 2, 5, 10, 15, 30]);
  const PAUSE_CHOICES = Object.freeze([5, 10, 20, 30, 60]);
  const BUDGET_CHOICES = Object.freeze([0, 10, 15, 30, 60, 120, 1440]);
  const COOLDOWN_CHOICES = Object.freeze([0, 5, 15, 30, 60]);
  const LOCK_CHOICES = Object.freeze([1, 2, 4, 8, 24]);
  const MAX_KEYWORDS = 200;
  const DEFAULT_PRESETS = Object.freeze(['tiktok', 'instagram-reels', 'x-home', 'reddit-home', 'facebook-feed', 'threads', '9gag']);
  const TIME = /^([01]\d|2[0-3]):[0-5]\d$/;

  let warned = false;
  function warnOnce(msg) {
    if (warned) return;
    warned = true;
    console.warn('[anti-brainrot] ' + msg);
  }

  function defaults() {
    const features = {};
    for (const f of FEATURES) features[f.id] = f.default;
    return {
      version: VERSION,
      focus: { enabled: true, unlockDelaySec: DEFAULT_DELAY, lockUntil: 0, reason: '' },
      theme: 'system',
      features,
      educational: {
        allowedCategories: [...DEFAULT_CATEGORIES],
        allowedChannels: [],
      },
      blocker: {
        blockedDomains: [],
        allowedDomains: [],
      },
      keywords: {
        blocked: [],
      },
      distractions: {
        mode: 'pause',
        presets: [...DEFAULT_PRESETS],
        custom: [],
        exceptions: [],
        passMinutes: 5,
        pauseSeconds: 10,
        dailyBudgetMinutes: 30,
        cooldownMinutes: 15,
        grayscalePass: true,
        grayscaleAlways: false,
        intention: true,
      },
      schedule: {
        days: [1, 2, 3, 4, 5],
        start: '09:00',
        end: '17:00',
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
      const lock = Number(raw.focus.lockUntil);
      if (Number.isFinite(lock) && lock > 0) out.focus.lockUntil = Math.floor(lock);
      if (typeof raw.focus.reason === 'string') out.focus.reason = raw.focus.reason.trim().slice(0, 200);
    }

    if (THEMES.includes(raw.theme)) out.theme = raw.theme;

    if (isObject(raw.features)) {
      for (const f of FEATURES) {
        if (typeof raw.features[f.id] === 'boolean') out.features[f.id] = raw.features[f.id];
      }
    }

    if (isObject(raw.educational)) {
      const cats = cleanStringList(raw.educational.allowedCategories);
      if (cats) out.educational.allowedCategories = cats;
      const chans = cleanStringList(raw.educational.allowedChannels);
      if (chans) out.educational.allowedChannels = chans;
    }

    if (isObject(raw.blocker)) {
      const blocked = cleanStringList(raw.blocker.blockedDomains);
      if (blocked) out.blocker.blockedDomains = blocked;
      const allowed = cleanStringList(raw.blocker.allowedDomains);
      if (allowed) out.blocker.allowedDomains = allowed;
    }

    if (isObject(raw.keywords)) {
      const list = cleanStringList(raw.keywords.blocked);
      if (list) out.keywords.blocked = list.slice(0, MAX_KEYWORDS);
    }

    if (isObject(raw.distractions)) {
      const d = raw.distractions;
      const o = out.distractions;
      if (MODES.includes(d.mode)) o.mode = d.mode;
      const presets = cleanStringList(d.presets);
      if (presets) o.presets = presets;
      const custom = cleanStringList(d.custom);
      if (custom) o.custom = custom;
      const exceptions = cleanStringList(d.exceptions);
      if (exceptions) o.exceptions = exceptions;
      if (PASS_CHOICES.includes(Number(d.passMinutes))) o.passMinutes = Number(d.passMinutes);
      if (PAUSE_CHOICES.includes(Number(d.pauseSeconds))) o.pauseSeconds = Number(d.pauseSeconds);
      if (BUDGET_CHOICES.includes(Number(d.dailyBudgetMinutes))) o.dailyBudgetMinutes = Number(d.dailyBudgetMinutes);
      if (COOLDOWN_CHOICES.includes(Number(d.cooldownMinutes))) o.cooldownMinutes = Number(d.cooldownMinutes);
      if (typeof d.grayscalePass === 'boolean') o.grayscalePass = d.grayscalePass;
      if (typeof d.grayscaleAlways === 'boolean') o.grayscaleAlways = d.grayscaleAlways;
      if (typeof d.intention === 'boolean') o.intention = d.intention;
    }

    if (isObject(raw.schedule)) {
      const sc = raw.schedule;
      if (Array.isArray(sc.days)) {
        const days = [...new Set(sc.days.map(Number).filter((d) => Number.isInteger(d) && d >= 0 && d <= 6))].sort();
        out.schedule.days = days;
      }
      const start = TIME.test(sc.start) ? sc.start : out.schedule.start;
      const end = TIME.test(sc.end) ? sc.end : out.schedule.end;
      if (start < end) {
        out.schedule.start = start;
        out.schedule.end = end;
      }
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

  function lowerSet(list) {
    return new Set((list || []).map((s) => String(s).trim().toLowerCase()));
  }

  function hasNew(before, after) {
    const b = lowerSet(before);
    for (const v of lowerSet(after)) if (!b.has(v)) return true;
    return false;
  }

  // True when `next` restricts less than `current` in any way. Used to apply
  // the rule "tighten any time, loosen only while the filter is off".
  function isLoosening(current, next) {
    const a = normalize(current);
    const b = normalize(next);
    for (const f of FEATURES) {
      if (a.features[f.id] && !b.features[f.id]) return true;
    }
    if (b.focus.unlockDelaySec < a.focus.unlockDelaySec) return true;
    if (b.focus.lockUntil < a.focus.lockUntil) return true;
    if (b.features.educational) {
      if (hasNew(a.educational.allowedCategories, b.educational.allowedCategories)) return true;
      if (hasNew(a.educational.allowedChannels, b.educational.allowedChannels)) return true;
    }
    if (b.features.adultSites) {
      if (hasNew(b.blocker.blockedDomains, a.blocker.blockedDomains)) return true;
      if (hasNew(a.blocker.allowedDomains, b.blocker.allowedDomains)) return true;
    }
    if (b.features.keywords) {
      if (hasNew(b.keywords.blocked, a.keywords.blocked)) return true;
    }
    if (b.features.distractions) {
      const x = a.distractions;
      const y = b.distractions;
      if (x.mode === 'block' && y.mode === 'pause') return true;
      if (hasNew(y.presets, x.presets)) return true;
      if (hasNew(y.custom, x.custom)) return true;
      if (hasNew(x.exceptions, y.exceptions)) return true;
      if (y.passMinutes > x.passMinutes) return true;
      if (y.pauseSeconds < x.pauseSeconds) return true;
      if (y.dailyBudgetMinutes > x.dailyBudgetMinutes) return true;
      if (y.cooldownMinutes < x.cooldownMinutes) return true;
      if (x.grayscalePass && !y.grayscalePass) return true;
      if (x.grayscaleAlways && !y.grayscaleAlways) return true;
      if (x.intention && !y.intention) return true;
    }
    if (b.features.schedule) {
      if (hasNew(b.schedule.days.map(String), a.schedule.days.map(String))) return true;
      if (b.schedule.start > a.schedule.start) return true;
      if (b.schedule.end < a.schedule.end) return true;
    }
    return false;
  }

  function minutesOf(hhmm) {
    const [h, m] = hhmm.split(':').map(Number);
    return h * 60 + m;
  }

  // Locked hours: true when the schedule feature is on and `now` falls inside
  // the window on one of the chosen days, or when an ad hoc lock (Lock for N
  // hours) has not run out yet. Ignores the filter switch on purpose, because
  // the lock is what forces the switch on.
  function isLockedNow(settings, now = new Date()) {
    const s = normalize(settings);
    if (s.focus.lockUntil > now.getTime()) return true;
    if (!s.features.schedule) return false;
    if (!s.schedule.days.includes(now.getDay())) return false;
    const minutes = now.getHours() * 60 + now.getMinutes();
    return minutes >= minutesOf(s.schedule.start) && minutes < minutesOf(s.schedule.end);
  }

  function pad2(n) {
    return String(n).padStart(2, '0');
  }

  // Human text for when the current lock ends, for example "17:00" or
  // "tomorrow 09:30".
  function lockedUntilText(settings, now = new Date()) {
    const s = normalize(settings);
    let end = null;
    if (s.focus.lockUntil > now.getTime()) end = new Date(s.focus.lockUntil);
    if (s.features.schedule && s.schedule.days.includes(now.getDay())) {
      const minutes = now.getHours() * 60 + now.getMinutes();
      if (minutes >= minutesOf(s.schedule.start) && minutes < minutesOf(s.schedule.end)) {
        const [h, m] = s.schedule.end.split(':').map(Number);
        const scheduleEnd = new Date(now);
        scheduleEnd.setHours(h, m, 0, 0);
        if (!end || scheduleEnd > end) end = scheduleEnd;
      }
    }
    if (!end) return '';
    const sameDay = end.toDateString() === now.toDateString();
    const time = `${pad2(end.getHours())}:${pad2(end.getMinutes())}`;
    return sameDay ? time : `tomorrow ${time}`;
  }

  class LockedError extends Error {
    constructor(message) {
      super(message || 'The filter is on. Loosening it needs the filter off first.');
      this.name = 'LockedError';
    }
  }

  // Applies a partial change. Throws LockedError when the filter is on, stays
  // on, and the change would loosen it. Calls are serialised so that two
  // overlapping updates (two quick clicks, or the popup and the options page)
  // cannot read the same stale snapshot and overwrite each other.
  let queue = Promise.resolve();
  function update(patch) {
    const run = queue.then(async () => {
      const current = await load();
      const merged = normalize(merge(current, patch));
      if (current.focus.enabled && merged.focus.enabled && isLoosening(current, merged)) {
        throw new LockedError();
      }
      if (current.focus.enabled && !merged.focus.enabled && isLockedNow(current)) {
        throw new LockedError(`Locked until ${lockedUntilText(current)}. The filter stays on.`);
      }
      await save(merged);
      return merged;
    });
    queue = run.catch(() => {});
    return run;
  }

  // Same queue, no guards. For the worker's own enforcement writes (locked
  // hours forcing the switch on, a revoked permission switching a feature
  // off), never for user actions.
  function patch(partial) {
    const run = queue.then(async () => {
      const merged = normalize(merge(await load(), partial));
      await save(merged);
      return merged;
    });
    queue = run.catch(() => {});
    return run;
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
    MODES,
    PASS_CHOICES,
    PAUSE_CHOICES,
    BUDGET_CHOICES,
    COOLDOWN_CHOICES,
    LOCK_CHOICES,
    DEFAULT_PRESETS,
    defaults,
    normalize,
    load,
    save,
    update,
    patch,
    isLoosening,
    isLockedNow,
    lockedUntilText,
    LockedError,
    onChange,
    isActive,
    activeAttributes,
  });
})();
