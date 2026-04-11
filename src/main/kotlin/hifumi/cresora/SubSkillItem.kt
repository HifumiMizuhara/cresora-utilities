package hifumi.cresora

import hifumi.cresora.skill.WeaponSkillRegistry
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.item.Item
import net.minecraft.item.ItemStack
import net.minecraft.server.network.ServerPlayerEntity
import net.minecraft.util.ActionResult
import net.minecraft.util.Hand
import net.minecraft.world.World

class SubSkillItem(settings: Settings) : Item(settings) {
    override fun use(world: World, user: PlayerEntity, hand: Hand): ActionResult {
        val stack = user.getStackInHand(hand)
        if (world.isClient || user !is ServerPlayerEntity) {
            return ActionResult.PASS
        }

        val effectId = stack.get(ModDataComponents.SUB_SKILL_EFFECT_ID)
        if (effectId != null) {
            val handler = WeaponSkillRegistry.getHandler(effectId)
            if (handler != null) {
                HotbarOverrideService.restoreHotbar(user)
                handler.activate(user, WeaponDefinition.DUMMY, WeaponData.DUMMY, WeaponSkillAccess.DUMMY)
            }
        }

        return ActionResult.SUCCESS
    }
}
