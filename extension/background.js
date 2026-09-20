// Service worker: seeds settings on install, keeps the toolbar badge in sync,
// switches the adult site ruleset and the user's own domain rules, manages
// the distracting sites rules, passes and daily budget, and enforces locked
// hours. Every piece re-derives its state from storage, so worker restarts
// are harmless.
importScripts('lib/features.js', 'lib/settings.js', 'lib/blocker.js', 'lib/distractions.js');

const S = globalThis.AntiBrainrot.settings;
const B = globalThis.AntiBrainrot.blocker;
const D = globalThis.AntiBrainrot.distractions;
const dnr = chrome.declarativeNetRequest;

const ADULT_RULESET = 'adult';
const DISTRACTION_RULE_BASE = 3000; // dynamic redirect rules 3000..3499
const EXCEPTION_RULE_BASE = 3500; // dynamic allow rules 3500..3999
const PASS_RULE_BASE = 4000; // session allow rules 4000..4999
const SCRIPT_ID = 'abr-distractions';
const MAIN_SCRIPT_ID = 'abr-distractions-main';
const SCHEDULE_ALARM = 'abr-schedule';
const PASS_ALARM_PREFIX = 'abr-pass:';

function warn(text, err) {
  console.warn('[anti-brainrot] ' + text + (err && err.message ? ': ' + err.message : ''));
}

// ---------------------------------------------------------------- badge

async function refreshBadge(settings) {
  const off = !settings.focus.enabled;
  await chrome.action.setBadgeText({ text: off ? 'OFF' : '' });
  if (off) {
    await chrome.action.setBadgeBackgroundColor({ color: '#6b7280' });
    await chrome.action.setBadgeTextColor({ color: '#ffffff' });
  }
}

// ---------------------------------------------------------------- adult sites

async function syncBlocker(settings) {
  const on = S.isActive(settings, 'adultSites');
  try {
    const enabled = await dnr.getEnabledRulesets();
    const has = enabled.includes(ADULT_RULESET);
    if (on && !has) await dnr.updateEnabledRulesets({ enableRulesetIds: [ADULT_RULESET] });
    if (!on && has) await dnr.updateEnabledRulesets({ disableRulesetIds: [ADULT_RULESET] });
  } catch (err) {
    warn('could not switch the adult ruleset', err);
  }
  try {
    const existing = await dnr.getDynamicRules();
    await dnr.updateDynamicRules({
      removeRuleIds: existing.filter((r) => r.id < DISTRACTION_RULE_BASE).map((r) => r.id),
      addRules: on ? B.dynamicRules(settings.blocker, chrome.runtime.id) : [],
    });
  } catch (err) {
    warn('could not update custom site rules', err);
  }
}

// ---------------------------------------------------------------- distracting sites

async function readLocal() {
  const got = await chrome.storage.local.get(['passes', 'budget', 'cooldowns', 'stats']);
  return { passes: got.passes || {}, budget: got.budget || null, cooldowns: got.cooldowns || {}, stats: got.stats || null };
}

function todayStats(stats) {
  const day = D.dayKey();
  return stats && stats.day === day ? { ...stats } : { day, blocks: 0, passes: 0 };
}

async function recordBlock() {
  const { stats } = await readLocal();
  const next = todayStats(stats);
  next.blocks += 1;
  await chrome.storage.local.set({ stats: next });
}

async function applyPassRules(passes, patterns) {
  const now = Date.now();
  const addRules = [];
  let id = PASS_RULE_BASE;
  for (const [pattern, expiresAt] of Object.entries(passes)) {
    if (expiresAt > now && patterns.includes(pattern)) addRules.push(D.passRule(pattern, id++));
  }
  try {
    const existing = await dnr.getSessionRules();
    await dnr.updateSessionRules({
      removeRuleIds: existing.filter((r) => r.id >= PASS_RULE_BASE && r.id < PASS_RULE_BASE + 1000).map((r) => r.id),
      addRules,
    });
  } catch (err) {
    warn('could not update pass rules', err);
  }
}

