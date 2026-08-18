# Boris (Android)

A nostr-native app for reading and highlighting. Paste a URL, share one from the browser, or open an `http`/`https`/`nostr:` link (articles, notes, `npub`/`nprofile`). You get a calm article view, your marks, swarm highlights from friends and the rest of the nostrverse, and listen with on-device TTS. Bookmarks and RSS live in the library.

No ads, no trackers, no paywalls, no subscriptions. Nostr is the backend, so your highlights travel with your npub. Pair a local relay like [Citrine](https://github.com/greenart7c3/Citrine) and keep reading in airplane mode.

Login is optional. Amber or a bunker can sign events. Boris never holds an `nsec`.

<p align="center">
  <a href="https://zapstore.dev/apps/org.dergigi.boris"><img src="docs/badges/get-it-on-zapstore.svg" alt="Get it on Zap Store" height="70"></a>
  <a href="https://github.com/dergigi/boris-android/releases"><img src="docs/badges/get-it-on-github.png" alt="Get it on GitHub" height="70"></a>
</p>

## Screenshots

### Reading

Light mode. Cream background, serif type. Marks in the article. TTS is the bar at the bottom.

<p align="center">
  <img src="screenshots/read-1.png" width="180" alt="Reading in light mode" />
  <img src="screenshots/read-2.png" width="180" alt="Orange highlight in light mode" />
  <img src="screenshots/read-3.png" width="180" alt="Listening with on-device TTS" />
  <img src="screenshots/read-4.png" width="180" alt="TTS playing in light mode" />
</p>

### Dark

Dark theme. Article with its cover, then a panel of other people's highlights on the same passage. A profile. TTS again.

<p align="center">
  <img src="screenshots/dark-1.png" width="180" alt="Article in dark mode" />
  <img src="screenshots/dark-2.png" width="180" alt="Swarm highlights in dark mode" />
  <img src="screenshots/dark-3.png" width="180" alt="Profile highlights in dark mode" />
  <img src="screenshots/dark-4.png" width="180" alt="Listening in dark mode" />
</p>

### Home

Four of the five tabs: home, library, feeds, search. You is the last one. It's on Zapstore.

<p align="center">
  <img src="screenshots/1-home.png" width="180" alt="Home" />
  <img src="screenshots/2-library.png" width="180" alt="Library" />
  <img src="screenshots/3-feeds.png" width="180" alt="Feeds" />
  <img src="screenshots/4-search.png" width="180" alt="Search" />
</p>

### Settings

The settings list, highlight colors, airplane mode, About.

<p align="center">
  <img src="screenshots/settings-1.png" width="180" alt="Settings" />
  <img src="screenshots/settings-2.png" width="180" alt="Highlight colors" />
  <img src="screenshots/settings-3.png" width="180" alt="Airplane mode" />
  <img src="screenshots/settings-4.png" width="180" alt="About" />
</p>

## Contributing

Boris is free and open source and always will be. Fork it, change it, send a pull request, or just use it.

Come say hi on [Nostr](https://njump.to/npub19802see0gnk3vjlus0dnmfdagusqtmsxpl5yfmkwn9uvnfnqylqduhr0x). [Report a bug](https://github.com/dergigi/boris-android/issues/new?template=bug_report.yml) or [suggest a feature](https://github.com/dergigi/boris-android/issues/new?template=feature_request.yml). Pull requests run lint and unit tests; same commands as under [Build](#build).

If you like the work, you can [send sats](https://dergigi.com/value/).

## Build

```bash
./gradlew :app:assembleDebug
```

Pull requests run Android Lint (debug) and JVM unit tests. Same checks locally:

```bash
./gradlew :app:lintDebug :app:testDebugUnitTest
```

Fatal lint errors fail the job. Warnings are reported and do not fail the build.

## Release build

Signing uses a local upload keystore. Put these in gitignored `local.properties`:

```properties
OEM_STORE_FILE=/absolute/path/to/upload.jks
OEM_STORE_PASSWORD=…
OEM_KEY_ALIAS=upload
OEM_KEY_PASSWORD=…
```

Then:

```bash
./gradlew :app:assembleRelease
```

## Publishing to Zapstore

Config lives in [`zapstore.yaml`](zapstore.yaml) (includes publisher `pubkey`). Releases are published with [`zsp`](https://github.com/zapstore/zsp).

1. Install `zsp` from [zsp releases](https://github.com/zapstore/zsp/releases) (or `go install github.com/zapstore/zsp@latest`).
2. Cut a GitHub release that includes the signed APK (or build `assembleRelease` locally).
3. Publish with your Nostr key for `npub1dergggklka99wwrs92yz8wdjs952h2ux2ha2ed598ngwu9w7a6fsh9xzpc`:

```bash
export SIGN_WITH='nsec1…'   # or bunker://… or browser
./scripts/zapstore-publish.sh
```

`SIGN_WITH` can also live in a gitignored `.env` file at the repo root.

The script publishes through `zapstore.yaml` (not a bare APK path) so `release_notes` from [`CHANGELOG.md`](CHANGELOG.md) are included. Use `ZSP_EXTRA_ARGS='--overwrite-release'` to replace an already-published version.

First publish links the APK signing certificate to your Nostr identity (NIP-C1) and whitelists the repo via `zapstore.yaml`.

## License

[MIT](LICENSE). Versions are in [CHANGELOG.md](CHANGELOG.md).
