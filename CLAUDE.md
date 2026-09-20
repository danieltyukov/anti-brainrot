# Anti-Brainrot (anti-brainrot)

Chrome MV3 extension: hides YouTube Shorts unconditionally, hides feeds and
distractions behind a friction timer, optional educational-only playback.

Read `PLAN.md` first (working plan with checkboxes), then
`docs/specs/2026-09-20-anti-brainrot-design.md` (why). Keep both current.

## Conventions

- No build step. `extension/` loads unpacked as is. Library files attach to
  `globalThis.AntiBrainrot` and are shared by popup, options, worker and content scripts.
- Tests: `npm test` (Node built-in runner over `test/*.test.js`).
  Checks: `npm run check`. Package: `npm run build`. Icons: `npm run icons`.
- Manual testing: use the chrome-devtools-ext MCP tools (`install_extension`,
  `reload_extension`, `list_pages`, `evaluate_script`, `take_screenshot`).
  That MCP runs its own automation Chrome (profile
  ~/.cache/chrome-devtools-mcp/chrome-profile). The `chrome-ext` CLI daemon is
  a different Chrome; loading there does not affect the MCP browser. Never
  drive the user's main Chrome.
- The extension id is pinned by the `key` in manifest.json:
  ibcicobbbpfmonjbhpmllnjgdkedneop. Redirect rules depend on it.
- Writing: no emojis, no em dashes or en dashes, plain direct prose. Applies to
  code comments, docs, commit messages, UI copy.
- Commits: conventional prefixes, no AI attribution or session links.
- Shorts hiding must never depend on a setting.
- Console output only with the `[anti-brainrot]` prefix and only for real problems.
