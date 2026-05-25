package hifumi.cresora.combat
import com.mojang.serialization.Codec

enum class CombatDamageType(val id: String) {
    PHYSICAL("physical"),
    ARCANE("arcane");

    fun translationKey(): String = "item.cresora.weapon.damage_type.$id"

    companion object {
        private val BY_ID = entries.associateBy(CombatDamageType::id)

        val CODEC: Codec<CombatDamageType> = Codec.STRING.xmap(
            { id -> BY_ID[id] ?: throw IllegalArgumentException("Unknown combat damage type: $id") },
            CombatDamageType::id
        )
    }
}
