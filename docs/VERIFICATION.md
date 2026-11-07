# Verification — step-by-step implementation

Recorded 7 November 2026 on `feature/catalogue-step-by-step`, after author commit `a90ab0c` plus the uncommitted Slice 15 changes. These are observed checks, not author approval. No commits, pushes or PR were made by the agent.

| Check | Observed result |
| --- | --- |
| Backend | TypeScript compilation and 46 tests passed, including actual local SMTP capture with RUN_SMTP_TEST=1. |
| Android JVM | 91 tests passed, including nine new detail/enquiry state and event tests. |
| APK builds | Debug and unsigned release assembly passed. |
| Lint | 0 errors, 26 warnings; dependency/target update advisories and existing missing launcher-icon warning remain unsuppressed. |
| Release contents | Debug gallery activities and detail/enquiry preview classes absent from release DEX. |
| Whitespace | git diff --check passed. |

## Actual Android → backend → Mailpit walkthrough

Device: Samsung SM_S921B, Android API 36, USB forwarding to the demo backend. Mailpit remained running on local SMTP 1025 / inbox 8025. Only synthetic `.test` contact data was used.

- Catalogue → selected product → detail → enquiry preserved the same product.
- Submitting empty fields showed validation and produced no email.
- Submit remained reachable while the keyboard was open.
- A temporary backend with unavailable SMTP returned failure: the form retained all fields and showed no confirmation/email.
- Backgrounding and terminating the app process restored the navigation entry and draft fields without submitting automatically.
- Restoring the normal backend and explicitly retrying produced one matching captured email with the correct product, sales recipient and customer Reply-To, then showed confirmation with no editable contact fields.
- Back returned through product details to the catalogue.
- The final installed build was visually checked for dark status-bar icons against the light app theme, including when the device uses dark mode.

The reusable `scripts/smoke-android.py` saves screenshots and requires an explicit device serial. Its failure mode owns a temporary backend and changes/restores USB forwarding; it does not stop the user's backend or Mailpit. Captured test emails are left for inspection.

## Repeat the checks

From the repository root with Node 22, JDK 17 and Android SDK configured:

```sh
nvm use
RUN_SMTP_TEST=1 npm --prefix backend test
./android/gradlew -p android :app:testDebugUnitTest :app:assembleDebug :app:assembleRelease :app:lintDebug
```

Start the demo backend/Mailpit using README instructions. For an unlocked USB device or emulator used for this loopback smoke:

```sh
./android/gradlew -p android :app:assembleDebug -PapiBaseUrl=http://127.0.0.1:3000
adb -s YOUR_SERIAL install -r android/app/build/outputs/apk/debug/app-debug.apk
python3 scripts/smoke-android.py --serial YOUR_SERIAL --smtp-failure --output /tmp/catalogue-smoke
```

Without RUN_SMTP_TEST=1, the optional Mailpit integration test skips. Backend tests use isolated HTTP servers; the UI smoke needs the normal backend on 3000. The script expects current English labels and demo fixtures. It restarts the test app and creates local test enquiries.

## Scope and remaining gaps

The new complete walkthrough covered one physical API 36 phone. New detail/form screens have compiled previews, but API 26, tablet, narrow/large-font device rendering and TalkBack were not checked this turn. Earlier catalogue-only emulator checks are recorded separately in README.

Duplicate in-flight submissions and restored sent-state guards have focused JVM coverage; there was no physical stress test. Draft process restoration was exercised on the phone. One matching captured email establishes the observed outcome, not a cross-request exactly-once guarantee: there is no database, durable outbox or server idempotency, so a lost acknowledgement followed by manual retry can duplicate email.

At the Slice 15 checkpoint, live-provider access, external delivery, signed release deployment and HTTPS release runtime were not verified. See the later live-mode/media check below for updated provider evidence. The unsigned release build is compilation evidence; plain HTTP remains debug-only. The Slice 16 workflow is prepared and statically validated; hosted CI awaits the author's push. The author owns review, commits, pushes and PR creation.


## Slice 16 handoff checks

The author committed Slice 15 as `88a04ec`. Slice 16 changes documentation and adds CI; application behaviour and dependencies are unchanged, so the Slice 15 app test evidence above was not rerun unnecessarily.

- Official actionlint v1.7.12 validated .github/workflows/verify.yml with no findings. Workflow action versions/inputs were checked against their official action manifests. The Android job requests platform-tools, Platform 36 and Build Tools 36.0.0, excluding the obsolete tools package. Inputs were checked against the [official SDK action](https://github.com/android-actions/setup-android) and [Gradle action](https://github.com/gradle/actions/tree/v4/setup-gradle).
- A fresh Docker image build passed, including npm ci and TypeScript compilation. An isolated temporary container returned health success, the demo catalogue (five featured / eight remaining) and bundled image bytes. It was stopped afterward; the existing backend/Mailpit stayed running.
- Current README/decision/AI/plan links resolve, screenshot files were checked and git diff --check passed. Historical implementation notes preserve earlier checkpoint wording and are clearly marked as history.
- All four challenge PDF pages were reviewed against docs/SUBMISSION_CHECKLIST.md. The author confirmed the nine-hour rough split: planning 1 hour, building 5 hours, testing/debugging 1.5 hours and reviewing 1.5 hours. Codex was the only AI tool used; exact historical model selections remain unavailable.
- Hosted CI, a fresh emulator setup timed under ten minutes, signed release deployment and author PR/reviewer access remain unverified or author-owned. These are not marked complete from local evidence.


## Live default and PR-media follow-up — 7 November 2026

At the author's request, normal server/source selection, Compose and .env.example now default to live Fake Store products. Demo remains an explicit mode for offline-provider review and deterministic automation; there is no silent fallback. The source-selection regression test now expects live by default; all 46 backend tests passed again, including real local SMTP capture. Android code/build configuration did not change.

Actual Fake Store /products returned HTTP 200. An isolated live backend returned mode=live, five featured and fifteen remaining products; its product image route returned HTTP 200 image/png (273,291 bytes). An actual phone walkthrough against that live backend verified product details, empty-field validation, sending and confirmation. The captured Mailpit message matched the live product and synthetic pr.review@example.test Reply-To. The normal development backend now reports live mode too.

Recorded screenshots and a silent 59-second H.264 MP4 (approximately 1.5 MB) are included under docs/screenshots and docs/media. The recording was decoded and its first/middle/final frames inspected, including the final confirmation. GitHub attachment upload/inline video playback remains an author publication step. USB forwarding was restored after recording; the owned temporary live backend was stopped. The earlier 91-test Android evidence and controlled demo failure/restoration evidence remain unchanged.
