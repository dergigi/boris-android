# Phase 6: On-device article extraction - Pattern Map

**Mapped:** 2026-08-20
**Files analyzed:** 18
**Analogs found:** 18 / 18

Phase 6 replaces `r.jina.ai` with origin GET + on-device extract. Copy existing seams. Do not invent a second HTTP client, converter, or error UI. Package `org.dergigi.boris` only. No Hilt/Koin/Room. Do not touch Nostr Article/Note/Profile or RSS `when` arms except a shared 500-char constant rename.

## File Classification

| New/Modified File | Role | Data Flow | Closest Analog | Match Quality |
|-------------------|------|-----------|----------------|---------------|
| `app/src/main/java/org/dergigi/boris/data/ReaderRepository.kt` | service | request-response | itself (`null` arm + `parse` + cache) | exact |
| `app/src/main/java/org/dergigi/boris/data/ArticleExtractor.kt` | utility | transform | `HtmlToMarkdown.kt` + `OgMeta.kt` | role-match |
| `app/src/main/java/org/dergigi/boris/data/HtmlToMarkdown.kt` | utility | transform | itself | exact |
| `app/src/main/java/org/dergigi/boris/data/HttpUserAgents.kt` | utility | request-response | `OgMetaClient.kt` | role-match |
| `app/src/main/java/org/dergigi/boris/data/OgMetaClient.kt` | service | request-response | itself (share UA string) | exact |
| `app/src/main/java/org/dergigi/boris/data/UrlExtractor.kt` | utility | transform | itself (`preferHttps`, `articleUrl`) | exact |
| `app/src/main/java/org/dergigi/boris/data/ArticleCover.kt` | utility | transform | itself (keep `stripLeadingImage`) | exact |
| `app/src/main/java/org/dergigi/boris/data/PublishedTime.kt` | utility | transform | itself (keep `fromHtml`) | exact |
| `app/src/main/java/org/dergigi/boris/ui/reader/ReaderViewModel.kt` | store | request-response | itself (`load` catch → `Error`) | exact |
| `app/src/main/java/org/dergigi/boris/ui/reader/ReaderScreen.kt` | component | request-response | itself (error + Open original) | exact |
| `app/src/main/res/values/strings.xml` | config | — | itself (`reader_open_original`) | exact |
| `app/build.gradle.kts` | config | — | itself (catalog deps + compileOptions) | exact |
| `gradle/libs.versions.toml` | config | — | itself (`okhttp` / `junit` entries) | exact |
| `app/src/test/java/org/dergigi/boris/data/ArticleExtractorTest.kt` | test | transform | `HtmlToMarkdownTest.kt` + `OgMetaTest.kt` | role-match |
| `app/src/test/java/org/dergigi/boris/data/HtmlToMarkdownTest.kt` | test | transform | itself | exact |
| `app/src/test/java/org/dergigi/boris/data/ReaderRepositoryParseTest.kt` | test | transform | itself (HTML cases only) | exact |
| `app/src/test/java/org/dergigi/boris/data/ArticleCoverTest.kt` | test | transform | itself (drop Jina cases) | exact |
| `app/src/test/java/org/dergigi/boris/data/PublishedTimeTest.kt` | test | transform | itself (drop `fromJinaHeader`) | exact |

## Pattern Assignments

### `app/src/main/java/org/dergigi/boris/data/ReaderRepository.kt` (service, request-response)

**Analog:** this file. Change only the `null` `NostrLink` arm, `parse`, `toProxyUrl`, `cachedBodyBytes`, interceptor comment, and the 500-char constant. Leave `fetchArticle` / `fetchNote` / `rssContent` body as-is.

