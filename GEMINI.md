# AGENTS

Keep these collaboration files updated at the project root:

- `WORK_DONE.md`
  - Records work that is completed and already landed
  - Update this after finishing code, resource, version, or runtime verification work
- `TODO.md`
  - Records work that still needs follow-up
  - New issues, polish items, and future plans should go here first
- `cresora_document.md`
  - Records current internal API surface, registry schemas, and service responsibilities
  - ALWAYS update this after adding new content types, changing JSON schemas, or modifying service-layer APIs

Maintenance rules:

- Completed work goes into `WORK_DONE.md`
- Incomplete or follow-up work goes into `TODO.md`
- API, registry, or architectural changes go into `cresora_document.md`
- ALWAYS run a build (e.g., `./gradlew classes`) and verify after completing implementation to ensure no compilation errors or regressions.
- After large feature changes, update these three files before ending the task
- Any future collaborator or agent should read `WORK_DONE.md`, `TODO.md`, and `cresora_document.md` first
