package hifumi.cresora

import net.fabricmc.api.ClientModInitializer
import net.minecraft.client.gui.screen.ingame.HandledScreens

object CreSoraUtilitiesClient : ClientModInitializer {
	override fun onInitializeClient() {
		StoryDialogueClient.init()
		HandledScreens.register(CreSoraUtilities.UPGRADE_SCREEN_HANDLER, ::UpgradeScreen)
		HandledScreens.register(CreSoraUtilities.WEAPON_UPGRADE_SCREEN_HANDLER, ::WeaponUpgradeScreen)
		HandledScreens.register(CreSoraUtilities.WEAPON_SKILL_MATERIAL_SCREEN_HANDLER, ::WeaponSkillMaterialScreen)
		HandledScreens.register(CreSoraUtilities.CRESORA_MENU_SCREEN_HANDLER, ::CresoraMenuScreen)
		HandledScreens.register(CreSoraUtilities.DOMAIN_SELECTION_SCREEN_HANDLER, ::DomainSelectionScreen)
		HandledScreens.register(CreSoraUtilities.DOMAIN_REWARD_SCREEN_HANDLER, ::DomainRewardScreen)
		HandledScreens.register(CreSoraUtilities.STORY_CHAPTER_SELECTION_SCREEN_HANDLER, ::StoryChapterSelectionScreen)
		HandledScreens.register(CreSoraUtilities.STORY_STAGE_SELECTION_SCREEN_HANDLER, ::StoryStageSelectionScreen)
		HandledScreens.register(CreSoraUtilities.MASQUERADE_LOADOUT_SCREEN_HANDLER, ::MasqueradeLoadoutScreen)
		HandledScreens.register(CreSoraUtilities.MASQUERADE_SUPPORT_SCREEN_HANDLER, ::MasqueradeSupportScreen)
		HandledScreens.register(CreSoraUtilities.ARTIFACT_SHOP_SCREEN_HANDLER, ::ArtifactShopScreen)
		HandledScreens.register(CreSoraUtilities.ARTIFACT_ALPHA_SCREEN_HANDLER, ::ArtifactAlphaScreen)
		HandledScreens.register(CreSoraUtilities.ARTIFACT_BETA_SCREEN_HANDLER, ::ArtifactBetaScreen)
		HandledScreens.register(CreSoraUtilities.RESONANCE_SCREEN_HANDLER, ::ResonanceScreen)
		HandledScreens.register(CreSoraUtilities.RESONANCE_RESULT_SCREEN_HANDLER, ::ResonanceResultScreen)
	}
}
