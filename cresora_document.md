# CreSora Utilities API Document

最終更新: 2026-04-06 (デバフシステムの実装・統合反映)

## 1. 結論

このプロジェクトの API は、典型的な「外部公開ライブラリ API」ではありません。
実態は次の 4 本柱です。

1. `CreSoraUtilities` を起点とする初期化・登録 API
2. `ModDataComponents` と mixin access interface 群による保存 API
3. `*ContentRegistry` 群と `data/cresora-utilities/cresora/*.json` によるデータ駆動 API
4. `*Service` / `*Support` / `ArtifactUiFlow` / `Commands` によるゲーム内実行 API

要するに、今の CreSora は「コード中心のフレームワーク」ではなく「サービス singleton + JSON レジストリ」の集合体です。

## 2. エントリポイント

### 2.1 Mod 初期化

ファイル:

- `src/main/kotlin/hifumi/cresora/CreSoraUtilities.kt`

中核:

- `object CreSoraUtilities : ModInitializer`
- `const val MOD_ID = "cresora-utilities"`
- `val version: String`

`onInitialize()` で次を順番に登録します。

- Data Component
- 各種 content registry
- story dialogue networking
- screen handler
- loot function
- event / tick hook
- debuff systems
- 装備・武器・特殊アイテム本体
- loot table 改変

### 2.2 初期化時に呼ばれる主要 registry

- `EquipmentContentRegistry.init()`
- `WeaponContentRegistry.init()`
- `ArtifactSpecialItemRegistry.init()`
- `ShopContentRegistry.init()`
- `MobCombatProfileRegistry.init()`
- `DomainRewardProfileRegistry.init()`
- `DomainContentRegistry.init()`
- `MasqueradeContentRegistry.init()`
- `StoryTextRegistry.init()`
- `StoryContentRegistry.init()`
- `ResonanceContentRegistry.init()`
- `MusicEchoContentRegistry.init()`

### 2.3 登録済み ScreenHandler ID

`CreSoraUtilities.kt` に登録されています。

- `cresora-utilities:upgrade`
- `cresora-utilities:weapon_upgrade`
- `cresora-utilities:weapon_skill_material`
- `cresora-utilities:cresora_menu`
- `cresora-utilities:domain_selection`
- `cresora-utilities:domain_reward`
- `cresora-utilities:story_chapter_selection`
- `cresora-utilities:story_stage_selection`
- `cresora-utilities:masquerade_loadout`
- `cresora-utilities:masquerade_support`
- `cresora-utilities:artifact_shop`
- `cresora-utilities:artifact_alpha`
- `cresora-utilities:artifact_beta`
- `cresora-utilities:resonance`
- `cresora-utilities:resonance_result`

### 2.4 Loot Function

- `cresora-utilities:set_level`

## 3. 保存 API

### 3.1 Data Component

ファイル:

- `src/main/kotlin/hifumi/cresora/ModDataComponents.kt`

登録済み component:

- `LEVEL: ComponentType<Int>`
  - id: `cresora-utilities:level`
  - 旧装備互換用のレベル値
- `EQUIPMENT_DATA: ComponentType<EquipmentData>`
  - id: `cresora-utilities:equipment_data`
  - 現行の装備データ本体
- `WEAPON_DATA: ComponentType<WeaponData>`
  - id: `cresora-utilities:weapon_data`
  - 武器データ本体
- `STORY_LOAN_SESSION_ID: ComponentType<String>`
  - 一時貸与武器の回収識別子
- `MASQUERADE_SESSION_ID: ComponentType<String>`
  - マスカレード持込武器の識別子

### 3.2 Player / Mob access interface

Mixin 実装前提の保存口です。各 `Service` はこれを読む構造です。

- `CreditsAccess`
  - `cresoraGetCredits()`
  - `cresoraSetCredits(value)`
- `AdventureRankAccess`
  - `cresoraGetAdventureRank()`
  - `cresoraSetAdventureRank(rank)`
  - `cresoraGetAdventureRankXp()`
  - `cresoraSetAdventureRankXp(xp)`
