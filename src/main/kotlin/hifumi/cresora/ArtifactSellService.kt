package hifumi.cresora

import net.minecraft.item.ItemStack

data class ArtifactSellPreview(
    val sellable: Boolean,
    val unitPrice: Int,
    val totalPrice: Int
)

object ArtifactSellService {
    fun preview(stack: ItemStack): ArtifactSellPreview {
        if (stack.isEmpty) {
            return ArtifactSellPreview(false, 0, 0)
        }
        val data = EquipmentStackSupport.getEquipmentData(stack) ?: return ArtifactSellPreview(false, 0, 0)
        val unitPrice = when (data.rarity) {
            EquipmentRarity.THREE_STAR -> 600 + data.level * 75
            EquipmentRarity.FOUR_STAR -> 2_500 + data.level * 150
            EquipmentRarity.FIVE_STAR -> 8_000 + data.level * 400
        }
        return ArtifactSellPreview(true, unitPrice, unitPrice * stack.count.coerceAtLeast(1))
    }
}
