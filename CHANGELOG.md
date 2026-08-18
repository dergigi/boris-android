# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

Commit messages follow [Conventional Commits](https://www.conventionalcommits.org/).

## [Unreleased]

### Fixed

- Reader TTS bottom chrome no longer leaves article text visible through the navigation bar inset

## [1.4.2] - 2026-08-18

### Changed

- Slim TTS player in the reader sits above the reading progress bar and uses the same translucent background

## [1.4.1] - 2026-08-18

### Added

- Lock-screen TTS playback now shows the current article cover as media artwork when one is available
- The slim TTS player now stays available at the bottom of the article reader

### Fixed

- TTS skips hidden Markdown reference link definitions instead of reading their URLs at the end of an article

## [1.4.0] - 2026-08-18

### Added

- Listen to articles with on-device text-to-speech: play/pause from the reader top bar
- Background playback with a media notification and lock-screen controls (play, pause, stop, paragraph skip)
- Follow-along: the spoken paragraph is highlighted and kept on screen; scrolling by hand pauses auto-scroll, not speech
- Mini player while browsing other screens: title, play/pause, paragraph skip, and speed cycle; tap the title to return to the article
- Text-to-Speech settings matching the webapp: playback speed, speaker language, follow-along toggle, and a preview sentence with play
- Speed and language sync with the webapp via Nostr settings (`ttsDefaultSpeed`, `ttsLanguageMode`)
- Playback starts near the saved reading position and one article speaks at a time; playing another article switches over
- While speaking, volume keys change volume instead of scrolling the article
- If no TTS voice is installed, an error message links to system TTS settings; reading is never blocked

## [1.3.3] - 2026-08-18

### Added

- Settings About includes Report a bug and Suggest a feature (same GitHub issue templates as About Boris)

### Fixed

- Report a bug and Suggest a feature open GitHub issue templates in the browser instead of the reader
- External https links include `CATEGORY_BROWSABLE` so installed apps (GitHub) can claim them

## [1.3.2] - 2026-08-18

### Changed

- Reader highlight pane lists highlights in article order (first on the page at the top)
- Settings About "Vision" always opens Purple Text, Orange Highlights in the reader

## [1.3.1] - 2026-08-18

### Changed

- Profile search field is smaller and quieter
- Highlight share lives in the card's overflow menu instead of a separate icon

## [1.3.0] - 2026-08-18

### Added

- Search box on your profile and other people's profiles filters the visible highlights, writings, or bookmarks
- Profile pages show public bookmarks (NIP-51) and web bookmarks (NIP-B0) as extra tabs

## [1.2.0] - 2026-08-18

### Added

- Share button on highlight cards: web highlights include a scroll-to-text fragment (`#:~:text=`); nostr-native highlights share the public article URL

### Changed

- Home cards for nostr-native articles show the author name instead of the d-tag

## [1.1.0] - 2026-08-18

### Added

- "Load more" at the end of a profile's highlights list pages in older highlights (NIP-01 `until`) until the relays have nothing left

### Changed

- Home rows (recently highlighted by you/friends/others, continue reading, most highlighted, random) show up to 21 articles instead of 12, backed by a larger highlight pool
- Profile pull-to-refresh only re-queries the visible tab's kind (writings or highlights); profile, relation, and the other tab stay on cache
- Renamed the "Random articles" home row to "Random unreads"

## [1.0.1] - 2026-08-17

### Fixed

- Reader no longer lags or freezes on articles with many highlights: quote positions are matched once per paragraph instead of on every drawn frame

## [1.0.0] - 2026-08-17

First stable release: Android reader with Amber/bunker login, NIP-84 highlights, Feeds, Library, and Search.

### Fixed

- Shared or opened links no longer reopen the reader after Back and a screen rotation
- Article URLs with percent-encoding (e.g. `%26`, `%2F`) no longer get double-decoded before fetch
- Open-with / VIEW links go through the same URL extractor as share (no raw `dataString` fallback)
- Reader “Try again” cancels the previous in-flight fetch so an older response cannot overwrite a newer load
- Inline and gallery images that use `http://` are fetched over `https://` (cleartext is blocked)

### Changed

- Gradle resolves `google()` / `mavenCentral()` before the Huawei Maven mirror (mirror kept as fallback)

## [0.75.0] - 2026-08-17

### Changed

- About Boris intro page shows the Boris logo (same asset as Zapstore)

## [0.74.0] - 2026-08-17

### Changed

- Search: drop the local-cache intro copy, nudge the field down a bit, and round it like Home article cards
- Find in page: same field styling; drop the “Type to search this article” copy
- Find in page: tapping a result closes the sidebar and keeps a light-blue selection mark on the match

## [0.73.0] - 2026-08-17

### Changed

- Bottom nav and Feeds screen title use “Feeds” instead of “Feed”
- Feeds adds an All tab (default) that merges Highlights, Writings, and RSS; default tab is configurable in settings with the same pill chips as the screen
- Library adds an All tab (default) that merges Private, Public, Web, Look, and Archive by time; settings use the same pill chips as the Library screen

## [0.72.0] - 2026-08-17

### Changed

- Home loading “Connecting…” shows briefly once, then cycles the later status lines
- Home shows results much sooner: rows render before link previews finish, and relay queries no longer wait the full timeout for one slow relay

## [0.71.0] - 2026-08-17

### Changed

- Cold-start quote screen fades out gently instead of cutting away
- Home loading spinner cycles through short status labels (relays, highlights, bookmarks, …)

## [0.70.0] - 2026-08-17

### Fixed

- Settings can be opened while logged out (they no longer bounce back immediately)

## [0.69.0] - 2026-08-17

### Changed

- Home “First time?” section dismisses automatically after the About Boris walkthrough reaches the last screen
- Home “Connect?” copy focuses on publishing highlights and discovering friends’ highlights

## [0.68.0] - 2026-08-17

### Changed

- Cold-start quote screen lasts at most 5 seconds

### Added

- Home: dismissible “First time?” section that opens the About Boris walkthrough
- Home: dismissible “Connect?” section that opens login on You (hidden when already logged in)

## [0.67.0] - 2026-08-17

### Fixed

- Bunker login: wait for NIP-42 AUTH, subscribe after auth, and republish on `auth-required` (Amber connect was timing out)

## [0.66.0] - 2026-08-17

### Changed

- Cold-start screen picks a random quote photo from the splash set

## [0.65.0] - 2026-08-17

### Changed

- Splash circle uses the yellow highlighter (inset so the mask does not clip it); quote photo stays on the fullscreen cold-start screen
- Cold-start quote screen lasts at most 2 seconds

## [0.64.0] - 2026-08-17

### Changed

- Splash: television / books quote photo in the Android 12 circle icon
- Cold start: fullscreen quote loading screen while relays fetch with no local highlights yet

## [0.63.0] - 2026-08-17

### Changed

- App icon: full yellow highlighter on a transparent background (no circular crop)

## [0.62.0] - 2026-08-17

### Added

- Airplane mode: show human-readable offline cache size next to download progress

### Fixed

- Highlight cards: render markdown links as labels instead of raw `[text](url)`
- Reader: space between blockquotes and following images

## [0.61.0] - 2026-08-17

### Added

- Settings About: Vision link to Purple Text, Orange Highlights

### Changed

- Feed settings category uses the RSS feed icon
- Zapstore description: “What highlighter was supposed to be.”

## [0.60.0] - 2026-08-17

### Added

- Settings: import RSS feeds from an OPML file

## [0.59.0] - 2026-08-17

### Added

- Settings About: link to the web app at read.withboris.com

### Changed

- Settings category colors are grouped and muted (one tint per card), with circular icon wells
- About shows the shared version · commit footer at the bottom

## [0.58.0] - 2026-08-17

### Added

- Settings About section with tutorial, Support Boris, and website/GitHub/author links
- Profile and You overflow menus: Copy Link (npub), Share (njump URL), plus open with njump or native app
- Reader highlights pane: 3-dot menu on highlight cards (go to quote, profile, njump, delete)

### Changed

- Highlights pane header: visibility toggle before settings

### Fixed

- Article list titles no longer inherit oversized reader body typography when wrapping

## [0.57.0] - 2026-08-17

### Changed

- Note cards: cover prefers an image from the note, then the author's picture, then a sticky-note icon; articles/web use an article icon instead of the highlighter

## [0.56.0] - 2026-08-17

### Added

- Reading position sync via Nostr (kind 39802), with scroll settings for sync, auto-scroll to saved position, and auto-archive at 100%
- Home: Random articles section from unread public/web library bookmarks
- Profile overflow menu: open with njump or native app

### Changed

- Most Highlighted ranks highlights from the last 7 days only (section title: Most highlighted this week)

### Fixed

- About/Support: spaces preserved around inline links (`send me sats`, `zaps`)

## [0.55.0] - 2026-08-17

### Added

- Support Boris screen: zap supporters with avatars and sats totals, reachable via the orange heart on the You tab

### Changed

- About Boris: Start reading is the only primary button on the last page

## [0.54.0] - 2026-08-17

### Added

- Home: Continue Reading section with articles you started, built from local reading positions
- Home: Most Highlighted section ranking articles by highlight count
- Home settings: re-order all Home sections

## [0.53.0] - 2026-08-17

### Added

- Reader menu: Find in article, a Ctrl+F-style fulltext search with a side pane, painted matches, and previous/next navigation

## [0.52.0] - 2026-08-17

### Added

- Reader menu: Open in native app for nostr articles (`nostr:naddr` / `nevent` / `note`)

## [0.51.0] - 2026-08-16

### Changed

- Search results reuse shared cards: highlights (with mine/friends/nostrverse colors), people (`AuthorCard`), and articles/bookmarks (`ArticleRow`)

## [0.50.0] - 2026-08-16

### Added

- Search tab searches your local cache for highlights, articles, bookmarks, and people

### Changed

- You tab shows settings as a top-bar icon again; sign-out stays in the 3-dot menu

## [0.49.0] - 2026-08-16

### Changed

- Home, Library, Feed, and You top bars use a shared 3-dot menu; settings for each screen live there. Help, library info, and feed scope toggles stay visible.

## [0.48.0] - 2026-08-16

### Changed

- Reader meta row: author is a clickable avatar pill that opens their profile; published date is a pill too

## [0.47.0] - 2026-08-16

### Added

- About Boris: separate Report a bug and Suggest a feature buttons, with GitHub issue templates
- About Boris: Start reading button on the last page
- About Boris: official Nostr logo on Connect on Nostr; send me sats links to dergigi.com/value

### Changed

- About Boris copy cleaned up for the Android app (no PWA pitch, feed instead of explore, simpler airplane mode wording, RSS mention, peace of mind rewrite)

## [0.46.0] - 2026-08-16

### Added

- Splash screen with the new icon while the event cache loads online; skipped offline so the app opens immediately

### Changed

- App icon updated to the highlighter mark

## [0.45.1] - 2026-08-16

### Changed

- Relays settings screen is split into Read, Write, and Local sections

## [0.45.0] - 2026-08-16

### Added

- Relays settings category showing the connection status of local and remote relays, with last-seen times for unreachable ones, plus how many of your follows write to each relay
- Outbox routing (NIP-65): highlights and writings from people you follow are fetched from their write relays, so content on small personal relays shows up
- Relay health tracking: dead relays are skipped for a while after repeated failures and get second chances later
- Persistent relay pool: connections stay open across queries instead of reconnecting for every request
- Relay discovery (NIP-66) expands the relay set for global feeds

## [0.44.0] - 2026-08-16

### Added

- Reading position is saved per article on the device and restored when reopening
- Article cards on home, library, and feed show a subtle reading progress bar once an article has been started

## [0.43.0] - 2026-08-16

### Added

- Library settings category with a default shelf setting: Private, Public, Web, Lookmarks, or Archive
- Settings gear on the library screen, opening the Library settings

## [0.42.0] - 2026-08-16

### Added

- Reading preview on the Appearance settings screen
- Settings gear in the reader highlights pane, opening the Highlights settings screen

## [0.41.2] - 2026-08-16

### Changed

- Offline downloads and App & Airplane Mode are one Airplane mode settings screen

## [0.41.1] - 2026-08-16

### Fixed

- Empty RSS tab opens Feed settings instead of offering a useless retry

## [0.41.0] - 2026-08-16

### Added

- 3-dot menu on highlight cards in the feed, You tab, and profiles: go to quote, view profile, open with njump, open with a native nostr app
- Delete your own highlights via NIP-09 deletion requests, signed with Amber or your bunker

## [0.40.0] - 2026-08-16

### Added

- The reader top bar hides when scrolling down and reappears when scrolling up, giving the article the full screen
- New "Hide top bar on scroll" toggle in the Scroll behaviour settings, on by default

## [0.39.0] - 2026-08-16

### Added

- RSS: a third tab in the feed next to Highlights and Writings, turning Boris into a simple RSS reader
- Manage feed URLs in the Feed settings category; the list syncs across devices like other settings
- RSS items render straight from the feed content, so they are readable offline and open instantly; teaser-only feeds fall back to the regular web fetch
- Highlighting an RSS item works like highlighting the regular web page

## [0.38.0] - 2026-08-16

### Added

- Offline downloads: Boris prefetches your whole library for offline reading, with per-shelf toggles and progress in the new Offline settings
- Configurable storage limit for the article and image caches, 1 GB by default

## [0.37.0] - 2026-08-16

### Added

- Web highlights also get zap splits, covering you and Boris since the author is unknown
- New toggle to disable zap splits entirely, on by default

## [0.36.0] - 2026-08-16

### Added

- Zap splits: highlights of nostr-native content carry weighted zap tags for you, the author(s), and Boris (NIP-57 Appendix G)
- New Zap Splits settings category with presets and sliders, synced with the webapp via the same settings keys

## [0.35.0] - 2026-08-16

### Changed

- Settings look like the stock Android settings app: grouped category rows with colored icon tiles, each opening its own sub-screen

### Added

- Reader settings in the reader's 3-dot menu jumps straight to the reading options

## [0.34.1] - 2026-08-16

### Fixed

- Highlight cards and article marks only color the quote, not the surrounding context sentences

## [0.34.0] - 2026-08-16

### Changed

- Archive uses the same books icon as the webapp, on Home, in the reader, and on the Library shelf
- The reader + becomes a bookmark when the article is already saved, and a circled check when it is archived

## [0.33.1] - 2026-08-16

### Changed

- The setting is now Open weblinks in Boris, and it covers settings links too

## [0.33.0] - 2026-08-16

### Added

- About Boris is a swipeable pager, ending with a page to connect on Nostr or open GitHub for bugs and ideas

### Fixed

- Home cards pick up the title and cover from an article you just read, instead of falling back to the slug

## [0.32.1] - 2026-08-16

### Fixed

- Highlight cards on the feed and profile show the surrounding sentences from the context tag, same as the reader pane
- Article images keep their real aspect ratio instead of rendering as a thin cropped strip

## [0.32.0] - 2026-08-16

### Added

- Home can hide articles you already archived, from the top-bar toggle or a matching settings switch

### Fixed

- Clicking a highlight from a profile or the feed waits for the article and marks, then scrolls to that quote
- Profile highlights use the friends or nostrverse color based on your relationship to that person

## [0.31.0] - 2026-08-16

### Added

- The reader + button lets you save nostr-native articles and notes to private or public bookmarks

### Changed

- Boolean settings use a switch on the right of the label

### Fixed

- Opening the highlights pane from a purple pill no longer lands on an empty filter

## [0.30.2] - 2026-08-16

### Changed

- Full-width images lives in its own Media settings section
- The airplane-mode note now says Citrine is a great option for Android, and Citrine itself is the Zapstore link

### Fixed

- A missing space before the first "here" in the relays learn-more sentence

## [0.30.1] - 2026-08-16

### Changed

- The reading preview in settings sits after the Highlights section, so it reflects both reading and highlight choices
- Settings sections are separated by horizontal dividers
- App & Airplane Mode is the last settings section, after Scroll Behaviour

## [0.30.0] - 2026-08-16

### Added

- Home has a header bar like the other screens, with a help button that opens an About page listing all Boris features with the illustrations from readwithboris.com

### Changed

- The website pill in the article view is now clickable and opens the site's root URL in the browser; nostr-native articles no longer show it
- The cover image shows an author and publication date byline below the title, replacing the hard-to-read date in the top-right corner
- The airplane mode section uses the webapp's relay copy ("Don't know what relays are? Learn more here, here, and here") with the same three links, opened in the reader
- The Citrine link in settings points to Zapstore

### Fixed

- Home covers no longer go missing: link previews are cached on disk and shown instantly, nostr article covers come from the event cache, and a failed preview fetch falls back to the cached one

## [0.29.0] - 2026-08-15

### Changed

- The Feed now shows highlights as the same bordered highlight cards used on profile pages, colored by who made them (you, friends, nostrverse)
- Highlight cards show the highlighter's profile picture next to their name, in the Feed, on profile pages, and in the reader's highlights pane
- The highlights pill in the article view takes its color from who made the highlights: yours win, then friends, then nostrverse

## [0.28.1] - 2026-08-15

### Changed

- Settings are regrouped into Appearance, Reading, Highlights, Feed, App & Airplane Mode, and Scroll Behaviour, so all highlight options live in one place
- Theme swatches are the same 40dp size as the appearance toggles, so the Appearance rows line up

## [0.28.0] - 2026-08-15

### Added

- Home shows an "Open from clipboard" banner when the clipboard holds a URL or nostr link, with one tap to read it and a dismiss button

## [0.27.1] - 2026-08-15

### Changed

- Link color, font size, highlight color, and scroll amount pickers sit on the same line as their labels again
- The Volume buttons settings section is now Scroll Behaviour, with the checkbox reworded to "Use volume buttons to scroll"

## [0.27.0] - 2026-08-15

### Added

- Settings include an App & Airplane Mode section with Citrine detection at `ws://127.0.0.1:4869`
- Highlights, library saves, and settings created offline stay on device and rebroadcast to remote relays when the network returns

## [0.26.1] - 2026-08-15

### Changed

- Settings color and font-size pickers are the same 40dp rounded squares as highlight style and alignment

## [0.26.0] - 2026-08-15

### Added

- Settings can turn off opening article links in the reader, so they go to the system browser instead
- Settings include a Feed section for the default feed scope, the same `defaultExploreScope*` keys as Explore on the webapp

## [0.25.1] - 2026-08-15

### Fixed

- Full-width images in the reader now span the article column instead of staying at their intrinsic size
- Profile header sits below the system status bar

## [0.25.0] - 2026-08-15

### Added

- Reader shows a full-bleed cover on articles that have a NIP-23 or Open Graph image, with the title overlaid like the webapp
- Reader opens a right-side highlights pane from the count pill or a painted quote, with mine/friends/nostrverse filters and jump-to-quote cards

## [0.24.0] - 2026-08-15

### Added

- Reader paints swarm highlights: yours in yellow, friends in orange, others in purple

## [0.23.1] - 2026-08-15

### Fixed

- Highlights, writings, and contact lists now load from the event cache first, so previously seen marks show immediately

## [0.23.0] - 2026-08-15

### Added

- Library has an Archive shelf for articles and pages marked with 📚
- Event cache keeps profiles, relay lists, bookmarks, and articles on disk, so screens render instantly and previously loaded content shows without a connection
- Library renders from the cache first and refreshes from relays in the background
- Previously opened web articles render offline via an HTTP cache for reader fetches

## [0.22.0] - 2026-08-15

### Added

- Reader shows a Move to Archive button at the end of each article when logged in
- Reader shows the author card at the end of nostr-native articles
- Tapping an author card opens that npub's profile, same layout as You

## [0.21.0] - 2026-08-15

### Changed

- Library sources explainer names Private, Public, and Web bookmarks, and says lookmarks are public
- Lookmarks use an eye icon so they line up with the other library shelves
- Settings version line reads Version 0.x.y

## [0.20.0] - 2026-08-15

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

[Unreleased]: https://github.com/dergigi/boris-android/compare/v0.34.1...HEAD
[0.34.1]: https://github.com/dergigi/boris-android/releases/tag/v0.34.1
[0.34.0]: https://github.com/dergigi/boris-android/releases/tag/v0.34.0
[0.33.1]: https://github.com/dergigi/boris-android/releases/tag/v0.33.1
[0.33.0]: https://github.com/dergigi/boris-android/releases/tag/v0.33.0
[0.32.1]: https://github.com/dergigi/boris-android/releases/tag/v0.32.1
[0.32.0]: https://github.com/dergigi/boris-android/releases/tag/v0.32.0
[0.31.0]: https://github.com/dergigi/boris-android/releases/tag/v0.31.0
[0.30.2]: https://github.com/dergigi/boris-android/releases/tag/v0.30.2
[0.30.1]: https://github.com/dergigi/boris-android/releases/tag/v0.30.1
[0.30.0]: https://github.com/dergigi/boris-android/releases/tag/v0.30.0
[0.29.0]: https://github.com/dergigi/boris-android/releases/tag/v0.29.0
[0.28.1]: https://github.com/dergigi/boris-android/releases/tag/v0.28.1
[0.28.0]: https://github.com/dergigi/boris-android/releases/tag/v0.28.0
[0.27.1]: https://github.com/dergigi/boris-android/releases/tag/v0.27.1
[0.27.0]: https://github.com/dergigi/boris-android/releases/tag/v0.27.0
[0.26.1]: https://github.com/dergigi/boris-android/releases/tag/v0.26.1
[0.26.0]: https://github.com/dergigi/boris-android/releases/tag/v0.26.0
[0.25.1]: https://github.com/dergigi/boris-android/releases/tag/v0.25.1
[0.25.0]: https://github.com/dergigi/boris-android/releases/tag/v0.25.0
[0.24.0]: https://github.com/dergigi/boris-android/releases/tag/v0.24.0
[0.23.1]: https://github.com/dergigi/boris-android/releases/tag/v0.23.1
[0.23.0]: https://github.com/dergigi/boris-android/releases/tag/v0.23.0
[0.22.0]: https://github.com/dergigi/boris-android/releases/tag/v0.22.0
[0.21.0]: https://github.com/dergigi/boris-android/releases/tag/v0.21.0
[0.20.0]: https://github.com/dergigi/boris-android/releases/tag/v0.20.0
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