async function revokeExpiredPasses(settings) {
  const s = settings || (await S.load());
  const { passes, cooldowns } = await readLocal();
  const now = Date.now();
  const kept = {};
  const cool = {};
  for (const [pattern, until] of Object.entries(cooldowns)) if (until > now) cool[pattern] = until;
  const cooldownMs = s.distractions.cooldownMinutes * 60 * 1000;
  for (const [pattern, expiresAt] of Object.entries(passes)) {
    if (expiresAt > now) kept[pattern] = expiresAt;
    else if (cooldownMs > 0 && expiresAt + cooldownMs > now) cool[pattern] = Math.max(cool[pattern] || 0, expiresAt + cooldownMs);
  }
  await chrome.storage.local.set({ passes: kept, cooldowns: cool });
  const patterns = S.isActive(s, 'distractions') ? D.activePatterns(s.distractions) : [];
  await applyPassRules(kept, patterns);
}

async function syncDistractions(settings) {
  const on = S.isActive(settings, 'distractions');
  const patterns = on ? D.activePatterns(settings.distractions) : [];
  const exceptions = on ? D.activeExceptions(settings.distractions) : [];
  try {
    const existing = await dnr.getDynamicRules();
    await dnr.updateDynamicRules({
      removeRuleIds: existing.filter((r) => r.id >= DISTRACTION_RULE_BASE && r.id < PASS_RULE_BASE).map((r) => r.id),
      addRules: [
        ...D.redirectRules(patterns, chrome.runtime.id, settings.distractions.mode, DISTRACTION_RULE_BASE),
        ...D.exceptionRules(exceptions, EXCEPTION_RULE_BASE),
      ],
    });
  } catch (err) {
    warn('could not update distracting site rules', err);
  }
  try {
    await chrome.scripting.unregisterContentScripts({ ids: [SCRIPT_ID, MAIN_SCRIPT_ID] });
  } catch {
    // not registered yet
  }
  if (patterns.length > 0) {
    const matches = D.matchPatterns(patterns);
    try {
      await chrome.scripting.registerContentScripts([
        {
          id: MAIN_SCRIPT_ID,
          matches,
          js: ['content/distractions-main.js'],
          runAt: 'document_start',
          world: 'MAIN',
          persistAcrossSessions: true,
        },
        {
          id: SCRIPT_ID,
          matches,
          js: ['lib/features.js', 'lib/settings.js', 'lib/distractions.js', 'content/distractions.js'],
          runAt: 'document_start',
          persistAcrossSessions: true,
        },
      ]);
    } catch (err) {
      warn('could not register the distracting sites watcher (permission missing?)', err);
    }
  }
  await revokeExpiredPasses(settings);
}

async function passStatus(url) {
  const settings = await S.load();
  const { passes, budget, cooldowns } = await readLocal();
  const on = S.isActive(settings, 'distractions');
  const pattern = on && url ? D.blockingPattern(settings.distractions, url) : null;
  const d = settings.distractions;
  const cooldownUntil = pattern ? D.cooldownUntil(cooldowns, pattern) : null;
  return {
    on,
    pattern,
    mode: d.mode,
    pauseSeconds: d.pauseSeconds,
    intention: d.intention,
    passMinutes: d.passMinutes,
    dailyBudgetMinutes: d.dailyBudgetMinutes,
    budgetLeft: D.budgetLeft(budget, d),
    canPass: Boolean(pattern) && !cooldownUntil && D.canPass(budget, d),
    cooldownUntil,
    activePass: pattern ? D.activePass(passes, pattern) : null,
    filterOn: settings.focus.enabled,
    reason: settings.focus.reason,
  };
}

// Serialised so two pause pages clicking Continue at once cannot both spend
// the same budget minutes.
let passQueue = Promise.resolve();
function grantPass(url) {
  const run = passQueue.then(() => grantPassNow(url));
  passQueue = run.catch(() => {});
  return run;
}