- `ResonanceAccess`
  - `cresoraGetChordProgression()`
  - `cresoraSetChordProgression(value)`
  - `cresoraGetSubstituteChord()`
  - `cresoraSetSubstituteChord(value)`
  - `cresoraGetLimitedPityPulls()`
  - `cresoraSetLimitedPityPulls(value)`
  - `cresoraGetStandardPulls()`
  - `cresoraSetStandardPulls(value)`
  - `cresoraGetLimitedFourStarPulls()`
  - `cresoraSetLimitedFourStarPulls(value)`
  - `cresoraGetStandardFourStarPulls()`
  - `cresoraSetStandardFourStarPulls(value)`
  - `cresoraGetDeepPityStreak()`
  - `cresoraSetDeepPityStreak(value)`
  - `cresoraGetArpeggioReady()`
  - `cresoraSetArpeggioReady(value)`
- `StoryProgressAccess`
  - `cresoraGetStoryClearsRaw()`
  - `cresoraSetStoryClearsRaw(value)`
- `MasqueradeProgressAccess`
  - season id / best wave / attempts / total cleared waves / archive raw
- `AdventureRankMobAccess`
  - mob rank
  - elite flag
  - pack id

## 4. 基本モデル API

### 4.1 Stat

ファイル:

- `StatType.kt`
- `StatEntry.kt`
- `CombatDamageType.kt`
- `WeaponRarity.kt`

`StatType`:

- `ATK_FLAT`
- `ATK_PERCENT`
- `HP_FLAT`
- `HP_PERCENT`
- `DEF_FLAT`
- `DEF_PERCENT`
- `CRIT_RATE`
- `CRIT_DMG`
- `ALL_DMG_BONUS`
- `PHYSICAL_RESISTANCE`
- `ARCANE_RESISTANCE`
- `DAMAGE_REDUCTION`

重要仕様:

- `DAMAGE_REDUCTION` は legacy 互換枠
- 現行の実戦表示は `PHYSICAL_RESISTANCE` / `ARCANE_RESISTANCE` 側が本流
- `CombatDamageType` は `PHYSICAL` と `ARCANE` の 2 系統
- `WeaponRarity` は `TWO_STAR` から `FIVE_STAR`

### 4.2 EquipmentData

ファイル:

- `EquipmentData.kt`

構造:

- `rarity: EquipmentRarity`
- `level: Int`
- `mainStat: StatEntry`
- `subStats: List<StatEntry>`
- `upgradeCount: Int`
- `slotTypeId: String`
- `setId: String`

公開メソッド:

- `normalized()`

### 4.3 WeaponData

ファイル:

- `WeaponData.kt`

構造:

- `weaponId: String`
- `rarity: WeaponRarity`
- `baseLevel: Int`
- `skillLevel: Int`

公開メソッド:

- `normalized()`
- `normalized(definition: WeaponDefinition)`

## 5. Content Registry API

ここが最重要です。今の CreSora は多くのゲーム内容を JSON で差し替えられます。

### 5.1 EquipmentContentRegistry

ファイル:

- `src/main/kotlin/hifumi/cresora/EquipmentContentRegistry.kt`
- `src/main/resources/data/cresora-utilities/cresora/equipment_content.json`

トップレベル schema:

- `slots: List<EquipmentSlotType>`
- `sets: List<EquipmentSet>`
- `equipmentDefinitions: List<EquipmentDefinition>`
- `dropProfiles: List<EquipmentDropProfile>`
- `mobLoot: List<EquipmentMobLootDefinition>`

主 API:

- `requireSlot(id)`
- `requireSet(id)`
- `requireEquipment(id)`
- `equipmentDefinitions()`
- `requireDropProfile(id)`
- `mobLootRules()`
- `equipmentCountForSet(setId)`

`EquipmentSlotType` の主フィールド:

