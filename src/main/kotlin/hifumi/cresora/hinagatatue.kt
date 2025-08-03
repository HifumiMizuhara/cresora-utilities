package hifumi.cresora

import com.google.common.collect.Multimap

import net.minecraft.entity.player.PlayerEntity
import net.minecraft.item.Item
import net.minecraft.item.ItemStack
import net.minecraft.item.tooltip.TooltipType
import net.minecraft.text.Text
import net.minecraft.util.Formatting
import net.minecraft.util.Hand
import net.minecraft.util.TypedActionResult
import net.minecraft.world.World
import kotlin.math.roundToInt
import hifumi.cresora.CreSoraUtilities.PENDANT_ATTRIBUTE_ID
import net.minecraft.entity.LivingEntity
import net.minecraft.entity.attribute.EntityAttribute
import net.minecraft.entity.attribute.EntityAttributeModifier
import net.minecraft.entity.attribute.EntityAttributes
import net.minecraft.registry.entry.RegistryEntry
import net.minecraft.util.Identifier

class Hinagatas_Tue(settings: Settings) : Item(settings) {
    companion object {
        val bairitu = 0.7
    }
    override fun appendTooltip(
        stack: ItemStack,
        context: TooltipContext,
        tooltip: MutableList<Text>,
        type: TooltipType
    ) {
        // データコンポーネントからレベルを取得
        val level = stack.getOrDefault(ModDataComponents.LEVEL, 1)

        // ツールチップに「Level: X」と表示する
        val atk= ((level*bairitu*10).roundToInt()/10.0).toString()
        tooltip.add(Text.literal("+$level"))
        tooltip.add(Text.translatable("item.cresora.tuelevel", atk).formatted(Formatting.GRAY))
        tooltip.add(Text.translatable("item.cresora.bairitu",bairitu).formatted(Formatting.GRAY))
        super.appendTooltip(stack, context, tooltip, type)
    }
    override fun use(world: World, user: PlayerEntity, hand: Hand): TypedActionResult<ItemStack> {
        val pendantStack = user.getStackInHand(hand) // 手に持っているペンダント
        val offHandStack = user.getStackInHand(Hand.OFF_HAND) // オフハンドのアイテム

        // --- アップグレード条件のチェック ---
        // 1. ペンダントをメインハンドに持っている
        // 2. オフハンドにダイヤモンドを持っている
        if (hand == Hand.MAIN_HAND && offHandStack.isOf(CreSoraUtilities.TUESHOKAKU)) {
            // サーバーサイドでのみ実際の処理を行う
            if (!world.isClient) {
                // レベルを取得して1加算
                val currentLevel = pendantStack.getOrDefault(ModDataComponents.LEVEL, 1)
                val newLevel = currentLevel + 1

                // ペンダントのコンポーネントを更新

                val possib=9-offHandStack.getOrDefault(ModDataComponents.LEVEL, 1)
                if ((1..possib).shuffled().first()==1) {
                    pendantStack.set(ModDataComponents.LEVEL, newLevel)
                    user.sendMessage(Text.translatable("item.cresora.tuelevelled",currentLevel,newLevel))
                }
                else {
                    user.sendMessage(Text.translatable("item.cresora.tuelevelfailed"))
                }
                offHandStack.decrement(1)
            }

            // アクションが成功したことをクライアントに伝える (腕を振るアニメーション)
            return TypedActionResult.success(pendantStack, world.isClient())
        }
        if (hand == Hand.MAIN_HAND && offHandStack.isOf(CreSoraUtilities.STRENGTH_PENDANT)) {
            // サーバーサイドでのみ実際の処理を行う
            if (!world.isClient) {
                // レベルを取得して1加算
                val currentLevel = pendantStack.getOrDefault(ModDataComponents.LEVEL, 1)


                // ペンダントのコンポーネントを更新
                val level = offHandStack.getOrDefault(ModDataComponents.LEVEL, 1)
                val newLevel = currentLevel + level
                var made=256-currentLevel
                if (made<4) made=4
                if ((1..made).shuffled().first() == 1) { //?%
                    pendantStack.set(ModDataComponents.LEVEL, newLevel)
                    user.sendMessage(Text.translatable("item.cresora.tuelevelled", currentLevel, newLevel))
                } else {
                    user.sendMessage(Text.translatable("item.cresora.tuelevelfailed2"))
                }
                offHandStack.decrement(1)
            }
        }

        // アップグレード条件を満たさない場合は、何もしない (pass)
        return TypedActionResult.pass(pendantStack)
    }
    /**
     * 新しいアイテムスタックを作成する際に、デフォルトのコンポーネントを設定する
     */
    override fun getDefaultStack(): ItemStack {
        val stack = super.getDefaultStack()
        // デフォルトでレベル1のコンポーネントを付与する
        stack.set(ModDataComponents.LEVEL, 1)
        return stack
    }
}
