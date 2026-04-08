# Completed Work

## Enemy / Elite Refactor (2026-04-07)

- Implemented a visual scaling system for elite monsters using the `EntityAttributes.GENERIC_SCALE` attribute (1.21.7 compatible), increasing their model and hitbox size by 18% for better visibility.
- Built a custom debuff system (`CresoraDebuff`) that operates independently of vanilla StatusEffects, allowing for deeper integration with mod-specific combat mechanics.
- Implemented six initial custom debuffs applied by elite monsters:
    - **Nerve Damage**: Reduces attack power by 15% and increases incoming damage by 12%.
    - **Root**: Completely stops movement by applying a -100% speed attribute modifier.
    - **Smoke**: Periodically applies Blindness and provides a visual penalty.
    - **Burn**: Deals periodic fire damage.
    - **Cooldown Penalty**: Slows down weapon skill cooldown progression by 50%.
    - **Heal Block**: Disables all forms of HP recovery (natural regen and weapon skills).
- Refactored `WeaponSkillService` cooldown management to use a tick-based "remaining duration" model instead of a static "expire timestamp", enabling dynamic cooldown speed adjustments (penalties).
- Integrated debuff effects into `NaturalRegenService` (healing check), `WeaponSkillService` (healing and cooldown checks), and `PlayerEntityMixin` (damage/defense multipliers).
- Added localization for all debuffs and system messages in both English (`en_us.json`) and Japanese (`ja_jp.json`).

## UI / Gacha / Core Fixes (2026-04-06)

- Fixed the natural HP regeneration bug in `NaturalRegenService` where recovery would "bank" indefinitely at 19.x HP; implemented fractional recovery to top off health and added an accumulation cap of 1.1 points.
- Fixed a bug where players would only recover to 20 HP upon respawning; added a `pendingFullHeal` flag in `EquipmentAttributeService` to ensure players reach their full calculated maximum health on the first tick after respawn.
- Removed all acquisition methods for the "Wand Upgrader" (`tueshokaku`) from loot tables and material tags.
- Revamped the Gacha (Resonance) system probabilities: `5-star: 0.3%`, `4-star: 8.0%`, `3-star: 15.0%`, `2-star: 76.7%`.
- Implemented a 10-pull 4-star pity system and a 50/50 guarantee (pickup guarantee on next 5-star if previous was a spook).
- Refactored `ArtifactChestScreenBase.kt` to use a modern, polished visual style:
    - Integrated vanilla `generic_54.png` background using the 1.21.2+ `drawGuiTexture` API and `RenderPipelines.GUI`.
    - Automated background slicing to match different slot counts (rows) without texture distortion.
    - Added decorative gray stained-glass pane fillers to empty slots across all 12 custom screens.
- Updated technical documentation in `cresora_document.md` to reflect the 1.21.2+ rendering API and gacha logic changes.
- Split the monolithic `WeaponSkillService.kt` into modular components using a Strategy pattern architecture:
    - Extracted all individual weapon skills (`dark_lux`, `baa_mimic`, `orchid_pavilion_echo`, etc.) into dedicated classes in the `hifumi.cresora.skill` package.
    - Introduced the `WeaponSkillHandler` interface and `WeaponSkillRegistry` to map and delegate skill logic.
    - Moved skill-specific state classes (e.g., `SnowMistState`, `SunlitHasteState`, `BoyaState`) and private methods out of the central service to encapsulate them properly.

## Version And Build

