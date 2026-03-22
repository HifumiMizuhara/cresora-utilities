package hifumi.cresora

import net.minecraft.entity.player.PlayerEntity
import net.minecraft.item.ItemStack
import net.minecraft.text.Text
import net.minecraft.util.Formatting
import kotlin.math.max

object UpgradeLogic {
    const val XP_COST = 2

    enum class MaterialType {
        NONE,
        TUESHOKAKU,
        PENDANT,
        INVALID
    }

    data class Preview(
        val materialType: MaterialType,
        val currentLevel: Int,
        val resultLevel: Int,
        val successRatePermille: Int,
        val canUpgrade: Boolean,
        val messageKey: String?
    )

    data class AttemptResult(
        val success: Boolean,
        val consumedMaterial: Boolean,
        val message: Text
    )

    fun getPreview(baseStack: ItemStack, materialStack: ItemStack, player: PlayerEntity?): Preview {
        if (!baseStack.isOf(CreSoraUtilities.STRENGTH_PENDANT)) {
            return Preview(MaterialType.NONE, 0, 0, 0, false, "screen.cresora.upgrade.need_base")
        }

        val currentLevel = normalizePendantLevel(baseStack)
        val baseData = EquipmentStackSupport.getEquipmentData(baseStack) ?: EquipmentStackSupport.defaultPendantData(currentLevel)
        if (currentLevel >= baseData.rarity.maxLevel) {
            return Preview(MaterialType.NONE, currentLevel, currentLevel, 0, false, "screen.cresora.upgrade.max_level")
        }

        if (materialStack.isEmpty) {
            return Preview(MaterialType.NONE, currentLevel, currentLevel, 0, false, "screen.cresora.upgrade.insert_material")
        }

        if (player != null && player.experienceLevel < XP_COST) {
            return Preview(MaterialType.NONE, currentLevel, currentLevel, 0, false, "item.cresora.not_enough_xp")
        }

        if (materialStack.isOf(CreSoraUtilities.TUESHOKAKU)) {
            val materialLevel = tueshokaku.normalizeLevel(materialStack)
            val denominator = 9 - materialLevel
            val successRate = 1000 / denominator
            return Preview(
                MaterialType.TUESHOKAKU,
                currentLevel,
                currentLevel + 1,
                successRate,
                true,
                "screen.cresora.upgrade.ready_tool"
            )
        }

        if (materialStack.isOf(CreSoraUtilities.STRENGTH_PENDANT)) {
            val sacrificeLevel = normalizePendantLevel(materialStack)
            val levelGain = max(1, sacrificeLevel)
            val denominator = max(1, max(currentLevel, levelGain))
            val successRate = 1000 / denominator
            return Preview(
                MaterialType.PENDANT,
                currentLevel,
                (currentLevel + levelGain).coerceAtMost(baseData.rarity.maxLevel),
                successRate,
                true,
                "screen.cresora.upgrade.ready_wand"
            )
        }

        return Preview(MaterialType.INVALID, currentLevel, currentLevel, 0, false, "screen.cresora.upgrade.invalid_material")
    }

    fun attemptUpgrade(player: PlayerEntity, baseStack: ItemStack, materialStack: ItemStack): AttemptResult {
        val preview = getPreview(baseStack, materialStack, player)
        if (!preview.canUpgrade) {
            return AttemptResult(false, false, Text.translatable(preview.messageKey ?: "screen.cresora.upgrade.invalid_material").formatted(Formatting.RED))
        }

        if (player.experienceLevel < XP_COST) {
            return AttemptResult(false, false, Text.translatable("item.cresora.not_enough_xp").formatted(Formatting.RED))
        }

        val denominator = 1000 / preview.successRatePermille
        val success = player.random.nextInt(denominator) == 0
        player.addExperienceLevels(-XP_COST)

        return when (preview.materialType) {
            MaterialType.TUESHOKAKU -> {
                materialStack.decrement(1)
                if (success) {
                    val baseData = EquipmentStackSupport.ensurePendantData(baseStack, player.random)
                    EquipmentStackSupport.syncPendantData(
                        baseStack,
                        EquipmentUpgradeService.applyLevels(baseData, preview.resultLevel - baseData.level, player.random)
                    )
                    AttemptResult(true, true, Text.translatable("item.cresora.tuelevelled", preview.currentLevel, preview.resultLevel))
                } else {
                    AttemptResult(false, true, Text.translatable("item.cresora.tuelevelfailed").formatted(Formatting.RED))
                }
            }

            MaterialType.PENDANT -> {
                materialStack.decrement(1)
                if (success) {
                    val baseData = EquipmentStackSupport.ensurePendantData(baseStack, player.random)
                    EquipmentStackSupport.syncPendantData(
                        baseStack,
                        EquipmentUpgradeService.applyLevels(baseData, preview.resultLevel - baseData.level, player.random)
                    )
                    AttemptResult(true, true, Text.translatable("item.cresora.tuelevelled", preview.currentLevel, preview.resultLevel))
                } else {
                    AttemptResult(false, true, Text.translatable("item.cresora.tuelevelfailed2").formatted(Formatting.RED))
                }
            }

            else -> AttemptResult(false, false, Text.translatable("screen.cresora.upgrade.invalid_material").formatted(Formatting.RED))
        }
    }

    fun normalizePendantLevel(stack: ItemStack): Int {
        return EquipmentStackSupport.getCompatibilityLevel(stack)
    }
}
