package hifumi.cresora

import net.minecraft.entity.player.PlayerEntity
import net.minecraft.item.ItemStack

object WeaponSkillArtifactSupport {
    data class EligibleArtifact(
        val inventorySlot: Int,
        val stack: ItemStack,
        val data: EquipmentData
    )

    fun requiredArtifactCount(definition: WeaponDefinition, data: WeaponData): Int {
        return WeaponUpgradeService.skillArtifactCost(definition, data.skillLevel)
    }

    fun eligibleArtifacts(player: PlayerEntity, definition: WeaponDefinition): List<EligibleArtifact> {
        val minRarity = definition.upgrades.skillArtifactMinRarity ?: return emptyList()
        val result = mutableListOf<EligibleArtifact>()
        for (slot in 0 until player.inventory.size()) {
            val stack = player.inventory.getStack(slot)
            val equipmentData = EquipmentStackSupport.getEquipmentData(stack) ?: continue
            if (equipmentData.rarity.ordinal < minRarity.ordinal) {
                continue
            }
            result += EligibleArtifact(slot, stack.copy(), equipmentData)
        }
        return result
    }
}
