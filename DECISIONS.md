# Decisions and assumptions

## Scope and acceptance rules

- Kotlin/Compose Android and TypeScript/Node.js are allowed challenge choices. Android uses the author's expertise; TypeScript gives the small backend explicit contracts and validation. Both live in one repository for coherent local setup and review; they remain separate build/deployment units.
- Home's five “chosen top-rated” products are editorial IDs 1, 2, 5, 7 and 9, rather than recalculated global ratings. Available chosen IDs are shown; there is no replacement policy if a future provider removes an ID.
- Category picks rank by rating descending, then ID ascending for ties. Up to two picks are selected while reserving at least one remaining item; a single-product category therefore has no separate featured pick. Featured products are excluded from remaining items.
- Search matches a trimmed, case-insensitive substring in title or description, only on explicit Submit. Search and category filters combine. Search results use one list; no duplicate featured section. Unknown categories and no matches produce an empty result rather than an error.
- Prices are illustrative USD. Name and email are required; message is optional. Product IDs are validated and looked up server-side. Client validation provides feedback; the server independently enforces its schema.
- Mailpit is the local sales inbox. Only SMTP acceptance produces HTTP 201 and the app's confirmation. It does not establish external delivery. Fixed sender/recipient configuration and customer Reply-To keep the form from choosing arbitrary destinations.

## Deliberate simplifications

The challenge asks for a 2–3-hour time box. The author chose a six-hour learning scope and later a one-hour wrap-up. The step-by-step learning/review workflow extended beyond the original challenge target; actual personal time is recorded separately in AI_USAGE.md and must not be inferred from commit timestamps.

Live Fake Store API products are the default, as requested by the author after live catalogue/image access was verified. Explicit PRODUCT_SOURCE=demo provides bundled illustrative products/images for repeatable offline-provider review and automated smoke checks. The app labels this mode; there is no silent fallback. The regression suite uses controlled data, while an additional recorded phone walkthrough verifies live products, validation and SMTP-backed confirmation.

No database is needed to browse read-only provider data and hand enquiries to SMTP. This reduces setup and scope, but sacrifices durable enquiry history, retries/outbox, cross-instance rate limits and idempotency. SavedStateHandle preserves a draft across supported Android saved-instance restoration; it is not durable business storage. A lost server acknowledgement followed by manual retry can produce duplicate mail.

No accounts, payments, cart, admin editor, analytics or cloud deployment are included. Mailpit replaces external delivery for local review, and release signing/HTTPS deployment remain future work. The debug app permits local HTTP; release does not. The repository contains no provisioned cloud infrastructure; the Node container can be adapted for AWS, but deployment compatibility has not been exercised there.

## Growth without speculative layers

One Android module keeps build/review overhead low. Domain contracts and models stay independent of Android networking/Compose; data maps DTOs/errors, ViewModels own state/validation/request coordination, and Screens render state and emit actions. Koin wires implementations; typed navigation passes scalar IDs. No repository/use-case class merely forwards one data-source method.

The backend injects ProductSource and EnquiryMailer. Replacing Fake Store/demo products or SMTP transport changes those boundaries rather than screen code. Catalogue ranking lives server-side. Theme, fonts, strings and shared Compose components isolate presentation changes. The live adapter validates responses, coalesces concurrent loads and caches valid products for 60 seconds; product images stay behind bounded backend routes.

GET transport recovery is limited; POST is never automatically replayed. The form snapshots validated inputs and blocks duplicate in-flight actions. JSON size/schema checks, plain-text email, rate limiting and generic public errors bound the local attack surface. The in-memory rate limit is a single-process PoC choice, not distributed protection.

## Another day

1. Verify a clean reviewer setup, hosted CI and live-provider behaviour; fix the remaining launcher icon and review applicable dependency/SDK advisories.
2. Exercise API 26, tablet/narrow/large-text layouts, TalkBack and keyboard/navigation recovery, then add only the device tests needed for regressions found.
3. If durable enquiries are needed, add persistence, idempotency keys and an outbox before enabling automatic retries; choose retention and contact-data handling deliberately.
4. Configure production SMTP/HTTPS, secrets and signed releases; adapt the container to the chosen AWS runtime and verify deployment rather than assuming it.
5. Move editorial IDs/currency/configuration behind a maintainable content contract as product requirements firm up; extract modules when real ownership/reuse warrants it.