**Imports pattern** (lines 1-12):
```kotlin
package org.dergigi.boris.data

import okhttp3.Cache
import okhttp3.CacheControl
import okhttp3.OkHttpClient
import okhttp3.Request
import org.dergigi.boris.nostr.Nip01Event
import org.dergigi.boris.nostr.Nip23
import org.dergigi.boris.nostr.RelayQuery
import java.io.File
import java.io.IOException
import java.util.concurrent.TimeUnit
```
Do not add jsoup imports here. Parse through `ArticleExtractor` + `HtmlToMarkdown`. Do not construct a new `OkHttpClient` in `fetch`.

**Constructor + default client** (lines 14-16, 217-272):
```kotlin
class ReaderRepository(
    private val client: OkHttpClient = defaultClient,
) {
```
Reuse `companion` `defaultClient`. Tests construct `ReaderRepository()` with no args.

**Do not change these arms** (lines 19-21, 64-128, 135-152):
```kotlin
is NostrTarget.Article -> fetchArticle(target.ref)
is NostrTarget.Note -> fetchNote(target)
is NostrTarget.Profile -> throw IOException("Profile links cannot be opened as articles")
```
`rssContent` still calls `HtmlToMarkdown.convert(html)` without `ArticleExtractor`. Only the 500-char constant may be renamed and shared.

**Today's web-fetch arm to replace** (lines 22-36):
```kotlin
null -> {
    val targetUrl = UrlExtractor.normalize(url)
    rssContent(url, targetUrl) ?: run {
        val request = Request.Builder()
            .url(toProxyUrl(targetUrl))
            .header("Accept", "text/plain")
            .get()
            .build()
        val text = try {
            execute(request)
        } catch (e: IOException) {
            executeFromCache(request) ?: throw e
        }
        withCover(parse(targetUrl, text))
    }
}
```
Copy this structure: `rssContent` first, then GET, then `executeFromCache` on live fail, then parse. Swap `toProxyUrl` for `UrlExtractor.preferHttps(UrlExtractor.normalize(url))`. Set `User-Agent` + `Accept: text/html,application/xhtml+xml` like `OgMetaClient` lines 16-21. Delete `toProxyUrl` (line 215).

**Error throw pattern** (lines 45-51):
```kotlin
private fun execute(request: Request): String =
    client.newCall(request).execute().use { response ->
        if (!response.isSuccessful) {
            throw IOException("Failed to fetch readable content (${response.code})")
        }
        response.body?.string().orEmpty()
    }
```
Keep `execute().use`. Replace the message with the two D-13 strings only:
- network / timeout / non-2xx after retry → `IOException("Could not reach this page.")`
- 2xx but thin extract / not HTML → `IOException("Could not find an article on this page.")`
Do not leak status codes, Jina, or jsoup.

**FORCE_CACHE fallback** (lines 53-62) — D-15, keep as-is, but the request URL must be the origin:
```kotlin
private fun executeFromCache(request: Request): String? = try {
    val cached = request.newBuilder()
        .cacheControl(CacheControl.FORCE_CACHE)
        .build()
    client.newCall(cached).execute().use { response ->
        if (response.isSuccessful) response.body?.string() else null
    }
} catch (_: IOException) {
    null
}
```

**Empty-extract bar** (lines 141-142, 277-278):
```kotlin
val markdown = HtmlToMarkdown.convert(html)
if (markdown.length < MIN_RSS_MARKDOWN_CHARS) return null
```
```kotlin
/** Feed bodies shorter than this are teasers; fetch the web page instead. */
private const val MIN_RSS_MARKDOWN_CHARS = 500
```
Reuse this number for web extract. Rename to a shared `MIN_ARTICLE_MARKDOWN_CHARS = 500` (companion or small helper). Do not invent a second threshold.

