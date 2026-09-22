// Block page: five views. "adult", "block", "keyword" and "guard" only
// offer a way back. "pause" runs a countdown, optionally asks for an intention, and lets
// the user continue for a few minutes if the daily budget allows and no
// cooldown is running. The countdown restarts whenever the tab is hidden so
// a background tab cannot wait it out for free.
(() => {
  'use strict';

  const { blocker: B, keywords: K, settings: S } = globalThis.AntiBrainrot;
  const $ = (id) => document.getElementById(id);

  const original = B.blockedUrlFrom(location.href);
  const kind = B.kindFrom(location.href);
  const host = original ? B.hostOf(original) : null;

  for (const el of document.querySelectorAll('.host')) {
    if (host) el.textContent = host.replace(/^www\./, '');
  }
  $('view-' + kind).hidden = false;
  document.title = kind === 'pause' ? 'Pause' : 'Blocked by Anti Brainrot';
  if (kind === 'guard') $('back').textContent = 'Leave';
  // A way to Subscriptions only makes sense when the blocked page was on
  // YouTube; on an adult site or a feed elsewhere it would be a non sequitur.
  $('subscriptions').hidden = !B.isYouTube(original);
  if (kind === 'keyword') {
    S.load().then((s) => {
      const word = K.match(s.keywords.blocked, original);
      if (word) $('keyword-word').textContent = `"${word}"`;
    });
  }

  $('back').addEventListener('click', () => {
    // The guard replaced the extensions page in place; going back would
    // only reopen it, so leave for a blank tab instead.
    if (kind === 'guard') location.replace('about:blank');
    else if (history.length > 1) history.back();
    else window.close();
  });

  function send(message) {
    return new Promise((resolve) => {
      try {
        chrome.runtime.sendMessage(message, (reply) => resolve(reply || {}));
      } catch {
        resolve({});
      }
    });
  }

  function timeText(ms) {
    const d = new Date(ms);
    return `${String(d.getHours()).padStart(2, '0')}:${String(d.getMinutes()).padStart(2, '0')}`;
  }

  let total = 10;
  let remaining = 10;
  let countdownDone = false;
  let status = null;
  let timer = null;

  function updateContinue() {
    if (!status || !status.canPass) {
      $('continue').disabled = true;
      return;
    }
    const intentionOk = !status.intention || $('intention').value.trim().length >= 3;
    $('continue').disabled = !(countdownDone && intentionOk);
  }

  function startCountdown() {
    clearInterval(timer);
    remaining = total;
    countdownDone = false;
    $('countdown').textContent = String(remaining);
    updateContinue();
    timer = setInterval(() => {
      if (document.hidden) return;
      remaining -= 1;
      $('countdown').textContent = String(Math.max(0, remaining));
      if (remaining <= 0) {
        clearInterval(timer);
        countdownDone = true;
        $('countdown').textContent = 'Ready';
        updateContinue();
      }
    }, 1000);
  }

  async function boot() {
    send({ type: 'record-block', kind });
    status = await send({ type: 'pass-status', url: original });
    if (status.reason) {
      $('reason').textContent = status.reason;
      $('reason').hidden = false;
    }
    if (kind !== 'pause') return;
    if (status.activePass && original) {
      location.replace(original);
      return;
    }
    total = Number(status.pauseSeconds) || 10;
    $('intention-wrap').hidden = !status.intention;
    $('continue').textContent = `Continue for ${status.passMinutes} minute${status.passMinutes === 1 ? '' : 's'}`;
    if (!status.on) {
      $('budget').textContent = 'Distracting sites are not being filtered right now.';
    } else if (status.cooldownUntil) {
      $('cooldown').textContent = `Cooling down. The next pass for this site opens at ${timeText(status.cooldownUntil)}.`;
      $('cooldown').hidden = false;
      $('continue').hidden = true;
      $('countdown').hidden = true;
      $('intention-wrap').hidden = true;
      $('budget').textContent = '';
      return;
    } else if (!status.canPass) {
      $('exhausted').hidden = false;
      $('continue').hidden = true;
      $('countdown').hidden = true;
      $('intention-wrap').hidden = true;
      $('budget').textContent = '';
      return;
    } else if (status.dailyBudgetMinutes >= 1440) {
      $('budget').textContent = 'No daily budget is set.';
    } else {
      $('budget').textContent = `Budget left today: ${status.budgetLeft} minutes.`;
    }
    startCountdown();
  }

  document.addEventListener('visibilitychange', () => {
    if (document.hidden && kind === 'pause' && !countdownDone && status && status.canPass) startCountdown();
  });

  $('intention').addEventListener('input', updateContinue);
  $('continue').addEventListener('click', async () => {
    $('continue').disabled = true;
    const reply = await send({ type: 'pass', url: original });
    if (reply.ok && original) {
      location.replace(original);
    } else {
      $('budget').textContent = reply.reason === 'budget'
        ? 'Your daily budget is used up.'
        : reply.reason === 'cooldown'
          ? 'This site is cooling down. Try again later.'
          : 'Could not start a pass. Is the filter on?';
    }
  });

  boot();
})();
