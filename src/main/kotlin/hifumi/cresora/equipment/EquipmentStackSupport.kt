package hifumi.cresora.equipment
import hifumi.cresora.ModDataComponents
import hifumi.cresora.StatEntry
import hifumi.cresora.StatType
import net.minecraft.item.Item
import net.minecraft.item.ItemStack
import net.minecraft.util.math.random.Random

object EquipmentStackSupport {
    private val definitionsByItem: MutableMap<Item, EquipmentDefinitionRef> = linkedMapOf()
    private val itemsByDefinitionId: MutableMap<String, Item> = linkedMapOf()

    fun registerEquipmentItem(item: Item, definition: EquipmentDefinitionRef) {
        definitionsByItem[item] = definition
        itemsByDefinitionId[definition.id] = item
    }

    fun getDefinitionRef(item: Item): EquipmentDefinitionRef? = definitionsByItem[item]

    fun getDefinition(item: Item): EquipmentDefinition? = definitionsByItem[item]?.resolve()

    fun getDefinition(stack: ItemStack): EquipmentDefinition? = getDefinition(stack.item)

    fun itemForDefinitionId(definitionId: String): Item? = itemsByDefinitionId[definitionId]

    fun allEquipmentItems(): List<Item> = itemsByDefinitionId.values.toList()

    fun isEquipment(stack: ItemStack): Boolean = definitionsByItem.containsKey(stack.item)

    fun getEquipmentData(stack: ItemStack): EquipmentData? {
        val definition = getDefinition(stack) ?: return null
        val existing = stack.get(ModDataComponents.EQUIPMENT_DATA)
            ?.copy(slotTypeId = definition.slotTypeId, setId = definition.setId)
            ?.normalized()
        if (existing != null) {
            return syncEquipmentData(stack, EquipmentBalanceMigration.migrate(existing))
        }

        val legacyLevel = stack.getOrDefault(ModDataComponents.LEVEL, 0).coerceAtLeast(0)
        return syncEquipmentData(stack, defaultEquipmentData(definition, legacyLevel))
    }

    fun getCompatibilityLevel(stack: ItemStack): Int {
        return getEquipmentData(stack)?.level ?: stack.getOrDefault(ModDataComponents.LEVEL, 0).coerceAtLeast(0)
    }

    fun defaultEquipmentData(definition: EquipmentDefinition, level: Int = 1): EquipmentData {
        val normalizedLevel = level.coerceAtLeast(0)
        val slotType = definition.slotType()
        val defaultMainStat = slotType.defaultMainStat()
        return EquipmentData(
            rarity = rarityForLevel(normalizedLevel),
            level = normalizedLevel,
            mainStat = StatEntry(
                defaultMainStat,
                defaultMainStatValue(defaultMainStat, normalizedLevel)
            ),
            subStats = emptyList(),
            upgradeCount = normalizedLevel / 4,
            slotTypeId = definition.slotTypeId,
            setId = definition.setId
        ).normalized()
    }

    fun defaultPendantData(level: Int = 1): EquipmentData {
        return defaultEquipmentData(EquipmentDefinitions.HINAGATA_WAND.resolve(), level)
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
        val normalized = EquipmentBalanceMigration.markCurrent(
            data.copy(slotTypeId = definition.slotTypeId, setId = definition.setId)
        )
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
            StatType.PHYSICAL_RESISTANCE -> 1.0 + level * 0.35
            StatType.ARCANE_RESISTANCE -> 1.0 + level * 0.35
            StatType.DAMAGE_REDUCTION -> 1.6 + level * 0.55
        }
    }
}