**HTML parse path to keep and promote** (lines 179-196):
```kotlin
val preview = OgMeta.parse(text, targetUrl)
val title = htmlTitleRegex.find(text)?.groupValues?.getOrNull(1)?.trim()
val markdown = HtmlToMarkdown.convert(text, targetUrl)
    .let(UrlExtractor::upgradeImageHttpUrls)
    .ifBlank { null }
val cover = preview.imageUrl?.let(UrlExtractor::preferHttps)
ReadableContent(
    url = targetUrl,
    title = preview.title ?: title?.let(HtmlToMarkdown::decode),
    markdown = cover?.let { image ->
        markdown?.let { ArticleCover.stripLeadingImage(it, image) }
    } ?: markdown,
    publishedAt = PublishedTime.fromHtml(text),
    imageUrl = cover,
    summary = preview.description,
)
```
After extract: convert the article node only (`HtmlToMarkdown.convert(element, targetUrl)` or `element.outerHtml()` into `convert`). Still use `OgMeta.parse` + `PublishedTime.fromHtml` + `ArticleCover.stripLeadingImage` + `upgradeImageHttpUrls`. Delete the Jina branch (lines 161-178) and `markdownBlockRegex` / `titleRegex` / `markdownRegex` (lines 280-288). Keep `htmlTitleRegex` or drop it if `OgMeta` already covers title. Rename `parse` to `internal fun parseHtml` if you want; tests currently call `repository.parse(...)`.

**Cache key to fix** (lines 230-252):
```kotlin
val candidates = listOf(
    "https://r.jina.ai/$normalized",
    normalized,
).distinct()
```
After: `listOf(UrlExtractor.preferHttps(normalized))` only. Same origin URL for live GET, `FORCE_CACHE`, and `cachedBodyBytes`.

**Keep the cache interceptor** (lines 263-269). Change the jina comment only:
```kotlin
addNetworkInterceptor { chain ->
    chain.proceed(chain.request()).newBuilder()
        .removeHeader("Pragma")
        .removeHeader("Cache-Control")
        .header("Cache-Control", "public, max-age=$FRESH_SECONDS")
        .build()
}
```
Do not rewrite it to cache 401/403. OkHttp already skips unsuccessful responses.

**Post-fetch unchanged** (lines 39-42):
```kotlin
val ready = content.copy(markdown = content.markdown?.let(UrlExtractor::embedImageLinks))
ArticlePreview.remember(ready)
OfflineStore.markDownloaded(url)
```

---

### `app/src/main/java/org/dergigi/boris/data/ArticleExtractor.kt` (utility, transform)

**Analog:** `HtmlToMarkdown.kt` (stateless `object`, HTML in / Markdown out) plus `OgMeta.kt` (`parse(html, baseUrl)` returning structured result).

**Object + single entrypoint** from `HtmlToMarkdown.kt` lines 8-9:
```kotlin
package org.dergigi.boris.data

object HtmlToMarkdown {
    fun convert(html: String, baseUrl: String? = null): String {
```
Copy: `object ArticleExtractor` with `fun article(html: String, baseUrl: String): org.jsoup.nodes.Element?`. No class, no DI, no Android imports.

**baseUrl for relative URLs** from `OgMeta.kt` lines 11, 54-59:
```kotlin
fun parse(html: String, baseUrl: String): OgPreview {
    // ...
    imageUrl = image?.let { absoluteUrl(it, baseUrl) },
}
private fun absoluteUrl(raw: String, baseUrl: String): String? {
    return UrlExtractor.articleUrl(trimmed, baseUrl)
}
```
Pass the origin URL into `Jsoup.parse(html, baseUrl)` so `absUrl("src")` / `absUrl("href")` resolve. Then still run results through `UrlExtractor.articleUrl` / `preferHttps` when converting images (same as `HtmlToMarkdown.image` lines 72-80).

**Chrome drop analog** from `HtmlToMarkdown.kt` lines 16-20 (regex strip of script/style/head). Replace regex with jsoup `doc.select(CHROME).remove()`. Do not convert the full `body` without this step.

**Scoring analog** from `ReaderRepository.rssContent` lines 141-142: reject thin bodies with the same 500-char bar. `ArticleExtractor.score` returning 0 for link-dense / short nodes is new algorithm (see RESEARCH.md Pattern 3). No existing score function in-repo.

