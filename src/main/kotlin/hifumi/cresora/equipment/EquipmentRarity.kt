package hifumi.cresora.equipment
import com.mojang.serialization.Codec

enum class EquipmentRarity(
    val id: String,
    val maxLevel: Int,
    val initialSubStatCount: Int,
    val subStatLimit: Int
) {
    THREE_STAR("3_star", 8, 1, 2),
    FOUR_STAR("4_star", 12, 2, 3),
    FIVE_STAR("5_star", 16, 3, 4);

    fun translationKey(): String = "item.cresora.equipment.rarity.$id"

    companion object {
        private val BY_ID = entries.associateBy(EquipmentRarity::id)

        val CODEC: Codec<EquipmentRarity> = Codec.STRING.xmap(
            { id -> BY_ID[id] ?: throw IllegalArgumentException("Unknown equipment rarity: $id") },
            EquipmentRarity::id
        )
    }
}
