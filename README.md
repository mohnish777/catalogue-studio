# Catalogue Studio

An Android product catalogue built with Kotlin and Jetpack Compose. The planned product discovery, search and email-enquiry flows will use a local Node.js backend.

## Current checkpoint

The normal Android launcher now opens the complete catalogue through a typed navigation graph. Search, categories, featured picks and product cards use the local backend. Product taps load the full product-detail screen; its Enquire button opens the enquiry form for the same product ID, and Back returns through the stack. The form validates fields, retains failed submissions and shows confirmation after backend acceptance. The backend provides health, catalogue, product-detail and image endpoints with explicit demo/live sources; Mailpit supplies a local test inbox. The backend now accepts validated enquiries and hands email to local Mailpit SMTP; the Android form now connects to that endpoint. Start the backend to load products; connection failures offer Retry.

## Run in Android Studio

1. Open the repository's `android/` directory as the project.
2. Use JDK 17 for Gradle and install Android SDK Platform 36 through SDK Manager.
3. Let Gradle sync. The wrapper selects Gradle 8.14.3 automatically.
4. Select the `app` run configuration and an emulator or device running API 26 or newer, then press Run.

The project pins the Gradle build process to a locally installed JDK 17 through `android/gradle/gradle-daemon-jvm.properties`. This is separate from the Java/Kotlin bytecode targets in the app build script. Use a full JDK containing `bin/jlink`; a runtime-only Java installation can fail Android's `JdkImageTransform` even when Java itself starts successfully.

Android Studio records your SDK location in `android/local.properties`, which is ignored by Git. For command-line builds, either set `ANDROID_HOME` to your SDK directory or create that file with `sdk.dir=/absolute/path/to/Android/sdk`.

## Build from the command line

From the repository root:

```sh
cd android
./gradlew assembleDebug --console=plain
```

The APK is written to `android/app/build/outputs/apk/debug/app-debug.apk`, relative to the repository root.

To install and launch on a running emulator, from `android/`:

```sh
adb devices
adb -s emulator-5554 install -r app/build/outputs/apk/debug/app-debug.apk
adb -s emulator-5554 shell am start -n com.cataloguestudio/.MainActivity
```

Replace `emulator-5554` with the intended device serial from `adb devices`. The CLI commands assume `adb` is on your PATH.

Run Android static checks with:

```sh
./gradlew lintDebug --console=plain
```

## Run the backend and inbox with Docker

With Docker Desktop running, start both services from the repository root:

```sh
docker compose up --build -d --wait
```

