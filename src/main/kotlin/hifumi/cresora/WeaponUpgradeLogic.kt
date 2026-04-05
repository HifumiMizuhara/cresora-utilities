package hifumi.cresora

import net.minecraft.entity.player.PlayerEntity
import net.minecraft.item.ItemStack
import net.minecraft.server.network.ServerPlayerEntity
import net.minecraft.text.Text
import net.minecraft.util.Formatting

object WeaponUpgradeLogic {
    enum class UpgradeType {
        BASE,
        SKILL,
        DISMANTLE
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
        val artifactCost: Int,
        val availableArtifacts: Int,
        val canUpgrade: Boolean,
        val messageKey: String?
    )

    data class DismantlePreview(
        val returnCount: Int,
        val canDismantle: Boolean,
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

    fun getSkillPreview(stack: ItemStack, availableCredits: Int?, availableArtifacts: Int? = null): SkillPreview {
        val definition = WeaponStackSupport.getDefinition(stack)
            ?: return SkillPreview(0, 0, "shield", 0.0, 0.0, 0, 0, 0, false, "screen.cresora.weapon_upgrade.need_weapon")
        val data = WeaponStackSupport.ensureWeaponData(stack)
        val currentValue = when (definition.skill.effectId) {
            "current_hp_true_damage" -> WeaponCombatSupport.skillValuePercent(definition, data)
            "flame_aura" -> WeaponCombatSupport.skillValuePercent(definition, data)
            "sunlit_haste" -> WeaponCombatSupport.skillValuePercent(definition, data)
            else -> WeaponCombatSupport.skillValueHearts(definition, data)
        }
        val resultValue = when (definition.skill.effectId) {
            "current_hp_true_damage" -> WeaponCombatSupport.skillValuePercent(definition, data.copy(skillLevel = data.skillLevel + 1).normalized(definition))
            "flame_aura" -> WeaponCombatSupport.skillValuePercent(definition, data.copy(skillLevel = data.skillLevel + 1).normalized(definition))
            "sunlit_haste" -> WeaponCombatSupport.skillValuePercent(definition, data.copy(skillLevel = data.skillLevel + 1).normalized(definition))
            else -> WeaponCombatSupport.skillValueHearts(definition, data.copy(skillLevel = data.skillLevel + 1).normalized(definition))
        }
        if (data.skillLevel >= definition.maxSkillLevel) {
            return SkillPreview(data.skillLevel, data.skillLevel, definition.skill.effectId, currentValue, currentValue, 0, 0, 0, false, "screen.cresora.weapon_upgrade.max_skill")
        }
        val cscCost = WeaponUpgradeService.skillUpgradeCost(definition, data.skillLevel)
        val artifactCost = WeaponUpgradeService.skillArtifactCost(definition, data.skillLevel)
        if (availableCredits != null && availableCredits < cscCost) {
            return SkillPreview(
                data.skillLevel,
                data.skillLevel + 1,
                definition.skill.effectId,
                currentValue,
                resultValue,
                cscCost,
                artifactCost,
                availableArtifacts ?: 0,
                false,
                "item.cresora.not_enough_credits"
            )
        }
        if (artifactCost > 0 && availableArtifacts != null && availableArtifacts < artifactCost) {
            return SkillPreview(
                data.skillLevel,
                data.skillLevel + 1,
                definition.skill.effectId,
                currentValue,
                resultValue,
                cscCost,
                artifactCost,
                availableArtifacts,
                false,
                "screen.cresora.weapon_upgrade.no_artifact_materials"
            )
        }
        return SkillPreview(
            data.skillLevel,
            data.skillLevel + 1,
            definition.skill.effectId,
            currentValue,
            resultValue,
            cscCost,
            artifactCost,
            availableArtifacts ?: 0,
            true,
            if (artifactCost > 0) "screen.cresora.weapon_upgrade.ready_skill_select" else "screen.cresora.weapon_upgrade.ready_skill"
        )
    }

    fun getDismantlePreview(stack: ItemStack): DismantlePreview {
        val definition = WeaponStackSupport.getDefinition(stack)
            ?: return DismantlePreview(0, false, "screen.cresora.weapon_upgrade.need_weapon")
        val data = WeaponStackSupport.ensureWeaponData(stack)
        if (stack.isEmpty || WeaponStackSupport.fragmentItem(definition.id) == null) {
            return DismantlePreview(0, false, "screen.cresora.weapon_upgrade.cannot_dismantle")
        }
        val baseReturn = maxOf(1, definition.craft.fragmentsRequired / 4)
        val rarityBonus = when (data.rarity) {
            WeaponRarity.TWO_STAR -> 0
            WeaponRarity.THREE_STAR -> 1
            WeaponRarity.FOUR_STAR -> 2
            WeaponRarity.FIVE_STAR -> 3
        }
        val investedLevels = (data.baseLevel - definition.craft.craftedBaseLevel).coerceAtLeast(0)
        val upgradeReturn = (((investedLevels * definition.upgrades.baseFragmentCost).toDouble()) * 0.6).toInt()
        val returnCount = (baseReturn + rarityBonus + upgradeReturn).coerceAtMost(
            definition.craft.fragmentsRequired + investedLevels * definition.upgrades.baseFragmentCost
        ).coerceAtLeast(1)
        return DismantlePreview(returnCount, true, "screen.cresora.weapon_upgrade.ready_dismantle")
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
            UpgradeType.DISMANTLE -> attemptDismantle(serverPlayer, stack, definition)
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
        val preview = getSkillPreview(stack, CreditsService.getCredits(player), WeaponSkillArtifactSupport.eligibleArtifacts(player, definition).size)
        if (!preview.canUpgrade) {
            return AttemptResult(false, Text.translatable(preview.messageKey ?: "screen.cresora.weapon_upgrade.need_weapon").formatted(Formatting.RED))
        }
        if (preview.artifactCost > 0) {
            return AttemptResult(false, Text.translatable("screen.cresora.weapon_upgrade.need_material_selection").formatted(Formatting.YELLOW))
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

    fun attemptSkillUpgradeWithArtifacts(
        player: ServerPlayerEntity,
        stack: ItemStack,
        selectedInventorySlots: List<Int>
    ): AttemptResult {
        val definition = WeaponStackSupport.getDefinition(stack)
            ?: return AttemptResult(false, Text.translatable("screen.cresora.weapon_upgrade.need_weapon").formatted(Formatting.RED))
        val data = WeaponStackSupport.ensureWeaponData(stack)
        val preview = getSkillPreview(stack, CreditsService.getCredits(player), WeaponSkillArtifactSupport.eligibleArtifacts(player, definition).size)
        if (!preview.canUpgrade) {
            return AttemptResult(false, Text.translatable(preview.messageKey ?: "screen.cresora.weapon_upgrade.need_weapon").formatted(Formatting.RED))
        }
        val required = preview.artifactCost
        if (required <= 0) {
            return attemptSkillUpgrade(player, stack, definition, data)
        }
        if (selectedInventorySlots.distinct().size < required) {
            return AttemptResult(false, Text.translatable("screen.cresora.weapon_upgrade.not_enough_selected_materials").formatted(Formatting.RED))
        }
        val eligibleBySlot = WeaponSkillArtifactSupport.eligibleArtifacts(player, definition).associateBy(WeaponSkillArtifactSupport.EligibleArtifact::inventorySlot)
        val selected = selectedInventorySlots.distinct().take(required).mapNotNull(eligibleBySlot::get)
        if (selected.size < required) {
            return AttemptResult(false, Text.translatable("screen.cresora.weapon_upgrade.invalid_selected_materials").formatted(Formatting.RED))
        }
        if (!CreditsService.spendCredits(player, preview.cscCost)) {
            return AttemptResult(false, Text.translatable("item.cresora.not_enough_credits").formatted(Formatting.RED))
        }
        val minRarity = definition.upgrades.skillArtifactMinRarity
        for (artifact in selected) {
            val inventoryStack = player.inventory.getStack(artifact.inventorySlot)
            val equipmentData = EquipmentStackSupport.getEquipmentData(inventoryStack)
            if (inventoryStack.isEmpty || equipmentData == null || (minRarity != null && equipmentData.rarity.ordinal < minRarity.ordinal)) {
                CreditsService.addCredits(player, preview.cscCost)
                return AttemptResult(false, Text.translatable("screen.cresora.weapon_upgrade.invalid_selected_materials").formatted(Formatting.RED))
            }
        }
        for (artifact in selected) {
            player.inventory.getStack(artifact.inventorySlot).decrement(1)
        }
        player.inventory.markDirty()
        WeaponStackSupport.syncWeaponData(stack, data.copy(skillLevel = data.skillLevel + 1).normalized(definition))
        return AttemptResult(
            true,
            Text.translatable("screen.cresora.weapon_upgrade.upgraded_skill", preview.currentLevel, preview.resultLevel)
        )
    }

    private fun attemptDismantle(
        player: ServerPlayerEntity,
        stack: ItemStack,
        definition: WeaponDefinition
    ): AttemptResult {
        val preview = getDismantlePreview(stack)
        if (!preview.canDismantle) {
            return AttemptResult(
                false,
                Text.translatable(preview.messageKey ?: "screen.cresora.weapon_upgrade.cannot_dismantle").formatted(Formatting.RED)
            )
        }
        val fragmentItem = WeaponStackSupport.fragmentItem(definition.id)
            ?: return AttemptResult(false, Text.translatable("screen.cresora.weapon_upgrade.cannot_dismantle").formatted(Formatting.RED))
        val dismantledName = stack.name.copy()
        stack.decrement(1)
        player.inventory.offerOrDrop(ItemStack(fragmentItem, preview.returnCount))
        player.inventory.markDirty()
        return AttemptResult(
            true,
            Text.translatable("screen.cresora.weapon_upgrade.dismantled", dismantledName, preview.returnCount)
        )
    }
}
