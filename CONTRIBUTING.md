# Contributing

Thanks for helping. This project is small on purpose. Keep it that way.

## Ground rules

- No build step and no runtime dependencies. `extension/` loads unpacked as
  is. Shared code is plain scripts that attach to `globalThis.AntiBrainrot`.
- Shorts hiding never depends on a setting.
- The rule "tighten any time, loosen only while off" applies to every new
  setting. If you add one, extend `settings.isLoosening` and its tests.
- Plain prose in docs and UI copy. No emojis, no em dashes or en dashes.
- Console output only with the `[anti-brainrot]` prefix and only for real
  problems.

## Running things

```
npm test          # unit tests
npm run check     # consistency checks (run before every PR)
npm run build     # zip for release
```

Load `extension/` through `chrome://extensions` with Developer mode on. After
changing files, press the reload icon on the extension card, then reload the
YouTube tab (content scripts of an old version do not reconnect).

## Fixing a selector

YouTube renames elements often. When something reappears:

1. Open the page, inspect the element, and find the outermost YouTube
   custom element that wraps it (`ytd-...`, `yt-...`, `ytm-...` or a
   `...-view-model`).
2. Add it to the matching block in `extension/content/hide.css`. Keep the
   attribute prefix `html[data-abr-<feature>]`. Shorts rules carry no
   attribute.
3. Verify on the live page, and add a line to `docs/E2E.md` with the date.

Prefer `:has()` with stable hrefs (`a[href^="/shorts/"]`, `a[href*="list=RD"]`)
over class names, which change more often.

## Adding a feature toggle

1. Add it to `FEATURES` in `extension/lib/features.js` (label, default,
   attr, optional parent and mode).
2. Add the CSS block, or the JS in `content.js` if it needs behaviour.
3. Update the tests in `test/features.test.js` and `test/settings.test.js`
   (root order, default attributes).
4. Run `npm run check`: it fails if a CSS attribute and the registry drift.

## Pull requests

- One change per PR, with tests for anything in `extension/lib/`.
- Conventional commit prefixes: `feat:`, `fix:`, `docs:`, `test:`, `chore:`,
  `ci:`.
- Add a line under Unreleased in `CHANGELOG.md`.