- `id`
- `translationKey`
- `trinketGroup`
- `trinketSlot`
- `mainStatCandidates`
- `mainWeights`
- `subWeights`
- `defaultMainStat`

`EquipmentSet` の主フィールド:

- `id`
- `translationKey`
- `bonuses`

`EquipmentSetBonus` の主フィールド:

- `requiredPieces`
- `descriptionKey`
- `stats`
- `effectHooks`

注意:

- `effectHooks` は schema と dispatch まではある
- ただし動的効果本体はまだ本格実装されていない
- `EquipmentEffectHookService` は未実装フックをログに出すだけ

### 5.2 WeaponContentRegistry

ファイル:

- `src/main/kotlin/hifumi/cresora/WeaponContentRegistry.kt`
- `src/main/resources/data/cresora-utilities/cresora/weapon_content.json`

トップレベル schema:

- `weaponDefinitions: List<WeaponDefinition>`

`WeaponDefinition` の主フィールド:

- `id`
- `baseItemId`
- `baseAttackDamage`
- `attackDamagePerLevel`
- `totalAttackSpeed`
- `maxBaseLevel`
- `maxSkillLevel`
- `critRateBonusPercent`
- `maxAllDamageBonusPercent`
- `damageType`
- `attackCurve`
- `skill`
- `upgrades`
- `craft`
- `drops`

主 API:

- `weaponDefinitions()`
- `requireWeapon(id)`
- `definitionByFragmentId(fragmentItemId)`

重要な設計判断:

- 武器の「数値と入手」はかなり JSON 化済み
- ただし `skill.effectId` が新規なら、たいてい `WeaponSkillService` 側のコード追加がまだ必要

### 5.3 ArtifactSpecialItemRegistry

ファイル:

- `ArtifactSpecialItemRegistry.kt`
- `artifact_special_items.json`

対象:

- `残響のカナタ・α式`
- `残響のカナタ・β式`

主 API:

- `definition(id)`
- `definitions()`
- `shopDefinitions()`
- `definitionsWithMobDrop()`
- `definitionForKind(kind)`

### 5.4 ShopContentRegistry

ファイル:

- `ShopContentRegistry.kt`
- `shop_content.json`

対象:

- CSC で買える一般資源
- 特殊アイテムショップ表示

主 API:

- `shopOffers()`

`ShopOfferDefinition` は sealed interface です。

- `id`
- `price`
- `createDisplayStack()`
- `displayName()`
- `grant(player)`

### 5.5 ResonanceContentRegistry

ファイル:

- `ResonanceContentRegistry.kt`
- `resonance_content.json`

対象:

- 限定奏鳴
- 常設奏鳴
- レート
- ピックアップ
- 各レアリティ pool

主 API:

- `banners()`
- `banner(id)`
- `limitedBanners()`

### 5.6 StoryContentRegistry

ファイル:

- `StoryContentRegistry.kt`
- `story_content.json`

対象:

- 章 ID
- グループ
- タイトル参照
- 戦闘前ストーリー
- 戦闘ヒント
- 一時配布武器
- 戦闘 objective
- wave
- 戦闘後ストーリー
- 報酬

主 API:

- `chapters()`
- `chapterGroups()`
- `chaptersForGroup(groupId)`
- `requireChapter(id)`
- `requiredFreeMainSlots(chapter)` など

### 5.7 StoryTextRegistry

ファイル:

- `StoryTextRegistry.kt`
- `story_texts.json`

役割:

- ストーリー本文の locale 別解決
- fallback locale 管理
- 章タイトルとラベル生成

主 API:

- `resolve(locale, textId)`
- `normalizeLocale(locale)`
- `resolvePlayerLocale(player)`
- `chapterTitle(locale, chapter)`
- `chapterLabel(locale, chapter)`

これで本文が `lang/*.json` から分離されています。

### 5.8 DomainContentRegistry

ファイル:

- `DomainContentRegistry.kt`
- `domain_content.json`

対象:

