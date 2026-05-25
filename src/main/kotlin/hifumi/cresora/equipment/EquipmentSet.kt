package hifumi.cresora.equipment
import hifumi.cresora.StatEntry
import com.mojang.serialization.Codec
import com.mojang.serialization.codecs.RecordCodecBuilder

data class EquipmentEffectHook(
    val trigger: EquipmentEffectTrigger,
    val effectId: String,
    val parameters: Map<String, String> = emptyMap()
) {
    companion object {
        val CODEC: Codec<EquipmentEffectHook> = RecordCodecBuilder.create { instance ->
            instance.group(
                EquipmentEffectTrigger.CODEC.fieldOf("trigger").forGetter(EquipmentEffectHook::trigger),
                Codec.STRING.fieldOf("effectId").forGetter(EquipmentEffectHook::effectId),
                Codec.unboundedMap(Codec.STRING, Codec.STRING)
                    .optionalFieldOf("parameters", emptyMap())
                    .forGetter(EquipmentEffectHook::parameters)
            ).apply(instance, ::EquipmentEffectHook)
        }
    }
}

enum class EquipmentEffectTrigger(val id: String) {
    EQUIP_CHANGED("equip_changed"),
    ATTACK_DEALT("attack_dealt"),
    DAMAGE_TAKEN("damage_taken"),
    KILL("kill"),
    TICK("tick");

    companion object {
        private val BY_ID = entries.associateBy(EquipmentEffectTrigger::id)

        val CODEC: Codec<EquipmentEffectTrigger> = Codec.STRING.xmap(
            { id -> BY_ID[id] ?: throw IllegalArgumentException("Unknown equipment effect trigger: $id") },
            EquipmentEffectTrigger::id
        )
    }
}

fun EquipmentEffectHook.descriptionKey(): String? {
    return parameters["descriptionKey"]
}

data class EquipmentSetBonus(
    val requiredPieces: Int,
    val descriptionKey: String? = null,
    val stats: List<StatEntry> = emptyList(),
    val effectHooks: List<EquipmentEffectHook> = emptyList()
) {
    companion object {
        val CODEC: Codec<EquipmentSetBonus> = RecordCodecBuilder.create { instance ->
            instance.group(
                Codec.INT.fieldOf("requiredPieces").forGetter(EquipmentSetBonus::requiredPieces),
                Codec.STRING.optionalFieldOf("descriptionKey")
                    .forGetter { java.util.Optional.ofNullable(it.descriptionKey) },
                StatEntry.CODEC.listOf().optionalFieldOf("stats", emptyList()).forGetter(EquipmentSetBonus::stats),
                EquipmentEffectHook.CODEC.listOf().optionalFieldOf("effectHooks", emptyList()).forGetter(EquipmentSetBonus::effectHooks)
            ).apply(instance) { requiredPieces, descriptionKey, stats, effectHooks ->
                EquipmentSetBonus(requiredPieces, descriptionKey.orElse(null), stats, effectHooks)
            }
        }
    }
}

data class EquipmentSet(
    val id: String,
    private val translationKeyId: String,
    private val bonuses: List<EquipmentSetBonus>
) {
    fun translationKey(): String = translationKeyId

    fun activeBonuses(pieceCount: Int): List<EquipmentSetBonus> {
        return bonuses.filter { pieceCount >= it.requiredPieces }
    }

    fun allBonuses(): List<EquipmentSetBonus> = bonuses.sortedBy(EquipmentSetBonus::requiredPieces)

    companion object {
        val CODEC: Codec<EquipmentSet> = RecordCodecBuilder.create { instance ->
            instance.group(
                Codec.STRING.fieldOf("id").forGetter(EquipmentSet::id),
                Codec.STRING.fieldOf("translationKey").forGetter { it.translationKeyId },
                EquipmentSetBonus.CODEC.listOf().fieldOf("bonuses").forGetter { it.bonuses }
            ).apply(instance, ::EquipmentSet)
        }
    }
}