**Do not** copy `Jsoup.connect` from jsoup docs. Boris already fetched the bytes with OkHttp.

---

### `app/src/main/java/org/dergigi/boris/data/HtmlToMarkdown.kt` (utility, transform)

**Analog:** this file. Extend; do not replace.

**Existing tag pipeline to keep** (lines 36-60):
```kotlin
for (level in 1..6) {
    s = s.replace(Regex("(?is)<h$level[^>]*>(.*?)</h$level>")) { m ->
        "\n\n" + "#".repeat(level) + " " + stripTags(m.groupValues[1]).trim() + "\n\n"
    }
}
s = s.replace(Regex("(?is)<(strong|b)\\b[^>]*>(.*?)</\\1>")) { "**${it.groupValues[2]}**" }
s = s.replace(Regex("(?is)<(em|i)\\b[^>]*>(.*?)</\\1>")) { "*${it.groupValues[2]}*" }
s = s.replace(Regex("(?is)<blockquote[^>]*>(.*?)</blockquote>")) { ... }
s = s.replace(Regex("(?is)<li[^>]*>(.*?)</li>")) { "\n- ${it.groupValues[1].trim()}" }
```
Add before the generic `<li>` → `- ` rule: `<ol><li>` → `1. `. Add GFM tables (`table` / `thead` / `tr` / `th` / `td`). Add footnote pair `sup a[href^=#]` + matching `li[id]` / `.footnotes li` → `[^id]` / `[^id]: text` so `Footnotes.expand` (lines 10-27 of `Footnotes.kt`) lights up.

**Image + HTTPS** (lines 72-80) — D-06, keep:
```kotlin
private fun image(tag: String, baseUrl: String?): String {
    val src = attr(tag, "src") ?: return ""
    val url = if (baseUrl.isNullOrBlank()) {
        src
    } else {
        UrlExtractor.articleUrl(src, baseUrl) ?: return ""
    }.let(UrlExtractor::preferHttps)
    val alt = attr(tag, "alt").orEmpty()
    return "\n\n![$alt]($url)\n\n"
}
```

**New overload:** `fun convert(element: org.jsoup.nodes.Element, baseUrl: String): String` can call `convert(element.outerHtml(), baseUrl)` so RSS (`convert(html)` string) stays on the same pipeline. RSS tests must stay green.

**Entity decode** (lines 89-101) stays public (`fun decode`) — `OgMeta` and `readerLoadingState` call it.

---

### `app/src/main/java/org/dergigi/boris/data/HttpUserAgents.kt` (utility, request-response)

**Analog:** `OgMetaClient.kt` lines 33-34 (browser UA) and `SettingsVersionFooter.kt` line 29 (`BuildConfig.VERSION_NAME`).

**Retry UA — copy this string exactly** (`OgMetaClient.kt` 33-34):
```kotlin
private const val USER_AGENT =
    "Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/122.0.0.0 Mobile Safari/537.36"
```
Move it here as `BROWSER_UA`. Point `OgMetaClient.fetch` at the same constant so OG and reader retry stay identical.

**Honest first UA:**
```kotlin
const val BORIS_UA =
    "Boris/${org.dergigi.boris.BuildConfig.VERSION_NAME} (Android; +https://github.com/dergigi/boris-android)"
```
`buildConfig = true` is already on (`app/build.gradle.kts` line 89). `BuildConfig.VERSION_NAME` is already read in settings. Data layer already imports Android-adjacent types (`OgMetaClient` uses OkHttp only; `BuildConfig` is generated in `org.dergigi.boris`). Acceptable in this object.

**Request header analog** (`OgMetaClient.kt` 16-21):
```kotlin
val request = Request.Builder()
    .url(url)
    .header("User-Agent", USER_AGENT)
    .header("Accept", "text/html,application/xhtml+xml")
    .get()
    .build()
```
Origin GET uses this shape with `BORIS_UA` first, then one rebuild with `BROWSER_UA` on 401/403/empty extract.

