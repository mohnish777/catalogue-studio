# Submission requirement audit

Checked against all four pages of Code Challenge: AI-Native Engineer, 7 November 2026. This is a handoff audit; local implementation does not imply author review or hosted success.

| PDF requirement | Implementation / evidence | Remaining action or qualification |
| --- | --- | --- |
| One allowed client and backend; explain choice | Kotlin/Compose Android + TypeScript/Node; README and DECISIONS.md | Implemented |
| Product data through backend | ProductSource adapters, backend HTTP/image routes; Android points to backend | Actual live catalogue/image access and recorded phone enquiry flow additionally verified |
| AWS-compatible infrastructure if included | Portable Node Dockerfile; Compose for local backend/Mailpit; no cloud IaC | AWS deployment not provisioned/tested |
| Mobile runs against local backend; reviewer setup under 10 minutes | README quick start, Docker build/container checks; prior emulator catalogue checks and latest complete physical-device flow | Fresh clone/emulator timing not measured; preinstalled SDK/JDK/Docker required |
| No secrets; example configuration | backend/.env.example; .env and local.properties ignored; no reviewer credentials needed | Author reviews staged diff before pushing |
| No paid services/accounts for reviewer | Bundled demo mode, local Node and Mailpit | Live is now the default; explicit demo is available if upstream is unavailable; no external SMTP account required |
| Five chosen featured home products | Editorial IDs 1/2/5/7/9, rule/HTTP tests and demo container response | Missing future editorial IDs are not replaced automatically |
| Category top-rated and additional products | Rating/ID ordering, reserved remaining item, no duplication; rule tests | Small-category assumption explained in decisions |
| Search only after button, matching text | Explicit submit; title/description matching; combined filters; tests and earlier UI checks | Case-insensitive substring is documented assumption |
| Product tile → product page | ID-only typed navigation, product fetch, detail UI; focused tests and phone flow | Implemented |
| Enquire → contact form → sales email → thank-you | Strict server validation, SMTP handoff, form retention; real Mailpit capture and phone walkthrough | Local capture replaces external delivery; no exactly-once/durable outbox |
| Repository, full source and tests | Single repository with android/, backend/, scripts/, docs/ | Author supplies repo URL and private access if needed |
| Feature branch PR into main; leave open | feature/catalogue-step-by-step; docs/PR_DESCRIPTION.md draft | Author commits/pushes, verifies hosted CI, creates/leaves PR open |
| Setup/configuration README | Current quick start, emulator/phone URLs, direct Node mode, demo/live, checks | Cold prerequisite downloads are not promised under ten minutes |
| Assumptions, cuts, evolution, another day | DECISIONS.md | Author reviews/defends decisions |
| AI tools/models, instructions, plan, prompts and corrections | AI_USAGE.md, AGENTS.md, docs/PLAN.md, chronological notes | Author confirmed only Codex; exact historical model selections remain unavailable |
| Actual time roughly split by activity | Author reports approximately nine hours in AI_USAGE.md | Author confirmed planning 1h / building 5h / testing-debugging 1.5h / reviewing 1.5h; original time targets exceeded |
| Submit within three days | No receipt/deadline metadata available in this repository | Author checks email deadline and submits repository link |

## Final author gate

Review Slice 16 and the confirmed time/tool notes, commit approved changes, push, inspect both Actions jobs, update the PR draft with actual hosted results, create the PR into main and leave it open. Review the included screenshots and grant private access before sending the repository link. The assistant does not perform these Git publication steps.
