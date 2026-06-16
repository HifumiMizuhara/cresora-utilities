package hifumi.cresora.weapon

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents
import net.minecraft.client.MinecraftClient
import net.minecraft.client.network.AbstractClientPlayerEntity
import net.minecraft.particle.ParticleTypes
import kotlin.math.cos
import kotlin.math.sin

object SpiritRenderer {
    private const val MAX_RENDER_DISTANCE_SQUARED = 48.0 * 48.0

    fun init() {
        ClientTickEvents.END_CLIENT_TICK.register { client ->
            render(client)
        }
    }

    private fun render(client: MinecraftClient) {
        val world = client.world ?: return
        val camera = client.player ?: return
        if (client.isPaused || world.time % 4L != 0L) {
            return
        }
        for (player in world.players) {
            if (player.squaredDistanceTo(camera) > MAX_RENDER_DISTANCE_SQUARED) {
                continue
            }
            val definition = WeaponStackSupport.getDefinition(player.mainHandStack) ?: continue
            val spirit = definition.spirit ?: continue
            val data = WeaponStackSupport.getWeaponData(player.mainHandStack) ?: continue
            if (spirit.nameKey.isBlank()) {
                continue
            }
            spawnOrbitingOrb(world.time.toDouble(), player, data.spiritBondStage.coerceIn(1, 6))
        }
    }

    private fun spawnOrbitingOrb(worldTime: Double, player: AbstractClientPlayerEntity, bondStage: Int) {
        val world = player.clientWorld
        val angle = worldTime / 9.0 + player.id * 0.37
        val radius = 0.42 + bondStage * 0.03
        val baseY = player.y + if (player.isSneaking) 1.15 else 1.32
        val orbX = player.x + cos(angle) * radius
        val orbZ = player.z + sin(angle) * radius
        val orbY = baseY + sin(angle * 0.7) * 0.12

        world.addParticleClient(ParticleTypes.END_ROD, orbX, orbY, orbZ, 0.0, 0.005, 0.0)
        if (bondStage >= 3) {
            world.addParticleClient(
                ParticleTypes.ENCHANT,
                orbX,
                orbY + 0.03,
                orbZ,
                cos(angle + 1.2) * 0.01,
                0.0,
                sin(angle + 1.2) * 0.01
            )
        }
        if (bondStage >= 5) {
            world.addParticleClient(
                ParticleTypes.GLOW,
                orbX,
                orbY - 0.04,
                orbZ,
                0.0,
                0.0,
                0.0
            )
        }
    }
}
