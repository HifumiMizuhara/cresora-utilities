# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

**CreSora Utilities** is a Minecraft mod for Fabric 1.21.7 written in Kotlin and Java. It implements a complex game system combining equipment, weapons, resonance, story progression, and special events (Blood Moon). The architecture blends **service singletons + JSON registries** with a **code generation engine** (Cresora Weapon Compiler).

## Build & Development

### Build Commands

```bash
# Build the mod
./gradlew build

# Run the dev server (interactive)
./gradlew runServer

# Clean build
./gradlew clean build

# Just compile without packaging
./gradlew compileKotlin

# Generate weapons from .cresora files (runs automatically before compileKotlin)
./gradlew generateWeapons
```

**Build runs automatically:**
- `generateWeapons` task runs before `compileKotlin`, transforming `.cresora` files in `src/main/cresora/` into Kotlin code and JSON
- If Kotlin compilation fails after changing `.cresora` files, manually run `./gradlew generateWeapons` to diagnose compiler issues

### Project Structure

- **`src/main/kotlin/hifumi/cresora/`** — Main mod logic (services, screen handlers, events)
- **`src/main/java/hifumi/cresora/mixin/`** — Mixin access interfaces for entity/player data
- **`src/compiler/kotlin/hifumi/cresora/compiler/`** — Cresora Weapon Compiler (CWC) implementation
- **`src/main/cresora/`** — Weapon definitions in `.cresora` DSL (compiled to Kotlin + JSON at build time)
- **`src/main/resources/data/cresora-utilities/cresora/`** — Generated and manual JSON registries (equipment, weapons, etc.)
- **`src/main/resources/assets/cresora-utilities/lang/`** — Translations (en_us, ja_jp, lzh, zh_cn)

## Architecture

### Core Systems (Service-Based)

The mod initializes systems in `CreSoraUtilities.kt` through registries and a Fabric ModInitializer. Key service singletons:

- **`EquipmentContentRegistry` / `EquipmentService`** — Artifact equipment with stats and special effects
- **`WeaponContentRegistry` / `WeaponSkillService`** — Weapon definitions, skills, and upgrades
- **`ResonanceService`** — Character resonance stat system
- **`AdventureRankService`** — Adventure rank progression
- **`MasqueradeService`** — Character switching system
- **`BloodMoonService`** — Special night event with waves and rewards
- **`CresoraDebuffService`** — Debuff application and tracking
- **`EquipmentEffectHookService`** — Equipment ability triggers

### Cresora Weapon Compiler (CWC)

**Located:** `src/compiler/kotlin/hifumi/cresora/compiler/`

Transformations per `.cresora` file:

1. **Lexer** (`Lexer.kt`) — Tokenizes, handles `//` and `/* */` comments
2. **Parser** (`Parser.kt`) — Builds AST with semantic validation
3. **Compiler** (`CresoraCompiler.kt`) — Generates:
   - Kotlin service class (`CompiledWeaponSkill*Registry.kt`)
   - JSON entries in `cwc_weapon_content.json`
   - Item model JSON files
   - Translation keys

**Key Structures:**
- `area_of_effect` is a dedicated AST node (not a string passthrough)
- `execute` blocks hold Minecraft command strings; fully-qualified names required
- Data consistency: type-checked at compile time; migratable between versions

**To add weapons:** Write in `src/main/cresora/` with `.cresora` extension. Rebuild to auto-generate.

### Data Components

Registered in `ModDataComponents.kt` and persisted on items:

- `LEVEL` — Legacy equipment level
- `EQUIPMENT_DATA` — Equipment stats/effects
- `WEAPON_DATA` — Weapon state and upgrades
- `STORY_LOAN_SESSION_ID` — Temp weapon tracking
- `MASQUERADE_SESSION_ID` — Character-switch weapon ID

### Mixin Interfaces (Access Bridges)

Java mixin interfaces provide write access to protected Minecraft data:

- `CreditsAccess` — `cresoraGetCredits()`, `cresoraSetCredits()`
- `AdventureRankAccess` — `cresoraGetAdventureRank()`, `cresoraSetAdventureRank()`, rank XP
- `MobEntityAccess` — Mob-level tracking for scaling
- `PlayerEntityAccess` — Player transient combat state

