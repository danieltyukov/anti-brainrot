// Options page: educational mode lists, blocker lists, prevent removal
// status, delay, theme, data.
(() => {
  'use strict';

  const { settings: S, education: E, blocker: B, keywords: K, countdown: C, distractions: D } = globalThis.AntiBrainrot;
  const DAYS = ['Sun', 'Mon', 'Tue', 'Wed', 'Thu', 'Fri', 'Sat'];
  const $ = (id) => document.getElementById(id);
  let settings = null;
  let statusTimer = null;

  function status(text, isError) {
    const el = $('status');
    el.textContent = text;
    el.classList.toggle('error', Boolean(isError));
    clearTimeout(statusTimer);
    statusTimer = setTimeout(() => {
      el.textContent = '';
      el.classList.remove('error');
    }, isError ? 6000 : 2000);
  }

  async function apply(patch) {
    try {
      settings = await S.update(patch);
      status('Saved');
      render();
      return true;
    } catch (err) {
      status(err.message, true);
      render();
      return false;
    }
  }

  // ---------------------------------------------------------------- render

  function renderCategories() {
    const grid = $('categories');
    grid.replaceChildren();
    for (const category of E.CATEGORIES) {
      const label = document.createElement('label');
      const input = document.createElement('input');
      input.type = 'checkbox';
      input.value = category;
      input.checked = settings.educational.allowedCategories.includes(category);
      input.addEventListener('change', () => {
        const next = E.CATEGORIES.filter((c) => (c === category ? input.checked : settings.educational.allowedCategories.includes(c)));
        apply({ educational: { allowedCategories: next } });
      });
      label.append(input, document.createTextNode(category));
      grid.appendChild(label);
    }
  }

  function renderDelay() {
    const select = $('delay');
    select.replaceChildren();
    for (const seconds of S.DELAY_CHOICES) {
      const option = document.createElement('option');
      option.value = String(seconds);
      option.textContent = C.delayLabel(seconds);
      option.selected = seconds === settings.focus.unlockDelaySec;
      select.appendChild(option);
    }
    if (document.activeElement !== $('reason')) $('reason').value = settings.focus.reason;
    $('delay-hint').textContent = settings.focus.enabled
      ? 'The filter is on: the delay can be made longer now, shorter only once the filter is off.'
      : 'Applies the next time the filter is turned off.';
  }

  function renderTheme() {
    const box = $('theme');
    box.replaceChildren();
    for (const theme of S.THEMES) {
      const label = document.createElement('label');
      const input = document.createElement('input');
      input.type = 'radio';
      input.name = 'theme';
      input.value = theme;
      input.checked = settings.theme === theme;
      input.addEventListener('change', () => apply({ theme }));
      label.append(input, document.createTextNode(theme[0].toUpperCase() + theme.slice(1)));
      box.appendChild(label);
    }
  }

  function renderSelect(id, choices, value, label) {
    const select = $(id);
    select.replaceChildren();
    for (const v of choices) {
      const option = document.createElement('option');
      option.value = String(v);
      option.textContent = label(v);
      option.selected = v === value;
      select.appendChild(option);
    }
  }

  function renderDistractions() {
    const d = settings.distractions;
    const modes = $('dmode');
    modes.replaceChildren();
    for (const [mode, text] of [['pause', 'Pause, then a timed pass'], ['block', 'Block outright']]) {
      const label = document.createElement('label');
      const input = document.createElement('input');
      input.type = 'radio';
      input.name = 'dmode';
      input.value = mode;
      input.checked = d.mode === mode;
      input.addEventListener('change', () => apply({ distractions: { mode } }));
      label.append(input, document.createTextNode(text));
      modes.appendChild(label);
    }
    const grid = $('presets');
    grid.replaceChildren();
    for (const preset of D.PRESETS) {
      const label = document.createElement('label');
      const input = document.createElement('input');
      input.type = 'checkbox';
      input.checked = d.presets.includes(preset.id);
      input.addEventListener('change', () => {
        const next = D.PRESETS.map((p) => p.id).filter((id) => (id === preset.id ? input.checked : settings.distractions.presets.includes(id)));
        apply({ distractions: { presets: next } });
      });
      label.append(input, document.createTextNode(preset.label));
      grid.appendChild(label);
    }
    if (document.activeElement !== $('custom-patterns')) $('custom-patterns').value = d.custom.join('\n');
    if (document.activeElement !== $('exceptions')) $('exceptions').value = d.exceptions.join('\n');
    renderSelect('cooldown', S.COOLDOWN_CHOICES, d.cooldownMinutes, (v) => (v === 0 ? 'None' : C.delayLabel(v * 60)));
    $('grayscale-always').checked = d.grayscaleAlways;
    renderSelect('pause-seconds', S.PAUSE_CHOICES, d.pauseSeconds, (v) => `${v} seconds`);
    renderSelect('pass-minutes', S.PASS_CHOICES, d.passMinutes, (v) => C.delayLabel(v * 60));
    renderSelect('daily-budget', S.BUDGET_CHOICES, d.dailyBudgetMinutes, (v) => (v === 0 ? 'No passes' : v >= 1440 ? 'Unlimited' : C.delayLabel(v * 60)));
    $('intention-on').checked = d.intention;
    $('grayscale-pass').checked = d.grayscalePass;
  }

  function renderSchedule() {
    $('schedule-enabled').checked = settings.features.schedule;
    const box = $('days');
    box.replaceChildren();
    for (let day = 1; day <= 7; day += 1) {
      const index = day % 7;
      const label = document.createElement('label');
      const input = document.createElement('input');
      input.type = 'checkbox';
      input.checked = settings.schedule.days.includes(index);
      input.addEventListener('change', () => {
        const next = [0, 1, 2, 3, 4, 5, 6].filter((i) => (i === index ? input.checked : settings.schedule.days.includes(i)));
        apply({ schedule: { days: next } });
      });
      label.append(input, document.createTextNode(DAYS[index]));
      box.appendChild(label);
    }
    $('schedule-start').value = settings.schedule.start;
    $('schedule-end').value = settings.schedule.end;
  }

  const UPDATE_URL = 'https://danieltyukov.github.io/anti-brainrot/updates.xml';

  function renderGuard() {
    const id = chrome.runtime.id;
    // Chrome's own safe search policies ride along while the adult filter
    // is on: ForceYouTubeRestrict 0 is Strict, the same as the header rule.
    const f = settings.features;
    const policy = JSON.stringify({
      ExtensionInstallForcelist: [`${id};${UPDATE_URL}`],
      ...(f.adultSites && f.safeSearch ? { ForceGoogleSafeSearch: true } : {}),
      ...(f.adultSites && f.restrictYouTube ? { ForceYouTubeRestrict: 0 } : {}),
    });
    $('policy-command').textContent =
      `sudo mkdir -p /etc/opt/chrome/policies/managed\nprintf '%s\\n' '${policy}' | sudo tee /etc/opt/chrome/policies/managed/anti-brainrot.json`;
    const status = $('managed-status');
    status.textContent = 'Checking how this copy was installed.';
    try {
      chrome.management.getSelf().then((info) => {
        const managed = info.installType === 'admin' || info.mayDisable === false;
        status.classList.toggle('on', managed);
        status.textContent = managed
          ? 'Chrome reports this copy as installed by policy. It cannot be removed or turned off from Chrome.'
          : 'Chrome reports this copy as removable. Prevent removal in the popup guards the extensions page; only the policy below stops the toolbar menu as well.';
      }, () => {
        status.textContent = '';
      });
    } catch {
      status.textContent = '';
    }
  }

  function render() {
    document.documentElement.dataset.theme = settings.theme;
    renderGuard();
    renderDistractions();
    renderSchedule();
    $('lock-banner').hidden = !settings.focus.enabled;
    $('edu-enabled').checked = settings.features.educational;
    $('safe-search').checked = settings.features.safeSearch;
    $('restrict-youtube').checked = settings.features.restrictYouTube;
    renderCategories();
    if (document.activeElement !== $('channels')) $('channels').value = settings.educational.allowedChannels.join('\n');
    if (document.activeElement !== $('blocked-domains')) $('blocked-domains').value = settings.blocker.blockedDomains.join('\n');
    if (document.activeElement !== $('allowed-domains')) $('allowed-domains').value = settings.blocker.allowedDomains.join('\n');
    if (document.activeElement !== $('blocked-keywords')) $('blocked-keywords').value = settings.keywords.blocked.join('\n');
    renderDelay();
    renderTheme();
  }

  // ---------------------------------------------------------------- actions

  $('edu-enabled').addEventListener('change', (event) => {
    apply({ features: { educational: event.target.checked } });
  });
  $('safe-search').addEventListener('change', (e) => apply({ features: { safeSearch: e.target.checked } }));
  $('restrict-youtube').addEventListener('change', (e) => apply({ features: { restrictYouTube: e.target.checked } }));

  $('save-channels').addEventListener('click', () => {
    apply({ educational: { allowedChannels: E.parseChannelList($('channels').value) } });
  });

  $('save-domains').addEventListener('click', () => {
    apply({
      blocker: {
        blockedDomains: B.parseDomainList($('blocked-domains').value),
        allowedDomains: B.parseDomainList($('allowed-domains').value),
      },
    });
  });

  $('save-keywords').addEventListener('click', () => {
    apply({ keywords: { blocked: K.parseList($('blocked-keywords').value) } });
  });

  $('delay').addEventListener('change', (event) => {
    apply({ focus: { unlockDelaySec: Number(event.target.value) } });
  });

  $('save-patterns').addEventListener('click', () => {
    apply({ distractions: { custom: D.parseList($('custom-patterns').value), exceptions: D.parseList($('exceptions').value) } });
  });
  $('cooldown').addEventListener('change', (e) => apply({ distractions: { cooldownMinutes: Number(e.target.value) } }));
  $('grayscale-always').addEventListener('change', (e) => apply({ distractions: { grayscaleAlways: e.target.checked } }));
  $('reason').addEventListener('change', (e) => apply({ focus: { reason: e.target.value } }));
  $('pause-seconds').addEventListener('change', (e) => apply({ distractions: { pauseSeconds: Number(e.target.value) } }));
  $('pass-minutes').addEventListener('change', (e) => apply({ distractions: { passMinutes: Number(e.target.value) } }));
  $('daily-budget').addEventListener('change', (e) => apply({ distractions: { dailyBudgetMinutes: Number(e.target.value) } }));
  $('intention-on').addEventListener('change', (e) => apply({ distractions: { intention: e.target.checked } }));
  $('grayscale-pass').addEventListener('change', (e) => apply({ distractions: { grayscalePass: e.target.checked } }));

  $('schedule-enabled').addEventListener('change', (e) => apply({ features: { schedule: e.target.checked } }));
  $('schedule-start').addEventListener('change', (e) => apply({ schedule: { start: e.target.value } }));
  $('schedule-end').addEventListener('change', (e) => apply({ schedule: { end: e.target.value } }));

  $('export').addEventListener('click', () => {
    const blob = new Blob([JSON.stringify(settings, null, 2)], { type: 'application/json' });
    const url = URL.createObjectURL(blob);
    const a = document.createElement('a');
    a.href = url;
    a.download = 'anti-brainrot-settings.json';
    a.click();
    setTimeout(() => URL.revokeObjectURL(url), 1000);
    $('data-note').textContent = 'Exported anti-brainrot-settings.json.';
  });

  $('import').addEventListener('change', async (event) => {
    const file = event.target.files && event.target.files[0];
    if (!file) return;
    try {
      const parsed = JSON.parse(await file.text());
      const ok = await apply(S.normalize(parsed));
      $('data-note').textContent = ok ? `Imported ${file.name}.` : 'Import refused: it would loosen the filter while it is on.';
    } catch {
      status('That file is not valid JSON.', true);
    }
    event.target.value = '';
  });

  let resetArmed = null;
  $('reset').addEventListener('click', async () => {
    const button = $('reset');
    if (!resetArmed) {
      button.textContent = 'Click again to confirm reset';
      resetArmed = setTimeout(() => {
        resetArmed = null;
        button.textContent = 'Reset to defaults';
      }, 4000);
      return;
    }
    clearTimeout(resetArmed);
    resetArmed = null;
    button.textContent = 'Reset to defaults';
    const ok = await apply(S.defaults());
    $('data-note').textContent = ok ? 'Settings reset to defaults.' : 'Reset refused: it would loosen the filter while it is on.';
  });

  // ---------------------------------------------------------------- boot

  S.load().then((s) => {
    settings = s;
    render();
  });
  S.onChange((s) => {
    settings = s;
    render();
  });
})();