- Target version migrated to `Minecraft 1.21.7`
- `Fabric Loader` updated to `0.18.4`
- `Fabric API` updated to `0.129.0+1.21.7`
- Mod version unified to `1.3.1`
- Mod version updated to `1.3.2`
- Mod version updated to `1.3.2.2`
- Mod version updated to `1.3.2.3`
- Mod version updated to `1.3.2.4`
- Mod version updated to `1.3.2.5`
- Created the `Version_1.3.0_log.md` and `Version_1.3.1_log.md` release note files with all supported log languages
- Created the `Version_1.3.2_log.md` release note file with all supported log languages
- Created the `Version_1.3.2.2_log.md` release note file with all supported log languages
- Created the `Version_1.3.2.3_log.md` release note file with all supported log languages
- Created the `Version_1.3.2.4_log.md` release note file with all supported log languages
- Created the `Version_1.3.2.5_log.md` release note file with all supported log languages
- Built the remapped `1.3.0` and `1.3.1` release jars under `build/libs`
- Built the remapped `1.3.2` release jar under `build/libs`
- Built the remapped `1.3.2.2` release jar under `build/libs`
- Built the remapped `1.3.2.3` release jar under `build/libs`
- Built the remapped `1.3.2.4` release jar under `build/libs`
- Fixed `1.21.7` startup issues caused by missing `registryKey` during item registration

## Documentation

- Created `cresora_document.md` at the project root, summarizing the current internal API surface across initialization, data components, registries, services, commands, and JSON-driven content entry points
- Fixed `Minecraft 1.21.7` item-render routing for recently added weapon / fragment assets by adding the missing `assets/cresora-utilities/items/*.json` item-definition files for `cadenza_allegro`, `kyokusui_no_ryusho`, `pastoral_flute_reverie`, and the shared `weapon_fragment_*` items

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
- Replaced vanilla hunger-based natural regeneration with a custom Adventure Rank table that scales combat and non-combat healing by the player's food bar
- Added combat-state tracking for both dealt and received hits so the custom natural regeneration swaps cleanly between battle and out-of-battle rates
- Moved player `damage_reduction` application from the generic `LivingEntity.damage` hook to `PlayerEntity.damage` so the stat now applies on the real player override path instead of being skipped by the player damage chain
- Added a Masquerade-only safety cap so total player `damage_reduction` cannot exceed `50%` during Masquerade runs, preventing support-buff stacking from trivializing the mode
- Split combat damage into `physical` and `arcane` weapon types, with `requiem_toward_dawn` and `hanwu_juanxue` classified as `arcane` and all other current weapons classified as `physical`
- Added new player-facing `physical_resistance` and `arcane_resistance` stats, surfaced them in `/cresora_stats`, and kept legacy `damage_reduction` as a compatibility layer that still contributes to both resistances
- Updated equipment generation, fallback content defaults, JSON content data, and weapon tooltips so new resistance rolls and damage-type labels now match the split combat model
- Added JSON-backed hostile combat profiles with species-fixed attack types plus per-mob `physical` / `arcane` resistance values
- Applied hostile-side resistance reduction during live combat, so enemy archetypes now actually mitigate incoming typed damage instead of the split model living only on the player side
- Expanded combat feedback to show typed outgoing / incoming damage, enemy resistance lines on hostile overhead labels, and typed floating damage markers including fixed-damage hits
- Fixed `TreasureChestService` so it no longer probes unloaded chunks during cleanup / particle passes or chest-state restore, which was a strong candidate for the post-logout `Can't keep up!` spikes after all player chunks unloaded
- Added persistent hostile classification data for `normal / elite` mob tags plus optional pack IDs, saved directly on mobs alongside the existing Adventure Rank data
- Added natural field pack spawning with a fixed chance, generating `3-5` monsters per pack and guaranteeing `1-2` elites inside each generated pack
- Added visible overhead classification markers and command tags (`cresora_normal_mob`, `cresora_elite_mob`, `cresora_pack_mob`) so normal mobs and elite mobs can now be targeted or inspected cleanly
- Added modest field-only elite combat scaling for pack elites without inflating already-scripted domain / Masquerade elites

## CSC Economy

