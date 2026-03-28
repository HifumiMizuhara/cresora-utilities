# Completed Work

## Version And Build

- Target version migrated to `Minecraft 1.21.7`
- `Fabric Loader` updated to `0.18.4`
- `Fabric API` updated to `0.129.0+1.21.7`
- Mod version unified to `1.3.0`
- Created the `Version_1.3.0_log.md` release note file with all supported log languages
- Built the remapped `1.3.0` release jar under `build/libs`
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

## CSC Economy

- Added persistent player currency storage for `CreSora Credits (CSC)` with save/load and respawn-copy support
- Added the player-facing `/cresora_credits` command plus `op` subcommands for credit lookup, grant, and set
- Added `CSC` balance output to `/cresora_stats`
- Replaced artifact-upgrade `XP` cost checks and spending with a fixed `CSC 500` cost
- Synced the upgrade screen preview with server-side CSC balance so the client can lock the button when funds are insufficient
- Added CSC income from hostile kills with enemy-level-based payout scaling
- Added CSC income from artifact pickup and upgrade-material pickup using the existing reward-classification path
- Updated upgrade-related UI and messages across `ja_jp`, `en_us`, `zh_cn`, and `lzh` from `XP` wording to `CSC`

## Expanded Hostile Reward Coverage

- Expanded artifact and upgrade-material mob drops from the original `zombie / skeleton / warden` set to a much broader hostile pool, excluding the Ender Dragon
- Added JSON-backed loot entries for additional overworld, nether, raid, ocean, and end hostile mobs such as `drowned`, `husk`, `spider`, `creeper`, `witch`, `blaze`, `guardian`, `enderman`, `vindicator`, `evoker`, `ravager`, `piglin_brute`, `shulker`, and more
- Added shared hostile-family classification so expanded enemies now use family-specific CSC reward values, adventure-rank XP payout, and combat scaling instead of falling into one flat default bucket
- Updated drop generation so artifact level growth, rarity-upgrade chance, and extra upgrade-material count can scale from the defeated hostile mob's `Lv`, not only from the killer's adventure rank
- Verified the broadened hostile-drop and reward-scaling changes with a successful `./gradlew build`

## Weapon System Phase 1

- Added a new JSON-backed weapon content registry at `data/cresora-utilities/cresora/weapon_content.json`
- Added the first weapon `rondo_melody` plus its crafting material `rondo_melody_fragment`
- Added synced `weapon_data` with weapon id, rarity, base level, and skill level
- Added a dedicated held-weapon attribute path so custom weapons can grant their own attack damage and sword-like attack speed without reusing the Trinkets artifact pipeline
- Added the `Melody of the Rondo` right-click shield skill with a `15` second duration and `20` second cooldown
- Added shield absorption state on players so incoming damage is consumed until the shield cap is exhausted or the duration expires
- Added generic hostile-kill weapon drop handling so `rondo_melody_fragment` can drop from hostile mobs at `Lv 5+`, and direct `rondo_melody` drops can roll at `Lv 5 / 25 / 45 / 65+` by rarity
- Added a dedicated weapon-upgrade screen with separate base-level and skill-level upgrades
- Added CSC-based weapon growth rules so base levels consume `100 * n^2` CSC plus fragments, while skill levels consume `10000 * n` CSC
- Added multilingual weapon names, upgrade UI text, and skill feedback for `ja_jp`, `en_us`, `zh_cn`, and `lzh`
- Verified the Phase 1 weapon-system implementation with a successful `./gradlew build`
- Fixed the `Melody of the Rondo` shield path so damage absorption still applies when the player has no `damage_reduction` stat bonus equipped
- Replaced the weapon skill cooldown from vanilla item cooldown state with an internal tick-based cooldown so sneak-right-click weapon leveling remains usable during cooldown
- Improved the weapon-upgrade UI so base/skill upgrade lock reasons and current CSC are visible in-screen instead of only appearing as disabled buttons
- Added cooldown progress feedback for weapon skills via action-bar updates while the matching weapon is held
- Added a second JSON-backed star-2 weapon `masquerade_invitation` plus its crafting material `masquerade_invitation_fragment`
- Added generic weapon-skill value handling so weapons can now use non-shield skill payloads such as instant healing without rewriting the weapon UI
- Added the `heal` weapon-skill effect path and implemented `masquerade_invitation` as a `30` second cooldown self-heal weapon with `2 + 0.5n` hearts restored at skill level `n`
- Updated weapon tooltip and weapon-upgrade UI text so skill previews now render from the actual effect type instead of hardcoded shield wording
- Replaced the old player-global weapon cooldown with per-weapon cooldown tracking and per-weapon boss bars, so different weapons no longer lock each other out
- Added multilingual item, tooltip, and upgrade text plus temporary item models for `masquerade_invitation`
- Verified the new heal-weapon implementation with a successful `./gradlew build`

