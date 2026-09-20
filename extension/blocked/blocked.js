// Block page: shows which host was blocked and offers a way back.
(() => {
  'use strict';
  const { blocker: B } = globalThis.AntiBrainrot;
  const original = B.blockedUrlFrom(location.href);
  const host = original ? B.hostOf(original) : null;
  if (host) document.getElementById('host').textContent = host.replace(/^www\./, '');
  document.getElementById('back').addEventListener('click', () => {
    if (history.length > 1) history.back();
    else window.close();
  });
})();