Open [backend health](http://127.0.0.1:3000/health) and [the Mailpit inbox](http://127.0.0.1:8025). Compose waits for Mailpit to be healthy before starting the backend; the backend has its own `/health` check. Docker supplies Node 22, so a host Node installation is only needed for direct backend development below.

The backend image compiles TypeScript in a build stage, then runs the compiled JavaScript with production dependencies as the non-root `node` user. The server listens on `0.0.0.0` inside the container so Docker can forward requests to it. The published host ports remain bound to `127.0.0.1`.

If a direct development server already uses port 3000, either stop that server or choose a different Docker host port:

```sh
BACKEND_PORT=3001 docker compose up --build -d --wait
```

Then use `http://127.0.0.1:3001/health`. This changes the host port only; the container still listens on port 3000.

Useful commands:

```sh
docker compose ps
docker compose logs backend
docker compose stop
```

## Run the backend directly for development

Use Node.js 22 or newer. With nvm installed, select the project's version from the repository root:

```sh
nvm install
nvm use
cd backend
npm ci
npm run dev
```

The server listens on `127.0.0.1:3000`. Open [the health endpoint](http://127.0.0.1:3000/health), or run:

```sh
curl http://127.0.0.1:3000/health
```

Expected response: `{"status":"ok"}`. Stop the server with Ctrl+C. `npm run dev` watches the TypeScript files and restarts when they change; type checking is a separate command.

From `backend/`, verify and run the compiled JavaScript with:

```sh
npm run typecheck
npm run build
npm start
```

Run either the development server or the compiled server at a time. If port 3000 is already occupied, use `PORT=3001 npm run dev` and request port 3001 instead.

Direct execution defaults to your computer's loopback interface (`HOST=127.0.0.1`). The container sets `HOST=0.0.0.0` through its Dockerfile. The enquiry endpoint sends through local Mailpit SMTP as described below.

## Start the local email inbox

With Docker Desktop running, start Mailpit from the repository root:

```sh
docker compose up -d mailpit
```

Open [the Mailpit inbox](http://127.0.0.1:8025) in your browser.

| Address | Purpose |
| --- | --- |
| `127.0.0.1:1025` | SMTP server that accepts test email |
| `http://127.0.0.1:8025` | Browser inbox for inspecting captured messages |

Mailpit captures email locally; it does not deliver to an external mailbox. This setup needs no email-provider account or credentials. Both published ports bind to your computer's loopback interface. Inbox data is disposable; no persistent storage is configured.

The backend now sends enquiry email to this SMTP server. Direct development defaults to 127.0.0.1:1025; Compose uses the mailpit service hostname.

To stop the inbox:

```sh
docker compose stop mailpit
```

## Catalogue data foundation

`backend/src/catalogue.ts` defines the product schema, its inferred TypeScript type, the product-source interface and a demo implementation. The demo source reads thirteen sample products across four categories from `backend/fixtures/products.json` and validates them with Zod. Sample copy, prices and ratings are illustrative.

The HTTP routes load products through the selected source and apply the catalogue rules described below. Catalogue/detail responses expose relative backend image paths, served from bundled JPEGs in demo mode or fetched through the backend in live mode. Android requests are a separate chunk.

From `backend/`, run:

```sh
npm run typecheck
npm test
```

The test command compiles TypeScript and uses Node's built-in test runner against the compiled module. Tests cover loading the sample dataset, rejecting malformed product values and reporting missing fixture files. Fixture paths resolve relative to the module, so loading does not depend on the directory the server is launched from.

`backend/src/catalogue-rules.ts` applies catalogue rules to already validated products without file or network access:

- Home features the available editorial IDs `1, 2, 5, 7, 9` in that order. Missing picks are skipped, without replacements.
- A selected category features up to two products by descending rating, breaking ties by ascending ID. One product is reserved for the remaining list; a category with a single product has no featured section.
- Featured products are excluded from the remaining list, whose original source order is preserved.
- A nonblank search matches a case-insensitive substring in the title or description, optionally combined with a category. Search results have no featured section. Surrounding whitespace is trimmed; blank search behaves like browsing.
- Category choices always come from the full dataset, uniquely and alphabetically sorted. Unknown categories and searches with no matches return empty product lists.

The returned object contains `categories`, `featured` and `remaining`. Focused tests cover editorial selection, category ranking/ties, small categories, search, combined filters and input immutability. The UI's explicit search-submit/category-change behaviour remains a later chunk.

## Product HTTP endpoints

`backend/src/app.ts` connects the source and catalogue rules. `server.ts` selects one source at startup and injects it into `createApp(source)`. Calling `createApp()` without arguments still uses demo data for tests/local embedding. Demo requests read and validate the fixture; live requests use the cache described below.

| Request | Successful response |
| --- | --- |
| `GET /health` | `{ "status": "ok" }`; does not read product data |
| `GET /api/catalogue` | `{ "mode": "demo", "categories": [...], "featured": [...], "remaining": [...] }` |
| `GET /api/catalogue?category=electronics&q=portable` | Same shape, with combined category/search rules |
| `GET /api/products/9` | A single complete product object |
| `GET /images/9.jpg` | Product image bytes with the actual image Content-Type |

Try these against the direct server (use port 3001 instead if that is your Docker host port):

```sh
curl 'http://127.0.0.1:3000/api/catalogue?category=electronics&q=portable'
curl http://127.0.0.1:3000/api/products/9
curl http://127.0.0.1:3000/api/products/999
```

`category` and `q` must each be a single string, up to 100 and 200 characters respectively after trimming. Repeated values are rejected. Product IDs must use positive decimal integer notation without leading zeros and fit within JavaScript's safe integer range. Invalid inputs return HTTP 400 before loading products. An unknown category or unmatched search returns HTTP 200 with empty product lists; a missing product returns HTTP 404.

Errors use `{ "error": { "code": "PRODUCT_NOT_FOUND", "message": "Product not found." } }`. Other codes are `INVALID_QUERY` (400), `INVALID_PRODUCT_ID` (400), `ROUTE_NOT_FOUND` (404), and `CATALOGUE_UNAVAILABLE` (500). Source failures are logged on the server; their paths, messages and stack traces are excluded from responses. Express 5 forwards rejected async route handlers to the final four-argument error middleware. HTTP tests use a temporary loopback port and cover the real demo source, validation, empty results, missing products and injected source failures.

## Choose demo or live products

`PRODUCT_SOURCE` defaults to `demo` when unset. Set it to exactly `demo` or `live`; other values, including an explicitly empty value, fail at startup. The startup log and catalogue response's `mode` field identify the selected source.

From `backend/`, start live mode directly:

```sh
PRODUCT_SOURCE=live npm run dev
```

Or, from the repository root, start it through Compose:

```sh
PRODUCT_SOURCE=live BACKEND_PORT=3001 docker compose up --build -d --wait
```

Choose `PRODUCT_SOURCE=demo` to return to the bundled sample data. Changing the variable requires restarting/recreating the server. No credentials or new dependencies are needed.

`backend/src/product-sources.ts` implements the live adapter against [Fake Store API's documented products endpoint](https://github.com/keikaavousi/fake-store-api). It validates the upstream response and maps `image` to `imageUrl` and `rating.rate/count` to our `rating/reviewCount`. The live source retains the original upstream image URL internally. HTTP handlers return a copy with a backend image path, leaving the source/cache unchanged.

Live loading has a five-second request/body timeout and an in-memory 60-second cache, measured from successful validation. Catalogue/detail requests share the same source and cache. Concurrent cache misses share one pending request. Failed loads are not cached, and expired data is not returned when a refresh fails; the next request can retry. The existing HTTP error handler returns `CATALOGUE_UNAVAILABLE` (500). Live mode never silently substitutes demo data. `/health` checks the process independently and does not prove upstream availability.

Tests inject fetch responses and a clock to verify mapping, cache expiry, concurrent requests, failures/retries and HTTP integration without relying on external availability. During Slice 4D verification, a separate real request successfully loaded and validated 20 upstream products. This check confirms connectivity at that time, not continued upstream availability.

## Product image delivery

All catalogue/detail `imageUrl` values have the form `/images/9.jpg`. Clients resolve this relative path against the backend base URL. The image route validates the ID and finds the product in the selected source before loading any image; it does not accept arbitrary URLs or filesystem paths.

`backend/src/product-images.ts` reads demo images by product ID from `backend/public/images/`. The 13 bundled JPEG assets were reused from the earlier PoC; delivery code was implemented in this branch. Docker copies them into the runtime image, and paths resolve relative to the compiled module rather than the working directory.

In live mode the backend fetches the product's original image URL. Only HTTPS URLs on `fakestoreapi.com` under `/img/`, without URL credentials, are allowed. Redirects are rejected. A five-second timeout covers fetching/reading the body; JPEG, PNG and WebP Content-Types are accepted. Actual streamed bytes are limited to 2 MiB regardless of Content-Length; unsupported types, empty bodies and oversized responses fail explicitly. The route forwards the actual MIME type, so a stable `.jpg` URL can return PNG bytes without converting the image.

Successful responses include `X-Content-Type-Options: nosniff` and `Cache-Control: public, max-age=60`. There is no server-side image cache or disk download in live mode. Unknown products return `PRODUCT_NOT_FOUND` (404); malformed IDs return `INVALID_PRODUCT_ID` (400). Image loading failures return `IMAGE_UNAVAILABLE` with 500 for missing/unreadable demo files or 502 for live image failures. Failures while loading the product list retain `CATALOGUE_UNAVAILABLE` (500). No placeholder image or demo fallback is substituted.

Open `http://127.0.0.1:3000/images/9.jpg` in a browser, adjusting the port if using Docker on 3001. Tests verify all 13 demo image responses, live proxying, response URL mapping without cache mutation, URL/type/size limits and failure responses. A separate real live HTTP check returned product 1's image through `/images/1.jpg` with HTTP 200 and image/png (273,291 bytes).

## Android catalogue contract

The `com.cataloguestudio.domain` package is pure Kotlin, with no Android, Compose, Ktor or serialization imports:

- `Models.kt` defines read-only product/catalogue properties and a typed `CatalogueMode` (`DEMO` or `LIVE`). The catalogue carries categories, featured and remaining products. IDs use `Long` so they can represent the backend's safe-integer IDs. Image URLs are strings; the data mapper resolves backend-relative paths before returning products to domain callers.
- `Result.kt` defines our two-parameter `Result<Data, Error>` with `Success(data)` and `Error(error)`, plus `DataError` cases for connection, timeout, invalid request, missing product, server, decoding and unknown failures. This is distinct from Kotlin's built-in `Result<T>`, which holds a Throwable on failure. The data layer's safeRequest helper translates HTTP/transport/decoding failures into these cases.
- `CatalogueRemoteDataSource.kt` defines suspend functions for `getCatalogue(category, query)`, `getProduct(id)` and the future `sendEnquiry(enquiry)`. A null category/query means no filter. Sending returns `Result<Unit, DataError>` because success needs no domain payload. `suspend` allows coroutine callers to await an implementation; it does not automatically move work to a background thread.

The intended flow is ViewModel → `CatalogueRemoteDataSource` → typed `Result` → UI state. `KtorCatalogueDataSource` now implements the contract using the shared client, DTO mappers and typed error handling. The UI will use an exhaustive `when` over success/error. Only tests construct/call the implementation so far; no repository or use-case wrapper is added for this single backend source.

Slice 5A introduced the catalogue contract. Slice 5B adds the enquiry contract and tested local validation below; networking is still separate.

## Android enquiry validation

`domain/Enquiry.kt` carries `productId`, `name`, `email` and an optional `message` (default empty). `domain/EnquiryValidator.kt` provides independent name/email/message validators. Each returns one `Result<String, EnquiryValidationError>`: success contains trimmed text, and failure contains a field-specific error. The future form can run all three validators together to show errors beside each field, then build an Enquiry from successful values. Constructing the data class alone does not validate it or send anything.

| Field | Local rules |
| --- | --- |
| Name | Required, up to 100 characters after trimming; no control characters, including CR/LF, tabs or NUL. International names, spaces, apostrophes and hyphens are allowed. |
| Email | Required, up to 254 characters after trimming; practical address format with a dotted domain, local part up to 64 characters and domain labels up to 63. No controls or internal whitespace; plus addressing and subdomains are supported. Case is preserved. This does not check mailbox existence. |
| Message | Optional, up to 2,000 characters after trimming; internal line breaks are preserved. |

Lengths use Kotlin String length (UTF-16 code units), matching the planned backend string limits. Control checks use the raw name/email so trimming cannot silently hide leading/trailing newlines. The backend enquiry endpoint will validate requests independently in Slice 12; the form and email sending are not wired yet.

JUnit 5 and AssertK are test-only dependencies managed by the version catalog; Android JVM unit tests use JUnit Platform. No emulator or backend is needed for validation tests. From `android/`, run:

```sh
./gradlew testDebugUnitTest --console=plain
```

Tests cover required fields, normalization, international names, malformed/header-like email input, single-line contact fields, optional/multiline messages and exact length boundaries.

## Android catalogue JSON mapping

Slice 6A adds `data/CatalogueDtos.kt` and `data/CatalogueMappers.kt`. DTOs describe the existing catalogue/product HTTP response fields and use `@Serializable`; the Kotlin serialization plugin generates their decoding code. Domain classes remain free of serialization annotations. DTOs/mappers are `internal` to the app module. Presentation will use the domain contract; package separation within this single module is by convention.

The shared `catalogueJson` configuration ignores unknown additive fields while requiring all declared fields to be present and non-null. Malformed JSON or incompatible field shapes fail decoding rather than producing placeholder/default products. DTO decoding is not a replacement for the backend's semantic product validation.

Mappers preserve the backend's category/product grouping and order. Only exact `demo` and `live` values map to `CatalogueMode`; unknown modes raise `SerializationException`. Product image paths must start with `/images/`; mapping resolves them against the configured backend URL, preserving scheme/host/port and handling base URLs with or without a trailing slash. For example, `/images/9.jpg` becomes `http://10.0.2.2:3000/images/9.jpg`. External/protocol-relative image URLs and malformed paths are rejected. A malformed image path or unknown mode will be mapped to a typed decoding failure when request handling is added.

Mapping test flow: JSON string → `catalogueJson.decodeFromString<...Dto>()` → `toCatalogue(baseUrl)` / `toProduct(baseUrl)` → domain model. These tests run on the JVM without network access or an emulator. The Kotlin serialization plugin matches the existing Kotlin version; its JSON runtime version is pinned in the version catalog.

## Android HTTP client and typed failures

Slice 6B adds `data/CatalogueHttpClient.kt` and `data/SafeRequest.kt`. The client factory accepts a base URL and an injected Ktor engine, configures JSON request/response handling using `catalogueJson`, and sets a 15-second request timeout per attempt, five-second connect timeout and 15-second socket inactivity timeout. Redirect following and automatic exceptions for HTTP error statuses are disabled, so the request helper handles statuses explicitly.

`safeRequest(execute, decode)` runs an HTTP call, checks its status and decodes/maps successful responses inside one guarded block. `KtorCatalogueDataSource` supplies both lambdas. An empty success can use `decode = { Unit }`. Error bodies are not decoded as success DTOs.

| Failure | Domain error |
| --- | --- |
| HTTP 400 | BAD_REQUEST |
| HTTP 404 | NOT_FOUND |
| HTTP 408/504, Ktor request/connect/socket timeout | REQUEST_TIMEOUT |
| Other HTTP 5xx | SERVER_ERROR |
| Malformed/incompatible JSON, unsupported response type or mapper SerializationException | SERIALIZATION |
| DNS/connection/I/O failure | NO_INTERNET (a connection failure classification, not proof the device is offline) |
| Other status/exception | UNKNOWN |

Coroutine cancellation is rethrown rather than converted into an error result. The helper also checks that the caller's coroutine remains active when an engine throws another exception during cancellation. The helper itself does not retry; the client now allows the single read-only transport recovery described below.

Tests use [Ktor MockEngine](https://ktor.io/docs/client-testing.html) and a coroutine test dispatcher. They exercise the actual client configuration, JSON decoding/mapping, status handling, connection failures, cancellation and a delayed request timeout using virtual time. Tests close the client and injected engine. This is not a real socket, Android engine or backend connectivity check. Slice 6D supplies the production engine and Koin/Application wiring described below.

## Android endpoint implementation

Slice 6C adds `data/KtorCatalogueDataSource.kt`, implementing the domain interface with an injected shared HttpClient and backend base URL. The same base URL must be supplied to the client factory and this source so requests and resolved image URLs target the same backend.

| Domain call | HTTP request | Successful domain result |
| --- | --- | --- |
| `getCatalogue(category, query)` | GET `/api/catalogue`, with `category` and `q` parameters | Catalogue mapped from CatalogueDto |
| `getProduct(id)` | GET `/api/products/{id}` | Product mapped from ProductDto |
| `sendEnquiry(enquiry)` | POST `/api/enquiries` with JSON body | Unit after a successful HTTP status |

Null filters are omitted; non-null values (including a blank query) are forwarded through Ktor's parameter builder, which handles encoding. Explicit search submission remains a later presentation concern. The data source does not reimplement filtering/ranking or retry failed submissions. Each method uses safeRequest for status handling, decoding/mapping failures and cancellation.

`data/EnquiryRequestDto.kt` maps the domain Enquiry into a serializable request with exactly productId/name/email/message. The empty optional message is included explicitly. This transport mapping preserves the supplied values; the future form uses EnquiryValidator to produce normalized fields, and the backend will validate independently.

The backend now implements POST `/api/enquiries` in Slice 12. Earlier Android enquiry protocol tests use simulated 201 responses; Slice 12 separately verifies real local SMTP acceptance and Mailpit capture.

Endpoint tests verify methods/paths, absent and combined filters, special-character query encoding, domain response mapping, exact enquiry JSON/escaping, optional empty message, server failures without retry, mapper failures and cancellation. These endpoint tests use simulated responses. Slice 6D wires the production engine; screens do not call the data source yet.

## Android dependency wiring

Slice 6D adds `CatalogueStudioApplication.kt` and `data/di/CatalogueDataModule.kt`. The manifest registers the Application so Android runs its `onCreate()` before the Activity. It starts Koin once with the application context and the data module, using `BuildConfig.BASE_URL` for both HTTP requests and image mapping. Dependencies use Koin 4.1.1; the Android transport is Ktor's OkHttp engine, matching Ktor 3.3.1. `gradle.properties` gives the Gradle build process a 2 GiB maximum heap and 512 MiB metaspace limit after dependency dex merging exceeded the default 512 MiB heap; these settings do not affect app runtime memory.

The module registers three lazy singletons: HttpClientEngine, HttpClient and CatalogueRemoteDataSource (backed by KtorCatalogueDataSource). Registration sends no requests. When a future ViewModel requests the domain interface, Koin resolves and reuses its client and engine. The module captures the base URL directly instead of registering an unqualified String dependency. Factory lambdas are needed here to call the client/engine factories and pass that configuration.

OkHttp connection retries remain explicitly disabled so enquiry submissions cannot be replayed by the engine. The client-level recovery policy below allows one retry only for GET transport I/O failures. Redirects remain disabled and the existing JSON/timeouts apply. The module owns the supplied engine as well as the client: `HttpClient(engine)` does not own an externally supplied engine, so both have `onClose` callbacks when Koin closes. In Android these objects live for the app process; no Activity closes them, and no Application.onTerminate cleanup is assumed.

The optional engine factory parameter lets isolated JVM tests substitute MockEngine without changing production bindings. Tests verify lazy creation, singleton reuse, the shared request/image base URL and resource closure. A separate test resolves the default OkHttp engine through the same Koin module and makes real HTTP requests to a temporary loopback MockWebServer, checking catalogue decoding and 404 handling. MockWebServer is test-only and matches the OkHttp version used by Ktor. This verifies the production transport on the JVM; it does not verify emulator networking or Android Application startup on a device.

The placeholder screen still makes no requests. Slice 8D adds the catalogue ViewModel binding as described below; UI consumers follow later. Enquiry backend delivery remains Slice 12.

## Android product display mapping

Slice 7A adds `presentation/ProductUi.kt` and `presentation/UiText.kt`. A future ViewModel will call `Product.toProductUi()` after receiving domain products, then pass the resulting display models to Compose components. The mapper preserves identity, title, description, image URL, raw category and review count. It formats price as explicit USD with cents/grouping (64.99 → $64.99) and rating with one decimal place (4 → 4.0), using Locale.US independently of the device format locale. This is display formatting, not currency conversion or price arithmetic. Formatters are created per mapping call rather than sharing mutable NumberFormat instances.

Known category labels live in Android string resources. `toCategoryLabel()` returns a `UiText.StringResource` for the four current API categories; unknown categories become `UiText.DynamicString` with their original text. For example, the API's `jewelery` displays as “Jewellery” but remains `jewelery` in the raw category field used for filtering. UiText holds a resource ID or dynamic text without needing a Context; the UI resolver described below turns it into display text.

Six JVM tests cover preserved fields, USD cents/grouping/rounding, rating formatting, independence from German/French default format locales, known category resources and unknown-category fallback. No new dependencies or requests are introduced. The app still shows its placeholder; ViewModels and Compose components will consume these models in later chunks.

## Android text resolution and error messages

Slice 7B adds `presentation/ErrorUiText.kt` and `ui/UiText.kt`. The two `toUiText()` extensions map every DataError and EnquiryValidationError to a string resource. Their exhaustive `when` expressions require a mapping if a new enum value is added. Messages describe connection, timeout, request, missing-item, service and response failures, plus field validation. A connection failure does not claim the device is definitely offline. Validation wording avoids duplicating numeric limits from EnquiryValidator.

A future ViewModel will store the mapped UiText in screen state. The Compose UI calls `message.asString()`: DynamicString returns the supplied text; StringResource uses Compose's `stringResource(id)` for the current configuration. Resources currently have English values. No Context enters the ViewModel, and resolved resource strings are not cached in state. Existing category labels use the same resolver.

The compiler/resource build checks the exhaustive mappings and resource references; the existing JVM suite runs unchanged. No dependencies or tests that merely duplicate the mapping table were added. Device rendering is not verified in this chunk. Shared components and screen consumers follow in later chunks, so the placeholder screen remains the same.

## Product cards and image states

Slice 7C adds `ui/ProductCard.kt` and `ui/ProductImage.kt`. ProductCard accepts a ProductUi, a click callback and a Modifier. It displays the resource-backed category, a two-line title, formatted USD price, rating and plural-aware review count. The full title stays in accessibility text when visually ellipsized. The card forwards clicks to its caller; it does not navigate or fetch catalogue data.

ProductImage uses Coil 3.3.0 with its OkHttp network module and ContentScale.Fit so product images keep their proportions. Its image area stays the same size while loading, succeeding or showing “Image unavailable”. Loading/error content uses our theme and string resources; a failed image does not replace product information or disable the card. Card images are decorative for accessibility because the adjacent title identifies the product; standalone callers can supply a content description. The star icon is decorative, while a “Rated … out of 5” semantic label explains the rating. Coil manages image requests separately from the Ktor JSON data source. The small catalogue uses SubcomposeAsyncImage for clear loading/error slots; reconsider subcomposition if list scale/performance warrants it.

The debug-only `ui/ProductCardGalleryActivity.kt` provides a review gallery and three Android Studio previews: standard phone, 320 dp width and larger text. Previews use a bundled debug fixture copied from the existing backend image, with a forced missing-image response; they need no backend. Running the gallery on a device uses the configured backend URL for a real image and an intentionally missing product image, plus a standalone loading state. Card taps show a confirmation toast. The gallery, preview image and preview tooling are excluded from release builds.

To review the gallery, start the backend, install the debug APK, then run (replace the serial as needed):

```sh
adb -s emulator-5554 shell am start -n com.cataloguestudio/.ui.ProductCardGalleryActivity
```

The normal launcher still shows the placeholder. The catalogue screen and its ViewModel follow in their own slices. This chunk was checked on an emulator at 390 dp and 320 dp widths and at 1.5x text size, including real backend image loading, a missing image and a card tap. The existing 51 JVM tests, debug/release builds and lint passed (23 dependency/configuration/manifest warnings, no source errors). Release APK inspection confirmed the gallery class and preview image are absent. Emulator size/density/font overrides were restored after the checks.

## Shared catalogue states and summary

Slice 7D adds `ui/StateComponents.kt` with LoadingState, ErrorState and EmptyState. The caller supplies a UiText message; ErrorState forwards its retry button to onRetry, and EmptyState only shows a clear-filters button when an onClearFilters callback is provided. This lets a filtered empty result offer a reset while a genuinely empty catalogue has no misleading filter action. Messages use the shared resolver, state titles have heading semantics, and changing status text uses a polite accessibility live region. Layouts grow with text rather than fixing a panel height.

`ui/CatalogueSummary.kt` displays a supplied source label and plural-aware product/featured counts. It does not choose a source, calculate counts or apply catalogue rules. Its FlowRow lets count labels wrap at narrow widths or larger font sizes. The future ViewModel/Screen will supply current results and decide which state to render.

The debug-only StateGalleryActivity displays loading, server failure, filtered empty and unfiltered empty examples, plus demo/nonempty and live/zero-count summaries. Retry and clear-filter taps show confirmation toasts without issuing requests or changing filters. Three debug previews cover a standard phone, 320 dp width and 1.5x text. Open it from “Review catalogue states” in the product-card gallery, or launch directly:

```sh
adb -s emulator-5554 shell am start -n com.cataloguestudio/.ui.StateGalleryActivity
```

The normal launcher still shows the placeholder. These components do not select application state or perform network calls; the catalogue ViewModel comes next in Slice 8. Verification passed: all 51 existing JVM tests, debug/release APK builds and lint (23 dependency/configuration/manifest warnings, no errors). Manual emulator checks covered 390 dp and 320 dp widths, 1.5x text, both button callbacks and the unfiltered state without a reset action. Release APK inspection confirmed StateGalleryActivity is absent. Emulator display/font overrides were restored after checks.

## Catalogue presentation contract

Slice 8A adds `presentation/CatalogueState.kt`, `CatalogueAction.kt` and `CatalogueEvent.kt`. These define the inputs/outputs for the catalogue ViewModel; the contract types themselves do not handle actions or send requests.

- CatalogueState holds draft query, submitted query, selected raw category, labelled category choices, featured/remaining ProductUi lists, optional confirmed source mode, loading and UiText error. Initial state is loading with mode unknown, so it does not imply demo mode or an empty catalogue before a response. CategoryUi keeps a raw filter value separate from its display label.
- CatalogueAction describes query edits, explicit Search, clear search, category selection, clear all filters, retry and product clicks. A null category means all categories. Clear search and clear all filters are separate intents so the ViewModel can handle them explicitly.
- CatalogueEvent currently contains NavigateToProduct(productId). Navigation will be a one-time event consumed by the future Root, rather than a persistent flag in state or a data-source side effect.

The intended flow is Screen action → ViewModel handles it → StateFlow publishes a new CatalogueState → Screen renders shared components. A product click emits an event for the future Root to navigate. Slices 8B–8D add initial loading, actions, cancellation, SavedStateHandle and DI registration as described below. Screen wiring follows later.

The state derives productCount from both result sections, hasActiveFilters from the selected category/submitted query, and isEmpty only when loading has finished, no error exists and both product sections are empty. For example, editing query to “headphones” while submittedQuery is “drive” leaves the active search as “drive” until Search is handled. Editing alone is not an applied filter. Read-only lists are used as state snapshots; callers must replace lists rather than mutate their backing collections. Data-class copy is shallow.

Five focused state tests cover loading/failure versus empty results, products in either section, draft versus applied search, independent category/search filters and derived counts across copies. These tests verify state interpretation, not HTTP orchestration; ViewModel tests will verify requests and cancellation when that code exists. No new dependencies or visible app changes are introduced.

## Catalogue ViewModel initial load

Slice 8B adds `presentation/CatalogueViewModel.kt`. Its constructor receives the domain CatalogueRemoteDataSource interface. It owns a private MutableStateFlow initialized with CatalogueState and exposes a read-only StateFlow via asStateFlow(). On creation, init launches one unfiltered getCatalogue() request in viewModelScope. Collecting state does not start additional requests.

For success, the ViewModel maps category values/labels and both product groups through the existing presentation helpers, preserving backend order and grouping. It publishes the confirmed mode, mapped lists and isLoading=false in one `.update { it.copy(...) }` operation. For a typed failure, it stops loading and publishes the mapped UiText error. Empty success and failure remain distinct. Expected network/decoding failures already arrive as Result.Error from the data source; cancellation remains an exception and is not turned into a UI error. The ViewModel adds no automatic retries or fallback products; client-level read recovery is described below.

viewModelScope ties the request to the ViewModel's lifetime, so clearing its ViewModelStore cancels a pending load. No dispatcher is injected because the ViewModel uses its standard main scope and the suspend data-source API. Lifecycle ViewModel 2.9.4 is declared directly; the shared coroutine test library was already present, and Turbine 1.2.1 is test-only.

Five focused JVM tests use an injected fake source, deferred responses, a test Main dispatcher and Turbine. They verify loading → mapped success, typed failure, empty live success, a single request shared by two collectors, and cancellation on ViewModelStore.clear with no late state/error. Tests close the ViewModelStore and restore Main. They make no real backend requests and do not verify screen rendering. Full verification passed: 61 JVM tests, debug APK build and lint (24 dependency/configuration/manifest warnings, no errors).

Only tests create the ViewModel so far. Slices 8C/8D add actions, saved state and Koin registration as described below. The launcher and galleries keep their existing behaviour.

## Catalogue actions and request cancellation

Slice 8C adds `CatalogueViewModel.onAction()`. The future Screen will pass button/text actions to this method; today JVM tests call it with a fake source. The flow is action → update draft or start request → CatalogueRemoteDataSource → typed result → mapped CatalogueState → StateFlow collectors. The backend owns matching, featured selection and result ordering; the ViewModel does not filter products locally.

| Action | Behaviour |
| --- | --- |
| Query change | Changes only the draft `query`; no request or change to applied filters/cards. |
| Search | Trims the draft into `submittedQuery` and requests it within the selected category. Whitespace-only text omits the API query and restores browsing. |
| Category selection | Selects the raw category (null means all), clears both draft/submitted search and loads that category's featured/additional products. |
| Clear search | Clears draft/submitted search and reloads the current category. |
| Clear filters | Clears category and both search values, then loads unfiltered home. |
| Retry | Repeats the submitted filters; an edited but unsent draft is preserved and not applied. |
| Product click | Sends NavigateToProduct(id) through a Channel exposed as a flow; no request or catalogue-state change. |

**Assumption:** category links start category browsing and clear any search. Explicit Search is scoped to the currently selected category. Clearing search keeps that category; clearing all filters returns home. These choices fill gaps in the acceptance criteria and can be changed in this one action handler.

Every load cancels `loadJob`, then publishes the requested filters, empty product lists, loading=true and error=null together. This prevents old cards appearing under new filters. Existing category choices and the last confirmed source mode remain available during loading/failure. Each coroutine captures its request arguments. After the suspend source returns, `ensureActive()` prevents a cancelled request from publishing even if its source returns a late success/error. Cancellation remains an exception, not a user-facing failure; the ViewModel does not retry automatically. Success updates only response fields, preserving draft edits made while waiting.

The small product-event handler was brought forward from 8D to keep action handling exhaustive without an unfinished branch. The Channel delivers each event to one collector and does not replay a consumed event. A send waits for a collector and is cancelled when the ViewModel is cleared. The future Root will collect events and navigate; this chunk adds no actual screen navigation or saved-state restoration.

Focused tests use separate deferred responses for each request. They cover explicit search versus typing, request normalization, category/search clearing, submitted-filter retry, cooperative cancellation, out-of-order success/error from a source that ignores cancellation, lifecycle cancellation with both source behaviours, and a single navigation event without state mutation or reload. No real backend or emulator is needed for these controller tests. Verification passed: 71 JVM tests (15 CatalogueViewModel tests), debug APK build and lint (24 warnings, no errors). No new dependencies were added.

## Catalogue saved inputs and ViewModel wiring

Slice 8D injects `SavedStateHandle` into CatalogueViewModel. Its initial CatalogueState reads three keys: `catalogue.query` (draft), `catalogue.submittedQuery` (applied search), and `catalogue.selectedCategory` (raw category or null). Draft changes save the draft immediately. Every load saves its three input values before launching the request, so Search, category selection and both clear actions also update the saved inputs. Retry preserves an unsent draft.

A newly created ViewModel starts in loading state and requests the restored category/submitted query once. It never submits the draft automatically. Products, category choices, mode, errors, Jobs and navigation events are not saved; products are fetched again. SavedStateHandle uses Android's saved-instance-state mechanism, not a database or durable preference store. Restoration depends on the lifecycle owner saving/restoring state; it is not a promise to retain inputs after force-stop, task removal or a fresh launch.

`presentation/di/CataloguePresentationModule.kt` declares `viewModelOf(::CatalogueViewModel)`. CatalogueStudioApplication assembles this presentation module alongside the existing data module. Registration is lazy and makes no request. A future Root will call `koinViewModel()`: its lifecycle owner supplies creation extras, Koin creates the owner's SavedStateHandle and resolves the shared CatalogueRemoteDataSource, then calls the ViewModel constructor. The owner retains the ViewModel across configuration changes; a new ViewModel created after restoration reads the saved inputs and reloads. SavedStateHandle is not registered as an app singleton, and no ViewModel is created in Application.onCreate.

Lifecycle viewmodel-savedstate 2.9.4 is now a direct dependency because production code imports SavedStateHandle; it was already present transitively. No new library version or DI framework is introduced. The product-click event handler was completed in 8C.

Five additional ViewModel tests verify prefilled restoration, action → saved-input snapshot → new handle/new ViewModel, unsent versus applied queries, and persistence of clear-search/category-change/clear-all. They confirm that results, errors and events do not survive recreation. Two isolated Koin tests verify lazy registration and constructor resolution with distinct supplied handles and a shared fake source. Those tests supply handles explicitly and manage ViewModelStores; they do not exercise Android's Bundle serialization or Koin's lifecycle-owner creation extras. Slice 9A's debug host also checks restoration after background process termination on an emulator; production Root restoration checks remain later. Verification passed for 8D: all 78 JVM tests, debug APK build and lint (25 dependency/configuration/manifest warnings, no errors). The extra warning compared with 8C is the newly declared saved-state dependency's version advisory.

## Catalogue controls

Slice 9A adds `ui/CatalogueControls.kt`: the brand/introduction, search field, explicit Search button and horizontal category chips. It accepts query, selected raw category, labelled category choices and an action callback. It holds no application state and makes no requests. Query edits forward OnQueryChange; the button and keyboard Search both forward OnSearch and dismiss the keyboard. The clear icon forwards OnClearSearch. All products passes a null category, while each chip passes its raw backend value. FilterChip exposes selected semantics; the clear icon has a resource-backed accessibility label. The search button sits below the field so both have space on narrow phones and with larger text. Chip labels scroll horizontally without truncation. There are no fixed component heights.

`src/debug/.../CatalogueControlsGalleryActivity.kt` is a temporary review host with the real ViewModel: Android's Koin Activity delegate supplies its SavedStateHandle, collectAsStateWithLifecycle observes state, and method-reference callbacks dispatch actions. The flow is field/button/chip → ViewModel → data source/backend → StateFlow → controls and result-count/loading/error/empty feedback. The review host displays the submitted term separately so draft edits are easy to distinguish from applied search. It does not render product cards yet; product sections and production Root/Screen wiring are subsequent chunks. Debug previews cover 390 dp, 320 dp and 1.5x text, with an active search/category and a long category label. Previews do not create a ViewModel or make requests.

Lifecycle runtime-compose 2.9.4 is declared as a debug dependency for this host; it can move to main when production Root wiring needs it. The gallery Activity, its diagnostic strings and previews are debug-only. The launcher is unchanged. Start the local backend, build/install the debug APK, then launch the review screen explicitly (replace the serial with your chosen emulator):

```sh
adb -s emulator-5554 shell am start -n com.cataloguestudio/.ui.CatalogueControlsGalleryActivity
```

Verification for 9A passed: all 78 existing JVM tests, debug/release APK builds, lint (26 dependency/configuration/manifest warnings, no errors) and whitespace checks. Release APK inspection confirmed the review Activity and its diagnostic resource are absent. Manual emulator checks covered typing without submission, explicit button/keyboard search, filtered empty results, category selection, clearing search while retaining category, and clearing all filters. Home showed 13 products; a submitted “drive” search showed two. Backgrounding, terminating the process and reopening restored draft “drivex” separately from submitted “drive”, which reloaded its two results. Layout checks covered 390 dp, 320 dp and 1.5x text, including horizontal scrolling to longer category labels. Emulator size/density/font settings were restored afterward.

**9A follow-up:** some catalogue requests after idle time returned the existing connection error even while the backend was reachable. Explicit Retry or resubmitting recovered; some of the original manual checks above required this. The separate networking change below addresses this failure without changing the controls or enquiry retry policy.

## Read-only transport recovery

The networking follow-up reproduced `IOException: unexpected end of stream` in OkHttp while reading response headers after an idle period. This is consistent with reuse of a connection closed by the backend during its idle timeout. Temporary diagnostic stack logging was removed after capturing the failure. A deterministic socket-level regression test warms a connection, closes it after the next request arrives but before response headers, and verifies recovery on a new connection. That test failed with the original client configuration and passes with the fix.

`CatalogueHttpClient.kt` now installs Ktor's existing HttpRequestRetry plugin before HttpTimeout. A GET that throws a transport IOException during request dispatch can be repeated once immediately (two attempts maximum). Connect/socket/request timeouts and coroutine cancellation are not retried. HTTP error responses, redirects and JSON/domain decoding failures are not retried. Request arguments are preserved. The 15-second request timeout applies to each attempt, not a combined deadline. This is a bounded transport recovery inside the client, not another ViewModel action or a polling loop.

OkHttp's general automatic retries remain disabled. POST and other non-GET methods are excluded from the Ktor retry predicate. This matters for enquiries: if the server receives a submission and the acknowledgement is lost, the client cannot know whether an email was sent. It must return a typed failure and let the user retry explicitly. There is no automatic enquiry resend or claim of exactly-once delivery. The real backend enquiry endpoint still follows in Slice 12.

Four production-engine tests use a temporary loopback MockWebServer: GET recovery on a new connection with category/query preserved, a second transport failure stopping at two attempts, a received enquiry body not being replayed after a lost acknowledgement, and a 503 response not being retried. Existing MockEngine timeout/cancellation tests now also assert a single attempt. No dependencies, backend code, ViewModels or UI were changed.

Verification passed: all 82 JVM tests, debug APK build, lint (26 existing dependency/configuration/manifest warnings, no errors) and whitespace checks. On the emulator against the local backend, four sequential checks each waited seven seconds before requesting again: drive search (two products), Electronics browsing (two), all-category home (13), and blank search (13). All completed without manual retry or a visible connection error. This checks the reproduced idle scenario rather than every network failure. Emulator size/density settings were restored and the owned emulator stopped after verification.

## Catalogue product sections

Slice 9B adds `ui/CatalogueProductSections.kt`. Its `LazyGridScope.catalogueProductSections(...)` extension appends a featured heading/carousel and remaining-product heading/cards to the parent's grid. It accepts the two ProductUi lists, resource-or-dynamic section titles and the action callback. It does not fetch, sort, filter or deduplicate products: the backend already supplies disjoint ordered groups. Empty groups add no heading or placeholder. Search therefore renders its result grid without a featured section.

Headings and the carousel span `maxLineSpan`, so they use the full row regardless of the column count. Featured cards use a horizontal LazyRow, while remaining cards are individual lazy-grid items. This keeps a single vertical scroll container; the horizontal row can be nested safely. Stable product IDs identify cards, prefixed grid keys avoid collisions with section keys, and contentType separates headings/carousel/cards for reuse. Featured-card width grows with font scale and is capped at the available width. The parent owns adaptive columns and overall padding/scrolling; the debug host uses a 156-dp minimum card width scaled with font size, producing two columns at 390 dp and one at 320 dp or 1.5x text.

`src/debug/.../CatalogueProductsGalleryActivity.kt` obtains the existing ViewModel from Koin and collects its StateFlow with lifecycle awareness. Debug controls load home, Electronics, or a drive search. ProductCard → OnProductClick(id) → ViewModel Channel event → the Activity's STARTED lifecycle collector → a confirmation toast containing the ID. This verifies both card-section callbacks without adding navigation. Three themed previews use fixed ProductUi fixtures and a bundled debug image, with no preview requests. The host, diagnostic strings and previews are debug-only; the normal launcher and production Root/Screen wiring remain later work.

With the backend running and the debug APK installed, open the product review screen explicitly:

```sh
adb -s emulator-5554 shell am start -n com.cataloguestudio/.ui.CatalogueProductsGalleryActivity
```

Verification passed: all 82 existing JVM tests, debug/release APK builds, lint (26 existing dependency/configuration/manifest warnings, no errors) and whitespace checks. Release APK inspection confirmed the product-review Activity and its diagnostic resource are absent. On the emulator, all five featured IDs appeared in backend order (1, 2, 5, 7, 9), and the grid showed the eight remaining IDs in order (3, 4, 6, 8, 10, 11, 12, 13) without featured duplicates. Real local images loaded. Electronics displayed one category pick and one remaining product; drive search displayed only IDs 9/10 and no featured heading/carousel. Featured and grid taps produced ID 1 and ID 13 confirmations. Layout checks confirmed two grid columns at 390 dp, one at 320 dp and one with 1.5x text, plus a featured card that fits the larger-text viewport. Emulator size/density/font settings were restored and the owned emulator stopped. No new dependencies, backend changes or ViewModel changes.

Next is 9C: combine controls, summary, state feedback and these sections into CatalogueScreen/Root. Application entry and destination navigation remain Slice 10.

## Complete catalogue screen

Slice 9C adds `ui/CatalogueScreen.kt`, with `CatalogueRoot` and `CatalogueScreen` in the same file. Root gets the existing ViewModel through `koinViewModel()`, collects state with `collectAsStateWithLifecycle()` and forwards `NavigateToProduct(id)` through its callback. `ObserveAsEvents.kt` collects events while the owner is STARTED; `rememberUpdatedState` supplies the latest callback without restarting the collector after recomposition. The collector is cancelled when the Root leaves composition. This small lifecycle helper moved forward from Slice 10 because the complete Root needs to handle product clicks now. NavController, routes and the normal launcher remain Slice 10.

Screen accepts only CatalogueState and onAction. One LazyVerticalGrid contains the controls, full-width feedback/summary and the reviewed product sections. It owns grid scrolling, keyboard/system-bar insets, a centred maximum width of 1100 dp and font-aware adaptive card columns. The hero's Explore products button animates to the feedback/result-summary item, which always exists. CatalogueControls now leaves outer padding to its parent, avoiding double padding inside this grid; its earlier debug gallery was adjusted accordingly. Business state and requests still belong to the ViewModel/source. Existing backend ordering and featured grouping are preserved.

Loading and error states replace the result summary/cards so they cannot claim a zero-product result or a previous source mode. Confirmed results show their source/counts; search labels use submittedQuery rather than a new draft. Filtered empty results offer Clear filters; an unfiltered empty catalogue does not. Retry, clear, search, category and card actions all use the existing action contract. Koin Compose ViewModel 4.1.1 is the only new library; lifecycle-runtime-compose 2.9.4 is now a main dependency for Root and the event helper.

With the backend running and the debug APK installed, open the complete screen:

```sh
adb -s emulator-5554 shell am start -n com.cataloguestudio/.ui.CatalogueScreenGalleryActivity
```

This debug host runs the actual Root and confirms selected IDs by toast until navigation exists. For deterministic whole-screen layout review, restart it with `--es scenario loading`, `error`, `empty` or `filtered-empty` (add `-S` to the `am start` command to restart). These fixed scenarios do not make requests: Retry/Clear filters only show an action-confirmation toast. A normal launch without scenario extras uses real backend state/actions. Six themed previews cover populated, narrow, larger-text, loading, error and filtered-empty layouts; populated previews use a bundled debug image. The host, fixtures and previews are absent from release builds.

Verification: all 82 existing JVM tests and debug/release assembly passed. Lint reported no errors and 27 warnings (the additional warning is a newer-version notice for Koin Compose ViewModel). Release inspection confirmed that the review Activity, fixed states and previews are excluded. Emulator checks covered real home loading, Explore scrolling, product ID 1 callback, submitted drive search versus an unsent drivex draft, filtered-empty reset, Electronics picks, and deterministic loading/error/empty layouts with Retry/Clear callback confirmations. Layout bounds checks covered two columns at 390 dp, one at 320 dp/1.5x text and the 1100-dp tablet width cap. Larger-text/tablet screenshots were captured but not visually reviewed. Emulator settings were restored and the owned emulator stopped. The actual disconnect/reconnect retry walkthrough remains for final testing; fixed error scenarios confirm layout/dispatch only.

The author has one hour remaining and requested minimal per-chunk checks. Continue with one small implementation chunk and a quick compile; reserve broad regression, device/layout matrices and complete failure walkthroughs for Slice 15. Slice 9C awaits author review; next is a small Slice 10 navigation/app-entry chunk.

## App entry and typed navigation

Slice 10 adds `presentation/navigation/CatalogueRoutes.kt`: CatalogueRoute has no arguments, while ProductDetailRoute and EnquiryRoute carry only a Long productId. Kotlin serialization provides the route contract; Navigation Compose 2.9.5 encodes/decodes arguments without hand-built path strings. The destination will load its own product data rather than receiving a whole ProductUi object.

MainActivity → StudioTheme → CatalogueApp → rememberNavController/NavHost → catalogueGraph → CatalogueRoot. CatalogueRoot's existing lifecycle event collector invokes onNavigateToProduct(id), and the graph navigates to ProductDetailRoute(id). Its temporary shell decodes the ID with toRoute; Enquire forwards that ID to EnquiryRoute. Both shells offer Back via popBackStack. launchSingleTop avoids an additional top entry when the same destination/arguments are opened repeatedly. NavController stays in app/graph wiring; neither the catalogue Screen nor its ViewModel depends on it. Koin now scopes the catalogue ViewModel to its navigation back-stack entry, preserving catalogue state while that entry remains in the stack.

The detail/enquiry shells show a title and product reference to make argument flow reviewable. They do not fetch product details, submit an enquiry or claim success. Slice 11 replaces the detail shell; Slice 14 replaces the enquiry shell. Normal launcher verification and complete forward/back/restoration checks are deferred to Slice 15 under the author's time constraint; Kotlin compilation (`:app:compileDebugKotlin --offline`) and whitespace checks passed. No test suite or emulator walkthrough was run for this chunk.

## Product details

Slice 11 replaces the product-detail navigation shell. `presentation/ProductDetailState.kt` holds state/actions/events, and `ProductDetailViewModel.kt` reads the productId using SavedStateHandle.toRoute<ProductDetailRoute>(). Navigation owns and restores the scalar argument. The ViewModel calls source.getProduct(id), maps a successful Product to the reviewed ProductUi, or maps typed failures (including not-found) to UiText. Retry cancels an earlier load, clears stale data and requests the same ID. Cancellation is checked before applying the result. Only a loaded product can emit NavigateToEnquiry(product.id); Back emits NavigateBack. Koin registers the ViewModel alongside CatalogueViewModel.

In `ui/ProductDetailScreen.kt`, ProductDetailRoot lifecycle-collects state/events and forwards navigation callbacks to the graph. ProductDetailScreen accepts state/onAction, renders shared loading/error/image components and shows category, full title, price, rating/review count and description. Product content scrolls within the space left by Scaffold's top and bottom bars; the Enquire action stays outside that scroll area. It appears only for loaded content, so failures offer Retry rather than an enquiry for an unavailable product. The layout has system-bar insets and an 840-dp content-width cap. Three debug-only themed previews compile normal/narrow/larger-text loaded states with a bundled image and no preview network requests.

Flow: product card → ProductDetailRoute(id) → navigation-scoped ViewModel/SavedStateHandle → getProduct(id) → GET /api/products/:id → ProductUi state → detail screen. Enquire → ViewModel event with the loaded product ID → Root callback → existing EnquiryRoute(id). That destination remains a shell until Slice 14; backend enquiry sending remains Slice 12.

Kotlin compilation (`:app:compileDebugKotlin --offline`) and whitespace checks passed. No test suite or device walkthrough was run for this chunk. Detail failure/retry, saved-route restoration, Back-stack behaviour, long descriptions and the anchored action remain on the Slice 15 walkthrough list under the author's time constraint.

## Enquiry endpoint and local SMTP

Slice 12 adds `backend/src/enquiry.ts` and `backend/src/mail.ts`, and connects POST `/api/enquiries` in `app.ts`. The route expects application/json, allows at most 16 KB and rejects unknown fields. productId must be a positive safe integer; name is required/up to 100 characters, email is required/valid/up to 254, and optional message is trimmed/up to 2000. Name/email reject control characters before trimming. The backend validates independently of Android, looks up the product and refuses unknown IDs before sending. An in-memory per-IP limit allows ten attempts per minute, including invalid attempts; it resets with the process and is not shared across servers.

The mailer uses Nodemailer SMTP with a fixed configured sender/recipient and customer Reply-To. Its plain-text body includes product ID/title/price, name, email and message. User input cannot choose recipients or supply attachments/HTML. Connection/greeting/DNS timeouts are 3 seconds and socket-idle timeout is 5 seconds; these are phase/idle bounds, not a single end-to-end deadline. SMTP credentials/content are not logged. SMTP acceptance yields 201 `{ "status": "accepted" }`; a failed or unconfirmed handoff yields 503 EMAIL_UNAVAILABLE. Input, missing product, media/body and rate-limit failures return 400, 404, 415/413 and 429 respectively. Product-source failures retain the existing server-error handling.

Flow: Android's future form → POST body → rate/body/schema validation → product lookup → mailer.send → SMTP server accepts message → HTTP 201. There is no database/outbox or automatic resend; a lost acknowledgement followed by manual retry can duplicate email. Mailpit captures locally and does not deliver to an external mailbox. The Android form and success screen remain Slices 13/14.

Direct development works with Mailpit's defaults. Optional configuration:

```sh
cd backend
cp .env.example .env
npm run dev
```

The dev/start scripts load an existing .env; .env is ignored by Git. SMTP_HOST/SMTP_PORT default to 127.0.0.1/1025; SMTP_SECURE defaults to false. MAIL_FROM defaults to catalogue@catalogue-studio.test and SALES_EMAIL to sales@catalogue-studio.test. SMTP_USER and SMTP_PASS must both be set or both absent. Compose sets SMTP_HOST=mailpit; rebuild the backend container to pick up code/dependencies. No provider account or credentials are required for Mailpit.

With the backend and Mailpit running, this sample enquiry appears in the local inbox:

```sh
curl --fail-with-body http://127.0.0.1:3000/api/enquiries \
  -H 'Content-Type: application/json' \
  -d '{"productId":9,"name":"Catalogue Review","email":"review@example.test","message":"Please share more information."}'
```

Focused verification passed: TypeScript build, whitespace check, invalid/header/unknown-field requests, unknown product, malformed/oversized JSON, rate limiting, actual SMTP acceptance and unavailable-SMTP 503. The captured Mailpit message was checked for sender, sales recipient, customer Reply-To and product/contact/message content. Isolated ephemeral HTTP servers were closed afterward; the existing backend/Mailpit services stayed running. The real development server now returns INVALID_ENQUIRY for invalid input, confirming it picked up the route. No broad test suite was run; repeatable automated coverage and the Android-to-inbox walkthrough remain Slice 15. The sample capture is left in the inbox for review.

## Enquiry form state and submission

Slice 13 adds `presentation/EnquiryState.kt` and `EnquiryViewModel.kt`, registered with Koin. The ViewModel decodes EnquiryRoute's productId from SavedStateHandle and loads the selected product independently of submitting. State separates product loading/loadError from isSending/sendError/isSent, and holds raw name/email/message plus resource-backed field errors. canSubmit means the product is ready and no request/success is active; field validation happens on Submit so invalid fields can explain their errors.

Edits update StateFlow and SavedStateHandle. Submit validates all three fields with the existing EnquiryValidator, reveals errors together and then revalidates edited fields. Valid submission creates one trimmed Enquiry snapshot and sets isSending synchronously before launching sendEnquiry. While sending, all actions including Back are ignored; Slice 14 will also handle the system Back gesture. No duplicate tap or edit can change the in-flight snapshot. Failures clear isSending and expose sendError while retaining raw inputs/saved fields; retry is an explicit Submit action.

Only successful backend acknowledgement sets isSent. Success removes saved contact fields and clears their UI values, saving just a sent flag alongside the route ID. A restored sent screen skips loading/submitting and can render confirmation immediately. An interrupted in-flight request is never auto-resubmitted after restoration; a manual retry can still duplicate email if the original SMTP handoff succeeded but its acknowledgement was lost. SavedStateHandle provides Android saved-instance restoration, not a durable form database or storage across every force-stop/task removal.

The manifest still disables backups. `res/xml/data_extraction_rules.xml` also explicitly excludes all app-file domains from cloud backup/device transfer on Android 12+, following [Android's backup configuration guidance](https://developer.android.com/identity/data/autobackup). These file-transfer rules are separate from Android's saved-instance state used for form restoration.

Quick Kotlin compilation and whitespace checks passed; no test suite or device walkthrough was run. Invalid/restored fields, duplicate-tap guards, failed-submit retention, sent restoration and interrupted-send behaviour remain focused coverage for Slice 15. The normal enquiry destination remains a shell until Slice 14 connects EnquiryRoot/Screen to this ViewModel.

## Enquiry form and confirmation UI

Slice 14 adds `ui/EnquiryScreen.kt` with EnquiryRoot and EnquiryScreen, replacing the last navigation shell in CatalogueApp. Root gets the existing EnquiryViewModel from Koin, lifecycle-collects state/events and forwards NavigateBack to popBackStack. BackHandler consumes system Back while sending, matching the ViewModel's action guard; the visible Back button is also disabled during that request.

The screen uses a 720-dp maximum width, system/IME insets and Scaffold content padding. Its scrollable body renders product-loading recovery or the loaded product title/category/price plus name, email and optional message fields. Field values/errors come directly from state, and changes dispatch actions. Name/email use Next keyboard actions; email uses the email keyboard without autocorrect, and message permits multiple lines with a character count. Required/helper text and field errors use resources; send errors appear beside the retained form with a polite accessibility live region.

Send enquiry stays in the bottom bar above the keyboard. Tapping clears focus/hides the keyboard and dispatches Submit; the ViewModel owns validation and the request. Sending disables fields and the button, displays Sending/progress and blocks Back. The launcher Activity explicitly uses adjustResize for keyboard insets. Only isSent replaces the form with Enquiry received and Back to product, which pops to the existing detail entry. No UI timer or optimistic success is used. Seven debug-only themed previews compile normal/narrow/larger-text forms, invalid fields, sending, retained send error and confirmation; they require no preview network calls.

Quick Kotlin compilation and whitespace checks passed; no test suite or device walkthrough was run for this chunk. Actual keyboard scrolling, validation/failure recovery, Back, duplicate-tap behaviour, saved-state restoration and the Android → backend → Mailpit → confirmation walkthrough remain Slice 15. Start Mailpit and the backend, rebuild/run Android, then use any product's Enquire button to review the form.

## Android backend address

The generated `BuildConfig.BASE_URL` defaults to `http://10.0.2.2:3000`. Inside the Android Emulator, `10.0.2.2` reaches your computer's loopback interface. Startup wiring supplies this address to the data module; opening the normal launcher now loads the catalogue through that source.

For a USB-connected physical phone, add `apiBaseUrl=http://127.0.0.1:3000` to `android/local.properties` (ignored by Git), then forward the phone's loopback port to your computer before running from Android Studio:

```sh
adb -s YOUR_DEVICE_SERIAL reverse tcp:3000 tcp:3000
```

Reapply forwarding after reconnecting the phone or restarting adb. Rebuild after changing the URL. `10.0.2.2` is specific to the Android Emulator; it does not reach your computer from a physical phone. The backend can keep listening on computer loopback when using USB forwarding. URL precedence: `-PapiBaseUrl`, then `local.properties`, then the emulator default.

Override it at build time when needed:

```sh
./gradlew assembleDebug -PapiBaseUrl=https://your-backend.example
```

For a physical device using a future local backend, a LAN address also requires explicitly making the backend reachable on that interface; the defaults above expose only loopback. Plain HTTP is permitted only by the debug manifest. A release build needs an HTTPS backend address.

## Project structure

```text
android/
├── settings.gradle.kts       Project repositories and :app registration
├── build.gradle.kts          Root plugin declarations
├── gradle/libs.versions.toml Plugin and library versions
└── app/
    ├── build.gradle.kts      Android SDK, JVM, Compose and BASE_URL settings
    └── src/
        ├── debug/AndroidManifest.xml
        └── main/
            ├── AndroidManifest.xml
            ├── java/com/cataloguestudio/
            │   ├── CatalogueStudioApplication.kt  Starts Koin
            │   ├── MainActivity.kt
            │   ├── domain/  Models, contracts, typed results and enquiry validation
            │   ├── data/    DTO mapping, Ktor/OkHttp client, typed requests, endpoints and di/
            │   ├── presentation/  Display models, UiText/error mapping, catalogue ViewModel/state/actions/events and di/
            │   └── ui/      Theme, text resolution, product cards, states and catalogue summary
            └── res/values/strings.xml

backend/
├── Dockerfile               TypeScript build and production runtime stages
├── .dockerignore            Excludes local dependencies, build output and secrets
├── package.json             Node dependencies and scripts
├── tsconfig.json            TypeScript compiler settings
├── fixtures/products.json  Bundled demo product data
├── public/images/          Bundled demo JPEGs
├── test/                   Source, rule and HTTP tests
└── src/
    ├── app.ts              Express routes and public response mapping
    ├── catalogue.ts        Product schema, source contract and demo loader
    ├── catalogue-rules.ts  Featured selection and search/filter rules
    ├── product-sources.ts  Live adapter, cache and source selection
    ├── product-images.ts   Demo image loading and bounded live image fetching
    └── server.ts           Source selection and server startup

compose.yaml                Local backend and Mailpit startup
```
