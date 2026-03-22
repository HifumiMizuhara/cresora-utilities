# Completed Work

## Version And Build

- Target version migrated to `Minecraft 1.21.7`
- `Fabric Loader` updated to `0.18.4`
- `Fabric API` updated to `0.129.0+1.21.7`
- Mod version unified to `1.2.0`
- Fixed `1.21.7` startup issues caused by missing `registryKey` during item registration

## Core Logic Fixes

- Corrected `MOD_ID` to `cresora-utilities`
- Aligned the `level` data component namespace to `cresora-utilities:level`
- Added the new `equipment_data` component scaffold for future generalized equipment growth
- Fixed incorrect mod constants
- Added upgrade material maintenance logic
- Clamped the upgrade level cap to `8`
- Applied all equipment stats to players, including crit, damage bonus, and reduction
- Switched live equipment stat lookup from inventory scanning to equipped Trinkets items
- Changed percent-based attack, health, and armor bonuses to use total-value multiplication instead of base-only multiplication
- Removed double-counting of `ALL_DMG_BONUS` from the attack attribute path so it only affects final damage once

## Equipment Refactor Phase 1

- Added the reusable `StatType`, `StatEntry`, `EquipmentRarity`, and `EquipmentData` model types
- Registered `cresora-utilities:equipment_data` as a synced data component
- Added a compatibility support layer so legacy `level`-based pendants also emit normalized `EquipmentData`
- Updated existing pendant tooltip, loot initialization, and upgrade writes to keep `level` and `equipment_data` in sync
- Verified the Phase 1 scaffolding with a successful `./gradlew build`

## Equipment Refactor Phase 2 And 3

- Added randomized pendant generation for `rarity`, `mainStat`, and `subStats`
- Added normalization and growth rules for duplicate stat rejection, rarity caps, and `+4` growth events
- Switched pendant upgrade progression to operate on real `EquipmentData`
- Updated pendant attribute application to aggregate equipment stats into attack, health, and armor bonuses
- Added tooltip detail rendering for rarity, main stat, sub stats, and next growth timing
- Added max-level handling in the upgrade flow and corresponding translation keys
- Verified the new equipment-system implementation with a successful `./gradlew build`

## Upgrade System Refactor

- Replaced the old offhand upgrade flow with a dedicated upgrade GUI
- Added centralized upgrade logic in `src/main/kotlin/hifumi/cresora/UpgradeLogic.kt`
- Added the screen handler in `src/main/kotlin/hifumi/cresora/UpgradeScreenHandler.kt`
- Added the client screen in `src/client/kotlin/hifumi/cresora/UpgradeScreen.kt`
- Made right-click on the main equipment open the upgrade GUI
- Registered the client `HandledScreen`

## Resources And GUI

- Added the missing `1.21.7` item resource declarations
- Fixed missing textures for the upgrade tool
- Fixed missing textures for the main equipment item
- Added the upgrade GUI texture resource at `src/main/resources/assets/cresora-utilities/textures/gui/upgrade.png`
- Reworked slot positions, player inventory alignment, and button placement multiple times
- Added and updated GUI translation keys
- Fixed floating and overlapping GUI text issues
- Fixed overlap between the custom GUI and vanilla inventory / crafting screens
- Unified the GUI title around the generic “equipment upgrade” direction
- Shortened GUI wording to compact labels such as `XP`, `Lv`, and `RATE`
- Reduced the GUI back to vanilla container dimensions and inventory rhythm
- Fixed transparent text color issues in the vanilla-sized GUI
- Styled the upper half as a workstation-like layout while keeping the vanilla container skeleton
- Reduced on-screen text density after in-game testing showed the compact panel was still overloaded
- Moved informational metrics back to the left panel and reduced the right panel to result-focused interaction
- Verified each major GUI iteration with successful Gradle builds

## Preserved Existing Design Choices

- Preserved the `versionverifier` namespace behavior
- Old saves are not preserved; the mod is now moving forward with the refactored design

## Trinkets Integration

- Added `Trinkets Canary` as a runtime dependency for `Minecraft 1.21.7`
- Added direct `Cardinal Components` dependencies required by the Trinkets API on the compile classpath
- Moved pendant stat aggregation to the Trinkets equipped-item component instead of selecting the strongest inventory copy
- Tagged `hinagatastue` for the Trinkets `chest/necklace` slot
- Added explicit player Trinkets slot definitions so accessory slots render for players
- Added explicit Trinket registration for `hinagatastue` and restricted it to the `chest/necklace` slot in code
- Added the fallback `trinkets:all` item tag for `hinagatastue` so Canary accepts pendant insertion more reliably during runtime
- Verified the Trinkets-integrated build with a successful `./gradlew build`

## Local Runtime Verification

- Started the local test server multiple times
- Started the local test client multiple times
- Confirmed the client can connect to the local test server
- Granted `op` to current offline-mode test users multiple times
- Issued test equipment and upgrade tools multiple times for GUI and logic verification
- Re-ran local runtime startup verification after the `EquipmentData` refactor
- Confirmed the dedicated server reaches `Done` on `1.21.7` with the new equipment system
- Confirmed the local client reaches the main render/bootstrap stage with the new equipment system
- Re-ran local runtime verification on `2026-03-22`
- Confirmed the dedicated server reaches `Done`, accepts the `about` command, and shuts down cleanly
- Confirmed the local client auto-connects to `localhost:25565` and joins as `Player167` without mod startup errors
- Added an in-game `/cresora_stats` debug command to report equipped stat totals and current player attributes
- Converted `/cresora_stats` into a player-facing stat overview command with permission level `0`
- Updated `/cresora_stats` to always show all 10 supported buff stat types with localized labels
- Removed the misleading pendant tooltip summary line that mixed flat ATK and ATK% into one number
- Reworked pendant mob drops into source-based rarity tiers so zombies, skeletons, and wardens no longer share the same quality band
- Added a base critical profile of `5%` crit rate and `50%` crit damage before equipment bonuses are applied
- Updated `/cresora_stats` to show effective crit values including the base crit profile instead of showing `0%` when no gear crit bonus is equipped
- Added visible crit feedback via action-bar text and crit-hit sound so runtime combat checks are easier to verify
- Compressed `/cresora_stats` so all 10 supported buff stats remain visible with far fewer chat lines
- Updated crit action-bar feedback to show the applied crit multiplier during combat
- Re-verified the compressed stat command and crit-display changes with a successful `./gradlew build`

## Current State

- Core upgrade logic is working
- The vanilla container skeleton is stable
- Phase 1 data scaffolding for the generalized equipment system is now in place
- The first real `EquipmentData`-driven pendant implementation is now in place
- GUI polishing is now focused on in-game readability and visual hierarchy
- The project is ready to continue feature work from a dedicated `dev` branch
