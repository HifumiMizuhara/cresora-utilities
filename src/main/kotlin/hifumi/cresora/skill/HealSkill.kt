package hifumi.cresora.skill

import hifumi.cresora.*
import net.minecraft.server.network.ServerPlayerEntity
import net.minecraft.text.Text
import net.minecraft.util.ActionResult
import net.minecraft.util.Formatting

object HealSkill : WeaponSkillHandler {
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
        val before = player.health
        player.heal(WeaponCombatSupport.healHp(definition, data))
        val healedHearts = ((player.health - before).coerceAtLeast(0.0f) / 2.0f).toDouble()
        player.sendMessage(
            Text.translatable(
                "item.cresora.weapon.skill.heal_activated",
                Text.translatable(definition.translationKey()),
                WeaponSkillService.formatNumber(healedHearts)
            ).formatted(Formatting.GREEN),
            true
        )
        return ActionResult.SUCCESS
    }
}
