# Decisions and assumptions

## Boundaries, without extra modules

Android is the client because the author already has Android expertise. The backend is Node.js/TypeScript, an explicitly allowed combination. A single repository keeps the locally running system and review steps together.

The Android skills recommend layered feature modules and convention plugins for larger applications. The agreed six-hour scope deliberately uses one app module with package boundaries: domain models/interfaces, Ktor DTOs and mapping, MVI ViewModels, and Compose UI/theme. There are no cross-feature Gradle graphs or pass-through use cases. A single remote source is called a data source rather than a repository. Extract Gradle modules when ownership or build isolation makes them useful.

Product-source selection and email delivery are server-side boundaries. Changing a provider does not require rewriting HTTP routes or Android screens. UI colours, typography, and components are centralised. Standard containers can be hosted on AWS later; no cloud infrastructure is required for local review.

## Why no application database

The challenge asks for product browsing and email enquiries, not accounts, saved enquiries, stock changes, or order history. External/demo product sources and SMTP cover that scope. A database would introduce schema, migration, and setup work without satisfying an additional acceptance criterion.

Add persistence when enquiries need an auditable history, durable retry/outbox processing, or editorial picks need to be edited through an admin UI. Email is not a substitute for durable workflow storage if those requirements arrive.

## Catalogue behaviour

- Home shows exactly five explicitly selected IDs: 1, 2, 5, 7, and 9. The sample and Fake Store datasets contain these IDs. They are editorial choices, not automatically the five highest ratings.
- Category pages select up to two highest-rated products, descending rating with ID as a stable tie-breaker, leaving at least one additional product where possible. Featured products are not duplicated below.
- Search is submitted explicitly using the arrow or keyboard search action. It matches case-insensitive substrings in title **or description**. Empty search means browsing; category and search filters combine. Changing category clears search.
- Unknown categories and searches without matches return an empty collection. Unknown product IDs return 404.
- Prices use USD, matching the Fake Store example. No currency conversion or stock promises are implied.
- The default dataset is explicitly bundled demo data because the live API returned a Cloudflare HTTP 403 during implementation. The live adapter remains available and tested with representative HTTP responses; a successful real upstream fetch could not be verified here. There is no silent runtime fallback.

## Enquiries and SMTP

The user selected a local inbox. Mailpit is the local sales inbox for this demonstration, so reviewers need no email-provider account. This demonstrates a real SMTP send/capture, **not external email delivery**. An external sales mailbox is a configuration/deployment step, documented in the example environment file.

Name and email are required; message is optional. The server fixes sender and recipient configuration; the customer email is Reply-To. Text-only messages avoid HTML interpolation. The server validates types, bounds body/message sizes, and limits enquiry requests to ten per minute per IP. Contact details are not logged. Android backup/data transfer is excluded; form restoration uses transient SavedStateHandle state.

The app shows success only after SMTP acceptance, prevents repeated taps during a send, and preserves the form on failure. SMTP acceptance is not a guarantee of eventual delivery to an external recipient. A network timeout can be ambiguous, and a manual retry could duplicate an email. There is no durable queue or cross-request idempotency store in this version. Those belong with a persisted enquiry workflow if delivery guarantees become a requirement.

## Deliberate cuts and next day

No authentication, cart, checkout, favourites, admin interface, application database, offline sync, dark theme, bespoke animations, cloud deployment, or production email-provider setup. These are outside the requested catalogue critical path. The public local demo is not represented as a production-hardened deployment; multi-instance rate limiting and monitoring would need deployment decisions.

With another day: human review and explanation of every decision; more screen sizes, large-font and TalkBack checks; a Compose instrumentation suite; a confirmed live product provider; and external SMTP validation if required. Keep the existing working system small rather than adding features to fill the time box.

## Toolchain

Node 22 LTS is the development/container baseline; Node 24 also passes the host checks. Node 20 was already installed locally but is end-of-life as of this build date, so it was not retained as the project baseline. See the [official release status](https://nodejs.org/en/about/previous-releases). Android uses a pinned compatible AGP 8.13.2 / Gradle 8.14.3 / Kotlin 2.2.21 toolchain and SDK 36. Updating the entire Android toolchain is deferred; lint update advisories are reported rather than suppressed.
