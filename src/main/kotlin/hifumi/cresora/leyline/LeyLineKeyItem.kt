package hifumi.cresora.leyline

import net.minecraft.item.Item
import net.minecraft.item.ItemUsageContext
import net.minecraft.util.ActionResult
import net.minecraft.util.Hand
import net.minecraft.world.World
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.server.network.ServerPlayerEntity
import net.minecraft.server.world.ServerWorld
import net.minecraft.util.math.BlockPos

class LeyLineKeyItem(
    val element: LeyLineElement,
    settings: Settings
) : Item(settings) {

    override fun useOnBlock(context: ItemUsageContext): ActionResult {
        val world = context.world
        if (world.isClient) return ActionResult.SUCCESS

        val player = context.player as? ServerPlayerEntity ?: return ActionResult.PASS
        val targetPos = context.blockPos
        val clickedFace = context.side
        val placePos = targetPos.offset(clickedFace)

        if (tryPlaceLeyLineBlock(world as ServerWorld, player, placePos, context.hand)) {
            return ActionResult.SUCCESS
        }

        return ActionResult.PASS
    }

    override fun use(world: World, user: PlayerEntity, hand: Hand): ActionResult {
        val stack = user.getStackInHand(hand)
        if (world.isClient) return ActionResult.SUCCESS

        val player = user as? ServerPlayerEntity ?: return ActionResult.PASS
        
        // Raycast up to 5 blocks
        val hitResult = player.raycast(5.0, 1.0f, false)
        if (hitResult.type == net.minecraft.util.hit.HitResult.Type.BLOCK) {
            val blockHit = hitResult as net.minecraft.util.hit.BlockHitResult
            val placePos = blockHit.blockPos.offset(blockHit.side)
            if (tryPlaceLeyLineBlock(world as ServerWorld, player, placePos, hand)) {
                return ActionResult.SUCCESS
            }
        }

        // Raycast missed or failed, try placing 2 blocks in front on the ground
        val direction = player.horizontalFacing
        val frontPos = player.blockPos.offset(direction, 2)
        
        // Search downwards/upwards from frontPos to find the first solid block
        var foundPos: BlockPos? = null
        for (dy in 3 downTo -3) {
            val checkPos = frontPos.up(dy)
            val state = world.getBlockState(checkPos)
            val belowState = world.getBlockState(checkPos.down())
            if ((state.isAir || state.isReplaceable) && belowState.isSolidBlock(world, checkPos.down())) {
                foundPos = checkPos
                break
            }
        }

        if (foundPos != null && tryPlaceLeyLineBlock(world as ServerWorld, player, foundPos, hand)) {
            return ActionResult.SUCCESS
        }

        return ActionResult.PASS
    }

    private fun tryPlaceLeyLineBlock(
        world: ServerWorld,
        player: ServerPlayerEntity,
        pos: BlockPos,
        hand: Hand
    ): Boolean {
        val state = world.getBlockState(pos)
        if (!state.isAir && !state.isReplaceable) return false

        // Delegate actual block placement and session creation to LeyLineService
        val placed = LeyLineService.placeLeyLine(world, pos, player, element)
        if (placed) {
            val stack = player.getStackInHand(hand)
            if (!player.isCreative) {
                stack.decrement(1)
            }
            return true
        }
        return false
    }
}
