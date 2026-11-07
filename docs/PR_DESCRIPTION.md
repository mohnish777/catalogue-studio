# Suggested title

feat: build Android catalogue with local enquiry backend

# Description to paste into the author's PR

**What it is**

Adds a Kotlin/Compose Android catalogue backed by a local TypeScript/Node service. Customers browse five editorial picks, filter categories, submit searches, view product details and send an enquiry to a local Mailpit sales inbox. The form validates input, preserves failed submissions and shows confirmation after backend SMTP acceptance.

All products and images go through the backend. The quick start selects live Fake Store products; explicit demo mode provides repeatable offline-provider review. Product-source, SMTP and Android presentation boundaries permit replacement as the product grows. One repository holds independently buildable Android/backend components.

**Jira ticket**

Not applicable — standalone AI-Native Engineer take-home challenge.

**Testing Instructions:**

1. Follow README prerequisites. From the root run docker compose up --build -d --wait, open Android Studio on android/, and run app on an API 26+ emulator using full JDK 17 / SDK 36.
2. Open home; expect five featured products plus additional items. Select a category; expect ranked picks with no duplication in remaining items. Search explicitly and verify matching title/description text; clear filters to return home.
3. Tap a product → Enquire. Submit empty fields and expect errors. Fill synthetic name/email and optional message; Send enquiry should show Enquiry received and one matching product/contact message at http://127.0.0.1:8025.
4. Review failure/restoration evidence in docs/VERIFICATION.md or repeat scripts/smoke-android.py with an explicit serial and loopback debug build as documented.

Local evidence: 91 Android JVM tests and 46 backend tests passed, including actual Mailpit SMTP capture. Debug/unsigned release assembly passed. Lint: 0 errors, 26 warnings. The API 36 physical-phone walkthrough passed success, actual SMTP failure/retained form, draft restoration without autosend, explicit retry and Back navigation.

**Screenshots and video:**

Actual Android phone captures; catalogue/detail/confirmation use live Fake Store products through our backend. The retained-error screenshot uses controlled demo data. Contacts are synthetic.

<img src="https://raw.githubusercontent.com/mohnish777/catalogue-studio/feature/catalogue-step-by-step/docs/screenshots/catalogue.png" width="260" alt="Live catalogue">
&nbsp;&nbsp;
<img src="https://raw.githubusercontent.com/mohnish777/catalogue-studio/feature/catalogue-step-by-step/docs/screenshots/product-details.png" width="260" alt="Live product details">

<img src="https://raw.githubusercontent.com/mohnish777/catalogue-studio/feature/catalogue-step-by-step/docs/screenshots/retained-form.png" width="260" alt="Failed submission retains input">
&nbsp;&nbsp;
<img src="https://raw.githubusercontent.com/mohnish777/catalogue-studio/feature/catalogue-step-by-step/docs/screenshots/confirmation.png" width="260" alt="Enquiry confirmation">

[Download walkthrough MP4](https://github.com/mohnish777/catalogue-studio/raw/refs/heads/feature/catalogue-step-by-step/docs/media/catalogue-walkthrough.mp4): live products → details → validation → enquiry → confirmation.

<!-- Before publishing, drag docs/media/catalogue-walkthrough.mp4 into this description editor and put GitHub's generated attachment URL here on its own line for inline playback. Do not invent an attachment URL. Repository links above resolve after the author pushes these files. -->

Reviewing from a phone browser: screenshots and the MP4 can be viewed without running the project. To run on a physical Android phone, follow README's USB debugging, local.properties and adb reverse steps; the backend stays on a computer. An iPhone can review media only.

**Additional Considerations:**

- See DECISIONS.md for assumptions, simplifications, growth boundaries and another-day priorities; AI_USAGE.md records AI direction/corrections and actual time.
- Actual time: approximately nine hours, confirmed by the author (planning 1h, building 5h, testing/debugging 1.5h, reviewing 1.5h); this exceeds the challenge's 2–3-hour target. A review-led learning workflow extended the initial six-hour personal scope.
- Mailpit is local capture, not external delivery. No database/outbox/idempotency; lost acknowledgement followed by manual retry may duplicate email.
- Single API 36 phone covered the latest full flow. API 26/tablet/new form large-font/TalkBack, signed HTTPS release runtime remain unverified. Live catalogue/image access and the recorded phone flow were additionally verified; the 91/46-test evidence remains the earlier controlled demo run.
- CI workflow is prepared locally; replace this sentence with the actual hosted result/link after the author pushes. No hosted pass is claimed yet.

**Checklist**

- [x] No Jira ticket applies to this standalone challenge
- [ ] Project builds with no warnings and no errors — local builds pass, but 26 lint warnings remain
- [ ] KTLint runs without warnings or errors — not configured; no pass claimed
- [x] Relevant setup, assumptions and boundaries documented
- [x] Critical paths have focused unit/integration coverage
- [x] Local tests pass
- [x] Author confirmed actual-time split and Codex-only AI tool use
- [ ] Hosted backend and Android jobs pass after author push
- [ ] Author reviews screenshots, opens feature-branch PR into main and leaves it open

---

## Author handoff (keep outside the pasted description)

Review the local diff before staging. Suggested final commit: ci: add verification workflow and submission documentation. You can commit workflow and docs separately if easier to review. Push feature/catalogue-step-by-step, verify both jobs, create the PR with base main and head feature/catalogue-step-by-step, and keep it open. Grant private-repository reviewer access if needed. After pushing the media files, preview the image links and drag docs/media/catalogue-walkthrough.mp4 into the PR editor to get GitHub’s inline video attachment URL. See docs/MEDIA.md for the short handoff. The assistant has not committed, pushed or created a PR.
