# Testing Patterns

**Analysis Date:** 2026-08-14

Write new tests under `app/src/test/java/org/dergigi/boris/`. Do not add tests under `com.readwithboris`.

## Test Framework

**Runner:**
- JUnit 4.13.2 (`libs.junit` in `gradle/libs.versions.toml`)
- Wired as `testImplementation(libs.junit)` in `app/build.gradle.kts`
- Android unit-test source set: `app/src/test/java/`

**Assertion Library:**
- `org.junit.Assert` static imports: `assertEquals`, `assertNull`
- Compare expected first, actual second

**Run Commands:**
```bash
./gradlew :app:test                          # All unit tests (all build variants)
./gradlew :app:testDebugUnitTest             # Debug unit tests only
./gradlew :app:testDebugUnitTest --tests org.dergigi.boris.data.UrlExtractorTest
./gradlew :app:testDebugUnitTest --tests org.dergigi.boris.data.UrlExtractorTest.extractsBareHttpsUrl
```

No watch mode. No coverage task is configured.

## Test File Organization

**Location:**
- Separate test tree mirroring the production package: `app/src/test/java/org/dergigi/boris/data/`
- Not co-located with `main/` sources
- No `androidTest/` source set. No Compose UI tests, Robolectric, or instrumented tests

**Naming:**
- `{Type}Test` for the full public surface: `UrlExtractorTest`, `ImageStoreTest`
- `{Type}{Slice}Test` when only one seam is tested: `ReaderRepositoryParseTest` covers `parse()`, not `fetch()`

**Structure:**
```
app/src/test/java/org/dergigi/boris/
  data/
    UrlExtractorTest.kt
    ImageStoreTest.kt
    ReaderRepositoryParseTest.kt
app/src/main/java/org/dergigi/boris/
  data/
    UrlExtractor.kt
    ImageStore.kt
    ReaderRepository.kt
```

Add a new test next to the type it covers. A new `data/` helper gets `app/src/test/java/org/dergigi/boris/data/{Name}Test.kt`. A new `internal` UI helper (like `readingTimeLabel` in `app/src/main/java/org/dergigi/boris/ui/reader/ReaderScreen.kt`) gets `app/src/test/java/org/dergigi/boris/ui/reader/{Name}Test.kt`.

## Test Structure

**Suite Organization:**
```kotlin
package org.dergigi.boris.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class UrlExtractorTest {
    @Test
    fun extractsBareHttpsUrl() {
        assertEquals(
            "https://example.com/article",
            UrlExtractor.extract("https://example.com/article"),
        )
    }

    @Test
    fun returnsNullWhenEmpty() {
        assertNull(UrlExtractor.extract("   "))
        assertNull(UrlExtractor.extract(null))
    }
}
```

Match `app/src/test/java/org/dergigi/boris/data/UrlExtractorTest.kt`.

**Patterns:**
- Flat JUnit 4 class. No nested suites, no `@Before` / `@After`, no `@RunWith`.
- Test method names are camelCase sentences: `extractsBareHttpsUrl`, `parsesJinaMarkdownPayload`, `filenameFallsBackWhenMissingExtension`. No `should_`, no backtick names.
- One behavior per `@Test`. Multiple `assertEquals` / `assertNull` calls are fine when they prove the same case (`parsesJinaMarkdownPayload` in `ReaderRepositoryParseTest.kt`).
- Construct the SUT once as a field when it is a class: `private val repository = ReaderRepository()` in `app/src/test/java/org/dergigi/boris/data/ReaderRepositoryParseTest.kt`.
- Call `object` APIs directly: `UrlExtractor.extract(...)`, `ImageStore.filenameFor(...)`.

## Mocking

**Framework:** None. No Mockito, MockK, or `mock()`.

**Patterns:**
```kotlin
class ReaderRepositoryParseTest {
    private val repository = ReaderRepository()

    @Test
    fun parsesHtmlFallback() {
        val raw = "<html><head><title>Page Title</title></head><body><p>Hi</p></body></html>"
        val content = repository.parse("https://example.com", raw)
        assertEquals("Page Title", content.title)
        assertEquals(raw, content.html)
        assertNull(content.markdown)
    }
}
```

