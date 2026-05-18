# Repository Guidelines

## Project Structure & Module Organization
`src/main/kotlin/hifumi/cresora` holds the server-side mod logic: registries, services, hooks, screen handlers, and item systems. `src/client/kotlin/hifumi/cresora` contains client screens and client-only wiring. Java mixins live in `src/main/java` and `src/client/java`. The Cresora Weapon Compiler lives in `src/compiler/kotlin`, while weapon DSL sources live in `src/main/cresora`. Assets and data packs live under `src/main/resources/assets` and `src/main/resources/data`.

## Build, Test, and Development Commands
Use Java 21. Prefer the local Gradle cache path already used in this repo:

- `GRADLE_USER_HOME=.gradle-user ./gradlew generateWeapons --console=plain` regenerates weapon classes and `cwc_weapon_content.json` from `.cresora` files.
- `GRADLE_USER_HOME=.gradle-user ./gradlew classes --console=plain` compiles the mod and is the minimum gate before committing.
- `GRADLE_USER_HOME=.gradle-user ./gradlew runClient --console=plain` launches a local client for GUI and gameplay smoke tests.
- `GRADLE_USER_HOME=.gradle-user ./gradlew runServer --console=plain` launches a local server for multiplayer or command-path checks.

## Coding Style & Naming Conventions
Follow the style already present in the file you touch; do not reformat half the codebase for sport. Use `PascalCase` for types, `camelCase` for members, `UPPER_SNAKE_CASE` for constants, and keep singleton names explicit: `*Service`, `*Registry`, `*Hooks`, `*ScreenHandler`. Keep `.cresora` filenames in `snake_case` and aligned with item or weapon ids.

## Testing Guidelines
There is no real automated test suite wired into `build.gradle` yet. For now, `classes` is mandatory, and gameplay changes should get a manual smoke pass in `runClient` or `runServer`. Compiler or parser work should be checked by rerunning `generateWeapons` and verifying the generated output is intentional.

## Commit & Pull Request Guidelines
Recent history favors short imperative subjects, usually with conventional prefixes such as `feat:` or `chore:`. Keep commits narrow. PRs should state gameplay impact, note any touched registries or generated files, and include screenshots for UI changes. If you modify content schemas, registries, or service APIs, update `cresora_document.md`; log finished work in `WORK_DONE.md` and future follow-up in `TODO.md`.
