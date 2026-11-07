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

Live-provider access, external delivery, signed release deployment and HTTPS release runtime were not verified. The unsigned release build is compilation evidence; plain HTTP remains debug-only. Hosted CI awaits Slice 16 setup and the author's push. The author owns review, commits, pushes and PR creation.