Test an `internal` seam instead of mocking HTTP. `ReaderRepository.parse` in `app/src/main/java/org/dergigi/boris/data/ReaderRepository.kt` exists so tests never call `fetch()`.

**What to Mock:**
- Do not add mocks for new unit tests.
- If a function needs IO, extract a pure/internal function (`parse`, `filenameFor`, `mimeFor`, `extract`) and test that.

**What NOT to Mock:**
- `UrlExtractor`, `ReadableContent`, regex parsers, filename/MIME helpers
- Do not mock `OkHttpClient` or `Context` in this source set. Those paths stay untested on the JVM.

## Fixtures and Factories

**Test Data:**
```kotlin
val raw = """
    Title: Hello World
    URL Source: https://example.com/hello
    Markdown Content:
    # Hello

    Body text.
""".trimIndent()

val markdown = """
    Intro
    ![one](https://example.com/a.jpg)
    ![two](/images/b.png "caption")
    ![skip](mailto:x@example.com)
""".trimIndent()
```

Copy this style from `ReaderRepositoryParseTest.kt` and `UrlExtractorTest.kt`. Use real-looking URLs (`https://www.citadel21.com/...`, `https://cdn.example.com/...`).

**Location:**
- Inline in the test method. No `fixtures/` directory, no factory helpers.
- Extract a local `val` only when the same payload is asserted twice in one test.

## Coverage

**Requirements:**
- None enforced. No JaCoCo / Kover plugin.
- Cover every new pure function and every `internal` parser/helper.
- Current JVM coverage: `UrlExtractor` (extract/normalize/articleUrl/imageUrls), `ReaderRepository.parse`, `ImageStore.filenameFor` / `mimeFor`.
- Untested on the JVM: `ReaderRepository.fetch`, `ImageStore.save` / `shareIntent`, ViewModels, Compose screens, `readingTimeLabel`.

**Configuration:**
- Not applicable. Do not add a coverage gate unless a phase asks for it.

**View Coverage:**
```bash
# Not configured
./gradlew :app:testDebugUnitTest
```

## Test Types

**Unit Tests:**
- JVM-only JUnit 4. No Android framework.
- Scope: string/URL/HTML/markdown parsing and filename/MIME rules.
- Keep each test fast and hermetic. No network, no `Context`, no disk.

**Integration Tests:**
- Not used. `fetch()` talks to `https://r.jina.ai/` and is not unit-tested.

**E2E Tests:**
- Not used. No Espresso, Compose UI Test, or screenshot tests.
- `ReaderScreen` / `ReaderScreenContent` is split so a later Compose test can drive `ReaderScreenContent` with fake `ReaderUiState`. Do not add that harness unless a phase requires it.

## Common Patterns

**Async Testing:**
- Not used. Production coroutines (`viewModelScope.launch`, `withContext(Dispatchers.IO)`) stay outside the unit-test surface.

**Error Testing:**
```kotlin
@Test
fun returnsNullWhenEmpty() {
    assertNull(UrlExtractor.extract("   "))
    assertNull(UrlExtractor.extract(null))
}

@Test
fun ignoresMailtoAndAnchors() {
    assertNull(UrlExtractor.articleUrl("mailto:hi@example.com"))
    assertNull(UrlExtractor.articleUrl("#section"))
}
```

Assert `null` for expected rejection. Do not use `assertThrows` unless you add a test for a function that already throws (`IOException` in `fetch` / `save`). Prefer testing the non-throwing seam.

**Snapshot Testing:**
- Not used. Assert concrete strings and lists.

**Visibility for tests:**
- Mark a helper `internal` when tests need it and other app packages should not (`parse` in `ReaderRepository.kt`, `readingTimeLabel` in `ReaderScreen.kt`).
- Do not widen a function to `public` just for tests.

---

*Testing analysis: 2026-08-14*
*Update when test patterns change*
