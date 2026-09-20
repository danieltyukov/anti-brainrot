#!/usr/bin/env node
// Builds extension/rules/adult.json, the static declarativeNetRequest ruleset
// behind the "Block adult sites" feature. Run with: node scripts/build-adult-list.mjs
//
// Sources (downloaded at build time, never at runtime):
//   1. Sinfonietta/hostfiles pornography list (MIT)
//   2. blocklistproject/Lists porn list (Unlicense)
//   3. Tranco top 1M, used only as a popularity filter
// Plus scripts/adult-core.txt, a hand-curated list that is always included.
//
// See docs/BLOCKLIST.md for the full description of the process.

import { inflateRawSync } from 'node:zlib';
import { execFileSync } from 'node:child_process';
import { mkdtempSync, readFileSync, rmSync, statSync, writeFileSync } from 'node:fs';
import { tmpdir } from 'node:os';
import path from 'node:path';
import { fileURLToPath } from 'node:url';

const ROOT = path.resolve(path.dirname(fileURLToPath(import.meta.url)), '..');
const CORE_FILE = path.join(ROOT, 'scripts', 'adult-core.txt');
const OUT_FILE = path.join(ROOT, 'extension', 'rules', 'adult.json');

const SOURCES = {
  sinfonietta: 'https://raw.githubusercontent.com/Sinfonietta/hostfiles/master/pornography-hosts',
  blocklistproject: 'https://raw.githubusercontent.com/blocklistproject/Lists/master/porn.txt',
  tranco: 'https://tranco-list.eu/top-1m.csv.zip',
};

const EXTENSION_ID = 'ibcicobbbpfmonjbhpmllnjgdkedneop';
const BLOCK_PAGE = `chrome-extension://${EXTENSION_ID}/blocked/blocked.html?u=\\0`;

// Total size of the requestDomains list, core list included.
const MAX_DOMAINS = 15000;
// A parent domain with at least this many blocked subdomains, while not being
// blocked itself, is treated as a hosting platform (tumblr.com, blogspot.com,
// pages.dev, free.fr). Its subdomains are not carried over by the parent's rank.
const HOSTED_LIMIT = 20;
const KEYWORDS_PER_RULE = 6;
const FETCH_TIMEOUT_MS = 120000;

// Hostname keywords. A plain string matches anywhere in the hostname. An object
// with a pattern is used verbatim (RE2 syntax). Keywords fully contained in
// another keyword (pornhub, youporn, eporner, hqporner in porn; nhentai in
// hentai) are left out because the shorter keyword already covers them.
const KEYWORDS = [
  'porn',
  'xxx',
  'xvideos',
  'xnxx',
  'xhamster',
  'hentai',
  'redtube',
  'brazzers',
  'chaturbate',
  'stripchat',
  'livejasmin',
  'bongacams',
  'onlyfans',
  'fansly',
  'rule34',
  'spankbang',
  'camsoda',
  'myfreecams',
  // \b keeps aeromexico.com, jeromes.com and the like out.
  { term: 'erome', pattern: '\\berome' },
  'motherless',
  'fapello',
  'thothub',
  'tnaflix',
  'xmoviesforyou',
  // Whole label only: beegees.com, beegfs.io, frisbeegolf and friends stay open.
  { term: 'beeg', pattern: '\\bbeeg\\b' },
  { term: 'cam4', pattern: '\\bcam4\\b' },
  'jerkmate',
];

// Benign hostnames that the keyword rules would otherwise catch. They are
// found by matching the keywords against the Tranco list. Shipped as an allow
// rule with a higher priority than the redirect rules.
const KEYWORD_EXCEPTIONS = [
  // XXXLutz furniture group
  'xxxlutz.com', 'xxxlutz.de', 'xxxlutz.at', 'xxxlutz.ch', 'xxxlutz.cz', 'xxxlutz.sk',
  'xxxlutz.hu', 'xxxlutz.ro', 'xxxlutz.se', 'xxxlutz.si', 'xxxlutz.hr', 'xxxlutz.rs',
  'xxxlutz.bg', 'xxxlutz.pl', 'xxxlesnina.hr', 'xxxlesnina.si',
  // other xxx / porn substrings
  'mixxx.org', 'xxxpowersports.com', 'plussizexxxl.gr', 'maxxxmoveis.com.br',
  'ixxxijewelrystore.nl', 'auxxxilium.tech',
  // Pornic, a town in France
  'pornic.fr', 'ville-pornic.fr', 'faiencerie-pornic.fr', 'bistrotdeshalles-pornic.fr',
  // odpornosc (Polish for immunity), "por nombre" and "por nuestra" (Spanish), Somporn (Thai name)
  'odpornosc.org.pl', 'buscardnipornombre.pe', 'pornuestralarasegura.com', 'sompornlotto.online',
];

