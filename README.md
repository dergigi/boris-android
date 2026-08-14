# Boris (Android)

A calm native reader for web articles. Paste a URL, share one from the browser, or open an `http`/`https` link with Boris.

Versions follow [Semantic Versioning](https://semver.org/spec/v2.0.0.html). Notable changes are listed in [`CHANGELOG.md`](CHANGELOG.md) using [Keep a Changelog](https://keepachangelog.com/en/1.0.0/).

```bash
./gradlew :app:assembleDebug
```

Companion webapp: [readwithboris.com](https://readwithboris.com/).

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
