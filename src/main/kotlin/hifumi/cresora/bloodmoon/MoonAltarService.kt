package hifumi.cresora.bloodmoon
import hifumi.cresora.CreSoraUtilities
import hifumi.cresora.equipment.ArtifactSpecialItemKind
import hifumi.cresora.equipment.ArtifactSpecialItemSupport
import net.fabricmc.fabric.api.event.player.UseBlockCallback
import net.minecraft.entity.mob.HostileEntity
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.item.ItemStack
import net.minecraft.server.network.ServerPlayerEntity
import net.minecraft.server.world.ServerWorld
import net.minecraft.text.Text
import net.minecraft.util.ActionResult
import net.minecraft.util.Hand
import net.minecraft.util.Formatting
import net.minecraft.util.hit.BlockHitResult
import net.minecraft.world.World

object MoonAltarService {
    private const val MOON_BRICK_DROP_CHANCE = 0.02

    fun init() {
        UseBlockCallback.EVENT.register(UseBlockCallback { player, world, hand, hitResult ->
            onUseBlock(player, world, hand, hitResult)
        })
    }

    fun tryDropMoonBrick(player: ServerPlayerEntity, hostile: HostileEntity) {
        if (player.random.nextDouble() >= MOON_BRICK_DROP_CHANCE) {
            return
        }
        player.inventory.offerOrDrop(ItemStack(CreSoraUtilities.MOON_BRICK_ITEM))
    }

    private fun onUseBlock(
        player: PlayerEntity,
        world: World,
        hand: Hand,
        hitResult: BlockHitResult
    ): ActionResult {
        if (world.isClient || hand != Hand.MAIN_HAND) {
            return ActionResult.PASS
        }
        val serverPlayer = player as? ServerPlayerEntity ?: return ActionResult.PASS
        val serverWorld = world as? ServerWorld ?: return ActionResult.PASS
        if (!serverWorld.getBlockState(hitResult.blockPos).isOf(CreSoraUtilities.MOON_ALTAR_BLOCK)) {
            return ActionResult.PASS
        }

        val stack = serverPlayer.getStackInHand(hand)
        if (!ArtifactSpecialItemSupport.isKind(stack, ArtifactSpecialItemKind.NOTE)) {
            serverPlayer.sendMessage(Text.translatable("message.cresora.moon_altar.need_blood_note").formatted(Formatting.GRAY), true)
            return ActionResult.SUCCESS
        }

        val server = serverPlayer.server ?: return ActionResult.PASS
        val targetDay = MoonPhaseService.scheduleBloodMoonForNextNight(server)
        if (!serverPlayer.isCreative) {
            stack.decrement(1)
        }
        serverPlayer.sendMessage(Text.translatable("message.cresora.moon_altar.scheduled", targetDay).formatted(Formatting.DARK_RED), false)
        return ActionResult.SUCCESS
    }
}