Do not copy `OgMetaClient`'s private 8s client (lines 8-13). Reader stays on `ReaderRepository.defaultClient`.

---

### `app/src/main/java/org/dergigi/boris/data/OgMetaClient.kt` (service, request-response)

**Analog:** this file. Only change: `USER_AGENT` becomes `HttpUserAgents.BROWSER_UA` (or whatever the shared name is). Leave prefix-byte fetch, own short-timeout client, and `OgMeta.parse`. Reader fetch does not call this for the article body.

---

### `app/src/main/java/org/dergigi/boris/data/UrlExtractor.kt` (utility, transform)

**Analog:** this file.

**Web-fetch URL** — `preferHttps` lines 38-45:
```kotlin
fun preferHttps(url: String): String {
    val trimmed = url.trim()
    return if (trimmed.startsWith("http://", ignoreCase = true)) {
        "https://" + trimmed.substring(7)
    } else {
        trimmed
    }
}
```
Call this on the article GET. Update the comment at lines 34-37 (it still says article opens keep `http://` for Jina). `normalize` (lines 23-32) keeps scheme as typed; do not change that for share/paste. Only the repository origin GET upgrades.

**Relative images** — `articleUrl` lines 53-81 plus `nonHttpSchemes` line 196 (`javascript` already denied). `HtmlToMarkdown.image` already uses this. After jsoup `absUrl`, still pass through `articleUrl` + `preferHttps`.

---

### `app/src/main/java/org/dergigi/boris/data/ArticleCover.kt` (utility, transform)

**Analog:** this file. Delete Jina-only helpers. Keep cover strip.

**Delete** (lines 4-13, 26-38): `imageFromJina`, `descriptionFromJina`, `header`, `imageField`, `descriptionField`.

**Keep** (lines 15-24):
```kotlin
fun firstMarkdownImage(markdown: String, baseUrl: String? = null): String? =
    UrlExtractor.imageUrls(markdown, baseUrl).firstOrNull()

fun stripLeadingImage(markdown: String, coverUrl: String): String {
    val match = leadingImage.find(markdown) ?: return markdown
    val found = UrlExtractor.articleUrl(match.groupValues[1].trim()) ?: return markdown
    if (ArticleUrl.normalize(found) != ArticleUrl.normalize(coverUrl)) return markdown
    return markdown.removeRange(match.range).trimStart()
}
```
`ReaderRepository.parse` HTML path and `withCover` already call `stripLeadingImage`.

---

### `app/src/main/java/org/dergigi/boris/data/PublishedTime.kt` (utility, transform)

**Analog:** this file. Delete `fromJinaHeader` (lines 48-52) and `headerField` (lines 25-28). Keep `fromHtml` (lines 54-59) and `parse` / `label`. HTML extract uses `PublishedTime.fromHtml(text)` as today's fallback already does (`ReaderRepository.kt` line 192).

---

### `app/src/main/java/org/dergigi/boris/ui/reader/ReaderViewModel.kt` (store, request-response)

**Analog:** this file. Two error kinds stay as `ReaderUiState.Error(message, url)`. Do not add a new sealed variant.

**Load + IO + catch** (lines 166-224):
```kotlin
fun load() {
    if (url.isBlank()) {
        _state.value = ReaderUiState.Error("No URL to read.", url)
        // ...
        return
    }
    loadJob = viewModelScope.launch {
        _state.value = readerLoadingState(url)
        try {
            val content = withContext(Dispatchers.IO) { repository.fetch(url) }
            _state.value = ReaderUiState.Ready(content)
            // highlight / membership / archive / author / rss / eventRefs
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            _state.value = ReaderUiState.Error(
                e.message ?: "Failed to load this article.",
                url,
            )
        }
    }
}
```
Copy this: no session check before `fetch` (READ-01). Re-throw `CancellationException`. Map `e.message` into `Error`. Change the fallback `"Failed to load this article."` (line 219) to `"Could not reach this page."` so a null-message exception still uses D-13, not a third string.

