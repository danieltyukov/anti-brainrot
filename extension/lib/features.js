// Feature registry. The order here is the display order in the popup.
//
// id      stable key used in settings.features
// label   popup text
// default initial value
// attr    attribute name set on <html> as data-abr-<attr> (null = no CSS)
// parent  id of the parent row
// mode    'when-parent-off': child only matters when the parent is off
//         'when-parent-on':  child only matters when the parent is on
// section 'youtube' (default) or 'web' (applies outside YouTube)
//
// Every feature is a setting. Nothing is locked on; defaults are the only
// opinion the extension has.
(globalThis.AntiBrainrot ||= {}).features = (() => {
  'use strict';

  const FEATURES = [
    { id: 'homeFeed', label: 'Hide Home Feed', default: true, attr: 'home-feed' },
    { id: 'redirectHome', label: 'Redirect to Subscriptions', default: true, attr: null, parent: 'homeFeed', mode: 'when-parent-on' },
    { id: 'sidebar', label: 'Hide Video Sidebar', default: false, attr: 'sidebar' },
    { id: 'sidebarRecommended', label: 'Hide Recommended', default: true, attr: 'sidebar-recommended', parent: 'sidebar', mode: 'when-parent-off' },
    { id: 'liveChat', label: 'Hide Live Chat', default: true, attr: 'live-chat', parent: 'sidebar', mode: 'when-parent-off' },
    { id: 'playlist', label: 'Hide Playlist', default: false, attr: 'playlist', parent: 'sidebar', mode: 'when-parent-off' },
    { id: 'fundraiser', label: 'Hide Fundraiser', default: true, attr: 'fundraiser', parent: 'sidebar', mode: 'when-parent-off' },
    { id: 'endScreenFeed', label: 'Hide End Screen Feed', default: true, attr: 'end-screen-feed' },
    { id: 'endScreenCards', label: 'Hide End Screen Cards', default: true, attr: 'end-screen-cards' },
    { id: 'shorts', label: 'Hide Shorts', default: true, attr: 'shorts' },
    { id: 'comments', label: 'Hide Comments', default: true, attr: 'comments' },
    { id: 'commentAvatars', label: 'Hide Profile Photos', default: false, attr: 'comment-avatars', parent: 'comments', mode: 'when-parent-off' },
    { id: 'mixes', label: 'Hide Mixes', default: true, attr: 'mixes' },
    { id: 'merch', label: 'Hide Merch, Tickets, Offers', default: true, attr: 'merch' },
    { id: 'videoInfo', label: 'Hide Video Info', default: false, attr: 'video-info' },
    { id: 'videoButtons', label: 'Hide Buttons Bar', default: false, attr: 'video-buttons', parent: 'videoInfo', mode: 'when-parent-off' },
    { id: 'videoChannel', label: 'Hide Channel', default: false, attr: 'video-channel', parent: 'videoInfo', mode: 'when-parent-off' },
    { id: 'videoDescription', label: 'Hide Description', default: false, attr: 'video-description', parent: 'videoInfo', mode: 'when-parent-off' },
    { id: 'topHeader', label: 'Hide Top Header', default: false, attr: 'top-header' },
    { id: 'notifications', label: 'Hide Notifications', default: true, attr: 'notifications', parent: 'topHeader', mode: 'when-parent-off' },
    { id: 'inaptSearch', label: 'Hide Inapt Search Results', default: true, attr: 'inapt-search' },
    { id: 'explore', label: 'Hide Explore, Trending', default: true, attr: 'explore' },
    { id: 'moreFromYouTube', label: 'Hide More from YouTube', default: true, attr: 'more-from-youtube' },
    { id: 'subscriptions', label: 'Hide Subscriptions', default: false, attr: 'subscriptions' },
    { id: 'history', label: 'Hide History', default: false, attr: 'history' },
    { id: 'autoplay', label: 'Disable Autoplay', default: true, attr: 'autoplay' },
    { id: 'annotations', label: 'Disable Annotations', default: true, attr: 'annotations' },
    { id: 'thumbnails', label: 'Hide Thumbnails', default: false, attr: 'thumbnails' },
    { id: 'thumbnailsBlur', label: 'Blur Thumbnails', default: false, attr: 'thumbnails-blur', parent: 'thumbnails', mode: 'when-parent-off' },
    { id: 'metrics', label: 'Hide View Counts, Likes, Durations', default: false, attr: 'metrics' },
    { id: 'chips', label: 'Hide Filter Chips', default: true, attr: 'chips' },
    { id: 'richSections', label: 'Hide Posts, News, Games Shelves', default: true, attr: 'rich-sections' },
    { id: 'searchSuggestions', label: 'Hide Search Suggestions', default: false, attr: 'search-suggestions' },
    { id: 'grayscale', label: 'Grayscale YouTube', default: false, attr: 'grayscale' },
    { id: 'educational', label: 'Educational videos only', default: false, attr: 'educational' },
    { id: 'adultSites', label: 'Block adult sites', default: false, attr: null, section: 'web' },
    { id: 'distractions', label: 'Block distracting sites', default: false, attr: null, section: 'web' },
    { id: 'schedule', label: 'Locked hours', default: false, attr: null, section: 'web' },
    { id: 'preventRemoval', label: 'Prevent removal', default: false, attr: null, section: 'web' },
  ];

  // Optional permissions a feature needs before it can be switched on. The
  // popup requests them on the click; the worker finishes the switch.
  const PERMISSIONS = Object.freeze({
    adultSites: { origins: ['<all_urls>'] },
    distractions: { origins: ['<all_urls>'] },
    preventRemoval: { permissions: ['tabs'] },
  });

  const byIdMap = new Map(FEATURES.map((f) => [f.id, f]));

  function byId(id) {
    return byIdMap.get(id);
  }

  function roots() {
    return FEATURES.filter((f) => !f.parent);
  }

  function children(parentId) {
    return FEATURES.filter((f) => f.parent === parentId);
  }

  return Object.freeze({ FEATURES: Object.freeze(FEATURES), PERMISSIONS, byId, roots, children });
})();
