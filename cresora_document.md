# CreSora Utilities API Document

最終更新: 2026-06-30

このドキュメントは、Fabric 1.21.7 用 Minecraft Mod **CreSora Utilities** の内部 API、レジストリスキーマ、およびコアサービスの仕様書です。
各機能の開発履歴や完了したタスクのログについては、Git のコミット履歴 (`git log`) を参照してください。

## 1. 結論

このプロジェクトの API は、典型的な「外部公開ライブラリ API」ではありません。
実態は次の 5 本柱です。

1. `CreSoraUtilities` を起点とする初期化・登録 API
2. `ModDataComponents` と mixin access interface 群による保存 API
3. `*ContentRegistry` 群と `data/cresora-utilities/cresora/*.json` によるデータ駆動 API
4. `*Service` / `*Support` / `ArtifactUiFlow` / `Commands` によるゲーム内実行 API
5. **Cresora Weapon Compiler (CWC)**: `.cresora` スクリプトからコードと JSON を自動生成するビルドタイム API。武器のマイグレーションおよびデータ整合性チェックを自動化し、型安全な武器定義を保証します。また、アイテムモデル（`minecraft:item/handheld` 等を親とする）および基本翻訳キーの自動生成も行います。
   - `area_of_effect` は専用 AST ノードとして扱われ、文字列再構成ではなく構造化出力に変換されます。
   - `.cresora` の lexer は `//` と `/* ... */` の両方をコメントとして扱います。

要するに、今の CreSora は「サービス singleton + JSON レジストリ」に加えて「コード生成エンジン」を備えたハイブリッドフレームワークです。

## Blood Moon API 更新 (2026-05-10)

- `BloodMoonService`
  - `stopAndClearNight(server): Boolean` を追加。血月停止の単一収束口（戦役終了、pending 破棄、mob griefing 復帰）として利用。
  - `hasActiveBattle(): Boolean` を追加。コマンド側の状態分裂防止に使用。
  - 波次生成は「成功生成数ベース」に変更。`0` 体生成時は波を進めず短遅延リトライ。
  - 参加者保護は `AttackBlockCallback` に加え `PlayerBlockBreakEvents.BEFORE` を使用。
  - 報酬箱は `BloodMoonRewardChestStateService` を通じて owner/reward-seed を永続化し、再ログインや再起動後でも識別・復元可能。
- `MoonPhaseService`
  - 血月夜間減速の実行箇所を `BloodMoonService` に一本化（重複 `timeOfDay -= 1` を除去）。
  - `timeScaleMultiplier` / `specialDamageMultiplier` / `specialHealthScalar` は非血月特殊月相向けの予約フックとして維持。
- `Commands`
  - `/cresora moon clear_special` は必要時に血月停止フローへ委譲。
  - `/cresora moon set_special <non-blood>` は血色戦争中に拒否。

## Moon Altar API 更新 (2026-05-10)

- `MoonAltarService` を追加。
  - `UseBlockCallback` で `moon_altar` 右クリックを処理。
  - `blood_note` 投入時に `MoonPhaseService.scheduleBloodMoonForNextNight(server)` を呼び、次夜を血月予約。
  - `tryDropMoonBrick(player, hostile)` で hostile 撃破時 2% ドロップを処理。
- `MoonPhaseService`
  - 永続状態に `forcedBloodMoonDay` を追加。
  - その日付の夜判定では通常抽選より優先して血月を適用し、解決後にフラグを消費。
- コンテンツ追加:
  - `cresora-utilities:moon_brick` (item)
  - `cresora-utilities:moon_altar` (block + block item)
  - `data/cresora-utilities/recipes/moon_altar.json`

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
- `CompiledWeaponSkillRegistry.registerAll(this)` (CWCにより自動生成)
- `HotbarOverrideService` がサブスキル用ホットバーのセッション管理と復帰を担当します。

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
- `cresora-utilities:resonance_featured_selection`

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
- `WEAPON_ARTIFACTS: ComponentType<List<ItemStack>>`
  - id: `cresora-utilities:weapon_artifacts`
  - 武器に装着された4つの聖遺物（ItemStack）のリスト

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
  - boss flag
  - pack id

## 4. 基本モデル API

### 4.1 Stat

ファイル:

- `StatType.kt`
- `StatEntry.kt`
- `CombatDamageType.kt`
- `WeaponRarity.kt`
- `WeaponRole.kt`

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
- `WeaponRole` は `VANGUARD` (先鋒), `GUARD` (前衛), `DEFENDER` (重装), `SNIPER` (狙撃), `CASTER` (術師), `MEDIC` (医療), `SUPPORTER` (補助), `SPECIALIST` (特殊), `CATALYST` (法器) の 9 種類

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
- `breakthrough: Int` (精錬レベル 0..2)
- `spiritBondStage: Int` (霊絆段階 1..6、`normalized()` でクランプ。レガシーデータは codec のデフォルト値 `1` で復元)

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

- `effectHooks` は schema 上は存在しますが、未対応 hook は content load で reject されます。
- つまり現在の正式仕様は「`stats` は使える、`effectHooks` はまだ使えない」です。
- `mobLoot` は組み込み loot table へ注入され、artifact drop と upgrade material drop の両方を構成します。

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
- `role`
- `attackCurve`
- `customModelData` (Optional)
- `skill`
- `upgrades`
- `craft`
- `drops`
- `spirit` (Optional) — 武器擬人化「霊」メタデータ。`WeaponSpiritDefinition` を参照

`WeaponSpiritDefinition` の構造:

- `nameKey: String` — 霊の表示名翻訳キー(必須)
- `voiceLines: Map<String, String>` — `first_contact` などのフラグ別ボイスライン翻訳キー(任意)
- `bondStages: List<WeaponSpiritBondStageDefinition>` — 各段階の `{ stage, titleKey, storyKey }`(stage は 1..6、ユニーク)
- `awakeningConditionKey: String?` — 覚醒条件の翻訳キー(任意)

主 API:

- `weaponDefinitions()`
- `requireWeapon(id)`
- `definitionByFragmentId(fragmentItemId)`

重要な設計判断:

- 武器の「数値と入手」はかなり JSON 化済み
- ただし `skill.effectId` が新規なら、たいてい `WeaponSkillService` 側のコード追加がまだ必要
- `spirit` は CWC の `.cresora` DSL の `spirit { name, awakening_condition, voice_lines { ... }, bond_stage N { title, story } }` ブロックから生成される。`applyBundle` 検証で `bondStages` の重複と範囲(1..6)・必須キーの空白を弾く

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

- 限定奏鳴 (SELECT ガチャ：プレイヤー個別に★5 PU を選択)
- 常設奏鳴
- レート
- 各レアリティ pool（限定の `fiveStarPool` は省略可で、PU および「すり抜けプール」は実行時に WeaponContentRegistry の全★5から動的構成される）

主 API:

- `banners()`
- `banner(id)`
- `limitedBanners()`

注意:

- 各 rarity pool は content load 時に検証されます。
- 空 pool（LIMITED の `fiveStarPool` を除く）、負の weight、pool と rarity の不一致は reject されます。
- LIMITED バナーは `featuredFiveStarWeaponId` / `fiveStarPool` を持ちません。PU はプレイヤーごとに `cresoraGetSelectedFeaturedWeaponId()` から取得します。

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

現行の追加章:

- `0-3` / `Rehearsal of Resonance` 相当のチュートリアル章
- `0-2` クリア後に解放
- `StoryService` の専用進行で 6 種の旋律共鳴を順番に教える
- 各段階で必要な 2 本だけを一時配布し、対象の反応が発生したことを確認したら次の組み合わせへ進む
- `domain_reward_ids` で `resonance_practice` に接続する

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
- wave（`count` / `levelOffset` / `elite` / `spawnDelayTicks` / `boss`）
- unlock rank
- entry cost
- reward profile link

現行の追加ドメイン:

- `resonance_practice`
- `resonance_trial` mob pool を使う低コストの練習用ドメイン
- `resonance_practice` 報酬プロファイルと連動し、CSC を消費せず共鳴素材の導線だけを見せる

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
- 武器突破素材報酬（`proofReward` / `insightReward` -> `DomainMaterialRewardDefinition`）
  - `role`: WeaponRole (optional, e.g. `"defender"`, `"guard"`, `"medic"`, `"caster"`. If omitted/null, rolls a random role dynamically at drop time)
  - `minCount`: Int (non-negative)
  - `maxCount`: Int (>= minCount)
- 共鳴探索コンパスの副報酬枠 `resonantLocatorChance: Double = 0.0`（0.0〜1.0、`DomainService.generateRewardsForProfile` が確率判定で 1 個 `resonant_locator` を追加）
- CSC / Rank XP 報酬

現行の追加プロファイル:

- `resonance_practice`
- 低額 CSC / Rank XP と `chordProgression` / `substituteChord` を返す tutorial 向けプロファイル
- `artifactReward` や `proofReward` は持たない

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
- wave ごとの spawn（`elite` / `boss` / `modifiers`）
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
- `syncWeaponData(stack, data)`: `WEAPON_DATA` を同期し、`customModelData` が定義されている場合は `CUSTOM_MODEL_DATA` コンポーネントを適用します。
- `defaultWeaponData(definition)`
- `createWeaponStack(definition, rarity, baseLevel, skillLevel)`
- `countFragments(player, definition)`
- `removeFragments(player, definition, amount)`
- `getEquippedArtifacts(stack)`: 武器に装着されている4つの聖遺物スタックを取得します。
- `setEquippedArtifacts(stack, artifacts)`: 武器の聖遺物装着状態を更新します。
- `hasAnyEquippedArtifact(stack)`: 武器に何らかの聖遺物が装着されているかどうかを判定します。

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

### 7.1.0 FieldMobPackService

ファイル:

- `FieldMobPackService.kt`

責務:

