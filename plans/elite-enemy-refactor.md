# エネミー仕様変更：エリート強化とカスタムデバフシステム計画

## 1. 背景と目的
エリートモンスターの存在感を高め、戦闘に戦略的な深みを持たせるため、以下の2点を実施します。
- **視認性の向上**: `GENERIC_SCALE` 属性を利用し、エリートモンスターのサイズ（見た目・当たり判定）を大きくします。
- **カスタムデバフシステム**: バニラの StatusEffect に依存しない、Cresora 独自のデバフシステムを構築し、エリート攻撃時に「神経損傷」や「禁錮」などの特殊状態をプレイヤーに付与します。

## 2. 変更内容

### A. エリートモンスターの巨大化 (Visibility)
- `AdventureRankService.applyMobScaling` 内で、対象がエリート（`cresoraIsEliteMob()`）の場合に `EntityAttributes.GENERIC_SCALE` を適用します。
- **スケール値**: 1.15倍〜1.25倍程度（要調整）。
- **実装手法**: `EntityAttributeModifier` を使用。1.21.7 の仕様に合わせ、UUID ではなく `Identifier` (`cresora-utilities:elite_scale`) を使用して永続モディファイアとして追加します。

### B. カスタムデバフシステム (Custom Debuff System)
バニラのポーション効果とは別に、以下のコンポーネントを作成します。

1. **`CresoraDebuff` インターフェース/データクラス**:
   - `id: Identifier`
   - `nameKey: String`
   - `onApply(player: ServerPlayerEntity)`
   - `onTick(player: ServerPlayerEntity, remainingTicks: Int)`
   - `onRemove(player: ServerPlayerEntity)`
   - `canHeal(): Boolean` (回復禁止フラグ)
   - `cooldownMultiplier(): Double` (スキルCD延長係数)

2. **`CresoraDebuffRegistry`**:
   - 各デバフの定義を保持。

3. **`CresoraDebuffService`**:
   - プレイヤーごとのアクティブなデバフ（`Map<Identifier, Int>` 等）を管理。
   - `ServerTickEvents.END_SERVER_TICK` でデバフのカウントダウンと効果の適用を行う。
   - `LivingEntityMixin.damage` の戻り値フックで、エリートからプレイヤーへのダメージ発生時にデバフを抽選・付与。

### C. 実装するデバフの詳細
- **神経損傷 (Nerve Damage)**: 攻撃力低下、被ダメージ微増、またはスタミナ（既存システムがあれば）への影響。
- **禁錮 (Root)**: 移動速度属性を 0 にするか、テレポート等を制限。
- **視界不良 (Smoke)**: プレイヤーに短い盲目（Blindness）を付与するか、カスタムの画面オーバーレイを表示。
- **火傷 (Burn)**: 定期的な火属性ダメージ。
- **スキル冷却時間延長 (Cooldown Penalty)**: `WeaponSkillService` のクールダウン計算時に、アクティブなデバフがある場合、経過時間を遅らせるか付与時に CD を追加。
- **HP回復禁止 (Heal Block)**: `NaturalRegenService` や武器スキルの回復処理（`WeaponSkillService.heal` 等）で、デバフの有無をチェックして無効化。

## 3. 実装ステップ

### Step 1: 属性スケールの適用
- `AdventureRankService.kt` の修正。
- 1.21.7 の `EntityAttributes.GENERIC_SCALE` の存在確認と適用。

### Step 2: デバフシステムの基盤作成
- `CresoraDebuff.kt`, `CresoraDebuffRegistry.kt`, `CresoraDebuffService.kt` の新規作成。
- プレイヤーの状態を保持するための `PlayerEntity` への Mixin または `Component` (Fabric API) の追加。

### Step 3: 各デバフの実装
- 各種デバフ（神経損傷、禁錮等）のロジックを Registry に登録。

### Step 4: 既存サービスへの統合
- `WeaponSkillService`, `NaturalRegenService` へのフック追加。
- `LivingEntityMixin` への攻撃時デバフ付与ロジックの追加。

### Step 5: UI/フィードバック
- デバフ付与時にアクションバーにメッセージを表示。
- パーティクル演出の追加。

## 4. 検証・テスト計画
- エリートモンスターが通常より大きくスポーンすることを確認。
- エリートから攻撃を受けた際、意図したデバフが付与されるか確認。
- 「HP回復禁止」中に回復スキルや自然回復が動作しないことを確認。
- デバフの効果時間が終了したら、正常に解除されることを確認。
