package hifumi.cresora

import net.minecraft.component.DataComponentTypes
import net.minecraft.item.Item
import net.minecraft.item.ItemStack
import net.minecraft.item.Items
import net.minecraft.text.Text

object ArtifactDisplayStackFactory {
    fun shopDisplay(definition: ArtifactSpecialItemDefinition): ItemStack {
        val item = ArtifactSpecialItemSupport.itemForDefinitionId(definition.id) ?: Items.BARRIER
        return ItemStack(item).apply {
            set(
                DataComponentTypes.CUSTOM_NAME,
                Text.translatable("screen.cresora.shop.entry", Text.translatable(definition.translationKeyId), ArtifactSpecialItem.formatWholeNumber(definition.shopPrice))
            )
        }
    }

    fun alphaCandidateDisplay(definition: EquipmentDefinition): ItemStack {
        val item = displayItemForSet(definition.setId, definition)
        return ItemStack(item).apply {
            set(
                DataComponentTypes.CUSTOM_NAME,
                Text.translatable(
                    "screen.cresora.alpha.option",
                    Text.translatable(definition.setDefinition().translationKey()),
                    Text.translatable(definition.slotType().translationKey())
                )
            )
        }
    }

    fun betaStatDisplay(type: StatType, selectedOrder: Int): ItemStack {
        val item = when {
            selectedOrder > 0 -> Items.ENCHANTED_BOOK
            else -> statIcon(type)
        }
        return ItemStack(item).apply {
            val name = if (selectedOrder > 0) {
                Text.translatable("screen.cresora.beta.selected_option", selectedOrder, Text.translatable(type.translationKey()))
            } else {
                Text.translatable("screen.cresora.beta.option", Text.translatable(type.translationKey()))
            }
            set(DataComponentTypes.CUSTOM_NAME, name)
        }
    }

    private fun displayItemForSet(setId: String, fallbackDefinition: EquipmentDefinition): Item {
        val preferred = EquipmentContentRegistry.equipmentDefinitions().firstOrNull { candidate ->
            candidate.setId == setId && candidate.slotTypeId == "wand"
        }
        return EquipmentStackSupport.itemForDefinitionId(preferred?.id ?: fallbackDefinition.id) ?: Items.STICK
    }

    private fun statIcon(type: StatType): Item {
        return when (type) {
            StatType.ATK_FLAT -> Items.WOODEN_SWORD
            StatType.ATK_PERCENT -> Items.IRON_SWORD
            StatType.HP_FLAT -> Items.APPLE
            StatType.HP_PERCENT -> Items.GOLDEN_APPLE
            StatType.DEF_FLAT -> Items.LEATHER_CHESTPLATE
            StatType.DEF_PERCENT -> Items.IRON_CHESTPLATE
            StatType.CRIT_RATE -> Items.ARROW
            StatType.CRIT_DMG -> Items.CROSSBOW
            StatType.ALL_DMG_BONUS -> Items.NETHER_STAR
            StatType.DAMAGE_REDUCTION -> Items.SHIELD
        }
    }
}
