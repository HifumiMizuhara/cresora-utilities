package hifumi.cresora.skill

import hifumi.cresora.*
import net.minecraft.entity.effect.StatusEffectInstance
import net.minecraft.entity.effect.StatusEffects
import net.minecraft.server.network.ServerPlayerEntity
import net.minecraft.server.world.ServerWorld
import net.minecraft.text.Text
import net.minecraft.util.ActionResult
import net.minecraft.util.Formatting
import java.util.*

object SunlitHasteSkill : WeaponSkillHandler {
    private data class SunlitHasteState(
        val weaponId: String,
        var expireTick: Long,
        val normalAmplifier: Int,
        val sunlightAmplifier: Int
    )

    private val sunlitHasteStatesByPlayer: MutableMap<UUID, SunlitHasteState> = mutableMapOf()

    override fun activate(
        player: ServerPlayerEntity,
        definition: WeaponDefinition,
        data: WeaponData,
        access: WeaponSkillAccess
    ): ActionResult {
        WeaponSkillService.startCooldown(player, definition.id, definition.skill.cooldownSeconds * 20L)
        WeaponSkillService.showCooldownBar(player, definition)
        WeaponSkillService.clearShield(player)
        access.cresoraSetShieldExpireTick(0L)
        access.cresoraSetShieldWeaponId(null)
        val now = WeaponSkillService.currentWorldTime(player)
        val expireTick = now + definition.skill.durationSeconds.coerceAtLeast(1) * 20L
        val normalPercent = WeaponCombatSupport.skillValuePercent(definition, data).coerceAtLeast(0.0)
        val sunlightPercent = WeaponCombatSupport.secondarySkillValuePercent(definition, data).coerceAtLeast(normalPercent)
        val state = SunlitHasteState(
            weaponId = definition.id,
            expireTick = expireTick,
            normalAmplifier = speedAmplifier(normalPercent),
            sunlightAmplifier = speedAmplifier(sunlightPercent)
        )
        sunlitHasteStatesByPlayer[player.uuid] = state
        refreshSunlitHasteEffect(player, state)
        player.sendMessage(
            Text.translatable(
                "item.cresora.weapon.skill.sunlit_haste_activated",
                Text.translatable(definition.translationKey()),
                definition.skill.durationSeconds,
                WeaponSkillService.formatNumber(normalPercent),
                WeaponSkillService.formatNumber(sunlightPercent)
            ).formatted(Formatting.YELLOW),
            true
        )
        return ActionResult.SUCCESS
    }

    override fun onPlayerTick(player: ServerPlayerEntity) {
        val state = sunlitHasteStatesByPlayer[player.uuid] ?: return
        val now = WeaponSkillService.currentWorldTime(player)
        if (now >= state.expireTick || !player.isAlive) {
            sunlitHasteStatesByPlayer.remove(player.uuid)
            return
        }
        refreshSunlitHasteEffect(player, state)
    }

    private fun refreshSunlitHasteEffect(player: ServerPlayerEntity, state: SunlitHasteState) {
        val amplifier = if (isSunlitEnvironment(player)) state.sunlightAmplifier else state.normalAmplifier
        val current = player.getStatusEffect(StatusEffects.SPEED)
        if (current != null && current.amplifier == amplifier && current.duration > 10) {
            return
        }
        player.addStatusEffect(StatusEffectInstance(StatusEffects.SPEED, 30, amplifier, false, false, true))
    }

    private fun isSunlitEnvironment(player: ServerPlayerEntity): Boolean {
        val world = player.world as? ServerWorld ?: return false
        if (!world.dimension.hasSkyLight() || !world.isDay || world.isRaining || world.isThundering) {
            return false
        }
        return world.isSkyVisible(player.blockPos.up())
    }

    private fun speedAmplifier(percent: Double): Int {
        return kotlin.math.ceil(percent.coerceAtLeast(0.0) / 20.0).toInt().coerceAtLeast(1) - 1
    }
}
