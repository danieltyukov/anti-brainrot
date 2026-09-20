// Service worker: seeds settings on install, keeps the toolbar badge in sync,
// and switches the adult site ruleset and the user's own domain rules.
importScripts('lib/features.js', 'lib/settings.js', 'lib/blocker.js');

const S = globalThis.AntiBrainrot.settings;
const B = globalThis.AntiBrainrot.blocker;
const ADULT_RULESET = 'adult';

async function refreshBadge(settings) {
  const off = !settings.focus.enabled;
  await chrome.action.setBadgeText({ text: off ? 'OFF' : '' });
  if (off) {
    await chrome.action.setBadgeBackgroundColor({ color: '#6b7280' });
    await chrome.action.setBadgeTextColor({ color: '#ffffff' });
  }
}

async function syncBlocker(settings) {
  const on = S.isActive(settings, 'adultSites');
  const dnr = chrome.declarativeNetRequest;
  try {
    const enabled = await dnr.getEnabledRulesets();
    const has = enabled.includes(ADULT_RULESET);
    if (on && !has) await dnr.updateEnabledRulesets({ enableRulesetIds: [ADULT_RULESET] });
    if (!on && has) await dnr.updateEnabledRulesets({ disableRulesetIds: [ADULT_RULESET] });
  } catch (err) {
    console.warn('[anti-brainrot] could not switch the adult ruleset: ' + err.message);
  }
  try {
    const existing = await dnr.getDynamicRules();
    await dnr.updateDynamicRules({
      removeRuleIds: existing.map((r) => r.id),
      addRules: on ? B.dynamicRules(settings.blocker, chrome.runtime.id) : [],
    });
  } catch (err) {
    console.warn('[anti-brainrot] could not update custom site rules: ' + err.message);
  }
}

async function sync(settings) {
  const s = settings || (await S.load());
  await refreshBadge(s);
  await syncBlocker(s);
}

chrome.runtime.onInstalled.addListener(async () => {
  // Normalises whatever is stored (or nothing) into the current schema.
  await S.save(await S.load());
  await sync();
});

chrome.runtime.onStartup.addListener(() => {
  sync();
});

S.onChange((settings) => {
  sync(settings);
});

sync();
