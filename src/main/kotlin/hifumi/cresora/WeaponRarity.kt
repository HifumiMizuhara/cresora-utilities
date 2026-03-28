package hifumi.cresora

import com.mojang.serialization.Codec

enum class WeaponRarity(
    val id: String,
    val stars: Int
) {
    TWO_STAR("2_star", 2),
    THREE_STAR("3_star", 3),
    FOUR_STAR("4_star", 4),
    FIVE_STAR("5_star", 5);

    fun translationKey(): String = "item.cresora.weapon.rarity.$id"

    companion object {
        val CODEC: Codec<WeaponRarity> = Codec.STRING.xmap(
            { value -> entries.firstOrNull { it.id == value } ?: error("Unknown weapon rarity: $value") },
            WeaponRarity::id
        )
    }
}
