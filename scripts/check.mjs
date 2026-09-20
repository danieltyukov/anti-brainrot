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
const cssAttrs = new Set([...css.matchAll(/data-abr-([a-z-]+)/g)].map((m) => m[1]));
for (const a of cssAttrs) if (!registryAttrs.has(a)) problems.push(`hide.css uses data-abr-${a} which is not in the feature registry`);
for (const a of registryAttrs) if (!cssAttrs.has(a)) problems.push(`feature attribute ${a} has no rule in hide.css`);

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