**Error type** (lines 1148-1156):
```kotlin
sealed interface ReaderUiState {
    data class Loading(...) : ReaderUiState
    data class Ready(val content: ReadableContent) : ReaderUiState
    data class Error(val message: String, val url: String) : ReaderUiState
}
```
Leave this shape. Repository throws the two D-13 strings; ViewModel does not classify HTTP codes.

---

### `app/src/main/java/org/dergigi/boris/ui/reader/ReaderScreen.kt` (component, request-response)

**Analog:** this file. Error column already shows the message, Try again, and Open original.

**Error UI** (lines 841-868):
```kotlin
is ReaderUiState.Error -> {
    Column(...) {
        Text(
            text = state.message,
            style = MaterialTheme.typography.bodyLarge.copy(textAlign = TextAlign.Center),
        )
        Button(onClick = onRetry, modifier = Modifier.padding(top = 16.dp)) {
            Text("Try again")
        }
        if (state.url.isNotBlank()) {
            OutlinedButton(
                onClick = ::openOriginal,
                modifier = Modifier.padding(top = 8.dp),
            ) {
                Text("Open original")
            }
        }
    }
}
```
Keep this layout for both error kinds. `state.message` is the D-13 copy. Prefer `stringResource(R.string.reader_open_original)` like the overflow menu at line 723, instead of the hardcoded `"Open original"` at line 865.

**Open original action** (lines 573-576) plus `ArticleActions.kt` 29-32:
```kotlin
fun openOriginal() {
    val url = articleUrl ?: return
    openOriginalArticle(context, url)
}
```
```kotlin
fun openOriginalArticle(context: Context, url: String) {
    val target = NostrLink.parse(url)?.publicUrl ?: url
    openExternalUri(context, target)
}
```
`articleUrl` already includes `Error.url` (`ReaderScreen.kt` 544-545). Do not hide the button on extract-fail.

---

### `app/src/main/res/values/strings.xml` (config)

**Analog:** this file, next to `reader_open_original` (line 179).

```xml
<string name="reader_open_original">Open original</string>
```

Add:
```xml
<string name="reader_error_unreachable">Could not reach this page.</string>
<string name="reader_error_no_article">Could not find an article on this page.</string>
```
Verbatim D-13. No Jina, no jsoup. Repository may keep the same literals as Kotlin constants so JVM tests do not need `Resources`. Screen can keep showing `state.message`. Wire `stringResource` only if you also load those strings in the ViewModel via `getApplication<Application>().getString(...)` (same as `highlight_cancelled` at `ReaderViewModel.kt` 243).

---

### `gradle/libs.versions.toml` + `app/build.gradle.kts` (config)

**Analog:** existing catalog + module deps. No desugaring in the repo today.

**Catalog shape** (`gradle/libs.versions.toml` lines 10-13, 32, 39):
```toml
okhttp = "4.12.0"
junit = "4.13.2"

okhttp = { group = "com.squareup.okhttp3", name = "okhttp", version.ref = "okhttp" }
junit = { group = "junit", name = "junit", version.ref = "junit" }
```
Add:
```toml
jsoup = "1.23.1"
# desugar.jdk.libs.nio version: resolve in Wave 0 from Google/Huawei metadata

jsoup = { group = "org.jsoup", name = "jsoup", version.ref = "jsoup" }
desugar-jdk-libs-nio = { group = "com.android.tools", name = "desugar_jdk_libs_nio", version.ref = "desugarJdkLibsNio" }
```

**Module deps** (`app/build.gradle.kts` 78-81, 114-140):
```kotlin
compileOptions {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}
dependencies {
    implementation(libs.okhttp)
    testImplementation(libs.junit)
}
```
Add `isCoreLibraryDesugaringEnabled = true` inside `compileOptions`. Add `implementation(libs.jsoup)` and `coreLibraryDesugaring(libs.desugar.jdk.libs.nio)`. Do not bump `versionCode` / `versionName` (lines 38-39). This phase is not a release.

