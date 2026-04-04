package hifumi.cresora

import com.mojang.serialization.Codec
import net.minecraft.item.Item
import net.minecraft.item.Items

enum class ResonanceCurrencyType(
    val id: String,
    val translationKey: String,
    private val iconItem: Item
) {
    CHORD_PROGRESSION(
        id = "chord_progression",
        translationKey = "item.cresora-utilities.chord_progression",
        iconItem = Items.PAPER
    ),
    SUBSTITUTE_CHORD(
        id = "substitute_chord",
        translationKey = "item.cresora-utilities.substitute_chord",
        iconItem = Items.NAME_TAG
    );

    fun icon(): Item = iconItem

    companion object {
        val CODEC: Codec<ResonanceCurrencyType> = Codec.STRING.xmap(
            { value -> entries.firstOrNull { it.id == value } ?: error("Unknown resonance currency type: $value") },
            ResonanceCurrencyType::id
        )

        fun byId(id: String): ResonanceCurrencyType {
            return entries.firstOrNull { it.id == id } ?: error("Unknown resonance currency type: $id")
        }
    }
}
