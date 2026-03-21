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
- Fixed incorrect mod constants
- Added upgrade material maintenance logic
- Clamped the upgrade level cap to `8`

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
- Preserved the design where the equipment effect can still work from the ender chest
- Old saves are not preserved; the mod is now moving forward with the refactored design

## Local Runtime Verification

- Started the local test server multiple times
- Started the local test client multiple times
- Confirmed the client can connect to the local test server
- Granted `op` to current offline-mode test users multiple times
- Issued test equipment and upgrade tools multiple times for GUI and logic verification

## Current State

- Core upgrade logic is working
- The vanilla container skeleton is stable
- GUI polishing is now focused on in-game readability and visual hierarchy
- The project is ready to continue feature work from a dedicated `dev` branch
