package hifumi.cresora.world

import hifumi.cresora.weapon.WeaponStackSupport
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents
import net.minecraft.particle.ParticleTypes
import net.minecraft.server.world.ServerWorld

object SpiritManifestationService {
    private const val FIRST_SPIRIT_WEAPON_ID = "harukanaru_shojo_no_ketsui"

    fun init() {
        ServerTickEvents.END_SERVER_TICK.register { server ->
            val now = server.overworld.time
            if (now % 8L != 0L) {
                return@register
            }
            for (player in server.playerManager.playerList) {
                val definition = WeaponStackSupport.getDefinition(player.mainHandStack) ?: continue
                if (definition.id != FIRST_SPIRIT_WEAPON_ID) {
                    continue
                }
                val world = player.world as? ServerWorld ?: continue
                world.spawnParticles(
                    ParticleTypes.ENCHANT,
                    player.x,
                    player.y + 1.0,
                    player.z,
                    5,
                    0.35,
                    0.55,
                    0.35,
                    0.02
                )
                world.spawnParticles(
                    ParticleTypes.WITCH,
                    player.x,
                    player.y + 0.8,
                    player.z,
                    2,
                    0.25,
                    0.35,
                    0.25,
                    0.01
                )
            }
        }
    }
}