// Domains removed from the generated list. The source lists contain them by
// mistake, or they are mainstream sites that host some adult content among
// everything else. Grouped by reason.
const ALLOW = [
  // required baseline
  'google.com', 'youtube.com', 'wikipedia.org', 'github.com', 'reddit.com', 'twitter.com',
  'x.com', 'tumblr.com', 'imgur.com', 'discord.com', 'twitch.tv', 'amazon.com', 'bbc.co.uk',
  'cloudflare.com', 'akamaihd.net', 'facebook.com', 'instagram.com', 'tiktok.com',
  'pinterest.com', 'deviantart.com', 'patreon.com',
  // more platforms with user content
  'blogspot.com', 'wordpress.com', 'medium.com', 'vimeo.com', 'dailymotion.com', 'telegram.org',
  't.me', 'pixiv.net', 'fandom.com', 'archive.org', 'flickr.com', 'weebly.com', 'wix.com',
  'neocities.org', 'github.io', 'pages.dev', 'vercel.app', 'netlify.app', 'onrender.com',
  'myshopify.com', 'itch.io', 'boosty.to', 'booth.pm', 'fanbox.cc', 'fantia.jp', 'weheartit.com',
  'photobucket.com', 'fastpic.ru', 'imageban.ru', 'radikal.ru', 'chomikuj.pl', 'k2s.cc',
  'filefactory.com', 'streamtape.com', 'disqus.com', '4chan.org', '2chan.net', 'pr0gramm.com',
  'funnyjunk.com', 'joyreactor.cc', 'reactor.cc', 'thechive.com', 'collegehumor.com',
  'fark.com', 'godlikeproductions.com', 'knuddels.de', 'spin.de', 'startpagina.nl',
  // mainstream media, shops, services and tools that the lists carry by mistake
  'xiaohongshu.com', 'character.ai', 'aidungeon.com', 'seaart.ai', 'nicovideo.jp', 'likee.video',
  'yandex.by', 'kinopoisk.ru', 'ekstrabladet.dk', 'super.cz', 'lacuarta.com', 'chicagoreader.com',
  'thestranger.com', 'breakingnews.ie', 'rb.ru', 'fishki.net', 'cosmopolitan.com',
  'refinery29.com', 'glamourmagazine.co.uk', 'fashionista.com', 'popmatters.com', 'more.com',
  'etonline.com', 'vh1.com', 'southparkstudios.com', 'contactmusic.com', 'pinkvilla.com',
  'monstersandcritics.com', 'wnycstudios.org', 'podimo.com', 'en-academic.com', 'opensea.io',
  'rarible.com', 'camscanner.com', 'dmm.com', 'suruga-ya.jp', 'yes24.com', 'hmv.co.jp',
  'cafepress.com', 'spencersonline.com', 'mob.com', 'fasthosts.co.uk', '101domain.com',
  'bigrock.com', 'meusitehostgator.com.br', 'dynu.net', 'amazonlogistics.eu', 'hidemyass.com',
  'period-calendar.com', 'aukro.cz', 'quoka.de', 'ambitionbox.com', 'modthesims.info',
  'ankama.com', 'thaiware.com', 'gmpg.org', 'use-application-dns.net', 'cda.pl',
  '1337x.to', '1377x.to', 'rutracker.net', 'rutor.info', 'rarbg.to', 'rargb.to',
  // mainstream dating apps (hookup and escort sites stay blocked)
  'tinder.com', 'gotinder.com', 'badoo.com', 'badoocdn.com', 'match.com', 'pof.com',
  'okcupid.com', 'bumble.com', 'hinge.co', 'eharmony.com', 'mamba.ru', 'taimi.com',
  'romeo.com', 'grindr.com', 'grindr.mobi', 'mingle2.com', 'afrointroductions.com',
  'elmaz.com', 'yourtango.com',
];

