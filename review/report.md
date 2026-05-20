# Review Report

## Scope

- Reviewed the weapon-upgrade and weapon-skill material selection flow, because it is a stateful UI path with item custody across screen transitions.
- Verification gate: `GRADLE_USER_HOME=.gradle-user ./gradlew classes --console=plain`

## Findings

### 1. Weapon could be ejected from the upgrade flow during skill-material selection

- Severity: High
- Location: `src/main/kotlin/hifumi/cresora/WeaponUpgradeScreenHandler.kt`
- Root cause:
  Before this fix, pressing the skill-upgrade button opened `WeaponSkillMaterialScreenHandler` with a reference to the current weapon stack, but the weapon itself was still owned by `WeaponUpgradeScreenHandler`. When the old screen closed, `onClosed()` returned that weapon to `playerInventory.offerOrDrop(...)`.
- Impact:
  On a full inventory, the target weapon could be dropped on the ground merely by opening material selection. Even when it was not dropped, the upgrade target stopped being modal and could drift back into the player inventory mid-flow.
- Fix status: Fixed
- Fix:
  The weapon is now explicitly removed from the upgrade handler before opening the material-selection screen, and the next screen becomes the owner of that stack until success or cancellation.
- Evidence:
  `WeaponUpgradeScreenHandler` now accepts an `initialWeapon`, seeds the custom slot from it, and removes the weapon with `takeWeaponStack()` before opening the next screen.
  See `src/main/kotlin/hifumi/cresora/WeaponUpgradeScreenHandler.kt:16`, `:51`, `:231`, `:242`
  `ArtifactUiFlow.openWeaponUpgrade(...)` now accepts a carried weapon stack so the upgraded weapon can be restored into the reopened screen instead of being lost back into inventory.
  See `src/main/kotlin/hifumi/cresora/ArtifactUiFlow.kt:45`

### 2. Material-selection screen was not actually modal

- Severity: High
- Location: `src/main/kotlin/hifumi/cresora/WeaponSkillMaterialScreenHandler.kt`
- Root cause:
  Non-option slot clicks still fell through to the base `ScreenHandler` logic. That meant the player inventory remained interactive while the material-selection screen was open.
- Impact:
  Players could move or drop inventory items, including the target weapon that had just been handed across screens. That makes the selected-material upgrade path nondeterministic and opens the door to stale references and state corruption.
- Fix status: Fixed
- Fix:
  The material-selection screen now consumes all non-option clicks, returns the carried weapon on cancel, and reopens the upgrade screen with the same weapon on success.
- Evidence:
  `onSlotClick(...)` now returns after handling the option grid instead of delegating player-inventory clicks.
  `onClosed(...)` now returns the carried weapon when the flow is cancelled.
  Successful completion now calls `ArtifactUiFlow.openWeaponUpgrade(serverPlayer, weaponStack)`.
  See `src/main/kotlin/hifumi/cresora/WeaponSkillMaterialScreenHandler.kt:66`, `:75`, `:112`

## Verification

- `GRADLE_USER_HOME=.gradle-user ./gradlew classes --console=plain`
- Result: `BUILD SUCCESSFUL`

## Current Status

- Confirmed bugs found in the reviewed flow: 2
- Confirmed bugs fixed: 2
- Compile gate after fixes: Passed
