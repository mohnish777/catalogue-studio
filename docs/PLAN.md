# Approved six-hour plan

Goal: a working Android catalogue with premium minimal UI, a locally running backend, and locally captured enquiry emails. One repository, one Android app module, and no application database.

| Allocation | Focus |
| --- | --- |
| 30 minutes | Layout, colours, assumptions, scaffolding |
| 60 minutes | Backend, product source, local email |
| 120 minutes | Android browse/search/detail/enquiry flows |
| 60 minutes | Visual polish and interaction states |
| 45 minutes | Critical tests, emulator walkthrough, fixes |
| 45 minutes | Setup verification, documentation, open PR |

All product data goes through the backend. Home has five editorial picks. Categories have top-rated and additional products. Search matches title/description text. Product details lead to a contact form, SMTP delivery, and thank-you state.

Use warm neutrals, strong typography, clean imagery, and consistent components. Include loading, empty, error, and success states. Reduce decorative polish first if behind. Stop by six focused hours and document actual time and unfinished work honestly.

Tests target selection/filtering, validation, SMTP success/failure, and Android state transitions. Verify the complete app-to-backend-to-inbox flow. Build on a feature branch, raise a PR into main, and leave it open.
