package hifumi.cresora

import net.minecraft.entity.player.PlayerEntity
import net.minecraft.item.ItemStack
import net.minecraft.server.network.ServerPlayerEntity
import net.minecraft.text.Text
import net.minecraft.util.Formatting

object WeaponUpgradeLogic {
    enum class UpgradeType {
        BASE,
        SKILL
    }

    data class BasePreview(
        val currentLevel: Int,
        val resultLevel: Int,
        val currentAttack: Double,
        val resultAttack: Double,
        val cscCost: Int,
        val fragmentCost: Int,
        val canUpgrade: Boolean,
        val messageKey: String?
    )

    data class SkillPreview(
        val currentLevel: Int,
        val resultLevel: Int,
        val effectId: String,
        val currentValueHearts: Double,
        val resultValueHearts: Double,
        val cscCost: Int,
        val canUpgrade: Boolean,
        val messageKey: String?
    )

    data class AttemptResult(
        val success: Boolean,
        val message: Text
    )

    fun getBasePreview(stack: ItemStack, availableCredits: Int?, availableFragments: Int?): BasePreview {
        val definition = WeaponStackSupport.getDefinition(stack)
            ?: return BasePreview(0, 0, 0.0, 0.0, 0, 0, false, "screen.cresora.weapon_upgrade.need_weapon")
        val data = WeaponStackSupport.ensureWeaponData(stack)
        val currentAttack = WeaponCombatSupport.attackDamage(definition, data)
        val resultAttack = WeaponCombatSupport.attackDamage(definition, data.copy(baseLevel = data.baseLevel + 1).normalized(definition))
        if (data.baseLevel >= definition.maxBaseLevel) {
            return BasePreview(data.baseLevel, data.baseLevel, currentAttack, currentAttack, 0, 0, false, "screen.cresora.weapon_upgrade.max_base")
        }
        val cscCost = WeaponUpgradeService.baseUpgradeCost(definition, data.baseLevel)
        val fragmentCost = definition.upgrades.baseFragmentCost
        if (availableCredits != null && availableCredits < cscCost) {
            return BasePreview(data.baseLevel, data.baseLevel + 1, currentAttack, resultAttack, cscCost, fragmentCost, false, "item.cresora.not_enough_credits")
        }
        if (availableFragments != null && availableFragments < fragmentCost) {
            return BasePreview(data.baseLevel, data.baseLevel + 1, currentAttack, resultAttack, cscCost, fragmentCost, false, "screen.cresora.weapon_upgrade.no_fragments")
        }
        return BasePreview(data.baseLevel, data.baseLevel + 1, currentAttack, resultAttack, cscCost, fragmentCost, true, "screen.cresora.weapon_upgrade.ready_base")
    }

    fun getSkillPreview(stack: ItemStack, availableCredits: Int?): SkillPreview {
        val definition = WeaponStackSupport.getDefinition(stack)
            ?: return SkillPreview(0, 0, "shield", 0.0, 0.0, 0, false, "screen.cresora.weapon_upgrade.need_weapon")
        val data = WeaponStackSupport.ensureWeaponData(stack)
        val currentValue = WeaponCombatSupport.skillValueHearts(definition, data)
        val resultValue = WeaponCombatSupport.skillValueHearts(definition, data.copy(skillLevel = data.skillLevel + 1).normalized(definition))
        if (data.skillLevel >= definition.maxSkillLevel) {
            return SkillPreview(data.skillLevel, data.skillLevel, definition.skill.effectId, currentValue, currentValue, 0, false, "screen.cresora.weapon_upgrade.max_skill")
        }
        val cscCost = WeaponUpgradeService.skillUpgradeCost(definition, data.skillLevel)
        if (availableCredits != null && availableCredits < cscCost) {
            return SkillPreview(data.skillLevel, data.skillLevel + 1, definition.skill.effectId, currentValue, resultValue, cscCost, false, "item.cresora.not_enough_credits")
        }
        return SkillPreview(data.skillLevel, data.skillLevel + 1, definition.skill.effectId, currentValue, resultValue, cscCost, true, "screen.cresora.weapon_upgrade.ready_skill")
    }

    fun attemptUpgrade(player: PlayerEntity, stack: ItemStack, type: UpgradeType): AttemptResult {
        val serverPlayer = player as? ServerPlayerEntity
            ?: return AttemptResult(false, Text.translatable("screen.cresora.weapon_upgrade.need_weapon").formatted(Formatting.RED))
        val definition = WeaponStackSupport.getDefinition(stack)
            ?: return AttemptResult(false, Text.translatable("screen.cresora.weapon_upgrade.need_weapon").formatted(Formatting.RED))
        val data = WeaponStackSupport.ensureWeaponData(stack)
        return when (type) {
            UpgradeType.BASE -> attemptBaseUpgrade(serverPlayer, stack, definition, data)
            UpgradeType.SKILL -> attemptSkillUpgrade(serverPlayer, stack, definition, data)
        }
    }

    private fun attemptBaseUpgrade(
        player: ServerPlayerEntity,
        stack: ItemStack,
        definition: WeaponDefinition,
        data: WeaponData
    ): AttemptResult {
        val preview = getBasePreview(stack, CreditsService.getCredits(player), WeaponStackSupport.countFragments(player, definition))
        if (!preview.canUpgrade) {
            return AttemptResult(false, Text.translatable(preview.messageKey ?: "screen.cresora.weapon_upgrade.need_weapon").formatted(Formatting.RED))
        }
        if (!CreditsService.spendCredits(player, preview.cscCost)) {
            return AttemptResult(false, Text.translatable("item.cresora.not_enough_credits").formatted(Formatting.RED))
        }
        if (!WeaponStackSupport.removeFragments(player, definition, preview.fragmentCost)) {
            CreditsService.addCredits(player, preview.cscCost)
            return AttemptResult(false, Text.translatable("screen.cresora.weapon_upgrade.no_fragments").formatted(Formatting.RED))
        }
        WeaponStackSupport.syncWeaponData(stack, data.copy(baseLevel = data.baseLevel + 1))
        return AttemptResult(
            true,
            Text.translatable("screen.cresora.weapon_upgrade.upgraded_base", preview.currentLevel, preview.resultLevel)
        )
    }

    private fun attemptSkillUpgrade(
        player: ServerPlayerEntity,
        stack: ItemStack,
        definition: WeaponDefinition,
        data: WeaponData
    ): AttemptResult {
        val preview = getSkillPreview(stack, CreditsService.getCredits(player))
        if (!preview.canUpgrade) {
            return AttemptResult(false, Text.translatable(preview.messageKey ?: "screen.cresora.weapon_upgrade.need_weapon").formatted(Formatting.RED))
        }
        if (!CreditsService.spendCredits(player, preview.cscCost)) {
            return AttemptResult(false, Text.translatable("item.cresora.not_enough_credits").formatted(Formatting.RED))
        }
        WeaponStackSupport.syncWeaponData(stack, data.copy(skillLevel = data.skillLevel + 1).normalized(definition))
        return AttemptResult(
            true,
            Text.translatable("screen.cresora.weapon_upgrade.upgraded_skill", preview.currentLevel, preview.resultLevel)
        )
    }
}
