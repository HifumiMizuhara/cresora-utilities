package hifumi.cresora

import net.fabricmc.api.ClientModInitializer
import net.minecraft.client.gui.screen.ingame.HandledScreens

object CreSoraUtilitiesClient : ClientModInitializer {
	override fun onInitializeClient() {
		HandledScreens.register(CreSoraUtilities.UPGRADE_SCREEN_HANDLER, ::UpgradeScreen)
	}
}
