package hifumi.cresora

import net.minecraft.item.Item
import net.minecraft.item.ItemStack
import net.minecraft.util.math.random.Random

object EquipmentStackSupport {
    fun getDefinition(item: Item): EquipmentDefinition? {
        return when (item) {
            CreSoraUtilities.STRENGTH_PENDANT -> EquipmentDefinitions.HINAGATA_WAND
            CreSoraUtilities.HINAGATA_HAT -> EquipmentDefinitions.HINAGATA_HAT
            CreSoraUtilities.HINAGATA_GLASSES -> EquipmentDefinitions.HINAGATA_GLASSES
            CreSoraUtilities.HINAGATA_ARMOR -> EquipmentDefinitions.HINAGATA_ARMOR
            CreSoraUtilities.HINAGATA_BOOTS -> EquipmentDefinitions.HINAGATA_BOOTS
            else -> null
        }
    }

    fun getDefinition(stack: ItemStack): EquipmentDefinition? = getDefinition(stack.item)

    fun isEquipment(stack: ItemStack): Boolean = getDefinition(stack) != null

    fun getEquipmentData(stack: ItemStack): EquipmentData? {
        val definition = getDefinition(stack) ?: return null
        val existing = stack.get(ModDataComponents.EQUIPMENT_DATA)
            ?.copy(slotType = definition.slotType, setId = definition.setId)
            ?.normalized()
        if (existing != null) {
            return existing
        }

        val legacyLevel = stack.getOrDefault(ModDataComponents.LEVEL, 0).coerceAtLeast(0)
        return syncEquipmentData(stack, defaultEquipmentData(definition, legacyLevel))
    }

    fun getCompatibilityLevel(stack: ItemStack): Int {
        return getEquipmentData(stack)?.level ?: stack.getOrDefault(ModDataComponents.LEVEL, 0).coerceAtLeast(0)
    }

    fun defaultEquipmentData(definition: EquipmentDefinition, level: Int = 1): EquipmentData {
        val normalizedLevel = level.coerceAtLeast(0)
        return EquipmentData(
            rarity = rarityForLevel(normalizedLevel),
            level = normalizedLevel,
            mainStat = StatEntry(
                definition.slotType.defaultMainStat(),
                defaultMainStatValue(definition.slotType.defaultMainStat(), normalizedLevel)
            ),
            subStats = emptyList(),
            upgradeCount = normalizedLevel / 4,
            slotType = definition.slotType,
            setId = definition.setId
        ).normalized()
    }

    fun defaultPendantData(level: Int = 1): EquipmentData {
        return defaultEquipmentData(EquipmentDefinitions.HINAGATA_WAND, level)
    }

    fun ensureEquipmentData(stack: ItemStack, random: Random? = null): EquipmentData {
        val definition = getDefinition(stack) ?: error("Non-equipment stack cannot receive equipment data: ${stack.item}")
        val existing = getEquipmentData(stack)
        if (existing != null) {
            return syncEquipmentData(stack, existing)
        }

        val legacyLevel = stack.getOrDefault(ModDataComponents.LEVEL, 0).coerceAtLeast(0)
        val generated = when {
            random != null -> EquipmentGenerationService.createEquipment(
                random = random,
                definition = definition,
                startingLevel = legacyLevel,
                forcedRarity = rarityForLevel(legacyLevel)
            )
            else -> defaultEquipmentData(definition, legacyLevel)
        }
        return syncEquipmentData(stack, generated)
    }

    fun ensurePendantData(stack: ItemStack, random: Random? = null): EquipmentData {
        return ensureEquipmentData(stack, random)
    }

    fun syncEquipmentData(stack: ItemStack, data: EquipmentData): EquipmentData {
        val definition = getDefinition(stack) ?: return data.normalized()
        val normalized = data.copy(slotType = definition.slotType, setId = definition.setId).normalized()
        stack.set(ModDataComponents.EQUIPMENT_DATA, normalized)
        stack.set(ModDataComponents.LEVEL, normalized.level)
        return normalized
    }

    fun syncPendantData(stack: ItemStack, data: EquipmentData): EquipmentData {
        return syncEquipmentData(stack, data)
    }

    fun syncEquipmentData(stack: ItemStack, level: Int = getCompatibilityLevel(stack), random: Random? = null): EquipmentData {
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
            ensureEquipmentData(stack, random)
        }

        stack.set(ModDataComponents.EQUIPMENT_DATA, synced)
        stack.set(ModDataComponents.LEVEL, synced.level)
        return synced
    }

    fun syncPendantData(stack: ItemStack, level: Int = getCompatibilityLevel(stack), random: Random? = null): EquipmentData {
        return syncEquipmentData(stack, level, random)
    }

    fun rarityForLevel(level: Int): EquipmentRarity {
        return when {
            level <= EquipmentRarity.THREE_STAR.maxLevel -> EquipmentRarity.THREE_STAR
            level <= EquipmentRarity.FOUR_STAR.maxLevel -> EquipmentRarity.FOUR_STAR
            else -> EquipmentRarity.FIVE_STAR
        }
    }

    private fun defaultMainStatValue(type: StatType, level: Int): Double {
        return when (type) {
            StatType.ATK_FLAT -> 3.0 + level * 1.4
            StatType.ATK_PERCENT -> 2.1 + level * 0.7
            StatType.HP_FLAT -> 8.0 + level * 4.0
            StatType.HP_PERCENT -> 2.4 + level * 0.8
            StatType.DEF_FLAT -> 2.0 + level * 1.2
            StatType.DEF_PERCENT -> 2.1 + level * 0.7
            StatType.CRIT_RATE -> 1.4 + level * 0.45
            StatType.CRIT_DMG -> 2.8 + level * 0.8
            StatType.ALL_DMG_BONUS -> 1.8 + level * 0.7
            StatType.DAMAGE_REDUCTION -> 1.6 + level * 0.55
        }
    }
}
