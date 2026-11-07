# AI usage

## Tools, model, and instructions

Implementation used the Codex desktop assistant, identified in this session as a GPT-6-based coding agent; an exact model version was not recorded. Tools included terminal/file editing, official documentation lookup, Gradle, Android Emulator/adb, Docker Compose, local SMTP/Mailpit, and GitHub CLI/Git. No delegated agents were used.

Installed Android skills applied: android-module-structure, android-compose-ui, android-presentation-mvi, android-data-layer, android-error-handling, android-di-koin, android-navigation, and android-testing. The raise-pr skill guided the PR description. User instructions to avoid overengineering and keep a single app module take precedence over the skills' larger multi-module examples.

[AGENTS.md](AGENTS.md) is the committed project instruction file. [docs/PLAN.md](docs/PLAN.md) records the agreed scope and six-hour budget.

## Five shaping prompts and decisions

1. The user chose Android, asked whether a backend was required, and asked whether two repositories were necessary. The PDF required a backend but did not require separate repositories; we chose a monorepo.
2. The user replaced a broad 20–35-hour estimate with a hard six-hour time box and asked whether omitting a database would cost assessment points. We kept all catalogue/enquiry flows, removed unnecessary persistence, and documented the reasoning.
3. The user selected a premium minimal visual direction. The UI uses a restrained warm/green palette, Instrument Serif/DM Sans, consistent cards, and explicit failure/empty/success states.
4. The user selected a local email inbox. We implemented SMTP delivery with Mailpit, preserving a configurable path to a real provider instead of pretending local capture means external delivery.
5. The user required Android skills, a new feature branch, and an open PR into main. We used the skills proportionately and kept verification and assumptions in the submission.

## Errors caught and corrected

- The first backend setup ran in an unintended nested directory, and the shell selected a parent project's Node 14. Folder inspection and npm engine warnings exposed both. Files were moved into the agreed backend package. The runtime was made explicit and dependencies were locked again. A release-support check then selected Node 22 LTS for the final project configuration; verification uses Node 22 in Docker and Node 24 on the host.
- Initial fixture/image paths assumed a different runtime directory. Image endpoint tests and a container run checked the corrected package-root paths. Both the local npm workflow and Docker now run from the backend package root.
- Android lint caught an API-27 navigation-bar theme attribute while the app supports API 26. The unnecessary XML flag was removed; edge-to-edge handling stays in the activity. Lint then passed.
- The first phone screenshot showed an oversized hero and poor product-image framing. The heading was shortened, the image container was corrected, and the Explore action now scrolls to the collection.
- A formatting check exposed a second-pass change in chained TypeScript calls; formatting was rerun and checked rather than claiming the first pass was sufficient.

## Ownership and review

AI generated the initial implementation, tests, documentation, and PR body. The agent reviewed the code and used builds, lint, API/SMTP tests, ViewModel tests, and an actual emulator walkthrough to challenge the output. See the verification record for observed results and limitations.

Human review is still pending. The author should read and understand the code and decisions before presenting this as their work, particularly provider replacement, MVI state, coroutine cancellation, SMTP failure handling, and the decision not to persist enquiries. No human review or managerial approval is claimed here.
