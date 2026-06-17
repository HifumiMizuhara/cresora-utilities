package hifumi.cresora.world

import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents
import net.minecraft.block.Block
import net.minecraft.block.BlockState
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.server.network.ServerPlayerEntity
import net.minecraft.text.Text
import net.minecraft.util.ActionResult
import net.minecraft.util.hit.BlockHitResult
import net.minecraft.util.math.BlockPos
import net.minecraft.world.World
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

class PortalBlock(settings: Settings) : Block(settings) {
    override fun onUse(
        state: BlockState,
        world: World,
        pos: BlockPos,
        player: PlayerEntity,
        hit: BlockHitResult
    ): ActionResult {
        if (world.isClient) {
            return ActionResult.SUCCESS
        }

        val serverPlayer = player as? ServerPlayerEntity ?: return ActionResult.CONSUME
        val server = serverPlayer.server ?: return ActionResult.CONSUME
        val now = world.time
        val previous = lastUseTick[serverPlayer.uuid]
        if (previous != null && now - previous < COOLDOWN_TICKS) {
            return ActionResult.CONSUME
        }
        lastUseTick[serverPlayer.uuid] = now
        val leavingCresora = CresoraWorldKeys.isCresoraWorld(world.registryKey)
        val targetWorld = if (leavingCresora) {
            server.overworld
        } else {
            server.getWorld(CresoraWorldKeys.CRESORA_WORLD)
        }

        if (targetWorld == null) {
            serverPlayer.sendMessage(Text.translatable("message.cresora.portal.no_dimension"), false)
            return ActionResult.FAIL
        }

        val targetPos = if (leavingCresora) {
            server.overworld.spawnPos.up()
        } else {
            SpiritGuideService.LANDING_POS
        }
        if (!leavingCresora) {
            val chunkX = SpiritGuideService.LANDING_POS.x shr 4
            val chunkZ = SpiritGuideService.LANDING_POS.z shr 4
            targetWorld.getChunk(chunkX, chunkZ)
            SpiritGuideService.ensureLanding(targetWorld)
            SpiritGuideService.ensureGuide(targetWorld)
        }
        val center = targetPos.toCenterPos()
        serverPlayer.teleport(targetWorld, center.x, center.y + 0.1, center.z, setOf(), serverPlayer.yaw, serverPlayer.pitch, false)
        serverPlayer.fallDistance = 0.0
        serverPlayer.sendMessage(
            Text.translatable(if (leavingCresora) "message.cresora.portal.returned" else "message.cresora.portal.entered"),
            true
        )
        return ActionResult.SUCCESS
    }

    companion object {
        private const val COOLDOWN_TICKS: Long = 20L
        private val lastUseTick: MutableMap<UUID, Long> = ConcurrentHashMap()

        fun init() {
            ServerPlayConnectionEvents.DISCONNECT.register(ServerPlayConnectionEvents.Disconnect { handler, _ ->
                lastUseTick.remove(handler.player.uuid)
            })
        }
    }
}
