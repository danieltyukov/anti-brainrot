// Educational mode policy: which videos may play, decided from YouTube's own
// category metadata plus a user allow list of channels. Pure, no DOM.
(globalThis.AntiBrainrot ||= {}).education = (() => {
  'use strict';

  const CATEGORIES = Object.freeze([
    'Film & Animation',
    'Autos & Vehicles',
    'Music',
    'Pets & Animals',
    'Sports',
    'Travel & Events',
    'Gaming',
    'People & Blogs',
    'Comedy',
    'Entertainment',
    'News & Politics',
    'Howto & Style',
    'Education',
    'Science & Technology',
    'Nonprofits & Activism',
  ]);

  const DEFAULT_ALLOWED = Object.freeze(['Education', 'Science & Technology', 'Howto & Style']);

  // Accepts handles (@name), channel ids (UC...), plain names, or YouTube URLs
  // and reduces them to one lowercase comparison key.
  function normalizeChannel(entry) {
    if (typeof entry !== 'string') return '';
    let s = entry.trim();
    s = s.replace(/^https?:\/\//i, '');
    s = s.replace(/^(www\.|m\.)?youtube\.com\//i, '');
    s = s.replace(/^(channel|c|user)\//i, '');
    s = s.split(/[/?#]/)[0];
    s = s.replace(/^@/, '');
    return s.trim().toLowerCase();
  }

  function parseChannelList(text) {
    if (typeof text !== 'string') return [];
    const out = [];
    for (const raw of text.split(/[\n,]+/)) {
      const key = normalizeChannel(raw);
      if (key && !out.includes(key)) out.push(key);
    }
    return out;
  }

  function lower(s) {
    return typeof s === 'string' ? s.trim().toLowerCase() : '';
  }

  function decide(meta, educational) {
    const m = meta && typeof meta === 'object' ? meta : {};
    const policy = educational && typeof educational === 'object' ? educational : {};
    const allowedChannels = (policy.allowedChannels || []).map(normalizeChannel).filter(Boolean);
    const keys = [m.channelId, m.channelHandle, m.channelName].map(normalizeChannel).filter(Boolean);
    if (keys.some((k) => allowedChannels.includes(k))) return { allow: true, reason: 'channel' };

    const category = lower(m.category);
    if (!category) return { allow: false, reason: 'unknown' };
    const allowedCategories = (policy.allowedCategories || []).map(lower);
    if (allowedCategories.includes(category)) return { allow: true, reason: 'category' };
    return { allow: false, reason: 'blocked' };
  }

  return Object.freeze({ CATEGORIES, DEFAULT_ALLOWED, normalizeChannel, parseChannelList, decide });
})();
