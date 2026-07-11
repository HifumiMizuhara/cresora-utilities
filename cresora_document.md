# CreSora Utilities 内部 API ドキュメント

最終更新: 2026-07-12

Fabric 1.21.7 用 Minecraft Mod **CreSora Utilities** の内部 API、レジストリスキーマ、コアサービスの現状仕様書です。  
完了した作業の履歴は Git (`git log`) を参照してください。

---

## 1. 概要

外部公開ライブラリ API ではなく、次の **5 本柱** で構成されたハイブリッドフレームワークです。

| 柱 | 役割 |
|---|---|
| 初期化・登録 | `CreSoraUtilities` 起点の registry / screen / hook 登録 |
| 永続化 | `ModDataComponents` と mixin access interface |
| データ駆動 | `*ContentRegistry` + `data/cresora-utilities/cresora/*.json` |
| 実行時 | `*Service` / `*Support` / `ArtifactUiFlow` / `Commands` |
| コード生成 | CWC / CAC / CMC（`.cresora` / `.artifact` / `.movement` → Kotlin + JSON） |

---

## 2. 目次

1. [概要](#1-概要)
2. [目次](#2-目次)
3. [エントリポイント](#3-エントリポイント)
4. [永続化](#4-永続化)
5. [基本モデル](#5-基本モデル)
6. [Content Registry](#6-content-registry)
7. [Stack Support](#7-stack-support)
8. [サービス API](#8-サービス-api)
   - [8.1 進行・経済](#81-進行経済)
   - [8.2 戦闘基盤](#82-戦闘基盤)
   - [8.3 月相・血月](#83-月相血月)
   - [8.4 装備・聖遺物](#84-装備聖遺物)
   - [8.5 武器・スキル・霊絆](#85-武器スキル霊絆)
   - [8.6 コンテンツセッション](#86-コンテンツセッション)
   - [8.7 共鳴ガチャ](#87-共鳴ガチャ)
   - [8.8 Phase 4 ワールド / NPC](#88-phase-4-ワールド--npc)
9. [UI / Command](#9-ui--command)
10. [コンパイラ](#10-コンパイラ)
11. [開発ガイド](#11-開発ガイド)

---

## 3. エントリポイント

### 3.1 Mod 初期化

- ファイル: `src/main/kotlin/hifumi/cresora/CreSoraUtilities.kt`
- `object CreSoraUtilities : ModInitializer`
- `MOD_ID = "cresora-utilities"` / `version: String`

`onInitialize()` の登録順:

1. Data Component
2. 各種 content registry
3. story dialogue networking
4. screen handler
5. loot function
6. event / tick hook
7. debuff systems
8. 装備・武器・特殊アイテム本体
9. loot table 改変

### 3.2 初期化時の主要 registry

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
- `CompiledWeaponSkillRegistry.registerAll(this)`（CWC 生成）
- `HotbarOverrideService` — サブスキル用ホットバーのセッション管理と復帰

### 3.3 ScreenHandler ID

`CreSoraUtilities.kt` に登録:

| ID |
|---|
| `cresora-utilities:upgrade` |
| `cresora-utilities:weapon_upgrade` |
| `cresora-utilities:weapon_skill_material` |
| `cresora-utilities:cresora_menu` |
| `cresora-utilities:domain_selection` |
| `cresora-utilities:domain_reward` |
| `cresora-utilities:story_chapter_selection` |
| `cresora-utilities:story_stage_selection` |
| `cresora-utilities:masquerade_loadout` |
| `cresora-utilities:masquerade_support` |
| `cresora-utilities:artifact_shop` |
| `cresora-utilities:artifact_alpha` |
| `cresora-utilities:artifact_beta` |
| `cresora-utilities:resonance` |
| `cresora-utilities:resonance_result` |
| `cresora-utilities:resonance_featured_selection` |

### 3.4 Loot Function

- `cresora-utilities:set_level`

---

## 4. 永続化

### 4.1 Data Component

- ファイル: `src/main/kotlin/hifumi/cresora/ModDataComponents.kt`

| Component | ID / 用途 |
|---|---|
| `LEVEL: ComponentType<Int>` | `cresora-utilities:level` — 旧装備互換レベル |
| `EQUIPMENT_DATA` | `cresora-utilities:equipment_data` — 現行装備データ |
| `WEAPON_DATA` | `cresora-utilities:weapon_data` — 武器データ |
| `STORY_LOAN_SESSION_ID` | 一時貸与武器の回収識別子 |
| `MASQUERADE_SESSION_ID` | マスカレード持込武器の識別子 |
| `WEAPON_ARTIFACTS` | `cresora-utilities:weapon_artifacts` — 武器装着聖遺物 4 枠 |

### 4.2 Mixin Access Interface

各 `Service` が読む保存口。実装は Java mixin。

**CreditsAccess**

- `cresoraGetCredits()` / `cresoraSetCredits(value)`

**AdventureRankAccess**

- `cresoraGetAdventureRank()` / `cresoraSetAdventureRank(rank)`
- `cresoraGetAdventureRankXp()` / `cresoraSetAdventureRankXp(xp)`

**ResonanceAccess**

- `cresoraGetChordProgression()` / `cresoraSetChordProgression(value)`
- `cresoraGetSubstituteChord()` / `cresoraSetSubstituteChord(value)`
- `cresoraGetLimitedPityPulls()` / `cresoraSetLimitedPityPulls(value)`
- `cresoraGetStandardPulls()` / `cresoraSetStandardPulls(value)`
- `cresoraGetLimitedFourStarPulls()` / `cresoraSetLimitedFourStarPulls(value)`
- `cresoraGetStandardFourStarPulls()` / `cresoraSetStandardFourStarPulls(value)`
- `cresoraGetDeepPityStreak()` / `cresoraSetDeepPityStreak(value)`
- `cresoraGetArpeggioReady()` / `cresoraSetArpeggioReady(value)`

**StoryProgressAccess**

- `cresoraGetStoryClearsRaw()` / `cresoraSetStoryClearsRaw(value)`

**MasqueradeProgressAccess**

- season id / best wave / attempts / total cleared waves / archive raw

**AdventureRankMobAccess**

- mob rank / elite flag / boss flag / pack id

---

## 5. 基本モデル

### 5.1 Stat

- ファイル: `StatType.kt` / `StatEntry.kt` / `CombatDamageType.kt` / `WeaponRarity.kt` / `WeaponRole.kt`

`StatType`: `ATK_FLAT`, `ATK_PERCENT`, `HP_FLAT`, `HP_PERCENT`, `DEF_FLAT`, `DEF_PERCENT`, `CRIT_RATE`, `CRIT_DMG`, `ALL_DMG_BONUS`, `PHYSICAL_RESISTANCE`, `ARCANE_RESISTANCE`, `DAMAGE_REDUCTION`

仕様:

- `DAMAGE_REDUCTION` は legacy 互換。実戦表示の本流は `PHYSICAL_RESISTANCE` / `ARCANE_RESISTANCE`
- `CombatDamageType`: `PHYSICAL` / `ARCANE`
- `WeaponRarity`: `TWO_STAR` … `FIVE_STAR`
- `WeaponRole`（9種）: `VANGUARD`, `GUARD`, `DEFENDER`, `SNIPER`, `CASTER`, `MEDIC`, `SUPPORTER`, `SPECIALIST`, `CATALYST`

### 5.2 EquipmentData

- ファイル: `EquipmentData.kt`
- フィールド: `rarity`, `level`, `mainStat`, `subStats`, `upgradeCount`, `slotTypeId`, `setId`, `balanceVersion`
  - `balanceVersion` は `combat_balance.json` の `version`。旧値は読込時に一度だけ新ロール尺度へ移行
- 公開メソッド: `normalized()`

### 5.3 WeaponData

- ファイル: `WeaponData.kt`
- フィールド: `weaponId`, `rarity`, `baseLevel`, `skillLevel`, `breakthrough`（精錬 0..2）, `spiritBondStage`（霊絆 1..6、`normalized()` でクランプ。レガシーは codec デフォルト `1`）
- 公開メソッド: `normalized()` / `normalized(definition)`

---

## 6. Content Registry

ゲーム内容の多くは JSON で差し替え可能。ここが最重要の境界です。

### 6.1 CombatBalanceProfileRegistry

- ファイル: `combat/CombatBalanceProfile.kt` / `data/.../cresora/combat_balance.json`

起動時に共有バランスを検証・読込。ランク別敵成長、フィールド精鋭／ボス、ドメイン、血月、マスカレード、自然回復、聖遺物ロール、武器突破の **数値上の唯一の調整口**。

- `caps` — 耐性・全ダメ・会心ダメ上限
- `survivor`〜`relic` — 敵ファミリー別曲線
- `domain` / `field` / `regen` — コンテンツ固有補正と自然回復率
- `tuning` — 血月・マスカレード・聖遺物ロール・武器成長／突破の共通倍率

`combatBalanceReport` Gradle task はランク `1 / 20 / 40 / 55 / 70` と未完成／標準／最大育成を評価し、DPS・TTK・イベント1ウェーブ時間・被弾率を出力。回帰基準: 標準育成は通常敵 3〜5 秒・精鋭 12〜18 秒、最大育成は通常敵 2 秒以内・精鋭 6〜10 秒・イベント波 30〜45 秒。

### 6.2 EquipmentContentRegistry

- ファイル: `EquipmentContentRegistry.kt` / `equipment_content.json`
- トップレベル: `slots`, `sets`, `equipmentDefinitions`, `dropProfiles`, `mobLoot`
- API: `requireSlot` / `requireSet` / `requireEquipment` / `equipmentDefinitions` / `requireDropProfile` / `mobLootRules` / `equipmentCountForSet`

主な型フィールド:

- `EquipmentSlotType`: `id`, `translationKey`, `trinketGroup`, `trinketSlot`, `mainStatCandidates`, `mainWeights`, `subWeights`, `defaultMainStat`
- `EquipmentSet`: `id`, `translationKey`, `bonuses`
- `EquipmentSetBonus`: `requiredPieces`, `descriptionKey`, `stats`, `effectHooks`

仕様:

- `effectHooks` は schema 上存在するが、未対応 hook は content load で reject。正式仕様は「`stats` 可、`effectHooks` まだ不可」
- `mobLoot` は組み込み loot table へ注入（artifact drop + upgrade material）

### 6.3 WeaponContentRegistry

- ファイル: `WeaponContentRegistry.kt` / `weapon_content.json`
- トップレベル: `weaponDefinitions`
- API: `weaponDefinitions` / `requireWeapon` / `definitionByFragmentId`

`WeaponDefinition` 主フィールド: `id`, `baseItemId`, `baseAttackDamage`, `attackDamagePerLevel`, `totalAttackSpeed`, `maxBaseLevel`, `maxSkillLevel`, `critRateBonusPercent`, `maxAllDamageBonusPercent`, `damageType`, `role`, `attackCurve`, `customModelData?`, `skill`, `upgrades`, `craft`, `drops`, `spirit?`

`WeaponSpiritDefinition`:

- `nameKey`（必須）
- `voiceLines: Map<String, String>`（任意）
- `bondStages: List<{ stage, titleKey, storyKey }>`（stage 1..6、ユニーク）
- `awakeningConditionKey?`

仕様:

- 数値と入手は JSON 化済み。新規 `skill.effectId` は通常 CWC で定義
- `spirit` は `.cresora` の `spirit { ... }` から生成。`applyBundle` で `bondStages` の重複・範囲・必須キー空白を弾く

### 6.4 ArtifactSpecialItemRegistry

- ファイル: `ArtifactSpecialItemRegistry.kt` / `artifact_special_items.json`
- 対象: 残響のカナタ・α式 / β式
- API: `definition` / `definitions` / `shopDefinitions` / `definitionsWithMobDrop` / `definitionForKind`

### 6.5 ShopContentRegistry

- ファイル: `ShopContentRegistry.kt` / `shop_content.json`
- 対象: CSC 一般資源、特殊アイテムショップ表示
- API: `shopOffers()`
- `ShopOfferDefinition`（sealed）: `id`, `price`, `createDisplayStack()`, `displayName()`, `grant(player)`

### 6.6 ResonanceContentRegistry

- ファイル: `ResonanceContentRegistry.kt` / `resonance_content.json`
- 対象: 限定奏鳴（SELECT・プレイヤー個別 ★5 PU）、常設奏鳴、レート、レアリティ pool
- API: `banners` / `banner` / `limitedBanners`

仕様:

- 各 rarity pool は load 時検証。空 pool（LIMITED の `fiveStarPool` 除く）、負 weight、rarity 不一致は reject
- LIMITED は `featuredFiveStarWeaponId` / `fiveStarPool` を持たない。PU は `cresoraGetSelectedFeaturedWeaponId()`。限定のすり抜けプールは実行時に全 ★5 から動的構成

### 6.7 StoryContentRegistry

- ファイル: `StoryContentRegistry.kt` / `story_content.json`
- 対象: 章 ID、グループ、タイトル参照、戦闘前／後ストーリー、ヒント、一時配布武器、objective、wave、報酬
- API: `chapters` / `chapterGroups` / `chaptersForGroup` / `requireChapter` / `requiredFreeMainSlots` など

現行追加章:

- `0-3`（Rehearsal of Resonance 相当）— `0-2` クリア後解放
- `StoryService` の専用進行で 6 種の旋律共鳴を順に教える
- 各段階で必要な 2 本だけ一時配布し、反応確認後に次へ
- `domain_reward_ids` で `resonance_practice` に接続

### 6.8 StoryTextRegistry

- ファイル: `StoryTextRegistry.kt` / `story_texts.json`
- 役割: ストーリー本文の locale 別解決、fallback、章タイトル／ラベル
- API: `resolve` / `normalizeLocale` / `resolvePlayerLocale` / `chapterTitle` / `chapterLabel`
- 本文は `lang/*.json` から分離

### 6.9 DomainContentRegistry

- ファイル: `DomainContentRegistry.kt` / `domain_content.json`
- 対象: mob pool、stage、wave（`count` / `levelOffset` / `elite` / `spawnDelayTicks` / `boss`）、unlock rank、entry cost、reward profile link
- API: `domains` / `requireDomain` / `requireMobPool`

現行追加ドメイン:

- `resonance_practice` — `resonance_trial` mob pool の低コスト練習用。CSC 消費なしで共鳴素材導線を見せる

### 6.10 DomainRewardProfileRegistry

- ファイル: `DomainRewardProfileRegistry.kt` / `domain_reward_profiles.json`
- 対象: 聖遺物、武器欠片、突破素材（`proofReward` / `insightReward` → `DomainMaterialRewardDefinition`）、`resonantLocatorChance`（0.0〜1.0）、CSC / Rank XP
- `DomainMaterialRewardDefinition`: `role?`（省略時はドロップ時にランダム）、`minCount`, `maxCount`
- API: `profiles` / `requireProfile`

現行追加プロファイル `resonance_practice`: 低額 CSC / Rank XP と `chordProgression` / `substituteChord`。artifact / proof 報酬なし

### 6.11 MasqueradeContentRegistry

- ファイル: `MasqueradeContentRegistry.kt` / `masquerade_content.json`
- 対象: `seasonId`, `maxWaveCount`, `rewardPerClearedWave`, wave spawn（`elite` / `boss` / `modifiers`）、演出サポート
- API: `definition` / `wave` / `supportBuff` / `supportBuffs`

### 6.12 MobCombatProfileRegistry

- ファイル: `MobCombatProfileRegistry.kt` / `mob_combat_content.json`
- 対象: mob ごとの攻撃属性、物理／術耐性
- API: `profile` / `attackType` / `resistancePercent`

### 6.13 MusicEchoContentRegistry

- ファイル: `MusicEchoContentRegistry.kt` / `music_echo_content.json`
- 対象: バージョン単位の mob 被ダメ倍率
- API: `activeEchoes` / `mobDamageTakenMultiplier`

---

## 7. Stack Support

### 7.1 EquipmentStackSupport

- ファイル: `EquipmentStackSupport.kt`
- 役割: 装備 item ↔ 定義 ID、`EQUIPMENT_DATA` 読み書き、legacy `LEVEL` 同期
- API: `registerEquipmentItem`, `getDefinitionRef`, `getDefinition`, `itemForDefinitionId`, `allEquipmentItems`, `isEquipment`, `getEquipmentData`, `getCompatibilityLevel`, `defaultEquipmentData`, `ensureEquipmentData`, `syncEquipmentData`, `rarityForLevel`

### 7.2 WeaponStackSupport

- ファイル: `WeaponStackSupport.kt`
- 役割: 武器 / 欠片 / rarity 共通欠片の解決、`WEAPON_DATA` 読み書き、新旧欠片互換
- API: `registerWeaponItem`, `registerFragmentItem`, `registerRarityFragmentItem`, `isWeapon`, `isWeaponFragment`, `getDefinition`, `getFragmentDefinition`, `weaponItem`, `fragmentItem`, `legacyFragmentItem`, `getWeaponData`, `ensureWeaponData`, `syncWeaponData`（`customModelData` 定義時は `CUSTOM_MODEL_DATA` も適用）, `defaultWeaponData`, `createWeaponStack`, `countFragments`, `removeFragments`, `getEquippedArtifacts`, `setEquippedArtifacts`, `hasAnyEquippedArtifact`

仕様:

- 強化素材の本流は rarity 共通欠片。旧個別欠片は互換残存
- 強化時は「同レア共通欠片 + 旧同レア欠片」を受付

### 7.3 ArtifactSpecialItemSupport

- ファイル: `ArtifactSpecialItemSupport.kt`
- API: `registerItem`, `isSpecialItem`, `definition`, `isKind`, `itemForDefinitionId`

---

## 8. サービス API

各節のテンプレ: **ファイル** / **責務** / **API** / **仕様**。

### 8.1 進行・経済

#### AdventureRankService

- ファイル: `AdventureRankService.kt`
- 責務: プレイヤー rank / xp、mob rank 割当、HP／防御スケール、頭上表示、rank 依存ダメージ倍率
- API: `getProgress`, `getRank`, `addXp`, `addReward`, `setRank`, `addRawXp`, `setProgressXp`, `copyTo`, `hostileKillXp`, `getOrAssignMobRank`, `applyMobScaling`, `damageMultiplier`, `mobRank`, `mobLevel`, `refreshMobDisplay`, `showMobDamage`, `showMobTrueDamage`, `showPlayerDamageFeedback`

仕様:

- mob rank 上限は `AdventureRankProgression` 依存
- field mob は近傍プレイヤー rank 基準で `-1〜+1`（精鋭 `-1〜+3`、ボス `+2〜+6`）
- HP は内部で cap と overflow 浮動変換
- モデル拡大は `EntityAttributes.GENERIC_SCALE`。精鋭 +18%、フィールドボス +35%。量は `FieldMobPackService.scaleBonus` → `applyMobScaling` で一括適用

#### CreditsService

- ファイル: `CreditsService.kt`
- 責務: CSC の取得・加算・消費・コピー
- API: `getCredits`, `hasCredits`, `addCredits`, `spendCredits`, `setCredits`, `copyTo`, `addPickupReward`, `addHostileKillReward`, `addFriendlyKillReward`, `addAdvancementReward`, `addExperienceReward`

#### GuideService

- ファイル: `GuideContentRegistry.kt`, `GuideRecords.kt`, `GuideProgressAccess.kt`, `GuideService.kt`, `GuideScreenHandler.kt`, `GuideScreen.kt`, `RecordsScreenHandler.kt` / `RecordsDisplayStackFactory.kt` / `RecordsScreen.kt`
- 責務:
  - 冒険之証（新手ガイド）の章別タスク。完了で CSC および和弦報酬
  - **記録中枢**（発見・羈絆・達成）: `StoryFlagService` / `ResonanceService` / `StoryProgressService` からリアルタイム集計。**新永続フィールドは増やさない**。唯一の新規書込は初回霊武器入手時の `spirit_met_<weaponId>` フラグ
  - 進捗の NBT 保存・重生コピー（`GuideProgressAccess`）
  - イベント連動（撃破、ガチャ、武器強化、章クリア等）
  - UI 表示と報酬受取。記録は `/cresora guide` 系でもテキスト表示

タスク API:

- `getPlayerChapter`, `getTaskProgress`, `claimTaskReward`, `claimChapterReward`, `onKillHostile`, `onResonancePull`, `copyTo`

記録中枢 API（`GuideRegionRecord` / `GuideSpiritBondRecord` / `GuideAchievementSummary`）:

- 発見: `getRegionRecords` — `RegionHooks.hasVisited`（`VISITED_FLAG_PREFIX` / `visitedFlag` / `hasVisited`）
- 羈絆: `getSpiritBondRecords` / `hasMetSpirit` / `onSpiritObtained` / `spiritMetFlag` — `spirit` 付き武器を列挙。段階・点数は `ResonanceService`。`onSpiritObtained` は `pull` 後に初回邂逅を記録
- 達成: `getAchievementSummary` — ガイド章・受取済タスク・章クリア・地域発見・霊邂逅・満羈絆

コマンド（プレイヤー文脈必須）:

| コマンド | 内容 |
|---|---|
| `/cresora guide` | 概要（章・発見進捗） |
| `/cresora guide discovery` | 地域一覧（未発見は `???` + 解放ランク） |
| `/cresora guide bond` | 霊羈絆一覧（未邂逅は `???`） |
| `/cresora guide achievements` | 達成まとめ |
| `/cresora_records` | 記録中枢 GUI（冒険之証「記録」タブからも） |

記録中枢 GUI:

- `ArtifactBookScreenBase` は 6 タブ（見聞／演奏／討伐／祈願／珍品／**記録**）。第 6 タブ → `cresora_records`。`RecordsScreen.getActiveTab()` = 6
- `RecordsScreenHandler` が権威: `activeSubTab`（0=発見 / 1=羈絆 / 2=達成）、`page`, `totalPages`, 左ページ比率、`selectedEntry`（`NO_SELECTION=-1`）。表示 18 スロット（0–2 子タブ、6/7 ページ送り、8 戻る、9–14 エントリ最大 6、15 詳細）。操作は `onSlotClick`（`ENTRIES_PER_PAGE=6`）。子タブ切替・ページ送りで選択クリア
- 選択時スロット 15 は `RecordsDisplayStackFactory.regionDetail` / `spiritDetail`。左ページは CUSTOM_NAME をタイトル、LORE 前 2 行を状態、第 3 行を段階タイトル、第 4 行（`currentStageStoryKey`）を折返しストーリー表示 — リポジトリ内で `storyKey` を初めて消費。未選択時は集計比率
- 表示スタック: 地域は発見済 `filled_map` / 未発見 `map`+`???`；霊は邂逅済 `amethyst_shard` / 未邂逅 `gray_dye`+`???`；達成は固定 6 行
- `getSpiritBondRecords` は `spiritBondStageForPoints` で段階導出（2N→N）
- `discovery` / `bond` が空のとき `commands.cresora.guide.discovery.empty` / `.bond.empty`

冒険之証 UI:

- プレイヤーインベントリスロットは非登録。380×220、開書式の見開き
- 左: 見聞進捗、円形進捗環、3 段緑バー、章報酬スロット、「受取」ボタン
- 右: 章タイトル、`<` `>`、タスクカード 3（スロット・進捗・報酬・受取／状態）
- 左縁タブ装飾、右上 ✕ で主メニューへ

---

### 8.2 戦闘基盤

#### FieldMobPackService

- ファイル: `FieldMobPackService.kt`
- 責務: 自然湧き敵対の分類（雑魚／精鋭／フィールドボス／パック随伴）、パックスポーン、ボス自然湧き、スケール係数、command tag、日食昇格
- API: `initializeOnSpawn`, `markExplicit(mob, elite)` / `markExplicit(mob, elite, boss)`, `eliteHealthScalar` / `eliteDefenseScalar` / `eliteToughnessScalar` / `eliteDamageScalar`, `scaleBonus`, `classificationTag`, `eliteKey` / `bossKey` / `packIdKey`, `promoteToEclipseElite` / `demoteEclipseElite`

スケール定数:

| 分類 | HP | 防御 | タフネス | ダメージ | モデル |
|---|---|---|---|---|---|
| 精鋭 | ×1.32 | ×1.18 | ×1.12 | ×1.16 | +18% |
| フィールドボス | ×2.20 | ×1.45 | ×1.35 | ×1.35 | +35% |

仕様:

- パック: 18% で 3〜5 体、内部 1 体を精鋭化。ボス湧き成功時はパック非生成
- フィールドボス自然湧き: **0.25%**
- command tag: `cresora_normal_mob` / `cresora_elite_mob` / `cresora_boss_mob` / `cresora_pack_mob`
- `cresoraIsEliteMob()` は `boss == true` でも `true`（ボスは精鋭上位互換）。`when` 順でボス先行のため二重加算なし
- フィールドボス撃破: 冒険 XP・CSC **×3.0**、`CHORD_PROGRESSION` +120 確定、`SUBSTITUTE_CHORD` +60（35%）
- フィールドボスは血月日食昇格の対象外
- ドメイン／マスカレードは `boss: Boolean` で明示生成し `markExplicit(mob, elite \|\| boss, boss)` を呼ぶ

#### CombatStatSupport

- ファイル: `CombatStatSupport.kt`
- 責務: 表示用会心・耐性の正規化
- 定数: `BASE_CRIT_RATE_PERCENT = 5.0`, `BASE_CRIT_DAMAGE_PERCENT = 50.0`
- API: `effectiveDisplayValue`, `effectiveCritRateRatio`, `effectiveCritDamageRatio`

#### CombatDamageResolver

- ファイル: `combat/CombatDamageResolver.kt`
- 責務: `CombatDamageRequest` → `ResolvedCombatDamage` の統一計算口
- 仕様: 通常攻撃・プレイヤー起点魔法スキルは 基礎 → 与ダメ → 会心 → 対象耐性。耐性低下後のモブ耐性は `combat_balance.json` 範囲（現行 `-50%〜75%`）へクランプ。真ダメージはこの列を通らない

#### CombatMobDisplayService

- ファイル: `CombatMobDisplayService.kt`
- 責務: mob 頭上ラベル、浮遊ダメージ数値、アクションバー被弾／与ダメ通知
- API: `updateMobStatus`, `showDamage`, `showTrueDamage`, `showIncomingDamage`, `showPlayerHitFeedback`, `tick`, `discardOrphanedIndicator`

仕様:

- ダメージ表示は tag `cresora_damage_indicator`。追跡はメモリ `activeIndicators`（network entity id）のみ。エンティティはチャンク永続のため孤児化しうる → `discardOrphanedIndicator` がロード時回収
- `activeIndicators` 登録は `spawnEntity` より前（`ENTITY_LOAD` が同期発火するため）
- タグ導入前の残留は翻訳キー prefix `combat.cresora.damage_type.short.` で自己修復
- `tick` は 16 tick で discard

#### NaturalRegenService

- ファイル: `NaturalRegenService.kt`
- 責務: combat 状態と `combat_balance.json` 最大 HP 比に基づく滑らかな自然回復（ランク帯固定 HP 段差は廃止）
- API: `init()`, `markCombat(player)`

#### CresoraDebuffService / 旋律共鳴

- ファイル: `debuff/CresoraDebuffService.kt`, `debuff/CresoraDebuffHooks.kt`
- 責務: 旋律共鳴の音符付与・反応、Crescendo 対象指定ダメージ倍率、期限切れ掃除
- API: `triggerElementalSkill(player, noteId, radiusMeters)`, `getCrescendoMultiplier(target, attacker)`, `cleanupAll(server)`

その他の負マーク（`WeaponSkillService` 管理）:

- **キュン死 (kyundeath)**: 会心時確率付与。対象与ダメ -20%。ネームタグ末尾 `[❤]`
- **嘆き (nageki)**: 「遥かなる少女の決意」非所持時。攻撃力 -50%、防御 +100%、非確定ダメの術変換、10 秒毎 50% で 3 ハート無期限シールド（重複不可）

##### 旋律共鳴 (Resonant Chords)

武器スキルで音符を付与し、異なる音符の重ねで共鳴反応が発生。

| 音符 | 傾向 |
|---|---|
| 高音 `TREBLE` | 風・雷・移動速度上昇など |
| 低音 `BASS` | 盾・防御・闇など |
| 旋律 `MELODY` | 水・雨・蘭など |
| 和声 `HARMONY` | 共鳴・祝福・日の光など |

付与仕様:

- `WeaponSkillDefinition.note`（`"treble" \| "bass" \| "melody" \| "harmony"`）。CWC の skill ブロックで指定
- `note` 未指定または `radiusMeters == 0` はスキップ。**`note` 付きアクティブは必ず `radius`**（チュートリアル武器含む）。例外: `cooldown: 0s` のメニュー専用（例 `qianqiu_yeluo`）
- 対象は `HostileEntity` / `Monster` のみ。期限は `MinecraftServer.overworld.time` に統一

共鳴反応:

| 反応 | 組み合わせ | 効果概要 |
|---|---|---|
| 不協和音 Discord | 高音+低音 | 魔法 10.0、移動低下 IV 4s、周囲 5m ノックバック+3.0 |
| 急進曲 Crescendo | 高音+旋律 | 魔法 6.0、発光 6s、対その敵与ダメ 1.5× 6s |
| 漸退曲 Decrescendo | 低音+旋律 | 魔法 4.0、弱体 III + 移動低下 II 6s |
| 独奏狂詩曲 Cadenza | 高音+和声 | 確定魔法 12.0（`dealTrueDamage`）、スキル CT 総量の 20% 短縮 |
| 鎮魂曲 Requiem | 低音+和声 | 魔法 5.0、移動不能（低下 VI）3s、衝撃吸収 III 6s |
| 小夜曲 Serenade | 旋律+和声 | 魔法 4.0、弱体 I 5s、自他 8m 内味方を 8.0 HP 回復 |

---

### 8.3 月相・血月

#### MoonPhaseService

- ファイル: `MoonPhaseService.kt`
- 責務: 11 日周期の通常月相、18:00 夜開始判定と通知、特殊月相の抽選・永続化、月相由来 mob 補正
- API: `currentNight`, `moonLayer`, `damageMultiplier`, `healthScalar`, `levelBonus`, `summaryLine`, `setPhaseOffset`, `clearSpecialMoon`, `clearBloodMoon`, `refreshLoadedHostiles`, `isSolarEclipse`, `isLunarEclipse`, `killRewardMultiplier`, `scheduleBloodMoonForNextNight`（祭壇連携）

仕様:

- 通常月相: `朔 → 既朔 → 上弦 → 逾弦 → 几望 → 望 → 既望 → 退望 → 下弦 → 残月 → 晦`
- 特殊月相は互斥抽選。命中時はその夜の通常増強を無効化
- `血月` は 11 日周期内で最低 1 回発生するよう保底
- 当夜通知: `message.cresora.moon.night_announce`（en / ja / zh_cn / lzh）
- `血月` 詳細は `BloodMoonService`。`死月`・`？？` は接続口のみ
- `日食`: 天然湧き敵対を精英怪化（HP×1.32 / ATK×1.16 / DEF×1.18 / サイズ+18%）。`promoteToEclipseElite` / `demoteEclipseElite`
- `月食`（恵みの月夜）: 撃破 XP・CSC ×2（`killRewardMultiplier`）。バニラ戦利品は `LivingEntityMixin.cresora$doubleLunarEclipseLoot` がもう 1 回ロール。`moon_brick` 2%→8%。モブ強さ・湧き量は通常
- 永続状態 `forcedBloodMoonDay`: その夜は通常抽選より優先して血月を適用し、解決後に消費
- 血月夜間減速（`timeOfDay`）の実行は `BloodMoonService` に一本化。`timeScaleMultiplier` / `specialDamageMultiplier` / `specialHealthScalar` は非血月特殊月相向け予約フック
- `/cresora moon set` は英文 id: `new_moon`, `crescent_one`, `first_quarter`, `waxing_gibbous`, `near_full`, `full`, `full_after`, `wane_after`, `last_quarter`, `waning_crescent`, `old_moon`
- `/cresora moon set_special`: `blood_moon`, `solar_eclipse`, `lunar_eclipse`, `death_moon`, `unknown`（血色戦争中の非血月設定は拒否）
- `/cresora moon stop_blood_moon`: 当夜血月のみ解除
- `/cresora moon clear_special`: 必要時に血月停止フローへ委譲

#### BloodMoonService

- ファイル: `BloodMoonService.kt`, `BloodMoonHooks.kt`
- 責務: 血月夜の特殊ルール、bed 二度押しで血色戦争開始、参加者／wave／休息／報酬 chest／mob griefing、wave clear buff、自然 spawn pressure
- API: `init`, `clearTransientState`, `debugStop`, `stopAndClearNight`, `hasActiveBattle`, `playerDamageMultiplier`, `playerHealthMultiplier`, `damageMultiplier`, `maybeDuplicateNaturalSpawn`

仕様:

- 血月夜は時間進行 50%。通常補正: hostile HP +20%、damage +10%、自然 spawn pressure +20%
- bed は通常睡眠ではなく challenge 確認／参加。開始時 bed 周辺 50 block の player を参加者確定。途中離脱は bed 周辺へ戻す
- 開始時、選ばれた bed は special red bed に置換。勝利・中断で復元、破壊敗北時のみ消失
- wave 20 回。最終以外は各 wave 後 30 秒現実時間休息。clear は「実際に spawn 済み」が条件
- 休息と action bar countdown は `world.time` 基準
- wave clear ごとに +10% damage / +20% max HP 累積
- 波次生成は成功生成数ベース。0 体時は波を進めず短遅延リトライ
- active wave hostile は bed へ進軍し、接触で mob rank ベース耐久ダメ。頭上に floating HP（無敵中は guarded）
- bed 耐久 100% 開始。75 / 50 / 25% 到達でその wave 残り時間だけ無敵、次 wave 開始で解除。0% で即敗北
- player は battle bed を attack / break 不可。保護は bed と近傍 TNT 着火阻止に限定（`AttackBlockCallback` + `PlayerBlockBreakEvents.BEFORE`）
- active battle 中 overworld は dawn せず夜へ巻き戻し
- 最終 wave は elite `minecraft:wither` ×2。spawn hostile に `GLOWING`
- challenge 中 `DO_MOB_GRIEFING` を一時 false。respawn point 上書きは終了・中断・respawn 後に戻す
- 全員 offline で自動中断し掃除。`debugStop` / `stopAndClearNight` は battle・pending・bed lock 解除、wave mob despawn、griefing 復帰の収束口。`hasActiveBattle` でコマンド側の状態分裂防止
- 勝利後、参加者 1 人につき chest 1。owner 以外回収不可。報酬: 100k CSC、5 部位 ★5 聖遺物（スロットごと独立セット抽選）、確定 `blood_note`、50% `lossless_crown` (★5)、75% `blood_tear` (★4)、現 rank 必要 XP 上限 50% 分の adventure XP
- ownership / reward-seed は `BloodMoonRewardChestStateService` 経由で永続化し、再ログイン・再起動後も復元

#### MoonAltarService

- ファイル: `bloodmoon/MoonAltarService.kt`
- 責務: `moon_altar` 右クリック、`blood_note` 投入で翌夜血月予約、hostile 撃破時 `moon_brick` ドロップ
- API / 仕様:
  - `UseBlockCallback` で処理
  - `tryDropMoonBrick(player, hostile)` — 通常 2%（月食中 8%）
  - 予約は `MoonPhaseService.forcedBloodMoonDay` に保存
- 関連コンテンツ: `cresora-utilities:moon_brick`（item）、`moon_altar`（block + block item）、`data/.../recipes/moon_altar.json`

---

### 8.4 装備・聖遺物

#### EquipmentGenerationService

- ファイル: `EquipmentGenerationService.kt`
- 責務: 聖遺物生成、rarity / main / sub 抽選、サブ伸び値
- API: `createEquipment`, `createPendant`, `rollNewSubStat`, `rollSubStatIncrease`, `rollMainStatIncrease`

#### EquipmentUpgradeService

- ファイル: `EquipmentUpgradeService.kt`
- 責務: レベル上昇、`+4` 成長イベント、β式リロール
- API: `applyLevels`, `applyGrowthEvent`, `rerollToMaxWithPrioritySubStat`

#### EquipmentPlayerSupport

- ファイル: `EquipmentPlayerSupport.kt`
- 責務: Trinkets + 主手武器装着聖遺物から `EquipmentData` 集約、セット解決、総ステータス
- API:
  - `getEquippedEquipmentData`
  - `getActiveSetBonuses` — Trinkets と武器装着聖遺物を**独立ソース**として結合（同一セット 4+4 なら両系統同時発動）
  - `getActiveSetSummaries`
  - `getAggregatedStats` — Trinkets・武器装着聖遺物・セットボーナスの 3 ソース加算。持ち替えで能動切替

#### EquipmentEffectHookService

- ファイル: `EquipmentEffectHookService.kt`
- 責務: セット effect hook の条件ディスパッチ、表示スタック
- API / 仕様:
  - `getDisplayStacks(player, buffId, rawStacks)`
  - `ArtifactSkillRegistry` が毎 tick `onTransientStateTick`（duration buff の期限を `on_tick` と独立保証）
  - 切断時 `clearTransientState`、オンライン外は `pruneTransientState`

#### EquipmentAttributeService / WeaponAttributeService

- ファイル: `EquipmentAttributeService.kt`, `WeaponAttributeService.kt`
- 責務: 毎 tick で属性 modifier 反映
- 補足: Equipment は Attack / MaxHealth / Armor。Weapon は held に応じた Attack / Speed / 一部 skill スカラー

---

### 8.5 武器・スキル・霊絆

#### WeaponUpgradeService

- ファイル: `weapon/WeaponUpgradeService.kt`
- 責務: レベル／スキル強化コスト、限界突破（精錬）とロール専用素材（T1 証 / T2 極意）
- API: `baseUpgradeCost`, `skillUpgradeCost`, `skillArtifactCost`, `breakthroughRoleMaterialCost(rarity, targetBreakthrough)` — 2★=1, 3★=2, 4★=4, 5★=8

精錬段階制限:

| 精錬 | レベル上限 | スキル上限 |
|---|---|---|
| 0 | max の 50% | 3 |
| 1 | max の 75% | 7 |
| 2 | max の 100% | 10 |

#### WeaponUpgradeScreen

- ファイル: `client/.../weapon/WeaponUpgradeScreen.kt`, `weapon/WeaponUpgradeScreenHandler.kt`
- 責務: ステータス閲覧・強化・スキル説明・聖遺物装着を 1 パネル化。`backgroundWidth` = 312。左に武器ショーケース（スロット 79, 26）、右にタブ

タブ:

1. **STATUS** — 攻撃力、会心、全ダメ、HP、攻速、属性、ロール、レア度
2. **UPGRADE** — レベル・精錬、スキル強化、分解
3. **SKILL** — 説明テキスト、次レベル／CT プレビュー
4. **ARTIFACTS** — 聖遺物 2×2。着脱可

分解制限: 聖遺物装着中は無効。警告 `screen.cresora.weapon_upgrade.cannot_dismantle_has_artifacts`

#### WeaponCombatSupport

- ファイル: `weapon/WeaponCombatSupport.kt`
- 責務: 攻撃力・速度・会心・全ダメ・スキル値（シールド／回復等）の計算
- API: `attackDamage`, `attackDamageModifier`, `attackSpeedModifier`, `critRateBonusPercent`, `allDamageBonusPercent`, `skillValueHearts` / `secondarySkillValueHearts`, `skillValuePercent` / `secondarySkillValuePercent`, `shieldHp`, `healHp`, `currentHpTrueDamageRatio`, `healHp(player, amount)`, `grantShield`, `applyStatusEffect`

精錬戦闘ボーナス:

| 精錬 | 効果 |
|---|---|
| 0 | 100% 属性 |
| 1 | 115% 属性、会心 +5% |
| 2 | 130% 属性、会心 +10%、全ダメ +10% |

#### WeaponSkillService & WeaponSkillRegistry

- ファイル: `weapon/WeaponSkillService.kt`, `skill/WeaponSkillHandler.kt`, `skill/WeaponSkillRegistry.kt`
- 設計: Strategy パターン。各スキルは独立ハンドラ。`WeaponSkillService` は共通インフラのみ

`WeaponSkillService` 責務 / API:

- 共通 CT（BossBar）。**残りティック数**管理でデバフによる速度変更に対応
- `grantShield(player, amount, duration, weaponId?)` — `weaponId` null 時はメインハンド武器 ID
- `applySoulBreak` / `getSoulBreakStacks` — 破魂（物理耐性・防御低下）
- `dealTrueDamage` — 真ダメ一元管理。`isDealingTrueDamage` で耐性・スケール迂回。`isTrueDamage` を `onDamageDealt` へ伝播
- イベントを `WeaponSkillRegistry` 経由でディスパッチ
- アクティブスロット制限: `lastHeldWeaponIdByPlayer` でメインハンド／`HotbarOverrideService` 一時指定と一致検証。切替時 `clearTransientState`
- mark / 破魂 / 一時無敵は online 集合では prune せず、各 effect の expiry で回収
- `getDisplayStacks` — 幼馴染 4 セット +「遥かなる少女の決意」装備中、決意バフ（`ketsui`）表示に +5

`WeaponSkillHandler`:

- `activate`, `onTick` / `onPlayerTick`, `onDamageAbsorbed`, `onDamageDealt`, `onDamageTaken`, `clearTransientState`, `pruneTransientState`（デフォルト空）

`WeaponSkillRegistry`:

- `effectId` ↔ ハンドラ。`CompiledWeaponSkillRegistry`（CWC）が自動登録
- `registerSubSkill(parent, sub)` — 親装備中にサブの `onTick` / `onPlayerTick` も毎 tick
- `getDefinition(id)` で逆引き

`HotbarOverrideService`:

- `open_skill_menu` 時に元ホットバー・親武器 ID・当時の `WeaponData`・許可サブ ID を保持
- `SubSkillItem` は親文脈で発動（`WeaponDefinition.DUMMY` ではなく実データ）

#### SpiritBondService / SpiritRenderer

- ファイル: `weapon/SpiritBondService.kt`（server）, `client/.../weapon/SpiritRenderer.kt`
- 責務: 武器擬人化「霊」の同期と描画
- API: `SpiritBondService.init` / `syncInventory`, `SpiritRenderer.init`

仕様:

- サーバー: `END_SERVER_TICK` で 1 秒ごと（`time % 20`）に `spirit` 付き武器へ `ResonanceService.spiritBondStage` を当て `WeaponData.spiritBondStage` 同期
- クライアント: `END_CLIENT_TICK` で `time % 4`、48m 内。メインハンド霊武器でオーブ（`END_ROD`、stage≥3 で `ENCHANT`、≥5 で `GLOW`）。半径は stage 線形拡大
- ガチャ重複で `addSpiritBondPoints`。しきい値 `[0, 40, 100, 200, 350, 550]` → stage 1..6
- ツールチップ（`CresoraWeaponItem`）と強化画面 Stats が stage / `titleKey` / `awakeningConditionKey` を表示

---

### 8.6 コンテンツセッション

#### DomainService

- ファイル: `DomainService.kt`
- 責務: 秘境 session、arena、wave、報酬、story linked stage
- API: `hasActiveSession`, `generateOneOffRewards`, `startSession`, `startLinkedStoryStage`, `tick`, `onPlayerDeath`, `onPlayerDisconnect`, `damageMultiplier`, `isDomainMob`

#### StoryService

- ファイル: `StoryService.kt`
- 責務: 楽章演奏 session、プレ／ポスト進行、カウントダウン、survival objective、一時貸与武器、`resonantChordTutorialSteps` 進行（章 ID 特例なし）
- API: `hasActiveSession`, `damageTakenMultiplier`, `allowsTrueDamage`, `startSession`, `tick`, `onPlayerDeath`, `onPlayerDisconnect`, `onResonantChordTriggered`, `handleDialogueAction`

`startSession` ゲート（順）:

1. session 重複（story / domain / masquerade）
2. `unlockRank` ≤ AdventureRank
3. `prerequisiteChapterId` クリア済
4. `requiredRegionId`（`StoryFlagService.hasFlag("region_visited_<id>")`）
5. `requiredSpiritId` + `requiredBondStage`（`ResonanceService.spiritBondStage`）
6. `linkedDomainId` があれば `DomainService.startLinkedStoryStage`
7. 武器配布スロット空き

オプショナルゲート: `requiredRegionId`, `requiredSpiritId`, `requiredBondStage`（1..6、spirit 設定時のみ必須、未設定時 0）

旋律共鳴チュートリアル: `resonantChordTutorialSteps` 非空なら全ステップ反応確認で戦闘完了。各ステップは `reactionKey` / `effectKey` / 一時貸与 2 本（`removeOnExit`）。`grantedWeapons`・`battleObjective` との併用禁止。空きスロットは全ステップ最大貸与本数から算出

#### StoryProgressService

- ファイル: `StoryProgressService.kt`
- 責務: 章クリア履歴、前提章ロック
- API: `clearedChapterIds`, `isCleared`, `markCleared`, `copyTo`, `missingPrerequisite`

#### StoryFlagService（Phase 4）

- ファイル: `story/StoryFlagService.kt`, `story/StoryFlagAccess.kt`, `java/.../mixin/ServerPlayerEntityMixin.java`（`cresora$storyFlagsRaw`）
- 責務: NPC 対話・章進行用汎用フラグ（`String` 集合）。NBT `cresora_story_flags` にカンマ区切り永続。`copyTo` で重生引継ぎ
- API: `getFlags`, `hasFlag`, `setFlag` / `unsetFlag`, `checkFlags(required: Map<String,Boolean>)`, `copyTo`

#### StoryDialogueNetworking（Phase 0 / 4）

- ファイル: `story/StoryDialogueNetworking.kt`, `client/.../story/StoryDialogueClient.kt` / `StoryDialogueScreen.kt`
- 責務: `DIALOGUE` / `COUNTDOWN` / `NPC_DIALOGUE` を `StoryDialogueStatePayload` で配信。Phase 4 で分岐選択肢・フラグ条件。サーバー側 `NpcDialogueSession`
- API: `startNpcDialogue`, `showSimpleDialogue`, `showCountdown`, `close`

#### MasqueradeService

- ファイル: `MasqueradeService.kt`
- 責務: run session、持込武器制限、inventory snapshot／restore、wave、support buff、シーズン進捗
- API: `hasActiveSession`, `clampPlayerDamageReduction`, `currentWave`, `clearedWaveCount`, `supportCandidateIds`, `getAggregatedSupportStats`, `startSession`, `selectSupportBuff`, `tick`, `onPlayerDeath`, `onPlayerLeave`, `restoreAfterRespawn`, `restoreAfterJoin`, `damageMultiplier`, `damageTakenMultiplier`, `adjustIncomingDamage`

復旧仕様:

- 開始前に元インベントリ・offhand・選択スロット・帰還地点を `MasqueradeRecoveryPersistentState` に永続化してから切替
- 切断・再起動後は JOIN で一度だけ復元。切断 callback では packet を送らない
- 死亡時は respawn 用に印付け、`COPY_FROM` 後にインベントリのみ一度復元
- domain world 取得不能な teardown でも session / runtime mob map は必ず切り離す

#### LeyLineService

- ファイル: `leyline/LeyLineService.kt`, `leyline/LeyLineHooks.kt`
- 責務: 7 元素（日・月・火・水・木・金・土）の地脉喷涌ライフサイクル、キー返却、境界パーティクル、不在 10 秒失敗、T1–T4 ウェーブ、報酬 chest
- API:
  - `placeLeyLine(world, pos, player, element)` — 50 秒有効期限。同 world/pos に pending／active があれば `false`（キー未消費）
  - `onBlockRemoved` — 開始前破壊でキー返却
  - `startEvent(player, pos, tier)` — pending の所有者 UUID・ブロック・元素を検証。元素は pending から導出（GUI 値は受け取らない）。配置者のみ開始可。10×10 セッション登録
  - `tick(server)`, `damageMultiplier(attacker)`
- エリート: `FieldMobPackService.markExplicit(mob, true)` + `applyMobScaling`、全モブ `GLOWING`
- 報酬: 配置者が領域内なら付与。協力者は受取人でない。最終 wave 時に配置者領域外なら失敗破棄。プレビューは `ArtifactUiFlow.openDomainReward`

#### TreasureChestService

- ファイル: `treasure/TreasureChestService.kt`, `treasure/TreasureChestPersistentState.kt`
- 責務: 共鳴探索（Resonance Hunt）— 宝箱ライフサイクルと守護者チャレンジ、NBT 永続

構成:

- **共鳴探索コンパス (`resonant_locator`)**: 安全スポーン探索して宝箱設置。使用時 CD 12 秒
- **共鳴の宝箱**: TTL 10 分、厳格な所有者チェック
- **ガイドトレイル**: 出現時 `END_ROD` ビーム
- **守護者チャレンジ**: 所有者 6 block 接近で開始
  - 3★: 通常 3 / 4★: 通常 2+精鋭 1 / 5★: 通常 2+精鋭 2（うち 1 はウィザスケ）
  - スケール: `applyMobScaling` + `combat_balance.json` field boss 補正
  - 頭上 `TextDisplayEntity`（`y + 1.25`）で進捗
  - 死亡または 32 block 離脱で失敗リセット
  - 討伐 CSC: 通常 40 / 精鋭 100 + ゴールドナゲット。開封: 50% 対応レア聖遺物 Lv0–4、または 50% 武器欠片
- `TreasureChestPersistentState` で再起動後復元
- 通知キー: `message.cresora.treasure_chest.*`（en / ja / zh_cn / lzh）

---

### 8.7 共鳴ガチャ

#### ResonanceService

- ファイル: `ResonanceService.kt`
- 責務: コード進行／代理コード、限定 SELECT／常設ガチャ、pity／deep pity／arpeggio、LIMITED 用 PU★5 のプレイヤー単位選択（変更時は LIMITED ピティ群全リセット）
- API: `getProgress`（含 `selectedFeaturedWeaponId`）, `getCurrency` / `addCurrency` / `setCurrency` / `spendCurrency`, `copyTo`, `currencyCount`, `canPull`（LIMITED は PU 未選択で false）, `pull` → `Success | NotEnoughCurrency | FeaturedNotSelected | NoFiveStarWeaponsAvailable`, `getSelectedFeaturedWeaponId`, `selectableFeaturedWeaponIds`, `setSelectedFeaturedWeaponId`, `getSpiritBondPoints` / `addSpiritBondPoints`, `spiritBondStageForPoints`, `spiritBondStage`

LIMITED ★5 抽選:

- ★5 時、`limitedFiveStarGuaranteed` または `random < 0.5` なら PU（確定枠消費）
- それ以外は全 ★5 から PU 除外リストを均等抽選（すり抜け）
- PU 以外が無い場合はフォールバックで PU

霊絆しきい値: `[0, 40, 100, 200, 350, 550]` → stage 1..6

---

### 8.8 Phase 4 ワールド / NPC

#### NpcDialogueContentRegistry

- ファイル: `npc/NpcDialogueContentRegistry.kt`, `data/.../cresora/npc_dialogue_content.json`
- 責務: NPC 対話ツリー JSON ロード。ノードは `speaker` / `body` / `choices` / `flagsToSet` / `conditionFlags` / `nextNodeId`。選択肢は `actionId` / `label` / `requiredFlags` / `nextNodeId`。欠落時は `spirit_guide_intro` デモをフォールバック
- API: `init`, `getTree`, `getRootNode`, `getNode`

#### SpiritGuideService / SpiritGuideEntity

- ファイル: `npc/SpiritGuideEntity.kt`, `client/.../npc/SpiritGuideRenderer.kt`, `world/SpiritGuideService.kt`, `world/CresoraWorldKeys.kt`, `world/PortalBlock.kt`
- 責務: `cresora_world` 案内人の着地・スポーン、ポータル双方向遷移
- 仕様:
  - Entity: `MOVEMENT_SPEED=0`, `KNOCKBACK_RESISTANCE=1`, `shouldSave=false`, `DIALOGUE_TREE_ID` を DataTracker 同期
  - Service: 5 秒ごと着地エリア／ポータル差分復元、案内人 1 体維持（重複・レガシー村人 `discard`）。チャンク未ロード時スキップ
  - 右クリック → `StoryDialogueNetworking.startNpcDialogue`
  - `PortalBlock`: OW ⇄ `cresora_world`（着地時チャンク先読み + 案内人確保）
  - `MinecraftServerMixin` で界門のスポーン保護バイパス
- API: `init`, `ensureLanding`, `ensureGuide`

---

## 9. UI / Command

### 9.1 ArtifactUiFlow

- ファイル: `ArtifactUiFlow.kt`
- 画面遷移の単一窓口

API: `openMenu`（Deprecated → `openGuide`）, `openGuide`, `openStoryChapterSelection`, `openStoryStageSelection`, `openRewardSummary`, `openWeaponUpgrade`, `openDomainSelection`, `openMasqueradeLoadout`, `openMasqueradeSupport`, `openDomainReward`, `openShop`, `openResonance`, `openResonanceResult`, `openResonanceFeaturedSelection`, `openUpgradeScreen`, `openAlphaSelection`, `openBetaSelection`, `openWeaponSkillMaterialSelection`

### 9.2 Commands

- ファイル: `Commands.kt`

| コマンド | サブ |
|---|---|
| `/about` | |
| `/cresora moon` | `set`, `set_special`, `clear_special`, `stop_blood_moon` |
| `/cresora_stats` | |
| `/cresora_rank` | `set`, `addxp`, `setxp` |
| `/cresora_credits` | `get`, `add`, `set` |
| `/cresora_shop` | |
| `/cresora_domain` | |
| `/cresora_masquerade` | |
| `/cresora_story` | `list`, `start <chapter_id>` |
| `/cresora_resonance` | `currency`, `get`, `add`, `set` |
| `/cresora guide` | （[GuideService](#guideservice) 参照） |
| `/cresora_records` | |

`/cresora_stats` 表示: Adventure Rank, CSC, セット効果概要, `ATK / HP`, `DEF / Crit`, `All DMG / Physical RES / Arcane RES`, 実攻撃力 / 最大 HP / 防御力

---

## 10. コンパイラ

`GRADLE_USER_HOME=.gradle-user ./gradlew compileAssets` で CWC / CAC / CMC を一括実行。生成物は `build/generated/cresora/{kotlin,resources}` を毎回全再生成。`src/main/resources` は読み取り専用基底で、`lang` / `items` / `models/item` はオーバーレイへコピーしてから DSL 出力を重ねる。`processResources` は古い checked-in 生成物を除外。

### 10.1 Cresora Weapon Compiler (CWC)

`.cresora` →:

- Kotlin: `hifumi.cresora.skill.generated.*Skill`
- JSON: `cwc_weapon_content.json`
- 登録: `CompiledWeaponSkillRegistry`

既存の手動 `weapon_content.json` は実行時にマージ。

#### 主な機能

- **自動インポート**: `Text`, `LivingEntity`, `ServerWorld`, `ParticleTypes`, `WeaponSkillService`, `AdventureRankService` など
- **ソース抽出**: `execute` はトークン再結合ではなくオフセット切出し（`as?` / `?.` / 改行・コメント維持）
- **実行ラベル**: `run execute@ { ... }` → `return@execute` 可
- **`decay: independent`**: スタックごと `expireTicks: MutableList<Long>`
- **AOE**: `area_of_effect` は専用 AST。`ignite` 等を 1.21.7 レジストリ API 向けに変換
- **`translations`**: `en_us` / `ja_jp` / `zh_cn` / `lzh` をオーバーレイ `lang/*.json` へマージ（ソース資産は非破壊）
- **サブスキル**: `open_skill_menu` / `sub_skill`。ホットバー 0–8 に展開し、終了・時間切れで復元。親との対応も registry 登録
- **入力拒否**: unknown token / field / 不明コマンド名は parse／codegen エラー（コメント握りつぶし廃止）
- **Lexer**: `//` と `/* ... */` 両対応

```cresora
weapon "Name" {
    id: "id"
    stats { ... }
    skill "Tactical Stance" {
        on_activate {
            open_skill_menu("flame_strike", "ice_wall", 5s)
        }
    }
    sub_skill "Flame Strike" {
        effect_id: "flame_strike"
        icon: "minecraft:blaze_powder"
        on_activate {
            add_buff("burn", 1)
            close_skill_menu()
        }
    }
    translations {
        ja_jp {
            name: "武器名"
            sub_skill_flame_strike_name: "炎の連撃"
        }
    }
}
```

#### 型付きアクションノード

ハンドラ直下・`area_of_effect` 内・`execute` トップレベル文の次は専用 AST + パース時検証（CWC / CAC 共通）:

| コマンド | 検証 |
|---|---|
| `apply_mark(target, "id", duration)` | 引数 3、ID は文字列、duration は時間値 |
| `grant_invulnerability(target, duration)` | 引数 2 |
| `add_buff("id"[, stacks])` | ID 文字列、stacks 整数（省略 1） |
| `start_cooldown([duration])` | 引数 0–1（省略時スキル cooldown） |
| `send_message("key"[, "color"])` | color は Formatting 16 色（省略 WHITE） |
| `apply_status_effect("ns:id", duration[, amplifier])` | 識別子形式、amplifier 整数（省略 0） |

時間値 (`DurationValue`): `30s` = 秒（×20 tick）、bare 数値 = tick、`skill_duration` = スキル duration 実行時参照。artifact 文脈では `start_cooldown` / `skill_duration` はエラー。任意 Kotlin は `ExpressionNode` + `InstructionMapping.expandAll`。`execute` 内の `·` (U+00B7) はエラー（KotlinPoet 内部使用）。

#### コンパイラテスト (`compilerTest`)

- `src/compilerTest/kotlin/`（JUnit 5）: `GRADLE_USER_HOME=.gradle-user ./gradlew compilerTest`
- カバレッジ: Lexer、不正 DSL パース、`area_of_effect`、型付きノード、`InstructionMapping`、CWC/CAC golden
- golden: `src/compilerTest/resources/golden/`。意図変更時は失敗出力 `build/golden-actual/` で期待値更新

### 10.2 Artifact Compiler (CAC)

`.artifact` →:

- Kotlin: `hifumi.cresora.equipment.generated.ArtifactSkill_...`（`ArtifactSkillHandler`）
- JSON: `cac_artifact_content.json`
- 登録: `CompiledArtifactRegistry`

#### 機能

- 動的ステータス: `getAttackDamageScalar` / `getArmorScalar` / `getCritRateBonus` / `getCritDamageBonus` → `EquipmentPlayerSupport.getAggregatedStats` が集約
- `decay: independent` — `ArtifactSkillRegistry.onTransientStateTick` で毎 tick クレンジング
- 組み込み命令: `log`, `apply_mark`, `spawn_particles`, `add_buff`, `send_message`, `apply_status_effect`, `grant_invulnerability`（型付き検証は CWC と同じ。`start_cooldown` / `skill_duration` / `skill_value` は不可）
- `execute` 生ソース抽出

#### `requires_weapon`

- `set N { requires_weapon: "weapon_id"; }` — 指定武器がインベントリにあるときのみセット発動
- 指定時 `stats` はフラット JSON ではなくハンドラメソッド生成
- イベントハンドラが無くても `requires_weapon` / `display_stack_bonus` のみでハンドラ自動生成

#### `display_stack_bonus`

- `display_stack_bonus "buffId" { stacks: N; }` — 表示スタック加算
- 経路: `EquipmentEffectHookService.getDisplayStacks` → `WeaponSkillService.getDisplayStacks`

#### 相互作用例: 幼なじみ × 遥かなる少女の決意

- `osananajimi.artifact` 4 セットが `requires_weapon: "harukanaru_shojo_no_ketsui"`
- 会心ダメ +10%、全ダメ +10%、決意バフ表示 +5（上限 100 に影響せず最大 105）。会心獲得メッセージにも +5 反映

### 10.3 Movement Compiler (CMC)

`.movement` → メインストーリー／クエスト進行アセット。

- メタ: chapter ID、`sort_order`、タイトル、解放ランク、前提、秘境 ID 等
- フェーズ:
  - `phase pre_battle` — `dialogue`
  - `phase battle` — `battle_objective`, `wave`, `modifiers`；`resonant_chord_tutorial` は `step` ごとに `effect_key` と貸与 2 本。生成 `resonantChordTutorialSteps` を `StoryService` が章 ID 非依存で実行
  - `phase post_battle` — 会話
  - `rewards` — CSC / 共鳴通貨等
  - `translations` — locale 別 `speaker_id` / `text_id`
- 生成: `story_content.json` / `story_texts.json`（完全生成。DSL 削除で旧 chapter/text は残らない）

---

## 11. 開発ガイド

### 11.1 JSON 化済み領域

装備種別・セット・ドロップ、武器数値・ドロップ、秘境・報酬、マスカレード wave／サポート、ストーリー章・本文、ショップ、ガチャ banner、音律エコー、mob combat profile

### 11.2 まだコードが必要な領域

- 新しい `WeaponSkillDefinition.effectId`: 標準は CWC。手動 `WeaponSkillHandler` は CWC 不可の特殊系のみ
- 新しい動的 set effect hook 本体: `EquipmentEffectHookService` へのロジック追加
- 特殊 GUI: 新規 Screen / ScreenHandler

### 11.3 追加手順

**装備セット**

1. `equipment_content.json` に slot / set / equipmentDefinitions
2. 必要なら dropProfile / mobLoot
3. モデル・テクスチャ・翻訳

**武器**

1. `src/main/cresora/*.cresora` に定義
2. assets・翻訳
3. `./gradlew compileAssets`
4. 必要なら Resonance / 秘境報酬へ接続

**ストーリー**

1. `src/main/cresora/*.movement`
2. 同ファイル `translations` に全 locale
3. `./gradlew compileAssets` → `story_content.json` / `story_texts.json`
4. ギミック不足時は CMC AST・JSON codec・runtime を同時拡張

### 11.4 注意点

- set effect hook は schema あり／動的処理は未完成
- 新武器は「完全ノーコード」ではない
- `WeaponSkillService` は依然巨大で固有効果の集中点
- 最も安定した境界は `*ContentRegistry` と `*Support`。`*Service` の private runtime は変動しやすい

### 11.5 参照優先順

1. `CreSoraUtilities.kt`
2. `ModDataComponents.kt`
3. `Commands.kt`
4. `ArtifactUiFlow.kt`
5. `*ContentRegistry.kt`
6. `EquipmentStackSupport.kt` / `WeaponStackSupport.kt`
7. `AdventureRankService.kt` / `CreditsService.kt` / `ResonanceService.kt` / `CresoraDebuffService.kt`
8. `StoryService.kt` / `DomainService.kt` / `MasqueradeService.kt`
9. `WeaponSkillService.kt`
