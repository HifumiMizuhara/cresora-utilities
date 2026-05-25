package hifumi.cresora.equipment
import net.minecraft.item.Item
import net.minecraft.item.ItemStack

object ArtifactSpecialItemSupport {
    private val definitionsByItem: MutableMap<Item, String> = linkedMapOf()
    private val itemsByDefinitionId: MutableMap<String, Item> = linkedMapOf()

    fun registerItem(item: Item, definitionId: String) {
        definitionsByItem[item] = definitionId
        itemsByDefinitionId[definitionId] = item
    }

    fun isSpecialItem(stack: ItemStack): Boolean = definitionsByItem.containsKey(stack.item)

    fun definition(stack: ItemStack): ArtifactSpecialItemDefinition? {
        val definitionId = definitionsByItem[stack.item] ?: return null
        return ArtifactSpecialItemRegistry.definition(definitionId)
    }

    fun definition(item: Item): ArtifactSpecialItemDefinition? {
        val definitionId = definitionsByItem[item] ?: return null
        return ArtifactSpecialItemRegistry.definition(definitionId)
    }

    fun isKind(stack: ItemStack, kind: ArtifactSpecialItemKind): Boolean {
        return definition(stack)?.kind == kind
    }

    fun itemForDefinitionId(definitionId: String): Item? = itemsByDefinitionId[definitionId]
}
