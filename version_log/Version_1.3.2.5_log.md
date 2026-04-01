# Version 1.3.2.5 Log

Date: 2026-04-01
Target: Minecraft 1.21.7 / Fabric Loader 0.18.4
Supported Languages: Japanese, English, Simplified Chinese, Literary Chinese, Literary Chinese (Simplified Script)

## Japanese

### 概要

- 戦闘を物理ダメージと術ダメージへ分離し、日本語名 `寒霧、雪巻きて` を正式反映しました。

### 更新内容

- 武器ダメージ種別 `physical` / `arcane` を追加
- 現行では `暁へと向かうレクエイム` と `寒霧、雪巻きて` を術ダメージ、その他を物理ダメージへ分類
- プレイヤー耐性を `物理耐性` と `術耐性` に分離
- 旧 `被ダメ軽減` は互換値として残し、両耐性へ加算される形に変更
- `/cresora_stats` と武器ツールチップへ新表示を追加
- `寒霧、雪巻きて` の日本語名称と欠片名を正式表記へ更新

## English

### Summary

- Split combat into physical and arcane damage, and updated the official Japanese name of `hanwu_juanxue`.

### Changes

- Added `physical` and `arcane` weapon damage types
- Classified `requiem_toward_dawn` and `hanwu_juanxue` as arcane damage, with all other current weapons treated as physical damage
- Split player defense into `Physical RES` and `Arcane RES`
- Kept legacy `Damage Reduction` as a compatibility value that now contributes to both resistance types
- Updated `/cresora_stats` and weapon tooltips to show the new split
- Updated the Japanese display name and fragment name for `hanwu_juanxue`

## 简体中文

### 概要

- 将战斗拆分为物理伤害与术伤害，并同步更新了 `hanwu_juanxue` 的日文正式译名。

### 更新内容

- 新增武器伤害类型 `physical` / `arcane`
- 现阶段将 `requiem_toward_dawn` 与 `hanwu_juanxue` 归类为术伤害，其余现存武器归类为物理伤害
- 将玩家防御拆分为 `物理抗性` 与 `术抗性`
- 保留旧 `减伤` 作为兼容数值，并改为同时计入两类抗性
- 更新 `/cresora_stats` 与武器提示文本，显示新的伤害/抗性划分
- 更新 `hanwu_juanxue` 的日文显示名与碎片名

## 文言

### 概要

- 今析戰傷為形與術二途，又正 `hanwu_juanxue` 之日文定名。

### 更新

- 增兵傷之類 `physical` 與 `arcane`
- 今以 `requiem_toward_dawn` 及 `hanwu_juanxue` 為術傷，餘兵皆屬形傷
- 析玩家之禦為 `禦形` 與 `禦術`
- 舊 `減傷` 猶存，以為兼容，今並益於二禦
- 更新 `/cresora_stats` 與兵提示，以明新制
- 正 `hanwu_juanxue` 之日文顯名及其片名

## 文言（简体字）

### 概要

- 今析战伤为形与术二途，又正 `hanwu_juanxue` 之日文定名。

### 更新

- 增兵伤之类 `physical` 与 `arcane`
- 今以 `requiem_toward_dawn` 及 `hanwu_juanxue` 为术伤，余兵皆属形伤
- 析玩家之御为 `御形` 与 `御术`
- 旧 `减伤` 犹存，以为兼容，今并益于二御
- 更新 `/cresora_stats` 与兵提示，以明新制
- 正 `hanwu_juanxue` 之日文显名及其片名
