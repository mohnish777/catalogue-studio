# Catalogue Studio

An Android product catalogue built with Kotlin and Jetpack Compose. The planned product discovery, search and email-enquiry flows will use a local Node.js backend.

## Current checkpoint

The Android project setup and visual foundation are ready. The app launches a minimal screen displaying "Catalogue Studio" and has pure Kotlin catalogue/enquiry contracts plus local enquiry validation ready for the later HTTP implementation. The backend provides health, catalogue, product-detail and image endpoints with explicit demo/live sources, and Mailpit supplies a local test inbox. Enquiry sending and Android-to-backend requests are not implemented yet; no backend is needed to run the placeholder screen.

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

Direct execution defaults to your computer's loopback interface (`HOST=127.0.0.1`). The container sets `HOST=0.0.0.0` through its Dockerfile. Enquiry sending belongs to a later slice.

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

The backend does not send email yet. A later enquiry slice will connect it to the SMTP address above.

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

- `Models.kt` defines read-only product/catalogue properties and a typed `CatalogueMode` (`DEMO` or `LIVE`). The catalogue carries categories, featured and remaining products. IDs use `Long` so they can represent the backend's safe-integer IDs. Image URLs are strings; resolving backend-relative paths belongs to the later data implementation.
- `Result.kt` defines our two-parameter `Result<Data, Error>` with `Success(data)` and `Error(error)`, plus `DataError` cases for connection, timeout, invalid request, missing product, server, decoding and unknown failures. This is distinct from Kotlin's built-in `Result<T>`, which holds a Throwable on failure. HTTP/exception mapping is not implemented yet.
- `CatalogueRemoteDataSource.kt` defines suspend functions for `getCatalogue(category, query)`, `getProduct(id)` and the future `sendEnquiry(enquiry)`. A null category/query means no filter. Sending returns `Result<Unit, DataError>` because success needs no domain payload. `suspend` allows coroutine callers to await an implementation; it does not automatically move work to a background thread.

The intended flow is ViewModel → `CatalogueRemoteDataSource` → typed `Result` → UI state. In Slice 6 a Ktor implementation will translate HTTP DTOs into these domain models and map expected transport failures into `DataError`. The UI can then use an exhaustive `when` over success/error. No implementation or caller is connected in this chunk, and no repository or use-case wrapper is added for this single backend source.

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

## Android backend address for later slices

The generated `BuildConfig.BASE_URL` defaults to `http://10.0.2.2:3000`. Inside the Android Emulator, `10.0.2.2` reaches your computer's loopback interface. This address is configured now; no HTTP client or network request exists yet.

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
            ├── java/com/cataloguestudio/MainActivity.kt
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
