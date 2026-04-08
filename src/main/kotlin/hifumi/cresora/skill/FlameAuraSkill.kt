package hifumi.cresora.skill

import hifumi.cresora.WeaponCombatSupport
import hifumi.cresora.WeaponData
import hifumi.cresora.WeaponDefinition
import hifumi.cresora.WeaponSkillAccess
import hifumi.cresora.WeaponSkillService
import net.minecraft.entity.effect.StatusEffectInstance
import net.minecraft.entity.effect.StatusEffects
import net.minecraft.entity.mob.MobEntity
import net.minecraft.server.network.ServerPlayerEntity
import net.minecraft.server.world.ServerWorld
import net.minecraft.text.Text
import net.minecraft.util.ActionResult
import net.minecraft.util.Formatting
import kotlin.math.roundToInt

object FlameAuraSkill : WeaponSkillHandler {
    override fun activate(
        player: ServerPlayerEntity,
        definition: WeaponDefinition,
        data: WeaponData,
        access: WeaponSkillAccess
    ): ActionResult {
        WeaponSkillService.clearShield(player)
        val world = player.world as? ServerWorld ?: return ActionResult.FAIL
        val radius = definition.skill.radiusMeters.coerceAtLeast(0.0)
        val burnSeconds = WeaponCombatSupport.skillValuePercent(definition, data).coerceAtLeast(0.0)
        val burnTicks = (burnSeconds * 20.0).toInt().coerceAtLeast(20)
        val resistanceSeconds = definition.skill.durationSeconds.coerceAtLeast(1)
        player.addStatusEffect(StatusEffectInstance(StatusEffects.FIRE_RESISTANCE, resistanceSeconds * 20, 0, false, true, true))
        val targets = world.getOtherEntities(player, player.boundingBox.expand(radius)) { entity ->
            entity is MobEntity && entity.isAlive
        }.mapNotNull { it as? MobEntity }
        var ignitedCount = 0
        for (target in targets) {
            target.setOnFireForTicks(burnTicks)
            ignitedCount++
        }
        player.sendMessage(
            Text.translatable(
                "item.cresora.weapon.skill.flame_aura_activated",
                Text.translatable(definition.translationKey()),
                ignitedCount,
                formatNumber(burnSeconds),
                resistanceSeconds
            ).formatted(Formatting.GOLD),
            true
        )
        return ActionResult.SUCCESS
    }

    private fun formatNumber(value: Double): String {
        val rounded = kotlin.math.round(value * 10.0) / 10.0
        return if (kotlin.math.abs(rounded - rounded.roundToInt().toDouble()) < 1.0e-6) {
            rounded.roundToInt().toString()
        } else {
            rounded.toString()
        }
    }
}
