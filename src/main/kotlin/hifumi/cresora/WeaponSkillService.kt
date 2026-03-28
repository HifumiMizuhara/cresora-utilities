package hifumi.cresora

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents
import net.minecraft.entity.boss.BossBar
import net.minecraft.entity.boss.ServerBossBar
import net.minecraft.server.network.ServerPlayerEntity
import net.minecraft.server.world.ServerWorld
import net.minecraft.text.Text
import net.minecraft.util.ActionResult
import net.minecraft.util.Formatting
import java.util.UUID

object WeaponSkillService {
    private val cooldownBars: MutableMap<UUID, MutableMap<String, ServerBossBar>> = mutableMapOf()
    private val cooldownsByPlayer: MutableMap<UUID, MutableMap<String, Long>> = mutableMapOf()

    fun init() {
        ServerTickEvents.END_SERVER_TICK.register { server ->
            val onlinePlayers = server.playerManager.playerList
            pruneOfflineState(onlinePlayers.mapTo(linkedSetOf(), ServerPlayerEntity::getUuid))
            for (player in onlinePlayers) {
                clearExpiredShield(player)
                updateCooldownFeedback(player)
            }
        }
    }

    fun tryActivate(player: ServerPlayerEntity, stack: net.minecraft.item.ItemStack): ActionResult {
        val definition = WeaponStackSupport.getDefinition(stack) ?: return ActionResult.PASS
        val data = WeaponStackSupport.ensureWeaponData(stack)
        if (isCoolingDown(player, definition.id)) {
            player.sendMessage(Text.translatable("item.cresora.weapon.skill.cooldown").formatted(Formatting.RED), true)
            return ActionResult.FAIL
        }
        val access = player as? WeaponSkillAccess ?: return ActionResult.FAIL
        startCooldown(player, definition.id, currentWorldTime(player) + definition.skill.cooldownSeconds * 20L)
        showCooldownBar(player, definition)
        return when (definition.skill.effectId) {
            "heal" -> activateHeal(player, definition, data, access)
            else -> activateShield(player, definition, data, access)
        }
    }

    fun absorbDamage(player: ServerPlayerEntity, amount: Float): Float {
        clearExpiredShield(player)
        val access = player as? WeaponSkillAccess ?: return amount
        val remainingShield = access.cresoraGetShieldHp()
        if (remainingShield <= 0.0f) {
            return amount
        }
        if (amount <= remainingShield) {
            access.cresoraSetShieldHp(remainingShield - amount)
            if (access.cresoraGetShieldHp() <= 0.0f) {
                clearShield(player)
            }
            player.sendMessage(
                Text.translatable("item.cresora.weapon.skill.blocked", formatNumber(amount / 2.0)).formatted(Formatting.AQUA),
                true
            )
            return 0.0f
        }
        access.cresoraSetShieldHp(0.0f)
        clearShield(player)
        player.sendMessage(
            Text.translatable("item.cresora.weapon.skill.broken").formatted(Formatting.RED),
            true
        )
        return amount - remainingShield
    }

    fun clearExpiredShield(player: ServerPlayerEntity) {
        val access = player as? WeaponSkillAccess ?: return
        if (access.cresoraGetShieldHp() <= 0.0f) {
            clearShield(player)
            return
        }
        if (currentWorldTime(player) >= access.cresoraGetShieldExpireTick()) {
            clearShield(player)
            player.sendMessage(Text.translatable("item.cresora.weapon.skill.expired").formatted(Formatting.GRAY), true)
        }
    }

    fun isCoolingDown(player: ServerPlayerEntity, weaponId: String): Boolean {
        val expireTick = cooldownsByPlayer[player.uuid]?.get(weaponId) ?: return false
        return currentWorldTime(player) < expireTick
    }

    private fun updateCooldownFeedback(player: ServerPlayerEntity) {
        val cooldowns = cooldownsByPlayer[player.uuid]
        if (cooldowns.isNullOrEmpty()) {
            removeCooldownBars(player)
            return
        }

        val now = currentWorldTime(player)
        val iterator = cooldowns.entries.iterator()
        while (iterator.hasNext()) {
            val (weaponId, cooldownExpireTick) = iterator.next()
            if (now >= cooldownExpireTick) {
                iterator.remove()
                removeCooldownBar(player, weaponId)
                continue
            }
            val definition = runCatching { WeaponContentRegistry.requireWeapon(weaponId) }.getOrNull()
            if (definition == null) {
                iterator.remove()
                removeCooldownBar(player, weaponId)
                continue
            }
            val totalTicks = definition.skill.cooldownSeconds * 20L
            val remainingTicks = (cooldownExpireTick - now).coerceAtLeast(0L)
            val progress = (1.0f - remainingTicks.toFloat() / totalTicks.toFloat()).coerceIn(0.0f, 1.0f)
            val playerBars = cooldownBars.getOrPut(player.uuid) { linkedMapOf() }
            val bossBar = playerBars.getOrPut(weaponId) {
                ServerBossBar(Text.empty(), BossBar.Color.BLUE, BossBar.Style.NOTCHED_10).apply {
                    addPlayer(player)
                }
            }
            bossBar.name = Text.translatable(
                "item.cresora.weapon.skill.cooldown_progress",
                Text.translatable(definition.translationKey()),
                formatNumber(remainingTicks / 20.0)
            )
            bossBar.percent = progress
            bossBar.color = BossBar.Color.BLUE
            bossBar.style = BossBar.Style.NOTCHED_10
            if (!bossBar.players.contains(player)) {
                bossBar.addPlayer(player)
            }
        }
        if (cooldowns.isEmpty()) {
            cooldownsByPlayer.remove(player.uuid)
            removeCooldownBars(player)
        }
    }