---

### `app/src/test/java/org/dergigi/boris/data/ArticleExtractorTest.kt` (test, transform)

**Analog:** `HtmlToMarkdownTest.kt` + `OgMetaTest.kt`.

**Test class shape** (`HtmlToMarkdownTest.kt` 1-13):
```kotlin
package org.dergigi.boris.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class HtmlToMarkdownTest {
    @Test
    fun convertsParagraphsAndFormatting() {
        val markdown = HtmlToMarkdown.convert("<p>First <strong>bold</strong> ...")
```

**HTML fixtures with baseUrl** (`OgMetaTest.kt` 9-19):
```kotlin
val html = """
    <html><head>
    <title>Tab title</title>
    ...
    </head></html>
""".trimIndent()
val preview = OgMeta.parse(html, "https://www.geoffreylitt.com/post")
```

Copy: JVM JUnit 4, `org.junit.Assert.*`, multiline fixture strings, `baseUrl` argument, no Robolectric, no live network. Filename `{Type}Test.kt`.

Cases: `article` kept; `nav` / `#comments` dropped; link-dense node scores 0; under-500 text → null; relative `img src` becomes absolute via `baseUri`.

---

### `app/src/test/java/org/dergigi/boris/data/HtmlToMarkdownTest.kt` (test, transform)

**Analog:** this file. Keep every existing `@Test`. Add GFM table, `[^1]` footnote pair, `ol` numbering, script tags gone. Chrome-free fixtures belong in `ArticleExtractorTest`; this file stays converter-only so RSS still uses `convert(html)` without extract.

---

### `app/src/test/java/org/dergigi/boris/data/ReaderRepositoryParseTest.kt` (test, transform)

**Analog:** this file. Keep the construct + `parse` style (lines 7-8, 43-72).

```kotlin
class ReaderRepositoryParseTest {
    private val repository = ReaderRepository()

    @Test
    fun parsesHtmlFallback() {
        val raw = "<html><head><title>Page Title</title></head><body><p>Hi</p></body></html>"
        val content = repository.parse("https://example.com", raw)
```

**Delete:** `parsesJinaMarkdownPayload` (11-27), `parsesPublishedTimeFromJinaHeader` (30-40), `parsesCoverFromJinaHeaderAndStripsTheLeadImage` (75-91).

**Keep:** `parsesHtmlFallback`, `htmlFallbackKeepsImagesInMarkdown`, `noteMarkdownEmbedsImageLinksAndKeepsLineBreaks`.

**Add:** thin HTML → empty / no-article; chrome fixture (`<nav>Home</nav><article>…500+ chars…</article><div id="comments">nope</div>`) must not contain `Home` or `nope`. Drive `parse` / `parseHtml` only. No sockets.

---

### `app/src/test/java/org/dergigi/boris/data/ArticleCoverTest.kt` (test, transform)

**Analog:** this file. Delete `readsImageAndDescriptionFromJinaHeader` (8-20) and `missingJinaFieldsAreNull` (55-59). Keep `firstMarkdownImageTakesTheLeadPicture` and both `stripLeadingImage` tests.

---

### `app/src/test/java/org/dergigi/boris/data/PublishedTimeTest.kt` (test, transform)

**Analog:** this file. Delete `fromJinaHeader` (32-41) and `fromJinaHeaderMissing` (44-52). Keep `fromHtmlMeta` (54-62) and `parse` / `label` tests.

---

## Shared Patterns

### No DI, one-way layers
**Source:** `ReaderViewModel.kt` 72; `ARCHITECTURE.md`
**Apply to:** all new Kotlin
```kotlin
private val repository = ReaderRepository()
```
`object` for extract/convert/UA. UI imports data; data does not import UI (except generated `BuildConfig` on the UA object). Package `org.dergigi.boris` only.