const SECOND_LEVEL = new Set(['co', 'com', 'net', 'org', 'gov', 'edu', 'ac']);
const DOMAIN_RE = /^(?:[a-z0-9](?:[a-z0-9-]{0,61}[a-z0-9])?\.)+[a-z0-9](?:[a-z0-9-]{0,61}[a-z0-9])?$/;
const LOCAL_NAMES = new Set([
  'localhost', 'localhost.localdomain', 'local', 'broadcasthost', 'ip6-localhost',
  'ip6-loopback', 'ip6-localnet', 'ip6-mcastprefix', 'ip6-allnodes', 'ip6-allrouters',
  'ip6-allhosts', '0.0.0.0',
]);

function fail(message) {
  console.error(`error: ${message}`);
  process.exit(1);
}

async function download(url) {
  const res = await fetch(url, {
    signal: AbortSignal.timeout(FETCH_TIMEOUT_MS),
    headers: { 'user-agent': 'anti-brainrot-blocklist-builder' },
  });
  if (!res.ok) throw new Error(`${url}: HTTP ${res.status}`);
  return Buffer.from(await res.arrayBuffer());
}

// Minimal zip reader for an archive with a single, deflate or stored, entry.
function unzipSingleEntry(buf) {
  const EOCD = 0x06054b50;
  let eocd = -1;
  for (let i = buf.length - 22; i >= Math.max(0, buf.length - 22 - 65535); i--) {
    if (buf.readUInt32LE(i) === EOCD) { eocd = i; break; }
  }
  if (eocd < 0) throw new Error('zip: end of central directory not found');
  const entries = buf.readUInt16LE(eocd + 10);
  if (entries !== 1) throw new Error(`zip: expected one entry, found ${entries}`);
  const cd = buf.readUInt32LE(eocd + 16);
  if (buf.readUInt32LE(cd) !== 0x02014b50) throw new Error('zip: bad central directory header');
  const method = buf.readUInt16LE(cd + 10);
  const compressedSize = buf.readUInt32LE(cd + 20);
  const nameLength = buf.readUInt16LE(cd + 28);
  const localOffset = buf.readUInt32LE(cd + 42);
  if (compressedSize === 0xffffffff || localOffset === 0xffffffff) throw new Error('zip: zip64 not supported');
  const name = buf.toString('utf8', cd + 46, cd + 46 + nameLength);
  if (buf.readUInt32LE(localOffset) !== 0x04034b50) throw new Error('zip: bad local file header');
  const start = localOffset + 30 + buf.readUInt16LE(localOffset + 26) + buf.readUInt16LE(localOffset + 28);
  const data = buf.subarray(start, start + compressedSize);
  if (method === 0) return { name, data };
  if (method === 8) return { name, data: inflateRawSync(data) };
  throw new Error(`zip: unsupported compression method ${method}`);
}

function unzipWithCli(buf) {
  const dir = mkdtempSync(path.join(tmpdir(), 'adult-list-'));
  try {
    const file = path.join(dir, 'archive.zip');
    writeFileSync(file, buf);
    return execFileSync('unzip', ['-p', file], { maxBuffer: 256 * 1024 * 1024 });
  } finally {
    rmSync(dir, { recursive: true, force: true });
  }
}

function unzip(buf) {
  try {
    return unzipSingleEntry(buf).data;
  } catch (err) {
    console.error(`warning: built-in zip reader failed (${err.message}), trying the unzip command`);
    return unzipWithCli(buf);
  }
}

// Returns the lowercase ASCII (punycode) form of a domain, or null when the
// entry is not a usable public domain name.
export function normalizeDomain(raw) {
  let d = String(raw || '').trim().toLowerCase();
  if (!d || LOCAL_NAMES.has(d)) return null;
  if (/^[0-9.]+$/.test(d) || d.includes(':')) return null;
  try {
    d = new URL('http://' + d).hostname;
  } catch {
    return null;
  }
  d = d.replace(/\.$/, '');
  if (!DOMAIN_RE.test(d)) return null;
  if (!d.includes('.')) return null;
  return d;
}

// Parses hosts-format text ("0.0.0.0 domain") or plain domain lists.
export function parseHosts(text) {
  const out = new Set();
  for (const rawLine of text.split('\n')) {
    let line = rawLine;
    const hash = line.indexOf('#');
    if (hash >= 0) line = line.slice(0, hash);
    line = line.trim();
    if (!line) continue;
    const parts = line.split(/\s+/);
    const candidate = /^[0-9a-f.:]+$/i.test(parts[0]) && parts.length > 1 ? parts[1] : parts[0];
    const d = normalizeDomain(candidate);
    if (d) out.add(d);
  }
  return out;
}

