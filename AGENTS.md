# Catalogue Studio working instructions

These project instructions record the author's workflow for future assistance.

- Work in this repository, not in BAE or unrelated projects. Keep the current feature branch unless the author requests a branch change.
- Implement one requested slice or small chunk at a time, explain its purpose/data flow, give a suggested commit message and stop for review.
- Do not commit, push, create/merge a PR or rewrite Git history. The author owns these actions.
- Keep the PoC small: Kotlin/Compose Android, TypeScript/Node backend, one repository. Do not add modules, wrappers, databases or infrastructure without a demonstrated requirement.
- Product/provider and image access goes through the backend. Keep personal addresses/SDK settings in ignored local.properties and secrets in ignored .env; maintain .env.example.
- Use relevant Android skills where available, while following the author's explicit scope/time constraints. External skills are not build dependencies.
- Run checks proportionate to changed behaviour. Record actual evidence and remaining gaps; do not claim hosted CI or human review from local results.
- Use synthetic data for local enquiry checks. Target an explicit adb serial and restore temporary forwarding/services you own.
- Update project documentation and the existing slice plan when the requested work changes their status. Preserve historical evidence as historical.
