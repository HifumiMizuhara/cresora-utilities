package hifumi.cresora.equipment
import hifumi.cresora.StatType
import hifumi.cresora.adventurerank.AdventureRankService
import net.minecraft.entity.mob.HostileEntity
import net.minecraft.entity.mob.MobEntity
import net.minecraft.item.ItemStack
import net.minecraft.server.network.ServerPlayerEntity
import net.minecraft.text.Text

object ArtifactSpecialUpgradeService {
    fun alphaCandidates(baseStack: ItemStack): List<EquipmentDefinition> {
        val currentDefinition = EquipmentStackSupport.getDefinition(baseStack) ?: return emptyList()
        return EquipmentContentRegistry.equipmentDefinitions().filter { candidate ->
            candidate.slotTypeId == currentDefinition.slotTypeId &&
                candidate.setId != currentDefinition.setId
        }
    }

    fun performAlphaSelection(
        player: ServerPlayerEntity,
        baseStack: ItemStack,
        materialStack: ItemStack,
        targetDefinitionId: String
    ): ItemStack? {
        if (!ArtifactSpecialItemSupport.isKind(materialStack, ArtifactSpecialItemKind.ALPHA)) {
            player.sendMessage(Text.translatable("screen.cresora.alpha.invalid_material"), false)
            return null
        }

        val currentDefinition = EquipmentStackSupport.getDefinition(baseStack) ?: return null
        val targetDefinition = runCatching { EquipmentContentRegistry.requireEquipment(targetDefinitionId) }.getOrNull() ?: return null
        if (targetDefinition.slotTypeId != currentDefinition.slotTypeId || targetDefinition.setId == currentDefinition.setId) {
            player.sendMessage(Text.translatable("screen.cresora.alpha.invalid_target"), false)
            return null
        }

        val item = EquipmentStackSupport.itemForDefinitionId(targetDefinition.id) ?: return null
        val result = ItemStack(item)
        EquipmentStackSupport.syncEquipmentData(
            result,
            EquipmentGenerationService.createEquipment(
                random = player.random,
                definition = targetDefinition
            )
        )
        materialStack.decrement(1)
        player.sendMessage(
            Text.translatable(
                "screen.cresora.alpha.success",
                Text.translatable(targetDefinition.setDefinition().translationKey()),
                Text.translatable(targetDefinition.slotType().translationKey())
            ),
            false
        )
        return result
    }

    fun performBetaReforge(
        player: ServerPlayerEntity,
        baseStack: ItemStack,
        materialStack: ItemStack,
        selectedTypes: List<StatType>
    ): ItemStack? {
        if (!ArtifactSpecialItemSupport.isKind(materialStack, ArtifactSpecialItemKind.BETA)) {
            player.sendMessage(Text.translatable("screen.cresora.beta.invalid_material"), false)
            return null
        }

        if (selectedTypes.distinct().size != 2) {
            player.sendMessage(Text.translatable("screen.cresora.beta.need_two"), false)
            return null
        }

        val definition = EquipmentStackSupport.getDefinition(baseStack) ?: return null
        val currentData = EquipmentStackSupport.ensureEquipmentData(baseStack, player.random)
        val rerolled = EquipmentUpgradeService.rerollToMaxWithPrioritySubStat(
            definition = definition,
            rarity = currentData.rarity,
            priorityCandidates = selectedTypes,
            random = player.random
        )
        materialStack.decrement(1)
        EquipmentStackSupport.syncEquipmentData(baseStack, rerolled)
        player.sendMessage(Text.translatable("screen.cresora.beta.success"), false)
        return baseStack
    }

    fun tryDropSpecialItems(player: ServerPlayerEntity, hostile: MobEntity) {
        tryDropSpecialItems(player, AdventureRankService.mobLevel(hostile))
    }

    fun tryDropSpecialItems(player: ServerPlayerEntity, hostileRank: Int) {
        for (definition in ArtifactSpecialItemRegistry.definitionsWithMobDrop()) {
            val mobDrop = definition.mobDrop ?: continue
            val chance = scaledDropChance(hostileRank, mobDrop)
            if (chance <= 0.0 || player.random.nextDouble() >= chance) {
                continue
            }
            val item = ArtifactSpecialItemSupport.itemForDefinitionId(definition.id) ?: continue
            player.inventory.offerOrDrop(ItemStack(item))
            player.sendMessage(Text.translatable("screen.cresora.special_drop", Text.translatable(definition.translationKeyId)), false)
        }
    }

    private fun scaledDropChance(rank: Int, mobDrop: ArtifactSpecialMobDrop): Double {
        if (rank < mobDrop.rankMin) {
            return 0.0
        }
        if (mobDrop.rankMax <= mobDrop.rankMin) {
            return mobDrop.maxChance.coerceAtLeast(0.0)
        }
        val progress = ((rank - mobDrop.rankMin).toDouble() / (mobDrop.rankMax - mobDrop.rankMin).toDouble()).coerceIn(0.0, 1.0)
        return mobDrop.maxChance.coerceAtLeast(0.0) * progress
    }
}
