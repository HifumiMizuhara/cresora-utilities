# CreSora Utilities API Document

最終更新: 2026-05-10 (Blood Moon stabilization sweep 反映)

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
- `attackCurve`
- `customModelData` (Optional)
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

注意:

- 各 rarity pool は content load 時に検証されます。
- 空 pool、負の weight、pool と rarity の不一致、限定 banner の不正な featured 参照は reject されます。

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
- `syncWeaponData(stack, data)`: `WEAPON_DATA` を同期し、`customModelData` が定義されている場合は `CUSTOM_MODEL_DATA` コンポーネントを適用します。
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

重要仕様:

- 通常月相は `朔 -> 既朔 -> 上弦 -> 逾弦 -> 几望 -> 望 -> 既望 -> 退望 -> 下弦 -> 残月 -> 晦` の 11 日周期
- 特殊月相は互斥抽選で、命中時はその夜の通常増強を無効化
- `血月` は 11 日周期内で最低 1 回発生するよう保底される
- 当夜メッセージは `夜晚降临,今晚是...` 形式で送信
- `血月` の詳細効果は `BloodMoonService` が実装済み。他の特殊月相は接続口のみ公開
- `/cresora moon set` は中文表示名ではなく英文 id を受け取る。使用可能 id: `new_moon`, `crescent_one`, `first_quarter`, `waxing_gibbous`, `near_full`, `full`, `full_after`, `wane_after`, `last_quarter`, `waning_crescent`, `old_moon`
- `/cresora moon set_special` は特殊月相を当夜に強制設定する。使用可能 id: `blood_moon`, `solar_eclipse`, `lunar_eclipse`, `death_moon`, `unknown`
- `/cresora moon stop_blood_moon` は当夜の血月だけを解除し、他の特殊月相記録は巻き込まない

重要仕様:

- mob rank 上限は `AdventureRankProgression` 側に依存
- field mob は近傍プレイヤー rank を基準に `±5` の振れ幅で割当
- HP は内部で cap と overflow 防御変換を使う
- **エリートモブの視覚化**: `EntityAttributes.GENERIC_SCALE` を利用し、エリートモブのモデルサイズとヒットボックスを **18% 拡大** しています。

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
- 報酬は 100k CSC、5-star artifact set、`blood_note`、20% `lossless_crown`、50% `blood_tear`、および現在 rank の必要 XP 上限 50% 分の adventure XP
- reward chest ownership / reward seed は persistent state へ保存し、server restart 後も復元できる

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
  - `grantShield(player, amount, duration, weaponId)`: スクリプトから安全にシールドを付与するための標準インターフェース。
- `applySoulBreak(target, stacks, durationTicks)`: 破魂（Soul Break）スタックを付与。物理耐性と防御力を減少させます。
- `getSoulBreakStacks(target)`: 現在の破魂スタック数を取得。
- `WeaponSkillRegistry` を介した各ハンドラへのイベント（Tick, Damage 等）のディスパッチ
  - いまは「装備中の武器 + そのサブスキル」だけを走査します。全登録ハンドラを毎 tick 回す設計はやめました。
- `clearTransientState(player)`: 切断時に武器由来の一時状態を掃除します。

`WeaponSkillHandler` インターフェース:

各スキルクラスが実装する共通フックです。

- `activate(...)`: 右クリック発動時の主処理
- `onTick(server)` / `onPlayerTick(player)`: 継続効果の更新
- `onDamageAbsorbed(...)`: 被ダメージ吸収時の特殊処理
- `onDamageDealt(...)`: 攻撃命中時の追加効果（Dark/Lux 付与、追撃等）
- `onDamageTaken(...)`: 被弾時の反撃・軽減処理

`WeaponSkillRegistry`:

- `effectId` (JSONの `skill.effectId` に対応) とハンドラを紐付けます。
- `CompiledWeaponSkillRegistry` (CWC 生成) により、DSL 定義されたスキルが自動的に登録されます。
- CWC 生成の `registerSubSkill(parentEffectId, subSkillEffectId)` により、メインスキルとサブスキルの親子関係も登録します。`WeaponSkillService` は装備中武器のメインスキルに加え、その配下のサブスキル `onTick` / `onPlayerTick` も毎 tick 呼びます。
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
- `clearTransientState(player)`: pending respawn snapshot を切断時に消します。
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

- 宝箱本体は persistent state で残ります。
- 一方でプレイヤーごとの再出現スケジュールは transient state なので、切断時に破棄されます。

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
- `clearTransientState(player)` による切断時のデバフ破棄

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
3. `./gradlew generateWeapons` を実行してコードと JSON を生成
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

## 12. Cresora Weapon Compiler (CWC)
`.cresora` ファイルを `src/main/cresora/` に配置することで、ビルド時に以下の要素が自動生成されます。

- **Kotlin コード**: `hifumi.cresora.skill.generated.*Skill` (ハンドラ本体)
- **JSON データ**: `cwc_weapon_content.json` (自動生成武器専用の定義ファイル)
- **登録処理**: `CompiledWeaponSkillRegistry` (レジストリへの自動登録)

CWC はコンパイルのたびに出力先（`generated` パッケージおよび `cwc_weapon_content.json`）を完全にクリアしてから再生成するため、常に最新のスクリプト内容が正確に反映されます。既存の `weapon_content.json` は手動定義用として保持され、ゲーム実行時に自動的にマージされます。

#### CWC 2.0 強化点 (2026-04-12 反映)
- **ソース抽出 (`execute` ブロック)**: トークン再結合ではなく、元のソースコードから直接オフセットを切り出す方式を採用。これにより `as?`, `?.`, `!!` や改行、コメントのフォーマットが 100% 維持されます。
- **実行ラベル (`execute@run`)**: `execute` ブロックが `run execute@ { ... }` にラップされて生成されるため、スクリプト内で `return@execute` を使用した早期リターンが可能です。
- **AOE 構文の修正**: `area_of_effect` 内での `ignite` 等のパラメータが 1.21.7 のレジストリ API に適合するように自動変換されます。
- **翻訳ブロック (`translations`)**: スクリプト内に `en_us`, `ja_jp`, `zh_cn`, `lzh` の各キーと値を直接記述可能。ビルド時に `lang/*.json` へ自動的にマージされるため、外部ファイルの編集が不要になりました。
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
- **入力拒否**: unknown top-level token、unknown `sub_skill` field、unknown skill/buff field、未対応 block action は parse error になります。黙って読み飛ばす仕様ではありません。