- Added persistent player currency storage for `CreSora Credits (CSC)` with save/load and respawn-copy support
- Added the player-facing `/cresora_credits` command plus `op` subcommands for credit lookup, grant, and set
- Added `CSC` balance output to `/cresora_stats`
- Replaced artifact-upgrade `XP` cost checks and spending with a fixed `CSC 500` cost
- Synced the upgrade screen preview with server-side CSC balance so the client can lock the button when funds are insufficient
- Added CSC income from hostile kills with enemy-level-based payout scaling
- Added CSC income from artifact pickup and upgrade-material pickup using the existing reward-classification path
- Updated upgrade-related UI and messages across `ja_jp`, `en_us`, `zh_cn`, and `lzh` from `XP` wording to `CSC`
- Expanded `/cresora_shop` into a mixed purchase-and-sale screen with vanilla resource offers and Echo selling
- Added JSON-backed vanilla resource shop offers so iron and other standard materials can be listed in the shop without code edits
- Added fixed CSC sell values for Echoes based on rarity and level, with in-screen sale preview and sale confirmation
- Added new CSC income routes for completed advancements, friendly-mob kills, and positive experience gain
- Routed advancement completion through a dedicated mixin hook so CSC reward is paid when an advancement actually completes, not merely when the UI is opened
- Fixed unintended CSC growth by removing the old generic `PlayerEntity.addExperience` hook and moving experience-based CSC payout to a dedicated `ExperienceOrbEntity` pickup redirect, so CSC now increases only from actual XP orb collection instead of every code path that calls `addExperience`
- Added high-value CSC audit logging in `CreditsService`, so any credit gain/spend of `10,000+` now records the player, delta, total, and originating call path in the server log for fast runtime diagnosis
- Inflated shop-side CSC pricing to better match the current economy scale: vanilla resource packs now cost `20,000-120,000` CSC, `zankyo_kanata_alpha` now costs `50,000` CSC, and `zankyo_kanata_beta` now costs `2,000,000` CSC

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
- Rebalanced weapon attack values so star-2, star-4, and star-5 weapons no longer start below strong vanilla melee options
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
- Replaced one dummy four-star weapon with `requiem_toward_dawn`, a real four-star weapon with a scaling all-damage bonus and a flame-aura skill
- Added the new four-star weapon `gaoshan_liushui` with the composite `healing_aura` skill: immediate self-heal, overflow-to-temporary-guard conversion, and an 8-second nearby ally support aura that heals every 2 seconds and knocks back nearby hostile mobs
- Extended weapon skill definitions to support secondary skill values and tick intervals so composite JSON-driven weapon skills no longer need hardcoded one-off numbers
- Added multilingual tooltip / activation text plus resonance-pool registration for `gaoshan_liushui`, and wired temporary placeholder item resources so it renders without missing-model errors
- Added the new five-star standard weapon `hanwu_juanxue` with a 10-second `snow_frost` combat state, on-hit Frost application, and `Lingering Snow` crit-damage stacks during the skill window
- Added a non-stacking Frost debuff runtime that reapplies slowness and deals escalating freeze damage once per second up to the weapon's current skill cap
- Added the `hanwu_juanxue` snow-environment special rule so its holder gains `+50%` attack while standing in cold / snowy terrain
- Added multilingual tooltip, passive description, activation text, stack feedback, and standard-resonance banner registration for `hanwu_juanxue`
- Fixed the new `dark_lux` on-hit status path so normal attacks now actually apply `Dark`; the previous implementation returned early on non-`hanwu_juanxue` weapons and left the `Dark Lux` branch unreachable
- Reduced `dark_lux` skill cooldown from `40` seconds to `30` seconds in both JSON content and the built-in fallback registry
- Added persistent world save data for field treasure chests so spawned chest positions and rewards now survive server restarts instead of degrading into untracked vanilla chests
- Increased `lakeside_stride` true-damage ratio from `0.5n%` to `5n%`
- Added dedicated temporary texture paths for `requiem_toward_dawn` and its fragment so the weapon no longer points at missing placeholder texture ids
- Added weapon dismantling to the weapon-upgrade screen so weapons can be broken back into their own fragments with level-aware partial refund rules
- Added two new fragment-farming domains, `requiem_reliquary` and `lakeside_sanctum`, for focused `requiem_toward_dawn` and `lakeside_stride` shard runs
- Added three-click confirmation protection for weapon dismantling and for artifact-to-artifact sacrifice upgrades so destructive actions are harder to trigger by mistake

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
- Fixed the domain-exit edge case where a full inventory could interfere with clean reward delivery by forcing overflow rewards to drop at the player's feet after return
- Added an explicit close button to the domain reward screen so players can leave the summary UI cleanly after clear
- Added a JSON-driven story system scaffold with chapter id support such as `0-0`, `0-1`, and `0-2`
- Added story chapters with separate `preBattleStory`, `battle`, and `postBattleStory` sections plus JSON-defined CSC and resonance-currency rewards
- Implemented story chapter `0-0` with its requested pre-battle line, a single `Lv 2` zombie battle wave, its requested post-battle line, and rewards of `CSC 3450`, `代理コード x100`, and `コード進行 x100`
- Added the player-facing `/cresora_story` command plus `/cresora_story start <chapter_id>` execution flow
- Added a shared reward-summary opener so stories can reuse the reward UI without pretending to be domains
- Added a new `/cresora` root command that opens a chest-style CreSora menu for Story, Domains, Shop, and Resonance
- Added chest-style Story chapter-group and stage selection screens so players can choose `第0章 -> 0-0` without typing the chapter id
- Kept the existing `/cresora_domain`, `/cresora_shop`, `/cresora_resonance`, and `/cresora_story start <chapter_id>` commands intact while repointing `/cresora_story` itself to the new story UI
- Reworked story dialogue lines to support translation keys so chapter text can localize per language instead of being hardwired to Japanese
- Added a visible `3, 2, 1` countdown right before story combat begins
- Renamed the user-facing story feature label from `ストーリー / Story` to `楽章演奏` across menus, screens, rewards, and command feedback
- Moved story dialogue text out of `lang/*.json` into a dedicated `data/cresora-utilities/cresora/story_texts.json` bundle so scenario structure and localized prose are now stored separately
- Switched story dialogue rendering to resolve lines from the dedicated story-text bundle with per-player locale lookup plus locale fallback
- Added a shared inventory-entry gate for stage content so both `楽章演奏` and `秘境` now refuse to start when the player's main inventory is completely full
- Extended story chapter content to support JSON-driven `combatHints` and temporary `grantedWeapons` loadouts
- Implemented story chapter `0-1` as the first weapon tutorial, including the requested pre/post dialogue, a `Lv 30` zombie battle, right-click skill guidance, temporary maxed `湖畔を歩む` / `円舞曲のメロディ` distribution, and automatic cleanup of the loaned weapons on exit
- Added a dedicated synced item marker component for story-loaned weapons so temporary tutorial weapons can be reclaimed reliably when a story session ends
- Changed story clear handling so empty-reward chapters no longer open a blank reward chest screen
- Added full `en_us`, `zh_cn`, and `lzh` translations for story chapter `0-1` in the dedicated story text bundle and kept the built-in fallback registry in sync
- Added persistent story-clear progression on players and wired it through save/load plus respawn-copy
- Added prerequisite-chapter support to story chapter definitions, then gated chapter `0-1` behind `0-0` clear in both the UI and `/cresora_story start`
- Updated story list and stage-selection labels so locked chapters now surface their prerequisite chapter instead of pretending they are freely available
- Changed movement-performance rewards to first-clear-only while keeping story chapter replay available
- Added per-chapter story titles backed by the dedicated `story_texts.json` bundle, and surfaced those titles in story selection and `/cresora_story list`
- Added story chapter `0-2` `狂奏`, including the requested pre/post dialogue, temporary max-level `円舞曲のメロディ`, and automatic cleanup of the loaned weapon on exit
- Extended story battle definitions with a survival objective and enemy damage-reduction modifiers, then used them in `0-2` for the `45` second survival fight against a `100%` damage-reduction skeleton
- Refined story dialogue rendering so voiced lines now separate speaker and body more clearly in chat, reducing wrap-induced speaker confusion during long scenes
- Extended story battle modifiers with `trueDamageImmune`, then used it in `0-2` so the training skeleton now ignores fixed-damage burst skills as intended
- Added a live survival countdown action bar for `survive_time` story battles and raised chapter `0-2` enemy rank from `25` to `50`
- Added a dedicated client-side story dialogue screen with `Continue`, `Auto`, and `Skip` controls, replacing the old chat-line-by-line story delivery path
- Added story-specific play-stage payloads so the server now pushes dialogue and countdown state to the client while keeping battle flow authoritative on the server
- Added objective and combat-hint display to both the dedicated dialogue screen and story stage selection tooltips
- Fixed a `Minecraft 1.21.7` client crash in the dedicated story dialogue screen by removing the duplicate per-frame blur path from the custom full-screen renderer
- Fixed a second `Minecraft 1.21.7` story-dialogue client crash by stopping runtime writes to `Screen.title` during dialogue-state updates, which broke when `AUTO` advanced to the next line
- Fixed the story-dialogue close packet so the empty `story_dialogue_close` payload now uses one shared singleton instance, preventing server-side custom-payload encode failures on dialogue exit
- Fixed story chapter `0-2` post-battle speaker assignments so Lumine's concealed-answer line remains voiced and the following memory-loss / pity lines render as narration instead of fake spoken dialogue
- Added the new `Masquerade` endgame mode with a `/cresora` menu entry plus `/cresora_masquerade`
- Added JSON-backed Masquerade wave definitions, per-wave enemy stat modifiers, and JSON-backed support-buff definitions in `masquerade_content.json`
- Added the Masquerade four-weapon loadout selection screen and per-wave `Performance Support` pick screen
- Added Masquerade runtime reward payout based on cleared waves with `CSC` and `Chord Progression`
- Added Masquerade-only combat modifiers including wave enemy damage reduction, stat-buff aggregation, wave-skip support, and five-wave guard support
- Added inventory snapshot / restore handling for Masquerade runs so players only fight with the selected loadout during the session
- Added localized Masquerade UI and support-buff text for `ja_jp`, `en_us`, `zh_cn`, and `lzh`
- Verified at server startup that `masquerade_content.json` loads successfully through the runtime registry path
- Added season-driven Masquerade progress persistence keyed by `seasonId` from `masquerade_content.json`
- Added automatic season rollover that archives the old season best wave / attempts / total cleared waves and resets the current season back to wave `1`
- Added current-season and previous-season Masquerade record display to the loadout screen so the last season best wave remains visible after season updates
- Merged existing domain progression into `楽章演奏` by adding domain-linked story stages `0-0A`, `0-0B`, `0-1A`, `D0-1`, and `D0-2` while keeping the underlying domain combat/reward runtime reusable
- Wired domain-linked story stages so launching from `楽章演奏` now starts the linked domain session, and clearing that session records story progression under the linked stage id
- Removed `requiem_reliquary` from the active domain content bundle for now, leaving it out of the runtime-selectable domain list
- Added localized story-stage titles for the new linked domain entries in `story_texts.json` and updated story-stage card lore to show domain objective, reward focus, entry CSC, and recommended level band
- Updated `/cresora_story list` to show the user-facing stage display labels instead of internal chapter ids
- Folded the `rondo_forge + masquerade_soiree` reward loop into story stage `0-0` itself and folded `credit_drill` rewards into `0-1`, so those chapters now grant repeatable domain-style drops after the story battle ends instead of opening separate `0-0A / 0-0B / 0-1A` stages
- Kept `D0-1` and `D0-2` as the remaining domain-linked side stages inside `楽章演奏`, while removing rank-gate enforcement from the linked-story launch path and lowering current domain unlock ranks to `1`

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
- Re-run local runtime verification on `2026-04-04` using the `/jikki-tesuto` workflow
- Successfully started both dedicated server and client on `Minecraft 1.21.7`
- Confirmed the client can connect to the local server (`localhost:25565`)
- Automatically detected player join (`Player990`) and granted `op` status
- Verified chat feedback for the `op` command in the client console
- Successfully executed the `/jikki-tesuto` (integrated server/client test) on `2026-04-05`
- Confirmed the dedicated server starts, accepts player `Player24`, and automatically grants `op` status
- Confirmed the client connects successfully and receives the `op` feedback in the chat HUD


