package hifumi.cresora.equipment
import hifumi.cresora.StatType
import com.mojang.serialization.Codec
import com.mojang.serialization.codecs.RecordCodecBuilder

data class EquipmentDropProfile(
    val id: String,
    private val mainWeights: Map<StatType, Double> = emptyMap(),
    private val subWeights: Map<StatType, Double> = emptyMap()
) {
    fun mainWeight(type: StatType): Double = mainWeights[type] ?: 1.0

    fun subWeight(type: StatType): Double = subWeights[type] ?: 1.0

    companion object {
        val CODEC: Codec<EquipmentDropProfile> = RecordCodecBuilder.create { instance ->
            instance.group(
                Codec.STRING.fieldOf("id").forGetter(EquipmentDropProfile::id),
                Codec.unboundedMap(StatType.CODEC, Codec.DOUBLE)
                    .optionalFieldOf("mainWeights", emptyMap())
                    .forGetter { StatType.entries.associateWith(it::mainWeight).filterValues { value -> value != 1.0 } },
                Codec.unboundedMap(StatType.CODEC, Codec.DOUBLE)
                    .optionalFieldOf("subWeights", emptyMap())
                    .forGetter { StatType.entries.associateWith(it::subWeight).filterValues { value -> value != 1.0 } }
            ).apply(instance, ::EquipmentDropProfile)
        }
    }
}