    private fun showCooldownBar(player: ServerPlayerEntity, definition: WeaponDefinition) {
        val playerBars = cooldownBars.getOrPut(player.uuid) { linkedMapOf() }
        val bossBar = playerBars.getOrPut(definition.id) { ServerBossBar(Text.empty(), BossBar.Color.BLUE, BossBar.Style.NOTCHED_10) }
        bossBar.name = Text.translatable(
            "item.cresora.weapon.skill.cooldown_progress",
            Text.translatable(definition.translationKey()),
            formatNumber(definition.skill.cooldownSeconds.toDouble())
        )
        bossBar.percent = 0.0f
        if (!bossBar.players.contains(player)) {
            bossBar.addPlayer(player)
        }
    }

    private fun startCooldown(player: ServerPlayerEntity, weaponId: String, expireTick: Long) {
        cooldownsByPlayer.getOrPut(player.uuid) { linkedMapOf() }[weaponId] = expireTick
    }

    private fun removeCooldownBar(player: ServerPlayerEntity, weaponId: String) {
        val playerBars = cooldownBars[player.uuid] ?: return
        playerBars.remove(weaponId)?.removePlayer(player)
        if (playerBars.isEmpty()) {
            cooldownBars.remove(player.uuid)
        }
    }

    private fun removeCooldownBars(player: ServerPlayerEntity) {
        cooldownBars.remove(player.uuid)?.values?.forEach { it.removePlayer(player) }
    }

    private fun pruneOfflineState(onlinePlayerIds: Set<UUID>) {
        val offlinePlayers = cooldownBars.keys.filterNot(onlinePlayerIds::contains)
        for (playerId in offlinePlayers) {
            cooldownBars.remove(playerId)?.values?.forEach(ServerBossBar::clearPlayers)
        }
        cooldownsByPlayer.keys.removeIf { !onlinePlayerIds.contains(it) }
    }

    private fun ServerBossBar.clearPlayers() {
        players.toList().forEach(::removePlayer)
    }

    private fun activateShield(
        player: ServerPlayerEntity,
        definition: WeaponDefinition,
        data: WeaponData,
        access: WeaponSkillAccess
    ): ActionResult {
        access.cresoraSetShieldHp(WeaponCombatSupport.shieldHp(definition, data))
        access.cresoraSetShieldExpireTick(currentWorldTime(player) + definition.skill.durationSeconds * 20L)
        access.cresoraSetShieldWeaponId(definition.id)
        player.sendMessage(
            Text.translatable(
                "item.cresora.weapon.skill.activated",
                Text.translatable(definition.translationKey()),
                formatNumber(WeaponCombatSupport.shieldHearts(definition, data))
            ).formatted(Formatting.AQUA),
            true
        )
        return ActionResult.SUCCESS
    }

    private fun activateHeal(
        player: ServerPlayerEntity,
        definition: WeaponDefinition,
        data: WeaponData,
        access: WeaponSkillAccess
    ): ActionResult {
        clearShield(player)
        access.cresoraSetShieldExpireTick(0L)
        access.cresoraSetShieldWeaponId(null)
        val before = player.health
        player.heal(WeaponCombatSupport.healHp(definition, data))
        val healedHearts = ((player.health - before).coerceAtLeast(0.0f) / 2.0f).toDouble()
        player.sendMessage(
            Text.translatable(
                "item.cresora.weapon.skill.heal_activated",
                Text.translatable(definition.translationKey()),
                formatNumber(healedHearts)
            ).formatted(Formatting.GREEN),
            true
        )
        return ActionResult.SUCCESS
    }

    private fun clearShield(player: ServerPlayerEntity) {
        val access = player as? WeaponSkillAccess ?: return
        access.cresoraSetShieldHp(0.0f)
        access.cresoraSetShieldExpireTick(0L)
        access.cresoraSetShieldWeaponId(null)
    }

    private fun currentWorldTime(player: ServerPlayerEntity): Long {
        return (player.world as? ServerWorld)?.time ?: 0L
    }

    private fun formatNumber(value: Double): String {
        val rounded = kotlin.math.round(value * 10.0) / 10.0
        return if (rounded % 1.0 == 0.0) rounded.toInt().toString() else rounded.toString()
    }
}