## Adventure Rank System

- Added a player-specific `Adventure Rank` progression layer with persistent rank and rank XP storage
- Added reusable adventure-rank progression and profile services so future artifact slots can hook into the same scaling backend
- Added item-tag-based reward classification so future artifact categories can contribute to rank XP without rewriting the rank core
- Added hostile-mob scaling that assigns nearby-player-based rank tiers and boosts enemy health and outgoing damage
- Added a hard enemy HP clamp at `500` and converted overflow HP into armor and toughness bonuses instead of letting health keep climbing
- Added mob-type-specific enemy health curves, including a separate Warden curve
- Extended adventure-rank progression from `10` to `70` and replaced the short fixed XP table with a long-form rank curve
- Rebalanced hostile scaling so higher ranks add moderate HP plus rank-based defense instead of relying on raw HP inflation
- Changed field hostile level assignment from exact nearest-player rank to a randomized `nearest rank ±5` roll so natural spawns have local level spread

## Music Echo

- Added a JSON-backed `Music Echo` content registry for recurring version-based mob tuning
- Activated the `v1.3.2` music echo so all mobs now take `3%` less damage
- Verified the `Music Echo` implementation with a successful `./gradlew build`
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

## Resonance System

- Added a JSON-backed resonance banner registry at `data/cresora-utilities/cresora/resonance_content.json`
- Added `/cresora_resonance` with a chest-style UI for the limited and standard resonance banners
- Added the new resonance currency items `chord_progression` and `substitute_chord`
- Added persistent player resonance progress for limited pity, standard pull count, deep-pity streak, and `Arpeggio Concerto` readiness, including respawn-copy support
- Implemented the limited-banner pity curve from pull `100` to `150` and the one-time `Arpeggio Concerto` guarantee within `100` pulls after three consecutive deep-pity featured wins
- Added a resonance result screen and banner/result display stacks so pulls can be checked in-game without relying on chat only
- Migrated resonance currency spending from item-count checks to persistent player-side balance data, matching the `CSC` storage model
- Added `/cresora_resonance currency` plus admin `get / add / set` subcommands for the new resonance balances
- Added result-screen action buttons so players can return to the resonance menu or pull the same banner again immediately
- Corrected the Chinese resonance wording from `代码` to `和弦` in the supported UI strings