- 自然湧き敵対モブの分類（雑魚 / 精鋭 / フィールドボス / パック随伴）
- パックスポーン制御（18% 確率で 3〜5 体のパックを作成、内部 1 体を精鋭化）
- フィールドボス自然湧き（**0.25% 確率**、出現時はパック非生成）
- 精鋭・ボス用の HP / 防御 / 攻撃 / モデルスケール係数の提供
- `cresora_normal_mob` / `cresora_elite_mob` / `cresora_boss_mob` / `cresora_pack_mob` の command tag 同期
- 日食昇格対象の選定（`promoteToEclipseElite` / `demoteEclipseElite`、ボスは対象外）

主 API:

- `initializeOnSpawn(hostile, world, spawnReason)`
- `markExplicit(mob, elite)` / `markExplicit(mob, elite, boss)` — 明示的に分類（ドメイン・マスカレード等で使用）
- `eliteHealthScalar(mob)` / `eliteDefenseScalar(mob)` / `eliteToughnessScalar(mob)` / `eliteDamageScalar(mob)` — `when { isBoss -> BOSS_*, isElite -> ELITE_*, else -> 1.0 }`
- `scaleBonus(mob)` — `BOSS_SCALE_BONUS = 0.35`、`ELITE_SCALE_BONUS = 0.18`、雑魚は `0.0`
- `classificationTag(entity)` — 頭上に付与される `[雑魚] / [精鋭] / [ボス]` ラベル（ボスは `DARK_RED + BOLD`）
- `eliteKey()` / `bossKey()` / `packIdKey()` — NBT キー
- `promoteToEclipseElite(...)` / `demoteEclipseElite(...)`（日食用）

スケール定数:

| 分類 | HP | 防御 | タフネス | ダメージ | モデル |
|---|---|---|---|---|---|
| 精鋭 | x1.32 | x1.18 | x1.12 | x1.16 | +18% |
| フィールドボス | **x2.20** | **x1.45** | **x1.35** | **x1.35** | **+35%** |

重要仕様:

- `cresoraIsEliteMob()` は `boss == true` のときも `true` を返す（多態：ボスは精鋭の上位互換）。`isFieldBoss / isFieldElite` の private 述語は `when` の順序でボスを先に判定するため二重加算は発生しない
- フィールドボス撃破時、`AdventureRankHooks` が冒険ランクXP・クレジット報酬を **x3.0**、`CHORD_PROGRESSION` を +120 確定、`SUBSTITUTE_CHORD` を +60（35% 確率）追加付与する
- フィールドボスは `BloodMoonService.promoteEclipseElite` の昇格対象から除外される（既に精鋭超のスケールがあるため）
- ボスはパック非生成。boss spawn 判定が成功するとパック判定はスキップ
- ドメイン / マスカレードの wave 定義は `boss: Boolean` で明示的にボスを生成可能。両サービスは `markExplicit(mob, elite || boss, boss)` を呼ぶ

### 7.1.1 MoonPhaseService

ファイル:

- `MoonPhaseService.kt`

責務:

- 11日周期の通常月相管理
- 18:00 の夜開始判定と当夜通知
- 特殊月相の抽選・永続化
- 月相由来の mob 追加補正

主 API:

- `currentNight(server)`
- `moonLayer(server)`
- `damageMultiplier(server)`
- `healthScalar(server)`
- `levelBonus(server)`
- `summaryLine(server)`
- `setPhaseOffset(server, targetPhase)`
- `clearSpecialMoon(server)`
- `clearBloodMoon(server)`
- `refreshLoadedHostiles(server)`
- `isSolarEclipse(server)` / `isLunarEclipse(server)`
- `killRewardMultiplier(server)` — 月食中は `2.0`、通常は `1.0`

重要仕様:

- 通常月相は `朔 -> 既朔 -> 上弦 -> 逾弦 -> 几望 -> 望 -> 既望 -> 退望 -> 下弦 -> 残月 -> 晦` の 11 日周期
- 特殊月相は互斥抽選で、命中時はその夜の通常増強を無効化
- `血月` は 11 日周期内で最低 1 回発生するよう保底される
- 当夜メッセージは翻訳キー `message.cresora.moon.night_announce`（%s に月相名）で全プレイヤーへ送信（en/ja/zh_cn/lzh 対応）
- `血月` の詳細効果は `BloodMoonService` が実装済み。`死月`・`？？` は接続口のみ公開
- `日食` の効果: 全天然湧き敵対モブが精英怪化（HP x1.32 / ATK x1.16 / DEF x1.18 / サイズ+18%）。`FieldMobPackService.promoteToEclipseElite / demoteEclipseElite` が担当
- `月食` の効果（恵みの月夜）: 敵対モブ撃破時の冒険ランクXPとクレジットが2倍（`killRewardMultiplier` 経由、`AdventureRankHooks` / `CreditsService.addHostileKillReward(multiplier)` で適用）。バニラ戦利品は `LivingEntityMixin.cresora$doubleLunarEclipseLoot` がプレイヤー撃破時に loot テーブルをもう1回ロール。`moon_brick` ドロップ率は 2% → 8%（`MoonAltarService`）。モブの強さ・湧き量は通常どおり
- `/cresora moon set` は中文表示名ではなく英文 id を受け取る。使用可能 id: `new_moon`, `crescent_one`, `first_quarter`, `waxing_gibbous`, `near_full`, `full`, `full_after`, `wane_after`, `last_quarter`, `waning_crescent`, `old_moon`
- `/cresora moon set_special` は特殊月相を当夜に強制設定する。使用可能 id: `blood_moon`, `solar_eclipse`, `lunar_eclipse`, `death_moon`, `unknown`
- `/cresora moon stop_blood_moon` は当夜の血月だけを解除し、他の特殊月相記録は巻き込まない

重要仕様:

- mob rank 上限は `AdventureRankProgression` 側に依存
- field mob は近傍プレイヤー rank を基準に `-1〜+1` の振れ幅で割当（精鋭は `-1〜+3`、ボスは `+2〜+6`）
- HP は内部で cap と overflow 防御変換を使う
- **モブ視覚化（モデル拡大）**: `EntityAttributes.GENERIC_SCALE` を利用し、精鋭モブは **+18%**、フィールドボスは **+35%** に拡大される。スケール量は `FieldMobPackService.scaleBonus(entity)` が決定し、`applyMobScaling` 内で一括適用される。

### 7.1.2 BloodMoonService

ファイル:

- `BloodMoonService.kt`
- `BloodMoonHooks.kt`

責務:

- `血月` 夜の特殊ルールを管理する
- 血月中の bed interaction を捕捉し、二度押し確認で `血色战争` を開始する
- challenge 中の参加者、wave、休息時間、報酬 chest、mob griefing 抑制を管理する
- 参加者の wave clear buff を player damage / max HP pipeline へ提供する
- 血月中の自然敵対 mob spawn pressure を `MobEntitySpawnMixin` 経由で増やす

主 API:

- `init()`
- `clearTransientState(player)`
- `debugStop(server)`
- `playerDamageMultiplier(player)`
- `playerHealthMultiplier(player)`
- `damageMultiplier(attacker)`
- `maybeDuplicateNaturalSpawn(hostile, world, spawnReason)`

重要仕様:

- 血月夜は時間進行を 50% に落とす
- 血月中は bed interaction が通常睡眠ではなく challenge confirmation / join に使われる
- 通常の血月補正は hostile HP +20%、hostile damage +10%、自然 hostile spawn pressure +20%
- `血色战争` は開始時点で bed 周辺 50 block の player を参加者として確定し、参加者の途中離脱は bed 周辺へ戻す
- `血色战争` 開始時、選ばれた bed は special red bed に置き換わり、勝利・中断時に元の状態へ復元する。bed が破壊された敗北時のみ消失する
- wave は 20 回。各 wave 後、最終 wave 以外は 30 秒の現実時間休息を置く
- wave clear 判定は「その wave が実際に spawn 済みであること」を条件にし、開始直後の空状態を clear とみなさない
- 休息時間と休息 action bar countdown は `world.time` 基準で進む
- 各 wave clear 後、参加者に +10% damage / +20% max HP の累積 buff を与える
- battle 中、active wave hostile は可能な限り bed へ進軍し、bed 接触時に mob rank ベースの耐久 damage を与える
- battle bed の頭上には durability を示す floating HP label を常時表示し、無敵中は guarded 表示へ切り替える
- bed durability は 100% 開始。75 / 50 / 25% を割る瞬間、その wave の残り時間だけ無敵化し、次 wave 開始で無敵が解ける
- bed durability が 0% になると `血色战争` は即時敗北する
- player は battle bed を attack / break できない
- active `血色战争` が続く限り、overworld は dawn へ進まず夜へ巻き戻される
- 最終 wave は追加で elite `minecraft:wither` を 2 体生成する
- battle で spawn した hostile には探索しやすいよう `GLOWING` を付与する
- challenge 中は `DO_MOB_GRIEFING` を一時的に false にし、終了後に復元する
- challenge 中に一時上書きした participant の respawn point は終了時・中断時・respawn 後に元へ戻す
- active battle 中の block protection は battle bed とその近傍 TNT 着火阻止に限定する
- 参加者が全員 offline になった active battle は自動中断し、mob / gamerule / respawn override を掃除する
- `debugStop(server)` は active battle、pending confirmation、bed lock を解除し、active wave mob を despawn する
- 勝利後、参加者 1 人につき 1 個の chest を bed 近くに生成する。owner 以外は報酬を回収できない
- 報酬は 100k CSC、5 部位 ★5 聖遺物（各スロット `wand / hat / glasses / armor / boots` から登録済み全聖遺物プールよりランダム抽選、セットは部位ごとに独立）、確定の `blood_note`、50% で `lossless_crown` (★5)、75% で `blood_tear` (★4)、および現在 rank の必要 XP 上限 50% 分の adventure XP
- reward chest ownership / reward seed は persistent state へ保存し、server restart 後も復元できる



### 7.1.3 MoonAltarService

ファイル:

