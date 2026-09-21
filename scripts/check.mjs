// Consistency checks that run in CI and before a release.
import { readFileSync } from 'node:fs';
import { createHash } from 'node:crypto';

const problems = [];
const read = (p) => readFileSync(new URL('../' + p, import.meta.url), 'utf8');

const manifest = JSON.parse(read('extension/manifest.json'));
const pkg = JSON.parse(read('package.json'));

if (manifest.version !== pkg.version) {
  problems.push(`manifest version ${manifest.version} differs from package.json version ${pkg.version}`);
}
if (manifest.manifest_version !== 3) problems.push('manifest_version must be 3');
if (!manifest.key) problems.push('manifest.key is missing; the block page redirect depends on the pinned id');
if ((manifest.host_permissions || []).some((h) => h.includes('<all_urls>') || h === '*://*/*')) {
  problems.push('host_permissions must stay YouTube only; all-sites access belongs in optional_host_permissions');
}
if (!(manifest.optional_host_permissions || []).includes('<all_urls>')) {
  problems.push('optional_host_permissions must include <all_urls> for the site blocker');
}

// Pinned id derived from the public key, same way Chrome does it.
const expectedId = createHash('sha256')
  .update(Buffer.from(manifest.key, 'base64'))
  .digest('hex')
  .slice(0, 32)
  .replace(/[0-9a-f]/g, (c) => String.fromCharCode('a'.charCodeAt(0) + parseInt(c, 16)));

for (const resource of manifest.declarative_net_request.rule_resources) {
  const rules = JSON.parse(read('extension/' + resource.path));
  for (const rule of rules) {
    const sub = rule.action && rule.action.redirect && rule.action.redirect.regexSubstitution;
    if (sub && sub.startsWith('chrome-extension://') && !sub.startsWith(`chrome-extension://${expectedId}/`)) {
      problems.push(`${resource.path} rule ${rule.id} redirects to a different extension id than the manifest key implies (${expectedId})`);
    }
  }
}

// Every attribute in hide.css must exist in the registry and vice versa.
globalThis.AntiBrainrot = {};
new Function(read('extension/lib/features.js'))();
const registryAttrs = new Set(globalThis.AntiBrainrot.features.FEATURES.map((f) => f.attr).filter(Boolean));
const css = read('extension/content/hide.css');
// Only attributes placed on <html> count; element tags like
// yt-chip-cloud-chip-renderer[data-abr-shorts-chip] are set by content.js.
// The html compound selector runs to the first space outside parentheses,
// so html:is([data-abr-a], [data-abr-b]) counts both.
const cssAttrs = new Set();
function htmlCompound(line) {
  let depth = 0;
  for (let i = 0; i < line.length; i += 1) {
    const c = line[i];
    if (c === '(') depth += 1;
    else if (c === ')') depth -= 1;
    else if (c === ' ' && depth === 0) return line.slice(0, i);
  }
  return line;
}
for (const line of css.split('\n')) {
  const t = line.trim();
  if (!/^html/.test(t)) continue;
  for (const m of htmlCompound(t).matchAll(/\[data-abr-([a-z-]+)\]/g)) cssAttrs.add(m[1]);
}
for (const a of cssAttrs) if (!registryAttrs.has(a)) problems.push(`hide.css uses data-abr-${a} which is not in the feature registry`);
for (const a of registryAttrs) if (!cssAttrs.has(a)) problems.push(`feature attribute ${a} has no rule in hide.css`);

// The update manifest the policy install reads must advertise this version
// and point at this version's CRX on the release.
const updates = read('site/updates.xml');
const appid = /appid='([a-p]{32})'/.exec(updates);
const advertised = /<updatecheck[^>]*\sversion='([^']+)'/.exec(updates);
const codebase = /codebase='([^']+)'/.exec(updates);
if (!appid || appid[1] !== expectedId) problems.push(`site/updates.xml appid must be ${expectedId}`);
if (!advertised || advertised[1] !== manifest.version) problems.push(`site/updates.xml advertises ${advertised && advertised[1]} but the manifest is ${manifest.version}`);
if (!codebase || codebase[1] !== `https://github.com/danieltyukov/anti-brainrot/releases/download/v${manifest.version}/anti-brainrot-${manifest.version}.crx`) {
  problems.push('site/updates.xml codebase must point at this version\'s CRX on the GitHub release');
}
if (!(manifest.optional_permissions || []).includes('tabs')) problems.push('optional_permissions must include tabs for Prevent removal');

// Changelog must mention the version.
const changelog = read('CHANGELOG.md');
if (!changelog.includes(`## [${manifest.version}]`)) problems.push(`CHANGELOG.md has no section for ${manifest.version}`);

// No stray decorative characters in shipped text.
for (const file of ['README.md', 'CHANGELOG.md', 'PRIVACY.md', 'CONTRIBUTING.md']) {
  let text = '';
  try {
    text = read(file);
  } catch {
    continue;
  }
  if (/[–—]/.test(text)) problems.push(`${file} contains an em or en dash`);
  if (/[\u{1F300}-\u{1FAFF}\u{2600}-\u{27BF}]/u.test(text)) problems.push(`${file} contains an emoji`);
}

if (problems.length > 0) {
  console.error('check failed:');
  for (const p of problems) console.error('  - ' + p);
  process.exit(1);
}
console.log(`check ok: version ${manifest.version}, id ${expectedId}, ${registryAttrs.size} css attributes`);
