# Verification record

Local verification on 7 November 2026. These are observed local results; GitHub Actions results must be checked separately on the PR.

| Check | Observed result |
| --- | --- |
| Clean backend dependency install (`npm ci`) | Passed with npm 10 / Node 24; lockfile v3 includes platform-specific optional dependencies |
| Backend format check | Passed |
| Backend TypeScript check and build | Passed |
| Backend tests with local SMTP | 13 passed, 0 failed, 0 skipped |
| Android debug assembly | Passed; APK installed on emulator |
| Android unit tests | 9 passed, 0 failed |
| Android lint | Passed with 0 errors and 24 warnings: pinned dependency/toolchain update notices, target-SDK advisory, and the legacy backup attribute advisory. Backup/data-transfer exclusion rules are present. No baseline or blanket suppression was used. |
| Kotlin formatting | Formatted with ktfmt 0.64, Kotlin language style |
| Docker Compose startup | Backend healthy; Mailpit healthy |
| Emulator success walkthrough | Passed category, submitted search, product details, enquiry, thank-you, and actual captured email verification |
| Emulator SMTP failure/retry | Passed: Mailpit stopped; form retained, error shown, no false thank-you; Mailpit restarted; retry succeeded |
| Live Fake Store API | Real requests returned HTTP 403/Cloudflare challenge; successful live fetch unverified. Adapter behaviour and mapping tested with representative responses. |

The emulator used an existing API 33 arm64 AVD with its display overridden to 1080×2400 at 420 dpi (roughly a 411-dp phone width). Screenshots are actual emulator captures, with the status-bar clock set to demo time. This is not a claim of a full physical-device, API-26, tablet, large-font, or TalkBack test matrix.

The first debug assembly took approximately two minutes with the local SDK present. Docker Compose built and started successfully. A clean machine's download speed can affect first-run setup; Android Studio/SDK installation is treated as a prerequisite. The smoke test is committed so the critical local walkthrough can be repeated.

The smoke check originally assumed adb text injection preserved the email's first-letter case. The emulator IME capitalised it. The email had arrived correctly; the script now reads the actual field value before comparing it with the captured message.

## Time spent

The agreed limit is **six focused hours**, not a target to fill. Implementation began at **12:54:56 IST on 7 November 2026**. Earlier PDF review/planning and the user's repository-creation time are excluded. Time for the completed build/review is recorded below at submission; category splits are approximate because builds, downloads, and tests overlap.
