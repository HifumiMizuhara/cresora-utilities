package hifumi.cresora

import net.minecraft.entity.player.PlayerEntity
import net.minecraft.screen.SimpleNamedScreenHandlerFactory
import net.minecraft.text.Text
import net.minecraft.util.Hand
import net.minecraft.util.ActionResult
import net.minecraft.world.World

class Hinagatas_Tue(settings: Settings) : ArtifactEquipmentItem(EquipmentDefinitions.HINAGATA_WAND, settings) {
    override fun use(world: World, user: PlayerEntity, hand: Hand): ActionResult {
        if (hand == Hand.MAIN_HAND) {
            if (!world.isClient) {
                user.openHandledScreen(
                    SimpleNamedScreenHandlerFactory(
                        { syncId, playerInventory, _ -> UpgradeScreenHandler(syncId, playerInventory) },
                        Text.translatable("screen.cresora.upgrade")
                    )
                )
            }
            return ActionResult.SUCCESS
        }

        return ActionResult.PASS
    }
}
