package hifumi.cresora.equipment
import hifumi.cresora.CresoraMenuScreenHandler
import hifumi.cresora.UpgradeScreenHandler
import hifumi.cresora.domain.DomainRewardScreenHandler
import hifumi.cresora.domain.DomainSelectionScreenHandler
import hifumi.cresora.masquerade.MasqueradeLoadoutScreenHandler
import hifumi.cresora.masquerade.MasqueradeSupportScreenHandler
import hifumi.cresora.resonance.ResonanceFeaturedSelectionScreenHandler
import hifumi.cresora.resonance.ResonanceResultScreenHandler
import hifumi.cresora.resonance.ResonanceScreenHandler
import hifumi.cresora.resonance.ResonanceService
import hifumi.cresora.story.StoryChapterSelectionScreenHandler
import hifumi.cresora.story.StoryStageSelectionScreenHandler
import hifumi.cresora.weapon.WeaponSkillMaterialScreenHandler
import hifumi.cresora.weapon.WeaponUpgradeScreenHandler
import net.minecraft.item.ItemStack
import net.minecraft.screen.SimpleNamedScreenHandlerFactory
import net.minecraft.server.network.ServerPlayerEntity
import net.minecraft.text.Text

object ArtifactUiFlow {
    fun openMenu(player: ServerPlayerEntity) {
        player.openHandledScreen(
            SimpleNamedScreenHandlerFactory(
                { syncId, playerInventory, _ -> CresoraMenuScreenHandler(syncId, playerInventory) },
                Text.translatable("screen.cresora.menu")
            )
        )
    }

    fun openStoryChapterSelection(player: ServerPlayerEntity) {
        player.openHandledScreen(
            SimpleNamedScreenHandlerFactory(
                { syncId, playerInventory, _ -> StoryChapterSelectionScreenHandler(syncId, playerInventory) },
                Text.translatable("screen.cresora.story")
            )
        )
    }

    fun openStoryStageSelection(player: ServerPlayerEntity, chapterGroup: String) {
        player.openHandledScreen(
            SimpleNamedScreenHandlerFactory(
                { syncId, playerInventory, _ -> StoryStageSelectionScreenHandler(syncId, playerInventory, chapterGroup) },
                Text.translatable("screen.cresora.story.chapter_group_title", chapterGroup)
            )
        )
    }

    fun openRewardSummary(player: ServerPlayerEntity, title: Text, displayStacks: List<ItemStack>) {
        player.openHandledScreen(
            SimpleNamedScreenHandlerFactory(
                { syncId, playerInventory, _ -> DomainRewardScreenHandler(syncId, playerInventory, displayStacks) },
                title
            )
        )
    }

    fun openWeaponUpgrade(player: ServerPlayerEntity, weaponStack: ItemStack = ItemStack.EMPTY) {
        player.openHandledScreen(
            SimpleNamedScreenHandlerFactory(
                { syncId, playerInventory, _ -> WeaponUpgradeScreenHandler(syncId, playerInventory, weaponStack) },
                Text.translatable("screen.cresora.weapon_upgrade")
            )
        )
    }

    fun openDomainSelection(player: ServerPlayerEntity) {
        player.openHandledScreen(
            SimpleNamedScreenHandlerFactory(
                { syncId, playerInventory, _ -> DomainSelectionScreenHandler(syncId, playerInventory) },
                Text.translatable("screen.cresora.domain")
            )
        )
    }

    fun openMasqueradeLoadout(player: ServerPlayerEntity) {
        player.openHandledScreen(
            SimpleNamedScreenHandlerFactory(
                { syncId, playerInventory, _ -> MasqueradeLoadoutScreenHandler(syncId, playerInventory) },
                Text.translatable("screen.cresora.masquerade")
            )
        )
    }

    fun openMasqueradeSupport(player: ServerPlayerEntity) {
        player.openHandledScreen(
            SimpleNamedScreenHandlerFactory(
                { syncId, playerInventory, _ -> MasqueradeSupportScreenHandler(syncId, playerInventory) },
                Text.translatable("screen.cresora.masquerade.support")
            )
        )
    }

    fun openDomainReward(player: ServerPlayerEntity, displayStacks: List<ItemStack>) {
        openRewardSummary(player, Text.translatable("screen.cresora.domain.reward"), displayStacks)
    }

    fun openShop(player: ServerPlayerEntity) {
        player.openHandledScreen(
            SimpleNamedScreenHandlerFactory(
                { syncId, playerInventory, _ -> ArtifactShopScreenHandler(syncId, playerInventory) },
                Text.translatable("screen.cresora.shop")
            )
        )
    }

    fun openResonance(player: ServerPlayerEntity) {
        player.openHandledScreen(
            SimpleNamedScreenHandlerFactory(
                { syncId, playerInventory, _ -> ResonanceScreenHandler(syncId, playerInventory) },
                Text.translatable("screen.cresora.resonance")
            )
        )
    }

    fun openResonanceResult(player: ServerPlayerEntity, result: ResonanceService.PullResult) {
        player.openHandledScreen(
            SimpleNamedScreenHandlerFactory(
                { syncId, playerInventory, _ -> ResonanceResultScreenHandler(syncId, playerInventory, result) },
                Text.translatable("screen.cresora.resonance.result")
            )
        )
    }

    fun openResonanceFeaturedSelection(player: ServerPlayerEntity) {
        player.openHandledScreen(
            SimpleNamedScreenHandlerFactory(
                { syncId, playerInventory, _ -> ResonanceFeaturedSelectionScreenHandler(syncId, playerInventory) },
                Text.translatable("screen.cresora.resonance.featured_selection")
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

    fun openWeaponSkillMaterialSelection(player: ServerPlayerEntity, weaponStack: ItemStack) {
        player.openHandledScreen(
            SimpleNamedScreenHandlerFactory(
                { syncId, playerInventory, _ -> WeaponSkillMaterialScreenHandler(syncId, playerInventory, weaponStack) },
                Text.translatable("screen.cresora.weapon_skill_material")
            )
        )
    }
}
