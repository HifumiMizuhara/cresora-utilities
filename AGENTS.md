# GEMINI.md (Antigravity Developer Guide)

This file provides guidance to the Gemini / Antigravity AI coding assistant when working with code in the **CreSora Utilities** repository.

## Terminology Rule

- Effective immediately, any reference to `Phase〇`, `Phase N`, or `Phase` in this repository means the phases defined in [roadmap.md](/Users/hifumimizuhara/IdeaProjects/cresora-utilities-1.21.7/roadmap.md).
- Do not interpret `Phase` references using ad hoc labels from commit messages or other progress logs unless the prompt explicitly says so.

## Project Overview

**CreSora Utilities** is a Minecraft mod for Fabric 1.21.7 written in Kotlin and Java. It implements a complex game system combining equipment, weapons, resonance, story progression, and special events (Blood Moon). The architecture blends **service singletons + JSON registries** with a **code generation engine** (Cresora Weapon Compiler).

---

## Build, Test, and Development Commands

Always use Java 21. Prefer the local Gradle cache path already used in this repo (`GRADLE_USER_HOME=.gradle-user`).

### Key Commands

- **Compile all DSL assets (Weapons, Artifacts, Movements)**:
  ```bash
  GRADLE_USER_HOME=.gradle-user ./gradlew compileAssets --console=plain
  ```
  Compiles `.cresora`, `.artifact`, and `.movement` files in `src/main/cresora/` to Kotlin code, JSON configurations, and translation maps. Runs automatically before compilation. If compilation fails after changing DSL files, manually run this to diagnose compiler issues.

- **Compile and verify (Minimum gate before committing)**:
  ```bash
  GRADLE_USER_HOME=.gradle-user ./gradlew classes --console=plain
  ```
  Compiles all main and compiler classes. Always run this to verify that no compilation errors or regressions are introduced.

- **Run client (Local smoke testing / GUI and gameplay)**:
  ```bash
  GRADLE_USER_HOME=.gradle-user ./gradlew runClient --console=plain
  ```
  Launches a local client for GUI, screen flow, and gameplay smoke tests.

- **Run server (Multiplayer / Command-path verification)**:
  ```bash
  GRADLE_USER_HOME=.gradle-user ./gradlew runServer --console=plain
  ```
  Launches a local server for multiplayer validation or admin command checks.

- **Clean the build cache**:
  ```bash
  GRADLE_USER_HOME=.gradle-user ./gradlew clean --console=plain
  ```

- **Run tests (if configured)**:
  ```bash
  GRADLE_USER_HOME=.gradle-user ./gradlew test --console=plain
  ```

---

## Project Structure

- **`src/main/kotlin/hifumi/cresora/`** — Server-side mod logic: registries, services, hooks, screen handlers, and item systems.
- **`src/client/kotlin/hifumi/cresora/`** — Client screens and client-only wiring.
- **`src/main/java/` & `src/client/java/`** — Java mixins for entity/player data access bridges.
- **`src/compiler/kotlin/hifumi/cresora/compiler/`** — Cresora Weapon Compiler (CWC) implementation.
- **`src/main/cresora/`** — Weapon definitions in `.cresora` DSL format (compiled to Kotlin + JSON).
- **`src/main/resources/data/cresora-utilities/cresora/`** — Generated and manual JSON registries (equipment, weapons, etc.).
- **`src/main/resources/assets/cresora-utilities/`** — Translations, item models, and textures.
  - `src/main/resources/assets/cresora-utilities/lang/` — Translations (en_us, ja_jp, lzh, zh_cn).

---

## Architecture & Systems

### Core Services (Service-Based Singletons)
The mod initializes systems in `CreSoraUtilities.kt` through registries and Fabric's ModInitializer. Key service singletons are stateless and hold transient combat/session data:
- **`EquipmentContentRegistry` / `EquipmentService`** — Artifact equipment with stats and special effects.
- **`EquipmentEffectHookService`** — Equipment ability triggers.
- **`WeaponContentRegistry` / `WeaponSkillService`** — Weapon definitions, active skills, and upgrade paths.
- **`ResonanceService`** — Character resonance pull and pity stat system.
- **`AdventureRankService`** — Adventure rank progression and combat HP/damage scaling.
- **`MasqueradeService`** — Character switching, wave challenge, and support systems.
- **`BloodMoonService`** — Special night event, bed protection, and participant wave-clear challenge.
- **`CresoraDebuffService`** — Custom debuff application and tracking.
- **`HotbarOverrideService`** — Handles temporary hotbar session overrides and returns for sub-skills.
- **`GuideService`** — Beginner's guide (Adventurer's Handbook) task tracking and rewards.
- **`SpiritBondService` / `SpiritRenderer`** (Phase 2) — Server-side service syncs `WEAPON_DATA.spiritBondStage` once per second from `ResonanceService` spirit bond points; client renderer draws an orbiting orb particle around players holding a spirit weapon (`END_ROD` base, additional `ENCHANT` at stage ≥3, `GLOW` at stage ≥5).

