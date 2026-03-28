package hifumi.cresora

import com.mojang.serialization.Codec
import com.mojang.serialization.codecs.RecordCodecBuilder

data class EquipmentSlotType(
    val id: String,
    private val translationKeyId: String,
    val trinketGroup: String,
    val trinketSlot: String,
    val mainStatCandidates: List<StatType>,
    private val mainWeights: Map<StatType, Double> = emptyMap(),
    private val subWeights: Map<StatType, Double> = emptyMap(),
    private val defaultMainStatType: StatType
) {
    fun translationKey(): String = translationKeyId

    fun mainWeight(type: StatType): Double = mainWeights[type] ?: 1.0

    fun subWeight(type: StatType): Double = subWeights[type] ?: 1.0

    fun defaultMainStat(): StatType = defaultMainStatType

    companion object {
        val CODEC: Codec<EquipmentSlotType> = RecordCodecBuilder.create { instance ->
            instance.group(
                Codec.STRING.fieldOf("id").forGetter(EquipmentSlotType::id),
                Codec.STRING.fieldOf("translationKey").forGetter { it.translationKeyId },
                Codec.STRING.fieldOf("trinketGroup").forGetter(EquipmentSlotType::trinketGroup),
                Codec.STRING.fieldOf("trinketSlot").forGetter(EquipmentSlotType::trinketSlot),
                StatType.CODEC.listOf().fieldOf("mainStatCandidates").forGetter(EquipmentSlotType::mainStatCandidates),
                Codec.unboundedMap(StatType.CODEC, Codec.DOUBLE)
                    .optionalFieldOf("mainWeights", emptyMap())
                    .forGetter { it.mainStatCandidates.associateWith(it::mainWeight).filterValues { value -> value != 1.0 } },
                Codec.unboundedMap(StatType.CODEC, Codec.DOUBLE)
                    .optionalFieldOf("subWeights", emptyMap())
                    .forGetter { StatType.entries.associateWith(it::subWeight).filterValues { value -> value != 1.0 } },
                StatType.CODEC.fieldOf("defaultMainStat").forGetter { it.defaultMainStatType }
            ).apply(instance, ::EquipmentSlotType)
        }
    }
}
