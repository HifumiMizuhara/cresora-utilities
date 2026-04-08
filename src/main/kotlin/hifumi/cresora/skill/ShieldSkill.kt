package hifumi.cresora.skill

import hifumi.cresora.*
import net.minecraft.server.network.ServerPlayerEntity
import net.minecraft.text.Text
import net.minecraft.util.ActionResult
import net.minecraft.util.Formatting

object ShieldSkill : WeaponSkillHandler {
    override fun activate(
        player: ServerPlayerEntity,
        definition: WeaponDefinition,
        data: WeaponData,
        access: WeaponSkillAccess
    ): ActionResult {
        WeaponSkillService.startCooldown(player, definition.id, definition.skill.cooldownSeconds * 20L)
        WeaponSkillService.showCooldownBar(player, definition)
        access.cresoraSetShieldHp(WeaponCombatSupport.shieldHp(definition, data))
        access.cresoraSetShieldExpireTick(WeaponSkillService.currentWorldTime(player) + definition.skill.durationSeconds * 20L)
        access.cresoraSetShieldWeaponId(definition.id)
        player.sendMessage(
            Text.translatable(
                "item.cresora.weapon.skill.activated",
                Text.translatable(definition.translationKey()),
                WeaponSkillService.formatNumber(WeaponCombatSupport.shieldHearts(definition, data))
            ).formatted(Formatting.AQUA),
            true
        )
        return ActionResult.SUCCESS
    }

    override fun onPlayerTick(player: ServerPlayerEntity) {
        WeaponSkillService.clearExpiredShield(player)
    }

    override fun onDamageAbsorbed(player: ServerPlayerEntity, amount: Float): Float {
        WeaponSkillService.clearExpiredShield(player)
        val access = player as? WeaponSkillAccess ?: return amount
        val remainingShield = access.cresoraGetShieldHp()
        if (remainingShield <= 0.0f) {
            return amount
        }
        var remainingAmount = amount
        if (remainingAmount <= remainingShield) {
            access.cresoraSetShieldHp(remainingShield - remainingAmount)
            if (access.cresoraGetShieldHp() <= 0.0f) {
                WeaponSkillService.clearShield(player)
            }
            player.sendMessage(
                Text.translatable("item.cresora.weapon.skill.blocked", WeaponSkillService.formatNumber(remainingAmount / 2.0)).formatted(Formatting.AQUA),
                true
            )
            return 0.0f
        }
        access.cresoraSetShieldHp(0.0f)
        WeaponSkillService.clearShield(player)
        player.sendMessage(
            Text.translatable("item.cresora.weapon.skill.broken").formatted(Formatting.RED),
            true
        )
        return remainingAmount - remainingShield
    }
}
