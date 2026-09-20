# Unrot (yt-anti-brain-rot)

Chrome MV3 extension: hides YouTube Shorts unconditionally, hides feeds and
distractions behind a friction timer, optional educational-only playback.

Read `PLAN.md` first (working plan with checkboxes), then
`docs/specs/2026-09-20-unrot-design.md` (why). Keep both current.

## Conventions

- No build step. `extension/` loads unpacked as is. Library files attach to
  `globalThis.Unrot` and are shared by popup, options, worker and content scripts.
- Tests: `npm test` (Node built-in runner over `test/*.test.js`).
  Checks: `npm run check`. Package: `npm run build`. Icons: `npm run icons`.
- Load for manual testing with `chrome-ext load extension` (dev Chrome
  profile) or the chrome-devtools-ext MCP `install_extension` tool. Never
  drive the user's main Chrome.
- Writing: no emojis, no em dashes or en dashes, plain direct prose. Applies to
  code comments, docs, commit messages, UI copy.
- Commits: conventional prefixes, no AI attribution or session links.
- Shorts hiding must never depend on a setting.
- Console output only with the `[unrot]` prefix and only for real problems.
