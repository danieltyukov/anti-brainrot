// Feature registry. The order here is the display order in the popup.
//
// id      stable key used in settings.features
// label   popup text
// default initial value
// attr    attribute name set on <html> as data-abr-<attr> (null = no CSS)
// parent  id of the parent row
// mode    'when-parent-off': child only matters when the parent is off
//         'when-parent-on':  child only matters when the parent is on
// locked  always on, not editable (Shorts)
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
    { id: 'shorts', label: 'Hide Shorts', default: true, attr: null, locked: true },
    { id: 'comments', label: 'Hide Comments', default: true, attr: 'comments' },
    { id: 'mixes', label: 'Hide Mixes', default: true, attr: 'mixes' },
    { id: 'merch', label: 'Hide Merch, Tickets, Offers', default: true, attr: 'merch' },
    { id: 'videoInfo', label: 'Hide Video Info', default: false, attr: 'video-info' },
    { id: 'topHeader', label: 'Hide Top Header', default: false, attr: 'top-header' },
    { id: 'notifications', label: 'Hide Notifications', default: true, attr: 'notifications', parent: 'topHeader', mode: 'when-parent-off' },
    { id: 'inaptSearch', label: 'Hide Inapt Search Results', default: true, attr: 'inapt-search' },
    { id: 'explore', label: 'Hide Explore, Trending', default: true, attr: 'explore' },
    { id: 'moreFromYouTube', label: 'Hide More from YouTube', default: true, attr: 'more-from-youtube' },
    { id: 'subscriptions', label: 'Hide Subscriptions', default: false, attr: 'subscriptions' },
    { id: 'autoplay', label: 'Disable Autoplay', default: true, attr: null },
    { id: 'annotations', label: 'Disable Annotations', default: true, attr: 'annotations' },
    { id: 'educational', label: 'Educational videos only', default: false, attr: 'educational' },
  ];

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

  return Object.freeze({ FEATURES: Object.freeze(FEATURES), byId, roots, children });
})();
