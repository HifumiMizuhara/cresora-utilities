package hifumi.cresora

import net.minecraft.util.math.random.Random

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
}
