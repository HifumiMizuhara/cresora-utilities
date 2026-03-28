package hifumi.cresora

import net.minecraft.item.ItemStack
import net.minecraft.screen.SimpleNamedScreenHandlerFactory
import net.minecraft.server.network.ServerPlayerEntity
import net.minecraft.text.Text

object ArtifactUiFlow {
    fun openDomainSelection(player: ServerPlayerEntity) {
        player.openHandledScreen(
            SimpleNamedScreenHandlerFactory(
                { syncId, playerInventory, _ -> DomainSelectionScreenHandler(syncId, playerInventory) },
                Text.translatable("screen.cresora.domain")
            )
        )
    }

    fun openDomainReward(player: ServerPlayerEntity, displayStacks: List<ItemStack>) {
        player.openHandledScreen(
            SimpleNamedScreenHandlerFactory(
                { syncId, playerInventory, _ -> DomainRewardScreenHandler(syncId, playerInventory, displayStacks) },
                Text.translatable("screen.cresora.domain.reward")
            )
        )
    }

    fun openShop(player: ServerPlayerEntity) {
        player.openHandledScreen(
            SimpleNamedScreenHandlerFactory(
                { syncId, playerInventory, _ -> ArtifactShopScreenHandler(syncId, playerInventory) },
                Text.translatable("screen.cresora.shop")
            )
        )
    }

    fun openUpgradeScreen(player: ServerPlayerEntity, baseStack: ItemStack = ItemStack.EMPTY, materialStack: ItemStack = ItemStack.EMPTY) {
        player.openHandledScreen(
            SimpleNamedScreenHandlerFactory(
                { syncId, playerInventory, _ -> UpgradeScreenHandler(syncId, playerInventory, baseStack, materialStack) },
                Text.translatable("screen.cresora.upgrade")
            )
        )
    }

    fun openAlphaSelection(player: ServerPlayerEntity, baseStack: ItemStack, materialStack: ItemStack) {
        player.openHandledScreen(
            SimpleNamedScreenHandlerFactory(
                { syncId, playerInventory, _ -> ArtifactAlphaScreenHandler(syncId, playerInventory, baseStack, materialStack) },
                Text.translatable("screen.cresora.alpha")
            )
        )
    }

    fun openBetaSelection(player: ServerPlayerEntity, baseStack: ItemStack, materialStack: ItemStack) {
        player.openHandledScreen(
            SimpleNamedScreenHandlerFactory(
                { syncId, playerInventory, _ -> ArtifactBetaScreenHandler(syncId, playerInventory, baseStack, materialStack) },
                Text.translatable("screen.cresora.beta")
            )
        )
    }
}
