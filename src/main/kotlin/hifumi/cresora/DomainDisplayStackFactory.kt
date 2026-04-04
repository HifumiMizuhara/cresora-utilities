package hifumi.cresora

import net.minecraft.component.DataComponentTypes
import net.minecraft.item.Item
import net.minecraft.item.ItemStack
import net.minecraft.item.Items
import net.minecraft.text.Text

object DomainDisplayStackFactory {
    fun storyLinkedDisplay(chapter: StoryChapterDefinition, domain: DomainDefinition): ItemStack {
        val rewardProfile = DomainRewardProfileRegistry.requireProfile(domain.rewardProfileId)
        val displayItem = domainDisplayItem(rewardProfile)
        return ItemStack(displayItem).apply {
            set(DataComponentTypes.CUSTOM_NAME, Text.literal(chapter.displayName))
        }
    }

    fun domainDisplay(definition: DomainDefinition): ItemStack {
        val rewardProfile = DomainRewardProfileRegistry.requireProfile(definition.rewardProfileId)
        val displayItem = domainDisplayItem(rewardProfile)
        return ItemStack(displayItem).apply {
            set(
                DataComponentTypes.CUSTOM_NAME,
                Text.translatable(
                    "screen.cresora.domain.entry",
                    Text.translatable(definition.nameKey),
                    rewardLabel(rewardProfile),
                    ArtifactSpecialItem.formatWholeNumber(definition.entryCostCsc)
                )
            )
        }
    }

    fun rewardDisplayStacks(result: DomainRewardResult): List<ItemStack> {
        val displays = mutableListOf<ItemStack>()
        displays += result.items.map(ItemStack::copy)
        if (result.credits > 0) {
            displays += ItemStack(Items.GOLD_INGOT).apply {
                set(
                    DataComponentTypes.CUSTOM_NAME,
                    Text.translatable("screen.cresora.domain.reward_credits", ArtifactSpecialItem.formatWholeNumber(result.credits))
                )
            }
        }
        if (result.rankXp > 0) {
            displays += ItemStack(Items.EXPERIENCE_BOTTLE).apply {
                set(
                    DataComponentTypes.CUSTOM_NAME,
                    Text.translatable("screen.cresora.domain.reward_rank_xp", ArtifactSpecialItem.formatWholeNumber(result.rankXp))
                )
            }
        }
        return displays
    }

    fun rewardLabel(profile: DomainRewardProfile): Text {
        profile.artifactReward?.let { artifact ->
            return Text.translatable(EquipmentContentRegistry.requireSet(artifact.setId).translationKey())
        }
        profile.weaponFragmentReward?.let { weapon ->
            return Text.translatable(WeaponContentRegistry.requireWeapon(weapon.weaponId).fragmentTranslationKey())
        }
        return Text.translatable("screen.cresora.domain.reward.training")
    }

    private fun domainDisplayItem(profile: DomainRewardProfile): Item {
        profile.artifactReward?.let { artifact ->
            val fallback = EquipmentContentRegistry.equipmentDefinitions()
                .firstOrNull { it.setId == artifact.setId }
                ?: return Items.STICK
            val preferred = EquipmentContentRegistry.equipmentDefinitions().firstOrNull { candidate ->
                candidate.setId == artifact.setId && candidate.slotTypeId == "wand"
            } ?: fallback
            return EquipmentStackSupport.itemForDefinitionId(preferred.id) ?: Items.STICK
        }
        profile.weaponFragmentReward?.let { weapon ->
            return WeaponStackSupport.fragmentItem(weapon.weaponId) ?: Items.PRISMARINE_SHARD
        }
        return Items.EMERALD
    }
}
