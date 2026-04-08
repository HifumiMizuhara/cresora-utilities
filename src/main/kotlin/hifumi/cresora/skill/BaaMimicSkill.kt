package hifumi.cresora.skill

import hifumi.cresora.*
import net.minecraft.entity.EntityType
import net.minecraft.entity.SpawnReason
import net.minecraft.entity.attribute.EntityAttributes
import net.minecraft.entity.mob.HostileEntity
import net.minecraft.server.network.ServerPlayerEntity
import net.minecraft.server.world.ServerWorld
import net.minecraft.text.Text
import net.minecraft.util.ActionResult
import net.minecraft.util.Formatting

object BaaMimicSkill : WeaponSkillHandler {
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
        val world = player.world as? ServerWorld ?: return ActionResult.FAIL
        val radius = definition.skill.radiusMeters.coerceAtLeast(0.0)

        val n = when {
            data.baseLevel >= 61 -> 3
            data.baseLevel >= 41 -> 2
            else -> 1
        }

        val maxPlayerHp = world.players
            .filter { it.squaredDistanceTo(player.x, player.y, player.z) < 100.0 }
            .maxOfOrNull { it.maxHealth } ?: 20.0f

        val targets = world.getOtherEntities(player, player.boundingBox.expand(radius)) { entity ->
            val hostile = entity as? HostileEntity ?: return@getOtherEntities false
            if (!hostile.isAlive) {
                return@getOtherEntities false
            }
            val mobAccess = hostile as? AdventureRankMobAccess
            mobAccess?.cresoraIsEliteMob() != true
        }
            .mapNotNull { it as? HostileEntity }
            .sortedBy { it.squaredDistanceTo(player) }
            .take(n)

        for (target in targets) {
            val sheep = EntityType.SHEEP.spawn(world, null, target.blockPos, SpawnReason.COMMAND, true, false) ?: continue
            sheep.refreshPositionAndAngles(target.x, target.y, target.z, target.yaw, target.pitch)
            BaaMimicService.markTransformedSheep(sheep, target)
            val maxHealthAttr = sheep.getAttributeInstance(EntityAttributes.MAX_HEALTH)
            if (maxHealthAttr != null) {
                maxHealthAttr.baseValue = maxPlayerHp.toDouble()
                sheep.health = maxPlayerHp
            }
            AdventureRankService.refreshMobDisplay(sheep)
            target.discard()
        }

        player.sendMessage(
            Text.translatable(
                "item.cresora.weapon.skill.baa_mimic_activated",
                Text.translatable(definition.translationKey()),
                targets.size
            ).formatted(Formatting.GREEN),
            true
        )
        return ActionResult.SUCCESS
    }
}