## Artifact Targeting And Shop

- Added a new JSON-backed artifact special-item registry at `data/cresora-utilities/cresora/artifact_special_items.json`
- Added the new shop items `zankyo_kanata_alpha` and `zankyo_kanata_beta`
- Added the player-facing `/cresora_shop` command and a chest-style shop UI that sells the two new special items for CSC
- Added `α` conversion flow from the artifact-upgrade screen so players can open a selection UI and convert an artifact into the same slot from a different set
- Implemented the `α` conversion backend against the JSON-driven equipment registry so future multi-set content can reuse the same selection logic
- Added `β` reforge flow from the artifact-upgrade screen so players can select two stat types and reroll the target artifact straight to max level
- Added a dedicated max-reroll backend for `β` that guarantees one of the selected stat types is hit at least twice during the reroll path
- Added `β` hostile-mob drops with linear chance scaling from rank `45` to rank `70`
- Added multilingual item, shop, and special-upgrade text for `ja_jp`, `en_us`, `zh_cn`, and `lzh`
- Verified the artifact shop / `α` / `β` implementation with a successful `./gradlew build`

## Domain System Phase 1

- Added a new JSON-backed domain registry at `data/cresora-utilities/cresora/domain_content.json`
- Added a new JSON-backed domain reward registry at `data/cresora-utilities/cresora/domain_reward_profiles.json`
- Added the player-facing `/cresora_domain` command and a chest-style domain selection UI
- Added three starter domains for focused farming: `hinagata_archive`, `rondo_forge`, and `credit_drill`
- Added fixed-arena solo domain sessions with CSC entry cost, unlock-rank checks, wave spawning, fail/clear handling, and return-position restore
- Added session-fixed enemy Lv assignment for domain mobs so domain enemies no longer depend on nearby-player rank lookup
- Added domain-specific combat scaling hooks on top of the existing Adventure Rank mob scaling without breaking the global HP clamp / overflow-to-defense rules
- Added domain clear rewards for focused artifact drops, weapon fragment drops, CSC payout, and Adventure Rank XP payout
- Added a domain reward summary UI after clear
- Added a second weapon-fragment farming domain `masquerade_soiree` for `masquerade_invitation`
- Added multilingual domain UI / message text for `ja_jp`, `en_us`, `zh_cn`, and `lzh`
- Verified the Phase 1 domain implementation with a successful `./gradlew build`

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
- Tagged `hinagata_wand` for the Trinkets `chest/necklace` slot
- Added explicit player Trinkets slot definitions so accessory slots render for players
- Added explicit Trinket registration for `hinagata_wand` and restricted it to the `chest/necklace` slot in code
- Added the fallback `trinkets:all` item tag for `hinagata_wand` so Canary accepts pendant insertion more reliably during runtime
- Expanded the artifact lineup to five Trinkets-backed slots: wand, hat, glasses, armor, and boots
- Added temporary vanilla-backed item models for the new Hinagata hat, glasses, armor, and boots pieces
- Generalized artifact data and generation so non-wand pieces now share the same growth backend with slot-specific roll biases
- Added Hinagata set counting and set bonuses through the equipped Trinkets aggregation path
- Verified the Trinkets-integrated build with a successful `./gradlew build`
- Replaced hardcoded equipment slot / set / drop-profile tables with a JSON-driven content bundle at `data/cresora-utilities/cresora/equipment_content.json`
- Moved slot definitions, set bonuses, equipment definitions, and mob loot rules onto ID-based runtime registries
- Changed `EquipmentData` to store slot and set IDs instead of enum-backed values while keeping the saved component field names stable
- Added rarity-weight support per equipment definition so future artifacts can tune 3-star / 4-star / 5-star roll odds without code edits
- Generalized mob artifact loot pools so future artifact pieces can be routed from JSON-defined loot rules instead of hand-written `when` branches
- Added a set-summary line to `/cresora_stats` so players can confirm active set piece counts and active thresholds in-game
- Added placeholder set-effect hook triggers for equip change, attack dealt, damage taken, kill, and tick events so future non-numeric set effects can be attached cleanly
- Verified the JSON-driven equipment-content refactor with a successful `./gradlew build`
- Renamed the wand item ID and asset path from `hinagatastue` to `hinagata_wand` to match the equipment-definition naming scheme
- Replaced hardcoded per-item equipment registration in `CreSoraUtilities` with auto-registration from `equipment_content.json`
- Moved the upgrade-GUI open behavior into `ArtifactEquipmentItem` via a definition flag so future equipment items do not need one-off subclasses
- Changed set-bonus tooltips to render from JSON `stats` / `effectHooks` data instead of relying on fixed translation descriptions, so tooltip text now follows the actual configured effect values
- Replaced the temporary vanilla item textures for the Hinagata set, `rondo_melody`, `masquerade_invitation`, both weapon fragments, and `zankyo_kanata_alpha / beta` with the provided custom PNG assets from the project `texture` folder
- Updated the corresponding item models so those assets now resolve through `cresora-utilities:item/...` instead of falling back to vanilla placeholder icons

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
- Added enemy-family-biased initial stat rolls so zombie, skeleton, and warden pendants now lean toward different build archetypes
- Verified the mob-biased drop generation changes with a successful `./gradlew build`
- Added action-bar combat feedback for outgoing damage bonus and incoming damage reduction
- Re-verified the expanded combat feedback with a successful `./gradlew build`
- Removed the non-critical outgoing-damage action-bar spam so crit feedback remains visible during live combat
- Updated the crit-combo action-bar text ordering across all supported languages
- Expanded the upgrade screen flow so any supported artifact piece can be upgraded or used as sacrifice material
- Verified the five-piece artifact expansion with a successful `./gradlew build`

