package hifumi.cresora.equipment
import hifumi.cresora.StatEntry
import com.mojang.serialization.Codec
import com.mojang.serialization.codecs.RecordCodecBuilder

data class EquipmentData(
    val rarity: EquipmentRarity,
    val level: Int,
    val mainStat: StatEntry,
    val subStats: List<StatEntry>,
    val upgradeCount: Int,
    val slotTypeId: String = "wand",
    val setId: String = "hinagata"
) {
    fun normalized(): EquipmentData {
        val normalizedMainStat = mainStat.normalized()
        val normalizedSubStats = subStats
            .map(StatEntry::normalized)
            .filterNot { it.type == normalizedMainStat.type }
            .distinctBy(StatEntry::type)
            .take(rarity.subStatLimit)
        val normalizedLevel = level.coerceIn(0, rarity.maxLevel)
        val normalizedUpgradeCount = upgradeCount.coerceIn(normalizedLevel / 4, normalizedLevel / 4)

        return copy(
            level = normalizedLevel,
            mainStat = normalizedMainStat,
            subStats = normalizedSubStats,
            upgradeCount = normalizedUpgradeCount,
            slotTypeId = slotTypeId.ifBlank { "wand" },
            setId = setId.ifBlank { "hinagata" }
        )
    }

    companion object {
        val CODEC: Codec<EquipmentData> = RecordCodecBuilder.create { instance ->
            instance.group(
                EquipmentRarity.CODEC.fieldOf("rarity").forGetter(EquipmentData::rarity),
                Codec.INT.fieldOf("level").forGetter(EquipmentData::level),
                StatEntry.CODEC.fieldOf("mainStat").forGetter(EquipmentData::mainStat),
                StatEntry.CODEC.listOf().fieldOf("subStats").forGetter(EquipmentData::subStats),
                Codec.INT.fieldOf("upgradeCount").forGetter(EquipmentData::upgradeCount),
                Codec.STRING.optionalFieldOf("slotType", "wand").forGetter(EquipmentData::slotTypeId),
                Codec.STRING.optionalFieldOf("setId", "hinagata").forGetter(EquipmentData::setId)
            ).apply(instance, ::EquipmentData)
        }
    }
}
