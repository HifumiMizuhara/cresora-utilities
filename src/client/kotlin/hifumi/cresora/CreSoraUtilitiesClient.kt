package hifumi.cresora

import net.fabricmc.api.ClientModInitializer
import net.minecraft.client.gui.screen.ingame.HandledScreens

object CreSoraUtilitiesClient : ClientModInitializer {
	override fun onInitializeClient() {
		HandledScreens.register(CreSoraUtilities.UPGRADE_SCREEN_HANDLER, ::UpgradeScreen)
		HandledScreens.register(CreSoraUtilities.WEAPON_UPGRADE_SCREEN_HANDLER, ::WeaponUpgradeScreen)
		HandledScreens.register(CreSoraUtilities.DOMAIN_SELECTION_SCREEN_HANDLER, ::DomainSelectionScreen)
		HandledScreens.register(CreSoraUtilities.DOMAIN_REWARD_SCREEN_HANDLER, ::DomainRewardScreen)
		HandledScreens.register(CreSoraUtilities.ARTIFACT_SHOP_SCREEN_HANDLER, ::ArtifactShopScreen)
		HandledScreens.register(CreSoraUtilities.ARTIFACT_ALPHA_SCREEN_HANDLER, ::ArtifactAlphaScreen)
		HandledScreens.register(CreSoraUtilities.ARTIFACT_BETA_SCREEN_HANDLER, ::ArtifactBetaScreen)
	}
}
