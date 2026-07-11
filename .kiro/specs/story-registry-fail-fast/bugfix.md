# バグ修正要件ドキュメント

## はじめに

`StoryContentRegistry.init()` はバンドルJSON (`story_content.json`) の読み込み失敗時にハードコードされたデフォルトコンテンツ (`defaultBundle()`) へサイレントにフォールバックする。この設計はJSON破損・欠落を隠蔽し、プレイヤーが気づかないまま本番データとは異なるダミーストーリーで遊んでしまう問題を引き起こす。バンドルJSONの読み込みは即時失敗（fail-fast）すべきであり、組み込みデフォルトは削除すべきである。

## バグ分析

### 現在の挙動（欠陥）

1.1 WHEN `story_content.json` が欠落または破損している THEN `init()` は例外をキャッチし、エラーログのみ出力して `defaultBundle()` のハードコードデータで動作を継続する

1.2 WHEN `story_content.json` のパースが失敗する THEN `runCatching` により例外が握りつぶされ、modは正常に起動したかのように見える

1.3 WHEN `defaultBundle()` のフォールバックが適用されている THEN プレイヤーは本番ストーリーとは異なるダミーコンテンツ（チャプター 0-0, 0-1, 0-2）でゲームを進行できてしまう

### 期待される挙動（正常）

2.1 WHEN `story_content.json` が欠落している THEN `init()` は即座に例外を伝搬してmodの初期化を失敗させなければならない（SHALL）

2.2 WHEN `story_content.json` のパースが失敗する THEN `init()` は即座に例外を伝搬してmodの初期化を失敗させなければならない（SHALL）

2.3 WHEN `defaultBundle()` メソッドが存在する THEN 削除されていなければならない（SHALL）— ハードコードされたフォールバックデータはコードベースに存在してはならない

### 変更なし挙動（回帰防止）

3.1 WHEN `story_content.json` が正常に読み込まれる THEN `applyBundle()` によるバリデーション（IDの一意性、unlockRank範囲、前提チャプター存在確認など）は従来通り動作し続けなければならない（SHALL CONTINUE TO）

3.2 WHEN `story_content.json` が正常に読み込まれる THEN チャプターデータがレジストリに登録され、`chapters()` 等のクエリメソッドが従来通り機能し続けなければならない（SHALL CONTINUE TO）

3.3 WHEN `applyBundle()` のバリデーションが失敗する THEN `require()` による `IllegalArgumentException` が従来通り伝搬し続けなければならない（SHALL CONTINUE TO）

---

## バグ条件の導出

### バグ条件関数

```pascal
FUNCTION isBugCondition(X)
  INPUT: X of type StoryContentLoadAttempt
  OUTPUT: boolean

  // JSONリソースが欠落・破損・パース不能の場合にバグ条件が成立
  RETURN X.resourceMissing OR X.jsonMalformed OR X.codecParseFailed
END FUNCTION
```

### プロパティ仕様（修正検証）

```pascal
// Property: Fix Checking — JSON読み込み失敗時の即時失敗
FOR ALL X WHERE isBugCondition(X) DO
  result ← StoryContentRegistry.init'(X)
  ASSERT result IS Exception
    AND mod_initialization_aborted(result)
    AND no_fallback_data_applied(result)
END FOR
```

### 保存目標（回帰検証）

```pascal
// Property: Preservation Checking — 正常JSON読み込み時の動作保全
FOR ALL X WHERE NOT isBugCondition(X) DO
  ASSERT StoryContentRegistry.init(X) = StoryContentRegistry.init'(X)
END FOR
```

正常なJSONが提供された場合、修正前後で `applyBundle()` のバリデーションロジックとチャプター登録結果は同一でなければならない。