## Weapon System Phase 2

- Extended weapon definitions with crit-rate bonuses, optional attack curves, effect-specific skill values, quadratic skill CSC scaling, and artifact-material requirements
- Added the limited `5-star` weapon `lakeside_stride` with `+10%` crit rate, a custom level curve, and a right-click skill that deals fixed current-HP-based damage in a `5m` radius
- Added two placeholder `4-star` resonance-pool weapons, `dummy_four_star_a` and `dummy_four_star_b`, as no-effect fillers so the `4-star` banner table is no longer empty
- Added a dedicated weapon-skill material selection UI for weapons whose skill upgrades consume `4-star+` artifacts instead of using the old instant-upgrade path
- Added placeholder fragment items and vanilla-texture-backed item models for the new resonance and weapon entries
- Updated held-weapon crit handling so weapon crit-rate bonuses apply during live combat instead of staying tooltip-only
- Verified the resonance / weapon expansion build with a successful `GRADLE_USER_HOME=.gradle-user ./gradlew build --console=plain` on `2026-03-28`
- Removed the old weapon-from-fragment crafting interaction so weapon stacks now come from resonance, not right-clicking fragments
- Stopped direct field drops of complete `rondo_melody` and `masquerade_invitation` weapon stacks while keeping fragment drops for growth material loops
- Added dynamic treasure chests around players that award `CSC` plus `Chord Progression`, with star-3 / star-4 / star-5 payouts of `400 + 100`, `1000 + 125`, and `1500 + 150`
- Changed treasure-chest behavior so they no longer expire, no longer enforce owner locks, and now maintain up to `5` active chests per player instead of `1`

