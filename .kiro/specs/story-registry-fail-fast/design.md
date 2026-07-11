# StoryContentRegistry Fail-Fast バグ修正設計

## Overview

`StoryContentRegistry.init()` は `story_content.json` の読み込み失敗時に `runCatching` + `defaultBundle()` を使い、サイレントにハードコードされたダミーデータへフォールバックする。この設計は破損・欠落JSONを隠蔽し、プレイヤーがダミーストーリー（チャプター 0-0, 0-1, 0-2）で遊ぶ事態を許してしまう。

修正方針は以下の通り:
1. `defaultBundle()` メソッドを完全に削除する
2. `runCatching` を削除し、`loadBundledContent()` の例外をそのまま伝搬させる
3. `applyBundle()` のバリデーションロジックは一切変更しない

## Glossary

- **Bug_Condition (C)**: `story_content.json` がクラスパス上に存在しない、またはJSON構文が壊れている、またはCodecパースに失敗する状態
- **Property (P)**: バグ条件成立時に `init()` が例外を伝搬してmod初期化を中止すること
- **Preservation**: 正常なJSON読み込み時の `applyBundle()` バリデーションとチャプター登録が従来通り動作すること
- **`StoryContentRegistry`**: `src/main/kotlin/hifumi/cresora/story/StoryContentRegistry.kt` に定義されたシングルトンオブジェクト。ストーリーチャプターのJSON読み込み・バリデーション・レジストリを管理
- **`loadBundledContent()`**: クラスパスから `story_content.json` を読み取り、Codecデシリアライズして `StoryContentBundle` を返す内部関数
- **`applyBundle()`**: バンドルのバリデーション（ID一意性、`unlockRank` 範囲、前提チャプター存在確認など）を実行し、`chapters` マップへ格納する内部関数
- **`defaultBundle()`**: ハードコードされたダミーチャプターデータ（0-0, 0-1, 0-2）を返すフォールバック関数（削除対象）

## Bug Details

### Bug Condition

`story_content.json` の読み込みが失敗した場合（リソース欠落・JSON構文エラー・Codecパースエラー）、`init()` は例外を握りつぶし `defaultBundle()` のダミーデータで動作を継続する。プレイヤーは破損データに気づかず、本番とは異なるストーリーを体験してしまう。

**Formal Specification:**
```
FUNCTION isBugCondition(input)
  INPUT: input of type StoryContentLoadAttempt
  OUTPUT: boolean

  RETURN input.resourceStream IS null          // リソース欠落
         OR input.jsonParseFailed              // JSON構文エラー
         OR input.codecDecodeFailed            // Codecパースエラー
END FUNCTION
```

### Examples

- `story_content.json` がJARから削除された場合 → 現在: `defaultBundle()` で起動成功 / 期待: `IllegalStateException("Missing resource: ...")` が伝搬しmod初期化失敗
- `story_content.json` に不正なJSON（閉じ括弧欠落など）がある場合 → 現在: エラーログのみでダミーデータ使用 / 期待: `JsonSyntaxException` が伝搬
- `story_content.json` にCodecが期待するフィールドが不足している場合 → 現在: ログ出力のみ / 期待: `IllegalArgumentException("Invalid story content: ...")` が伝搬
- `story_content.json` が正常に読み込まれた場合 → 修正前後で動作同一（変更なし）

## Expected Behavior

### Preservation Requirements

**Unchanged Behaviors:**
- `applyBundle()` 内部のバリデーションロジック（ID一意性チェック、`unlockRank` 範囲チェック、前提チャプター存在チェック、`linkedDomainId` 存在チェック、武器パラメータ検証など）は一切変更しない
- 正常JSON読み込み時のチャプター登録処理（`chapters` マップへの格納）は従来通り機能する
- `chapters()`, `chapterGroups()`, `chaptersForGroup()`, `requireChapter()` 等のクエリメソッドは従来通り機能する
- `applyBundle()` のバリデーション失敗時の `require()` による `IllegalArgumentException` は従来通り伝搬する

