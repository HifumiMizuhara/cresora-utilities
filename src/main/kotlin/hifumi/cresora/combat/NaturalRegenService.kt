package hifumi.cresora.combat
import hifumi.cresora.weapon.WeaponSkillService
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents
import net.minecraft.server.MinecraftServer
import net.minecraft.server.network.ServerPlayerEntity
import java.util.UUID
import kotlin.math.floor

object NaturalRegenService {
    private const val COMBAT_GRACE_TICKS: Long = 120L

    private val lastCombatTickByPlayer: MutableMap<UUID, Long> = mutableMapOf()
    private val pendingHealByPlayer: MutableMap<UUID, Double> = mutableMapOf()

    fun init() {
        ServerTickEvents.END_SERVER_TICK.register { server ->
            tick(server)
        }
    }

    fun markCombat(player: ServerPlayerEntity) {
        lastCombatTickByPlayer[player.uuid] = currentTick(player.server)
    }

    private fun tick(server: MinecraftServer) {
        val activePlayers = server.playerManager.playerList.mapTo(linkedSetOf()) { it.uuid }
        val tick = currentTick(server)
        for (player in server.playerManager.playerList) {
            tickPlayer(player, tick)
        }
        lastCombatTickByPlayer.keys.retainAll(activePlayers)
        pendingHealByPlayer.keys.retainAll(activePlayers)
    }

    private fun tickPlayer(player: ServerPlayerEntity, tick: Long) {
        if (!player.isAlive || player.isSpectator || player.isCreative) {
            pendingHealByPlayer.remove(player.uuid)
            return
        }
        if (player.health >= player.maxHealth - 0.001f) {
            pendingHealByPlayer.remove(player.uuid)
            return
        }

        val foodLevel = player.hungerManager.foodLevel.coerceIn(0, 20)
        if (foodLevel <= 0) {
            pendingHealByPlayer.remove(player.uuid)
            return
        }

        val hungerScalar = foodLevel / 20.0
        val balance = CombatBalanceProfileRegistry.current().regen
        val basePercentPerSecond = if (isInCombat(player, tick)) {
            balance.combatPercentMaxHealthPerSecond
        } else {
            balance.nonCombatPercentMaxHealthPerSecond
        }
        val weaponScalar = 1.0 + WeaponSkillService.regenStageBonus(player).coerceAtLeast(0) * 0.15
        val ratePerTick = player.maxHealth.toDouble() * (basePercentPerSecond / 100.0) * weaponScalar / 20.0
        val accumulated = (pendingHealByPlayer[player.uuid] ?: 0.0) + ratePerTick * hungerScalar
        val missingHealth = (player.maxHealth - player.health).toDouble().coerceAtLeast(0.0)
        val wholePoints = floor(accumulated).coerceAtMost(floor(missingHealth))

        if (wholePoints < 1.0) {
            if (accumulated >= missingHealth && missingHealth > 0) {
                player.heal(missingHealth.toFloat())
                pendingHealByPlayer.remove(player.uuid)
            } else {
                pendingHealByPlayer[player.uuid] = accumulated.coerceAtMost(1.1)
            }
            return
        }

        val before = player.health
        player.heal(wholePoints.toFloat())
        val applied = (player.health - before).toDouble().coerceAtLeast(0.0)
        val remainder = (accumulated - applied).coerceAtLeast(0.0)
        if (remainder > 0.0001 && player.health < player.maxHealth - 0.001f) {
            pendingHealByPlayer[player.uuid] = remainder.coerceAtMost(1.1)
        } else {
            pendingHealByPlayer.remove(player.uuid)
        }
    }

    private fun isInCombat(player: ServerPlayerEntity, tick: Long): Boolean {
        val lastTick = lastCombatTickByPlayer[player.uuid] ?: return false
        return tick - lastTick <= COMBAT_GRACE_TICKS
    }

    private fun currentTick(server: MinecraftServer?): Long = server?.overworld?.time ?: 0L
}
