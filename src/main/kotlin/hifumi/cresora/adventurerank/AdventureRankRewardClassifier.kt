package hifumi.cresora.adventurerank
import hifumi.cresora.ModItemTags
import net.minecraft.item.ItemStack

object AdventureRankRewardClassifier {
    fun classify(stack: ItemStack): AdventureRankRewardSource? {
        return when {
            stack.isIn(ModItemTags.ADVENTURE_RANK_ARTIFACTS) -> AdventureRankRewardSource.ARTIFACT_OBTAIN
            stack.isIn(ModItemTags.ADVENTURE_RANK_UPGRADE_MATERIALS) -> AdventureRankRewardSource.UPGRADE_MATERIAL_OBTAIN
            else -> null
        }
    }
}