- mob pool
- stage
- wave
- unlock rank
- entry cost
- reward profile link

主 API:

- `domains()`
- `requireDomain(id)`
- `requireMobPool(id)`

### 5.9 DomainRewardProfileRegistry

ファイル:

- `DomainRewardProfileRegistry.kt`
- `domain_reward_profiles.json`

対象:

- 聖遺物報酬
- 武器欠片報酬
- CSC / Rank XP 報酬

主 API:

- `profiles()`
- `requireProfile(id)`

### 5.10 MasqueradeContentRegistry

ファイル:

- `MasqueradeContentRegistry.kt`
- `masquerade_content.json`

対象:

- `seasonId`
- `maxWaveCount`
- `rewardPerClearedWave`
- wave ごとの spawn
- 演出サポート定義

主 API:

- `definition()`
- `wave(waveNumber)`
- `supportBuff(id)`
- `supportBuffs()`

### 5.11 MobCombatProfileRegistry

ファイル:

- `MobCombatProfileRegistry.kt`
- `mob_combat_content.json`

対象:

- mob ごとの攻撃属性
- 物理耐性
- 術耐性

主 API:

- `profile(entityType)`
- `attackType(entityType)`
- `resistancePercent(entityType, damageType)`

### 5.12 MusicEchoContentRegistry

ファイル:

- `MusicEchoContentRegistry.kt`
- `music_echo_content.json`

対象:

- バージョン単位の mob 被ダメ倍率補正

主 API:

- `activeEchoes()`
- `mobDamageTakenMultiplier()`

## 6. Stack / Item API

### 6.1 EquipmentStackSupport

ファイル:

- `EquipmentStackSupport.kt`

役割:

- 装備 item と定義 ID の対応
- `EQUIPMENT_DATA` 読み書き
- legacy `LEVEL` との同期

主 API:

- `registerEquipmentItem(item, definition)`
- `getDefinitionRef(item)`
- `getDefinition(item|stack)`
- `itemForDefinitionId(definitionId)`
- `allEquipmentItems()`
- `isEquipment(stack)`
- `getEquipmentData(stack)`
- `getCompatibilityLevel(stack)`
- `defaultEquipmentData(definition, level)`
- `ensureEquipmentData(stack, random?)`
- `syncEquipmentData(stack, data)`
- `syncEquipmentData(stack, level, random?)`
- `rarityForLevel(level)`

### 6.2 WeaponStackSupport

ファイル:

- `WeaponStackSupport.kt`

役割:

- 武器 item / 欠片 item / rarity 共通欠片の解決
- `WEAPON_DATA` 読み書き
- 新旧欠片互換

主 API:

- `registerWeaponItem(item, definition)`
- `registerFragmentItem(item, definition)`
- `registerRarityFragmentItem(item, rarity)`
- `isWeapon(stack)`
- `isWeaponFragment(stack)`
- `getDefinition(stack)`
- `getFragmentDefinition(stack)`
- `weaponItem(definitionId)`
- `fragmentItem(definitionId)`
- `legacyFragmentItem(definitionId)`
- `getWeaponData(stack)`
- `ensureWeaponData(stack)`
- `syncWeaponData(stack, data)`
- `defaultWeaponData(definition)`
- `createWeaponStack(definition, rarity, baseLevel, skillLevel)`
- `countFragments(player, definition)`
- `removeFragments(player, definition, amount)`

重要仕様:

- 今後の強化素材は rarity 共通欠片が本流
- 旧 weapon 個別欠片は互換のため残存
- 強化時は「同レア共通欠片 + 旧同レア欠片」を受け付ける

### 6.3 ArtifactSpecialItemSupport

ファイル:

- `ArtifactSpecialItemSupport.kt`

主 API:

- `registerItem(item, definitionId)`
- `isSpecialItem(stack)`
- `definition(stack|item)`
- `isKind(stack, kind)`
- `itemForDefinitionId(definitionId)`

## 7. Service API