export function parseTranco(text) {
  const ranks = new Map();
  for (const line of text.split('\n')) {
    const comma = line.indexOf(',');
    if (comma < 0) continue;
    const rank = Number(line.slice(0, comma));
    const domain = line.slice(comma + 1).trim().toLowerCase();
    if (Number.isInteger(rank) && domain && !ranks.has(domain)) ranks.set(domain, rank);
  }
  return ranks;
}

// Approximate registrable domain: the last two labels, or three when the
// second-to-last is a known second-level label under a two-letter TLD.
export function registrableParent(domain) {
  const labels = domain.split('.');
  if (labels.length <= 2) return domain;
  const tld = labels[labels.length - 1];
  const second = labels[labels.length - 2];
  const keep = tld.length === 2 && SECOND_LEVEL.has(second) ? 3 : 2;
  return labels.slice(-keep).join('.');
}

export function parentDomains(domain) {
  const out = [];
  const labels = domain.split('.');
  for (let i = 1; i < labels.length - 1; i++) out.push(labels.slice(i).join('.'));
  return out;
}

function isCoveredBy(domain, set) {
  return parentDomains(domain).some((p) => set.has(p));
}

export function prune(domains) {
  const set = new Set(domains);
  return [...set].filter((d) => !isCoveredBy(d, set));
}

function escapeRegex(s) {
  return s.replace(/[.*+?^${}()|[\]\\]/g, '\\$&');
}

export function keywordPattern(kw) {
  return typeof kw === 'string' ? escapeRegex(kw) : kw.pattern;
}

export function keywordRules(keywords, firstId) {
  const rules = [];
  for (let i = 0; i < keywords.length; i += KEYWORDS_PER_RULE) {
    const group = keywords.slice(i, i + KEYWORDS_PER_RULE).map(keywordPattern);
    rules.push({
      id: firstId + rules.length,
      priority: 1,
      action: { type: 'redirect', redirect: { regexSubstitution: BLOCK_PAGE } },
      condition: {
        regexFilter: `^https?://[^/]*(?:${group.join('|')})[^/]*`,
        resourceTypes: ['main_frame'],
      },
    });
  }
  return rules;
}

const DOMAINS_PLACEHOLDER = '"__REQUEST_DOMAINS__"';

// JSON.stringify with two-space indentation, except that the domain list is
// packed eight per line to keep the file small.
function serialize(rules, domains) {
  const text = JSON.stringify(rules, null, 2);
  const lines = [];
  for (let i = 0; i < domains.length; i += 8) {
    lines.push('        ' + domains.slice(i, i + 8).map((d) => JSON.stringify(d)).join(', '));
  }
  return text.replace(DOMAINS_PLACEHOLDER, '[\n' + lines.join(',\n') + '\n      ]') + '\n';
}

function readCore() {
  const text = readFileSync(CORE_FILE, 'utf8');
  const out = new Set();
  for (const rawLine of text.split('\n')) {
    const line = rawLine.trim();
    if (!line || line.startsWith('#')) continue;
    const d = normalizeDomain(line);
    if (!d) fail(`invalid entry in adult-core.txt: ${JSON.stringify(rawLine)}`);
    if (d !== line) fail(`adult-core.txt entry must be written as ${d}: ${JSON.stringify(rawLine)}`);
    out.add(d);
  }
  return out;
}

