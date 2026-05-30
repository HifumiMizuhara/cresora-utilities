package hifumi.cresora.leyline

import net.minecraft.block.Block
import net.minecraft.block.BlockState
import net.minecraft.state.StateManager
import net.minecraft.state.property.EnumProperty
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.util.ActionResult
import net.minecraft.util.hit.BlockHitResult
import net.minecraft.util.math.BlockPos
import net.minecraft.world.World
import net.minecraft.server.network.ServerPlayerEntity

class LeyLineOverflowBlock(settings: Settings) : Block(settings) {
    companion object {
        val ELEMENT: EnumProperty<LeyLineElement> = EnumProperty.of("element", LeyLineElement::class.java)
    }

    init {
        defaultState = stateManager.defaultState.with(ELEMENT, LeyLineElement.SUN)
    }

    override fun appendProperties(builder: StateManager.Builder<Block, BlockState>) {
        builder.add(ELEMENT)
    }

    override fun onUse(
        state: BlockState,
        world: World,
        pos: BlockPos,
        player: PlayerEntity,
        hit: BlockHitResult
    ): ActionResult {
        if (world.isClient) return ActionResult.SUCCESS

        val serverPlayer = player as? ServerPlayerEntity ?: return ActionResult.CONSUME
        val element = state.get(ELEMENT)
        LeyLineService.openSelectionGui(serverPlayer, pos, element)

        return ActionResult.SUCCESS
    }

    override fun onStateReplaced(
        state: BlockState,
        world: net.minecraft.server.world.ServerWorld,
        pos: BlockPos,
        moved: Boolean
    ) {
        LeyLineService.onBlockRemoved(world, pos)
        super.onStateReplaced(state, world, pos, moved)
    }
}