**Scope:**
正常な `story_content.json` が提供されるすべてのケースにおいて、修正前後で `init()` の結果は同一でなければならない。変更の影響範囲は `init()` メソッド内部のエラーハンドリングパスと `defaultBundle()` の削除のみである。

## Hypothesized Root Cause

`init()` の実装に2つの防御的設計パターンが組み合わさっている:

1. **`defaultBundle()` によるフォールバック初期化**: `init()` の冒頭で `applyBundle(defaultBundle())` を呼び出し、JSON読み込み前にダミーデータをレジストリに適用している。これにより `loadBundledContent()` が失敗しても `chapters` マップは空にならない。

2. **`runCatching` による例外握りつぶし**: `loadBundledContent()` を `runCatching` で囲み、失敗時は `.onFailure` でエラーログのみ出力し、既に適用済みの `defaultBundle()` データで動作を継続する。

この2つが組み合わさることで「JSONが壊れていてもmodは正常に起動する」という隠蔽が成立している。開発初期のプロトタイプ段階では妥当だったかもしれないが、本番運用ではデータ破損の検知を妨げる深刻な設計欠陥となっている。

## Correctness Properties

Property 1: Bug Condition - JSON読み込み失敗時の即時失敗

_For any_ input where `story_content.json` が欠落、構文エラー、またはCodecパースに失敗する状態（isBugCondition が true を返す）、修正後の `init()` は例外を即座に伝搬し、mod初期化を中止しなければならない（SHALL）。フォールバックデータの適用は行われない。

**Validates: Requirements 2.1, 2.2, 2.3**

Property 2: Preservation - 正常JSON読み込み時の動作保全

_For any_ input where `story_content.json` が正常に読み込み・パースできる状態（isBugCondition が false を返す）、修正後の `init()` は修正前と同一の結果（`applyBundle()` によるバリデーション実行とチャプター登録）を生成しなければならない（SHALL）。

**Validates: Requirements 3.1, 3.2, 3.3**

## Fix Implementation

### Changes Required

**File**: `src/main/kotlin/hifumi/cresora/story/StoryContentRegistry.kt`

**Function**: `init()`

**Specific Changes**:

1. **`defaultBundle()` メソッドの完全削除**: ハードコードされたダミーチャプターデータ（0-0, 0-1, 0-2）を定義する `defaultBundle()` private関数を削除する。約120行のコード削除。

2. **`init()` からのフォールバック呼び出し削除**: `applyBundle(defaultBundle())` 行を削除する。

3. **`runCatching` の削除**: `runCatching { loadBundledContent() }` の `runCatching` ラッパーを除去し、`loadBundledContent()` を直接呼び出す。

4. **`.onSuccess` / `.onFailure` チェーンの除去**: Result型のコールバックチェーンを除去し、代わりに `loadBundledContent()` の戻り値を直接 `applyBundle()` に渡す。

5. **`init()` の簡素化後の形**:
   ```kotlin
   fun init() {
       val bundle = loadBundledContent()
       applyBundle(bundle)
       logger.info("Loaded story content from {}", CONTENT_RESOURCE)
   }
   ```

6. **`applyBundle()` は変更なし**: バリデーションロジックは一切触らない。

## Testing Strategy

### Validation Approach

テスト戦略は2段階で構成される: まず修正前コードでバグを実証するカウンターサンプルを発見し、次に修正後コードで正しい動作と既存動作の保全を検証する。

### Exploratory Bug Condition Checking

**Goal**: 修正実装前に、バグの存在を実証するカウンターサンプルを発見し、根本原因分析を確認する。反証された場合は再仮説が必要。

**Test Plan**: `story_content.json` が欠落・破損している状況をシミュレートし、`init()` が例外を伝搬せずにダミーデータで起動することを確認する。修正前コードで実行して失敗パターンを観察する。