### Cresora Weapon Compiler (CWC)
**Located:** `src/compiler/kotlin/hifumi/cresora/compiler/`

The compiler translates `.cresora` DSL files into:
1. **Kotlin skill class** (`CompiledWeaponSkill*Registry.kt`) implementing `WeaponSkillHandler`.
2. **JSON entries** in `cwc_weapon_content.json` registered in `WeaponContentRegistry`.
3. **Item model JSONs** and basic translation keys.

**Compiler Transformation Steps:**
1. **Lexer** (`Lexer.kt`) — Tokenizes inputs, handles `//` and `/* */` comments.
2. **Parser** (`Parser.kt`) — Builds an AST with semantic validation.
3. **Compiler** (`CresoraCompiler.kt`) — Generates the Kotlin registry classes, JSON weapon content, model files, and translation keys.

**Key Compiler Mechanisms & AST Nodes**:
- **Transient State Auto-Generation**: CWC automatically generates `clearTransientState(playerId)` and `pruneTransientState(activePlayerIds)` in all generated skill handlers. These maps (like `munenSeqStates`) are pruned by `WeaponSkillService` to prevent memory leaks.
- **Syntax Isolation**: `execute` blocks are wrapped in `run execute@ { ... }` supporting `return@execute` for clean early exits. Fully-qualified names are required inside `execute` blocks.
- **Dedicated AST Nodes**: Features like `area_of_effect` are structural AST nodes rather than raw string passthroughs.
- **Spirit Block (Phase 2)**: Optional `spirit { name, awakening_condition, voice_lines { key: translationKey }, bond_stage N { title, story } }` block. The parser validates `bond_stage` numbers (1..6, unique, no missing fields) and emits a `spirit` object in `cwc_weapon_content.json`. Consumed at runtime by `SpiritBondService` / `SpiritRenderer` / weapon tooltips. Production 5★ weapons must define this block — guarded by `WeaponSpiritRegistryTest`.
- **Data Consistency**: Type-checked at compile time; migratable between versions.

**To add weapons:** Write in `src/main/cresora/` with `.cresora` extension. Rebuild to auto-generate.

### Data Components
Registered in `ModDataComponents.kt` and persisted on items:
- `LEVEL` — Legacy equipment level.
- `EQUIPMENT_DATA` — Equipment stats and special effects.
- `WEAPON_DATA` — Weapon state and upgrade progress.
- `STORY_LOAN_SESSION_ID` — Temporary weapon tracking.
- `MASQUERADE_SESSION_ID` — Character-switch session weapon ID.

### Mixin Interfaces (Access Bridges)
Java mixin interfaces provide write access to protected Minecraft data and are bridged in `src/main/java/` / `src/client/java/` (using Java for compatibility):
- `CreditsAccess` — `cresoraGetCredits()`, `cresoraSetCredits()`
- `AdventureRankAccess` — `cresoraGetAdventureRank()`, `cresoraSetAdventureRank()`, rank XP
- `MobEntityAccess` — Mob-level tracking for scaling
- `PlayerEntityAccess` — Player transient combat state
- `GuideProgressAccess` — Guide progression and task details.

---

## Design Patterns & Conventions

### Service Pattern
All major features are stateless singletons holding only temporary combat/session data:
```kotlin
object EquipmentService {
    fun applyEquipment(player: ServerPlayerEntity, equipment: Equipment) { ... }
}
```
**Register in:** `CreSoraUtilities.kt` → `onInitialize()` block.

### Registry Pattern
Content registries load from JSON but also accept programmatic entries:
```kotlin
EquipmentContentRegistry.register("unique_boots", EquipmentData(...))
```
Registries are final before gameplay starts; queries use snapshot lookups.

