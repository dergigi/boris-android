# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

Commit messages follow [Conventional Commits](https://www.conventionalcommits.org/).

## [Unreleased]

## [0.1.0] - 2026-08-15

### Added

- Logged-in You tab shows Reading & Display settings
- Settings sync with the webapp via the same NIP-78 event (`kind` 30078, `d` tag `com.dergigi.boris.user-settings`)
- Reader uses those settings for highlight style, colors, visibility, font, size, alignment, and link color

## [0.0.9] - 2026-08-15

### Added

- Reader shows domain, reading time, highlight count, and published date when those are known
- Public kind 9802s paint on the article: yellow for you, purple for everyone else

### Changed

- Last tab is You, not Account
- Empty Home field opens `https://dergigi.com/2023/04/04/purple-text-orange-highlights/`

## [0.0.8] - 2026-08-15

### Added

- Feed shows recent public highlights from anyone
- Hidden relay discovery via NIP-66, falling back to the known public relays

## [0.0.7] - 2026-08-15

### Added

- Bottom tabs for Home, Library, Feed, Search, and Account
- Reader stays full screen without the tab bar
- Logged-in Account uses the kind 0 profile picture on the tab

### Changed

- Home is the URL field and Read
- Amber, Bunker, and nstart.me live on Account when logged out

## [0.0.6] - 2026-08-15

### Added

- Logged-in readers can highlight selected title or body text as NIP-84 kind 9802
- Yellow marks for your own highlights on this article URL
- Sign each highlight with the stored Amber or bunker session

## [0.0.5] - 2026-08-14

### Added

- Optional bunker login on Home via NIP-46 `bunker://`
- Paste or open a bunker link, pair, and show npub after restart
- Home restyled to match the webapp login (Amber, Bunker, Read)

### Changed

- Home greeting and copy match the webapp, including yellow highlight marks

## [0.0.4] - 2026-08-14

### Added

- Optional Amber login on Home via NIP-55 `get_public_key`
- Persist pubkey hex and signer package; show npub after restart
- Sign out clears the stored pair without touching Amber
- Missing Amber points at Zapstore first, then F-Droid and GitHub

## [0.0.3] - 2026-08-14

### Added

- Image gallery can download, share, download all, and open the image URL
- Article toolbar can share the current piece

## [0.0.2] - 2026-08-14

### Added

- Article title and body text can be selected and copied
- Tapping an article image opens a zoomable gallery
- Arrow keys move to the previous and next image in the gallery

### Fixed

- Image gallery swipes left and right between images in the article
- Image gallery stays open when the device rotates

## [0.0.1] - 2026-08-14

### Added

- Native Jetpack Compose reader for regular web articles
- Home screen to paste a URL and open it
- Share and open-with intents so a URL from the browser lands in the reader
- Readable-content fetch via `r.jina.ai`, matching the Boris webapp
- Serif reading view with Source Serif 4, system light/dark colors, reading time, and a link to the original
- Empty URL field falls back to `https://www.citadel21.com/the-paranoid-wallet`
- In-article http(s) links open in the reader instead of the browser
- Fetch errors offer an Open original action that uses the system view intent

### Fixed

- Opening an article no longer crashes when creating `ReaderViewModel`

### Changed

- Application ID and namespace are now `org.dergigi.boris`
- Home URL hint shows the default Citadel21 article

[Unreleased]: https://github.com/dergigi/boris-android/compare/v0.1.0...HEAD
[0.1.0]: https://github.com/dergigi/boris-android/releases/tag/v0.1.0
[0.0.9]: https://github.com/dergigi/boris-android/releases/tag/v0.0.9
[0.0.8]: https://github.com/dergigi/boris-android/releases/tag/v0.0.8
[0.0.7]: https://github.com/dergigi/boris-android/releases/tag/v0.0.7
[0.0.6]: https://github.com/dergigi/boris-android/releases/tag/v0.0.6
[0.0.5]: https://github.com/dergigi/boris-android/releases/tag/v0.0.5
[0.0.4]: https://github.com/dergigi/boris-android/releases/tag/v0.0.4
[0.0.3]: https://github.com/dergigi/boris-android/releases/tag/v0.0.3
[0.0.2]: https://github.com/dergigi/boris-android/releases/tag/v0.0.2
[0.0.1]: https://github.com/dergigi/boris-android/releases/tag/v0.0.1
