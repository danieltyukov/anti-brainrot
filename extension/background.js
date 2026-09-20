// Service worker: seeds settings on install and keeps the toolbar badge in sync.
importScripts('lib/features.js', 'lib/settings.js');

const S = globalThis.AntiBrainrot.settings;

async function refreshBadge(settings) {
  const s = settings || (await S.load());
  const off = !s.focus.enabled;
  await chrome.action.setBadgeText({ text: off ? 'OFF' : '' });
  if (off) {
    await chrome.action.setBadgeBackgroundColor({ color: '#6b7280' });
    await chrome.action.setBadgeTextColor({ color: '#ffffff' });
  }
}

chrome.runtime.onInstalled.addListener(async () => {
  // Normalises whatever is stored (or nothing) into the current schema.
  await S.save(await S.load());
  await refreshBadge();
});

chrome.runtime.onStartup.addListener(() => {
  refreshBadge();
});

S.onChange((settings) => {
  refreshBadge(settings);
});

refreshBadge();
