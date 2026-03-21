package hifumi.cresora

import net.minecraft.entity.player.PlayerEntity
import net.minecraft.item.ItemStack
import net.minecraft.item.tooltip.TooltipType
import net.minecraft.screen.SimpleNamedScreenHandlerFactory
import net.minecraft.text.Text
import net.minecraft.util.Formatting
import net.minecraft.util.Hand
import net.minecraft.util.ActionResult
import net.minecraft.world.World
import kotlin.math.roundToInt
import hifumi.cresora.CreSoraUtilities.PENDANT_ATTRIBUTE_ID
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents
import net.minecraft.entity.attribute.EntityAttributeModifier
import net.minecraft.entity.attribute.EntityAttributes
import net.minecraft.server.MinecraftServer
import net.minecraft.component.type.TooltipDisplayComponent
import java.util.function.Consumer

class Hinagatas_Tue(settings: Settings) : atkItem(settings) {

    override val bairitu = 1.3

    override fun appendTooltip(
        stack: ItemStack,
        context: TooltipContext,
        displayComponent: TooltipDisplayComponent,
        textConsumer: Consumer<Text>,
        type: TooltipType
    ) {
        // データコンポーネントからレベルを取得
        val level = stack.getOrDefault(ModDataComponents.LEVEL, 1)

        // ツールチップに「Level: X」と表示する
        val atk= ((level*bairitu*10).roundToInt()/10.0).toString()
        textConsumer.accept(Text.literal("+$level"))
        textConsumer.accept(Text.translatable("item.cresora.tuelevel", atk).formatted(Formatting.GRAY))
        textConsumer.accept(Text.translatable("item.cresora.bairitu",bairitu).formatted(Formatting.GRAY))
        super.appendTooltip(stack, context, displayComponent, textConsumer, type)
    }
    override fun register() {
        val STRENGTH_PENDANTS=this
        ServerTickEvents.END_SERVER_TICK.register { server: MinecraftServer ->
            for (player in server.playerManager.playerList) {

                val atkInstance = player.attributes.getCustomInstance(EntityAttributes.ATTACK_DAMAGE) ?: continue

                // ★★★ 修正点: 引数がIdentifierになり、正しく動作する ★★★
                val hasModifier = atkInstance.getModifier(PENDANT_ATTRIBUTE_ID) != null

                var pendantStack: ItemStack? = null
                var maxlev=0
                for (stack in player.inventory.getMainStacks()) {
                    if (stack.isOf(STRENGTH_PENDANTS)) {
                        if (stack.getOrDefault(ModDataComponents.LEVEL,1)>maxlev){
                            maxlev=stack.getOrDefault(ModDataComponents.LEVEL,1)
                            pendantStack=stack
                        }
                    }
                }
                for (stack in player.enderChestInventory.heldStacks) {
                    if (stack.isOf(STRENGTH_PENDANTS)) {
                        if (stack.getOrDefault(ModDataComponents.LEVEL,1)>maxlev){
                            maxlev=stack.getOrDefault(ModDataComponents.LEVEL,1)
                            pendantStack=stack
                        }
                    }
                }

                if (pendantStack != null) {
                    val level = pendantStack.getOrDefault(ModDataComponents.LEVEL, 1)
                    val attackBonus = level * STRENGTH_PENDANTS.bairitu

                    // ★★★ 修正点: getModifierの引数がIdentifierになる ★★★
                    val currentModifier = atkInstance.getModifier(PENDANT_ATTRIBUTE_ID)
                    if (currentModifier != null && currentModifier.value == attackBonus) {
                        // 正しい値なので何もしない
                    } else {
                        // 古いボーナスを一度削除
                        // ★★★ 修正点: removeModifierの引数がIdentifierになる ★★★
                        atkInstance.removeModifier(PENDANT_ATTRIBUTE_ID)

                        // ★★★ 修正点: EntityAttributeModifierのコンストラクタがIdentifierを要求する ★★★
                        val newModifier = EntityAttributeModifier(
                            PENDANT_ATTRIBUTE_ID, // 第1引数がIdentifier
                            attackBonus,
                            EntityAttributeModifier.Operation.ADD_VALUE
                        )
                        atkInstance.addTemporaryModifier(newModifier)
                    }
                } else {
                    if (hasModifier) {
                        // ★★★ 修正点: removeModifierの引数がIdentifierになる ★★★
                        atkInstance.removeModifier(PENDANT_ATTRIBUTE_ID)
                    }
                }
            }
        }
    }
    override fun use(world: World, user: PlayerEntity, hand: Hand): ActionResult {
        if (hand == Hand.MAIN_HAND) {
            if (!world.isClient) {
                user.openHandledScreen(
                    SimpleNamedScreenHandlerFactory(
                        { syncId, playerInventory, _ -> UpgradeScreenHandler(syncId, playerInventory) },
                        Text.translatable("screen.cresora.upgrade")
                    )
                )
            }
            return ActionResult.SUCCESS
        }

        return ActionResult.PASS
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
