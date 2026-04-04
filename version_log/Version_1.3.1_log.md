# Version 1.3.1 Log

Date: 2026-03-28
Target: Minecraft 1.21.7 / Fabric Loader 0.18.4
Supported Languages: Japanese, English, Simplified Chinese, Literary Chinese, Literary Chinese (Simplified Script)

## Japanese

### 概要

- このバージョンは `1.3.0` のパッチ更新です。聖遺物の右クリック強化導線を杖以外にも正しく広げました。

### 更新内容

- 帽子、メガネ、鎧、靴でも右クリックで強化画面を開けるよう修正
- 聖遺物定義の `opensUpgradeScreen` の既定値を有効側へ変更し、将来の追加装備でも同じ抜けを防止
- 強化画面の初期挿入処理を「杖」前提の命名から汎用装備前提へ整理

## English

### Summary

- This is a patch release for `1.3.0`. It fixes right-click upgrade access for artifacts beyond the wand.

### Changes

- Fixed hats, glasses, armor, and boots so they now open the upgrade screen on right-click
- Switched the default `opensUpgradeScreen` behavior for artifact definitions to enabled, preventing the same omission on future equipment
- Cleaned up the upgrade-screen auto-insert path so it no longer carries wand-only assumptions

## 简体中文

### 概要

- 本版本是 `1.3.0` 的补丁更新，修复了除权杖外其他圣遗物无法右键打开强化界面的问题。

### 更新内容

- 修复帽子、眼镜、护甲、靴子可通过右键打开强化界面
- 将圣遗物定义中的 `opensUpgradeScreen` 默认值改为开启，避免今后新增装备再次漏配
- 整理强化界面的初始放入逻辑，去除仅针对权杖的命名与假设

## 文言

### 概要

- 是版爲 `1.3.0` 之補，正諸聖遺物中杖外諸件不能右擊啓強化之失。

### 更新

- 正冠、鏡、甲、履，皆得以右擊開強化之畫
- 改聖遺物定義內 `opensUpgradeScreen` 之常值爲開，以杜後來新裝備復遺之弊
- 理強化畫面初置之路，不復存杖專用之名與假設

## 文言（简体字）

### 概要

- 是版为 `1.3.0` 之补，正诸圣遗物中杖外诸件不能右击启强化之失。

### 更新

- 正冠、镜、甲、履，皆得以右击开强化之画
- 改圣遗物定义内 `opensUpgradeScreen` 之常值为开，以杜后来新装备复遗之弊
- 理强化画面初置之路，不复存杖专用之名与假设
