// Popup: filter switch with friction timer, delay picker, and the toggle tree.
(() => {
  'use strict';

  const { features: F, settings: S, countdown: C } = globalThis.AntiBrainrot;
  const $ = (id) => document.getElementById(id);
  const list = $('list');
  const GROUPS = [
    { id: 'youtube', title: 'YouTube' },
    { id: 'web', title: 'Everywhere' },
  ];

  let settings = null;
  let countdown = null;
  let timer = null;
  let stats = null;

  // ---------------------------------------------------------------- helpers

  function applyTheme() {
    document.documentElement.dataset.theme = settings.theme;
  }

  function setPanel(id, visible) {
    $(id).hidden = !visible;
  }

  function stopCountdown() {
    clearInterval(timer);
    timer = null;
    countdown = null;
  }

  // ---------------------------------------------------------------- rows

  function makeRow(feature, isChild) {
    const row = document.createElement('div');
    row.className = 'row' + (isChild ? ' child' : '');
    row.dataset.id = feature.id;

    const label = document.createElement('label');
    label.className = 'switch';
    const input = document.createElement('input');
    input.type = 'checkbox';
    input.id = 'f-' + feature.id;
    input.checked = Boolean(settings.features[feature.id]);
    // Tighten any time, loosen only while off: an active feature cannot be
    // switched off while the filter is on.
    input.disabled = settings.focus.enabled && input.checked;
    const track = document.createElement('span');
    track.className = 'track';
    label.append(input, track);

    const text = document.createElement('label');
    text.className = 'label';
    text.htmlFor = input.id;
    text.textContent = feature.label;

    row.append(label, text);

    if (feature.parent) {
      const parentOn = Boolean(settings.features[feature.parent]);
      const covered = feature.mode === 'when-parent-off' ? parentOn : !parentOn;
      if (covered) row.classList.add('covered');
    }

    input.addEventListener('change', () => onToggle(feature, input));
    return row;
  }

  function renderList() {
    list.replaceChildren();
    for (const group of GROUPS) {
      const roots = F.roots().filter((f) => (f.section || 'youtube') === group.id);
      if (roots.length === 0) continue;
      if (GROUPS.filter((g) => F.roots().some((f) => (f.section || 'youtube') === g.id)).length > 1) {
        const title = document.createElement('div');
        title.className = 'group-title';
        title.textContent = group.title;
        list.appendChild(title);
      }
      for (const root of roots) {
        list.appendChild(makeRow(root, false));
        for (const child of F.children(root.id)) list.appendChild(makeRow(child, true));
      }
    }
    // While the filter is off everything is editable; that is when loosening
    // changes are meant to happen.
  }

  function renderLockRow() {
    const on = settings.focus.enabled && countdown === null;
    $('lock-row').hidden = !on;
    const select = $('lock-hours');
    if (select.options.length === 0) {
      for (const h of S.LOCK_CHOICES) {
        const option = document.createElement('option');
        option.value = String(h);
        option.textContent = h === 1 ? '1 hour' : `${h} hours`;
        select.appendChild(option);
      }
      select.value = '2';
    }
  }

  function localDayKey() {
    const d = new Date();
    return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')}`;
  }

  function renderStats() {
    const line = $('stats-line');
    const s = stats && stats.day === localDayKey() ? stats : null;
    if (!s || (s.blocks === 0 && s.passes === 0)) {
      line.hidden = true;
      return;
    }
    line.hidden = false;
    line.textContent = `Today: blocked ${s.blocks} time${s.blocks === 1 ? '' : 's'}, ${s.passes} pass${s.passes === 1 ? '' : 'es'} used.`;
  }

  function renderDelaySelect() {
    const select = $('delay');
    select.replaceChildren();
    for (const seconds of S.DELAY_CHOICES) {
      const option = document.createElement('option');
      option.value = String(seconds);
      option.textContent = C.delayLabel(seconds);
      option.selected = seconds === settings.focus.unlockDelaySec;
      select.appendChild(option);
    }
  }

  function render() {
    applyTheme();
    const on = settings.focus.enabled;
    const locked = on && S.isLockedNow(settings);
    document.body.classList.toggle('off', !on);
    $('power').title = locked ? `Locked hours until ${settings.schedule.end}` : on ? 'Turn filter off' : 'Turn filter on';
    $('power').setAttribute('aria-label', $('power').title);
    $('power').disabled = locked;

    setPanel('panel-off', !on);
    setPanel('panel-countdown', on && countdown !== null);

    const note = $('delay-note');
    note.hidden = !on || countdown !== null;
    note.textContent = locked
      ? `Locked until ${S.lockedUntilText(settings)}. The filter cannot be turned off before then. Adding restrictions is still fine.`
      : `Filter is on with a ${C.delayLabel(settings.focus.unlockDelaySec).toLowerCase()} unlock delay. Add restrictions any time. Removing one needs the filter off.`;
    $('power').title = locked ? `Locked until ${S.lockedUntilText(settings)}` : $('power').title;
    const reason = $('reason-line');
    reason.hidden = !on || !settings.focus.reason;
    reason.textContent = settings.focus.reason;
    renderLockRow();
    renderStats();

    renderDelaySelect();
    renderList();
  }

  // ---------------------------------------------------------------- actions

  function showNote(row, text) {
    let note = row.nextElementSibling;
    if (!note || !note.classList.contains('note')) {
      note = document.createElement('div');
      note.className = 'note';
      row.after(note);
    }
    note.textContent = text;
  }

  const NOTES = {
    adultSites: 'Acting on sites outside YouTube needs the permission Chrome just asked for.',
    keywords: 'Acting on sites outside YouTube needs the permission Chrome just asked for.',
    distractions: 'Acting on sites outside YouTube needs the permission Chrome just asked for.',
    preventRemoval: 'Leaving the extensions page needs the tabs permission Chrome just asked for.',
  };

  async function onToggle(feature, input) {
    const row = input.closest('.row');
    const needed = F.PERMISSIONS[feature.id];
    if (needed && input.checked) {
      let granted = false;
      try {
        // The worker finishes the switch if Chrome's prompt closes the popup.
        await chrome.storage.local.set({ pendingFeature: feature.id });
        granted = await chrome.permissions.request(needed);
      } catch {
        granted = false;
      }
      await chrome.storage.local.remove('pendingFeature');
      if (!granted) {
        input.checked = false;
        showNote(row, NOTES[feature.id]);
        return;
      }
    }
    try {
      await S.update({ features: { [feature.id]: input.checked } });
    } catch (err) {
      input.checked = !input.checked;
      showNote(row, err.message);
    }
  }

  async function turnOn() {
    stopCountdown();
    await S.update({ focus: { enabled: true, unlockDelaySec: Number($('delay').value) } });
  }

  async function turnOffNow() {
    stopCountdown();
    await S.update({ focus: { enabled: false } });
  }

  function beginCountdown() {
    const totalMs = settings.focus.unlockDelaySec * 1000;
    if (totalMs === 0) {
      turnOffNow();
      return;
    }
    countdown = C.start(totalMs);
    $('time').textContent = C.format(countdown.remainingMs);
    render();
    timer = setInterval(() => {
      countdown = C.tick(countdown);
      $('time').textContent = C.format(countdown.remainingMs);
      if (countdown.status === 'done') turnOffNow();
    }, 250);
  }

  function onPower() {
    if (!settings.focus.enabled) {
      turnOn();
    } else if (S.isLockedNow(settings)) {
      return;
    } else if (countdown) {
      stopCountdown();
      render();
    } else {
      beginCountdown();
    }
  }

  async function lockForHours() {
    const hours = Number($('lock-hours').value);
    const lockUntil = Math.max(settings.focus.lockUntil, Date.now() + hours * 3600 * 1000);
    try {
      await S.update({ focus: { lockUntil } });
    } catch (err) {
      showNote($('lock-row'), err.message);
    }
  }

  function cycleTheme() {
    const order = S.THEMES;
    const next = order[(order.indexOf(settings.theme) + 1) % order.length];
    S.update({ theme: next });
  }

  // ---------------------------------------------------------------- wiring

  $('power').addEventListener('click', onPower);
  $('turn-on').addEventListener('click', turnOn);
  $('cancel').addEventListener('click', () => {
    stopCountdown();
    render();
  });
  $('theme').addEventListener('click', cycleTheme);
  $('lock').addEventListener('click', lockForHours);
  chrome.storage.onChanged.addListener((changes, area) => {
    if (area === 'local' && changes.stats) {
      stats = changes.stats.newValue || null;
      renderStats();
    }
  });
  chrome.storage.local.get('stats').then((got) => {
    stats = got.stats || null;
    if (settings) renderStats();
  });
  $('delay').addEventListener('change', () => {
    if (!settings.focus.enabled) S.update({ focus: { unlockDelaySec: Number($('delay').value) } });
  });
  $('options').addEventListener('click', (event) => {
    event.preventDefault();
    chrome.runtime.openOptionsPage();
  });

  S.load().then((s) => {
    settings = s;
    render();
  });
  S.onChange((s) => {
    settings = s;
    if (!settings.focus.enabled) stopCountdown();
    render();
  });
})();
