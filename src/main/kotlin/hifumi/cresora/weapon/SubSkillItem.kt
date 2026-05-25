package hifumi.cresora.weapon

import hifumi.cresora.ModDataComponents
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.item.Item
import net.minecraft.item.ItemStack
import net.minecraft.server.network.ServerPlayerEntity
import net.minecraft.util.ActionResult
import net.minecraft.util.Hand
import net.minecraft.world.World

import net.minecraft.text.Text

class SubSkillItem(settings: Settings) : Item(settings) {
    override fun getName(stack: ItemStack): Text {
        val effectId = stack.get(ModDataComponents.SUB_SKILL_EFFECT_ID)
        return if (effectId != null) {
            Text.translatable("item.cresora-utilities.sub_skill.$effectId")
        } else {
            super.getName(stack)
        }
    }

    override fun use(world: World, user: PlayerEntity, hand: Hand): ActionResult {
        val stack = user.getStackInHand(hand)
        if (world.isClient || user !is ServerPlayerEntity) {
            return ActionResult.PASS
        }

        val effectId = stack.get(ModDataComponents.SUB_SKILL_EFFECT_ID)
        if (effectId != null) {
            val access = user as? WeaponSkillAccess ?: return ActionResult.FAIL
            return HotbarOverrideService.activateSubSkill(user, effectId, access)
        }

        return ActionResult.PASS
    }
}