### Hook/Event Pattern
Fabric callbacks (e.g., `AttackBlockCallback`, `ServerTickEvents.END_TICK`) trigger service methods:
```kotlin
AttackBlockCallback.EVENT.register { player, world, hand, pos, direction ->
    BloodMoonService.tryHandleSpecialBlockInteraction(player, pos)
    ActionResult.PASS
}
```

### Mixin Access Pattern
Kotlin code calls mixin interface methods to read/write protected data:
```kotlin
(player as CreditsAccess).cresoraSetCredits(newAmount)
```

---

## Coding Style & Quality Guidelines

Follow the style already present in the files you modify; do not reformat code unnecessarily.
- **Naming Conventions**: Use `PascalCase` for types, `camelCase` for members, `UPPER_SNAKE_CASE` for constants. Keep singleton names explicit: `*Service`, `*Registry`, `*Hooks`, `*ScreenHandler`. Keep `.cresora` filenames in `snake_case` matching the weapon IDs.
- **Null Handling**: Leverage Kotlin's standard nullable types and let-binding (`?.let { ... }`); avoid unnecessary double-bang (`!!`) or empty null checks where possible.
- **Collections**: Prefer immutable types and `Sequence`s for large datasets.
- **Logging**: Use SLF4J via `LoggerFactory.getLogger(MOD_ID)`.
- **Identifiers**: Always use `Identifier.of(MOD_ID, "name")` for consistency.
- **Comments**: Add only when **why** is non-obvious (hidden constraints, workarounds).
- **Input Validation**: Validate inputs strictly at boundaries (commands, UI event handlers); internal callers are trusted.
- **Error Handling**: Log exceptions gracefully and avoid throwing unhandled errors inside tick or combat event handlers to prevent server crashes.
- **No backwards-compatibility hacks**: Delete unused code; do not rename or re-export.

---

## Common Development Tasks

### Adding a New Equipment Item
1. Create a JSON entry in `src/main/resources/data/cresora-utilities/cresora/artifact_special_items.json`.
2. Register in `ArtifactSpecialItemRegistry.init()` (or programmatically).
3. Add translation keys to `en_us.json` and other language files under `src/main/resources/assets/cresora-utilities/lang/`.
4. Rebuild (`GRADLE_USER_HOME=.gradle-user ./gradlew classes`) to verify JSON validation.

### Adding a Weapon Skill
1. Author the skill in `src/main/cresora/*.cresora` (DSL file).
2. Run `GRADLE_USER_HOME=.gradle-user ./gradlew compileAssets` to compile.
3. Verify the generated Kotlin service and JSON in `cwc_weapon_content.json`.
4. Add translations if the skill has custom feedback messages.
5. Link the skill to a weapon via registry or JSON.

### Debugging Blood Moon Events
- Blood Moon state is persisted in `BloodMoonService` and restorable on server restart.
- Reward chest ownership is tracked via `BloodMoonRewardChestStateService`.
- Command: `/cresora moon clear_special` stops active battles and clears pending state.
- Check `MoonPhaseService` for night-phase scheduling conflicts.

### Working with Screen Handlers
Screen handlers are registered in `CreSoraUtilities.kt`:
```kotlin
UPGRADE_SCREEN_HANDLER = Registry.register(
    Registries.SCREEN_HANDLER,
    Identifier.of(MOD_ID, "upgrade"),
    ScreenHandlerType { syncId, inventory -> UpgradeScreenHandler(syncId, inventory) }
)
```
Client screens live in `src/main/resources/assets/cresora-utilities/textures/gui/`.

---

## Important Files & Responsibilities

| File/Package | Responsibility |
|---|---|
| `CreSoraUtilities.kt` | Mod initialization, item/block creation, registry wiring. |
| `ModDataComponents.kt` | Persistent item data definitions. |
| `*Service.kt` | Gameplay logic (equipment, weapons, resonance, etc.). |
| `*ContentRegistry.kt` | JSON / programmatic content loading. |
| `*ScreenHandler.kt` / `*Screen.kt` | GUI state and rendering. |
| `src/compiler/` | `.cresora` → Kotlin/JSON code generation. |
| `src/main/resources/data/` | Data-driven definitions (JSON). |
| `src/main/resources/assets/lang/` | Player-facing text and translations. |

---

## Debugging & Troubleshooting

