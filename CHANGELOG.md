# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

Commit messages follow [Conventional Commits](https://www.conventionalcommits.org/).

## [Unreleased]

### Added

- Reader can save the current article to the library with a plus button
- Settings shows the version and git commit, with links to GitHub

### Changed

- Reader share, copy, and open sit behind a 3-dot menu

## [0.19.0] - 2026-08-15

### Added

- Reader shows a thin progress bar and percentage at the bottom

## [0.18.0] - 2026-08-15

### Added

- Feed can switch between highlights and writings, with the same nostrverse / friends / you filters

## [0.17.0] - 2026-08-15

### Added

- Library has a 👀 shelf for your kind 7 lookmarks

## [0.16.0] - 2026-08-15

### Added

- Reader turns markdown footnotes into superscripts and a notes list at the end
- Kind 1 notes show linked images inline, and a tap opens the gallery

## [0.15.0] - 2026-08-15

### Added

- Library has an info icon that explains Private, Public, and Web
- Settings can turn volume-button scrolling on or off, and set how far each press moves
- Share and Open original for notes and long-form articles use njump.to
- Reader can copy the nostr id, or the plain URL for a web article

### Changed

- Settings sits to the left of sign out on You

## [0.14.0] - 2026-08-15

### Added

- Tap a highlight in the article, or the highlight-count chip, to jump between quotes

## [0.13.0] - 2026-08-15

### Added

- Volume down pages the article down, volume up pages it up. The ringer stays put while you read

## [0.12.0] - 2026-08-15

### Added

- You shows Writings next to Highlights: the logged-in user's kind 30023 articles, opened in the reader
- Sign out is a logout icon to the left of settings, with a confirm step

### Changed

- Library app bar says Your Library
- Feed title is Highlights Feed
- You app bar no longer repeats You

## [0.11.0] - 2026-08-15

### Added

- Library shelves show bookmarked notes, not only articles and web bookmarks

### Changed

- Highlights feed filters are Hub / Group / Person again

### Fixed

- Text selection works: the article no longer jumps to the top, the selected words stay selected, and the Copy / Highlight menu follows the selection

## [0.10.0] - 2026-08-15

### Added

- Open kind 1 notes from `nostr:note1` and `nostr:nevent1` in the reader
- Share and open `nostr:` URIs so Boris shows up as a target for naddr, note, and nevent links

### Fixed

- Text selection no longer jumps the article to the top or swallows the long-press

## [0.9.0] - 2026-08-15

### Added

- Library shows private, public, and web bookmarks, the same three shelves as Amethyst

### Changed

- Feed screen title is Highlights feed
- Feed, Home, and Settings use the highlighter icon, tinted with me / friends / nostrverse colors

## [0.8.0] - 2026-08-15

### Added

- Open NIP-23 long-form articles from `nostr:naddr`, njump, and readwithboris `/a/` links
- Highlights on those articles use the article address, so they show up in Feed and You

## [0.7.1] - 2026-08-15

### Fixed

- Highlights and text selection follow the words on justified paragraphs

## [0.7.0] - 2026-08-15

### Added

- Settings shows a live reading preview for font, size, alignment, links, and highlights
- Media Display setting for full-width images in articles

## [0.6.0] - 2026-08-15

### Added

- Feed can filter highlights by nostrverse, friends, and me
- Logged-in Home shows recently highlighted by friends between you and others

### Changed

- You settings gear lives in a top app bar, with room for more actions later

## [0.5.0] - 2026-08-15

### Added

- Logged-in Home shows recently highlighted by you, then recently highlighted by others

### Changed

- Home is only recently highlighted articles; the greeting, URL field, and Read button are gone
- Logged-out You is a quieter empty state with one reader-style sample mark

## [0.4.0] - 2026-08-15

### Added

- Home shows recently highlighted articles as a horizontal row of cards
- Cards use the article title, source, and Open Graph image when those are available

## [0.3.0] - 2026-08-15

### Added

- Logged-in You tab shows your profile and highlights, like the webapp `/my/highlights`
- Settings gear in the top-right opens Theme and Reading & Display

### Changed

- Theme and reading settings moved off the You tab onto a Settings screen

## [0.2.0] - 2026-08-15

### Added

- Logged-in You tab shows Theme: light, dark, or system, plus black/midnight/charcoal and paper-white/sepia/ivory palettes
- App chrome and reader follow those theme keys from the shared NIP-78 settings event

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

[Unreleased]: https://github.com/dergigi/boris-android/compare/v0.19.0...HEAD
[0.19.0]: https://github.com/dergigi/boris-android/releases/tag/v0.19.0
[0.18.0]: https://github.com/dergigi/boris-android/releases/tag/v0.18.0
[0.17.0]: https://github.com/dergigi/boris-android/releases/tag/v0.17.0
[0.16.0]: https://github.com/dergigi/boris-android/releases/tag/v0.16.0
[0.15.0]: https://github.com/dergigi/boris-android/releases/tag/v0.15.0
[0.14.0]: https://github.com/dergigi/boris-android/releases/tag/v0.14.0
[0.13.0]: https://github.com/dergigi/boris-android/releases/tag/v0.13.0
[0.12.0]: https://github.com/dergigi/boris-android/releases/tag/v0.12.0
[0.11.0]: https://github.com/dergigi/boris-android/releases/tag/v0.11.0
[0.10.0]: https://github.com/dergigi/boris-android/releases/tag/v0.10.0
[0.9.0]: https://github.com/dergigi/boris-android/releases/tag/v0.9.0
[0.8.0]: https://github.com/dergigi/boris-android/releases/tag/v0.8.0
[0.7.1]: https://github.com/dergigi/boris-android/releases/tag/v0.7.1
[0.7.0]: https://github.com/dergigi/boris-android/releases/tag/v0.7.0
[0.6.0]: https://github.com/dergigi/boris-android/releases/tag/v0.6.0
[0.5.0]: https://github.com/dergigi/boris-android/releases/tag/v0.5.0
[0.4.0]: https://github.com/dergigi/boris-android/releases/tag/v0.4.0
[0.3.0]: https://github.com/dergigi/boris-android/releases/tag/v0.3.0
[0.2.0]: https://github.com/dergigi/boris-android/releases/tag/v0.2.0
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
