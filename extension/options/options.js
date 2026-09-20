// Options page: educational mode lists, blocker lists, delay, theme, data.
(() => {
  'use strict';

  const { settings: S, education: E, blocker: B, countdown: C } = globalThis.AntiBrainrot;
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

  function render() {
    document.documentElement.dataset.theme = settings.theme;
    $('lock-banner').hidden = !settings.focus.enabled;
    $('edu-enabled').checked = settings.features.educational;
    renderCategories();
    if (document.activeElement !== $('channels')) $('channels').value = settings.educational.allowedChannels.join('\n');
    if (document.activeElement !== $('blocked-domains')) $('blocked-domains').value = settings.blocker.blockedDomains.join('\n');
    if (document.activeElement !== $('allowed-domains')) $('allowed-domains').value = settings.blocker.allowedDomains.join('\n');
    renderDelay();
    renderTheme();
  }

  // ---------------------------------------------------------------- actions

  $('edu-enabled').addEventListener('change', (event) => {
    apply({ features: { educational: event.target.checked } });
  });

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

  $('delay').addEventListener('change', (event) => {
    apply({ focus: { unlockDelaySec: Number(event.target.value) } });
  });

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