### 7.1 AdventureRankService

ファイル:

- `AdventureRankService.kt`

責務:

- プレイヤー rank / xp 管理
- mob rank 割当
- mob HP / 防御スケール適用
- mob overhead 表示更新
- rank 依存ダメージ倍率

主 API:

- `getProgress(player)`
- `getRank(player)`
- `addXp(player, amount)`
- `addReward(player, source, count)`
- `setRank(player, rank)`
- `addRawXp(player, amount)`
- `setProgressXp(player, xp)`
- `copyTo(old, new)`
- `hostileKillXp(entity)`
- `getOrAssignMobRank(entity, world)`
- `applyMobScaling(entity, rank, ...)`
- `damageMultiplier(source|attacker)`
- `mobRank(entity)`
- `mobLevel(entity)`
- `refreshMobDisplay(entity)`
- `showMobDamage(...)`
- `showMobTrueDamage(...)`
- `showPlayerDamageFeedback(...)`

重要仕様:

- mob rank 上限は `AdventureRankProgression` 側に依存
- field mob は近傍プレイヤー rank を基準に `±5` の振れ幅で割当
- HP は内部で cap と overflow 防御変換を使う
- **エリートモブの視覚化**: `EntityAttributes.GENERIC_SCALE` を利用し、エリートモブのモデルサイズとヒットボックスを **18% 拡大** しています。

### 7.2 CreditsService

ファイル:

- `CreditsService.kt`

責務:

- CSC の取得・加算・消費・コピー

主 API:

- `getCredits(player)`
- `hasCredits(player, amount)`
- `addCredits(player, amount)`
- `spendCredits(player, amount)`
- `setCredits(player, amount)`
- `copyTo(old, new)`
- `addPickupReward(player, source, count)`
- `addHostileKillReward(player, entity)`
- `addFriendlyKillReward(player, entity)`
- `addAdvancementReward(player, advancement)`
- `addExperienceReward(player, amount)`

### 7.3 ResonanceService

ファイル:

- `ResonanceService.kt`

責務:

- コード進行 / 代理コードの所持管理
- 限定 / 常設のガチャ実行
- pity / deep pity / arpeggio 管理

主 API:

- `getProgress(player)`
- `getCurrency(player, type)`
- `addCurrency(player, type, amount)`
- `setCurrency(player, type, amount)`
- `spendCurrency(player, type, amount)`
- `copyTo(old, new)`
- `currencyCount(player, banner)`
- `canPull(player, banner)`
- `pull(player, banner)`

### 7.4 EquipmentGenerationService

ファイル:

- `EquipmentGenerationService.kt`

責務:

- 聖遺物生成
- rarity / main stat / sub stat 抽選
- サブステ伸び値抽選

主 API:

- `createEquipment(...)`
- `createPendant(...)`
- `rollNewSubStat(data, random, dropProfile?)`
- `rollSubStatIncrease(type, rarity, random)`
- `rollMainStatIncrease(type, rarity, random)`

### 7.5 EquipmentUpgradeService

ファイル:

- `EquipmentUpgradeService.kt`

責務:

- 聖遺物レベル上昇
- `+4` 成長イベント
- β式リロール

主 API:

- `applyLevels(data, levelGain, random)`
- `applyGrowthEvent(data, random)`
- `rerollToMaxWithPrioritySubStat(definition, rarity, priorityCandidates, random)`

### 7.6 WeaponUpgradeService

ファイル:

- `WeaponUpgradeService.kt`

責務:

- 武器強化コスト計算

主 API:

- `baseUpgradeCost(definition, currentLevel)`
- `skillUpgradeCost(definition, currentLevel)`
- `skillArtifactCost(definition, currentLevel)`

### 7.7 WeaponCombatSupport

ファイル:

- `WeaponCombatSupport.kt`

責務:

- 武器攻撃力 / 速度 / 会心率 bonus / 全ダメ bonus / スキル値算出

主 API:

