---
description: 新武器やアイテムのテクスチャ・素材断片テクスチャの生成、切り抜き、およびゲーム内への実装手順。
---

# 武器・アイテムのカスタムテクスチャ生成とゲーム内実装

このワークフローは、新規武器やアイテム（およびその素材断片）のテクスチャを画像生成AIで生成し、Minecraft用にトリミング・リサイズ・回転処理を行ってゲーム内に実装する一連の手順を示します。

## 1. 概念図・テクスチャ画像の生成 (generate_image)
`generate_image` ツールを使用して、武器または素材断片のベースとなる概念図を生成します。

* **武器（剣など）のプロンプト例**:
  > A clean illustration of a Minecraft-style magic sword. It is a slender, elegant blade with a romantic, pink and violet color palette, adorned with small heart and crystal details on the hilt. Isolated on a solid black background.
* **素材断片（クリスタルなど）のプロンプト例**:
  > A clean illustration of a Minecraft-style crystal shard fragment. It is a glowing pink and violet gem or shard, shaped like a small heart-shaped crystal shard, isolated on a solid black background.

> [!TIP]
> 背景を単一の純黒（`solid black background`）に指定すると、後の自動背景透過処理が非常に容易になります。

---

## 2. Pythonスクリプトによる背景透過・回転・リサイズ処理
生成された画像は高解像度（通常1024x1024等）であり、武器の場合は垂直方向に直立していることが多いため、以下の処理を行います：
1. **背景の透過**: 黒に近いピクセル（RGB値が一定しきい値以下）をアルファ値 `0` に変換します。
2. **武器の回転**: Minecraftのツール（剣など）は斜め（左下から右上）に持つため、時計回りに `45度`（`-45度`）回転させます。
3. **リサイズとピクセル化**: 解像度を落とします。
   - **武器テクスチャ**: `32x32`（詳細度を高めたい場合）または `16x16` に `NEAREST` 補間法でリサイズ。
   - **断片・素材テクスチャ**: `16x16` にリサイズ。
4. **出力先**:
   - `src/main/resources/assets/cresora-utilities/textures/item/<weapon_id>.png`
   - `src/main/resources/assets/cresora-utilities/textures/item/<weapon_id>_fragment.png`

### 汎用画像処理スクリプト（Python / Pillow）
以下のスクリプト（`process_textures.py`）を一時的に作成して実行してください：

```python
import os
from PIL import Image

def process_image(in_path, out_path, size=16, rotate_angle=0):
    img = Image.open(in_path).convert("RGBA")
    
    # 黑色背景过滤
    datas = img.getdata()
    newData = []
    for item in datas:
        if item[0] < 30 and item[1] < 30 and item[2] < 30:
            newData.append((0, 0, 0, 0))
        else:
            newData.append(item)
    img.putdata(newData)
    
    bbox = img.getbbox()
    if not bbox:
        return
    cropped = img.crop(bbox)
    
    if rotate_angle != 0:
        cropped = cropped.rotate(rotate_angle, expand=True, resample=Image.Resampling.BICUBIC)
        r_bbox = cropped.getbbox()
        if r_bbox:
            cropped = cropped.crop(r_bbox)
            
    # 留白尺寸比例控制
    target_size = size - 4 if size > 16 else size - 2
    w, h = cropped.size
    ratio = min(target_size / w, target_size / h)
    new_w, new_h = int(w * ratio), int(h * ratio)
    
    resized = cropped.resize((new_w, new_h), Image.Resampling.NEAREST)
    
    final_img = Image.new("RGBA", (size, size), (0, 0, 0, 0))
    offset_x = (size - new_w) // 2
    offset_y = (size - new_h) // 2
    final_img.paste(resized, (offset_x, offset_y), resized)
    
    os.makedirs(os.path.dirname(out_path), exist_ok=True)
    final_img.save(out_path, "PNG")
    print(f"Processed: {out_path}")

# 使用例:
# process_image("sword_in.png", "src/main/resources/assets/cresora-utilities/textures/item/my_sword.png", size=32, rotate_angle=-45)
# process_image("frag_in.png", "src/main/resources/assets/cresora-utilities/textures/item/my_sword_fragment.png", size=16)
```

---

## 3. CWC（Cresora Weapon Compiler）への結合とビルド
コンパイラはアセットが textures ディレクトリ配下に存在しているかどうかを検出し、自動的にモデルの JSON にバインドします。

1. **アセットのコンパイル**:
   ```bash
   GRADLE_USER_HOME=.gradle-user ./gradlew compileAssets --console=plain
   ```
   コンパイラ `CresoraCompiler` は、`${weapon.id}.png` および `${weapon.id}_fragment.png` が存在する場合、自動的に `models/item/${weapon.id}.json` と `models/item/${weapon.id}_fragment.json` を更新し、カスタムテクスチャを割り当てます。

2. **クラスのビルドと整合性検証**:
   ```bash
   GRADLE_USER_HOME=.gradle-user ./gradlew classes --console=plain
   ```
   ビルド全体がエラーなく通ることを確認します。

3. **ドキュメントの記録**:
   - `WORK_DONE.md` に実施した内容を追記してください。
