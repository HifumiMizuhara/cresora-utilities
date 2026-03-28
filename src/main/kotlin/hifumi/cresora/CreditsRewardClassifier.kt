package hifumi.cresora

import net.minecraft.item.ItemStack

object CreditsRewardClassifier {
    fun classify(stack: ItemStack): CreditsRewardSource? {
        return when {
            stack.isIn(ModItemTags.ADVENTURE_RANK_ARTIFACTS) -> CreditsRewardSource.ARTIFACT_OBTAIN
            stack.isIn(ModItemTags.ADVENTURE_RANK_UPGRADE_MATERIALS) -> CreditsRewardSource.UPGRADE_MATERIAL_OBTAIN
            else -> null
        }
    }
}
