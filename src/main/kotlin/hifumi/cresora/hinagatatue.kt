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
        val data = EquipmentStackSupport.getEquipmentData(stack) ?: EquipmentStackSupport.defaultPendantData(EquipmentStackSupport.getCompatibilityLevel(stack))
        val aggregatedStats = EquipmentStatCalculator.aggregate(data)

        textConsumer.accept(Text.translatable(data.rarity.translationKey()).formatted(Formatting.GOLD))
        textConsumer.accept(
            Text.translatable("item.cresora.equipment.level", data.level, data.rarity.maxLevel).formatted(Formatting.GRAY)
        )
        textConsumer.accept(
            Text.translatable("item.cresora.equipment.main_stat", EquipmentStatCalculator.formatStatLine(data.mainStat)).formatted(Formatting.AQUA)
        )

        for (subStat in data.subStats) {
            textConsumer.accept(
                Text.translatable("item.cresora.equipment.sub_stat", EquipmentStatCalculator.formatStatLine(subStat)).formatted(Formatting.GRAY)
            )
        }

        val nextGrowthLevel = ((data.level / 4) + 1) * 4
        if (nextGrowthLevel <= data.rarity.maxLevel) {
            textConsumer.accept(
                Text.translatable("item.cresora.equipment.next_growth", nextGrowthLevel).formatted(Formatting.DARK_GREEN)
            )
        } else {
            textConsumer.accept(Text.translatable("item.cresora.equipment.maxed").formatted(Formatting.DARK_GREEN))
        }
        super.appendTooltip(stack, context, displayComponent, textConsumer, type)
    }
    override fun register() {
        ServerTickEvents.END_SERVER_TICK.register { server: MinecraftServer ->
            for (player in server.playerManager.playerList) {

                val attackInstance = player.attributes.getCustomInstance(EntityAttributes.ATTACK_DAMAGE)
                val healthInstance = player.attributes.getCustomInstance(EntityAttributes.MAX_HEALTH)
                val armorInstance = player.attributes.getCustomInstance(EntityAttributes.ARMOR)

                val totals = EquipmentPlayerSupport.getAggregatedStats(player)
                if (totals.isNotEmpty()) {
                    val bonuses = EquipmentStatCalculator.calculateAttributeBonuses(totals)
                    updateModifier(
                        attackInstance,
                        CreSoraUtilities.PENDANT_ATTACK_FLAT_ID,
                        bonuses.attackFlat,
                        EntityAttributeModifier.Operation.ADD_VALUE
                    )
                    updateModifier(
                        attackInstance,
                        CreSoraUtilities.PENDANT_ATTACK_SCALAR_ID,
                        bonuses.attackScalar,
                        EntityAttributeModifier.Operation.ADD_MULTIPLIED_TOTAL
                    )
                    updateModifier(
                        healthInstance,
                        CreSoraUtilities.PENDANT_HEALTH_FLAT_ID,
                        bonuses.healthFlat,
                        EntityAttributeModifier.Operation.ADD_VALUE
                    )
                    updateModifier(
                        healthInstance,
                        CreSoraUtilities.PENDANT_HEALTH_SCALAR_ID,
                        bonuses.healthScalar,
                        EntityAttributeModifier.Operation.ADD_MULTIPLIED_TOTAL
                    )
                    updateModifier(
                        armorInstance,
                        CreSoraUtilities.PENDANT_ARMOR_FLAT_ID,
                        bonuses.armorFlat,
                        EntityAttributeModifier.Operation.ADD_VALUE
                    )
                    updateModifier(
                        armorInstance,
                        CreSoraUtilities.PENDANT_ARMOR_SCALAR_ID,
                        bonuses.armorScalar,
                        EntityAttributeModifier.Operation.ADD_MULTIPLIED_TOTAL
                    )
                    if (player.health > player.maxHealth) {
                        player.health = player.maxHealth
                    }
                } else {
                    clearModifier(attackInstance, CreSoraUtilities.PENDANT_ATTACK_FLAT_ID)
                    clearModifier(attackInstance, CreSoraUtilities.PENDANT_ATTACK_SCALAR_ID)
                    clearModifier(healthInstance, CreSoraUtilities.PENDANT_HEALTH_FLAT_ID)
                    clearModifier(healthInstance, CreSoraUtilities.PENDANT_HEALTH_SCALAR_ID)
                    clearModifier(armorInstance, CreSoraUtilities.PENDANT_ARMOR_FLAT_ID)
                    clearModifier(armorInstance, CreSoraUtilities.PENDANT_ARMOR_SCALAR_ID)
                    if (player.health > player.maxHealth) {
                        player.health = player.maxHealth
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
        EquipmentStackSupport.syncPendantData(
            stack,
            EquipmentGenerationService.createPendant(net.minecraft.util.math.random.Random.create())
        )
        return stack
    }

    private fun updateModifier(
        instance: net.minecraft.entity.attribute.EntityAttributeInstance?,
        id: net.minecraft.util.Identifier,
        value: Double,
        operation: EntityAttributeModifier.Operation
    ) {
        if (instance == null) {
            return
        }
        if (value == 0.0) {
            instance.removeModifier(id)
            return
        }
        val existing = instance.getModifier(id)
        if (existing != null && existing.value == value && existing.operation == operation) {
            return
        }
        instance.removeModifier(id)
        instance.addTemporaryModifier(EntityAttributeModifier(id, value, operation))
    }

    private fun clearModifier(
        instance: net.minecraft.entity.attribute.EntityAttributeInstance?,
        id: net.minecraft.util.Identifier
    ) {
        instance?.removeModifier(id)
    }
}
