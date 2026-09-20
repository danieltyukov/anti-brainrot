// Isolated-world runtime for www.youtube.com.
//
// Responsibilities:
// - set data-abr-* attributes on <html> from the settings (hide.css keys on them)
// - redirect /shorts/ID to /watch?v=ID on in-page navigations (the declarative
//   rule only sees full page loads)
// - redirect the home page to the subscriptions feed when configured
// - switch YouTube autoplay off when configured
// - enforce educational mode using metadata from content/page-bridge.js
//
// Runs at document_start, so <body> may not exist yet when it starts.
(() => {
  'use strict';

  const U = globalThis.AntiBrainrot;
  const S = U.settings;
  const root = document.documentElement;
  const ATTR_PREFIX = 'data-abr-';
  const EVENT_VIDEO = 'abr:video';
  const EVENT_REQUEST = 'abr:request-video';

  let settings = null;
  let currentMeta = null;
  let blocked = false;
  let waitingForMeta = false;
  let pausedWhileWaiting = false;
  let autoplayTimer = null;
  let metaTimer = null;
  const META_TIMEOUT_MS = 8000;

  // ---------------------------------------------------------------- attributes

  function applyAttributes() {
    const want = new Set(settings ? S.activeAttributes(settings) : []);
    for (const name of root.getAttributeNames()) {
      if (name.startsWith(ATTR_PREFIX) && !want.has(name.slice(ATTR_PREFIX.length))) {
        root.removeAttribute(name);
      }
    }
    for (const attr of want) root.setAttribute(ATTR_PREFIX + attr, '');
  }

  // ---------------------------------------------------------------- navigation

  function isWatchPage() {
    return location.pathname === '/watch';
  }

  function currentVideoId() {
    return new URLSearchParams(location.search).get('v');
  }

  // Returns true when a redirect was issued.
  function redirectIfNeeded() {
    const target = U.shorts.rewriteUrl(location.href);
    if (target) {
      location.replace(target);
      return true;
    }
    if (settings && S.isActive(settings, 'redirectHome') && location.pathname === '/') {
      location.replace('/feed/subscriptions');
      return true;
    }
    return false;
  }

  // ---------------------------------------------------------------- small fixes

  // The Shorts filter chip on search results has no attribute to key on.
  function tagShortsChip() {
    for (const chip of document.querySelectorAll('ytd-search yt-chip-cloud-chip-renderer')) {
      if (chip.textContent.trim() === 'Shorts') chip.setAttribute('data-abr-shorts-chip', '');
    }
  }

  // YouTube prefixes the tab title with the unread count, for example "(3) ".
  const titlePrefix = /^\(\d+\)\s*/;
  function stripTitleCount() {
    if (!settings || !S.isActive(settings, 'notifications')) return;
    if (titlePrefix.test(document.title)) document.title = document.title.replace(titlePrefix, '');
  }

  function watchTitle() {
    const title = document.querySelector('title');
    if (!title) return;
    new MutationObserver(stripTitleCount).observe(title, { childList: true, characterData: true, subtree: true });
    stripTitleCount();
  }

  // Send the logo to Subscriptions before YouTube's router sees the click.
  function onLogoClick(event) {
    if (!settings || !S.isActive(settings, 'redirectHome')) return;
    const link = event.target && event.target.closest && event.target.closest('a#logo, ytd-topbar-logo-renderer a');
    if (!link) return;
    event.preventDefault();
    event.stopPropagation();
    location.assign('/feed/subscriptions');
  }

  // ---------------------------------------------------------------- autoplay

  function fixAutoplay() {
    clearInterval(autoplayTimer);
    if (!settings || !S.isActive(settings, 'autoplay') || !isWatchPage()) return;
    let tries = 0;
    autoplayTimer = setInterval(() => {
      tries += 1;
      const button = document.querySelector('.ytp-autonav-toggle-button');
      if (button) {
        if (button.getAttribute('aria-checked') === 'true') button.click();
        clearInterval(autoplayTimer);
      } else if (tries > 40) {
        clearInterval(autoplayTimer);
      }
    }, 250);
  }

  // ---------------------------------------------------------------- education

  function requestMeta() {
    document.dispatchEvent(new CustomEvent(EVENT_REQUEST));
  }

  function mainVideo() {
    return document.querySelector('video.html5-main-video') || document.querySelector('#movie_player video');
  }

  function pauseVideo() {
    const video = mainVideo();
    if (video && !video.paused) {
      video.pause();
      return true;
    }
    return false;
  }

  // Resumes a video that was only paused while the category was unknown.
  function resumeIfWePaused() {
    if (!pausedWhileWaiting) return;
    pausedWhileWaiting = false;
    const video = mainVideo();
    if (video && video.paused) video.play().catch(() => {});
  }

  function overlayElement() {
    return document.getElementById('abr-block');
  }

  function goBack() {
    if (history.length > 1) history.back();
    else location.assign('/feed/subscriptions');
  }

  function renderBlock(meta, decision) {
    let overlay = overlayElement();
    if (!overlay) {
      overlay = document.createElement('div');
      overlay.id = 'abr-block';
      overlay.setAttribute('role', 'dialog');
      overlay.setAttribute('aria-modal', 'true');
      (document.body || root).appendChild(overlay);
    }
    const allowed = settings.educational.allowedCategories.join(', ') || 'nothing yet';
    const title = meta.title ? `"${meta.title}"` : 'This video';

    overlay.replaceChildren();
    const card = document.createElement('div');
    card.className = 'abr-card';

    const brand = document.createElement('div');
    brand.className = 'abr-brand';
    brand.textContent = 'anti-brainrot';

    const heading = document.createElement('h1');
    heading.textContent = 'Not on your list';

    // Text nodes only: the title and category come from page data.
    const p1 = document.createElement('p');
    if (decision.reason === 'unknown') {
      p1.textContent = `${title} has no category Anti-Brainrot can read, so it stays blocked.`;
    } else {
      const bold = document.createElement('b');
      bold.textContent = meta.category;
      p1.append(document.createTextNode(`${title} is filed under `), bold, document.createTextNode('.'));
    }

    const p2 = document.createElement('p');
    p2.textContent = `Educational mode only plays: ${allowed}.`;

    const actions = document.createElement('div');
    actions.className = 'abr-actions';
    const back = document.createElement('button');
    back.type = 'button';
    back.textContent = 'Back';
    back.addEventListener('click', goBack);
    const subs = document.createElement('a');
    subs.className = 'abr-button';
    subs.href = '/feed/subscriptions';
    subs.textContent = 'Subscriptions';
    actions.append(back, subs);

    const hint = document.createElement('p');
    hint.className = 'abr-hint';
    hint.textContent = 'Allowed categories and channels can be changed in Anti-Brainrot options.';

    card.append(brand, heading, p1, p2, actions, hint);
    overlay.appendChild(card);
  }

  function block(meta, decision) {
    blocked = true;
    pausedWhileWaiting = false;
    renderBlock(meta, decision);
    pauseVideo();
  }

  function unblock() {
    blocked = false;
    const overlay = overlayElement();
    if (overlay) overlay.remove();
  }

  function evaluateEducation() {
    if (!settings || !S.isActive(settings, 'educational') || !isWatchPage()) {
      waitingForMeta = false;
      unblock();
      return;
    }
    const id = currentVideoId();
    if (!currentMeta || currentMeta.videoId !== id) {
      // Fail closed while we wait for metadata: pause now, keep pausing
      // through the play guard below, and ask the bridge. If the bridge never
      // answers, show the block screen rather than a silent paused player.
      waitingForMeta = true;
      if (pauseVideo()) pausedWhileWaiting = true;
      requestMeta();
      clearTimeout(metaTimer);
      metaTimer = setTimeout(() => {
        if (!settings || !S.isActive(settings, 'educational') || !isWatchPage()) return;
        if (currentMeta && currentMeta.videoId === currentVideoId()) return;
        block({ videoId: currentVideoId(), title: '', category: '' }, { allow: false, reason: 'unknown' });
      }, META_TIMEOUT_MS);
      return;
    }
    clearTimeout(metaTimer);
    waitingForMeta = false;
    const decision = U.education.decide(currentMeta, settings.educational);
    if (decision.allow) {
      unblock();
      resumeIfWePaused();
    } else {
      block(currentMeta, decision);
    }
  }

  function onVideoMeta(event) {
    let meta = null;
    try {
      meta = JSON.parse(event.detail);
    } catch {
      meta = null;
    }
    if (!meta || !meta.videoId) return;
    if (!meta.channelHandle) {
      const owner = document.querySelector('ytd-watch-metadata #owner a[href^="/@"]');
      if (owner) meta.channelHandle = owner.getAttribute('href').slice(1);
    }
    currentMeta = meta;
    evaluateEducation();
  }

  // Keep a blocked video paused even if YouTube or the user presses play, and
  // keep an undecided one paused while its metadata is on the way.
  document.addEventListener('play', (event) => {
    if (!event.target || event.target.tagName !== 'VIDEO') return;
    if (blocked) {
      event.target.pause();
    } else if (waitingForMeta) {
      event.target.pause();
      pausedWhileWaiting = true;
    }
  }, true);

  // ---------------------------------------------------------------- wiring

  function onNavigateStart() {
    unblock();
    clearTimeout(metaTimer);
    currentMeta = null;
    pausedWhileWaiting = false;
    // Closed until the next page has been judged.
    waitingForMeta = Boolean(settings && S.isActive(settings, 'educational'));
    redirectIfNeeded();
  }

  function onNavigateFinish() {
    if (redirectIfNeeded()) return;
    fixAutoplay();
    evaluateEducation();
    tagShortsChip();
    stripTitleCount();
    setTimeout(tagShortsChip, 1500);
  }

  function onSettings(next) {
    settings = next;
    applyAttributes();
    if (redirectIfNeeded()) return;
    fixAutoplay();
    evaluateEducation();
  }

  // Shorts can be redirected before settings arrive; nothing else can.
  if (U.shorts.rewriteUrl(location.href)) {
    location.replace(U.shorts.rewriteUrl(location.href));
    return;
  }

  document.addEventListener(EVENT_VIDEO, onVideoMeta);
  document.addEventListener('yt-navigate-start', onNavigateStart);
  document.addEventListener('yt-navigate-finish', onNavigateFinish);
  document.addEventListener('click', onLogoClick, true);
  window.addEventListener('popstate', () => redirectIfNeeded());
  if (document.readyState === 'loading') document.addEventListener('DOMContentLoaded', watchTitle);
  else watchTitle();

  S.load().then(onSettings);
  S.onChange(onSettings);
})();