- [MoonAltarService.kt](file:///Users/hifumimizuhara/IdeaProjects/cresora-utilities-1.21.7/src/main/kotlin/hifumi/cresora/bloodmoon/MoonAltarService.kt)

責務:

- `moon_altar`（月相の祭壇）ブロックとの右クリックインタラクションの処理
- 祭壇への `blood_note`（血色の書き置き）投入による、翌夜の「血月（Blood Moon）」確定予約のスケジュール

主 API / 仕様:

- `UseBlockCallback` を経由してプレイヤーと `moon_altar` ブロックのやり取りを処理します。
- `tryDropMoonBrick(player, hostile)`: 敵対モブ（hostile）を倒した際に、2% の確率で祭壇クラフト素材 `moon_brick` をドロップさせます。
- 投入された予約状態は `MoonPhaseService` の永続状態 `forcedBloodMoonDay` に保存され、日付条件が満たされた際に優先的に血月が引き起こされます。

### 7.1.4 LeyLineService

ファイル:

- [LeyLineService.kt](file:///Users/hifumimizuhara/IdeaProjects/cresora-utilities-1.21.7/src/main/kotlin/hifumi/cresora/leyline/LeyLineService.kt)
- [LeyLineHooks.kt](file:///Users/hifumimizuhara/IdeaProjects/cresora-utilities-1.21.7/src/main/kotlin/hifumi/cresora/leyline/LeyLineHooks.kt)

責務:

- 7つの元素（日、月、火、水、木、金、土）の「地脉喷涌 (Ley Line Overflow)」イベントのライフサイクルおよび戦闘処理の管理
- プレイヤーインベントリ内へのキー返却（タイムアウト時・破壊時）の調整
- 挑戦領域の境界パーティクル描画と、プレイヤー不在による自動失敗判定（10秒）の制御
- 各元素に応じた難易度（T1-T4、冒険ランク依存）のモブウェーブ（精英怪含む）のスポーンとステータス補正の適用
- 討伐成功時の聖遗物、武器の欠片、およびクレジット・冒険ランクXP等の報酬生成とチェストUIの表示

主 API / 仕様:

- `placeLeyLine(world, pos, player, element)`: 地点に特定元素の地脉噴湧ブロックを配置し、50秒の有効期限タイマー（過ぎると自動でブロックが消滅し、配置者にキーが返却される）を開始します。同じ world/pos に pending 配置または active session がある場合は `false` を返して何も置換しないため、呼び出し元はキーを消費しません。
- `onBlockRemoved(world, pos)`: イベント開始前にブロックが破壊された場合、配置者にキーを返却します。
- `startEvent(player, pos, tier)`: pending 配置の所有者 UUID、ブロック種別、サーバーに保存された元素を検証してから、プレイヤーの冒険ランク要件を確認しイベントを開始します。元素は GUI / client 側値を受け取らず pending 記録から導出されます。配置者以外は開始できません。ブロックを消去し、10x10の戦闘セッション（`LeyLineEventSession`）を登録します。
- `tick(server)`: 有効期限タイマーおよび進行中の戦闘セッション（プレイヤーの境界内チェック、境界パーティクルの再生、各ウェーブのモンスター死亡監視、成功時の報酬配布等）を毎ティック処理します。
- `damageMultiplier(attacker)`: スポーンしたイベントモンスター（`mobRuntime`に登録されたUUID）の攻撃ダメージ倍率を取得します。
- **エリートモブの統合**: スポーンした精英怪（1ウェーブにつき1体）に対し、`FieldMobPackService.markExplicit(mob, true)` を呼び出してモデルサイズを 18% 拡大し、`AdventureRankService.applyMobScaling` でエリートステータス補正を適用します。また、全モブに `GLOWING`（発光）ステータス効果を付与します。
- **報酬分配**: 討伐成功時に `generateRewards` で報酬を算出し、配置者が挑戦領域内にいる場合にその配置者へアイテム/クレジット/XPを付与します。協力者は討伐を手伝えますが、報酬の受取人にはなりません。最終 wave 完了時に配置者が領域外なら、報酬を横取りさせず session を失敗として即時破棄します。最終的な報酬プレビューとして `ArtifactUiFlow.openDomainReward` を呼び出し、チェストUIを表示します。

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
- 限定 (SELECT) / 常設のガチャ実行
- pity / deep pity / arpeggio 管理
- LIMITED 用 PU★5 のプレイヤー単位選択管理（変更時は LIMITED ピティ群を全リセット）

主 API:

- `getProgress(player)` → `Progress(... , selectedFeaturedWeaponId)`
- `getCurrency(player, type)`
- `addCurrency(player, type, amount)`
- `setCurrency(player, type, amount)`
- `spendCurrency(player, type, amount)`
- `copyTo(old, new)`
- `currencyCount(player, banner)`
- `canPull(player, banner)` ※ LIMITED は PU 未選択時に false
- `pull(player, banner)` → `PullOutcome.Success | NotEnoughCurrency | FeaturedNotSelected | NoFiveStarWeaponsAvailable`
- `getSelectedFeaturedWeaponId(player)`
- `selectableFeaturedWeaponIds()` → WeaponContentRegistry 上の `craftedRarity == FIVE_STAR` を全て返す
- `setSelectedFeaturedWeaponId(player, weaponId)` → 変更時に limited pity / four-star pity / guaranteed / deep pity / arpeggio を全てリセット
- `getSpiritBondPoints(player, weaponId)` / `addSpiritBondPoints(player, weaponId, delta)` — 霊絆ポイントの取得・加算(ガチャ重複時に自動加算)
- `spiritBondStageForPoints(points: Int): Int` — ポイント→段階(1..6)のしきい値 `[0, 40, 100, 200, 350, 550]` を適用する純粋関数
- `spiritBondStage(player, weaponId): Int` — 現在のポイントから段階を直接取得

LIMITED ★5 抽選の概要:

- ★5 を引いたとき、`limitedFiveStarGuaranteed` または `random.nextDouble() < 0.5` の場合は PU を排出（確定枠を消費）
- それ以外は「登録済み全★5から PU を除外したリスト」から重み均等で抽選（すり抜け）
- PU 以外の★5が一つも無い場合は最終フォールバックとして PU を排出

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

- [WeaponUpgradeService.kt](file:///Users/hifumimizuhara/IdeaProjects/cresora-utilities-1.21.7/src/main/kotlin/hifumi/cresora/weapon/WeaponUpgradeService.kt)

責務:

- 武器のレベル強化およびスキル強化コストの計算
- 限界突破（精錬/突破）およびロール専用突破素材（T1 証 / T2 極意）の必要コスト計算、上限チェック

主 API:

- `baseUpgradeCost(definition, currentLevel)`: 武器基本レベルアップに必要な CSC コストの取得。
- `skillUpgradeCost(definition, currentLevel)`: 武器スキルレベルアップに必要な CSC コストの取得。
- `skillArtifactCost(definition, currentLevel)`: 武器スキルレベルアップに必要な聖遺物（アーティファクト）素材の取得。
- `breakthroughRoleMaterialCost(rarity: WeaponRarity, targetBreakthrough: Int): Int`: 限界突破時のロール専用素材（T1 証 / T2 極意）の必要数の計算（2星 = 1個, 3星 = 2個, 4星 = 4個, 5星 = 8個）。
- 突破/精錬レベルに応じた制限：
  - 精錬（突破）段階 `breakthrough` (0..2) に応じ、レベル上限およびスキル上限を設定：
    - 精錬0段階: レベル上限 = 最大レベルの50%、スキル上限 = 3
    - 精錬1段階: レベル上限 = 最大レベルの75%、スキル上限 = 7
    - 精錬2段階: レベル上限 = 最大レベルの100%、スキル上限 = 10

### 7.6.1 WeaponUpgradeScreen (武器パネルの一元化)

ファイル:

- [WeaponUpgradeScreen.kt](file:///Users/hifumimizuhara/IdeaProjects/cresora-utilities-1.21.7/src/client/kotlin/hifumi/cresora/weapon/WeaponUpgradeScreen.kt)
- [WeaponUpgradeScreenHandler.kt](file:///Users/hifumimizuhara/IdeaProjects/cresora-utilities-1.21.7/src/main/kotlin/hifumi/cresora/weapon/WeaponUpgradeScreenHandler.kt)

責務:

- 武器のステータス閲覧、強化（レベル・精錬）、スキル説明を1つのパネルにまとめた一元的なGUIの描画とクライアント側の動作管理。
- 画面サイズを横に拡張（`backgroundWidth` = 312）し、右側にタブ機能付きの情報パネル（ステータス、強化、スキル説明）を配置。
- 左側には武器スロット（スロット座標を `79, 26` に中央寄せ）と基本情報を配置した武器ショーケースをレンダリング。

1. **STATUS (ステータス)**: 攻撃力、会心率、全ダメージバフ、HPボーナス、攻撃速度、ダメージ属性、適合ロール、品級（レア度）を一覧表示。
2. **UPGRADE (強化・精錬)**: ベース強化（レベル・精錬突破）、スキル強化、および分解の各種操作ボタンと必要材料・CSCを表示。
3. **SKILL (スキル説明)**: 武器スキルの詳細説明テキスト（自動折り返し描画）および次レベルへのステータス・クールダウン（CT）プレビューを表示。
4. **ARTIFACTS (聖遺物)**: 武器に装着する聖遺物の管理画面。4つの聖遺物スロット（2x2グリッド）が表示され、プレイヤーは聖遺物を装備・着脱可能。

分解制限:
- 武器に何らかの聖遺物が装着されている場合、分解（Dismantle）アクションは無効化され、警告メッセージ `screen.cresora.weapon_upgrade.cannot_dismantle_has_artifacts` が表示されます。

### 7.7 WeaponCombatSupport

ファイル:

- [WeaponCombatSupport.kt](file:///Users/hifumimizuhara/IdeaProjects/cresora-utilities-1.21.7/src/main/kotlin/hifumi/cresora/weapon/WeaponCombatSupport.kt)

責務:

- 武器のステータス（攻撃力、速度、会心率、全ダメージボーナスなど）およびスキル値（シールド量、回復量など）の計算

主 API:

- `attackDamage(definition, data)`: 基礎攻撃力およびレベルスケーリングを考慮した実数値の計算。
- `attackDamageModifier(definition, data)`
- `attackSpeedModifier(definition)`
- `critRateBonusPercent(definition)`
- `allDamageBonusPercent(definition, data)`: 武器および精錬レベルによる全ダメージボーナス%の計算。
- `skillValueHearts(definition, data)` / `secondarySkillValueHearts(definition, data)`
- `skillValuePercent(definition, data)` / `secondarySkillValuePercent(definition, data)`
- `shieldHp(definition, data)`
- `healHp(definition, data)`
- `currentHpTrueDamageRatio(definition, data)`
- `healHp(player, amount)`: プレイヤーの体力を回復するヘルパー（マクロ展開用）。
- `grantShield(player, amount, durationTicks)`: プレイヤーにシールドを付与するヘルパー（マクロ展開用）。
- `applyStatusEffect(player, effectId, durationTicks, amplifier)`: プレイヤーにステータス効果を付与するヘルパー（マクロ展開用）。
- 精錬（突破）による戦闘属性ボーナスの適用：
  - 精錬0段階: 100% 属性
  - 精錬1段階: 115% 属性、会心率 +5%
  - 精錬2段階: 130% 属性、会心率 +10%、全ダメージボーナス +10%

### 7.8 EquipmentPlayerSupport

ファイル:

- `EquipmentPlayerSupport.kt`

責務:

- Trinkets 装備および主手に持っている武器の装着聖遺物から `EquipmentData` を集約
- セット効果解決
- プレイヤー総ステータス算出

主 API:

- `getEquippedEquipmentData(player)`
- `getActiveSetBonuses(player)`: Trinketsと主手の武器の装着聖遺物の両方からセット効果を解決し、独立したソースとして結合して返します（設計仕様：同一セットがTrinkets側に4個、武器側に4個装備された場合、それぞれ独立したpieceCountとして集計されて両方の系統のボーナスが同時に発動します。武器スロットは独立した追加装備枠として機能します）。
- `getActiveSetSummaries(player)`
- `getAggregatedStats(player)`: Trinketsの装備に加えて、主手に持っている武器に装着されている聖遺物の属性ボーナス（フラット値およびパーセント値）も加算してプレイヤーの総ステータスを算出します（設計仕様：ステータス計算において、「Trinkets」「武器装着の聖遺物」「セットボーナス」の3つのソースはすべて加算されて効果を発揮し、武器を持ち替えるなどのアクションによって能動的に適用されるセット効果やステータスが切り替わります）。

### 7.8.1 EquipmentEffectHookService

ファイル:

- `EquipmentEffectHookService.kt`

責務:

- 聖遺物の効果（セットボーナスのエフェクトフック）をトリガー条件（攻撃時、ダメージ受傷時、キック時等）に応じてディスパッチするサービスです。
- `getDisplayStacks(player, buffId, rawStacks)`: 表示上のスタック数を取得します（将来の拡張用）。
- `ArtifactSkillRegistry` は生成済み handler の `onTransientStateTick(player)` を毎 server tick に実行し、duration を持つ buff の期限処理を DSL の `on_tick` と独立して保証します。`on_tick` はゲーム効果用の通常 hook のままです。
- 同 registry は切断時に全 handler の `clearTransientState(playerId)` を呼び、オンライン集合外の state は `pruneTransientState(...)` で回収します。

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

- [WeaponSkillService.kt](file:///Users/hifumimizuhara/IdeaProjects/cresora-utilities-1.21.7/src/main/kotlin/hifumi/cresora/weapon/WeaponSkillService.kt)
- [WeaponSkillHandler.kt](file:///Users/hifumimizuhara/IdeaProjects/cresora-utilities-1.21.7/src/main/kotlin/hifumi/cresora/skill/WeaponSkillHandler.kt)
- [WeaponSkillRegistry.kt](file:///Users/hifumimizuhara/IdeaProjects/cresora-utilities-1.21.7/src/main/kotlin/hifumi/cresora/skill/WeaponSkillRegistry.kt)

設計思想:

以前は `WeaponSkillService` に全てのロジックが集中していましたが、現在は **Strategy パターン** に基づき、各スキルが独立したクラスに分割されています。

`WeaponSkillService` の責務:

- スキルの共通クールダウン管理 (BossBar 表示)
  - **重要**: クールダウンは「残りティック数」で管理されるようになり、デバフ等による動的な速度変更に対応しています。
- 汎用シールド (Shield) および一時ガード (Temporary Guard) のライフサイクル管理
  - `grantShield(player, amount, duration, weaponId)`: スクリプトから安全にシールドを付与するための標準インターフェース。`weaponId` が未指定（`null`）の場合、現在メインハンドに持っている武器 ID を自動で割り当てます。
- `applySoulBreak(target, stacks, durationTicks)`: 破魂（Soul Break）スタックを付与。物理耐性と防御力を減少させます。
- `getSoulBreakStacks(target)`: 現在の破魂スタック数を取得。
- `dealTrueDamage(player, target, amount)`: 確定ダメージ (True Damage) の一元管理。内部で `isDealingTrueDamage` スレッドローカルフラグを設定し、モブ防御力や物理/術耐性、および各種被ダメージスケーリングをバイパスしてダメージを適用。再帰的な攻撃・ダメージループを防止するため、`isTrueDamage` フラグをハンドラーの `onDamageDealt` メソッドへ伝播します。
- `WeaponSkillRegistry` を介した各ハンドラへのイベント（Tick, Damage 等）のディスパッチ。
- 武器のアクティブスロット制限（属性リーク防止）：
  - 武器の属性ボーナス（会心率、全ダメージボーナスなど）を取得する際、メインハンドに持っている武器（または `HotbarOverrideService` で一時的に指定された武器）と一致していることを厳密に検証するチェック (`lastHeldWeaponIdByPlayer` によるトラッキング）。
  - 武器切り替え（または武器を外した際）の検知時に、古い武器の一時的な戦闘バフを破棄するため、ハンドラーの `clearTransientState(player.uuid)` を自動的に起動します。
- `clearTransientState(player)`: 切断時や武器切り替え時に武器由来の一時状態を即時掃除します。
- 対象 entity を key にする mark / 破魂 / 一時無敵は online player の集合では prune しません。各 effect 自身の expiry tick で回収するため、敵に付与した持続効果の寿命はプレイヤー接続状態に左右されません。
- `getDisplayStacks(player, buffId, rawStacks)`: 表示上のスタック数を取得します。幼馴染4セット効果かつ「遥かなる少女の決意」を装備中、決意バフ（`ketsui`）に対して表示上 `+5` の加算オフセットを自動で計算して返します。

`WeaponSkillHandler` インターフェース:

各スキルクラスが実装する共通フックです。

- `activate(...)`: 右クリック発動時の主処理
- `onTick(server)` / `onPlayerTick(player)`: 継続効果の更新
- `onDamageAbsorbed(...)`: 被ダメージ吸収時の特殊処理
- `onDamageDealt(...)`: 攻撃命中時の追加効果（Dark/Lux 付与、追撃等）
- `onDamageTaken(...)`: 被弾時の反撃・軽減処理
- `clearTransientState(playerId: UUID)`: プレイヤー切断・武器切り替え時の一時状態（バフスタック等）の削除用。
- `pruneTransientState(activePlayerIds: Set<UUID>)`: 非アクティブになったプレイヤーのデータを安全に刈り込むためのデフォルト空メソッド。`WeaponSkillService` のティックループ内で毎ティック呼び出されます。

`WeaponSkillRegistry`:

- `effectId` (JSONの `skill.effectId` に対応) とハンドラを紐付けます。
- `CompiledWeaponSkillRegistry` (CWC 生成) により、DSL 定義されたスキルが自動的に登録されます。
- CWC 生成の `registerSubSkill(parentEffectId, subSkillEffectId)` により、メインスキルとサブスキルの親子関係も登録します。`WeaponSkillService` は装備中武器のメインスキルに加え、その配下のサブスキル `onTick` / `onPlayerTick` も毎ティック呼び出します。
- スキル ID から `WeaponDefinition` を逆引きする `getDefinition(id)` ユーティリティにより、文脈に応じたデータ処理が可能です。

`HotbarOverrideService`:

- `open_skill_menu(...)` 実行時に元ホットバー、親武器 ID、発動時点の `WeaponData`、許可されたサブスキル ID を保持します。
- `SubSkillItem` は保持された親武器文脈を使ってサブスキルを発動します。これにより、サブスキル内の攻撃力・レベル・レアリティ計算は `WeaponDefinition.DUMMY` ではなく実際の武器データを参照します。

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
- `resonantChordTutorialSteps` が定義された章の旋律共鳴チュートリアル進行（段階別の貸与武器切替、反応確認、再スポーン）。章 ID の特例は持たない

主 API:

- `hasActiveSession(player)`
- `damageTakenMultiplier(hostile)`
- `allowsTrueDamage(hostile)`
- `startSession(player, chapterId)`
- `tick(server)`
- `onPlayerDeath(player)`
- `onPlayerDisconnect(player)`
- `onResonantChordTriggered(player, reactionKey)`
- `handleDialogueAction(player, actionId)`

`startSession` で行うゲート判定（順序通り）:

1. session 重複（story / domain / masquerade）
2. `unlockRank` ≤ AdventureRank
3. `prerequisiteChapterId` クリア済み（`StoryProgressService.missingPrerequisite`）
4. `requiredRegionId` 未到達なら拒否（`StoryFlagService.hasFlag("region_visited_<id>")`、`RegionHooks` が設定）
5. `requiredSpiritId` + `requiredBondStage` — 当該武器の `ResonanceService.spiritBondStage(player, sid)` が指定段階以上であるかを判定
6. `linkedDomainId` があれば `DomainService.startLinkedStoryStage` に委譲
7. 武器配布スロット空きチェック

`StoryChapterDefinition` のオプショナルゲートフィールド:
- `requiredRegionId: String?` — 地域到達フラグ要求
- `requiredSpiritId: String?` — `spirit` ブロックを持つ武器の ID
- `requiredBondStage: Int = 0` — 1..6（`requiredSpiritId` が設定された場合のみ必須、未設定時は 0）

`StoryChapterDefinition` の旋律共鳴チュートリアルフィールド:
- `resonantChordTutorialSteps: List<StoryResonantChordTutorialStepDefinition>` — 空なら通常戦闘、非空なら全ステップの反応確認で戦闘完了
- 各ステップは `reactionKey`、`effectKey`、異なる2本の一時貸与 `weapons`（`removeOnExit = true`）を持つ
- `grantedWeapons` および `battleObjective` との併用は禁止。必要な空きスロット数は全ステップ中の最大貸与本数から算出する

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

### 7.13.1 StoryFlagService (Phase 4)

ファイル:

- `story/StoryFlagService.kt`
- `story/StoryFlagAccess.kt`（mixin interface）
- `java/.../mixin/ServerPlayerEntityMixin.java`（`cresora$storyFlagsRaw` を NBT に永続化）

責務:

- NPC 対話・章進行用の汎用ストーリーフラグ(`String` 集合)の保持と判定
- フラグは `ServerPlayerEntity` の NBT(`cresora_story_flags`)にカンマ区切り文字列として永続化され、`copyTo` で重生時に引き継がれる

主 API:

- `getFlags(player): Set<String>`
- `hasFlag(player, flagKey): Boolean`
- `setFlag(player, flagKey)` / `unsetFlag(player, flagKey)`
- `checkFlags(player, required: Map<String,Boolean>): Boolean` — 値 `true` は所持、`false` は非所持を要求
- `copyTo(oldPlayer, newPlayer)`

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
- `onPlayerLeave(player)`
- `restoreAfterRespawn(newPlayer)`
- `restoreAfterJoin(player)`
- `damageMultiplier(attacker)`
- `damageTakenMultiplier(target)`
- `adjustIncomingDamage(player, source, amount)`

復旧仕様:

- session 開始前に元インベントリ・offhand・選択スロット・帰還地点を `MasqueradeRecoveryPersistentState` に永続化してから、持込武器用インベントリへ切り替える。
- 切断・サーバー再起動後は JOIN で一度だけ元インベントリを復元し、生存中の run は元の帰還地点へ戻す。切断 callback では packet を送らない。
- 死亡時は復旧 record を respawn 用に印付け、`COPY_FROM` 後にインベントリだけを一度復元する。respawn 前に切断しても JOIN 復旧へ引き継がれる。
- domain world を取得できない teardown でも session / runtime mob map は必ず切り離す。

### 7.15 TreasureChestService

ファイル:

- [TreasureChestService.kt](file:///Users/hifumimizuhara/IdeaProjects/cresora-utilities-1.21.7/src/main/kotlin/hifumi/cresora/treasure/TreasureChestService.kt)
- [TreasureChestPersistentState.kt](file:///Users/hifumimizuhara/IdeaProjects/cresora-utilities-1.21.7/src/main/kotlin/hifumi/cresora/treasure/TreasureChestPersistentState.kt)

責務:

- プレイヤーが能動的に探索・発見を行う「共鳴探索 (Resonance Hunt)」システムの管理
- 共鳴探索宝箱のライフサイクル（スポーン、消滅、開封報酬）および守護者チャレンジの処理
- 宝箱の永続状態の NBT シリアライズ同期

主 API / 構成要素:

- **共鳴探索コンパス (Resonant Locator)**: 右クリックで使用する能動レーダーアイテム。安全なスポーンポイント（溶岩・奈落・岩盤等の回避）を探索し、共鳴宝箱を設置します。使用時に 12秒間のクールダウン を適用。
- **共鳴の宝箱 (Resonant Cache)**: クレソラ専用の新規カスタムブロック。出現から 10分間 の有効期限（TTL）を持ち、他プレイヤーからの窃盗を防止する厳格な所有者チェックを備えます。
- **ガイドトレイル演出**: 宝箱出現時にプレイヤーから宝箱の座標に向けて `END_ROD` 粒子の高精度ビームを描画。
- **守護者チャレンジ (Guardian Challenge) システム**:
  - 所有者プレイヤーが宝箱の周囲 6ブロック 以内に接近した瞬間に自動的に戦闘チャレンジが開始されます。
  - **難易度スケーリング**:
    - 3星チャレンジ: 通常モブ 3体
    - 4星チャレンジ: 通常モブ 2体 ＋ エリートモブ 1体
    - 5星チャレンジ: 通常モブ 2体 ＋ エリートモブ 2体（内1体はウィザースケルトン）
  - **ステータススケーリング**: `AdventureRankService.applyMobScaling` に基づき、出現させたプレイヤーの冒険ランクに応じて動的にスケール。エリートモブはさらに高い倍率（HP 2.5倍、防御 2.0倍）が適用されます。
  - **フロー表示**: 宝箱の頭上 (`y + 1.25` の座標) に `DisplayEntity.TextDisplayEntity` を生成し、リアルタイムに進捗を表示します。
  - **失敗およびリセット**: プレイヤーが死亡するか、宝箱から 32ブロック 以上離脱した場合にチャレンジは即座に失敗となり、守護者およびテキストは自動で消滅しリセットされます。
  - **報酬設計**:
    - 守護者討伐時: 討伐ごとに CSC（通常モブ 40 / エリートモブ 100）を獲得し、ゴールドナゲットをドロップ。
    - 宝箱開封時: チャレンジクリア後に 50% の確率で対応するレア度（3〜5星）の聖遺物（レベル0〜4）、または 50% の確率で対応するレア度の武器欠片をドロップします。

注記:

- 宝箱本体、所有者UUID、出現座標、有効期限（`expireTime`）などの情報は `TreasureChestPersistentState` を通じて完全に NBT へシリアライズされ、サーバー再起動後も復元されます。
- プレイヤー向け通知はすべて `message.cresora.treasure_chest.*` 翻訳キー（en_us / ja_jp / zh_cn / lzh）。色は § コードを訳文内に埋め込み。守護者数の頭上表示（`challenge_display`）も `Text.translatable` でクライアント側解決。

### 7.16 NaturalRegenService

ファイル:

- `NaturalRegenService.kt`

責務:

- rank 帯と combat 状態に基づく自然回復

主 API:

- `init()`
- `markCombat(player)`

### 7.17 CresoraDebuffService

ファイル:

- [CresoraDebuffService.kt](file:///Users/hifumimizuhara/IdeaProjects/cresora-utilities-1.21.7/src/main/kotlin/hifumi/cresora/debuff/CresoraDebuffService.kt)
- [CresoraDebuffHooks.kt](file:///Users/hifumimizuhara/IdeaProjects/cresora-utilities-1.21.7/src/main/kotlin/hifumi/cresora/debuff/CresoraDebuffHooks.kt)

責務:

- 旋律共鳴 (Resonant Chords) の音符付与・反応処理
- Crescendo マークによる対象指定ダメージ倍率管理
- `cleanupAll(server)` によるサーバーtickごとの期限切れマーク掃除

主 API:

- `triggerElementalSkill(player, noteId, radiusMeters)`: 武器スキル発動時にエリア内のホスティルへ音符を付与し、反応を発火
- `getCrescendoMultiplier(target, attacker)`: Crescendo の対象指定 1.5x をダメージ系 mixin から問い合わせる
- `cleanupAll(server)`: サーバーtickごとの期限切れ掃除（`CresoraDebuffHooks.init()` 経由）

その他の負マーク:

- **キュン死 (kyundeath)**: 会心（クリティカル）攻撃時に確率で付与され、対象の与ダメージを 20% 低下。モブのネームタグ末尾にハートマーク `[❤]` がレンダリングされる。(`WeaponSkillService` 管理)
- **嘆き (nageki)**: 装備者が「遥かなる少女の決意」をインベントリに所持していない時に付与される負のマーク。攻撃力-50%、防御力+100%、与える非確定ダメージの術ダメージ変換、10秒毎に50%の確率で3ハート（6.0 HP）の無期限シールド（重複不可）を獲得。(`WeaponSkillService` 管理)

### 旋律共鳴 (Resonant Chords)

武器スキルによって敵に対象の音符 (Note) が付与され、異なる種類の音符が重なることで共鳴反応 (Chord Reaction) が発生します。
音符の種類:
- **高音 (TREBLE)**: 素早く明るい旋律。風や雷、移動速度上昇効果等。
- **低音 (BASS)**: 重く暗い旋律。盾や防御、闇などの効果等。
- **旋律 (MELODY)**: 流れる水や雨、蘭などの効果等。
- **和声 (HARMONY)**: 共鳴や祝福、日の光などの効果等。

音符はデータ駆動で、`WeaponSkillDefinition.note` (オプショナル、値: `"treble" | "bass" | "melody" | "harmony"`) に宣言する。CWC `.cresora` の skill ブロック内で `note: "treble"` のように指定する。`note` が未指定または `radiusMeters` が 0 のスキルは付与をスキップする。付与対象は `HostileEntity` / `Monster` のみ (パッシブ Mob は除外)。期限管理はディメンション非依存で `MinecraftServer.overworld.time` に統一されている。

共鳴反応一覧:
- **不協和音 (Discord - 高音 + 低音)**: 敵に10.0の魔法ダメージを与え、移動速度低下IVを4秒間付与。さらに周囲5ブロック以内の全モブをノックバックし、3.0のダメージを与える。
- **急進曲 (Crescendo - 高音 + 旋律)**: 敵に6.0의 魔法ダメージを与え、発光を6秒間付与。この敵に対してプレイヤーが与えるダメージを6秒間1.5倍にする。
- **漸退曲 (Decrescendo - 低音 + 旋律)**: 敵に4.0の魔法ダメージを与え、弱体化IIIおよび移動速度低下IIを6秒間付与。
- **独奏狂詩曲 (Cadenza - 高音 + 和声)**: 敵に12.0の確定魔法ダメージを与え、現在装備中の武器スキルのクールダウンを総CTの20%分短縮する。
- **鎮魂曲 (Requiem - 低音 + 和声)**: 敵に5.0の魔法ダメージを与え、移動不能（移動速度低下VI）を3秒間付与。プレイヤーに衝撃吸収III（追加体力6.0）を6秒間付与。
- **小夜曲 (Serenade - 旋律 + 和声)**: 敵に4.0の魔法ダメージを与え、弱体化Iを5秒間付与。プレイヤーおよび周囲8ブロック以内の味方を8.0HP回復する。



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

### 7.19 CombatMobDisplayService

ファイル:

- `CombatMobDisplayService.kt`

責務:

- mob 頭上のランク / HP / 状態ラベル更新（`updateMobStatus`）
- 被弾位置に浮かぶダメージ数値表示（`TextDisplayEntity`）のスポーンと寿命管理
- プレイヤーへのアクションバー被弾 / 与ダメージ通知

主 API:

- `updateMobStatus(entity)`
- `showDamage(target, source, damage)` / `showDamage(target, damage)`
- `showTrueDamage(target, attacker, damage)`
- `showIncomingDamage(player, source, damage)`
- `showPlayerHitFeedback(attacker, damage, source)`
- `tick(world)` — `END_WORLD_TICK` から呼ばれ、表示を上昇させ 16 tick で discard
- `discardOrphanedIndicator(entity)` — `ENTITY_LOAD` から呼ばれ、チャンクデータから復元された残留ダメージ表示を discard する。該当時 `true`

重要仕様:

- ダメージ表示はコマンドタグ `cresora_damage_indicator` を持つ。追跡はメモリ上の `activeIndicators`（network entity id キー）のみで、エンティティ自体はチャンクに永続保存されるため、寿命 16 tick 内のチャンクアンロード / サーバー停止で孤児化する。`discardOrphanedIndicator` がロード時にタグ付き・未追跡の表示を回収する
- `activeIndicators` への登録は `spawnEntity` より前に行う必要がある（`ENTITY_LOAD` が `spawnEntity` 内で同期発火し、未追跡だと新規スポーンが孤児と誤判定されるため）
- タグ導入前に保存された残留表示は、テキスト内の翻訳キー prefix `combat.cresora.damage_type.short.` で識別して同様に回収（自己修复）

### 7.20 GuideService

ファイル:

- `GuideContentRegistry.kt`
- `GuideRecords.kt`
- `GuideProgressAccess.kt`
- `GuideService.kt`
- `GuideScreenHandler.kt`
- `GuideScreen.kt`
- `RecordsScreenHandler.kt` / `RecordsDisplayStackFactory.kt` / `RecordsScreen.kt`（記録ハブ GUI）

责务:

- 新手引导（冒险之证）任务系统。按章节管理引导任务，完成任务获得 CSC 及和弦奖励。
- **记录中枢（发现・羁绊・达成记录）**：在新手任务之外，`GuideService` 还作为只读聚合中枢，统计玩家的地区发现、灵之羁绊与达成进度。所有记录均由既有持久化数据（`StoryFlagService` 访问标记、`ResonanceService` 羁绊点数、`StoryProgressService` 通关记录）实时计算，**不引入新的持久化字段**；唯一新写入的是首次获得带灵武器时记录的 `spirit_met_<weaponId>` 故事旗标。
- 玩家进度的保存与加载（`ServerPlayerEntityMixin` 扩展 `GuideProgressAccess` 支持 NBT 序列化与数据在重生时的复制）。
- 事件触发与进度自动更新（击杀敌对怪物、共鸣抽卡、升级武器、通关剧情等）。
- UI 显示与奖励手动领取，通过主菜单（Slot 0）或者 UI Flow 开启引导界面；记录中枢另通过 `/cresora guide` 系列子命令以文本形式呈现。

任务 API:

- `GuideService.getPlayerChapter(player)`
- `GuideService.getTaskProgress(player, task)`
- `GuideService.claimTaskReward(player, taskId)`
- `GuideService.claimChapterReward(player, chapterIndex)`
- `GuideService.onKillHostile(player)`
- `GuideService.onResonancePull(player, count)`
- `GuideService.copyTo(oldPlayer, newPlayer)`

记录中枢 API（`GuideRecords.kt` 定义 `GuideRegionRecord` / `GuideSpiritBondRecord` / `GuideAchievementSummary` 数据类）:

- 发现：`GuideService.getRegionRecords(player)`（发现数/总数由 `record.discovered` 计数与 `list.size` 派生）
  - 复用 `RegionHooks.hasVisited(player, regionId)`（新公开的 `VISITED_FLAG_PREFIX` / `visitedFlag(id)` / `hasVisited(...)` 辅助）。
- 羁绊：`GuideService.getSpiritBondRecords(player)`（邂逅数由 `record.encountered` 计数派生）/ `hasMetSpirit(player, weaponId)` / `onSpiritObtained(player, weaponId)` / `spiritMetFlag(weaponId)`
  - 枚举 `WeaponContentRegistry.weaponDefinitions()` 中带 `spirit` 的武器；阶段与点数来自 `ResonanceService.spiritBondStage(...)` 与新公开的 `maxSpiritBondStage()` / `spiritBondStageThreshold(stage)`。`onSpiritObtained` 在 `ResonanceService.pull` 排出武器后调用，记录首次邂逅。
- 达成记录：`GuideService.getAchievementSummary(player)` 汇总引导章节、已领取任务、剧情通关、地区发现、灵邂逅与满羁绊数量。

命令（均要求玩家上下文）:

- `/cresora guide` — 概览（当前章节、地区与灵的发现进度）。
- `/cresora guide discovery` — 逐条列出各地区的发现/未发现状态（未发现地区以 `???` + 解锁冒险阶提示遮蔽）。
- `/cresora guide bond` — 列出各灵的羁绊阶段与距下一阶所需点数（未邂逅以 `???` 遮蔽）。
- `/cresora guide achievements` — 达成记录汇总。
- `/cresora_records` — 打开记录中枢 GUI（亦由冒险之证书页第 6 个「记录」标签触发）。

记录中枢 GUI（`RecordsScreen` / `RecordsScreenHandler`）:
- `ArtifactBookScreenBase` 现支持 6 个书页标签（见闻/演奏/讨伐/祈愿/珍品/**记录**）；第 6 个标签路由到 `cresora_records` 命令，对所有书页风格界面统一生效。`RecordsScreen.getActiveTab()` 返回 6。
- `RecordsScreenHandler` 为服务端权威：属性委托保存 `activeSubTab`(0=发现/1=羁绊/2=达成)、`page`、`totalPages`、左页比率（current/total）及 `selectedEntry`（当前选中的条目绝对索引，`NO_SELECTION=-1` 表示未选）；显示槽位（18 格中：0/1/2=子标签，6/7=上/下页，8=返回，9..14=最多 6 条目，15=详情面板）由 `GuideService` 聚合结果填充并通过同步槽位（CUSTOM_NAME+LORE）下发。子标签切换、翻页与条目选择全部经由 `onSlotClick` 在服务端处理（每页 `ENTRIES_PER_PAGE=6`）；切换子标签或翻页会清空选中状态。
- 选中条目时，槽 15 由 `RecordsDisplayStackFactory.regionDetail` / `spiritDetail` 填充长文详情栈，槽 9..14 中对应条目由 `selectedMarker` 标记「▶」。`RecordsScreen` 在左页将详情栈的 CUSTOM_NAME 作为标题、LORE 前两行作为状态(stage/next-or-max)、第 3 行作为当前阶段标题、第 4 行（`currentStageStoryKey`）经 `textRenderer.wrapLines` 折行渲染为物语段落——这是 `storyKey` 在仓库内首次被消费。未选中时左页退回显示聚合比率。
- `RecordsDisplayStackFactory` 将每条记录编码为带名称与 lore 的 `ItemStack`（地区：已发现 `filled_map` / 未发现 `map`+`???`；灵：已邂逅 `amethyst_shard`+阶段/点数 / 未邂逅 `gray_dye`+`???`；达成：固定 6 行汇总）。
- `RecordsScreen` 客户端仅转发点击（`clickSlot`）并渲染同步槽位/属性：左页为子标签按钮与（选中时）详情面板或（未选中时）比率，右页为条目卡片列表，底部为翻页与页码、返回按钮。
- `/cresora guide` 及其 `discovery` / `bond` / `achievements` 子命令以 `.requires { entity is ServerPlayerEntity }` 在注册树根部预检玩家上下文；`discovery` 与 `bond` 在记录为空时分别输出 `commands.cresora.guide.discovery.empty` / `commands.cresora.guide.bond.empty` 占位文本。
- `GuideService.getSpiritBondRecords` 由 `ResonanceService.spiritBondStageForPoints(points)` 推导阶段，避免对每件武器二次解析原始羁绊映射（2N→N 次解析）。

UI 与界面设计 (2026-06-15 重构):
- 冒险之证 UI 移除了玩家物品栏的注册，不再渲染玩家背包格子。
- 界面尺寸为 380x220 像素，使用自定义开书式双页布局。
- 左侧页展示“见闻进度”标题、圆形进度环（通过中点圆算法绘制的外环）及 3 段式分段绿色进度条，以及章节奖励大奖 Slot 和“领取奖励”按钮。
- 右侧页展示章节标题和 `<` `>` 导航按钮，以及 3 个垂直排布的任务卡片，每个卡片内置任务槽、标题、进度、奖励数值和“领取”按钮或“已完成/进行中”状态文本。
- 书本左侧有“见闻”（激活）、“委托”、“秘境”、“讨伐”的装饰性标签。书本右侧上方配有 “✕” 关闭按钮以退回到主菜单。

### 7.21 SpiritBondService / SpiritRenderer

ファイル:

- `weapon/SpiritBondService.kt` (server)
- `client/.../weapon/SpiritRenderer.kt` (client)

責務:

- 武器擬人化「霊」モデル(Phase 2)の同期と描画
- `SpiritBondService` は `END_SERVER_TICK` で 1 秒ごと(`time % 20`)に全オンラインプレイヤーのインベントリを走査し、`spirit` メタデータを持つ武器スタックに対し `ResonanceService.spiritBondStage(player, weaponId)` を当てて `WeaponData.spiritBondStage` を同期する
- `SpiritRenderer` は `END_CLIENT_TICK` で `time % 4` ごとに 48m 以内のプレイヤーを巡回し、メインハンドの武器が霊を持つ場合に頭上を周回するオーブパーティクル(`END_ROD` 基準、bond stage ≥ 3 で `ENCHANT`、≥ 5 で `GLOW` を重畳)をスポーンする。半径は bond stage で線形拡大

主 API:

- `SpiritBondService.init()` — `CreSoraUtilities.onInitialize` から登録
- `SpiritBondService.syncInventory(player)`
- `SpiritRenderer.init()` — `CreSoraUtilitiesClient.onInitializeClient` から登録

連動:

- ガチャ重複で `ResonanceService.addSpiritBondPoints` が呼ばれ、しきい値 `[0, 40, 100, 200, 350, 550]` で stage 1..6 が決まる
- 武器ツールチップ(`CresoraWeaponItem`)と強化画面 Stats タブ(`WeaponUpgradeScreen`)が `spiritBondStage` と現在の `bondStages[stage].titleKey` / `awakeningConditionKey` を表示する

### 7.22 StoryDialogueNetworking (Phase 0 / Phase 4)

ファイル:

- `story/StoryDialogueNetworking.kt`（サーバー権威）
- `client/.../story/StoryDialogueClient.kt` / `StoryDialogueScreen.kt`（クライアント UI）

責務:

- 章進行用ダイアログ（`DIALOGUE`）、開戦カウントダウン（`COUNTDOWN`）、NPC 分岐対話（`NPC_DIALOGUE`）の3モードを1つの `StoryDialogueStatePayload` で配信
- Phase 4 で `NPC_DIALOGUE` モードと `DialogueChoicePayload` / `DialogueExtraLists` を追加し、分岐選択肢・フラグ条件表示に対応。`hints` は `extras` に統合
- `NpcDialogueSession(treeId, playerUuid, currentNodeId)` をサーバー側で保持し、選択肢アクション(`actionId`)でノード遷移。`flagsToSet` でフラグ書き込み、`conditionFlags` / `requiredFlags` で `StoryFlagService` 経由のゲート判定を行う

主 API:

- `startNpcDialogue(player, treeId)` — ツリーのルートノードを配信してセッション開始
- `showSimpleDialogue(...)` / `showCountdown(...)` — 既存の章ダイアログ・カウントダウン
- `close(player)` — セッション破棄 + クライアントへ close パケット送信

### 7.23 NpcDialogueContentRegistry (Phase 4)

ファイル:

- `npc/NpcDialogueContentRegistry.kt`
- `data/cresora-utilities/cresora/npc_dialogue_content.json`

責務:

- NPC 対話ツリー(`NpcDialogueTree` → `NpcDialogueNode` → `NpcDialogueChoice`)の JSON ロード
- ノードは `speaker` / `body`(Text)・`choices`(分岐)・`flagsToSet`(フラグ書込)・`conditionFlags`(表示ゲート)・`nextNodeId`(自動遷移)を持つ
- 選択肢は `actionId`・`label`・`requiredFlags`(有効/無効判定)・`nextNodeId` を持つ
- JSON リソース不在時は `spirit_guide_intro` デモツリーをフォールバック生成

主 API:

- `init()` — `CreSoraUtilities.onInitialize` から呼ばれる
- `getTree(treeId)` / `getRootNode(treeId)` / `getNode(treeId, nodeId)`

### 7.24 SpiritGuideService / SpiritGuideEntity (Phase 4)

ファイル:

- `npc/SpiritGuideEntity.kt`（`PassiveEntity` ベース、`shouldSave=false` で非保存・重複防止）
- `client/.../npc/SpiritGuideRenderer.kt`（ビルボード板ポリ + `textures/entity/spirit_guide.png`）
- `world/SpiritGuideService.kt` — 案内人の着地・スポーン管理
- `world/CresoraWorldKeys.kt` / `world/PortalBlock.kt` — `cresora_world` 次元キーと双方向界門

責務:

- `SpiritGuideEntity` は `MOVEMENT_SPEED=0`・`KNOCKBACK_RESISTANCE=1` の立ち止まり AI、`DIALOGUE_TREE_ID` を `DataTracker` で同期
- `SpiritGuideService` は `END_SERVER_TICK`(5秒ごと)で `cresora_world` の着地エリア床/ポータルを差分復元し、案内人を1体だけ維持（重複・レガシー村人を `discard`）。チャンク未ロード時はスキップ
- `UseEntityCallback` で案内人右クリック → `StoryDialogueNetworking.startNpcDialogue`
- `PortalBlock` でバニラ OW ⇄ `cresora_world` の双方向遷移（着地時にチャンク先読み + 案内人確保）
- `MinecraftServerMixin` で界門ブロックのスポーン保護をバイパス

主 API:

- `SpiritGuideService.init()` — `CreSoraUtilities.onInitialize` から登録
- `SpiritGuideService.ensureLanding(world)` / `ensureGuide(world)` — `PortalBlock` からも呼ばれる

## 8. UI / Command API

### 8.1 ArtifactUiFlow

ファイル:

- `ArtifactUiFlow.kt`

画面遷移の単一窓口です。

主 API:

- `openMenu(player)` (Deprecated: replaced by openGuide)
- `openGuide(player)`
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
- `openResonanceFeaturedSelection(player)`
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
  - `moon`
    - `set <phase_id>`
    - `set_special <special_id>`
    - `clear_special`
    - `stop_blood_moon`
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

- **新しい `WeaponSkillDefinition.effectId`**: CWC (`.cresora`) を使用して新武器を定義するのが現在の標準です。手動での `WeaponSkillHandler` 実装は、CWC で表現不可能な特殊なシステム（複雑な状態異常同期など）に限定されます。
- 新しい動的 set effect hook の本体: `EquipmentEffectHookService` へのロジック追加が必要です。
- 特殊な GUI 挙動: 新しい Screen / ScreenHandler の作成が必要です。

### 9.3 追加時の基本手順

新しい装備セット:

1. `equipment_content.json` に slot / set / equipmentDefinitions を追加
2. 必要なら dropProfile / mobLoot も追加
3. モデル・テクスチャ・翻訳を追加

新しい武器:

1. `src/main/cresora/` に `*.cresora` ファイルを作成し、定義とスクリプトを記述
2. モデル・アイテム定義（`assets`側）・翻訳を追加
3. `./gradlew compileAssets` を実行してコードと JSON を生成
4. 必要なら `ResonanceContentRegistry` や秘境報酬にも接続

新しいストーリー:

1. `src/main/cresora/` に `.movement` 章定義を追加
2. 同ファイルの `translations` に全 locale 文言を追加
3. `./gradlew compileAssets` で `story_content.json` / `story_texts.json` を生成
4. 戦闘ギミックが既存の型付き定義で足りない場合は、CMC AST・JSON codec・runtime を同時に拡張する

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

## 12. Cresora Weapon Compiler (CWC)
`.cresora` ファイルを `src/main/cresora/` に配置することで、ビルド時に以下の要素が自動生成されます。

- **Kotlin コード**: `hifumi.cresora.skill.generated.*Skill` (ハンドラ本体)
- **JSON データ**: `cwc_weapon_content.json` (自動生成武器専用の定義ファイル)
- **登録処理**: `CompiledWeaponSkillRegistry` (レジストリへの自動登録)

CWC はコンパイルのたびに `build/generated/cresora/kotlin` と `build/generated/cresora/resources` を完全に再生成するため、常に最新のスクリプト内容が正確に反映されます。`src/main/resources` は読み取り専用の基底資産で、`lang` / `items` / `models/item` は生成オーバーレイへコピーしてから DSL 出力を重ねます。`processResources` は古い checked-in の生成 JSON・管理対象 assets を除外し、このオーバーレイだけをパッケージングします。既存の `weapon_content.json` は手動定義用として保持され、ゲーム実行時に自動的にマージされます。

#### CWC 2.0 強化点
- **自動インポート機能**: 生成される Kotlin スキルクラスに、Minecraft やクレソラ関連の常用クラス（`Text`, `LivingEntity`, `ServerWorld`, `ParticleTypes`, `WeaponSkillService`, `AdventureRankService` など）を自動的にインポート。これにより、`execute` ブロック内で完全修飾名を使わずに簡潔に記述できるようになりました。
- **ソース抽出 (`execute` ブロック)**: トークン再結合ではなく、元のソースコードから直接オフセットを切り出す方式を採用。これにより `as?`, `?.`, `!!` や改行、コメントのフォーマットが 100% 維持されます。
- **実行ラベル (`execute@run`)**: `execute` ブロックが `run execute@ { ... }` にラップされて生成されるため、スクリプト内で `return@execute` を使用した早期リターンが可能です。
- **独立した減衰時間 (`decay: independent`)**: バフ定義ブロックに `decay: independent` を指定可能。指定時、バフスタックごとに個別の失効時刻を記録する `expireTicks: MutableList<Long>` を持つ内部状態クラス `*State` が自動生成されます。
- **AOE 構文の修正**: `area_of_effect` 内での `ignite` 等のパラメータが 1.21.7 のレジストリ API に適合するように自動変換されます。
- **翻訳ブロック (`translations`)**: スクリプト内に `en_us`, `ja_jp`, `zh_cn`, `lzh` の各キーと値を直接記述可能。ビルド時に生成オーバーレイ側の `lang/*.json` へ自動的にマージされ、ソース資産は書き換えません。
- **動的メッセージ出力**: `execute` ブロック内で `Text.translatable` を用いて、スタック数や回復量などを動的に埋め込んだメッセージ演出を簡単に実装できるようになりました。
- **サブスキル文脈登録**: 生成 registry はサブスキルを単体 handler として登録するだけでなく、親スキルとの対応も登録します。これにより、サブスキル発動後の継続効果 (`on_player_tick`) が親武器を装備している間に正しく更新されます。

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
- **ホットバー展開**: `open_skill_menu` で指定したサブスキルがホットバー（0〜8スロット）に並びます。元のアイテムは自動的に退避され、スキル使用後または時間切れで復元されます。
- **サブスキル定義**: `sub_skill` ブロックでアイコンと挙動を定義します。これらは独立したスキルハンドラとして生成されます。
- **入力拒否**: unknown top-level token、unknown `sub_skill` field、unknown skill/buff field、未対応 block action は parse error になります。黙って読み飛ばす仕様ではありません。さらに、ハンドラ直下・`execute` ブロック直下・`area_of_effect` 内の**不明なコマンド名はコード生成時にエラー**になります（旧仕様のようにコメントとして握りつぶされません）。

#### 型付きアクションノード (2026-06-11)
ハンドラ直下・`area_of_effect` 内・`execute` ブロックのトップレベル文に現れる以下の 6 コマンドは、raw 文字列ではなく専用 AST ノードとしてパースされ、**パース時に検証**されます（CWC / CAC 共通）:

| コマンド | 検証内容 |
|---|---|
| `apply_mark(target, "id", duration)` | 引数 3 個、マーク ID は文字列リテラル、duration は時間値 |
| `grant_invulnerability(target, duration)` | 引数 2 個 |
| `add_buff("id"[, stacks])` | バフ ID は文字列リテラル、stacks は整数（省略時 1） |
| `start_cooldown([duration])` | 引数 0–1 個（省略時はスキルの cooldown） |
| `send_message("key"[, "color"])` | color は Minecraft `Formatting` の 16 色名（省略時 WHITE） |
| `apply_status_effect("ns:id", duration[, amplifier])` | effect ID は識別子形式、amplifier は整数（省略時 0） |

- **時間値 (`DurationValue`) のセマンティクス**: `30s` のような接尾辞付きは**秒**（生成時に ×20 で tick 換算）、bare 数値は **tick**、`skill_duration` はスキル定義の duration を実行時参照。従来 `30s` が 30 tick として生成されていた単位バグはこの仕組みで修正済み。
- artifact (CAC) 文脈では `start_cooldown` と `skill_duration` はコンパイルエラー。
- 任意の Kotlin 式（if 文内の命令呼び出し等）は従来どおり `ExpressionNode` + `InstructionMapping.expandAll` の raw パススルー。raw パス内の `Ns` は従来仕様（接尾辞除去のみ）のまま。
- `execute` ブロック内に予約文字 `·` (U+00B7) を含めるとコンパイルエラー（KotlinPoet の改行抑止に内部使用しているため）。

#### コンパイラのテスト (`compilerTest`)
- ソースセット: `src/compilerTest/kotlin/`（JUnit 5）。`GRADLE_USER_HOME=.gradle-user ./gradlew compilerTest --console=plain` で実行（`check` タスクにも接続済み）。
- カバレッジ: Lexer のエラー/トークン化、不正な `.cresora`/`.artifact` のパースエラー、`area_of_effect` のパース（radius デフォルト 5.0、nested アクション）、型付きノードの検証、`InstructionMapping` の展開規則、CWC/CAC の golden スナップショット。
- golden 期待値は `src/compilerTest/resources/golden/`。コンパイラの出力を意図的に変えた場合は、テスト失敗時に `build/golden-actual/` に書き出される実出力で期待値を更新する。

## 13. Artifact Compiler (CAC)
`.artifact` ファイルを `src/main/cresora/` に配置することで、ビルド時に以下の要素が自動生成されます。

- **Kotlin コード**: `hifumi.cresora.equipment.generated.ArtifactSkill_...` (ハンドラ本体、[ArtifactSkillHandler](file:///Users/hifumimizuhara/IdeaProjects/cresora-utilities-1.21.7/src/main/kotlin/hifumi/cresora/equipment/ArtifactSkillRegistry.kt) 実装)
- **JSON データ**: `cac_artifact_content.json` (自動生成聖遺物専用の定義ファイル)
- **登録処理**: `CompiledArtifactRegistry` (聖遺物スキルレジストリへの自動登録)

#### CAC 仕様と拡張機能
- **動的ステータス補正**: バフ等の過渡状態を表現するため、`ArtifactSkillHandler` が以下のメソッドを提供し、[EquipmentPlayerSupport.getAggregatedStats](file:///Users/hifumimizuhara/IdeaProjects/cresora-utilities-1.21.7/src/main/kotlin/hifumi/cresora/equipment/EquipmentPlayerSupport.kt) で自動的に集約されます。
  - `getAttackDamageScalar(player)`: 攻撃力倍率補正
  - `getArmorScalar(player)`: 防御力倍率補正
  - `getCritRateBonus(player)`: 会心率補正 (％)
  - `getCritDamageBonus(player)`: 会心ダメージ補正 (％)
- **独立した減衰時間 (`decay: independent`)**:
  - `buff` 定義ブロックに `decay: independent` を指定することで、バフスタックごとに個別の残り持続時間 (Ticks) をカウントし、失効させる仕組み。
  - コンパイルされる `State` 状態クラスがスタックごとの失効時刻を記録する `expireTicks: MutableList<Long>` を持ち、DSL の `on_tick` 有無にかかわらず `ArtifactSkillRegistry.onTransientStateTick` で毎ティック自動クレンジングされます。
- **共通組み込み命令**: `log()`, `apply_mark()`, `spawn_particles()`, `add_buff()`, `send_message()`, `apply_status_effect()`, `grant_invulnerability()` をサポート（型付きノード対象のコマンドは CWC と同じパース時検証が適用される。`start_cooldown` / `skill_duration` / `skill_value` は artifact では使用不可）。
- **生ソースコード抽出**: `execute` 内の複雑な Kotlin ロジックを、トークン再構成ではなく原始ソースコードから直接抽出することで構文エラーを防ぎます。

#### 条件付きセット効果 (`requires_weapon`)
- `set N { }` ブロック内に `requires_weapon: "weapon_id";` を指定すると、そのセットボーナスは指定武器がインベントリに存在する場合のみ発動します。
- `requires_weapon` が指定された場合、`stats { }` ブロックの値はJSONのフラットボーナスではなく、武器所持チェック付きのハンドラメソッド（`getCritDamageBonus`, `getAllDamageBonus` 等）として生成されます。
- パッシブハンドラ: イベントハンドラ（`on_attack_dealt` 等）がなくても、`requires_weapon` や `display_stack_bonus` のみのセット効果からハンドラクラスが自動生成されます。

#### 表示スタック数ボーナス (`display_stack_bonus`)
- `display_stack_bonus "buffId" { stacks: N; }` を `set N { }` ブロック内に記述すると、指定バフの表示スタック数に固定値を加算します。
- 集約は `EquipmentEffectHookService.getDisplayStacks()` → `WeaponSkillService.getDisplayStacks()` の経路で行われます。

#### 聖遺物・武器相互作用の例：「幼なじみ」と「遥かなる少女の決意」
- `osananajimi.artifact` の4セット効果は `requires_weapon: "harukanaru_shojo_no_ketsui"` で宣言されており、インベントリに該当武器が存在する場合のみ、会心ダメージ+10.0%、全ダメージ+10.0%、および「決意」バフの表示スタック+5層が適用されます。
- この効果は「決意」の通常の上限値100層の影響を受けず、最大105層まで重複・加算されます。また、会心時の「決意」獲得時のメッセージにおいても、この+5層分を加算したスタック数が表示されます。



## 14. Movement Compiler (CMC)
メインストーリーおよびクエスト会話・戦闘等のゲーム進行を定義する `.movement` ファイルからアセットをコンパイルします。

#### CMC 仕様と構成
- **DSL ファイル形式**: `.movement`
- **メタデータ**: チャプター ID、並び順 (`sort_order`)、タイトル、解放ランク、前提チャプター、秘境ID等を記述。
- **フェーズ設計**:
  - `phase pre_battle`: 戦闘前の会話フェーズをダイアログ (`dialogue`) で定義。
  - `phase battle`: クリア条件 (`battle_objective`)、出現する敵ウェーブ (`wave`)、敵の耐性・被ダメージなどのモディファイア (`modifiers`) を構造化。
    - `resonant_chord_tutorial`: `step "<reactionKey>"` ごとに `effect_key` とちょうど2本の `granted_weapons` を定義。生成された `resonantChordTutorialSteps` を `StoryService` が章 ID 非依存で実行する。
  - `phase post_battle`: 戦闘後の会話フェーズを定義。
  - `rewards`: CSC や共鳴通貨（代理弦など）のクリア報酬を定義。
  - `translations`: 各ロケール (`ja_jp`, `en_us`, `zh_cn`, `lzh`) ごとに、作中で参照される `speaker_id` や `text_id` の対訳テキストを直接記述。
- **生成アセット**:
  - **JSON (Content)**: `build/generated/cresora/resources/data/cresora-utilities/cresora/story_content.json` にシリアライズ。
  - **JSON (Translations)**: `build/generated/cresora/resources/data/cresora-utilities/cresora/story_texts.json` に完全生成。DSL を削除した場合も旧 chapter/text が残りません。
- **ビルドタスク**: `GRADLE_USER_HOME=.gradle-user ./gradlew compileAssets` で CWC/CAC とともに自動実行されます。
