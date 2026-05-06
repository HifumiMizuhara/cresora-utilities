# WORK_DONE

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
- Reworked `tanboku_chokuu.cresora` so Zanso and Bokuchu Munen only apply state, show activation messages, and close the skill menu after their Tao checks succeed.
- Fixed the Tao gain message to show the actual current Tao value instead of a literal Kotlin expression string.

### Verification
- `GRADLE_USER_HOME=.gradle-user ./gradlew generateWeapons --console=plain`
- `GRADLE_USER_HOME=.gradle-user ./gradlew classes --console=plain`

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
