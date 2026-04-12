package hifumi.cresora.skill.generated

import hifumi.cresora.WeaponData
import hifumi.cresora.WeaponDefinition
import hifumi.cresora.WeaponSkillAccess
import hifumi.cresora.WeaponSkillService
import hifumi.cresora.skill.WeaponSkillHandler
import net.minecraft.entity.LivingEntity
import net.minecraft.entity.effect.StatusEffectInstance
import net.minecraft.registry.Registries
import net.minecraft.server.network.ServerPlayerEntity
import net.minecraft.text.Text
import net.minecraft.util.ActionResult
import net.minecraft.util.Formatting
import net.minecraft.util.Identifier

public object RequiemTowardDawnSkill : WeaponSkillHandler {
  override fun activate(
    player: ServerPlayerEntity,
    definition: WeaponDefinition,
    `data`: WeaponData,
    access: WeaponSkillAccess,
  ): ActionResult {
    WeaponSkillService.startCooldown(player, definition.id, definition.skill.cooldownSeconds * 20L)
    WeaponSkillService.showCooldownBar(player, definition)

    player.addStatusEffect(StatusEffectInstance(Registries.STATUS_EFFECT.getEntry(Identifier.of("minecraft:fire_resistance")).get(),
        10.toInt() * 20, 0.toInt()))

    player.world.getNonSpectatingEntities(LivingEntity::class.java,
        player.boundingBox.expand(4.0.toDouble())).forEach { target ->
        if (target != player) {
            // Sub-actions for AOE
            target.setOnFireFor(5.toFloat())
        }
    }
    player.sendMessage(Text.translatable("item.cresora.weapon.skill.flame_aura_activated.generic").formatted(Formatting.GOLD),
        true)


    return ActionResult.SUCCESS
  }
}
