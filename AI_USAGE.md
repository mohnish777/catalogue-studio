# AI usage and time spent

## Tools and ownership

The author used Codex desktop to plan, implement and explain the Android/Node solution, run local checks and maintain a Notion slice plan. This final handoff was produced by a GPT-6-based Codex agent; the exact historical model selected in every earlier session is not available in this record. The author confirmed that Codex was the only AI tool used for this challenge. No subagents were used for this final slice.

The first implementation was too broad for the author's desired learning/review workflow. It remains on a separate preserved branch. The author restarted from main on feature/catalogue-step-by-step and reviewed small increments. Commits on that branch are author-owned; the assistant does not commit, push or create the PR. Passing checks are not a substitute for the author's understanding of every submitted line.

## Instructions and plan

[AGENTS.md](AGENTS.md) commits the local working constraints used for continued work: small reviewed chunks, no agent Git publication, simple boundaries and evidence-based verification. It is added at the final documentation checkpoint, not presented as a file that existed throughout development.

External Codex Android skills guided Compose, presentation state, navigation, Koin, data/error handling and testing. The personal BAE Notion planner supplied structural inspiration only; no BAE project code belongs in this PoC. The raise-pr skill guided the final draft format. Skill files live outside this repository and are not required to build or run it. The authoritative portable project constraints are recorded in AGENTS.md; the source PDF was an external requirement reference, not an executable instruction file.

The [slice plan](docs/PLAN.md) records the 16-part sequence. [Implementation notes](docs/IMPLEMENTATION_NOTES.md) preserve the chronological explanations; [verification](docs/VERIFICATION.md) distinguishes actual local results from remaining gaps.

## Five prompts/moments that shaped the result

1. “I want to build an android app” selected familiar Kotlin/Compose rather than adding a new client stack during a time box.
2. “Time box it to 6 hours” and “let's not over engineer” led to one app module, one backend, explicit demo mode and no database/outbox.
3. “Don't commit” and “implement each slice at a time and review it one by one” corrected the initial oversized implementation and made author review the gate between chunks.
4. “Just make the android project runnable” prioritised a buildable foundation before product use cases; later font work was completed without requiring a detailed typography review.
5. The phone's “request took too long” report prompted emulator-versus-device URL/USB-forwarding investigation and bounded GET transport recovery, while avoiding automatic POST replay. The final demand to check the PDF drove explicit decisions, AI-use, time and PR handoff documents.

## Mistakes and corrections

- AI implemented too much before the author could review. The author identified this; work restarted from main in small slices, leaving the earlier branch preserved.
- Initial phone networking assumptions were insufficient. The author reported failure; inspection separated emulator-only 10.0.2.2 from physical-device loopback forwarding, and the team verified the working phone configuration. Ignored local.properties keeps personal overrides out of teammates' defaults.
- New plain-JVM ViewModel tests exposed Navigation's Android Bundle decoder requiring framework stubs. Reading the scalar ID through SavedStateHandle removed that test dependency; a real phone walkthrough then verified navigation/restoration still worked.
- Phone inspection exposed white status icons against the always-light theme on a dark-mode device; explicit light system-bar configuration corrected contrast.
- The earlier preserved MVP's CI requested the obsolete Android SDK tools package. The fresh workflow explicitly requests platform-tools, Platform 36 and Build Tools 36.0.0; hosted success is still pending the author's push.

## Actual time

The PDF asks for actual time, roughly split across planning, building, testing and reviewing. The initial six-hour allocation is a target, not an actual-time claim. Commit history shows a multi-hour learning session, including pauses, and cannot measure active effort.

| Activity | Actual personal time |
| --- | --- |
| Planning and requirements | 1 hour |
| Building and guided implementation | 5 hours |
| Testing and debugging | 1.5 hours |
| Reviewing and understanding code/docs | 1.5 hours |
| Total | Approximately 9 hours (author reported) |

Report the overrun honestly and explain that the review-led learning workflow expanded the scope. **Author-reported actual total: approximately 9 hours.** The author confirmed the rough activity split above; it includes the learning/review workflow rather than claiming the original target was met. Model history has not been invented.
