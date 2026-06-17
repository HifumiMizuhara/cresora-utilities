package hifumi.cresora

import hifumi.cresora.domain.DomainRewardScreen
import hifumi.cresora.domain.DomainSelectionScreen
import hifumi.cresora.domain.LeyLineSelectionScreen
import hifumi.cresora.equipment.ArtifactAlphaScreen
import hifumi.cresora.equipment.ArtifactBetaScreen
import hifumi.cresora.equipment.ArtifactShopScreen
import hifumi.cresora.masquerade.MasqueradeLoadoutScreen
import hifumi.cresora.masquerade.MasqueradeSupportScreen
import hifumi.cresora.resonance.ResonanceFeaturedSelectionScreen
import hifumi.cresora.resonance.ResonanceResultScreen
import hifumi.cresora.resonance.ResonanceScreen
import hifumi.cresora.npc.SpiritGuideRenderer
import hifumi.cresora.story.StoryChapterSelectionScreen
import hifumi.cresora.story.StoryDialogueClient
import hifumi.cresora.story.StoryStageSelectionScreen
import hifumi.cresora.weapon.SpiritRenderer
import hifumi.cresora.weapon.WeaponSkillMaterialScreen
import hifumi.cresora.weapon.WeaponUpgradeScreen
import hifumi.cresora.guide.GuideScreen
import net.fabricmc.api.ClientModInitializer
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry
import net.minecraft.client.gui.screen.ingame.HandledScreens

object CreSoraUtilitiesClient : ClientModInitializer {
	override fun onInitializeClient() {
		StoryDialogueClient.init()
		SpiritRenderer.init()
		EntityRendererRegistry.register(hifumi.cresora.npc.SpiritGuideEntity.ENTITY_TYPE, ::SpiritGuideRenderer)
		HandledScreens.register(CreSoraUtilities.UPGRADE_SCREEN_HANDLER, ::UpgradeScreen)
		HandledScreens.register(CreSoraUtilities.WEAPON_UPGRADE_SCREEN_HANDLER, ::WeaponUpgradeScreen)
		HandledScreens.register(CreSoraUtilities.WEAPON_SKILL_MATERIAL_SCREEN_HANDLER, ::WeaponSkillMaterialScreen)
		HandledScreens.register(CreSoraUtilities.CRESORA_MENU_SCREEN_HANDLER, ::CresoraMenuScreen)
		HandledScreens.register(CreSoraUtilities.DOMAIN_SELECTION_SCREEN_HANDLER, ::DomainSelectionScreen)
		HandledScreens.register(CreSoraUtilities.LEY_LINE_SELECTION_SCREEN_HANDLER, ::LeyLineSelectionScreen)
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
		HandledScreens.register(CreSoraUtilities.RESONANCE_FEATURED_SELECTION_SCREEN_HANDLER, ::ResonanceFeaturedSelectionScreen)
		HandledScreens.register(CreSoraUtilities.GUIDE_SCREEN_HANDLER, ::GuideScreen)
	}
}
