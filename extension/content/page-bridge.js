// Main-world bridge for www.youtube.com.
//
// Content scripts live in an isolated world and cannot read YouTube's page
// objects. This script runs in the page's own world, reads the player
// response for the current watch page, and hands the few fields the extension
// needs to the isolated world through a DOM event with a JSON string payload.
// It never touches settings and never modifies the page.
(() => {
  'use strict';

  const EVENT_VIDEO = 'abr:video';
  const EVENT_REQUEST = 'abr:request-video';

  function currentVideoId() {
    return new URLSearchParams(location.search).get('v');
  }

  function fromPlayerResponse(pr) {
    if (!pr || typeof pr !== 'object') return null;
    const details = pr.videoDetails || {};
    const micro = (pr.microformat && pr.microformat.playerMicroformatRenderer) || {};
    const videoId = details.videoId;
    if (!videoId) return null;
    return {
      videoId,
      title: details.title || (micro.title && micro.title.simpleText) || '',
      category: micro.category || '',
      channelId: details.channelId || micro.externalChannelId || '',
      channelName: details.author || micro.ownerChannelName || '',
      channelHandle: '',
    };
  }

  function fromEventDetail(detail) {
    if (!detail || typeof detail !== 'object') return null;
    const response = detail.response || detail.pageData || {};
    return fromPlayerResponse(response.playerResponse) || fromPlayerResponse(detail.playerResponse);
  }

  function fromPageManager() {
    try {
      const pm = document.querySelector('ytd-page-manager');
      const data = pm && typeof pm.getCurrentData === 'function' ? pm.getCurrentData() : null;
      return fromPlayerResponse(data && data.playerResponse);
    } catch {
      return null;
    }
  }

  function fromWatchFlexy() {
    try {
      const flexy = document.querySelector('ytd-watch-flexy');
      if (!flexy) return null;
      const pr = flexy.playerResponse
        || (flexy.__data && flexy.__data.playerResponse)
        || (flexy.data && flexy.data.playerResponse);
      return fromPlayerResponse(pr);
    } catch {
      return null;
    }
  }

  function fromInitial() {
    try {
      return fromPlayerResponse(window.ytInitialPlayerResponse);
    } catch {
      return null;
    }
  }

  // Extracts the JSON object that follows `ytInitialPlayerResponse =` in a
  // watch page's HTML by matching braces while skipping string contents.
  function extractInitialPlayerResponse(html) {
    const marker = 'ytInitialPlayerResponse = ';
    const start = html.indexOf(marker);
    if (start < 0) return null;
    let i = start + marker.length;
    if (html[i] !== '{') return null;
    let depth = 0;
    let inString = false;
    for (let j = i; j < html.length; j += 1) {
      const ch = html[j];
      if (inString) {
        if (ch === '\\') j += 1;
        else if (ch === '"') inString = false;
      } else if (ch === '"') {
        inString = true;
      } else if (ch === '{') {
        depth += 1;
      } else if (ch === '}') {
        depth -= 1;
        if (depth === 0) {
          try {
            return JSON.parse(html.slice(i, j + 1));
          } catch {
            return null;
          }
        }
      }
    }
    return null;
  }

  async function fromFetch(videoId) {
    try {
      const res = await fetch(`/watch?v=${encodeURIComponent(videoId)}`, { credentials: 'include' });
      const html = await res.text();
      return fromPlayerResponse(extractInitialPlayerResponse(html));
    } catch {
      return null;
    }
  }

  function emit(meta) {
    document.dispatchEvent(new CustomEvent(EVENT_VIDEO, { detail: JSON.stringify(meta) }));
  }

  function sleep(ms) {
    return new Promise((resolve) => setTimeout(resolve, ms));
  }

  function matches(meta, id) {
    return meta && meta.videoId === id;
  }

  async function resolveAndEmit(detail) {
    const id = currentVideoId();
    if (location.pathname !== '/watch' || !id) return;

    let meta = fromEventDetail(detail);
    if (!matches(meta, id)) meta = fromPageManager();
    if (!matches(meta, id)) meta = fromWatchFlexy();
    if (!matches(meta, id)) meta = fromInitial();

    // The SPA fills its data a moment after navigation. Poll briefly before
    // falling back to a network fetch of the watch page.
    for (let i = 0; i < 10 && !matches(meta, id); i += 1) {
      await sleep(200);
      if (currentVideoId() !== id) return;
      meta = fromPageManager() || fromWatchFlexy();
    }
    if (!matches(meta, id)) meta = await fromFetch(id);
    if (currentVideoId() !== id) return;

    if (!matches(meta, id)) {
      meta = { videoId: id, title: '', category: '', channelId: '', channelName: '', channelHandle: '' };
    }
    emit(meta);
  }

  document.addEventListener('yt-navigate-finish', (event) => {
    resolveAndEmit(event.detail);
  });
  document.addEventListener(EVENT_REQUEST, () => {
    resolveAndEmit(null);
  });
  if (document.readyState === 'loading') {
    document.addEventListener('DOMContentLoaded', () => resolveAndEmit(null));
  } else {
    resolveAndEmit(null);
  }
})();
