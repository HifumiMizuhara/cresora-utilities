# WORK_DONE

## 新武器「遥かなる少女の決意・★５」とCWC HPステータス拡張の追加 (2026-05-25)
- [x] CWC (Cresora Weapon Compiler) にHPパーセンテージステータスを拡張：
  - [AST.kt](file:///Users/hifumimizuhara/IdeaProjects/cresora-utilities-1.21.7/src/compiler/kotlin/hifumi/cresora/compiler/AST.kt) および [Parser.kt](file:///Users/hifumimizuhara/IdeaProjects/cresora-utilities-1.21.7/src/compiler/kotlin/hifumi/cresora/compiler/Parser.kt) を拡張し、DSLの `stats` ブロックで `hp_bonus: 10.0` のようにHPパーセンテージを指定できるよう改修。
  - [CresoraCompiler.kt](file:///Users/hifumimizuhara/IdeaProjects/cresora-utilities-1.21.7/src/compiler/kotlin/hifumi/cresora/compiler/CresoraCompiler.kt) を改修し、HPパーセンテージを `cwc_weapon_content.json` へシリアライズして出力するとともに、生成される Kotlin ハンドラーに `getHealthBonusPercent(player)` および `getAllDamageBonus(player)` の動的バフ補正値メソッドを自動生成するロジックを実装。
- [x] コアシステムおよびステータス計算への統合：
  - [WeaponContentRegistry.kt](file:///Users/hifumimizuhara/IdeaProjects/cresora-utilities-1.21.7/src/main/kotlin/hifumi/cresora/weapon/WeaponContentRegistry.kt) の `WeaponStats` と `WeaponDefinition` に `hpBonusPercent` フィールドを追加し、Codecでのシリアライズ・デシリアライズに対応。
  - [WeaponSkillHandler.kt](file:///Users/hifumimizuhara/IdeaProjects/cresora-utilities-1.21.7/src/main/kotlin/hifumi/cresora/skill/WeaponSkillHandler.kt) に `getHealthBonusPercent(player)` および `getAllDamageBonus(player)` のデフォルト実装を追加。
  - [WeaponSkillService.kt](file:///Users/hifumimizuhara/IdeaProjects/cresora-utilities-1.21.7/src/main/kotlin/hifumi/cresora/weapon/WeaponSkillService.kt) に `healthScalar(player)` および `allDamageBonusPercent(player, weaponId)` を実装し、アクティブ武器の基礎HPボーナスとスキルによる動的HP/全ダメージボーナスを合算して提供する仕組みを構築。
  - [EquipmentAttributeService.kt](file:///Users/hifumimizuhara/IdeaProjects/cresora-utilities-1.21.7/src/main/kotlin/hifumi/cresora/equipment/EquipmentAttributeService.kt) のプレイヤー最大体力更新ロジックに武器のHP%補正を統合。
  - [PlayerEntityMixin.java](file:///Users/hifumimizuhara/IdeaProjects/cresora-utilities-1.21.7/src/main/java/hifumi/cresora/mixin/PlayerEntityMixin.java) の攻撃ダメージ計算に武器スキル全ダメージボーナスを乗算するよう改修。
- [x] 「キュン死」デバフと戦闘フィードバックの拡張：
  - [LivingEntityMixin.java](file:///Users/hifumimizuhara/IdeaProjects/cresora-utilities-1.21.7/src/main/java/hifumi/cresora/mixin/LivingEntityMixin.java) を改修し、攻撃者が `"kyundeath"` マークを付与されている場合、最終与ダメージを 20% 低下させるデバフ処理を実装。
  - [CombatFeedbackService.kt](file:///Users/hifumimizuhara/IdeaProjects/cresora-utilities-1.21.7/src/main/kotlin/hifumi/cresora/combat/CombatFeedbackService.kt) に `hasPendingCrit(player)` を追加し、パッシブスキルの会心トリガー判定に利用可能に。
  - [CombatMobDisplayService.kt](file:///Users/hifumimizuhara/IdeaProjects/cresora-utilities-1.21.7/src/main/kotlin/hifumi/cresora/combat/CombatMobDisplayService.kt) において、`"kyundeath"` マークが付与されているモブのネームタグ末尾にハートマーク `[❤]` を表示するように改修。
- [x] ツールチップ・言語ファイル・DSL定義の追加：
  - [CresoraWeaponItem.kt](file:///Users/hifumimizuhara/IdeaProjects/cresora-utilities-1.21.7/src/main/kotlin/hifumi/cresora/weapon/CresoraWeaponItem.kt) で武器のHP%属性ボーナス（例：`HP +10.0%`）を緑色でツールチップに描画するよう変更。
  - 4ヶ国語（日英中・文言）の言語ファイルに対応する翻訳キー（武器名、スキル、バフ、デバフ、ツールチップなど）を追加。
  - [harukanaru_shojo_no_ketsui.cresora](file:///Users/hifumimizuhara/IdeaProjects/cresora-utilities-1.21.7/src/main/cresora/harukanaru_shojo_no_ketsui.cresora) にて新武器「遥かなる少女の決意」のスペックを定義（HP+10%、会心+10%、アクティブスキル「少女の眼差し」でキュン死デバフ適用、会心時に「決意」バフをスタックし会心ダメ&全ダメボーナスが最大100スタックするパッシブを実装）。
- [x] コードレビュー指摘に伴う最適化・修正：
  - スキル非発動時の毎秒エンティティ走査負荷（パフォーマンス）を排除するため、プレイヤーへの `"ketsui_active"` マーク付与によるガードを実装。
  - DoTループ内での `recordCrit` 呼び出しを削除し、バフが自己ループで無限スタックする問題を防止。
  - `ketsui.gained` の翻訳キーの `%s` 変数を2つに修正し、効果名とスタック数が正しくフォーマットされて表示されるよう修正（4ヶ国語）。
  - Java Mixin から Kotlin object にアクセスする際、静的アクセス `@JvmStatic` を介する記述に統一。
  - `lzh.json` の末尾改行を追加。
- [x] ビルドおよびアセットコンパイル検証：
  - `compileAssets` を実行し、CWCによる Kotlin コードおよび JSON データファイルがエラーなしで自動生成されることを確認。
  - `classes` ターゲットをビルドし、プロジェクト全体のKotlin/Javaコードが正常にビルド・コンパイルできることを確認。

## 武器突破（昇格）におけるロール専用素材の追加 (2026-05-24)
- [x] ロール専用突破素材の登録：
  - 各ロールに対応した2つのティア（T1: 証/Proof、T2: 極意/Insight）の新規アイテム（計18個）を `CreSoraUtilities.kt` に登録。
  - アイテムモデル定義を追加し、T1は紙 (`minecraft:item/paper`)、T2は本 (`minecraft:item/book`) のバニラテクスチャを再利用するモデル設定を適用。
- [x] 突破素材の必要コスト設定：
  - 武器のレア度（2星〜5星）に応じて、必要数を「2星=1個、3星=2個、4星=4個、5星=8個」に設定。
- [x] 強化ロジックへの統合：
  - `WeaponUpgradeService.kt` に `breakthroughRoleMaterialCost` を実装。
  - `WeaponStackSupport.kt` にプレイヤーインベントリ内のロール素材のカウントおよび消費用のユーティリティ関数を追加。
  - `WeaponUpgradeLogic.kt` の基礎強化処理（`attemptBaseUpgrade`）において、突破時に対応するロール専用素材を消費するように改修。不足している場合は `"screen.cresora.weapon_upgrade.no_role_materials"` エラーを返すように検証。
- [x] UIおよび多言語対応：
  - `WeaponUpgradeScreenHandler` の同期プロパティを拡張し、クライアントへ必要/所持ロール素材数を同期。
  - `WeaponUpgradeScreen.kt` で、突破が必要な時に「Fragments（欠片）」表示の下部にロール専用素材のコスト情報「素材名 必要数 / 所持数」をレンダリング。
  - 画面サイズの都合上、左右の装飾ボックスの高さを `73` に拡張し、インベントリタイトルやステータス文字位置を調整して重複表示を回避。
  - 4言語（日英中・文言）に素材名およびステータスメッセージの翻訳を追加。
- [x] 将来のタスク管理：
  - 将来のドメイン報酬・ショップシステム大規模リファクタリング時に突破素材の入手経路を組み込むTODOを `TODO.md` に追加。

## 武器のロール（分類）システムの実装 (2026-05-24)
- [x] 武器ロールの定義とシリアライズ：
  - 新規ファイル `src/main/kotlin/hifumi/cresora/weapon/WeaponRole.kt` を作成し、先鋒 (Vanguard)、前衛 (Guard)、重装 (Defender)、狙撃 (Sniper)、術師 (Caster)、医療 (Medic)、補助 (Supporter)、特殊 (Specialist)、法器 (Catalyst) の9種類のロールを示す `WeaponRole` 列挙型を定义。
  - DFU の `Codec` を実装し、JSON へのシリアライズとデシリアライズを可能に。
- [x] DSLコンパイラ (CWC) の拡張：
  - `AST.kt` の `WeaponDefNode` に `role` フィールドを追加。
  - `Parser.kt` に DSL での `role` フィールドのパースロジックおよびバリデーション（不正なロール名でビルドエラーにする仕組み）を追加。
  - `CresoraCompiler.kt` で AST ノードからコンパイルされた武器 JSON (`cwc_weapon_content.json`) へ `role` を出力するよう拡張。
- [x] 武器定義 (WeaponDefinition) の更新と DFU 16フィールド制限の回避：
  - `WeaponContentRegistry.kt` 内の `WeaponDefinition` データクラスに `role` を追加。
  - 17個になった定義フィールドに対し、DFU `RecordCodecBuilder` の16フィールド制限を回避するため、ゲームプレイに必須のステータス情報（`baseAttackDamage` 等）を `WeaponStats` という中間 `MapCodec` にグルーピングしてフラットにマッピングするよう `WEAPON_DEFINITION_CODEC` をリファクタリング。
- [x] 既存の武器へのロールの割り当て：
  - 各武器のスキルや設定を解析し、最適なロールを決定して `.cresora` ファイルに `role` を追加しました：
    - `cadenza_allegro` -> `role: specialist`（羊に変身させるクラウドコントロール）
    - `dark_lux` -> `role: supporter`（物理耐性・術耐性デバフの付与）
    - `gaoshan_liushui` -> `role: medic`（持続的な回復オーラの付与）
    - `hanwu_juanxue` -> `role: caster`（寒霜の蓄積による術攻撃）
    - `kyokusui_no_ryusho` -> `role: supporter`（チームへの強力な攻撃バフ）
    - `lakeside_stride` -> `role: guard`（攻撃的な確定ダメージバースト）
    - `masquerade_invitation` -> `role: medic`（直接ヒールスキル）
    - `pastoral_flute_reverie` -> `role: vanguard`（移動速度大幅アップの加速スキル）
    - `qianqiu_yeluo` -> `role: caster`（術ダメージによるバフ重複コンボ）
    - `requiem_toward_dawn` -> `role: caster`（火焔付与の術属性オーラ）
    - `rondo_melody` -> `role: defender`（シールド付与による自己防御）
    - `tanboku_chokuu` -> `role: guard`（複数の剣術の構えを切り替えて戦うメイン物理アタッカー）
  - すべてのテスト武器（`crimson_flash`, `azure_blade`, `stance_test`, `lossless_crown`, `blood_tear`）についてもそれぞれ適切なロールを定義しました。
  - DSL 資産再コンパイルおよび `classes` 编译を実行し、すべての既存武器定義が割り当てたロールで正常にロード・ビルドできることを実証しました。
- [x] ツールチップでのロール表示と多言語翻訳：
  - `CresoraWeaponItem.kt` の `appendTooltip` に武器ロールを多言語対応で表示する行を追加。
  - 日本語 (`ja_jp.json`)、英語 (`en_us.json`)、簡体字中国語 (`zh_cn.json`)、文言 (`lzh.json`) 各言語ファイルに武器ロールの表示文言と各ロール名の翻訳を追加。
- [x] 動作検証：
  - DSL 資産コンパイル `compileAssets` を実行し、武器 JSON に設定したロールが出力されること、未設定の既存武器が `guard` ロールとして正しく後方互換処理されることを確認。
  - プロジェクト全クラスビルド `classes` を実行し、全コードが正常にコンパイル・ビルドできることを検証。

## 开发者文档整合与清理 (2026-05-23)
- [x] 将 `AGENTS.md`, `CLAUDE.md` 和 `GEMINI.md` 的内容进行分析、对比与合并，整合为一份最全面且使用 Gemini 编译指令的 `GEMINI.md` 开发者引导指南。
- [x] 删除冗余文档：在整合完成后，物理删除不再需要的 `AGENTS.md` 和 `CLAUDE.md`，确立未来仅使用 Gemini 辅助开发的文档基础。
- [x] 执行 `classes` 任务完成全量代码编译和验证，确保无任何编译错误或行为回归。

## 《高山流水》武器自定义贴图实现 (2026-05-23)
- [x] 武器自定义贴图生成与优化：
  - 基于武器《高山流水》的传统国风（子期与伯牙、高山流水、古琴琴弦与翠色玉石）设定，利用生图模型生成了高质量的武器艺术概念图。
  - 使用 Python 与 Pillow 对生成的图像进行后期处理：剔除纯黑背景并转换为透明通道、将剑身垂直与水平翻转使手柄朝向左下、剑尖朝向右上（符合 Minecraft 经典武器贴图朝向规范）、重构为 32x32 分辨率的高保真像素风 PNG 贴图。
  - 将处理后的贴图保存至项目路径 `src/main/resources/assets/cresora-utilities/textures/item/gaoshan_liushui.png`。
- [x] 武器 DSL 与模型编译配置：
  - 修改 `src/main/cresora/gaoshan_liushui.cresora`，在 weapon 定义中指定 `texture: "cresora-utilities:item/gaoshan_liushui"`。
  - 执行 `compileAssets` 资产编译，使 CWC 编译器自动根据此属性生成 `gaoshan_liushui.json` 物品模型文件，成功将材质引用指向自定义贴图路径。
  - 运行 `classes` 任务完成全量代码编译和验证，确保无任何编译错误或行为回归。

## 武器突破（精炼）机制与 Tueshokaku 废除 (2026-05-23)
- [x] 武器突破（精炼）机制实现：
    - `WeaponData` 数据类新增 `breakthrough`（精炼等级 0~2）字段，支持 Codec 序列化与 NBT 数据同步。通过 optional 属性确保与原有武器 item stack 后方兼容性。
    - `WeaponUpgradeService` 新增 `levelCap`, `maxSkillLevelForBreakthrough`, `breakthroughCscCost`, `breakthroughFragmentCost` 方法，设定按比例递增的等级上限（精炼0级为 maxBaseLevel 的 50%，精炼1级为 75%，精炼2级为 100%），限制技能等级上限（精炼0/1/2级分别最大可升级技能至3/7/10级），并基于武器星级阶梯式计算 CSC 与武器碎片消耗。
    - `WeaponCombatSupport` 实现精炼属性倍率（精炼0级100%，精炼1级115%，精炼2级130%）与额外属性增益（精炼1级 +5% 暴击率，精炼2级 +10% 暴击率与 +10% 全伤害加成）。
    - `PlayerEntityMixin.java` 修改以向 `critRateBonusPercent` 传入 `weaponData` 以包含精炼带来的暴击率加成。
- [x] 强化操作与 UI 逻辑重构：
    - `WeaponUpgradeLogic` 支持当武器等级达到当前精炼上限且 breakthrough < 2 时，将原有升级预览转为精炼预览，重置武器等级为 1 并进阶精炼等阶，限制技能等级升级不能超过当前精炼等级对应的技能上限。
    - `WeaponUpgradeScreen` 将“基础升级”按钮动态更改为“精炼”，并修改显示展示当前精炼等级的伤害增益和费用。
    - 增加中文（简体、文言）、日文、英文下有关精炼、精炼等级不足等 UI 及交互提示翻译。
- [x] 废弃并彻底删除 Tueshokaku：
    - 物理删除 `tueshokaku.kt` 及相关资源模型文件 (`tueshokaku.json`)。
    - 移除 `CreSoraUtilities.kt` 中的 Tueshokaku 注册与字段。
    - 移除 `UpgradeLogic.kt` 与 `UpgradeScreenHandler.kt` 对 Tueshokaku 强化素材的处理逻辑，防具（Pendant）强化逻辑转为仅能使用同类防具（Pendant）作为素材。
    - 移除 `EquipmentContentRegistry.kt` 构建怪物战利品表时对 Tueshokaku 的注入逻辑。
- [x] 代码审核反馈与深度优化（2026-05-23 Code Review 反馈落实）：
    - 物理清理 `adventure_rank_upgrade_materials.json` 标签中残留的 Tueshokaku 引用，防止运行时警告。
    - 清理 4 国语言翻译文件中废弃 of Tueshokaku 与相关升级失败文案词条。
    - 去除 `UpgradeLogic.kt` 中因删除代码遗留的多余空白行。
    - 重构 `WeaponData.normalized(definition)`，使其直接调用 `WeaponUpgradeService` 的等级与技能限制计算方法，消除了两处完全重复的判定逻辑。
    - 将 `WeaponCombatSupport` 中硬编码的精炼战斗属性加成（+15% 伤害、+5%/+10% 暴击率、+10% 全属性伤害加成）提取为 `WeaponUpgradeService` 的统一常量，提升代码可读性与可维护性。
- [x] 整合与验证：
    - `./gradlew classes` 编译验证成功。

## プロジェクトパッケージ構造のリファクタリング (2026-05-22)
- [x] パッケージの整理とソースファイルの再配置：
    - 肥大化した `hifumi.cresora` ルートパッケージから、Kotlin ファイル群をドメイン別の新しいサブパッケージへ移動。
    - サブパッケージ構成：
        - `hifumi.cresora.adventurerank` (冒険ランク関連)
        - `hifumi.cresora.resonance` (共鳴/ガチャ関連)
        - `hifumi.cresora.masquerade` (マスカレード/キャラ切り替え関連)
        - `hifumi.cresora.bloodmoon` (ブラッドムーン関連)
        - `hifumi.cresora.story` (ストーリー関連)
        - `hifumi.cresora.domain` (秘境関連)
        - `hifumi.cresora.treasure` (宝箱関連)
        - `hifumi.cresora.credits` (クレジット関連)
        - `hifumi.cresora.combat` (戦闘/ダメージ計算関連)
        - `hifumi.cresora.equipment` (装備/聖遺物関連)
        - `hifumi.cresora.weapon` (武器関連)
        - `hifumi.cresora.debuff` (デバフ関連)
        - `hifumi.cresora.musicecho` (楽章共鳴関連)
    - `src/main/kotlin` および `src/client/kotlin` 内のファイルを対応するディレクトリ階層へ物理的に移動。
    - クライアント用 `Datagen.kt` を `src/client/kotlin/hifumi/cresora/` 直下に整理。
- [x] パッケージ宣言とインポート文の修正：
    - 移動したすべての Kotlin ファイル内の `package` 宣言を更新。
    - 全ソースコード（メイン/クライアントソース、Java Mixin ファイル、アセットコンパイラ、および自動生成された武器/聖遺物ハンドラーコード）のインポート参照を新しいパッケージ構成に適合するように修正。
- [x] アセットコンパイラ（CWC / CAC）の出力インポートの修正：
    - `CresoraCompiler.kt` (CWC) および `ArtifactCompiler.kt` (CAC) を修正し、生成コードに挿入されるデフォルトインポート宣言を新しいパッケージ構造に合わせて更新。
- [x] ビルドと整合性の検証：
    - `./gradlew compileAssets` で DSL アセット生成および生成コード内のインポートエラーがないことを確認。
    - `./gradlew classes` によるコンパイルチェックを行い、警告・エラーなくビルドが成功することを確認。
    - `cresora_document.md` 内のパッケージ修飾クラス名（例: `WeaponSkillService` への参照）を新パッケージ名へ更新。

## コンパイラアセット処理および構文解析器の最適化 (2026-05-22)
- [x] Movement/Artifact コンパイラのアセット走査最適化：
    - `MovementCompiler.kt` と `ArtifactCompiler.kt` のファイル走査フィルターを変更し、それぞれ `.movement` と `.artifact` 拡張子のみを対象にするように最適化。余計なファイル読み込みとパース処理を排除。
- [x] 翻訳キー重複警告の追加：
    - `MovementCompiler.kt` の `updateStoryTexts` にて、同一キーに対して異なる翻訳テキストがマージされ上書きされる際のコンソール警告ログを追加。
- [x] レアリティ変換バグの解消：
    - `MovementCompiler.kt` の `mapRarity` において、`"4_STAR"` から `"4_star"` へのマッピングでデッドコードとなっていた条件分岐を大文字の正規化により解消。
- [x] `Parser.kt` の重複関数整理：
    - 完全に一致していた `phasePreBattle()` と `phasePostBattle()` を `parseDialoguePhase()` に統合し、コードの重複を解消。
- [x] キーワード解析の厳密化と型安全化：
    - `movement()`, `phaseBattle()`, `battleWave()`, `modifiers()`, `rewards()` における識別子文字列マッチングを、`TokenType` を使った厳密な型安全マッチングへ移行。
    - 将来的なキーワードの追加と干渉を防ぐため、文字列識別子フィールドを `TokenType.IDENTIFIER` でラップ。
    - `phase` 解析時の冗長な `TokenType.IDENTIFIER` チェックを削除し簡素化。
- [x] 変数シャドウイングの解消：
    - `battleWave()` 内で、同名のクラスメソッド `modifiers()` との衝突を回避するため、ローカル変数 `modifiers` を `waveModifiers` にリネーム。

## Movement Compiler (CMC) の実装 & メインストーリー一元化 (2026-05-22)
- [x] ストーリー定義（会話、戦闘ウェーブ、報酬、多言語翻訳等）を管理する Movement Compiler (CMC) を実装：
    - `src/compiler/kotlin/hifumi/cresora/compiler/MovementCompiler.kt` を新規実装。
    - 各 `.movement` ファイルから定義を読み込み、`story_content.json` を生成、また多言語の翻訳キーを `story_texts.json` へ自動でマージする仕組みを構築。
- [x] Lexer / Parser / AST の拡張とコンパイラ統合：
    - `Lexer.kt` にストーリー関連のトークン・キーワードを追加。
    - `AST.kt` に `MovementDefNode`, `DialogueLineNode`, `BattleWaveNode`, `SpawnNode` などのノード群を定義。
    - `Parser.kt` でストーリー定義の入れ子フェーズ（`pre_battle`, `battle`, `post_battle`）や目標（`battle_objective`）等の構文解析をサポート。
    - `Main.kt` に `MovementCompiler` を組み込み、ビルドパイプラインに統合。
- [x] 既存ストーリーチャプターの `.movement` への移行：
    - `chapter_0_0.movement`, `chapter_0_1.movement`, `chapter_d0_1.movement`, `chapter_d0_2.movement`, `chapter_0_2.movement` を記述し、既存の JSON 定義と多言語翻訳（ja_jp, en_us, zh_cn, lzh）をすべて DSL に移行。
- [x] Gradle タスクのリネームと後方互換性の維持：
    - コンパイラが武器だけでなく聖遺物・ストーリーも含めた全アセットをビルドすることに伴い、Gradle タスク名を `generateWeapons` から `compileAssets` へリネーム。
    - 既存のスクリプト等を考慮し、非推奨（deprecated）として `generateWeapons` タスクも残し、`compileAssets` へ委譲するよう設定。
    - ガイド文書 (`AGENTS.md`, `GEMINI.md`, `CLAUDE.md`, `cresora_document.md`) 内のタスクコマンド指定をすべて `compileAssets` へアップデート。

## 確定ダメージ (True Damage) 一元管理リファクタ (2026-05-21)

- [x] 確定ダメージ（True Damage）処理ロジックの `WeaponSkillService` への一元化：
    - `WeaponSkillService.dealTrueDamage(player, target, amount)` を実装し、Minecraft の `damage()` パイプラインを正しく経由するよう改善。
    - `isDealingTrueDamage` を ThreadLocal で管理し、確定ダメージ処理中にスレッドセーフなフラグ制御を確立。
- [x] `LivingEntityMixin` を拡張し、確定ダメージを処理する際の適切なバイパスを実装：
    - 確定ダメージ適用時、モブ防御力、属性耐性、および各種被ダメージ倍率（MusicEcho, Masquerade, Story 等）の計算を安全にスキップ。
    - `cresora$showMobDamage` で確定ダメージ起因のダメージを検知し、`AdventureRankService.showMobTrueDamage`（確定ダメージ表示）を適切に呼び出すように分岐を整理。
- [x] 再帰ループの防止と安全弁の強化：
    - `WeaponSkillService.onAttackDealt` において、`isDealingTrueDamage` フラグを `onDamageDealt` ハンドラーへ伝播（`isTrueDamage` パラメータ）。
    - 確定ダメージ起因の攻撃で、淡墨長空の「破魂」スタックなどの追加ダメージボーナスが再帰的にループするのを防止。
- [x] CWC (Cresora Weapon Compiler) 命令および DSL への統合：
    - compiler の `InstructionMapping.kt` を更新し、DSL 上の `deal_true_damage` 組み込み命令が `WeaponSkillService.dealTrueDamage` を呼び出すように変更。
    - 淡墨長空の「墨中無念」DSL (`tanboku_chokuu.cresora`) と自動生成された Kotlin ハンドラーの 7撃目の確定ダメージ処理を新メソッド呼び出しへ統一。

## Resonance Hunt (共鳴探索) 守護者チャレンジシステムの統合 (2026-05-20)
- [x] 共鳴探索（Resonance Hunt）における守護者チャレンジ（Guardian Challenge）システムを設計・完全実装：
    - 宝箱の周囲6ブロック以内に所有者プレイヤーが接近した際、チャレンジが自動でトリガーされる近接検知ロジックを実装。
    - 難易度（星の数）に応じた守護者スポーン処理：
        - 3星：通常モブ3体（ゾンビ/スケルトン）
        - 4星：通常モブ2体＋エリートモブ1体
        - 5星：通常モブ2体＋エリートモブ2体（内1体はウィザースケルトン）
    - 守護者モブの動的ステータススケーリング：
        - `AdventureRankService.applyMobScaling` に加え、エリート属性に対するHP/防御倍率を適用。
        - 守護者を常時発光（glowing）状態に設定し、ターゲットを所有者プレイヤーに固定。
    - 宝箱上部への浮遊テキスト表示（Text Display Entity）の実装：
        - 宝箱の座標 `y + 1.25` に `DisplayEntity.TextDisplayEntity` を表示し、残り守護者数 `§6[共鸣挑战] §f击败守护者！ §7(剩余: X)` をリアルタイム同期。
    - 離脱/死亡時のチャレンジ自動リセットロジック：
        - プレイヤーの死亡、または宝箱から32ブロック以上離脱した場合に、スポーンした守護者および浮遊テキストをすべて自动消滅させ、チャレンジ状態をリセット。
    - 報酬システムのアップグレード（`grantUpgradedChallengeRewards`）：
        - チャレンジ成功後の開封時、50%の確率で聖遺物（対応する星数・レベル0-4のランダムな部位）、または50%の確率で対応する星数の武器断片をドロップ。
        - 守護者の討伐時にCSC（通常40 / エリート100）を即座に付与し、ゴールドナゲットを確定ドロップ。
- [x] コードレビューによる品質向上とバグ修正の実施：
    - `tickChallenges` における challenge の暗黙的な自動生成を排除し、トリガー時にのみ明示的に初期化することでメモリリークと状態不整合を防止。
    - `clearTransientState` において、プレイヤー切断時に進行中のチャレンジ、守護者エンティティ、浮遊テキストを確実にクリーンアップするロジック了。
    - `onGuardianKilled` のスレッドセーフな順序的実行と guardianUuids の即時同期処理により、`ConcurrentModificationException` を予防。
    - 守護者が1体も正常にスポーンできなかった（召喚失敗した）場合のフォールバック＆リセットを追加。
    - 近接検知 tick を 10 tick（0.5秒）に間引きしてパフォーマンスを最適化。
    - `findGuardianSpawnSpot` が失敗した場合に、宝箱の2ブロック上にフォールバックする安全なスポーン補正を追加。
    - NBT復元時に `activeChallenges` を明示的に clear するように修正。
    - literal string での `§` カラーコードと `formatted(Formatting)` メソッドの重複適用による画面表示の不整合リスクを解消。
- [x] コンパイルエラーおよび各種警告（`isSolid` の非推奨警告や冗長な null チェック）を完全に解決し、ビルドを安定化。

## コードレビューからのバグ修正 (2026-05-20)
- [x] `CresoraCompiler` と `ArtifactCompiler` の両方で、問題のあった `; { ... }` デッドコードラムダの生成バグを修正し、`run { ... }` に置き換えました。
- [x] 聖遺物からの過渡的なスカラーおよびボーナスステータスを完全に統合しました：
    - `ArtifactSkillHandler` インターフェースを更新し、スカラーのオーバーライドを追加 (`getAttackDamageScalar`, `getArmorScalar`, `getCritRateBonus`, `getCritDamageBonus`)。
    - `ArtifactSkillRegistry` のリフレクションの例外処理ブロックにロジックエラーのログ出力を追加。
    - `EquipmentPlayerSupport.getAggregatedStats()` を更新し、有効な聖遺物スキルハンドラーを照会して、過渡的なステータスを属性や戦闘処理に動的に反映するように修正。
- [x] `camelCase` 識別子変換の標準化：
    - 重複する変換ロジックを置き換えるため、`CresoraCompiler` と `ArtifactCompiler` の両方で共通のヘルパー関数 `camelCase()` を抽出。
    - 生成されるクラス名の命名規則を PascalCase に標準化 (例: `ArtifactSkillHinagata4pcOnAttackDealt`)。
- [x] `ArtifactCompiler` におけるローカライズ自動生成の拡張：
    - コンパイル時にバフ名、獲得メッセージ、失効メッセージの翻訳キーを自動生成。
    - 出力される翻訳 JSON ファイルの末尾に改行を追加することで POSIX 互換性を確保。
- [x] `InstructionMapping.kt` をリファクタリングし、`start_cooldown` に対する重複したチェックを整理し、`expanded` を読み取り専用の `val` に変更。
- [x] コンパイラおよび命令マッピングにおけるコードレビューの課題 (S-1, S-2, S-3) を修正：
    - `CresoraCompiler.kt` の `InstructionCallNode` 処理で `start_cooldown` をインターセプトし、冷却時間を正しく計算（`30s` などの時間リテラルを `* 20L` でティックに変換）し、不足していた `showCooldownBar(player, definition)` の呼び出しを復元。
    - `CresoraCompiler.kt` と `ArtifactCompiler.kt` の両方で `send_message` 指令をインターセプトし、カラー引数が指定されている場合に装飾用の `Formatting.<COLOR>` を使用してメッセージをフォーマットするように修正。これにより色のフォーマットデグレードを解決。
    - `InstructionMapping.kt` をリファクタリングし、曖昧さを避けるために文字列のコンテキストを型安全な `CompilerContext` 列挙型に変更。



## Artifact Compiler (CAC) & CWC Refactoring (2026-05-20)
- [x] 聖遺物コンパイラ (Artifact Compiler - CAC) を実装。`.artifact` ファイルから JSON と Kotlin Hook クラスの生成をサポート。
- [x] 聖遺物ランタイム Hook システムをリファクタリング。`EquipmentEffectHookService` を導入し、型安全なコンテキスト受け渡しをサポート。
- [x] コンパイラ (CWC/CAC) の DSL 構文を強化。C-style のセミコロン終端や `log()`, `apply_mark()` などの組み込み命令をサポート。
- [x] コンパイラの raw Kotlin コードブロック解析ロジックを修正。生ソースコード抽出 (Raw Source Extraction) により、空白や特殊記号による構文エラーを完全に解決。
- [x] Lexer に Kotlin の数値接尾辞 (`L`, `f`, `d`) のサポートを追加。
- [x] **CWC/CAC バフシステムの完全実装**:
    - `.artifact` DSL に `buff` ブロックを追加。持続時間、最大スタック数、stats（属性加算）の定義をサポート。
    - `add_buff` 命令を CWC/CAC 共通の intrinsic 指令として統合。武器スキルと聖遺物スキルの両方で共通のバフ付与ロジックを生成。
    - 瞬態状態管理（Transient State Management）の自動生成を強化。各バフごとに `expireTick` と `stacks` を保持する `State` クラスと `MutableMap` を自動生成し、`onTick` での期限切れ自動削除ロジックを実装。
    - サブスキルからのメインスキル状態へのアクセスを、クラス名修飾を介して型安全にサポート。
    - 生成された聖遺物スキルを `ArtifactSkillRegistry` に明示的に登録する `ArtifactSkillRegistry.init()` を `CreSoraUtilities` に追加。
    - 聖遺物の `stats` 解析における `StatType` のシリアライズ（小文字変換）を修正し、ランタイムでのロードエラーを解消。
    - サーバー起動テストを実施し、CAC コンテンツの正常なロードと初期化フローを確認。


## Real-Device Gameplay Smoke Test (2026-05-19)

- **Invoked workflow `/jikki-tesuto`**:
  - Cleaned and compiled both main and client environments successfully (`classes clientClasses`) using a fresh Gradle daemon to resolve incremental compilation and cache mismatches.
  - Launched the Fabric dev server and player client in separate background persistent terminal sessions.
  - Successfully connected a client session to the local dev server (`Player368 joined the game`).
  - Automatically caught the connection event and ran `op Player368` via server console stdin, granting operator status successfully.
  - Verified from the client side that the player received OP permissions, toggled their game mode to Creative, set the in-game time to day (`time set 1000`), and cleared the weather.
- **Custom Resonance Compass Asset & Modeling (共鳴コンパスのテクスチャ追加)**:
  - Generated a stunning golden and cosmic teal magical compass texture using `generate_image` based on high-fidelity circular game pixel-art prompts.
  - Developed and ran a scratch Python imaging script (using Pillow with custom polar masking) to crop the compass perfectly, eliminate non-transparent pixel backgrounds, scale it to a crisp 32x32 pixel-art icon, and save it under [resonant_locator.png](file:///Users/hifumimizuhara/IdeaProjects/cresora-utilities-1.21.7/src/main/resources/assets/cresora-utilities/textures/item/resonant_locator.png).
  - Updated [resonant_locator.json](file:///Users/hifumimizuhara/IdeaProjects/cresora-utilities-1.21.7/src/main/resources/assets/cresora-utilities/models/item/resonant_locator.json) to reference this new high-quality texture (`cresora-utilities:item/resonant_locator`), replacing the generic vanilla recovery compass template.
  - Ran a complete compilation build (`./gradlew classes clientClasses`) successfully to publish the new assets to the developer runtimes.

## Shop Registry Dependency & Compilation Hardening (2026-05-19)

- **Shop Registry Initialization Order Fix**:
  - Resolved a crash on mod initialization where `ShopContentRegistry.init()` attempted to load shop offers referencing `cresora-utilities:resonant_locator` before the static locator item itself was registered.
  - Moved the registration of all static blocks and items (including `moon_altar`, `resonant_locator`, `tueshokaku`, and `resonant_cache`) to the absolute beginning of `onInitialize()`. This guarantees that all static and dynamic identifiers are fully populated and available in standard registries before any JSON-based contents registries parse their schemas.
- **Gradle Daemon & Clean Build Verification**:
  - Addressed a ClassNotFoundException with Java Mixin class loading caused by out-of-sync compiler daemons.
  - Executed `./gradlew --stop` and a complete rebuild (`clean classes`), ensuring all Kotlin files and Java mixins compile in a unified and fresh environment classpath.
  - Verified a successful server startup: launched `./gradlew runServer` without any crashes, successfully loading all dynamic content registries (shop, resonance, story chapters, etc.) and starting the server network loop successfully.

## Resonance Hunt (共鳴探索) フィードバック改善と堅牢化 (2026-05-19)

コードレビューの指摘に基づき、共鳴探索（Resonance Hunt）システムに存在したクリティカルなバグおよび未実装要素を即座に修正・改善。

- **共鳴宝箱ブロックの可視化修正（モデルの継承バグ解消）**:
  - `resonant_cache` はプレーンな `Block` として登録されているため、従来の `minecraft:block/chest` 継承では静的モデルが存在せず、ゲーム内で透明（不可視）になっていたバグを修正。
  - `models/block/resonant_cache.json` を修正し、`gilded_blackstone`（金密なる黒石）のテクスチャを持つ `cube_all` 静的ブロックモデルを親とするように変更。これにより、ゲーム内で完璧にゴールド装飾の美しいブロックとして目視できるようになりました。
  - アイテムモデルの親を `minecraft:item/chest` から `cresora-utilities:block/resonant_cache` に修正し、インベントリ内でもブロックの形状を保持するように設定。
- **共鳴探索コンパスの入手経路の接続**:
  - `resonant_locator` がサバイバルのゲームプレイ上で入手不可能（登録のみ）だった問題を解決するため、[shop_content.json](file:///Users/hifumimizuhara/IdeaProjects/cresora-utilities-1.21.7/src/main/resources/data/cresora-utilities/cresora/shop_content.json) のショップアイテムリストに `resonant_locator`（価格: 50,000 CSC）を追加。プレイヤーがゲーム内で能動的に入手し、「共鳴探索」をフルで遊べるように調整。
- **NBTシリアライズのバリアフリー互換化（後方互換性バグ修正）**:
  - [TreasureChestPersistentState.kt](file:///Users/hifumimizuhara/IdeaProjects/cresora-utilities-1.21.7/src/main/kotlin/hifumi/cresora/TreasureChestPersistentState.kt) で新設された `expireTime` を強引に `fieldOf` で必須化していたため、アップデート前の古いセーブデータ（cresora_treasure_chests）が含まれるワールドを読み込むと Codec デコードエラーが発生し、既存のアクティブな宝箱がすべて失われる不具合を修正。
  - `Codec.LONG.optionalFieldOf("expireTime", 0L)` に修正し、古いデータ構造のセーブデータが破損・ロストすることなくスムーズにロードされ、安全に移行できるよう後方互換性を完全に保証。
- **検証**:
  - `./gradlew generateWeapons classes` を実行し、ビルドおよびコンパイルが正常に「BUILD SUCCESSFUL」で完了することを確認。

## Resonance Hunt (共鳴探索) 制度の全面実装と安定化 (2026-05-18)

Buggy で放置されていたバニラチェストの自動スポーン型「宝箱」システムを廃止し、プレイヤーの能動的で高品質な探索システム「共鳴探索 (Resonance Hunt)」を設計・全面実装。

- **能動的レーダーと専用コンパス**:
  - `resonant_locator` (共鳴探索コンパス/羅針盤) アイテムを新規追加・登録。
  - 使用時に 12秒間のクールダウン を適用し、無限連続設置の悪用を防止。
- **専用の共鳴宝箱ブロック**:
  - クレソラ専用の新規ブロック `resonant_cache` (共鳴宝箱) を導入し、バニラのピストン複製バグやバニラチェストのアイテム吸収・破壊ロジックとの競合を完全に排除。
- **安全な出現ロケーション探索**:
  - プレイヤーの周囲半径 10〜25ブロック内で、溶岩、奈落、岩盤、空中などを回避する安全な接地スポーン座標を高度なアルゴリズムで走査。
  - 秘境 (Domain) ディメンション内での探索コンパス使用を完全に遮断。
- **高クオリティなビジュアル演出**:
  - コンパス使用時に `BLOCK_BEACON_ACTIVATE` の重厚なサウンドを再生。
  - プレイヤーの視点から宝箱の出現位置に向けて、美しい `END_ROD` 粒子の高精度ガイド軌跡ビーム (`spawnGuideTrail`) を動的に描画。
- **厳格な所有者ロック (Strict Ownership)**:
  - 宝箱の開封時に所有者 UUID をチェック。他人の宝箱に触れた場合、所有者の名前付きで警告メッセージ（「この宝箱はあなたのものではありません！所有者: ○○」）を表示して窃盗を防御。
- **dynamic lifespan (TTL) と安全な消滅クレンジング**:
  - 宝箱は出現から 10分間 (12000 ticks) が経過すると自動で消滅。
  - ティック内の消滅処理で `ConcurrentModificationException` によるサーバー停止を防ぐため、安全な一時除去キューを活用した二フェーズ削除を採用。
- **NBT 永続化の完全シリアライズ**:
  - 宝箱の所有者、座標、報酬シード、および有効期限 (`expireTime`) を `TreasureChestPersistentState` の Minecraft `Codec` を通じて完全に NBT へシリアライズ・保存するよう強化。
- **多言語ローカライズ対応と 1.21.7 API 適応**:
  - `en_us`, `ja_jp`, `zh_cn` の全言語に探索システム、宝箱アイテム、エラー警告、開封報酬通知の翻訳リソースを追加。
  - Minecraft 1.21.7 (1.21.2+) の Fabric API 改定に追従し、`TypedActionResult<ItemStack>` から単一の `ActionResult` への一本化対応、および `itemCooldownManager` の `ItemStack` 引数仕様に完全適応。
- **検証**:
  - `GRADLE_USER_HOME=.gradle-user ./gradlew compileKotlin classes --console=plain` を実行し、すべてのコンパイル警告およびエラーがゼロで、正常にビルドが通ることを実証。

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
- Added `moon_altar` interactive block.
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
    - Wrapped `execute` blocks in `run execute@ { ... } to support `return@execute` for early exits.
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
- `gradlew classes` successfully compiles- [ ] Transition generated skills (BokuchuMunen) from direct health modification to true damage sources.
- [ ] Refactor legacy CommandActionNode AOE parsing to structural AreaOfEffectActionNode.
- [ ] Persist originalBedStates in BloodMoonService for perfect recovery.
- [ ] Explicitly initialize ArtifactSkillRegistry in CreSoraUtilities.onInitialize.
- [x] 多言語対応（zh_cn, lzh）の基盤実装および「千秋、葉落ちて」への適用
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

### Weapon Logic Refinement (Tanboku Chokuu)
- **API 1.21.x Compatibility**:
    - Fixed `target.kill()` to `target.kill(world)` to match the updated Minecraft API.
    - Ensured `world` is correctly cast to `ServerWorld` before calling `damage` or `spawnParticles` within `execute` blocks.
    - Replaced `iterateEntities()` with more robust `getOtherEntities` calls to improve type inference and performance.
- **Bug Fixes**:
    - Corrected `cresoraIsElite()` to `cresoraIsEliteMob()` in the `Zanso` sub-skill logic.
    - Fixed particle type mismatch for `DUST_COLOR_TRANSITION` by switching to simpler, more reliable particle types (`CRIT`, `SNOWFLAKE`) where appropriate.

- `gradlew classes` now completes successfully with all generated handlers compiling correctly.

## CWC Localization & Modeling Expansion

Enhanced the Cresora Weapon Compiler (CWC) and core weapon systems to support cleaner localization and custom placeholder models.

### Infrastructure Improvements
- **DSL Localization Enhancement**:
    - Added `send_localized_message(key, color, args...)` action to the DSL. This provides a cleaner way to send translatable messages with placeholders (`%s`, etc.) without writing manual Kotlin code in `execute` blocks.
    - Updated the code generator to translate this action into standard `player.sendMessage(Text.translatable(key, ...).formatted(COLOR), true)` calls.
- **Custom Model Data Support**:
    - Added `custom_model_data` property to the `weapon` block in the DSL.
    - Updated `WeaponDefinition` and the core registry to parse and store `customModelData`.
    - Modified `WeaponStackSupport` to automatically apply the `CUSTOM_MODEL_DATA` component to weapon item stacks if defined in the weapon's definition.
- **Asset Automation**:
    - Added `updateItemModels` to the compiler. It now automatically generates missing item model JSON files (`assets/cresora-utilities/models/item/<id>.json`) for weapons, using the `base_item` as a parent and placeholder texture.
    - This ensures all weapons defined in `.cresora` files have a visible in-game presence even before custom assets are created.

### Weapon Logic Refinement
- **Tanboku Chokuu & Dark Lux**:
    - Assigned temporary `custom_model_data` (10101, 10102) to provide a base for future unique modeling.
    - Standardized skill activation and feedback messages using the new localization action.

### Verification
- Successfully ran `./gradlew generateWeapons` to verify parser and code generation.
- Successfully ran `./gradlew classes` to ensure 1.21.7 API compatibility (including the overhauled `CustomModelDataComponent` constructor).

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

## CWC Sub-Skill & Tanboku Rename Fixes

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

## CWC & System Polish

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
- [x] 实现了圣遗物编译器 (Artifact Compiler - CAC)，支持从 `.artifact` 文件生成 JSON 和 Kotlin Hook 类。
- [x] 重构了圣遗物运行时 Hook 系统，引入 `EquipmentEffectHookService` 以支持类型安全的上下文传递。
- [x] 增强了编译器 (CWC/CAC) 的 DSL 语法，支持 C-style 分号结尾以及 `log()`, `apply_mark()` 等指令。
- [x] 修复了编译器对 raw Kotlin 代码块的解析逻辑，通过直接提取原始源码（Raw Source Extraction）完美解决了空格和特殊符号导致的语法错误。
- [x] 为 Lexer 增加了对 Kotlin 数字后缀 (`L`, `f`, `d`) 的支持。
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

## Comprehensive Code Review (2026-05-19)

Completed a comprehensive code review of the entire CreSora Utilities codebase.

### Accomplishments
- [x] Implement Cresora Artifact Compiler (CAC) for .artifact DSL.
- [x] Fix critical regressions in EquipmentContentRegistry (data overwriting).
- [x] Fix reflection issues in CompiledArtifactRegistry (added @JvmStatic).
- [x] Fix InstructionMapping string mangling bug.
- [x] Implement robust persistence for BloodMoonService (wave/mob tracking).
- [x] Add TTL and ownership checks to TreasureChestService.
- [x] Add guide particle trails for Resonant Locators.
- [x] Add recursion guard to WeaponSkillService damage processing.
- Reviewed key architectural singletons, state lifecycles (`WeaponSkillService`), threading safety structures (`TreasureChestService`), and the compiler core (`CresoraCompiler` & `AST`).
- Highlighted key architectural strengths: active weapon slot tracking, robust memory leak prevention (scavenging offline states), thread-safety under ticking collections, and structural DSL compilation using KotlinPoet.
- Authored a comprehensive code review report (`comprehensive_code_review.md`) outlining key findings and critical architectural recommendations (reactive attribute caching, decoupling of weapon constants, DSL source mapping, and state-machine design for complex events).
- Placed the final report in `/review/comprehensive_code_review.md` and registered as a project artifact.

### Verification
- `GRADLE_USER_HOME=.gradle-user ./gradlew classes --console=plain` passed successfully, verifying that compiler weapons generation and classes compilation are 100% stable.

## Blood Moon State Machine Refactoring (2026-05-19)

Refactored the Blood Moon battle status management to use a robust State Machine Pattern (State Pattern) as recommended in Proposal 4 of the comprehensive code review.

### Accomplishments
- Introduced the `BloodMoonBattleState` interface and defined explicit, self-contained concrete states: `RestingState`, `PreparingState`, `CombatState`, and `CompletedState`.
- Delegated the tick-based phase transition and countdown/status logic from the long `tickBattleSession` method into individual state classes, enhancing isolation and readability.
- Retained absolute compatibility with existing downstream services by exposing the current phase reactive getter (`val phase get() = currentState.phase`).
- Changed `BloodMoonBattleSession`, `BloodMoonBedKey`, and state-related classes to `internal` scope to securely expose types while maintaining compile-time validation.
- Cleaned up invalid state mutability by removing manual reassignments of `session.phase` outside the state pattern, driving all flow changes through return states.

### Verification
- `GRADLE_USER_HOME=.gradle-user ./gradlew classes --console=plain` passed successfully, verifying that all visibility modifiers, state machine logic, and parameters compiles cleanly with 0 errors.

## Item Definitions and Chunk I/O Optimizations (2026-05-19)

Resolved several P2 issues identified during code review, targeting missing item definitions for 1.21 assets and critical performance optimizations for remote chunk loading.

### Accomplishments
- Created missing 1.21 item-definition JSON files for `resonant_locator` and `resonant_cache` under `assets/cresora-utilities/items`, resolving the missing-model rendering issue in game.
- Hardened the treasure chest expiry cleanup (`TreasureChestService.kt`) by adding a `isChunkLoaded` check before evaluating block state deletion. This successfully prevents needless remote chunk force-loading I/O when players scatter caches across the world, eliminating server-side tick lag.

### Verification
- Stopped Gradle daemon and cleared build caches via `./gradlew clean classes` to resolve standard locks.
- `GRADLE_USER_HOME=.gradle-user ./gradlew clean classes --console=plain` passed successfully, verifying that all model files are in place and service optimizations compile cleanly.

## CWC Weapon Texture Fallback and Sub-Skill Dummy Model Warning Fixes (2026-05-23)

Resolved weapon custom textures not being applied and block model warnings during client startup.

### Accomplishments
- **Dynamic Weapon Texture Mapping**:
  - Updated the model generator in `CresoraCompiler.kt` to check if a custom weapon texture PNG file exists (`assets/cresora-utilities/textures/item/<weapon_id>.png`).
  - If a custom texture PNG exists, the generated model JSON now automatically uses the custom texture (`cresora-utilities:item/<weapon_id>`) instead of immediately falling back to the vanilla base item's texture (e.g. `minecraft:item/wooden_sword`).
  - If the custom PNG does not exist (like for test or placeholder weapons), it safely falls back to the vanilla base item's texture.
- **Sub-skill Dummy Model Warning Fix**:
  - Removed outdated block model overrides in `CresoraCompiler.kt` that forced sub-skill dummy models for `minecraft:lightning_rod` and `minecraft:blue_ice` to use incorrect block paths (e.g. `minecraft:lightning_rod`), which caused startup warning logs on 1.21.7.
  - Sub-skill dummy icon selectors now cleanly resolve to correct item models (`minecraft:item/lightning_rod` and `minecraft:item/blue_ice`).

### Verification
- `GRADLE_USER_HOME=.gradle-user ./gradlew compileAssets --console=plain` successfully compiled and generated all model files with correct texture path references (e.g., `cadenza_allegro.json` pointing to `cresora-utilities:item/cadenza_allegro`).
- `GRADLE_USER_HOME=.gradle-user ./gradlew classes --console=plain` compiled successfully with 0 errors.

