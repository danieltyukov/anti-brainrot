// Pure helpers for turning /shorts/ID URLs into /watch?v=ID URLs.
(globalThis.AntiBrainrot ||= {}).shorts = (() => {
  'use strict';

  const SHORTS_PATH = /^\/shorts\/([A-Za-z0-9_-]+)\/?$/;
  const YOUTUBE_HOST = /(^|\.)youtube\.com$/;

  function videoIdFromPath(pathname) {
    if (typeof pathname !== 'string') return null;
    const m = SHORTS_PATH.exec(pathname);
    return m ? m[1] : null;
  }

  function isShortsPath(pathname) {
    return videoIdFromPath(pathname) !== null;
  }

  function rewriteUrl(href) {
    let url;
    try {
      url = new URL(href);
    } catch {
      return null;
    }
    if (!YOUTUBE_HOST.test(url.hostname)) return null;
    const id = videoIdFromPath(url.pathname);
    if (!id) return null;
    return `${url.origin}/watch?v=${id}`;
  }

  return Object.freeze({ videoIdFromPath, isShortsPath, rewriteUrl });
})();
