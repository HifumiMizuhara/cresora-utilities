# Implementation Plan

## Overview

`StoryContentRegistry.init()` のサイレントフォールバックを fail-fast 設計に修正する。`defaultBundle()` の削除、`runCatching` の除去、`init()` の簡素化を行い、JSON読み込み失敗時に即座に例外を伝搬させる。

## Tasks

- [x] 1. バグ条件探索テストを作成する
  - **Property 1: Bug Condition** - JSON読み込み失敗時のサイレントフォールバック
  - **CRITICAL**: このテストは修正前コードで必ず失敗する — 失敗がバグの存在を証明する
  - **DO NOT**: テストやコードを修正しようとしないこと — 失敗は想定通り
  - **NOTE**: このテストは期待動作をエンコードしている — 修正後にパスすることで修正の正当性を検証する
  - **GOAL**: バグの存在を実証するカウンターサンプルを発見する
  - **Scoped PBT Approach**: 決定論的バグのため、具体的な失敗ケースにスコープする: `story_content.json` が欠落・破損した状態で `init()` が例外を伝搬せずダミーデータで起動する
  - テスト内容（設計書 Bug Condition より）:
    - `story_content.json` がクラスパスに存在しない環境で `init()` を呼び出し、例外が伝搬されることをアサート
    - JSON構文エラー（閉じ括弧欠落等）のリソースで `init()` を呼び出し、例外が伝搬されることをアサート
    - Codecパース失敗（必須フィールド欠落）のリソースで `init()` を呼び出し、例外が伝搬されることをアサート
  - `isBugCondition(input)`: `input.resourceMissing OR input.jsonParseFailed OR input.codecDecodeFailed`
  - 期待動作: `init()` が例外を即座に伝搬し、フォールバックデータが適用されない
  - 修正前コードで実行する
  - **EXPECTED OUTCOME**: テストが失敗する（`runCatching` + `defaultBundle()` により例外が握りつぶされるため）
  - カウンターサンプル文書化: 「`init()` がリソース欠落時に例外を投げず、`chapters()` がダミーデータ 0-0, 0-1, 0-2 を返す」
  - テストを作成・実行し、失敗を文書化した時点でタスク完了とする
  - _Requirements: 1.1, 1.2, 1.3, 2.1, 2.2_

- [x] 2. 保全プロパティテストを作成する（修正実装前）
  - **Property 2: Preservation** - 正常JSON読み込み時の動作保全
  - **IMPORTANT**: 観察ファーストメソドロジーに従う
  - 観察: 修正前コードで正常な `story_content.json` を読み込み、`applyBundle()` のバリデーション通過とチャプター登録結果を記録する
  - 観察: `applyBundle()` にバリデーション失敗データ（重複ID等）を渡し、`IllegalArgumentException` が伝搬されることを確認する
  - 観察: 正常バンドル適用後に `chapters()` がソート順通りのチャプターリストを返すことを確認する
  - プロパティテスト作成（設計書 Preservation Requirements より）:
    - 正常な `StoryContentBundle` → `applyBundle()` バリデーション通過 + `chapters` マップ正常構築
    - バリデーション失敗データ → `require()` による `IllegalArgumentException` 伝搬
    - `chapters()`, `chapterGroups()`, `requireChapter()` 等クエリメソッドの動作保全
  - `NOT isBugCondition(input)`: 正常にパース可能な `story_content.json` が提供される場合
  - 修正前コードで実行する
  - **EXPECTED OUTCOME**: テストがパスする（正常パスの動作はバグの影響を受けないため）
  - テストを作成・実行し、修正前コードでパスすることを確認した時点でタスク完了とする
  - _Requirements: 3.1, 3.2, 3.3_

- [x] 3. StoryContentRegistry fail-fast 修正

  - [x] 3.1 `defaultBundle()` メソッドを完全に削除する
    - ハードコードされたダミーチャプターデータ（0-0, 0-1, 0-2）を定義する `defaultBundle()` private関数を削除する（約120行）
    - フォールバックデータがコードベースに存在しないことを確認する
    - _Bug_Condition: isBugCondition(input) where input.resourceMissing OR input.jsonParseFailed OR input.codecDecodeFailed_
    - _Expected_Behavior: defaultBundle() が存在しないこと_
    - _Requirements: 2.3_

  - [x] 3.2 `init()` からフォールバック呼び出しと `runCatching` を削除する
    - `applyBundle(defaultBundle())` 行を削除する
    - `runCatching { loadBundledContent() }` の `runCatching` ラッパーを除去する
    - `.onSuccess` / `.onFailure` コールバックチェーンを除去する
    - _Bug_Condition: runCatching による例外握りつぶしの除去_
    - _Expected_Behavior: loadBundledContent() の例外がそのまま伝搬すること_
    - _Requirements: 2.1, 2.2_

  - [x] 3.3 `init()` を簡素化する
    - `loadBundledContent()` を直接呼び出し、戻り値を `applyBundle()` に渡す
    - 成功時のログ出力を維持する
    - 最終形: `val bundle = loadBundledContent()` → `applyBundle(bundle)` → `logger.info(...)`
    - _Bug_Condition: isBugCondition(input) from design_
    - _Expected_Behavior: init() が load → validate → log success の3ステップになること_
    - _Preservation: applyBundle() のバリデーションロジックは一切変更しない_
    - _Requirements: 2.1, 2.2, 3.1, 3.2_

  - [x] 3.4 バグ条件探索テストがパスすることを検証する
    - **Property 1: Expected Behavior** - JSON読み込み失敗時の即時失敗
    - **IMPORTANT**: タスク1と同じテストを再実行する — 新しいテストを書かない
    - タスク1のテストは期待動作をエンコードしている
    - テストがパスすれば、期待動作が満たされたことを確認できる
    - タスク1のバグ条件探索テストを実行する
    - **EXPECTED OUTCOME**: テストがパスする（修正によりバグが解消されたため）
    - _Requirements: 2.1, 2.2, 2.3_

  - [x] 3.5 保全テストが引き続きパスすることを検証する
    - **Property 2: Preservation** - 正常JSON読み込み時の動作保全
    - **IMPORTANT**: タスク2と同じテストを再実行する — 新しいテストを書かない
    - タスク2の保全プロパティテストを実行する
    - **EXPECTED OUTCOME**: テストがパスする（回帰なし）
    - 修正後もすべてのテストがパスすることを確認する（回帰なし）

- [x] 4. チェックポイント - すべてのテストがパスすることを確認
  - コンパイル確認: `GRADLE_USER_HOME=.gradle-user ./gradlew classes --console=plain`
  - バグ条件探索テスト（Property 1）がパスすること
  - 保全プロパティテスト（Property 2）がパスすること
  - 質問がある場合はユーザーに確認する

## Notes

- このプロジェクトにはゲームプレイコードの自動テストスイートが存在しないため、テストは新規作成となる
- テストファイルの配置先は `src/test/kotlin/` 配下を想定（要確認）
- `applyBundle()` のバリデーションロジックは一切変更しない — 影響範囲は `init()` 内部のエラーハンドリングパスと `defaultBundle()` の削除のみ
- ビルド検証: `GRADLE_USER_HOME=.gradle-user ./gradlew classes --console=plain`

## Task Dependency Graph

```json
{
  "waves": [
    ["1", "2"],
    ["3.1"],
    ["3.2"],
    ["3.3"],
    ["3.4", "3.5"],
    ["4"]
  ]
}
```