**Test Cases**:
1. **リソース欠落テスト**: クラスパスに `story_content.json` が存在しない環境で `init()` を呼び出す（修正前コードでは例外が握りつぶされダミーデータで動作）
2. **JSON構文エラーテスト**: 不正なJSON文字列を持つリソースで `init()` を呼び出す（修正前コードではフォールバック発動）
3. **Codecパース失敗テスト**: 必須フィールド欠落のJSONで `init()` を呼び出す（修正前コードではフォールバック発動）
4. **`defaultBundle()` 適用確認テスト**: フォールバック後に `chapters()` がダミーデータ（0-0, 0-1, 0-2）を返すことを確認

**Expected Counterexamples**:
- `init()` が例外を投げずに正常終了する
- `chapters()` がダミーデータを返し、本番データがない状態でmodが起動する
- 原因: `runCatching` + `defaultBundle()` の防御的設計による例外隠蔽

### Fix Checking

**Goal**: バグ条件が成立するすべての入力に対して、修正後の `init()` が期待動作（例外伝搬）を生成することを検証する。

**Pseudocode:**
```
FOR ALL input WHERE isBugCondition(input) DO
  result := StoryContentRegistry.init'(input)
  ASSERT result IS Exception
    AND mod_initialization_aborted(result)
    AND chapters_map_is_empty_or_unchanged()
END FOR
```

### Preservation Checking

**Goal**: バグ条件が成立しないすべての入力（正常JSON）に対して、修正後の `init()` が修正前と同一の結果を生成することを検証する。

**Pseudocode:**
```
FOR ALL input WHERE NOT isBugCondition(input) DO
  ASSERT StoryContentRegistry.init(input) = StoryContentRegistry.init'(input)
END FOR
```

**Testing Approach**: Property-based testing は保全検証に推奨される:
- 多様なバリデーション成功パターンを自動生成して広範なカバレッジを確保
- 手動ユニットテストでは見落としがちなエッジケースを検出
- 非バグ条件のすべての入力で動作が変わらないことの強い保証を提供

**Test Plan**: 修正前コードで正常JSONを読み込んだ結果を観察し、修正後コードでも同一結果が得られることをproperty-based testで検証する。

**Test Cases**:
1. **正常バンドル適用テスト**: 有効な `StoryContentBundle` を `applyBundle()` に渡して `chapters` マップが正しく構築されることを確認
2. **バリデーション例外伝搬テスト**: `applyBundle()` のバリデーション失敗（重複ID等）で `IllegalArgumentException` が従来通り伝搬することを確認
3. **クエリメソッド保全テスト**: `chapters()`, `chapterGroups()`, `requireChapter()` が正常バンドル適用後に従来通り機能することを確認

### Unit Tests

- `init()` がリソース欠落時に `IllegalStateException` をスローすることのテスト
- `init()` がJSON構文エラー時に例外をスローすることのテスト
- `init()` がCodecパース失敗時に `IllegalArgumentException` をスローすることのテスト
- `init()` が正常JSON時にチャプターを正しく登録することのテスト
- `defaultBundle()` が削除されていること（リフレクションによる存在確認）のテスト

### Property-Based Tests

- ランダムに生成した有効な `StoryContentBundle` を `applyBundle()` に渡し、バリデーション通過とチャプター登録が正しく動作することを検証
- ランダムに生成した不正入力（欠落フィールド、範囲外値など）で `applyBundle()` の `require()` が確実に例外を投げることを検証
- 正常バンドル適用後の `chapters()` ソート順が `compareBy` の仕様に一致することを検証

### Integration Tests

- modの `onInitialize()` フロー全体で `story_content.json` 欠落時にmod初期化が失敗し、Fabricのログに適切なエラーが記録されることのテスト
- 正常な `story_content.json` での起動フロー全体（`runServer` / `runClient` スモークテスト）で従来通り動作することの確認
- `story_content.json` に新規チャプターを追加した場合にバリデーションを通過し正しく登録されることのテスト
