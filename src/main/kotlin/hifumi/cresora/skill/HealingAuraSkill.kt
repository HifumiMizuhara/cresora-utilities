package hifumi.cresora.skill

import hifumi.cresora.WeaponCombatSupport
import hifumi.cresora.WeaponData
import hifumi.cresora.WeaponDefinition
import hifumi.cresora.WeaponSkillAccess
import hifumi.cresora.WeaponSkillService
import net.minecraft.entity.mob.HostileEntity
import net.minecraft.server.network.ServerPlayerEntity
import net.minecraft.server.world.ServerWorld
import net.minecraft.text.Text
import net.minecraft.util.ActionResult
import net.minecraft.util.Formatting
import java.util.UUID

data class BoyaState(
    val weaponId: String,
    var expireTick: Long,
    var nextPulseTick: Long,
    val intervalTicks: Long,
    val pulseHealHp: Float,
    val knockbackRadius: Double
)

object HealingAuraSkill : WeaponSkillHandler {
    val boyaStatesByPlayer: MutableMap<UUID, BoyaState> = mutableMapOf()

    override fun activate(
        player: ServerPlayerEntity,
        definition: WeaponDefinition,
        data: WeaponData,
        access: WeaponSkillAccess
    ): ActionResult {
        WeaponSkillService.clearShield(player)
        access.cresoraSetShieldExpireTick(0L)
        access.cresoraSetShieldWeaponId(null)
        val world = player.world as? ServerWorld ?: return ActionResult.FAIL
        val now = WeaponSkillService.currentWorldTime(player)
        val auraExpireTick = now + definition.skill.durationSeconds.coerceAtLeast(1) * 20L
        val immediateHealHp = WeaponCombatSupport.skillValueHp(definition, data)
        val pulseHealHp = WeaponCombatSupport.secondarySkillValueHp(definition, data).coerceAtLeast(0.0f)
        val recipients = linkedSetOf(player)
        val radius = definition.skill.radiusMeters.coerceAtLeast(0.0)
        if (radius > 0.0) {
            world.players
                .filterIsInstance<ServerPlayerEntity>()
                .filter { it.isAlive && !it.isSpectator && it.squaredDistanceTo(player) <= radius * radius }
                .forEach(recipients::add)
        }
        val (healedHp, overflowHp) = WeaponSkillService.restoreHealthOrGuard(player, immediateHealHp, auraExpireTick)
        for (recipient in recipients) {
            applyBoya(recipient, definition, pulseHealHp, auraExpireTick, now)
        }
        player.sendMessage(
            Text.translatable(
                "item.cresora.weapon.skill.healing_aura_activated",
                Text.translatable(definition.translationKey()),
                formatNumber(healedHp / 2.0),
                formatNumber(overflowHp / 2.0),
                recipients.size,
                formatNumber(pulseHealHp / 2.0),
                definition.skill.durationSeconds
            ).formatted(Formatting.GREEN),
            true
        )
        return ActionResult.SUCCESS
    }

    override fun onPlayerTick(player: ServerPlayerEntity) {
        val state = boyaStatesByPlayer[player.uuid] ?: return
        val now = WeaponSkillService.currentWorldTime(player)
        if (now >= state.expireTick || !player.isAlive) {
            boyaStatesByPlayer.remove(player.uuid)
            return
        }
        while (now >= state.nextPulseTick && state.nextPulseTick < state.expireTick) {
            WeaponSkillService.restoreHealthOrGuard(player, state.pulseHealHp, state.expireTick)
            knockbackNearbyHostiles(player, state.knockbackRadius)
            state.nextPulseTick += state.intervalTicks
        }
    }

    private fun applyBoya(
        player: ServerPlayerEntity,
        definition: WeaponDefinition,
        pulseHealHp: Float,
        expireTick: Long,
        now: Long
    ) {
        val intervalTicks = (definition.skill.tickIntervalSeconds.coerceAtLeast(0.5) * 20.0).toLong().coerceAtLeast(1L)
        boyaStatesByPlayer[player.uuid] = BoyaState(
            weaponId = definition.id,
            expireTick = expireTick,
            nextPulseTick = now + intervalTicks,
            intervalTicks = intervalTicks,
            pulseHealHp = pulseHealHp,
            knockbackRadius = definition.skill.radiusMeters.coerceAtLeast(0.0)
        )
    }

    private fun knockbackNearbyHostiles(player: ServerPlayerEntity, radius: Double) {
        if (radius <= 0.0) {
            return
        }
        val world = player.world as? ServerWorld ?: return
        val targets = world.getOtherEntities(player, player.boundingBox.expand(radius)) { entity ->
            entity is HostileEntity && entity.isAlive
        }.mapNotNull { it as? HostileEntity }
        for (target in targets) {
            target.takeKnockback(1.15, player.x - target.x, player.z - target.z)
        }
    }

    private fun formatNumber(value: Double): String {
        val rounded = kotlin.math.round(value * 10.0) / 10.0
        return if (rounded % 1.0 == 0.0) rounded.toInt().toString() else rounded.toString()
    }
}