## Weapon System Phase 3: Dark Lux

- Added the new ★5 weapon `Dark Lux` (ダーク・ルクス / 暗芒) with complex binary status interaction mechanics
- Implemented the `Dark` status (10s duration) applied on normal attacks, reducing Physical Resistance by `20%`
- Implemented the `Lux` status (30s duration) applied to all monsters within a 5m radius on skill activation, reducing Arcane Resistance by `20%`
- Added the `Binary Interaction` logic triggered when both Dark and Lux exist simultaneously:
    - **Annihilation (20%)**: Target loses `90%` of current HP; up to 3 nearby mobs take `20%` of their Max HP as true damage. Statuses cleared.
    - **Entanglement (20%)**: Target's Physical and Arcane resistance reduced by `50%` for 10s. Prevents re-application of Dark/Lux during this period. Statuses cleared.
    - **Dark Collapse (60%)**: Player heals `5%` Max HP; skill cooldown reduced based on player's missing HP percentage (e.g., `95%` reduction at `1/20` HP).
- Updated `LivingEntityMixin` to dynamically apply resistance offsets from these status effects during damage calculation
- Added `[D]`, `[L]`, and `[E]` status markers to the mob overhead labels in `CombatMobDisplayService` for visual feedback
- Verified the implementation with a successful `./gradlew classes` build
- Added the new `3-star` weapon `牧笛の追想 / 牧笛追想 / Pastoral Flute Reverie` as `pastoral_flute_reverie`
- Implemented the `sunlit_haste` skill so the weapon grants `+40%` move speed for `50s`, upgrades to `+60%` under direct sunlight, and loops cleanly on a matching `50s` cooldown
- Added `pastoral_flute_reverie` to both resonance banner `3-star` pools, localized all supported languages, and supplied placeholder item models so it renders cleanly before custom textures arrive
- Updated weapon-upgrade UI value/unit handling so percent-based move-speed skills no longer display as heart-based effects