## Key Patterns & Conventions

### Service Pattern

All major features are stateless singletons holding only temporary combat/session data:

```kotlin
object EquipmentService {
    fun applyEquipment(player: ServerPlayerEntity, equipment: Equipment) { ... }
}
```

**Register in:** `CreSoraUtilities.kt` → `onInitialize()` block

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

Mixins are bridged in `src/main/java/hifumi/cresora/mixin/` (Java for compatibility).

## Code Style & Quality

- **Language:** Kotlin primary; Java only for mixins
- **Null handling:** Use Kotlin nullable types and let-binding; avoid null checks
- **Collections:** Prefer immutable types and Sequences for large data
- **Logging:** Use SLF4J via `LoggerFactory.getLogger(MOD_ID)`
- **Identifiers:** Always `Identifier.of(MOD_ID, "name")` for consistency
- **Comments:** Add only when **why** is non-obvious (hidden constraints, workarounds)

### Validation Rules

- **Input validation:** Only at system boundaries (commands, API entry points); trust internal callers
- **Data consistency:** CWC compiler ensures weapon/equipment structural correctness at build time
- **Error handling:** Log and gracefully degrade; avoid throwing from event handlers
- **No backwards-compatibility hacks:** Delete unused code; don't rename or re-export

## Common Development Tasks

### Adding a New Equipment Item

1. Create JSON entry in `src/main/resources/data/cresora-utilities/cresora/artifact_special_items.json`
2. Register in `ArtifactSpecialItemRegistry.init()` (or programmatically)
3. Add translation keys to `en_us.json` and other language files
4. Rebuild to verify JSON validation

### Adding a Weapon Skill

1. Author in `src/main/cresora/*.cresora` (DSL file)
2. Run `./gradlew generateWeapons` to compile
3. Verify generated Kotlin service and JSON in `cwc_weapon_content.json`
4. Add translations if skill has custom feedback messages
5. Link skill to weapon via registry or JSON

### Debugging Blood Moon Events

- Blood Moon state is persisted in `BloodMoonService` and restorable on server restart
- Reward chest ownership is tracked via `BloodMoonRewardChestStateService`
- Command: `/cresora moon clear_special` stops active battle and clears pending state
- Check `MoonPhaseService` for night-phase scheduling conflicts

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

## Important Files & Responsibilities

| File/Package | Responsibility |
|---|---|
| `CreSoraUtilities.kt` | Mod init, item/block creation, registry wiring |
| `ModDataComponents.kt` | Persistent item data definitions |
| `*Service.kt` | Gameplay logic (equipment, weapons, resonance, etc.) |
| `*ContentRegistry.kt` | JSON / programmatic content loading |
| `*ScreenHandler.kt` / `*Screen.kt` | GUI state and rendering |
| `src/compiler/` | `.cresora` → Kotlin/JSON code generation |
| `src/main/resources/data/` | Data-driven definitions (JSON) |
| `src/main/resources/assets/lang/` | Player-facing text and translations |

## Debugging & Troubleshooting

- **Mod won't start:** Check `CreSoraUtilities.onInitialize()` for registration order issues
- **Weapon skill not appearing:** Verify `generateWeapons` completed; check `cwc_weapon_content.json` was generated
- **Screen handler black/won't open:** Ensure `ScreenHandlerType` is registered and client screen is mapped in fabric.mod.json
- **Test run crashes:** Check `run/` for latest log in `logs/latest.log`
- **Mixin access failures:** Verify Java mixin files are in `src/main/java/`, not `src/main/kotlin/`

## Testing

**Current state:** Integration and machine tests exist for combat, equipment scaling, and Blood Moon mechanics (see TODO.md for planned regression coverage).

Run tests (if configured):
```bash
./gradlew test
```

## Notes for Future Work

- CWC import DSL is planned to reduce boilerplate in `execute` blocks
- Remaining raw `execute` strings should be replaced with typed AST nodes
- Parser malformed-file and `area_of_effect` tests are TODO
- Dedicated textures for `moon_brick` and `moon_altar` (currently vanilla placeholders)
- See TODO.md for full roadmap (CWC polish, compiler tests, Blood Moon persistence, special moon phases)