- **Mod won't start**: Check `CreSoraUtilities.onInitialize()` for registration order issues.
- **Weapon skill not appearing**: Verify `compileAssets` completed; check `cwc_weapon_content.json` was generated.
- **Screen handler black/won't open**: Ensure `ScreenHandlerType` is registered and the client screen is mapped in `fabric.mod.json`.
- **Test run crashes**: Check the `run/` directory for the latest log in `logs/latest.log`.
- **Mixin access failures**: Verify Java mixin files are in `src/main/java/` or `src/client/java/`, not under `kotlin/` source directories.

---

## Testing Guidelines

Running `classes` compilation is mandatory, and gameplay changes **MUST** get a manual smoke pass in **both** `runClient` and `runServer`.

### Smoke Test Requirement (CRITICAL)

**Every change that touches runtime behavior, registries, worldgen, dimensions, or data-driven JSON MUST be verified with both server and client smoke tests:**

```bash
GRADLE_USER_HOME=.gradle-user ./gradlew runServer --console=plain
GRADLE_USER_HOME=.gradle-user ./gradlew runClient --console=plain
```

- `runServer` validates that the server can start without registry loading errors, dimension initialization failures, or datapack validation crashes.
- `runClient` validates that the client can start and that GUI, rendering, and client-server handshakes work end-to-end.
- A `classes` compile-only pass is **not sufficient** — many schema errors (e.g., missing biome `features`, invalid `carvers` format) only surface at runtime during registry/JSON loading.

### Compiler Tests

The CWC/CAC compilers have an automated test suite in the `compilerTest` source set (`src/compilerTest/kotlin/`, JUnit 5), covering lexer/parser error handling, `area_of_effect` parsing, typed action-node validation, `InstructionMapping` expansion, and golden codegen snapshots. Run it after any compiler or parser change:
```bash
GRADLE_USER_HOME=.gradle-user ./gradlew compilerTest --console=plain
```
Golden expectations live in `src/compilerTest/resources/golden/`; when compiler output changes intentionally, the failing test writes the actual output to `build/golden-actual/` — review the diff and copy it over the expectation. Compiler work should also be checked by rerunning `compileAssets` and verifying the generated output diff is intentional.

The main mod (gameplay) code still has no automated test suite.

---

## Commit & Pull Request Guidelines

**Commit messages must be written in English.**

Recent history favors short imperative subjects, usually with conventional prefixes such as `feat:` or `chore:`. Keep commits narrow. PRs should state gameplay impact, note any touched registries or generated files, and include screenshots for UI changes. If you modify content schemas, registries, or service APIs, update `cresora_document.md`. Completed work is recorded by the commit itself (`git log`); record only future follow-up in `TODO.md`.

---

## Collaboration & Documentation Maintenance Rules (CRITICAL)

To maintain integrity across agent sessions, keep these two living docs accurate. There is **no `WORK_DONE.md`** — completed work is recorded by Git history (`git log`), not a hand-maintained log.

- **`cresora_document.md`** (committed; the canonical reference)
  - Records current internal API surface, registry schemas, and service responsibilities.
  - **ALWAYS** update this after adding new content types, changing JSON schemas, or modifying service-layer APIs.
- **`TODO.md`** (local, gitignored scratch list)
  - A lean list of pending follow-up, polish, and verification work **only**.
  - Add new follow-up items here; delete each item once it ships. Do not keep a completed-items log — `git log` is the record of finished work.

### Maintenance Workflow:
1. API, registry, or architectural changes go into `cresora_document.md`.
2. Remaining follow-up / verification work goes into `TODO.md`; delete entries when finished.
3. **ALWAYS** run a compilation build (e.g., `GRADLE_USER_HOME=.gradle-user ./gradlew classes --console=plain`) and verify after completing implementation to ensure no compilation errors or regressions.
4. After large feature changes or bug fixes, update these docs before ending the task.
5. Any future collaborator or agent should read `cresora_document.md` and `TODO.md` first, and consult `git log` for what has already shipped.

## Notes for Future Work

- Typed action nodes (2026-06): `apply_mark`, `grant_invulnerability`, `add_buff`, `start_cooldown`, `send_message`, and `apply_status_effect` are parsed into dedicated AST nodes with parse-time validation. Duration semantics: `Ns` = seconds, bare number = ticks, `skill_duration` = the skill's duration. Arbitrary Kotlin still flows through `ExpressionNode`.
- Unknown DSL commands now fail `compileAssets` instead of being emitted as comments.
- Dedicated textures for `moon_brick` and `moon_altar` (currently vanilla placeholders).
- See `TODO.md` for the full roadmap (CWC polish, compiler tests, Blood Moon persistence, special moon phases).
