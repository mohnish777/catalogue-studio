# Catalogue Studio slice plan

The author requested a six-hour learning scope, small reviewed increments and author-owned commits. Author-confirmed actual total: approximately nine hours (planning 1h, building 5h, testing/debugging 1.5h, reviewing 1.5h). Codex was the only AI tool used. The original challenge's 2–3-hour target was exceeded; this is disclosed in AI_USAGE.md.

The early full implementation is preserved on a separate branch. The active feature/catalogue-step-by-step branch was restarted from main. This table describes the active branch; it does not treat the earlier MVP's results as current evidence.

| Slice | Scope | Status at final checkpoint |
| --- | --- | --- |
| 1 | Runnable Android/Gradle foundation | Accepted and author committed |
| 2 | Theme, typography and bundled assets | Accepted; detailed typography review waived by author |
| 3 | Local Node backend, Docker and Mailpit | Accepted and author committed |
| 4 | Product sources, catalogue rules, HTTP/image routes | Accepted and author committed |
| 5 | Domain contracts, validation and typed failures | Accepted and author committed |
| 6 | DTO mapping, Ktor networking and Koin | Accepted and author committed |
| 7 | Display models and shared UI components | Accepted and author committed |
| 8 | Catalogue state, search, cancellation and saved inputs | Accepted and author committed |
| 9 | Catalogue Compose screen | Accepted and author committed |
| 10 | Launcher, typed navigation and lifecycle events | Accepted and author committed |
| 11 | Product-detail state and UI | Accepted and author committed |
| 12 | Enquiry HTTP validation and local SMTP | Accepted and author committed |
| 13 | Enquiry state, restoration and submission rules | Accepted and author committed |
| 14 | Form and confirmation UI | Accepted; author commit a90ab0c |
| 15 | Focused regression and complete local walkthrough | Accepted; author commit 88a04ec |
| 16 | CI, decisions, AI/time docs and PR handoff | Implemented locally; author review, commit/push, hosted CI and PR remain pending |

The final reviewer follow-up makes live products the default, adds an actual phone video/live screenshots, and simplifies emulator/USB-phone setup. Backend source-selection coverage was updated and all 46 tests passed again. See docs/MEDIA.md and the verification follow-up.

## Final lead walkthrough

1. Run the local backend/inbox and app using README.
2. Home: five editorial picks and additional products.
3. Category: ranked picks and nonduplicated remaining products.
4. Type a term: typing alone does not request; Search submits and combines with category.
5. Open a product: detail and Enquire carry the same ID.
6. Empty form: validation; valid form: captured local sales email then confirmation.
7. Explain failed-submit retention, draft restoration, explicit retry and the absence of durable/exactly-once delivery.
8. Explain provider, SMTP and theme boundaries, demo-mode tradeoff and the nine-hour actual time.

## Author delivery steps

Review the pending Slice 16 diff and AI/time notes. Commit in reviewable chunks if desired, push the active feature branch, inspect both hosted verification jobs, open a PR into main and leave it open. Use docs/PR_DESCRIPTION.md as the draft. Add reviewer access for a private repo, then submit the repository link within the challenge's deadline. No assistant commit/push/PR is authorized.
