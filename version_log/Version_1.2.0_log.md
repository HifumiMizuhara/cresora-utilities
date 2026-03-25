# Version 1.2.0 Log

Date: 2026-03-25
Target: Minecraft 1.21.7 / Fabric Loader 0.18.4
Supported Languages: Japanese, English, Simplified Chinese, Literary Chinese, Literary Chinese (Simplified Script)

## Japanese

### 概要

- このバージョンでは、装備成長、戦闘表示、冒険ランク、Trinkets 連携をまとめて1つの更新単位として整理しました。

### 更新内容

- Minecraft `1.21.7` へ移行し、Fabric / Fabric API / Loader の対応を更新
- 装備データ基盤を `equipment_data` ベースへ再構築
- 強化対象を単一の杖から汎用装備システムへ拡張
- Trinkets Canary を導入し、装備スロット連携を追加
- `/cresora_stats` を一般ユーザー向けのステータス確認コマンドへ変更
- 10種類のステータスを常時表示するように整理
- 会心率 `5%`、会心ダメージ `50%` の基礎値を追加
- 会心時のアクションバー表示と効果音を追加
- モブドロップをソース別の傾向付きレアリティ制へ変更
- 冒険ランクシステムを追加し、上限を `70` まで拡張
- 敵HPの上限を `500` に制限し、超過分を防御値へ変換
- 敵の頭上に `Lv` と現在HPを表示
- 与えたダメージの浮遊表示を追加
- 「ひな形」シリーズを5部位化
- 部位: 杖 / 帽子 / メガネ / 鎧 / 靴
- 各部位に異なる主効果・副効果の生成傾向を設定
- ひな形シリーズのセット効果を追加
- 2セット効果: 攻撃力 `+5%`
- 4セット効果: 攻撃力 `+10%`、会心ダメージ `+10%`
- モブのランク決定を「周囲最高ランク」から「最寄りプレイヤーのランク」へ修正

## English

### Summary

- This version consolidates the equipment growth overhaul, combat display work, adventure-rank progression, and Trinkets integration into one release milestone.

### Changes

- Migrated the project to Minecraft `1.21.7` and updated Fabric-related dependencies
- Rebuilt the equipment foundation around the `equipment_data` component
- Expanded upgrades from a single wand flow into a generalized equipment system
- Added Trinkets Canary integration and wearable slot support
- Converted `/cresora_stats` into a player-facing stat overview command
- Ensured all 10 supported stat buffs are always visible
- Added base combat values of `5%` CRIT Rate and `50%` CRIT Damage
- Added action-bar crit feedback and crit-hit sound cues
- Reworked hostile drops into source-biased rarity profiles
- Added the Adventure Rank system and extended its cap to `70`
- Clamped hostile HP to `500` and redirected overflow into defense scaling
- Added hostile overhead labels for `Lv` and live HP
- Added floating damage numbers on successful hits
- Expanded the Hinagata series into five artifact slots
- Slots: Wand / Hat / Glasses / Armor / Boots
- Gave each slot its own main-stat and sub-stat roll bias
- Added Hinagata set bonuses
- 2-piece bonus: `ATK +5%`
- 4-piece bonus: `ATK +10%`, `CRIT DMG +10%`
- Changed hostile rank assignment from “highest nearby player” to “nearest nearby player”

## 简体中文

### 概要

- 本版本将装备成长重构、战斗显示、冒险等级系统与 Trinkets 联动整合为一次正式更新。

### 更新内容

- 迁移至 Minecraft `1.21.7`，并更新 Fabric 相关依赖
- 以 `equipment_data` 组件为核心重构装备数据基础
- 将强化系统从单一权杖扩展为通用装备系统
- 接入 Trinkets Canary，并增加可穿戴槽位支持
- 将 `/cresora_stats` 改为面向普通玩家的状态查看指令
- 保证 10 种支持的属性加成始终可见
- 增加基础暴击率 `5%` 与基础暴击伤害 `50%`
- 增加暴击动作栏反馈与暴击音效
- 将怪物掉落改为按来源倾向区分的稀有度体系
- 新增冒险等级系统，并将上限扩展至 `70`
- 将敌人生命上限限制为 `500`，超出部分转为防御成长
- 在敌人头顶显示 `Lv` 与当前生命值
- 为成功命中的攻击增加浮动伤害数字
- 将“雏形”系列扩展为 5 个装备部位
- 部位: 权杖 / 帽子 / 眼镜 / 护甲 / 靴子
- 为各部位设置不同的主词条与副词条生成倾向
- 新增雏形系列套装效果
- 2件套效果: 攻击力 `+5%`
- 4件套效果: 攻击力 `+10%`，暴击伤害 `+10%`
- 将敌人等级判定从“附近最高等级玩家”修正为“最近玩家等级”

## 文言

### 概要

- 是版統整裝備成長之改作、戰鬥顯示、冒險等級與 Trinkets 連動，別為一版之紀錄。

### 更新

- 遷於 Minecraft `1.21.7`，並更新 Fabric 諸依賴
- 以 `equipment_data` 爲本，重構裝備資料之基
- 使強化之制由一杖而推爲通用裝備系
- 加入 Trinkets Canary，增設可裝備之槽位
- 改 `/cresora_stats` 爲衆玩家可閱之狀態命令
- 使十種屬性增益皆恆可見
- 增會心率基值 `5%`、會心傷害基值 `50%`
- 增會心時之動作欄顯示與音效
- 改怪物掉落爲依來源偏向之稀有制
- 增冒險等級之系，且上限至 `70`
- 限敵生命於 `500`，其溢者轉爲防禦增幅
- 於敵首上示其 `Lv` 與現有生命
- 凡攻擊命中者，增浮動傷害數字
- 擴「雛形」系列爲五部裝備
- 部位: 杖 / 冠 / 鏡 / 甲 / 履
- 各部位皆別其主效副效生成之偏向
- 增雛形系列套裝之效
- 二件之效: 攻 `+5%`
- 四件之效: 攻 `+10%`、會傷 `+10%`
- 改敵等決定之法，自「近旁最高等玩家」爲「最近之玩家」

## 文言（简体字）

### 概要

- 是版统整装备成长之改作、战斗显示、冒险等级与 Trinkets 连动，别为一版之纪录。

### 更新

- 迁于 Minecraft `1.21.7`，并更新 Fabric 诸依赖
- 以 `equipment_data` 为本，重构装备资料之基
- 使强化之制由一杖而推为通用装备系
- 加入 Trinkets Canary，增设可装备之槽位
- 改 `/cresora_stats` 为众玩家可阅之状态命令
- 使十种属性增益皆恒可见
- 增会心率基值 `5%`、会心伤害基值 `50%`
- 增会心时之动作栏显示与音效
- 改怪物掉落为依来源偏向之稀有制
- 增冒险等级之系，且上限至 `70`
- 限敌生命于 `500`，其溢者转为防御增幅
- 于敌首上示其 `Lv` 与现有生命
- 凡攻击命中者，增浮动伤害数字
- 扩「雏形」系列为五部装备
- 部位: 杖 / 冠 / 镜 / 甲 / 履
- 各部位皆别其主效副效生成之偏向
- 增雏形系列套装之效
- 二件之效: 攻 `+5%`
- 四件之效: 攻 `+10%`、会伤 `+10%`
- 改敌等决定之法，自「近旁最高等玩家」为「最近之玩家」
