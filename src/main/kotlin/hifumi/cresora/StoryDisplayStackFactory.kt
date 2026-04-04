package hifumi.cresora

import net.minecraft.component.DataComponentTypes
import net.minecraft.item.ItemStack
import net.minecraft.item.Items
import net.minecraft.text.Text

data class StoryRewardResult(
    val credits: Int,
    val resonanceCurrencies: List<StoryCurrencyRewardDefinition>
)

object StoryDisplayStackFactory {
    fun rewardDisplayStacks(result: StoryRewardResult): List<ItemStack> {
        val displays = mutableListOf<ItemStack>()
        if (result.credits > 0) {
            displays += ItemStack(Items.GOLD_INGOT).apply {
                set(
                    DataComponentTypes.CUSTOM_NAME,
                    Text.translatable("screen.cresora.story.reward_credits", ArtifactSpecialItem.formatWholeNumber(result.credits))
                )
            }
        }
        for (currency in result.resonanceCurrencies) {
            if (currency.amount <= 0) {
                continue
            }
            displays += ItemStack(currency.type.icon()).apply {
                set(
                    DataComponentTypes.CUSTOM_NAME,
                    Text.translatable(
                        "screen.cresora.story.reward_currency",
                        Text.translatable(currency.type.translationKey),
                        ArtifactSpecialItem.formatWholeNumber(currency.amount)
                    )
                )
            }
        }
        return displays
    }
}
