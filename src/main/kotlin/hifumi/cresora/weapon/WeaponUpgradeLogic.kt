package hifumi.cresora.weapon
import hifumi.cresora.credits.CreditsService
import hifumi.cresora.equipment.EquipmentStackSupport
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
        val roleMaterialCost: Int,
        val availableRoleMaterials: Int,
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

    fun getBasePreview(stack: ItemStack, availableCredits: Int?, availableFragments: Int?, availableRoleMaterials: Int? = null): BasePreview {
        val definition = WeaponStackSupport.getDefinition(stack)
            ?: return BasePreview(0, 0, 0.0, 0.0, 0, 0, 0, 0, false, "screen.cresora.weapon_upgrade.need_weapon")
        val data = WeaponStackSupport.ensureWeaponData(stack)
        val currentAttack = WeaponCombatSupport.attackDamage(definition, data)
        val currentCap = WeaponUpgradeService.levelCap(definition, data.breakthrough)

        if (data.baseLevel >= currentCap) {
            if (data.breakthrough >= 2) {
                return BasePreview(data.baseLevel, data.baseLevel, currentAttack, currentAttack, 0, 0, 0, 0, false, "screen.cresora.weapon_upgrade.max_base")
            }
            val targetBt = data.breakthrough + 1
            val resultAttack = WeaponCombatSupport.attackDamage(definition, data.copy(baseLevel = 1, breakthrough = targetBt).normalized(definition))
            val cscCost = WeaponUpgradeService.breakthroughCscCost(data.rarity, targetBt)
            val fragmentCost = WeaponUpgradeService.breakthroughFragmentCost(data.rarity, targetBt)
            val roleMaterialCost = WeaponUpgradeService.breakthroughRoleMaterialCost(data.rarity, targetBt)
            val haveRoleMat = availableRoleMaterials ?: 0

            val hasCredits = availableCredits == null || availableCredits >= cscCost
            val hasFragments = availableFragments == null || availableFragments >= fragmentCost
            val hasRoleMaterials = availableRoleMaterials == null || availableRoleMaterials >= roleMaterialCost

            val canUpgrade = hasCredits && hasFragments && hasRoleMaterials
            val messageKey = when {
                !hasCredits -> "item.cresora.not_enough_credits"
                !hasFragments -> "screen.cresora.weapon_upgrade.no_fragments"
                !hasRoleMaterials -> "screen.cresora.weapon_upgrade.no_role_materials"
                else -> "screen.cresora.weapon_upgrade.ready_breakthrough"
            }

            return BasePreview(
                data.baseLevel, 1, currentAttack, resultAttack, cscCost, fragmentCost,
                roleMaterialCost, haveRoleMat, canUpgrade, messageKey
            )
        }

        val resultLevel = data.baseLevel + 1
        val resultAttack = WeaponCombatSupport.attackDamage(definition, data.copy(baseLevel = resultLevel).normalized(definition))
        val cscCost = WeaponUpgradeService.baseUpgradeCost(definition, data.baseLevel)
        val fragmentCost = definition.upgrades.baseFragmentCost

        val hasCredits = availableCredits == null || availableCredits >= cscCost
        val hasFragments = availableFragments == null || availableFragments >= fragmentCost
        val canUpgrade = hasCredits && hasFragments
        val messageKey = when {
            !hasCredits -> "item.cresora.not_enough_credits"
            !hasFragments -> "screen.cresora.weapon_upgrade.no_fragments"
            else -> "screen.cresora.weapon_upgrade.ready_base"
        }

        return BasePreview(
            data.baseLevel, resultLevel, currentAttack, resultAttack, cscCost, fragmentCost,
            0, availableRoleMaterials ?: 0, canUpgrade, messageKey
        )
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
        val skillCap = WeaponUpgradeService.maxSkillLevelForBreakthrough(data.breakthrough)
        if (data.skillLevel >= skillCap) {
            return SkillPreview(data.skillLevel, data.skillLevel, definition.skill.effectId, currentValue, currentValue, 0, 0, 0, false, "screen.cresora.weapon_upgrade.need_breakthrough")
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
        if (WeaponStackSupport.hasAnyEquippedArtifact(stack)) {
            return DismantlePreview(0, false, "screen.cresora.weapon_upgrade.cannot_dismantle_has_artifacts")
        }
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
        val cap0 = WeaponUpgradeService.levelCap(definition, 0)
        val cap1 = WeaponUpgradeService.levelCap(definition, 1)
        val historicalLevels = when (data.breakthrough) {
            0 -> 0
            1 -> (cap0 - definition.craft.craftedBaseLevel).coerceAtLeast(0)
            else -> (cap0 - definition.craft.craftedBaseLevel).coerceAtLeast(0) + (cap1 - 1).coerceAtLeast(0)
        }
        val currentStageLevels = if (data.breakthrough == 0) {
            (data.baseLevel - definition.craft.craftedBaseLevel).coerceAtLeast(0)
        } else {
            (data.baseLevel - 1).coerceAtLeast(0)
        }
        val investedLevels = historicalLevels + currentStageLevels
        val upgradeReturn = (((investedLevels * definition.upgrades.baseFragmentCost).toDouble()) * 0.6).toInt()
        var maxPossibleRefund = definition.craft.fragmentsRequired + investedLevels * definition.upgrades.baseFragmentCost
        for (bt in 1..data.breakthrough) {
            maxPossibleRefund += WeaponUpgradeService.breakthroughFragmentCost(data.rarity, bt)
        }
        val returnCount = (baseReturn + rarityBonus + upgradeReturn).coerceAtMost(maxPossibleRefund).coerceAtLeast(1)
        return DismantlePreview(returnCount, true, "screen.cresora.weapon_upgrade.ready_dismantle")
    }

    fun attemptUpgrade(player: PlayerEntity, stack: ItemStack, type: UpgradeType): AttemptResult {
        val serverPlayer = player as? ServerPlayerEntity
            ?: return AttemptResult(false, Text.translatable("screen.cresora.weapon_upgrade.need_weapon").formatted(Formatting.RED))
        val definition = WeaponStackSupport.getDefinition(stack)
            ?: return AttemptResult(false, Text.translatable("screen.cresora.weapon_upgrade.need_weapon").formatted(Formatting.RED))
        val data = WeaponStackSupport.ensureWeaponData(stack)
        val result = when (type) {
            UpgradeType.BASE -> attemptBaseUpgrade(serverPlayer, stack, definition, data)
            UpgradeType.SKILL -> attemptSkillUpgrade(serverPlayer, stack, definition, data)
            UpgradeType.DISMANTLE -> attemptDismantle(serverPlayer, stack, definition)
        }
        if (result.success && type == UpgradeType.BASE) {
            val newData = WeaponStackSupport.getWeaponData(stack)
            if (newData != null) {
                hifumi.cresora.guide.GuideService.onWeaponUpgrade(serverPlayer, newData.baseLevel)
            }
        }
        return result
    }

    private fun attemptBaseUpgrade(
        player: ServerPlayerEntity,
        stack: ItemStack,
        definition: WeaponDefinition,
        data: WeaponData
    ): AttemptResult {
        val currentCap = WeaponUpgradeService.levelCap(definition, data.breakthrough)
        val targetBt = data.breakthrough + 1
        val haveRoleMaterials = if (data.baseLevel >= currentCap && data.breakthrough < 2) {
            WeaponStackSupport.countRoleMaterials(player, definition.role, targetBt)
        } else 0
        val preview = getBasePreview(
            stack,
            CreditsService.getCredits(player),
            WeaponStackSupport.countFragments(player, definition),
            haveRoleMaterials
        )
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

        val isBreakthrough = data.baseLevel >= currentCap && data.breakthrough < 2
        if (isBreakthrough) {
            val nextBt = data.breakthrough + 1
            if (!WeaponStackSupport.removeRoleMaterials(player, definition.role, nextBt, preview.roleMaterialCost)) {
                // Rollback credits and fragments
                CreditsService.addCredits(player, preview.cscCost)
                val fragmentItem = WeaponStackSupport.fragmentItem(definition.id)
                if (fragmentItem != null && preview.fragmentCost > 0) {
                    player.inventory.offerOrDrop(ItemStack(fragmentItem, preview.fragmentCost))
                }
                return AttemptResult(false, Text.translatable("screen.cresora.weapon_upgrade.no_role_materials").formatted(Formatting.RED))
            }
            WeaponStackSupport.syncWeaponData(stack, data.copy(baseLevel = 1, breakthrough = nextBt).normalized(definition))
            return AttemptResult(
                true,
                Text.translatable("screen.cresora.weapon_upgrade.upgraded_breakthrough", data.breakthrough, nextBt)
            )
        }

        WeaponStackSupport.syncWeaponData(stack, data.copy(baseLevel = data.baseLevel + 1).normalized(definition))
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