- Added the new ★4 weapon `cadenza_allegro` (意気羊々たるカデンツァ / 得意羊羊狂想曲) with the "Baa-Mimic" skill
- Implemented the `baa_mimic` skill logic in `WeaponSkillService` to transform $n$ nearby non-elite enemies into sheep, with $n$ scaling by weapon level (Lv 1-40: 1, 41-60: 2, 61+: 3)
- Added `SheepEntityMixin` to handle loot inheritance from the original transformed monster and implement the "Fluffy Blessing" passive (doubles wool drops when the weapon is in inventory)
- Added localized strings for the new weapon and its unique effects in `ja_jp.json`
- Fixed the `baa_mimic` loot pipeline for `Minecraft 1.21.7` by moving transformed-sheep death handling onto the shared `LivingEntity.dropLoot` path, rebuilding original hostile loot generation with `LootWorldContext`, and preserving Cresora weapon/special drop logic after transformation kills
- Fixed the `cadenza_allegro` passive so sheep death and shearing now grant only the intended extra wool instead of duplicating whole loot tables, and excluded transformed mimic sheep from the passive bonus
- Added the missing `cadenza_allegro` item model, fallback registry entry, resonance-pool registration, multilingual tooltip/passive text, and corrected its JSON upgrade settings to a non-scaling skill configuration
- Repaired corrupted trailing JSON in `en_us`, `zh_cn`, and `lzh` language files and re-verified the whole project with a successful `./gradlew build`
- Added the new standard ★5 weapon `kyokusui_no_ryusho` (曲水流觞 / Kyokusui no Ryusho) with the `orchid_pavilion_echo` skill, including JSON/fallback definitions, placeholder item models, standard resonance registration, and multilingual names/tooltips
- Implemented the full `蘭亭の絶唱 / Orchid Pavilion Echo` runtime: 16-second random 2-second pulses, `Raise a Cup` attack/crit/[之] piercing stacks, `Recite Poetry` armor/natural-regen-stage boosts, `Place a Stone` stackable stone guard, `Ink Brush` invulnerability windows plus nearby ally healing, and automatic cleanup when the skill expires
- Extended live combat hooks so `kyokusui_no_ryusho` buffs now feed into attack attributes, armor attributes, crit damage, natural regeneration, and true-damage follow-up hits, then verified the whole project with a successful `./gradlew build`
- Reworked weapon fragments from weapon-specific items to rarity-shared items (`★2 / ★3 / ★4 / ★5`), added the new shared fragment items/models/localization, and switched domain rewards, field drops, dismantle returns, and upgrade cost checks to the rarity fragment path
- Preserved backward compatibility for legacy weapon-specific fragments by keeping the old items registered and allowing them to be consumed as equivalent same-rarity materials, while blocking all new acquisition routes from producing those legacy fragment items
- Updated domain / linked-story reward labels so `楽章演奏` and domain previews now show the new shared rarity fragment names instead of obsolete weapon-specific fragment names
- Successfully executed the `/jikki-tesuto` (integrated server/client test) on `2026-04-05`
- Confirmed the dedicated server starts, accepts player `Player24`, and automatically grants `op` status
- Confirmed the client connects successfully and receives the `op` feedback in the chat HUD


