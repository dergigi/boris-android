#!/usr/bin/env bash
# Publish Boris to Zapstore.
# Requires: zsp on PATH (or ~/bin/zsp), SIGN_WITH set to nsec1… / bunker://… / browser
# SIGN_WITH may also live in a gitignored .env file at the repo root.
#
# Always publishes via a zapstore.yaml-derived config so release_notes
# (CHANGELOG.md) are applied. Publishing a bare .apk skips the config and
# yields "No notes" in Zapstore.
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
cd "$ROOT"

export PATH="$HOME/bin:$PATH"

if [[ -z "${SIGN_WITH:-}" && -f "$ROOT/.env" ]]; then
  SIGN_WITH="$(python3 - <<'PY'
from pathlib import Path
for line in Path(".env").read_text().splitlines():
    if line.startswith("SIGN_WITH="):
        print(line.split("=", 1)[1].strip().strip("\"'"))
        break
PY
)"
  export SIGN_WITH
fi

if [[ -z "${SIGN_WITH:-}" ]]; then
  echo "Set SIGN_WITH to your Nostr signing method before publishing." >&2
  echo "  export SIGN_WITH='nsec1…'          # or" >&2
  echo "  export SIGN_WITH='bunker://…'      # or" >&2
  echo "  export SIGN_WITH=browser" >&2
  echo "Or put SIGN_WITH in a gitignored .env file." >&2
  exit 1
fi

if ! command -v zsp >/dev/null 2>&1; then
  echo "zsp not found. Install from https://github.com/zapstore/zsp/releases" >&2
  exit 1
fi

APK="${1:-app/build/outputs/apk/release/app-release.apk}"
if [[ ! -f "$APK" ]]; then
  echo "Release APK missing at $APK — building assembleRelease…"
  ./gradlew :app:assembleRelease
  APK="app/build/outputs/apk/release/app-release.apk"
fi
APK="$(cd "$(dirname "$APK")" && pwd)/$(basename "$APK")"

export GITHUB_TOKEN="${GITHUB_TOKEN:-$(gh auth token 2>/dev/null || true)}"

# Load keystore password for NIP-C1 linking when needed
if [[ -f local.properties ]]; then
  export KEYSTORE_PASSWORD="${KEYSTORE_PASSWORD:-$(python3 - <<'PY'
from pathlib import Path
props = {}
for line in Path("local.properties").read_text().splitlines():
    if "=" in line and not line.strip().startswith("#"):
        k, v = line.split("=", 1)
        props[k.strip()] = v.strip()
print(props.get("OEM_STORE_PASSWORD", ""))
PY
)}"
fi

P12="keystore/upload.p12"
JKS="keystore/upload.jks"
CERT="$P12"
[[ -f "$CERT" ]] || CERT="$JKS"
if [[ ! -f "$CERT" && -f local.properties ]]; then
  CERT="$(python3 - <<'PY'
from pathlib import Path
props = {}
for line in Path("local.properties").read_text().splitlines():
    if "=" in line and not line.strip().startswith("#"):
        k, v = line.split("=", 1)
        props[k.strip()] = v.strip()
print(props.get("OEM_STORE_FILE", ""))
PY
)"
fi

if [[ -n "$CERT" && -f "$CERT" && "${SKIP_CERT_LINK:-}" != "1" ]]; then
  echo "Linking APK signing certificate to Nostr identity (NIP-C1)…"
  if zsp identity --link-key "$CERT" --key-alias upload --offline 2>/dev/null | nak event \
      wss://relay.zapstore.dev wss://relay.damus.io wss://relay.primal.net; then
    echo "Identity proof published."
  else
    echo "Certificate linking skipped or already done." >&2
  fi
fi

# Keep committed zapstore.yaml metadata (including release_notes) while
# pointing at the local signed APK for this publish run.
PUBLISH_CFG="$(mktemp -t boris-zapstore.XXXXXX.yaml)"
cleanup() { rm -f "$PUBLISH_CFG"; }
trap cleanup EXIT

NOTES="$ROOT/CHANGELOG.md"
ICON="$ROOT/zapstore-icon.png"
{
  # Drop path fields so we can rewrite them as absolute paths. Relative paths
  # are resolved from the temp config directory and break.
  grep -vE '^(release_source|release_notes|icon):' zapstore.yaml || true
  echo "release_notes: $NOTES"
  echo "release_source: $APK"
  echo "icon: $ICON"
} > "$PUBLISH_CFG"

echo "Publishing to Zapstore (notes from CHANGELOG.md via zapstore.yaml)…"
echo "  APK: $APK"
echo "  notes: $NOTES"
ZSP_ARGS=(publish "$PUBLISH_CFG" --skip-preview --skip-certificate-linking)
# zsp asks for TTY confirmation; --quiet auto-yes when stdin is not a terminal.
if [[ ! -t 0 ]]; then
  ZSP_ARGS+=(--quiet)
fi
# shellcheck disable=SC2086
zsp "${ZSP_ARGS[@]}" ${ZSP_EXTRA_ARGS:-}

echo "Done. Check Zapstore for org.dergigi.boris"
