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

Light mode. Articles open as a serif page with source, read time, and how many marks are on the piece. Your highlights sit in the text. The player at the bottom reads from the current sentence, with speed on the bar.

<p align="center">
  <img src="screenshots/read-1.png" width="180" alt="Reading in light mode" />
  <img src="screenshots/read-2.png" width="180" alt="Orange highlight in light mode" />
  <img src="screenshots/read-3.png" width="180" alt="Listening with on-device TTS" />
  <img src="screenshots/read-4.png" width="180" alt="TTS playing in light mode" />
</p>

### Dark

Same reader at night. Covers stay with the article. A side panel shows friends' marks on the passage you are in. Open someone's npub to search their highlights, or keep listening with follow-along in the dark theme.

<p align="center">
  <img src="screenshots/dark-1.png" width="180" alt="Article in dark mode" />
  <img src="screenshots/dark-2.png" width="180" alt="Swarm highlights in dark mode" />
  <img src="screenshots/dark-3.png" width="180" alt="Profile highlights in dark mode" />
  <img src="screenshots/dark-4.png" width="180" alt="Listening in dark mode" />
</p>

### Home

The five tabs. Home is what people around you marked lately. Library is bookmarks, private and public. Feeds is writings and highlights from Nostrverse, friends, or you. Search looks through quote text. You is your own profile; it is on Zapstore, not in this row.

<p align="center">
  <img src="screenshots/1-home.png" width="180" alt="Home" />
  <img src="screenshots/2-library.png" width="180" alt="Library" />
  <img src="screenshots/3-feeds.png" width="180" alt="Feeds" />
  <img src="screenshots/4-search.png" width="180" alt="Search" />
</p>

### Settings

Theme, type, TTS, and highlight colors, including separate palettes for you, friends, and the rest of nostr. Airplane mode caches articles and talks to a local relay. About has the vision, support, and source.

<p align="center">
  <img src="screenshots/settings-1.png" width="180" alt="Settings" />
  <img src="screenshots/settings-2.png" width="180" alt="Highlight colors" />
  <img src="screenshots/settings-3.png" width="180" alt="Airplane mode" />
  <img src="screenshots/settings-4.png" width="180" alt="About" />
</p>

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

[MIT](LICENSE)
