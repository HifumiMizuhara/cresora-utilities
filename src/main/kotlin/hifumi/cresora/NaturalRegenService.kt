package hifumi.cresora

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents
import net.minecraft.server.MinecraftServer
import net.minecraft.server.network.ServerPlayerEntity
import java.util.UUID
import kotlin.math.floor

object NaturalRegenService {
    private const val COMBAT_GRACE_TICKS: Long = 120L

    private data class RegenBand(
        val minRank: Int,
        val maxRank: Int,
        val combatSeconds: Double,
        val combatHp: Double,
        val nonCombatSeconds: Double,
        val nonCombatHp: Double
    ) {
        fun matches(rank: Int): Boolean = rank in minRank..maxRank
        fun combatHpPerTick(): Double = combatHp / (combatSeconds * 20.0)
        fun nonCombatHpPerTick(): Double = nonCombatHp / (nonCombatSeconds * 20.0)
    }

    private val regenBands: List<RegenBand> = listOf(
        RegenBand(1, 10, combatSeconds = 3.0, combatHp = 1.0, nonCombatSeconds = 5.0, nonCombatHp = 1.0),
        RegenBand(11, 20, combatSeconds = 2.0, combatHp = 1.0, nonCombatSeconds = 4.0, nonCombatHp = 1.0),
        RegenBand(21, 30, combatSeconds = 3.0, combatHp = 2.0, nonCombatSeconds = 5.0, nonCombatHp = 2.0),
        RegenBand(31, 40, combatSeconds = 1.0, combatHp = 1.0, nonCombatSeconds = 2.0, nonCombatHp = 1.0),
        RegenBand(41, 50, combatSeconds = 3.0, combatHp = 4.0, nonCombatSeconds = 1.0, nonCombatHp = 1.0),
        RegenBand(51, 60, combatSeconds = 2.0, combatHp = 3.0, nonCombatSeconds = 3.0, nonCombatHp = 4.0),
        RegenBand(61, 70, combatSeconds = 1.0, combatHp = 2.0, nonCombatSeconds = 2.0, nonCombatHp = 3.0)
    )

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

        val rank = AdventureRankService.getRank(player)
        val baseBandIndex = regenBands.indexOfFirst { it.matches(rank) }.let { if (it >= 0) it else regenBands.lastIndex }
        val boostedBandIndex = (baseBandIndex + WeaponSkillService.orchidPavilionRegenStageBonus(player)).coerceIn(0, regenBands.lastIndex)
        val band = regenBands[boostedBandIndex]
        val hungerScalar = foodLevel / 20.0
        val ratePerTick = if (isInCombat(player, tick)) band.combatHpPerTick() else band.nonCombatHpPerTick()
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