- `attackDamage(definition, data)`
- `attackDamageModifier(definition, data)`
- `attackSpeedModifier(definition)`
- `critRateBonusPercent(definition)`
- `allDamageBonusPercent(definition, data)`
- `skillValueHearts(definition, data)`
- `secondarySkillValueHearts(definition, data)`
- `skillValuePercent(definition, data)`
- `secondarySkillValuePercent(definition, data)`
- `shieldHp(definition, data)`
- `healHp(definition, data)`
- `currentHpTrueDamageRatio(definition, data)`

### 7.8 EquipmentPlayerSupport

ファイル:

- `EquipmentPlayerSupport.kt`

責務:

- Trinkets 装備から `EquipmentData` を集約
- セット効果解決
- プレイヤー総ステータス算出

主 API:

- `getEquippedEquipmentData(player)`
- `getActiveSetBonuses(player)`
- `getActiveSetSummaries(player)`
- `getAggregatedStats(player)`

### 7.9 EquipmentAttributeService / WeaponAttributeService

ファイル:

- `EquipmentAttributeService.kt`
- `WeaponAttributeService.kt`

責務:

- 毎 tick でプレイヤー属性へ modifier を反映

補足:

- Equipment 側は Attack / MaxHealth / Armor
- Weapon 側は held weapon に応じた Attack / Speed / 一部 skill スカラー

### 7.10 WeaponSkillService & WeaponSkillRegistry

ファイル:

- `WeaponSkillService.kt`
- `skill/WeaponSkillHandler.kt`
- `skill/WeaponSkillRegistry.kt`
- `skill/*.kt` (各スキル実装)

設計思想:

以前は `WeaponSkillService` に全てのロジックが集中していましたが、現在は **Strategy パターン** に基づき、各スキルが独立したクラスに分割されています。

`WeaponSkillService` の責務:

- スキルの共通クールダウン管理 (BossBar 表示)
  - **重要**: クールダウンは「残りティック数」で管理されるようになり、デバフ等による動的な速度変更に対応しています。
- 汎用シールド (Shield) および一時ガード (Temporary Guard) のライフサイクル管理
- `WeaponSkillRegistry` を介した各ハンドラへのイベント（Tick, Damage 等）のディスパッチ

`WeaponSkillHandler` インターフェース:

各スキルクラスが実装する共通フックです。

- `activate(...)`: 右クリック発動時の主処理
- `onTick(server)` / `onPlayerTick(player)`: 継続効果の更新
- `onDamageAbsorbed(...)`: 被ダメージ吸収時の特殊処理
- `onDamageDealt(...)`: 攻撃命中時の追加効果（Dark/Lux 付与、追撃等）
- `onDamageTaken(...)`: 被弾時の反撃・軽減処理

`WeaponSkillRegistry`:

- `effectId` (JSONの `skill.effectId` に対応) とハンドラを紐付けます。
- 起動時に `init()` で全ての標準スキルを登録します。

### 7.11 DomainService

ファイル:

- `DomainService.kt`

責務:

- 秘境 session 管理
- arena 生成
- wave 進行
- 報酬生成
- story linked stage 連携

主 API:

- `hasActiveSession(player)`
- `generateOneOffRewards(player, domainId, sessionRank)`
- `startSession(player, domainId)`
- `startLinkedStoryStage(player, chapter)`
- `tick(server)`
- `onPlayerDeath(player)`
- `onPlayerDisconnect(player)`
- `damageMultiplier(attacker)`
- `isDomainMob(entity)`

### 7.12 StoryService

ファイル:

- `StoryService.kt`

責務:

- 楽章演奏 session 管理
- プレ / ポストストーリー進行
- 3,2,1 カウントダウン
- survival objective
- 一時貸与武器の配布 / 回収

主 API:

- `hasActiveSession(player)`
- `damageTakenMultiplier(hostile)`
- `allowsTrueDamage(hostile)`
- `startSession(player, chapterId)`
- `tick(server)`
- `onPlayerDeath(player)`
- `onPlayerDisconnect(player)`
- `handleDialogueAction(player, actionId)`

