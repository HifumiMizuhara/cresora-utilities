package hifumi.cresora

import com.mojang.serialization.Codec

data class EquipmentSetBonus(
    val requiredPieces: Int,
    val stats: List<StatEntry>
)

enum class EquipmentSet(
    val id: String,
    private val bonuses: List<EquipmentSetBonus>
) {
    HINAGATA(
        id = "hinagata",
        bonuses = listOf(
            EquipmentSetBonus(
                requiredPieces = 2,
                stats = listOf(StatEntry(StatType.ATK_PERCENT, 5.0))
            ),
            EquipmentSetBonus(
                requiredPieces = 4,
                stats = listOf(
                    StatEntry(StatType.ATK_PERCENT, 10.0),
                    StatEntry(StatType.CRIT_DMG, 10.0)
                )
            )
        )
    );

    fun translationKey(): String = "item.cresora.equipment.set.$id"

    fun activeBonuses(pieceCount: Int): List<StatEntry> {
        return bonuses
            .filter { pieceCount >= it.requiredPieces }
            .flatMap(EquipmentSetBonus::stats)
    }

    companion object {
        private val BY_ID = entries.associateBy(EquipmentSet::id)

        val CODEC: Codec<EquipmentSet> = Codec.STRING.xmap(
            { id -> BY_ID[id] ?: throw IllegalArgumentException("Unknown equipment set: $id") },
            EquipmentSet::id
        )
    }
}