## Current State

- Core upgrade logic is working
- The vanilla container skeleton is stable
- Phase 1 data scaffolding for the generalized equipment system is now in place
- The first real `EquipmentData`-driven pendant implementation is now in place
- GUI polishing is now focused on in-game readability and visual hierarchy
- The project is ready to continue feature work from a dedicated `dev` branch
- Fixed the natural HP regeneration bug in `NaturalRegenService` where recovery would "bank" indefinitely and fail to reach max HP when the missing health was less than 1.0; implemented fractional recovery to top off health and added an accumulation cap of 1.1 points to prevent "secret" background recovery at full HP
- Removed all acquisition methods for the "Wand Upgrader" (杖昇格ツール / `tueshokaku`) by removing its loot table modification and clearing its item tag, while preserving the item definition for legacy compatibility
- Fixed a bug where players would only recover up to the vanilla 20 HP upon respawning; added a pending heal flag in `EquipmentAttributeService` that ensures players are healed to their full calculated maximum health on the first tick after respawn attributes are applied
- Revamped Resonance (Gacha) system to reduce "chicken race" scenarios: adjusted base weapon probabilities (`5-star: 0.3%`, `4-star: 8%`, `3-star: 15%`, `2-star: 76.7%`) and introduced a hard pity system for 4-stars (every 10th pull guarantees a 4-star or higher if not already hit)
- Fixed a bug in the limited resonance banner where pulling a featured 5-star weapon would sometimes fail to reset the 5-star pity counter; refactored the progress update logic to explicitly set the counter to 0 before state recalculation
- Added [Version_1.2.0_log.md](/Users/hifumimizuhara/IdeaProjects/cresora-utilities-1.21.1/version_log/Version_1.2.0_log.md) as a multilingual version log entry under `version_log/`
- Expanded [Version_1.2.0_log.md](/Users/hifumimizuhara/IdeaProjects/cresora-utilities-1.21.1/version_log/Version_1.2.0_log.md) with a Literary Chinese (Simplified Script) section for release notes only
- Client Mixin added to hide Vanilla player HP bar and render it as numeric text (HP: xxx / yyy) via InGameHudMixin.
- Successfully executed the `/jikki-tesuto` (integrated server/client test) on `2026-04-05`
- Confirmed the dedicated server starts with the updated `ArenaManager.kt` ceiling and lighting logic
- Corrected the `BlockRotation` import path to `net.minecraft.util.BlockRotation` following a compilation failure after manual user changes
- Automatically granted `op` status to `Player798` and verified connection stability
- Confirmed the client connects successfully and receives the `op` feedback in the chat HUD
- Successfully executed the `/jikki-tesuto` (integrated server/client test) on `2026-04-06`
- Confirmed the dedicated server starts, accepts player `Player515`, and automatically grants `op` status
- Confirmed the client connects successfully and receives the `op` feedback in the chat HUD
- Verified that the client transition from login to world render is stable on `Minecraft 1.21.7`