### 7.13 StoryProgressService

ファイル:

- `StoryProgressService.kt`

責務:

- 章クリア履歴
- 前提章ロック

主 API:

- `clearedChapterIds(player)`
- `isCleared(player, chapterId)`
- `markCleared(player, chapterId)`
- `copyTo(old, new)`
- `missingPrerequisite(player, chapter)`

### 7.14 MasqueradeService

ファイル:

- `MasqueradeService.kt`

責務:

- マスカレード run session
- 持込武器制限
- inventory snapshot / restore
- wave 管理
- support buff 選択
- シーズン進捗反映

主 API:

- `hasActiveSession(player)`
- `clampPlayerDamageReduction(player, rawReductionRatio)`
- `currentWave(player)`
- `clearedWaveCount(player)`
- `supportCandidateIds(player)`
- `getAggregatedSupportStats(player)`
- `startSession(player, selectedInventorySlots)`
- `selectSupportBuff(player, buffId)`
- `tick(server)`
- `onPlayerDeath(player)`
- `onPlayerDisconnect(player)`
- `restoreAfterRespawn(newPlayer)`
- `damageMultiplier(attacker)`
- `damageTakenMultiplier(target)`
- `adjustIncomingDamage(player, source, amount)`

### 7.15 TreasureChestService

ファイル:

- `TreasureChestService.kt`

責務:

- 周辺ランダム宝箱生成
- 開封時の CSC / Chord Progression 付与
- 永続状態の同期

主 API:

- `init()`

注記:

- 外向け関数は少ない
- 実質は server tick と `UseBlockCallback` 駆動

### 7.16 NaturalRegenService

ファイル:

- `NaturalRegenService.kt`

責務:

- rank 帯と combat 状態に基づく自然回復
- `CresoraDebuffService.canHeal` による回復阻害の適用

主 API:

- `init()`
- `markCombat(player)`

### 7.17 CresoraDebuffService

ファイル:

- `CresoraDebuff.kt`
- `CresoraDebuffRegistry.kt`
- `CresoraDebuffService.kt`

責務:

- プレイヤーに対するデバフ（負の状態異常）の管理
- エリートモブ攻撃時のデバフ付与判定（現在35%）
- 攻撃力・被ダメージ・CT速度・回復可否への倍率適用

実装済みデバフ:

- **神経損傷 (NERVE_DAMAGE)**: 与ダメージ減少(0.85x)、被ダメージ増加(1.12x)
- **移動不能 (ROOT)**: 移動速度を100%減少（その場から動けなくなる）
- **煙幕 (SMOKE)**: 盲目 (Blindness) を付与
- **燃焼 (BURN)**: 継続的な火炎ダメージ
- **CT延長 (COOLDOWN_PENALTY)**: 武器スキルのクールダウン解消速度が50%に低下
- **回復阻害 (HEAL_BLOCK)**: 自然回復を完全に停止

### 7.18 CombatStatSupport

ファイル:

- `CombatStatSupport.kt`

責務:

- 表示用会心値と耐性値の正規化

定数:

- `BASE_CRIT_RATE_PERCENT = 5.0`
- `BASE_CRIT_DAMAGE_PERCENT = 50.0`

主 API:

- `effectiveDisplayValue(type, totals)`
- `effectiveCritRateRatio(totals)`
- `effectiveCritDamageRatio(totals)`

## 8. UI / Command API

### 8.1 ArtifactUiFlow

ファイル:

- `ArtifactUiFlow.kt`

画面遷移の単一窓口です。

主 API:

