# GEMINI.md (Antigravity Developer Guide)

This file provides guidance to the Gemini / Antigravity AI coding assistant when working with code in the **CreSora Utilities** repository.

## Project Overview

**CreSora Utilities** is a Minecraft mod for Fabric 1.21.7 written in Kotlin and Java. It implements a complex game system combining equipment, weapons, resonance, story progression, and special events (Blood Moon). The architecture blends **service singletons + JSON registries** with a **code generation engine** (Cresora Weapon Compiler).

---

## Build, Test, and Development Commands

Always use Java 21. Prefer the local Gradle cache path already used in this repo:

- **Regenerate weapons and CWC content**:
  ```bash
  GRADLE_USER_HOME=.gradle-user ./gradlew generateWeapons --console=plain
  ```
  Regenerates weapon classes and `cwc_weapon_content.json` from `.cresora` files in `src/main/cresora/`. Runs automatically before compilation.
  
- **Compile and verify (Minimum gate before committing)**:
  ```bash
  GRADLE_USER_HOME=.gradle-user ./gradlew classes --console=plain
  ```
  Compiles all main and compiler classes. Always run this to verify that no compilation errors or regressions are introduced.

- **Run client (Local smoke testing)**:
  ```bash
  GRADLE_USER_HOME=.gradle-user ./gradlew runClient --console=plain
  ```
  Launches a local client for GUI, screen flow, and gameplay smoke tests.

- **Run server (Multiplayer / Command-path verification)**:
  ```bash
  GRADLE_USER_HOME=.gradle-user ./gradlew runServer --console=plain
  ```
  Launches a local server for multiplayer validation or admin command checks.

---

## Project Structure

- **`src/main/kotlin/hifumi/cresora/`** — Server-side mod logic (services, screen handlers, events, items)
- **`src/client/kotlin/hifumi/cresora/`** — Client screens and client-only wiring
- **`src/main/java/` & `src/client/java/`** — Java mixins for entity/player data access bridges
- **`src/compiler/kotlin/hifumi/cresora/compiler/`** — Cresora Weapon Compiler (CWC) engine
- **`src/main/cresora/`** — Weapon definitions in `.cresora` DSL format (compiled to Kotlin + JSON)
- **`src/main/resources/data/cresora-utilities/cresora/`** — Generated and manual JSON registries (equipment, weapons, etc.)
- **`src/main/resources/assets/cresora-utilities/`** — Translations, item models, and textures

---

## Architecture & Systems

### Core Services (Service-Based Singletons)
The mod initializes systems in `CreSoraUtilities.kt` through registries and Fabric's ModInitializer. Key service singletons are stateless and hold transient combat/session data:
- **`EquipmentContentRegistry` / `EquipmentService`** — Artifact equipment with stats and special effects.
- **`WeaponContentRegistry` / `WeaponSkillService`** — Weapon definitions, active skills, and upgrade paths.
- **`ResonanceService`** — Character resonance pull and pity stat system.
- **`AdventureRankService`** — Adventure rank progression and combat HP/damage scaling.
- **`MasqueradeService`** — Character switching, wave challenge, and support systems.
- **`BloodMoonService`** — Special night event, bed protection, and participant wave-clear challenge.
- **`CresoraDebuffService`** — Custom debuff application and tracking.
- **`HotbarOverrideService`** — Handles temporary hotbar session overrides and returns for sub-skills.

### Cresora Weapon Compiler (CWC)
The compiler translates `.cresora` DSL files into:
1. **Kotlin skill class** (`CompiledWeaponSkill*Registry.kt`) implementing `WeaponSkillHandler`.
2. **JSON entries** in `cwc_weapon_content.json` registered in `WeaponContentRegistry`.
3. **Item model JSONs** and basic translation keys.

**Key Compiler Mechanisms**:
- **Transient State Auto-Generation**: CWC automatically generates `clearTransientState(playerId)` and `pruneTransientState(activePlayerIds)` in all generated skill handlers. These maps (like `munenSeqStates`) are pruned by `WeaponSkillService` to prevent memory leaks.
- **Syntax Isolation**: `execute` blocks are wrapped in `run execute@ { ... }` supporting `return@execute` for clean early exits.
- **Dedicated AST Nodes**: Features like `area_of_effect` are structural AST nodes rather than raw string passthroughs.

---

## Coding Style & Quality Guidelines

Follow the style already present in the files you modify; do not reformat code unnecessarily.
- **Naming Conventions**: Use `PascalCase` for types, `camelCase` for members, `UPPER_SNAKE_CASE` for constants. Keep singleton names explicit: `*Service`, `*Registry`, `*Hooks`, `*ScreenHandler`. `.cresora` files should be in `snake_case` matching the weapon IDs.
- **Null Handling**: Leverage Kotlin's standard nullable types and let-binding (`?.let { ... }`); avoid unnecessary double-bang (`!!`) or empty null checks where possible.
- **Input Validation**: Validate inputs strictly at boundaries (commands, UI event handlers); internal callers are trusted.
- **Error Handling**: Log exceptions gracefully and avoid throwing unhandled errors inside tick or combat event handlers to prevent server crashes.

---

## Collaboration & Documentation Maintenance Rules (CRITICAL)

To maintain absolute integrity across agent sessions, you **MUST** keep these collaboration files updated at the project root:

- **`WORK_DONE.md`**
  - Records work that is completed and already landed in the codebase.
  - Update this after finishing code, resource, version, or runtime verification work.
- **`TODO.md`**
  - Records work that still needs follow-up, polish, or testing.
  - New issues, polish items, and future roadmap plans must go here first.
- **`cresora_document.md`**
  - Records current internal API surface, registry schemas, and service responsibilities.
  - **ALWAYS** update this after adding new content types, changing JSON schemas, or modifying service-layer APIs.

### Maintenance Workflow:
1. Completed work goes into `WORK_DONE.md`.
2. Incomplete or follow-up work goes into `TODO.md`.
3. API, registry, or architectural changes go into `cresora_document.md`.
4. **ALWAYS** run a compilation build (e.g., `./gradlew classes`) and verify after completing implementation to ensure no compilation errors or regressions.
5. After large feature changes or bug fixes, update these three files before ending the task.
6. Any future collaborator or agent should read `WORK_DONE.md`, `TODO.md`, and `cresora_document.md` first.
