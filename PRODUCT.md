# Product

<!-- impeccable:product-schema 1 -->

## Platform

android

## Users

Someone who wants to read a long-form article on their phone without handing the app a private key. They paste a URL, share a link, or open http(s) in Boris. Optional identity (npub) comes from Amber on device or a bunker on another device.

## Product Purpose

Boris is a native Android reader. It fetches a clean markdown article and shows it for calm reading. Success is: the article is readable, selectable, and shareable, and Boris never holds an `nsec`.

## Positioning

A reading app that can know who you are without ever storing the user's key. Identity is Amber (NIP-55) or bunker (NIP-46 client). The key stays in the signer.

## Operating Context

- Home: paste a URL, optional Amber or bunker login, then Read.
- Share sheet and VIEW of http(s) open the reader; VIEW of `bunker://` fills the Home field and does not start pairing.
- Reader: Source Serif article, reading time, open original, share, image gallery.
- Companion webapp (bookmarks, highlights, nostr) lives at `/Users/gigi/Development/vibe/boris`. Android is the reading MVP plus this login. Home copy matches that webapp login.

## Capabilities and Constraints

- Reading is ungated. Login is optional chrome on Home, not a gate.
- Never paste, store, or log a user `nsec`.
- Kotlin, Jetpack Compose, no Hilt/Koin, no Room. minSdk 26.
- Versions stay `0.x.y` until 1.0.0 is asked for.
- Amber missing: Zapstore first, then F-Droid, then GitHub.
- Bunker pairing is `bunker://` only. No `nostrconnect://`, no camera QR, no `sign_event` yet.
- A bunker pairing secret is typically one-shot; reuse after sign-out often fails at the signer.

## Brand Commitments

- Name: Boris. Greeting: "Hi! I'm Boris."
- Home login copy matches the webapp, including yellow marks on "Connect your npub" and "your own highlights." Button labels on Android are Amber, Signer, and Read.
- Voice: plain, short, no hype. Do not write like typical AI.
- Visual home language follows the webapp login: zinc surfaces, indigo filled actions, outlined Signer, yellow highlight marks, nstart.me footer.

## Evidence on Hand

- Shipped reader and Home in `app/src/main/java/org/dergigi/boris/`.
- Theme tokens in `ui/theme/Color.kt`, `Type.kt`, `Theme.kt`.
- Webapp login: `/Users/gigi/Development/vibe/boris/src/components/LoginOptions.tsx` and `src/styles/components/login.css`.
- Do not invent bookmarks, highlights, testimonials, or social-client features for Android.

## Product Principles

- Reading stays first.
- Identity is stored npub, never a user key.
- Match the webapp's home words and chrome; keep Android buttons honest (Amber, Signer, Read).
- Fail closed on secrets, URIs, and missing signers.
- Keep the UI small. Do not add routes or chrome the reader does not need.
