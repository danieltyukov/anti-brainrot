// Pure countdown state machine used by the popup's friction timer.
// The popup owns the clock; this module only turns timestamps into state.
(globalThis.AntiBrainrot ||= {}).countdown = (() => {
  'use strict';

  function start(totalMs, now = Date.now()) {
    return { status: 'counting', totalMs, startedAt: now, remainingMs: totalMs };
  }

  function tick(state, now = Date.now()) {
    if (!state || state.status !== 'counting') return state;
    const remainingMs = Math.max(0, state.totalMs - (now - state.startedAt));
    return { ...state, remainingMs, status: remainingMs === 0 ? 'done' : 'counting' };
  }

  function pad(n) {
    return String(n).padStart(2, '0');
  }

  // Rounds up so a running countdown never displays 0:00 before it is done.
  function format(ms) {
    const total = Math.ceil(Math.max(0, ms) / 1000);
    const h = Math.floor(total / 3600);
    const m = Math.floor((total % 3600) / 60);
    const s = total % 60;
    return h > 0 ? `${h}:${pad(m)}:${pad(s)}` : `${m}:${pad(s)}`;
  }

  function delayLabel(seconds) {
    if (seconds === 0) return 'Instant';
    if (seconds % 3600 === 0) {
      const h = seconds / 3600;
      return h === 1 ? '1 hour' : `${h} hours`;
    }
    if (seconds % 60 === 0) {
      const m = seconds / 60;
      return m === 1 ? '1 minute' : `${m} minutes`;
    }
    return `${seconds} seconds`;
  }

  return Object.freeze({ start, tick, format, delayLabel });
})();