### Repository throws, ViewModel maps
**Source:** `ReaderRepository.kt` 21, 45-48; `ReaderViewModel.kt` 206-221
**Apply to:** origin fetch outcomes
```kotlin
throw IOException("Could not reach this page.")
// or
throw IOException("Could not find an article on this page.")
```
```kotlin
} catch (e: Exception) {
    _state.value = ReaderUiState.Error(e.message ?: "Could not reach this page.", url)
}
```

### Companion OkHttp + FORCE_CACHE
**Source:** `ReaderRepository.kt` 45-62, 254-272
**Apply to:** origin GET, UA retry, offline fallback
Reuse `defaultClient`. Second GET for browser UA, not a new client. Cache key is the origin URL. Interceptor still forces `max-age=300`.

### HTTPS hygiene
**Source:** `UrlExtractor.kt` 38-45, 53-81, 196; `HtmlToMarkdown.kt` 72-80
**Apply to:** article GET and image `src`
`preferHttps` on the request URL. `articleUrl` + `preferHttps` on images. `javascript` already in `nonHttpSchemes`.

### 500-character empty bar
**Source:** `ReaderRepository.kt` 141-142, 277-278
**Apply to:** `rssContent` and web extract after `HtmlToMarkdown.convert`
One constant. Thin extract is `ERROR_NO_ARTICLE`, not a Ready teaser.

### JVM fixture tests
**Source:** `HtmlToMarkdownTest.kt`, `OgMetaTest.kt`, `ReaderRepositoryParseTest.kt`
**Apply to:** all Phase 6 tests
JUnit 4, fixture strings, no Robolectric, no live network. `{Type}Test.kt` under `app/src/test/java/org/dergigi/boris/data/`.

### Version catalog deps
**Source:** `gradle/libs.versions.toml` + `app/build.gradle.kts` 114-140
**Apply to:** jsoup 1.23.1 and `desugar_jdk_libs_nio`
Add version + library alias, then `implementation(libs.jsoup)`. Enable core library desugaring in `compileOptions`.

### Footnote render already exists
**Source:** `Footnotes.kt` 10-27
**Apply to:** HTML footnote emit
Converter writes `[^n]` / `[^n]: text`. Do not add a second footnote renderer.

## No Analog Found

No in-repo jsoup or readability library. Chrome scoring (link density, `CHROME` / `ARTICLE` selectors) is new; copy the RESEARCH.md Pattern 3 sketch, not a port of Readability4J.

| File | Role | Data Flow | Reason |
|------|------|-----------|--------|
| jsoup `Jsoup.parse(html, baseUri)` API | — | transform | No DOM library in the tree. Official two-arg parse only; never `Jsoup.connect`. |
| `desugar_jdk_libs_nio` pin | config | — | No desugaring today. Planner resolves version in Wave 0 (RESEARCH A2). |

## Do Not Copy

| Tempting analog | Why not |
|-----------------|--------|
| `ReaderRepository.toProxyUrl` / Jina `parse` branch | D-01, D-04. Delete. |
| `cachedBodyBytes` `r.jina.ai` candidate | D-02. Origin key only. |
| `OgMetaClient` private OkHttpClient | Different timeouts; would skip reader disk cache. |
| `RssParser` XmlPullParser walk | XML feeds, not HTML articles. RSS stays on string `HtmlToMarkdown.convert`. |
| `com.readwithboris` tree | Dead namespace. Never edit. |
| Readability4J / Crux | Rejected in RESEARCH.md. |

## Metadata

**Analog search scope:** `app/src/main/java/org/dergigi/boris/data/`, `app/src/main/java/org/dergigi/boris/ui/reader/`, `app/src/test/java/org/dergigi/boris/data/`, `app/src/main/res/values/strings.xml`, `app/build.gradle.kts`, `gradle/libs.versions.toml`
**Files scanned:** 47 data sources, 90 test files (indexed), plus reader UI / gradle / strings
**Pattern extraction date:** 2026-08-20