## Adventure Rank System

- Added a player-specific `Adventure Rank` progression layer with persistent rank and rank XP storage
- Added reusable adventure-rank progression and profile services so future artifact slots can hook into the same scaling backend
- Added item-tag-based reward classification so future artifact categories can contribute to rank XP without rewriting the rank core
- Added hostile-mob scaling that assigns nearby-player-based rank tiers and boosts enemy health and outgoing damage
- Added a hard enemy HP clamp at `500` and converted overflow HP into armor and toughness bonuses instead of letting health keep climbing
- Added mob-type-specific enemy health curves, including a separate Warden curve
- Extended adventure-rank progression from `10` to `70` and replaced the short fixed XP table with a long-form rank curve
- Rebalanced hostile scaling so higher ranks add moderate HP plus rank-based defense instead of relying on raw HP inflation
- Added hostile overhead labels that show live `Lv` and current `HP / Max HP`
- Added floating damage numbers above hostile mobs on successful hits
- Switched hostile spawn-time rank assignment from the highest nearby player to the nearest nearby player
- Added adventure-rank-aware loot scaling for pendant level, rarity upgrades, and upgrade-material extra drops
- Expanded hostile artifact drops so zombies, skeletons, and wardens can roll any Hinagata set piece instead of wand-only drops
- Added rank XP gain from hostile kills and ground pickup of tagged artifacts / upgrade materials
- Added the player-facing `/cresora_rank` command plus `op` subcommands for rank testing and manual progression control
- Added adventure-rank summary output to `/cresora_stats`
- Added localized adventure-rank command and stat text for `ja_jp`, `en_us`, `zh_cn`, and `lzh`
- Verified the adventure-rank implementation with a successful `./gradlew build`

## Current State

- Core upgrade logic is working
- The vanilla container skeleton is stable
- Phase 1 data scaffolding for the generalized equipment system is now in place
- The first real `EquipmentData`-driven pendant implementation is now in place
- GUI polishing is now focused on in-game readability and visual hierarchy
- The project is ready to continue feature work from a dedicated `dev` branch
- Added [Version_1.2.0_log.md](/Users/hifumimizuhara/IdeaProjects/cresora-utilities-1.21.1/version_log/Version_1.2.0_log.md) as a multilingual version log entry under `version_log/`
- Expanded [Version_1.2.0_log.md](/Users/hifumimizuhara/IdeaProjects/cresora-utilities-1.21.1/version_log/Version_1.2.0_log.md) with a Literary Chinese (Simplified Script) section for release notes only