async function grantPassNow(url) {
  const settings = await S.load();
  if (!S.isActive(settings, 'distractions')) return { ok: false, reason: 'off' };
  const pattern = D.blockingPattern(settings.distractions, url);
  if (!pattern) return { ok: false, reason: 'nomatch' };
  const { passes, budget, cooldowns, stats } = await readLocal();
  const d = settings.distractions;
  if (D.cooldownUntil(cooldowns, pattern)) return { ok: false, reason: 'cooldown' };
  if (!D.canPass(budget, d)) return { ok: false, reason: 'budget' };
  const today = D.dayKey();
  const used = budget && budget.day === today ? Number(budget.usedMinutes) || 0 : 0;
  const expiresAt = Date.now() + d.passMinutes * 60 * 1000;
  passes[pattern] = expiresAt;
  const newBudget = { day: today, usedMinutes: used + d.passMinutes };
  const newStats = todayStats(stats);
  newStats.passes += 1;
  await chrome.storage.local.set({ passes, budget: newBudget, stats: newStats });
  await applyPassRules(passes, D.activePatterns(d));
  await chrome.alarms.create(PASS_ALARM_PREFIX + pattern, { when: expiresAt + 500 });
  return { ok: true, pattern, expiresAt, budgetLeft: D.budgetLeft(newBudget, d) };
}

// ---------------------------------------------------------------- locked hours

async function enforceSchedule(settings) {
  const s = settings || (await S.load());
  if (S.isLockedNow(s) && !s.focus.enabled) {
    await S.patch({ focus: { enabled: true } });
  }
}

// ---------------------------------------------------------------- sync

async function sync(settings) {
  const s = settings || (await S.load());
  await refreshBadge(s);
  await syncBlocker(s);
  await syncDistractions(s);
  await enforceSchedule(s);
}

// Runs on every worker start: normalises whatever is stored into the current
// schema (a no-op write when nothing changed), makes sure the schedule alarm
// exists, and re-derives all rules. Chrome does not always fire onInstalled
// for developer loads, so nothing depends on it.
async function boot() {
  const raw = (await chrome.storage.sync.get(S.KEY))[S.KEY];
  let settings = S.normalize(raw);
  if (JSON.stringify(raw) !== JSON.stringify(settings)) settings = await S.patch({});
  const existing = await chrome.alarms.get(SCHEDULE_ALARM);
  if (!existing) await chrome.alarms.create(SCHEDULE_ALARM, { periodInMinutes: 1 });
  await sync(settings);
}

chrome.runtime.onInstalled.addListener(() => {
  boot();
});

chrome.runtime.onStartup.addListener(() => {
  boot();
});

S.onChange((settings) => {
  sync(settings);
});

chrome.alarms.onAlarm.addListener((alarm) => {
  if (alarm.name === SCHEDULE_ALARM) enforceSchedule();
  else if (alarm.name.startsWith(PASS_ALARM_PREFIX)) revokeExpiredPasses();
});

chrome.runtime.onMessage.addListener((message, sender, sendResponse) => {
  if (!message || typeof message !== 'object') return false;
  if (message.type === 'pass-status') {
    passStatus(message.url).then(sendResponse, (err) => sendResponse({ error: err.message }));
    return true;
  }
  if (message.type === 'pass') {
    grantPass(message.url).then(sendResponse, (err) => sendResponse({ ok: false, reason: err.message }));
    return true;
  }
  if (message.type === 'record-block') {
    recordBlock().then(() => sendResponse({ ok: true }), () => sendResponse({ ok: false }));
    return true;
  }
  return false;
});

// The popup asks for the all-sites permission when a web feature is switched
// on and notes which one in storage. Chrome's prompt can close the popup
// before it can write the setting, so the worker completes the switch when
// the grant arrives, and reflects a revocation made in chrome://extensions.
chrome.permissions.onAdded.addListener(async (granted) => {
  if (!(granted.origins || []).includes('<all_urls>')) return;
  const { pendingFeature } = await chrome.storage.local.get('pendingFeature');
  if (!pendingFeature) return;
  await chrome.storage.local.remove('pendingFeature');
  try {
    await S.update({ features: { [pendingFeature]: true } });
  } catch (err) {
    warn('could not switch on ' + pendingFeature, err);
  }
});

chrome.permissions.onRemoved.addListener(async (removed) => {
  if (!(removed.origins || []).includes('<all_urls>')) return;
  const s = await S.load();
  if (!s.features.adultSites && !s.features.distractions) return;
  await S.patch({ features: { adultSites: false, distractions: false } });
});

boot();
