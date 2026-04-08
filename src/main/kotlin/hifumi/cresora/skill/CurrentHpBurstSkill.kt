package hifumi.cresora.skill

import hifumi.cresora.AdventureRankService
import hifumi.cresora.StoryService
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
import kotlin.math.roundToInt

object CurrentHpBurstSkill : WeaponSkillHandler {
    override fun activate(
        player: ServerPlayerEntity,
        definition: WeaponDefinition,
        data: WeaponData,
        access: WeaponSkillAccess
    ): ActionResult {
        WeaponSkillService.clearShield(player)
        val radius = definition.skill.radiusMeters.coerceAtLeast(0.0)
        val ratio = WeaponCombatSupport.currentHpTrueDamageRatio(definition, data)
        if (radius <= 0.0 || ratio <= 0.0) {
            player.sendMessage(Text.translatable("item.cresora.weapon.skill.none").formatted(Formatting.GRAY), true)
            return ActionResult.SUCCESS
        }
        val world = player.world as? ServerWorld ?: return ActionResult.FAIL
        val targets = world.getOtherEntities(player, player.boundingBox.expand(radius)) { entity ->
            entity is HostileEntity && entity.isAlive
        }.mapNotNull { it as? HostileEntity }
        var hitCount = 0
        var totalDamage = 0.0
        for (target in targets) {
            if (!StoryService.allowsTrueDamage(target)) {
                continue
            }
            val damage = (target.health.toDouble() * ratio).coerceAtLeast(0.0)
            if (damage <= 0.0) {
                continue
            }
            target.health = (target.health.toDouble() - damage).coerceAtLeast(0.001).toFloat()
            AdventureRankService.showMobTrueDamage(target, player, damage.toFloat())
            AdventureRankService.refreshMobDisplay(target)
            hitCount++
            totalDamage += damage
        }
        player.sendMessage(
            Text.translatable(
                "item.cresora.weapon.skill.current_hp_true_damage_activated",
                Text.translatable(definition.translationKey()),
                hitCount,
                formatNumber(totalDamage / 2.0)
            ).formatted(Formatting.AQUA),
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
