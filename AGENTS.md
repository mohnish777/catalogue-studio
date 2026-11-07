# Catalogue Studio

Keep the implementation small and explainable within a six-hour budget.
Android uses one app module with domain, data, presentation, and ui packages.
Use Kotlin, Compose, StateFlow/MVI, Koin, Ktor, and type-safe navigation.
Keep network calls out of composables. Preserve coroutine cancellation.
All catalogue data must come through the Node/TypeScript backend.
Keep the product source and SMTP delivery replaceable. No application database.
Use local SMTP for review; never commit credentials or send real mail in tests.
Run backend tests/typecheck and Android unit tests/build/lint before submission.
Document assumptions, limits, actual verification, AI corrections, and time honestly.
Use a feature branch, open a PR into main, and leave it open.