async function main() {
  const core = readCore();
  const allow = new Set([...ALLOW, ...KEYWORD_EXCEPTIONS].map((d) => normalizeDomain(d) || fail(`bad allow entry ${d}`)));
  for (const d of core) if (allow.has(d)) fail(`${d} is both in adult-core.txt and on the allow list`);

  console.log('downloading sources');
  const [sinfoniettaText, blpText, trancoZip] = await Promise.all([
    download(SOURCES.sinfonietta).then((b) => b.toString('utf8')),
    download(SOURCES.blocklistproject).then((b) => b.toString('utf8')),
    download(SOURCES.tranco).catch((err) => fail(`Tranco list could not be downloaded (${err.message}). Refusing to build a list without the popularity filter.`)),
  ]).catch((err) => fail(err.message));

  let trancoText;
  try {
    trancoText = unzip(trancoZip).toString('utf8');
  } catch (err) {
    fail(`Tranco archive could not be unpacked (${err.message})`);
  }
  const tranco = parseTranco(trancoText);
  if (tranco.size < 100000) fail(`Tranco list looks truncated (${tranco.size} entries)`);

  const sinfonietta = parseHosts(sinfoniettaText);
  const blp = parseHosts(blpText);
  const raw = new Set([...sinfonietta, ...blp]);
  console.log(`raw domains: sinfonietta ${sinfonietta.size}, blocklistproject ${blp.size}, union ${raw.size}, tranco ${tranco.size}`);

  // Count blocked subdomains per parent to recognise hosting platforms.
  const perParent = new Map();
  for (const d of raw) {
    const p = registrableParent(d);
    if (p !== d) perParent.set(p, (perParent.get(p) || 0) + 1);
  }

  // Popularity filter. Exact matches keep their Tranco rank. A domain whose
  // registrable parent is in Tranco is kept too, unless the parent looks like
  // a hosting platform or is allow-listed; such entries sort after all exact
  // matches so they only fill the cap when there is room.
  const ranked = new Map();
  let viaParent = 0;
  for (const d of raw) {
    if (tranco.has(d)) {
      ranked.set(d, tranco.get(d));
      continue;
    }
    const p = registrableParent(d);
    if (p === d || !tranco.has(p) || allow.has(p)) continue;
    if (!raw.has(p) && (perParent.get(p) || 0) >= HOSTED_LIMIT) continue;
    ranked.set(d, tranco.size + tranco.get(p));
    viaParent++;
  }
  console.log(`after tranco filter: ${ranked.size} (${ranked.size - viaParent} exact, ${viaParent} via parent)`);

  // Allow list: drop the domain itself and anything under it.
  let removed = 0;
  for (const d of [...ranked.keys()]) {
    if (allow.has(d) || isCoveredBy(d, allow)) {
      ranked.delete(d);
      removed++;
    }
  }
  console.log(`after allow list: ${ranked.size} (${removed} removed)`);

  const pruned = prune([...ranked.keys(), ...core]);
  console.log(`after pruning subdomains: ${pruned.length}`);

  const selected = new Set(pruned.filter((d) => core.has(d) || isCoveredBy(d, core)));
  const candidates = pruned
    .filter((d) => !selected.has(d))
    .sort((a, b) => ranked.get(a) - ranked.get(b) || a.localeCompare(b));
  for (const d of candidates) {
    if (selected.size >= MAX_DOMAINS) break;
    selected.add(d);
  }
  const domains = [...selected].sort();
  const cut = candidates.filter((d) => !selected.has(d)).length;
  const exactRanks = candidates.filter((d) => selected.has(d) && ranked.get(d) <= tranco.size).map((d) => ranked.get(d));
  console.log(`final: ${domains.length} domains (${core.size} core, cap ${MAX_DOMAINS}, ${cut} lower-ranked candidates cut)`);
  if (exactRanks.length) console.log(`lowest tranco rank kept outside the core list: ${Math.max(...exactRanks)}`);

  const rules = [
    {
      id: 1,
      priority: 1,
      action: { type: 'redirect', redirect: { regexSubstitution: BLOCK_PAGE } },
      condition: {
        requestDomains: '__REQUEST_DOMAINS__',
        regexFilter: '^https?://.*',
        resourceTypes: ['main_frame'],
      },
    },
    ...keywordRules(KEYWORDS, 2),
  ];
  rules.push({
    id: rules.length + 1,
    priority: 2,
    action: { type: 'allow' },
    condition: {
      requestDomains: [...new Set(KEYWORD_EXCEPTIONS.map(normalizeDomain))].sort(),
      resourceTypes: ['main_frame'],
    },
  });

  writeFileSync(OUT_FILE, serialize(rules, domains));
  const kb = statSync(OUT_FILE).size / 1024;
  console.log(`wrote ${path.relative(ROOT, OUT_FILE)}: ${rules.length} rules, ${kb.toFixed(1)} KB`);
  if (kb > 500) fail('output exceeds 500 KB');
}

if (process.argv[1] && path.resolve(process.argv[1]) === fileURLToPath(import.meta.url)) {
  main().catch((err) => fail(err.stack || err.message));
}
