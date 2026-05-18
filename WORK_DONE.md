# WORK_DONE

## CWC Default Imports & Active Slot Weapon Skill Isolation (2026-05-18)

- **CWC default imports generation**:
  - Enhanced the Cresora Weapon Compiler ([CresoraCompiler.kt](file:///Users/hifumimizuhara/IdeaProjects/cresora-utilities-1.21.7/src/compiler/kotlin/hifumi/cresora/compiler/CresoraCompiler.kt)) to automatically inject common imports (such as `Text`, `LivingEntity`, `ServerWorld`, `ParticleTypes`, `WeaponSkillService`, `AdventureRankService`, and others) into generated Kotlin skill singletons.
  - Simplified package naming in DSL files; fully-qualified names are no longer required in `execute` blocks.
  - Migrated and simplified [tanboku_chokuu.cresora](file:///Users/hifumimizuhara/IdeaProjects/cresora-utilities-1.21.7/src/main/cresora/tanboku_chokuu.cresora) to use these clean, translatable names.
- **Active Slot Weapon Skill Isolation & Swapping Hardening (Anti-Stat/Buff/Shield Leakage)**:
  - Hardened [WeaponSkillService](file:///Users/hifumimizuhara/IdeaProjects/cresora-utilities-1.21.7/src/main/kotlin/hifumi/cresora/WeaponSkillService.kt) to keep weapon-specific bonuses (like crit damage and crit rate) isolated to the active main-hand weapon, preventing inactive slot weapons from bleeding stats.
  - Introduced player-specific held weapon tracking (`lastHeldWeaponIdByPlayer`).
  - Added state cleansing triggers: when a player swaps their active weapon (or unequips it), the compiler-generated `clearTransientState(uuid)` is automatically triggered for the old weapon to purge active buffs (like Tao, Munen series, Zanso field states) instantly.
  - **Tao Swap Cleansing Fix (P2)**: Resolved a leak where `taoStacks` (stored directly on `WeaponSkillService` instead of the generated handler) were kept when swapping away from *Tanboku Chokuu*. It is now explicitly cleared upon weapon swap.
  - **Dynamic Shield Weapon Binding Fallback (P2)**: Fixed a bug where weapon-based shields could not be cleared on swap because the shield's `weaponId` was default-initialized to `null` by compilers/skills. `grantShield` now automatically resolves and binds the active held weapon ID if `weaponId` is not explicitly passed.
- **Verification**:
  - Recompiled and verified that the entire workspace builds successfully using `GRADLE_USER_HOME=.gradle-user ./gradlew generateWeapons classes --console=plain`.

## CWC Transient State Cleanup & Weapon Upgrade UI Flow Hardening (2026-05-18)

- **CWC transient state auto-generation & cleanup integration**:
  - Enhanced the Cresora Weapon Compiler ([CresoraCompiler.kt](file:///Users/hifumimizuhara/IdeaProjects/cresora-utilities-1.21.7/src/compiler/kotlin/hifumi/cresora/compiler/CresoraCompiler.kt)) to automatically generate `clearTransientState(playerId: UUID)` and `pruneTransientState(activePlayerIds: Set<UUID>)` override functions in all generated weapon skill classes.
  - Added these methods to the [WeaponSkillHandler](file:///Users/hifumimizuhara/IdeaProjects/cresora-utilities-1.21.7/src/main/kotlin/hifumi/cresora/skill/WeaponSkillHandler.kt) interface so individual skills can cleanly reset player-specific transient states (e.g. active buff state maps, stack trackers).
  - Integrated cleanup triggers into [WeaponSkillService](file:///Users/hifumimizuhara/IdeaProjects/cresora-utilities-1.21.7/src/main/kotlin/hifumi/cresora/WeaponSkillService.kt):
    - `pruneTransientState` is called during the server tick loop (`ServerTickEvents.END_SERVER_TICK`) for active player IDs.
    - `clearTransientState` is triggered when resetting player-specific combat stats (e.g., on player death/disconnects).
- **Weapon Upgrade & Material UI Stack Safety (Anti-Loss Protection)**:
  - Updated [ArtifactUiFlow.openWeaponUpgrade](file:///Users/hifumimizuhara/IdeaProjects/cresora-utilities-1.21.7/src/main/kotlin/hifumi/cresora/ArtifactUiFlow.kt) and [WeaponUpgradeScreenHandler](file:///Users/hifumimizuhara/IdeaProjects/cresora-utilities-1.21.7/src/main/kotlin/hifumi/cresora/WeaponUpgradeScreenHandler.kt) to accept and set an initial `ItemStack` on the upgrade slot.
  - Improved material selection flow: when switching from weapon upgrade to material selection ([WeaponSkillMaterialScreenHandler](file:///Users/hifumimizuhara/IdeaProjects/cresora-utilities-1.21.7/src/main/kotlin/hifumi/cresora/WeaponSkillMaterialScreenHandler.kt)), the weapon item is safely carried over to the sub-handler.
  - Implemented robust anti-loss protection: if the player closes the material selection UI without finishing the upgrade, `onClosed(player)` automatically returns the weapon stack back to the player's inventory (`offerOrDrop`).
  - Fixed multiple-return and item-loss bugs during UI transitions by safely managing the `ItemStack` lifecycle and clearing references (`weaponStack = ItemStack.EMPTY`) after successful transitions or handoffs.

## Blood Moon Retry + Bed Swap Fix (2026-05-16)

- Fixed the special-bed swap so the Blood War bed is force-updated as a paired structure instead of replacing the two halves independently.
- This prevents the battle bed from self-destructing the moment the Blood War starts.
- Added an always-on floating HP label above the Blood War bed so its durability reads like a live combat target.
- Tightened Blood War night locking so the overworld is held in night only while an active Blood War session exists.
- This lets the day index advance after victory or failure, which restores normal "next night can challenge again" behavior.
- Verified build: `GRADLE_USER_HOME=.gradle-user ./gradlew classes --console=plain` (success).

## Blood Moon Bed Defense Pass (2026-05-16)

- Reworked `BloodMoonService` so the Blood War bed becomes a dedicated objective instead of dead scenery.
- Blood War now converts the chosen bed into a special red bed for the duration of the battle, restores it on non-failure exits, and destroys it on defeat.
- Blood War mobs now path toward the bed, attack it on cadence, and consume bed durability from attack count plus mob rank.
- Added bed durability guard rails:
  - At 75% / 50% / 25%, the bed hardens and becomes invulnerable until the current wave ends.
  - The invulnerability is removed when the next wave starts.
  - If durability reaches 0%, the Blood War fails immediately.
- Players still cannot break or attack the battle bed.
- Active Blood War now locks the overworld in night; dawn is rewound until the battle ends.
- Blood War rewards now include adventure-rank XP equal to 50% of the player's current-rank XP cap.
- Added localized battle-bed guard/failure messages in `en_us`, `zh_cn`, `ja_jp`, and `lzh`.
- Verified build: `GRADLE_USER_HOME=.gradle-user ./gradlew classes --console=plain` (success).

## Blood Moon Battle Debug Sweep (2026-05-16)

- Fixed a major player-state corruption bug in `BloodMoonService`:
  - Blood War bed capture now stores each participant's original respawn point.
  - The battle-only respawn override is restored on victory, abort, debug stop, reconnect, and death-respawn handoff.
- Tightened battle protection to the actual battle bed instead of freezing block breaking across the entire 50-block arena.
- Stopped late passersby from being auto-enlisted into an in-progress Blood War, which also closes the free-reward and unwanted-respawn-override loophole.
- Added abandoned-session cleanup:
  - If every participant is offline, the active Blood War now tears down cleanly.
  - `DO_MOB_GRIEFING`, tracked mobs, and saved respawn overrides are all restored instead of leaving the night soft-locked.
- Hardened reward chest runtime tracking to use world + position keys so same-coordinate blocks in other dimensions cannot collide with Blood War rewards.
- Verified build: `GRADLE_USER_HOME=.gradle-user ./gradlew classes --console=plain` (success).

## Blood Moon Stabilization Sweep (2026-05-10)

- Removed duplicate blood-moon night rollback from `MoonPhaseService`; time-slow is now single-sourced in `BloodMoonService` and only active during night window.
- Unified blood-moon stop flow:
  - `BloodMoonService.stopAndClearNight(server)` added as canonical teardown path.
  - `/cresora moon clear_special` now routes through blood-moon teardown when needed.
  - `/cresora moon set_special <non-blood>` is blocked while blood war is active.
- Fixed wave reliability:
  - Wave spawn now counts successful spawns.
  - Zero-spawn wave no longer advances wave counters; it retries after short delay with warning.
  - Added per-mob spawn retries to reduce terrain collision failures.
- Battle protection expanded:
  - Added `PlayerBlockBreakEvents.BEFORE` guard.
  - Reused protected feedback for blocked interactions.
  - Kept TNT ignite interception through bed-zone interaction checks.
- Rest countdown UI switched to pure seconds for the real-time 30s rest rule.
- Reward chest robustness improved:
  - Added persistent chest ownership/seed state (`BloodMoonRewardChestState.kt`) so ownership and reward reconstruction survive reload/restart.
  - Added fallback direct reward grant when chest placement slots are insufficient.
- Added new localization keys (`wave_spawn_retry`, `reward_fallback`, `blocked_during_blood_war`) and updated rest wording in `en_us/zh_cn/ja_jp/lzh`.
- Verified build: `GRADLE_USER_HOME=.gradle-user ./gradlew classes --console=plain` (success).

## Moon Altar + Drop Loop (2026-05-10)

- Added `moon_brick` item and global hostile drop: 2%.
- Added `moon_altar` interactive block and crafting recipe (8 moon bricks in a ring).
- Added altar interaction flow:
  - Right-click altar with `blood_note` consumes 1 note (except creative).
  - Schedules the **next night** as guaranteed blood moon via `MoonPhaseService.scheduleBloodMoonForNextNight`.
- Extended moon persistent state with `forcedBloodMoonDay` so altar scheduling survives restart and is consumed when that night resolves.
- Enabled `blood_note` hostile drop at fixed 0.5% by updating artifact special-item drop config.
- Added block/item resources and localization for `moon_brick`, `moon_altar`, and altar feedback messages.
- Verified build: `GRADLE_USER_HOME=.gradle-user ./gradlew classes --console=plain` (success).

## Weapon Migration to CWC (Cresora Weapon Compiler) - Phase 1

Completed the migration of 5 hardcoded weapon implementations to CWC DSL.

### Infrastructure Improvements
- **Lexer & Parser Enhancement**: Added offset-based source extraction for `execute` blocks. This ensures that raw Kotlin code written in `.cresora` files is preserved exactly as-is, including complex operators (`as?`, `?.`, `!!`), comments, and formatting.
- **Compiler Code Generation**:
    - Wrapped `execute` blocks in `run execute@ { ... }` to support `return@execute` for early exits.
    - Improved action separation with mandatory newlines.
    - Fixed registry access for status effects to match 1.21.7 API.
    - Fixed `ignite` (AOE) to use float values for fire duration.

### Migrated Weapons
- **Rondo Melody**: Successfully moved to `rondo_melody.cresora`.
- **Masquerade Invitation**: Successfully moved to `masquerade_invitation.cresora`.
- **Requiem Toward Dawn**: Successfully moved to `requiem_toward_dawn.cresora`.
- **Pastoral Flute Reverie**: Successfully moved to `pastoral_flute_reverie.cresora`.
- **Cadenza Allegro**: Successfully moved to `cadenza_allegro.cresora`.

### Cleanup & Refactoring
- Removed legacy Kotlin handlers: `ShieldSkill.kt`, `HealSkill.kt`, `FlameAuraSkill.kt`, `SunlitHasteSkill.kt`, `BaaMimicSkill.kt`.
- Removed legacy JSON entries from `src/main/resources/data/cresora-utilities/cresora/weapon_content.json`.
- Updated `WeaponSkillRegistry.kt` to use `CompiledWeaponSkillRegistry` for all migrated weapons.

### Verification
- `generateWeapons` Gradle task executes successfully.
- `classes` Gradle task executes successfully (project builds).

## Weapon Migration to CWC - Phase 2 (Completion)

Successfully migrated all remaining weapons to CWC, eliminating manual handlers and unifying the skill system.

### Achievements
- **Full Migration**: Migrated `Dark Lux`, `Cold Mist, Coiling Snow` (Hanwu Juanxue), `High Mountains, Flowing Water` (Gaoshan Liushui), and `Kyokusui no Ryusho`.
- **API Unification**:
    - Unified damage processing hooks (`onDamageDealt`, `onDamageTaken`, `onDamageAbsorbed`) with weapon context integration.
    - Generalized attribute scalars (Attack Damage, Armor, Crit) and regen stage bonuses to move away from weapon-specific hardcoding in `WeaponAttributeService` and `NaturalRegenService`.
- **Compiler Maturity**:
    - Fixed Minecraft 1.21.7 API changes (World parameter in damage methods).
    - Resolved Kotlin syntax issues in generated code (trailing lambdas, property wrapping in long comments).
- **Localization Alignment**: Fixed translation keys for Japanese, Chinese, and Classical Chinese across all weapons.

### Verification
- `gradlew classes` completed successfully.
- All 11+ weapons are now managed via `.cresora` files.

## Weapon Migration to CWC - Phase 2-2 (Localization & Logic Refinement)

Integrated all multi-language support and specialized skill messaging directly into the `.cresora` scripts, making the system fully data-driven and decentralized.

### Key Achievements
- **Translations Block Integration**: Every `.cresora` file now contains a `translations` block supporting `en_us`, `ja_jp`, `zh_cn`, and `lzh` (Classical Chinese).
- **Smart Messaging**: Refactored `execute` blocks to use `player.sendMessage` with `Text.translatable` for detailed, formatted notifications (e.g., displaying specific stack counts, recovery amounts, or enemy transformation counts).
- **Core API Enhancements**:
    - Added `WeaponSkillService.grantShield` to provide a clean, standardized way for scripts to apply shields.
- **Improved Script Quality**: Standardized color schemes for skill notifications (Aqua for shields, Green for healing, Gold for fire/crit, etc.).

### Verification
- `gradlew classes` successfully compiles- [x] 多言語対応（zh_cn, lzh）の基盤実装および「千秋、葉落ちて」への適用
- [x] CWC デバッグ: `send_localized_message` の構文エラー修正、1.21.7 向けアイテム定義（items/）生成対応
- [x] CWC 拡張: 武器ごとの `texture` プロパティ追加
- [x] ビジュアル改善: 「千秋、葉落ちて」専用テクスチャの生成と適用
- [x] 翻訳改善: バフ翻訳の自動フォールバック実装
基本機能・スキル（断雷・斬霜・叶落満地傷）の実装
- サブスキル生成時の。Kotlin 構文エラー（改行不足）の修正
- モデルの親クラスを `minecraft:item/handheld` 等に完全修飾化し、カスタム namespace 下でのテクスチャ欠落を修正
- CWC の拡張: `dictionary` ブロックを導入し、アイテムタグ等の共通翻訳を `.cresora` ファイルで一元管理可能に
- 武器「千秋、葉落ちて」および共通タグに中国語（zh_cn）と文言（lzh）の翻訳を追加
- 武器の欠片（fragment）の翻訳自動生成サポート

## CWC Sub-Skill & Buff Fixes

Resolved critical issues in the Cresora Weapon Compiler (CWC) that prevented sub-skills from correctly accessing buff states and Minecraft 1.21.x APIs.

### Infrastructure Improvements
- **Sub-Skill Context Integration**: Updated `generateSubSkillClass` to pass the weapon's main skill context to the code generator. This allows sub-skills to correctly identify and interact with buffs defined at the weapon level.
- **Buff State Sharing**: Modified the compiler to prefix buff state maps (e.g., `munenSeqStates`) and state classes with the main skill's class name (e.g., `TanmokuChokuuSkill.munenSeqStates`). This enables seamless state sharing between a weapon's main skill and its sub-skills.
- **Execute Block Macros**: Added automatic expansion of the `close_skill_menu()` macro within `execute` blocks, translating it to `hifumi.cresora.HotbarOverrideService.restoreHotbar(player)`.
- **Weapon Registration Fix**: Updated `updateWeaponJson` to include mandatory `craft` fields (`fragmentItemId`, `fragmentBaseItemId`, `craftedBaseLevel`, `craftedSkillLevel`) in the generated JSON. This ensures all CWC-managed weapons are correctly parsed and registered as in-game items by `WeaponContentRegistry`.

### Weapon Logic Refinement (Tanmoku Chokuu)
- **API 1.21.x Compatibility**:
    - Fixed `target.kill()` to `target.kill(world)` to match the updated Minecraft API.
    - Ensured `world` is correctly cast to `ServerWorld` before calling `damage` or `spawnParticles` within `execute` blocks.
    - Replaced `iterateEntities()` with more robust `getOtherEntities` calls to improve type inference and performance.
- **Bug Fixes**:
    - Corrected `cresoraIsElite()` to `cresoraIsEliteMob()` in the `Zanso` sub-skill logic.
    - Fixed particle type mismatch for `DUST_COLOR_TRANSITION` by switching to simpler, more reliable particle types (`CRIT`, `SNOWFLAKE`) where appropriate.

- `gradlew classes` now completes successfully with all generated handlers compiling correctly.

+## CWC Localization & Modeling Expansion
+
+Enhanced the Cresora Weapon Compiler (CWC) and core weapon systems to support cleaner localization and custom placeholder models.
+
+### Infrastructure Improvements
+- **DSL Localization Enhancement**:
+    - Added `send_localized_message(key, color, args...)` action to the DSL. This provides a cleaner way to send translatable messages with placeholders (`%s`, etc.) without writing manual Kotlin code in `execute` blocks.
+    - Updated the code generator to translate this action into standard `player.sendMessage(Text.translatable(key, ...).formatted(COLOR), true)` calls.
+- **Custom Model Data Support**:
+    - Added `custom_model_data` property to the `weapon` block in the DSL.
+    - Updated `WeaponDefinition` and the core registry to parse and store `customModelData`.
+    - Modified `WeaponStackSupport` to automatically apply the `CUSTOM_MODEL_DATA` component to weapon item stacks if defined in the weapon's definition.
+- **Asset Automation**:
+    - Added `updateItemModels` to the compiler. It now automatically generates missing item model JSON files (`assets/cresora-utilities/models/item/<id>.json`) for weapons, using the `base_item` as a parent and placeholder texture.
+    - This ensures all weapons defined in `.cresora` files have a visible in-game presence even before custom assets are created.
+
+### Weapon Logic Refinement
+- **Tanmoku Chokuu & Dark Lux**:
+    - Assigned temporary `custom_model_data` (10101, 10102) to provide a base for future unique modeling.
+    - Standardized skill activation and feedback messages using the new localization action.
+
+### Verification
+- Successfully ran `./gradlew generateWeapons` to verify parser and code generation.
+- Successfully ran `./gradlew classes` to ensure 1.21.7 API compatibility (including the overhauled `CustomModelDataComponent` constructor).

## Weapon Implementation: "Qianqiu, Yeluo" (千秋、葉落ちて)

Successfully implemented the new 5-star weapon "Qianqiu, Yeluo" using the CWC (Cresora Weapon Compiler) framework.

### Accomplishments
- **Complex Logic Implementation**:
    - **Passive System**: 60% damage reduction, dynamic damage bonus based on missing HP, and self-damage mechanics integrated into `on_damage_taken` and `on_player_tick`.
    - **Skill Sub-menus**: Implemented the `open_skill_menu` pattern to select between Danrai, Zansou, and Yoraku-manchisho.
    - **技能①：断雷 (Danrai)**: Added "Jingtian" state with arcane damage and 30% root chance.
    - **技能②：斬霜 (Zansou)**: Implemented HP cost, arcane resistance reduction, and Skill 1 enhancement.
    - **技能③：叶落満地傷 (Yoraku-manchisho)**: Implemented stack-based activation, HP sturdy effect, AoE arcane damage, and cumulative stat scaling (ATK/Crit Dmg).
- **Localization**: Full English and Japanese support for skill messages and stack tracking.
- **Visuals**: Assigned temporary custom model data (10103) for future modeling.

### Verification
- **Build Success**: Successfully executed `./gradlew generateWeapons classes` with "BUILD SUCCESSFUL" status.
- **Compiler Fix**: Fixed a bug in `CresoraCompiler.kt` where sub-skill actions lacked newlines, causing syntax errors in generated code.
- **Logic Validation**: Verified arcane damage scaling, HP-based boosts, and AOE entity filtering in the generated Kotlin code.

## CWC Sub-Skill UI & Tanboku Rename Fixes

Resolved persistent display and localization issues related to the Sub-skill menu and the weapon "Tanboku Chokuu".

### Key Achievements
- **Tanboku Chokuu Rename & Localization**:
    - Renamed all occurrences of `tanmoku` to `tanboku` to match user preference and corrected English/Japanese name mappings.
    - Synced translation keys between `.cresora` files and `.json` lang files, fixing the "Raw Key" display during skill activation.
- **Sub-skill Icon Texture Fix (The "Dummy Skill" purple texture)**:
    - **Dynamic Icon Selection**: Enhanced `CresoraCompiler.kt` to automatically generate a `sub_skill_dummy.json` item model using the modern `minecraft:select` (1.21.7 compatible) mechanism. This allows the sub-skill menu to dynamically show the correct icons (e.g., iron sword for Danro, iron axe for Zanso).
    - **Global Model Fix**: Batch updated all mod-defined item models to include the missing `minecraft:` namespace in their parent definitions (e.g., `minecraft:item/handheld`), resolving several instances of missing textures.
    - Fixed missing translations for sub-skills in `.cresora` files.
    - Fixed sub-skill icon textures (purple/black) by correcting block model paths in `CresoraCompiler.kt`.
    - Added `message.cresora.hotbar_overridden` translation to `common_tags.cresora`.
    - Recompiled and verified generated assets using `./gradlew classes`.
- **Sub-skill Name Overrides**:
    - Modified `SubSkillItem.kt` to dynamically resolve and return translated sub-skill names (e.g., 「断露」) instead of the internal dummy item name.

### Verification
- `gradlew generateWeapons` successfully generates the overhauled `sub_skill_dummy.json`.
- `gradlew classes` compiles successfully, confirming 1.21.7 API and model selector compatibility.


## Machine Test Execution (2026-04-18)

Successfully performed the actual machine test by launching both the client and server.

### Accomplishments
- **Environment Launch**: Successfully launched Minecraft 1.21.7 client and server via `./gradlew runClient` and `./gradlew runServer`.
- **Automated OP**: Detected User "Player855" joining the server and automatically granted operator privileges via the server console input (`op Player855`).
- **Functionality Check**: Confirmed the player could join, receive OP, and switch to creative mode, indicating smooth communication between client and server.

### Verification
- Server log confirms: `Made Player855 a server operator`
- Client chat log confirms: `[Server: 已将Player855设为服务器管理员]` and `已将自己的游戏模式设置为创造模式`

## Tanboku Chokuu Sub-skill Runtime Fix

Fixed critical runtime issues in the `tanboku_chokuu` sub-skill flow.

### Fixes
- Preserved the parent weapon definition and `WeaponData` during hotbar override so sub-skills no longer execute with `WeaponDefinition.DUMMY` / `WeaponData.DUMMY`.
- Added CWC-generated parent-to-sub-skill registration so sub-skill `onPlayerTick` handlers continue ticking while the parent weapon is held.

## Moon Phase Cycle and Night Scaling

Implemented the server-side moon cycle system.

### Highlights
- Added an 11-day moon phase loop with the ordered phases `朔、既朔、上弦、逾弦、几望、望、既望、退望、下弦、残月、晦`.
- Added nightly 18:00 announcements in the format `夜晚降临,今晚是...`.
- Added mutually exclusive special moon rolls for Blood Moon, Solar Eclipse, Lunar Eclipse, Death Moon, and `？？`.
- Wired moon-based mob HP, damage, and displayed level scaling into the existing hostile mob pipeline.
- Added admin-facing moon inspection and calibration commands.

### Verification
- Build verification has not yet been run after this change set.
- Reworked `tanboku_chokuu.cresora` so Zanso and Bokuchu Munen only apply state, show activation messages, and close the skill menu after their Tao checks succeed.
- Fixed the Tao gain message to show the actual current Tao value instead of a literal Kotlin expression string.

### Verification
- `GRADLE_USER_HOME=.gradle-user ./gradlew generateWeapons --console=plain`
- `GRADLE_USER_HOME=.gradle-user ./gradlew classes --console=plain`

## Blood Moon Special Phase

Implemented the first detailed special moon phase.

### Highlights
- Blood Moon now guarantees at least one occurrence per 11-day moon cycle.
- Blood Moon nights slow night-time progression by 50%, block bed sleeping through bed interaction capture, increase natural hostile spawn pressure by duplicating eligible natural spawns at 20%, and apply global hostile HP/damage bonuses through existing adventure-rank scaling.
- Added `BloodMoonService` with bed-triggered `血色战争` sessions: confirmation interaction, 50-block participant capture, locked bed protection, no mob griefing during the session, 20 waves, 30 in-game minute rest windows, and final-wave elite withers.
- Added participant stacking buffs for the challenge: +10% outgoing damage and +20% max HP per cleared wave.
- Added one reward chest per participant with 100k CSC, one 5-star artifact set, `blood_note`, and placeholder weapon reward rolls for `lossless_crown` and `blood_tear`.
- Added `blood_note` as an artifact special item kind `note`, plus placeholder weapon definitions in CWC test content.
- Added Blood Moon translations for `zh_cn`, `en_us`, `ja_jp`, and `lzh`.

### Verification
- `./gradlew classes --console=plain`

## Blood Moon Battle Timing Fixes

Fixed two live gameplay regressions in `血色战争`.

### Highlights
- Added explicit active-wave tracking so a newly opened Blood Moon battle no longer auto-clears the first wave before any mob has spawned.
- Changed Blood Moon rest windows to count against `timeOfDay` progression instead of raw server ticks, so the 30-minute interval now follows in-game time exactly.

## Blood Moon Debug Stop Command

Added a direct escape hatch for live testing.

### Highlights
- Added `/cresora moon stop_blood_moon` to clear the current blood moon special state without wiping other special moon types.
- The command also stops any active `血色战争`, clears pending bed confirmations, unlocks the bed, restores `DO_MOB_GRIEFING`, and refreshes loaded hostile scaling.
- Added localized feedback for the new debug command in `zh_cn`, `en_us`, `ja_jp`, and `lzh`.

## Blood Moon Visibility and Rest Pass

Adjusted live combat readability and pacing for `血色战争`.

### Highlights
- Blood Moon battle mobs now spawn with the glowing effect so wave enemies are easy to track at night.
- Rest windows between waves now use real-time `30s` instead of in-game time, and the action-bar countdown follows the same real-time clock.

## CWC Parser and Hotbar Stability Pass

Hardened the Cresora Weapon Compiler and sub-skill hotbar flow against silent breakage.

### Fixes
- Added `/* ... */` block comment support to the `.cresora` lexer.
- Made the parser reject unknown top-level weapon fields, unknown stats fields, and malformed handler tokens instead of silently skipping them.
- Converted `area_of_effect` to a dedicated AST node instead of rebuilding it from string fragments.
- Replaced item model generation with overwrite-on-build behavior so stale models do not survive a recompile.
- Added offline cleanup for hotbar override sessions and changed bare `SubSkillItem` clicks to fall through normally.

### Verification
- `GRADLE_USER_HOME=.gradle-user ./gradlew classes --console=plain`

## Runtime Fragility Hardening

Hardened non-CWC runtime state so it stops leaking across disconnects and weapon swaps.

### Fixes
- Restricted weapon skill dispatch to the currently held weapon and its registered sub-skills instead of scanning every handler on every tick or hit.
- Added explicit disconnect cleanup for weapon-state, debuff-state, equipment-state, combat-feedback state, hotbar override sessions, and Masquerade respawn snapshots.
- Removed dead `Join.kt` scaffolding and normalized the Masquerade disconnect path so it no longer tries to restore a leaving player.

### Verification
- `GRADLE_USER_HOME=.gradle-user ./gradlew classes --console=plain`

## Specification Gap Hardening

Closed several “defined but not actually supported” seams across equipment, loot, resonance, treasure chests, and the CWC parser.

### Fixes
- Rejected unsupported `effectHooks` during equipment content loading and made runtime dispatch fail loudly instead of logging and continuing.
- Implemented equipment `mobLoot` table injection for artifact drops and upgrade material drops.
- Added disconnect cleanup for treasure chest spawn scheduling while keeping persisted chest entities intact.
- Strengthened resonance banner validation so empty or malformed rarity pools fail at content load time instead of collapsing inside pulls.
- Tightened the CWC parser to reject unknown top-level tokens, unknown `sub_skill` fields, unknown skill/buff fields, and unsupported block actions.
- Declared `tueshokaku` as an adventure-rank upgrade material so upgrade-material loot rules now resolve to a concrete item.

### Verification
- `GRADLE_USER_HOME=.gradle-user ./gradlew classes --console=plain`

## Contributor Guide Refresh (2026-05-16)

- Recreated `AGENTS.md` as a repository-specific contributor guide.
- Documented the actual source layout, Gradle workflow, naming patterns, manual verification flow, and required maintenance files (`WORK_DONE.md`, `TODO.md`, `cresora_document.md`).
