package hifumi.cresora.leyline

import net.minecraft.util.StringIdentifiable

enum class LeyLineElement(
    val id: String,
    val colorName: String,
    val translationKeyId: String,
    val blockModelPath: String
) : StringIdentifiable {
    SUN("sun", "亮黄色", "item.cresora-utilities.sun_key", "minecraft:block/yellow_concrete"),
    MOON("moon", "灰色", "item.cresora-utilities.moon_key", "minecraft:block/gray_concrete"),
    FIRE("fire", "金红色", "item.cresora-utilities.fire_key", "minecraft:block/red_concrete"),
    WATER("water", "蓝色", "item.cresora-utilities.water_key", "minecraft:block/blue_concrete"),
    WOOD("wood", "棕色", "item.cresora-utilities.wood_key", "minecraft:block/brown_concrete"),
    GOLD("gold", "金色", "item.cresora-utilities.gold_key", "minecraft:block/gold_block"),
    EARTH("earth", "土黄色", "item.cresora-utilities.earth_key", "minecraft:block/yellow_terracotta");

    override fun asString(): String = id

    companion object {
        fun fromId(id: String): LeyLineElement? = entries.firstOrNull { it.id == id }
    }
}
