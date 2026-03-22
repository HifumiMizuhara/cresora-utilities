package hifumi.cresora

import net.minecraft.item.ItemStack
import net.minecraft.util.math.random.Random

object EquipmentStackSupport {
    fun getEquipmentData(stack: ItemStack): EquipmentData? {
        val existing = stack.get(ModDataComponents.EQUIPMENT_DATA)?.normalized()
        if (existing != null) {
            return existing
        }

        // Backfill legacy or command-given pendants that only carry the old level component.
        if (stack.isOf(CreSoraUtilities.STRENGTH_PENDANT)) {
            val legacyLevel = stack.getOrDefault(ModDataComponents.LEVEL, 0).coerceAtLeast(0)
            return syncPendantData(stack, defaultPendantData(legacyLevel))
        }

        return null
    }

    fun getCompatibilityLevel(stack: ItemStack): Int {
        return getEquipmentData(stack)?.level ?: stack.getOrDefault(ModDataComponents.LEVEL, 0).coerceAtLeast(0)
    }

    fun defaultPendantData(level: Int = 1): EquipmentData {
        val normalizedLevel = level.coerceAtLeast(0)
        return EquipmentData(
            rarity = legacyRarityForLevel(normalizedLevel),
            level = normalizedLevel,
            mainStat = StatEntry(StatType.ATK_FLAT, 3.0 + normalizedLevel * 1.4),
            subStats = emptyList(),
            upgradeCount = normalizedLevel / 4
        ).normalized()
    }

    fun ensurePendantData(stack: ItemStack, random: Random? = null): EquipmentData {
        val existing = getEquipmentData(stack)
        if (existing != null) {
            return syncPendantData(stack, existing)
        }

        val legacyLevel = stack.getOrDefault(ModDataComponents.LEVEL, 0).coerceAtLeast(0)
        val generated = when {
            random != null -> EquipmentGenerationService.createPendant(
                random = random,
                startingLevel = legacyLevel,
                forcedRarity = legacyRarityForLevel(legacyLevel)
            )
            else -> defaultPendantData(legacyLevel)
        }
        return syncPendantData(stack, generated)
    }

    fun syncPendantData(stack: ItemStack, data: EquipmentData): EquipmentData {
        val normalized = data.normalized()
        stack.set(ModDataComponents.EQUIPMENT_DATA, normalized)
        stack.set(ModDataComponents.LEVEL, normalized.level)
        return normalized
    }

    fun syncPendantData(stack: ItemStack, level: Int = getCompatibilityLevel(stack), random: Random? = null): EquipmentData {
        val currentData = getEquipmentData(stack)
        val synced = if (currentData != null) {
            if (currentData.level == level.coerceAtLeast(0)) {
                currentData.normalized()
            } else if (random != null && level > currentData.level) {
                EquipmentUpgradeService.applyLevels(currentData, level - currentData.level, random)
            } else {
                currentData.copy(level = level.coerceAtLeast(0)).normalized()
            }
        } else {
            ensurePendantData(stack, random)
        }

        stack.set(ModDataComponents.EQUIPMENT_DATA, synced)
        stack.set(ModDataComponents.LEVEL, synced.level)
        return synced
    }

    private fun legacyRarityForLevel(level: Int): EquipmentRarity {
        return when {
            level <= EquipmentRarity.THREE_STAR.maxLevel -> EquipmentRarity.THREE_STAR
            level <= EquipmentRarity.FOUR_STAR.maxLevel -> EquipmentRarity.FOUR_STAR
            else -> EquipmentRarity.FIVE_STAR
        }
    }
}
