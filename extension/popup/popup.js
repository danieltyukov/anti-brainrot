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
    input.checked = feature.locked ? true : Boolean(settings.features[feature.id]);
    // Tighten any time, loosen only while off: an active feature cannot be
    // switched off while the filter is on.
    input.disabled = Boolean(feature.locked) || (settings.focus.enabled && input.checked);
    const track = document.createElement('span');
    track.className = 'track';
    label.append(input, track);

    const text = document.createElement('label');
    text.className = 'label';
    text.htmlFor = input.id;
    text.textContent = feature.label;

    row.append(label, text);
    if (feature.locked) {
      const tag = document.createElement('span');
      tag.className = 'tag';
      tag.textContent = 'always on';
      row.appendChild(tag);
    }

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
    list.classList.toggle('disabled', !settings.focus.enabled);
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
    document.body.classList.toggle('off', !on);
    $('power').title = on ? 'Turn filter off' : 'Turn filter on';
    $('power').setAttribute('aria-label', $('power').title);

    setPanel('panel-off', !on);
    setPanel('panel-countdown', on && countdown !== null);

    const note = $('delay-note');
    note.hidden = !on || countdown !== null;
    note.textContent = `Filter is on with a ${C.delayLabel(settings.focus.unlockDelaySec).toLowerCase()} unlock delay. Add restrictions any time. Removing one needs the filter off.`;

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

  async function onToggle(feature, input) {
    if (feature.locked) return;
    const row = input.closest('.row');
    if (feature.id === 'adultSites' && input.checked) {
      let granted = false;
      try {
        granted = await chrome.permissions.request({ origins: ['<all_urls>'] });
      } catch {
        granted = false;
      }
      if (!granted) {
        input.checked = false;
        showNote(row, 'Blocking sites outside YouTube needs the permission Chrome just asked for.');
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
    } else if (countdown) {
      stopCountdown();
      render();
    } else {
      beginCountdown();
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
