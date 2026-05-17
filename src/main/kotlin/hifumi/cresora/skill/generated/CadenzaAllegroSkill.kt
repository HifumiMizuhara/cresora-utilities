package hifumi.cresora.skill.generated

import hifumi.cresora.WeaponData
import hifumi.cresora.WeaponDefinition
import hifumi.cresora.WeaponSkillAccess
import hifumi.cresora.WeaponSkillService
import hifumi.cresora.skill.WeaponSkillHandler
import net.minecraft.server.network.ServerPlayerEntity
import net.minecraft.util.ActionResult

public object CadenzaAllegroSkill : WeaponSkillHandler {
  override fun activate(
    player: ServerPlayerEntity,
    definition: WeaponDefinition,
    `data`: WeaponData,
    access: WeaponSkillAccess,
  ): ActionResult {
    WeaponSkillService.startCooldown(player, definition.id, definition.skill.cooldownSeconds * 20L)
    WeaponSkillService.showCooldownBar(player, definition)


    ; run execute@ {
      val world = player.world as? net.minecraft.server.world.ServerWorld ?: return@execute;
                      val radius = definition.skill.radiusMeters;
                      val n = if (data.baseLevel >= 61) 3 else if (data.baseLevel >= 41) 2 else 1;
                      val maxPlayerHp = world.players.filter { it.squaredDistanceTo(player.x,
          player.y, player.z) < 100.0 }.maxOfOrNull { it.maxHealth } ?: 20.0f;

                      val targets = world.getOtherEntities(player,
          player.boundingBox.expand(radius)) { entity ->
                          val hostile = entity as? net.minecraft.entity.mob.HostileEntity ?:
          return@getOtherEntities false;
                          if (!hostile.isAlive) return@getOtherEntities false;
                          val mobAccess = hostile as? hifumi.cresora.AdventureRankMobAccess;
                          mobAccess?.cresoraIsEliteMob() != true
                      }.mapNotNull { it as? net.minecraft.entity.mob.HostileEntity }.sortedBy {
          it.squaredDistanceTo(player) }.take(n);

                      var transformedCount = 0;
                      for (target in targets) {
                          val sheep = net.minecraft.entity.EntityType.SHEEP.spawn(world, null,
          target.blockPos, net.minecraft.entity.SpawnReason.COMMAND, true, false) ?: continue;
                          sheep!!.refreshPositionAndAngles(target.x, target.y, target.z, target.yaw,
          target.pitch);
                          hifumi.cresora.BaaMimicService.markTransformedSheep(sheep!!, target);
                          val maxHealthAttr =
          sheep!!.getAttributeInstance(net.minecraft.entity.attribute.EntityAttributes.MAX_HEALTH);
                          if (maxHealthAttr != null) {
                              maxHealthAttr.baseValue = maxPlayerHp.toDouble();
                              sheep!!.health = maxPlayerHp;
                          }
                          hifumi.cresora.AdventureRankService.refreshMobDisplay(sheep!!);
                          target.discard();
                          transformedCount++;
                      }
                      if (transformedCount > 0) {
                         
          player.sendMessage(net.minecraft.text.Text.translatable("item.cresora.weapon.skill.baa_mimic_activated",
          net.minecraft.text.Text.translatable(definition.translationKey()),
          transformedCount).formatted(net.minecraft.util.Formatting.GREEN), true);
                      } else {
                         
          player.sendMessage(net.minecraft.text.Text.translatable("item.cresora.weapon.skill.baa_mimic_activated.generic").formatted(net.minecraft.util.Formatting.GRAY),
          true);
                      }
    }


    return ActionResult.SUCCESS
  }
}
