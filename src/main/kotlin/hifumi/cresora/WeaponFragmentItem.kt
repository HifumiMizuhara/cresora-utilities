package hifumi.cresora

import net.minecraft.entity.player.PlayerEntity
import net.minecraft.item.Item
import net.minecraft.server.network.ServerPlayerEntity
import net.minecraft.text.Text
import net.minecraft.util.ActionResult
import net.minecraft.util.Formatting
import net.minecraft.util.Hand
import net.minecraft.world.World

class WeaponFragmentItem(
    val definition: WeaponDefinitionRef,
    settings: Settings
) : Item(settings) {
    override fun use(world: World, user: PlayerEntity, hand: Hand): ActionResult {
        if (world.isClient) {
            return ActionResult.SUCCESS
        }
        val serverPlayer = user as? ServerPlayerEntity ?: return ActionResult.FAIL
        val weaponDefinition = definition.resolve()
        val required = weaponDefinition.craft.fragmentsRequired
        val available = WeaponStackSupport.countFragments(serverPlayer, weaponDefinition)
        if (available < required) {
            serverPlayer.sendMessage(
                Text.translatable("item.cresora.weapon.fragment.not_enough", required, available).formatted(Formatting.RED),
                false
            )
            return ActionResult.FAIL
        }
        if (!WeaponStackSupport.removeFragments(serverPlayer, weaponDefinition, required)) {
            serverPlayer.sendMessage(Text.translatable("item.cresora.weapon.fragment.not_enough", required, available).formatted(Formatting.RED), false)
            return ActionResult.FAIL
        }
        val weaponStack = WeaponStackSupport.createWeaponStack(
            weaponDefinition,
            weaponDefinition.craft.craftedRarity,
            weaponDefinition.craft.craftedBaseLevel,
            weaponDefinition.craft.craftedSkillLevel
        )
        serverPlayer.giveItemStack(weaponStack)
        serverPlayer.sendMessage(
            Text.translatable("item.cresora.weapon.fragment.crafted", Text.translatable(weaponDefinition.translationKey())).formatted(Formatting.GOLD),
            false
        )
        return ActionResult.SUCCESS
    }
}