- `openMenu(player)`
- `openStoryChapterSelection(player)`
- `openStoryStageSelection(player, chapterGroup)`
- `openRewardSummary(player, title, displayStacks)`
- `openWeaponUpgrade(player)`
- `openDomainSelection(player)`
- `openMasqueradeLoadout(player)`
- `openMasqueradeSupport(player)`
- `openDomainReward(player, displayStacks)`
- `openShop(player)`
- `openResonance(player)`
- `openResonanceResult(player, result)`
- `openUpgradeScreen(player, baseStack, materialStack)`
- `openAlphaSelection(player, baseStack, materialStack)`
- `openBetaSelection(player, baseStack, materialStack)`
- `openWeaponSkillMaterialSelection(player, weaponStack)`

### 8.2 Commands

ファイル:

- `Commands.kt`

登録コマンド:

- `/about`
- `/cresora`
- `/cresora_stats`
- `/cresora_rank`
  - `set`
  - `addxp`
  - `setxp`
- `/cresora_credits`
  - `get`
  - `add`
  - `set`
- `/cresora_shop`
- `/cresora_domain`
- `/cresora_masquerade`
- `/cresora_story`
  - `list`
  - `start <chapter_id>`
- `/cresora_resonance`
  - `currency`
  - `get`
  - `add`
  - `set`

`/cresora_stats` の現在表示対象:

- Adventure Rank
- CSC
- セット効果概要
- `ATK / HP`
- `DEF / Crit`
- `All DMG / Physical RES / Arcane RES`
- 実攻撃力 / 最大 HP / 防御力

## 9. 実装の勘所

### 9.1 すでに JSON 化されている領域

- 装備種別
- 装備セット
- 装備ドロップ
- 武器数値
- 武器ドロップ
- 秘境
- 秘境報酬
- マスカレード wave
- マスカレード演出サポート
- ストーリー章定義
- ストーリー本文
- ショップ商品
- ガチャ banner
- 音律エコー
- mob combat profile

### 9.2 まだコード実装が必要な領域

- **新しい `WeaponSkillDefinition.effectId`**: `WeaponSkillHandler` を実装した新しいクラスを作成し、`WeaponSkillRegistry` に登録する必要があります。
- 新しい動的 set effect hook の本体: `EquipmentEffectHookService` へのロジック追加が必要です。
- 特殊な GUI 挙動: 新しい Screen / ScreenHandler の作成が必要です。

### 9.3 追加時の基本手順

新しい装備セット:

1. `equipment_content.json` に slot / set / equipmentDefinitions を追加
2. 必要なら dropProfile / mobLoot も追加
3. モデル・テクスチャ・翻訳を追加

新しい武器:

1. `weapon_content.json` に `WeaponDefinition` を追加
2. モデル・アイテム定義・翻訳を追加
3. `skill.effectId` が新規の場合、`WeaponSkillHandler` を実装する新しいクラスを作成し、`WeaponSkillRegistry` に登録します。
4. 必要なら `ResonanceContentRegistry` や秘境報酬にも接続

新しいストーリー:

1. `story_content.json` に章定義追加
2. `story_texts.json` に全 locale 文言追加
3. 戦闘ギミックが既存 objective で足りなければ `StoryService` 拡張

## 10. 現状の注意点

- set effect hook は schema はあるが、動的処理は未完成
- 新武器追加は「完全ノーコード」ではない
- `WeaponSkillService` は依然として巨大で、固有効果の集中点になっている
- API と呼べる最も安定した境界は `*ContentRegistry` と `*Support` 群
- 逆に `*Service` の private runtime は変動しやすい

## 11. 参照優先順

コードを追うときは次の順が最短です。

1. `CreSoraUtilities.kt`
2. `ModDataComponents.kt`
3. `Commands.kt`
4. `ArtifactUiFlow.kt`
5. `*ContentRegistry.kt`
6. `EquipmentStackSupport.kt` / `WeaponStackSupport.kt`
7. `AdventureRankService.kt` / `CreditsService.kt` / `ResonanceService.kt` / `CresoraDebuffService.kt`
8. `StoryService.kt` / `DomainService.kt` / `MasqueradeService.kt`
9. `WeaponSkillService.kt`

これで大半の構造が見えます。
