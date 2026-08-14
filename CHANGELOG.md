# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

Commit messages follow [Conventional Commits](https://www.conventionalcommits.org/).

## [Unreleased]

### Added

- Article title and body text can be selected and copied
- Tapping an article image opens a zoomable gallery

### Fixed

- Image gallery swipes left and right between images in the article

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

[Unreleased]: https://github.com/dergigi/boris-android/compare/v0.0.1...HEAD
[0.0.1]: https://github.com/dergigi/boris-android/releases/tag/v0.0.1
