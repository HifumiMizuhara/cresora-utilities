package hifumi.cresora.equipment
import net.minecraft.util.math.random.Random
import hifumi.cresora.StatType
import hifumi.cresora.StatEntry

object EquipmentUpgradeService {
    fun applyLevels(data: EquipmentData, levelGain: Int, random: Random): EquipmentData {
        var updated = data.normalized()
        val targetLevel = (updated.level + levelGain).coerceAtMost(updated.rarity.maxLevel)

        while (updated.level < targetLevel) {
            updated = updated.copy(
                level = updated.level + 1,
                mainStat = updated.mainStat.add(
                    EquipmentGenerationService.rollMainStatIncrease(
                        updated.mainStat.type,
                        updated.rarity,
                        random
                    )
                )
            )

            if (updated.level % 4 == 0) {
                updated = applyGrowthEvent(updated, random)
            }
        }

        return updated.normalized()
    }

    fun applyGrowthEvent(data: EquipmentData, random: Random): EquipmentData {
        if (data.subStats.size < data.rarity.subStatLimit) {
            return data.copy(
                subStats = data.subStats + EquipmentGenerationService.rollNewSubStat(data, random),
                upgradeCount = data.upgradeCount + 1
            ).normalized()
        }

        if (data.subStats.isEmpty()) {
            return data.copy(upgradeCount = data.upgradeCount + 1).normalized()
        }

        val growthIndex = random.nextInt(data.subStats.size)
        val updatedSubStats = data.subStats.toMutableList()
        val targetEntry = updatedSubStats[growthIndex]
        updatedSubStats[growthIndex] = targetEntry.add(
            EquipmentGenerationService.rollSubStatIncrease(targetEntry.type, data.rarity, random)
        )
        return data.copy(
            subStats = updatedSubStats,
            upgradeCount = data.upgradeCount + 1
        ).normalized()
    }

    fun rerollToMaxWithPrioritySubStat(
        definition: EquipmentDefinition,
        rarity: EquipmentRarity,
        priorityCandidates: List<StatType>,
        random: Random
    ): EquipmentData {
        val normalizedCandidates = priorityCandidates.distinct().take(2)
        require(normalizedCandidates.size == 2) { "Beta reforge requires exactly two distinct priority stat candidates" }
        val guaranteedType = normalizedCandidates[random.nextInt(normalizedCandidates.size)]

        var updated = EquipmentGenerationService.createEquipment(
            random = random,
            definition = definition,
            forcedRarity = rarity,
            blockedMainStatTypes = setOf(guaranteedType)
        )
        updated = ensureSubStatPresent(updated, guaranteedType, random)

        var forcedHitsRemaining = minOf(2, rarity.maxLevel / 4)
        while (updated.level < rarity.maxLevel) {
            updated = updated.copy(
                level = updated.level + 1,
                mainStat = updated.mainStat.add(
                    EquipmentGenerationService.rollMainStatIncrease(
                        updated.mainStat.type,
                        updated.rarity,
                        random
                    )
                )
            )

            if (updated.level % 4 == 0) {
                updated = if (forcedHitsRemaining > 0) {
                    forcedHitsRemaining--
                    applyPriorityGrowthEvent(updated, guaranteedType, random)
                } else {
                    applyGrowthEvent(updated, random)
                }
            }
        }

        updated = fillMissingSubStats(updated, random)
        return updated.normalized()
    }

    private fun ensureSubStatPresent(data: EquipmentData, type: StatType, random: Random): EquipmentData {
        if (data.mainStat.type == type || data.subStats.any { it.type == type }) {
            return data
        }

        val replacement = StatEntry(type, EquipmentGenerationService.rollSubStatIncrease(type, data.rarity, random))
        if (data.subStats.isEmpty()) {
            return data.copy(subStats = listOf(replacement)).normalized()
        }

        val updatedSubStats = data.subStats.toMutableList()
        updatedSubStats[random.nextInt(updatedSubStats.size)] = replacement
        return data.copy(subStats = updatedSubStats).normalized()
    }

    private fun applyPriorityGrowthEvent(data: EquipmentData, type: StatType, random: Random): EquipmentData {
        val targetIndex = data.subStats.indexOfFirst { it.type == type }
        if (targetIndex < 0) {
            return applyGrowthEvent(ensureSubStatPresent(data, type, random), random)
        }

        val updatedSubStats = data.subStats.toMutableList()
        val targetEntry = updatedSubStats[targetIndex]
        updatedSubStats[targetIndex] = targetEntry.add(
            EquipmentGenerationService.rollSubStatIncrease(targetEntry.type, data.rarity, random)
        )
        return data.copy(
            subStats = updatedSubStats,
            upgradeCount = data.upgradeCount + 1
        ).normalized()
    }

    private fun fillMissingSubStats(data: EquipmentData, random: Random): EquipmentData {
        var updated = data.normalized()
        while (updated.subStats.size < updated.rarity.subStatLimit) {
            updated = updated.copy(
                subStats = updated.subStats + EquipmentGenerationService.rollNewSubStat(updated, random)
            ).normalized()
        }
        return updated
    }
}
