package hifumi.cresora

import hifumi.cresora.adventurerank.AdventureRankHooks
import hifumi.cresora.adventurerank.AdventureRankProfile
import hifumi.cresora.adventurerank.AdventureRankProgression
import hifumi.cresora.adventurerank.AdventureRankService
import hifumi.cresora.bloodmoon.BloodMoonHooks
import hifumi.cresora.bloodmoon.MoonAltarService
import hifumi.cresora.bloodmoon.MoonPhaseHooks
import hifumi.cresora.combat.MobCombatProfileRegistry
import hifumi.cresora.combat.NaturalRegenService
import hifumi.cresora.debuff.CresoraDebuffHooks
import hifumi.cresora.domain.DomainContentRegistry
import hifumi.cresora.domain.DomainHooks
import hifumi.cresora.domain.DomainRewardProfileRegistry
import hifumi.cresora.domain.DomainRewardScreenHandler
import hifumi.cresora.domain.DomainSelectionScreenHandler
import hifumi.cresora.equipment.ArtifactAlphaScreenHandler
import hifumi.cresora.equipment.ArtifactBetaScreenHandler
import hifumi.cresora.equipment.ArtifactEquipmentItem
import hifumi.cresora.equipment.ArtifactShopScreenHandler
import hifumi.cresora.equipment.ArtifactSpecialItem
import hifumi.cresora.equipment.ArtifactSpecialItemRegistry
import hifumi.cresora.equipment.ArtifactSpecialItemSupport
import hifumi.cresora.equipment.EquipmentAttributeService
import hifumi.cresora.equipment.EquipmentContentRegistry
import hifumi.cresora.equipment.EquipmentDefinitionRef
import hifumi.cresora.equipment.EquipmentGenerationService
import hifumi.cresora.equipment.EquipmentRarity
import hifumi.cresora.equipment.EquipmentStackSupport
import hifumi.cresora.masquerade.MasqueradeContentRegistry
import hifumi.cresora.masquerade.MasqueradeHooks
import hifumi.cresora.masquerade.MasqueradeLoadoutScreenHandler
import hifumi.cresora.masquerade.MasqueradeSupportScreenHandler
import hifumi.cresora.musicecho.MusicEchoContentRegistry
import hifumi.cresora.resonance.ResonanceContentRegistry
import hifumi.cresora.resonance.ResonanceResultScreenHandler
import hifumi.cresora.resonance.ResonanceScreenHandler
import hifumi.cresora.story.StoryChapterSelectionScreenHandler
import hifumi.cresora.story.StoryContentRegistry
import hifumi.cresora.story.StoryDialogueNetworking
import hifumi.cresora.story.StoryHooks
import hifumi.cresora.story.StoryStageSelectionScreenHandler
import hifumi.cresora.story.StoryTextRegistry
import hifumi.cresora.treasure.TreasureChestService
import hifumi.cresora.weapon.CresoraWeaponItem
import hifumi.cresora.weapon.HotbarOverrideHooks
import hifumi.cresora.weapon.HotbarOverrideService
import hifumi.cresora.weapon.SubSkillItem
import hifumi.cresora.weapon.WeaponAttributeService
import hifumi.cresora.weapon.WeaponContentRegistry
import hifumi.cresora.weapon.WeaponDefinitionRef
import hifumi.cresora.weapon.WeaponFragmentItem
import hifumi.cresora.weapon.WeaponRarity
import hifumi.cresora.weapon.WeaponRole
import hifumi.cresora.weapon.WeaponSkillMaterialScreenHandler
import hifumi.cresora.weapon.WeaponSkillService
import hifumi.cresora.weapon.WeaponStackSupport
import hifumi.cresora.weapon.WeaponUpgradeScreenHandler
import hifumi.cresora.leyline.LeyLineElement
import hifumi.cresora.leyline.LeyLineKeyItem
import hifumi.cresora.leyline.LeyLineOverflowBlock
import hifumi.cresora.leyline.LeyLineSelectionScreenHandler
import hifumi.cresora.leyline.LeyLineHooks
import com.mojang.serialization.Codec
import com.mojang.serialization.MapCodec
import com.mojang.serialization.codecs.RecordCodecBuilder
import dev.emi.trinkets.api.TrinketsApi
import hifumi.cresora.equipment.ArtifactSkillRegistry
import net.fabricmc.api.ModInitializer
import net.fabricmc.fabric.api.loot.v3.LootTableEvents
import net.fabricmc.loader.api.FabricLoader
import net.minecraft.block.AbstractBlock
import net.minecraft.block.Block
import net.minecraft.block.Blocks
import net.minecraft.entity.mob.HostileEntity
import net.minecraft.item.BlockItem
import net.minecraft.item.Item
import net.minecraft.item.ItemStack
import net.minecraft.loot.LootPool
import net.minecraft.loot.condition.RandomChanceLootCondition
import net.minecraft.loot.context.LootContext
import net.minecraft.loot.entry.ItemEntry
import net.minecraft.loot.function.LootFunction
import net.minecraft.loot.function.LootFunctionType
import net.minecraft.loot.function.SetCountLootFunction
import net.minecraft.loot.provider.number.ConstantLootNumberProvider
import net.minecraft.loot.provider.number.LootNumberProvider
import net.minecraft.loot.provider.number.LootNumberProviderTypes
import net.minecraft.loot.provider.number.UniformLootNumberProvider
import net.minecraft.registry.Registries
import net.minecraft.registry.Registry
import net.minecraft.registry.RegistryKey
import net.minecraft.registry.RegistryKeys
import net.minecraft.resource.featuretoggle.FeatureFlags
import net.minecraft.screen.ScreenHandlerType
import net.minecraft.util.Identifier
import org.slf4j.LoggerFactory
import java.util.Optional
import kotlin.math.max

val version: String
	get() = FabricLoader.getInstance()
		.getModContainer(CreSoraUtilities.MOD_ID)
		.map { it.metadata.version.friendlyString }
		.orElse("unknown")

object CreSoraUtilities : ModInitializer {
	const val MOD_ID = "cresora-utilities"

	private val logger = LoggerFactory.getLogger(MOD_ID)
	private val VERSION_VERIFIER_ID: Identifier = Identifier.of("${MOD_ID}$version", "versionverifier")
	val SUB_SKILL_DUMMY_ID: Identifier = Identifier.of(MOD_ID, "sub_skill_dummy")
	private val MOON_BRICK_ID: Identifier = Identifier.of(MOD_ID, "moon_brick")
	private val MOON_ALTAR_ID: Identifier = Identifier.of(MOD_ID, "moon_altar")
	private val RESONANT_LOCATOR_ID: Identifier = Identifier.of(MOD_ID, "resonant_locator")
	private val RESONANT_CACHE_ID: Identifier = Identifier.of(MOD_ID, "resonant_cache")
 
	private val EQUIPMENT_ITEMS: MutableMap<String, ArtifactEquipmentItem> = linkedMapOf()
	private val WEAPON_ITEMS: MutableMap<String, CresoraWeaponItem> = linkedMapOf()
	private val WEAPON_FRAGMENT_ITEMS: MutableMap<String, WeaponFragmentItem> = linkedMapOf()
	private val WEAPON_RARITY_FRAGMENT_ITEMS: MutableMap<WeaponRarity, Item> = linkedMapOf()
	private val ARTIFACT_SPECIAL_ITEMS: MutableMap<String, ArtifactSpecialItem> = linkedMapOf()
	private val ROLE_PROOF_ITEMS: MutableMap<WeaponRole, Item> = linkedMapOf()
	private val ROLE_INSIGHT_ITEMS: MutableMap<WeaponRole, Item> = linkedMapOf()
	val VERIFY: Item = Item(itemSettings(VERSION_VERIFIER_ID))
	val SUB_SKILL_DUMMY: Item = SubSkillItem(itemSettings(SUB_SKILL_DUMMY_ID).maxCount(1))
	val MOON_BRICK_ITEM: Item = Item(itemSettings(MOON_BRICK_ID))
	val MOON_ALTAR_BLOCK: Block = Block(blockSettings(MOON_ALTAR_ID, Blocks.CHISELED_STONE_BRICKS))
	val MOON_ALTAR_BLOCK_ITEM: Item = BlockItem(MOON_ALTAR_BLOCK, itemSettings(MOON_ALTAR_ID))
	val RESONANT_LOCATOR_ITEM: Item = Item(itemSettings(RESONANT_LOCATOR_ID).maxCount(16))
	val RESONANT_CACHE_BLOCK: Block = Block(blockSettings(RESONANT_CACHE_ID, Blocks.CHEST))
	val RESONANT_CACHE_BLOCK_ITEM: Item = BlockItem(RESONANT_CACHE_BLOCK, itemSettings(RESONANT_CACHE_ID))
	private val LEY_LINE_OVERFLOW_ID = Identifier.of(MOD_ID, "ley_line_overflow")
	val LEY_LINE_OVERFLOW_BLOCK: Block = LeyLineOverflowBlock(blockSettings(LEY_LINE_OVERFLOW_ID, Blocks.STONE))
	val LEY_LINE_OVERFLOW_BLOCK_ITEM: Item = BlockItem(LEY_LINE_OVERFLOW_BLOCK, itemSettings(LEY_LINE_OVERFLOW_ID))
	val LEY_LINE_KEYS: Map<LeyLineElement, LeyLineKeyItem> = LeyLineElement.entries.associateWith { element ->
		LeyLineKeyItem(element, itemSettings(Identifier.of(MOD_ID, "${element.id}_key")))
	}

	lateinit var UPGRADE_SCREEN_HANDLER: ScreenHandlerType<UpgradeScreenHandler>
	lateinit var WEAPON_UPGRADE_SCREEN_HANDLER: ScreenHandlerType<WeaponUpgradeScreenHandler>
	lateinit var WEAPON_SKILL_MATERIAL_SCREEN_HANDLER: ScreenHandlerType<WeaponSkillMaterialScreenHandler>
	lateinit var CRESORA_MENU_SCREEN_HANDLER: ScreenHandlerType<CresoraMenuScreenHandler>
	lateinit var DOMAIN_SELECTION_SCREEN_HANDLER: ScreenHandlerType<DomainSelectionScreenHandler>
	lateinit var DOMAIN_REWARD_SCREEN_HANDLER: ScreenHandlerType<DomainRewardScreenHandler>
	lateinit var STORY_CHAPTER_SELECTION_SCREEN_HANDLER: ScreenHandlerType<StoryChapterSelectionScreenHandler>
	lateinit var STORY_STAGE_SELECTION_SCREEN_HANDLER: ScreenHandlerType<StoryStageSelectionScreenHandler>
	lateinit var MASQUERADE_LOADOUT_SCREEN_HANDLER: ScreenHandlerType<MasqueradeLoadoutScreenHandler>
	lateinit var MASQUERADE_SUPPORT_SCREEN_HANDLER: ScreenHandlerType<MasqueradeSupportScreenHandler>
	lateinit var ARTIFACT_SHOP_SCREEN_HANDLER: ScreenHandlerType<ArtifactShopScreenHandler>
	lateinit var ARTIFACT_ALPHA_SCREEN_HANDLER: ScreenHandlerType<ArtifactAlphaScreenHandler>
	lateinit var ARTIFACT_BETA_SCREEN_HANDLER: ScreenHandlerType<ArtifactBetaScreenHandler>
	lateinit var RESONANCE_SCREEN_HANDLER: ScreenHandlerType<ResonanceScreenHandler>
	lateinit var RESONANCE_RESULT_SCREEN_HANDLER: ScreenHandlerType<ResonanceResultScreenHandler>
	lateinit var LEY_LINE_SELECTION_SCREEN_HANDLER: ScreenHandlerType<LeyLineSelectionScreenHandler>
	lateinit var SET_LEVEL_LOOT_FUNCTION: LootFunctionType<SetLevelLootFunction>

	override fun onInitialize() {
		ModDataComponents.initialize()

		Registry.register(Registries.BLOCK, MOON_ALTAR_ID, MOON_ALTAR_BLOCK)
		Registry.register(Registries.BLOCK, RESONANT_CACHE_ID, RESONANT_CACHE_BLOCK)
		Registry.register(Registries.BLOCK, LEY_LINE_OVERFLOW_ID, LEY_LINE_OVERFLOW_BLOCK)
		Registry.register(Registries.ITEM, VERSION_VERIFIER_ID, VERIFY)
		Registry.register(Registries.ITEM, SUB_SKILL_DUMMY_ID, SUB_SKILL_DUMMY)
		Registry.register(Registries.ITEM, MOON_BRICK_ID, MOON_BRICK_ITEM)
		Registry.register(Registries.ITEM, MOON_ALTAR_ID, MOON_ALTAR_BLOCK_ITEM)
		Registry.register(Registries.ITEM, RESONANT_LOCATOR_ID, RESONANT_LOCATOR_ITEM)
		Registry.register(Registries.ITEM, RESONANT_CACHE_ID, RESONANT_CACHE_BLOCK_ITEM)
		Registry.register(Registries.ITEM, LEY_LINE_OVERFLOW_ID, LEY_LINE_OVERFLOW_BLOCK_ITEM)
		for ((element, keyItem) in LEY_LINE_KEYS) {
			Registry.register(Registries.ITEM, Identifier.of(MOD_ID, "${element.id}_key"), keyItem)
		}

		EquipmentContentRegistry.init()
		WeaponContentRegistry.init()
		ArtifactSkillRegistry.init()
		ArtifactSpecialItemRegistry.init()
		registerEquipmentItems()
		registerWeaponRarityFragmentItems()
		registerWeaponItems()
		registerArtifactSpecialItems()
		registerRoleMaterials()
		ShopContentRegistry.init()
		MobCombatProfileRegistry.init()
		DomainRewardProfileRegistry.init()
		DomainContentRegistry.init()
		MasqueradeContentRegistry.init()
		StoryTextRegistry.init()
		StoryContentRegistry.init()
		ResonanceContentRegistry.init()
		MusicEchoContentRegistry.init()
		StoryDialogueNetworking.init()
		UPGRADE_SCREEN_HANDLER = Registry.register(
			Registries.SCREEN_HANDLER,
			Identifier.of(MOD_ID, "upgrade"),
			ScreenHandlerType({ syncId, playerInventory -> UpgradeScreenHandler(syncId, playerInventory) }, FeatureFlags.VANILLA_FEATURES)
		)
		WEAPON_UPGRADE_SCREEN_HANDLER = Registry.register(
			Registries.SCREEN_HANDLER,
			Identifier.of(MOD_ID, "weapon_upgrade"),
			ScreenHandlerType(::WeaponUpgradeScreenHandler, FeatureFlags.VANILLA_FEATURES)
		)
		WEAPON_SKILL_MATERIAL_SCREEN_HANDLER = Registry.register(
			Registries.SCREEN_HANDLER,
			Identifier.of(MOD_ID, "weapon_skill_material"),
			ScreenHandlerType(::WeaponSkillMaterialScreenHandler, FeatureFlags.VANILLA_FEATURES)
		)
		CRESORA_MENU_SCREEN_HANDLER = Registry.register(
			Registries.SCREEN_HANDLER,
			Identifier.of(MOD_ID, "cresora_menu"),
			ScreenHandlerType(::CresoraMenuScreenHandler, FeatureFlags.VANILLA_FEATURES)
		)
		DOMAIN_SELECTION_SCREEN_HANDLER = Registry.register(
			Registries.SCREEN_HANDLER,
			Identifier.of(MOD_ID, "domain_selection"),
			ScreenHandlerType(::DomainSelectionScreenHandler, FeatureFlags.VANILLA_FEATURES)
		)
		DOMAIN_REWARD_SCREEN_HANDLER = Registry.register(
			Registries.SCREEN_HANDLER,
			Identifier.of(MOD_ID, "domain_reward"),
			ScreenHandlerType(::DomainRewardScreenHandler, FeatureFlags.VANILLA_FEATURES)
		)
		STORY_CHAPTER_SELECTION_SCREEN_HANDLER = Registry.register(
			Registries.SCREEN_HANDLER,
			Identifier.of(MOD_ID, "story_chapter_selection"),
			ScreenHandlerType(::StoryChapterSelectionScreenHandler, FeatureFlags.VANILLA_FEATURES)
		)
		STORY_STAGE_SELECTION_SCREEN_HANDLER = Registry.register(
			Registries.SCREEN_HANDLER,
			Identifier.of(MOD_ID, "story_stage_selection"),
			ScreenHandlerType(::StoryStageSelectionScreenHandler, FeatureFlags.VANILLA_FEATURES)
		)
		MASQUERADE_LOADOUT_SCREEN_HANDLER = Registry.register(
			Registries.SCREEN_HANDLER,
			Identifier.of(MOD_ID, "masquerade_loadout"),
			ScreenHandlerType(::MasqueradeLoadoutScreenHandler, FeatureFlags.VANILLA_FEATURES)
		)
		MASQUERADE_SUPPORT_SCREEN_HANDLER = Registry.register(
			Registries.SCREEN_HANDLER,
			Identifier.of(MOD_ID, "masquerade_support"),
			ScreenHandlerType(::MasqueradeSupportScreenHandler, FeatureFlags.VANILLA_FEATURES)
		)
		ARTIFACT_SHOP_SCREEN_HANDLER = Registry.register(
			Registries.SCREEN_HANDLER,
			Identifier.of(MOD_ID, "artifact_shop"),
			ScreenHandlerType(::ArtifactShopScreenHandler, FeatureFlags.VANILLA_FEATURES)
		)
		ARTIFACT_ALPHA_SCREEN_HANDLER = Registry.register(
			Registries.SCREEN_HANDLER,
			Identifier.of(MOD_ID, "artifact_alpha"),
			ScreenHandlerType(::ArtifactAlphaScreenHandler, FeatureFlags.VANILLA_FEATURES)
		)
		ARTIFACT_BETA_SCREEN_HANDLER = Registry.register(
			Registries.SCREEN_HANDLER,
			Identifier.of(MOD_ID, "artifact_beta"),
			ScreenHandlerType(::ArtifactBetaScreenHandler, FeatureFlags.VANILLA_FEATURES)
		)
		RESONANCE_SCREEN_HANDLER = Registry.register(
			Registries.SCREEN_HANDLER,
			Identifier.of(MOD_ID, "resonance"),
			ScreenHandlerType(::ResonanceScreenHandler, FeatureFlags.VANILLA_FEATURES)
		)
		RESONANCE_RESULT_SCREEN_HANDLER = Registry.register(
			Registries.SCREEN_HANDLER,
			Identifier.of(MOD_ID, "resonance_result"),
			ScreenHandlerType(::ResonanceResultScreenHandler, FeatureFlags.VANILLA_FEATURES)
		)
		LEY_LINE_SELECTION_SCREEN_HANDLER = Registry.register(
			Registries.SCREEN_HANDLER,
			Identifier.of(MOD_ID, "ley_line_selection"),
			ScreenHandlerType(::LeyLineSelectionScreenHandler, FeatureFlags.VANILLA_FEATURES)
		)
		SET_LEVEL_LOOT_FUNCTION = Registry.register(
			Registries.LOOT_FUNCTION_TYPE,
			Identifier.of(MOD_ID, "set_level"),
			LootFunctionType(SetLevelLootFunction.CODEC)
		)

		Join.init()
		Commands.init()
		AdventureRankHooks.init()
		MoonPhaseHooks.init()
		BloodMoonHooks.init()
		MoonAltarService.init()
		DomainHooks.init()
		StoryHooks.init()
		MasqueradeHooks.init()
		CresoraDebuffHooks.init()
		HotbarOverrideHooks.init()
		PlayerLifecycleHooks.init()
		TreasureChestService.init()
		EquipmentAttributeService.init()
		WeaponAttributeService.init()
		WeaponSkillService.init()
		NaturalRegenService.init()
		HotbarOverrideService.init()
		LeyLineHooks.init()
		modifyLootTables()

		logger.info("CreSora Utilities initialized!")
	}

	fun equipmentItem(definitionId: String): ArtifactEquipmentItem {
		return EQUIPMENT_ITEMS[definitionId] ?: error("Unknown registered equipment item: $definitionId")
	}

	fun weaponItem(definitionId: String): CresoraWeaponItem {
		return WEAPON_ITEMS[definitionId] ?: error("Unknown registered weapon item: $definitionId")
	}

	fun artifactSpecialItem(definitionId: String): ArtifactSpecialItem {
		return ARTIFACT_SPECIAL_ITEMS[definitionId] ?: error("Unknown registered artifact special item: $definitionId")
	}

	fun getRoleProofItem(role: WeaponRole): Item {
		return ROLE_PROOF_ITEMS[role] ?: error("Unregistered proof item for role: $role")
	}

	fun getRoleInsightItem(role: WeaponRole): Item {
		return ROLE_INSIGHT_ITEMS[role] ?: error("Unregistered insight item for role: $role")
	}

	private fun itemSettings(id: Identifier): Item.Settings {
		return Item.Settings().registryKey(RegistryKey.of(RegistryKeys.ITEM, id))
	}

	private fun blockSettings(id: Identifier, baseBlock: Block): AbstractBlock.Settings {
		return AbstractBlock.Settings.copy(baseBlock).registryKey(RegistryKey.of(RegistryKeys.BLOCK, id))
	}

	private fun registerEquipmentItems() {
		for (definition in EquipmentContentRegistry.equipmentDefinitions()) {
			val itemId = Identifier.of(MOD_ID, definition.id)
			val item = ArtifactEquipmentItem(EquipmentDefinitionRef(definition.id), itemSettings(itemId))
			EQUIPMENT_ITEMS[definition.id] = Registry.register(Registries.ITEM, itemId, item)
			EquipmentStackSupport.registerEquipmentItem(item, EquipmentDefinitionRef(definition.id))
			TrinketsApi.registerTrinket(item, item)
		}
	}

	private fun registerWeaponItems() {
		for (definition in WeaponContentRegistry.weaponDefinitions()) {
			val weaponId = Identifier.of(MOD_ID, definition.id)
			val weaponItem = CresoraWeaponItem(WeaponDefinitionRef(definition.id), itemSettings(weaponId).maxCount(1))
			WEAPON_ITEMS[definition.id] = Registry.register(Registries.ITEM, weaponId, weaponItem)
			WeaponStackSupport.registerWeaponItem(weaponItem, WeaponDefinitionRef(definition.id))

			val fragmentId = Identifier.of(MOD_ID, definition.craft.fragmentItemId)
			val fragmentItem = WeaponFragmentItem(WeaponDefinitionRef(definition.id), itemSettings(fragmentId))
			WEAPON_FRAGMENT_ITEMS[definition.id] = Registry.register(Registries.ITEM, fragmentId, fragmentItem)
			WeaponStackSupport.registerFragmentItem(fragmentItem, WeaponDefinitionRef(definition.id))
		}
	}

	private fun registerWeaponRarityFragmentItems() {
		for (rarity in WeaponRarity.entries) {
			val itemId = Identifier.of(MOD_ID, rarity.fragmentItemId())
			val item = Item(itemSettings(itemId))
			WEAPON_RARITY_FRAGMENT_ITEMS[rarity] = Registry.register(Registries.ITEM, itemId, item)
			WeaponStackSupport.registerRarityFragmentItem(item, rarity)
		}
	}

	private fun registerArtifactSpecialItems() {
		for (definition in ArtifactSpecialItemRegistry.definitions()) {
			val itemId = Identifier.of(MOD_ID, definition.id)
			val item = ArtifactSpecialItem(definition.id, itemSettings(itemId))
			ARTIFACT_SPECIAL_ITEMS[definition.id] = Registry.register(Registries.ITEM, itemId, item)
			ArtifactSpecialItemSupport.registerItem(item, definition.id)
		}
	}

	private fun registerRoleMaterials() {
		for (role in WeaponRole.entries) {
			val proofId = Identifier.of(MOD_ID, "${role.id}_proof")
			val proofItem = Item(itemSettings(proofId))
			ROLE_PROOF_ITEMS[role] = Registry.register(Registries.ITEM, proofId, proofItem)

			val insightId = Identifier.of(MOD_ID, "${role.id}_insight")
			val insightItem = Item(itemSettings(insightId))
			ROLE_INSIGHT_ITEMS[role] = Registry.register(Registries.ITEM, insightId, insightItem)
		}
	}

	private fun modifyLootTables() {
		LootTableEvents.MODIFY.register { key, builder, source, registries ->
			if (source.isBuiltin) {
				EquipmentContentRegistry.applyMobLootRules(key, builder, registries)
			}
		}
	}
}

class SetLevelLootFunction(
	val levelProvider: LootNumberProvider,
	val forcedRarity: EquipmentRarity? = null,
	val dropProfileId: String? = null
) : LootFunction {

	override fun getType(): LootFunctionType<*> = CreSoraUtilities.SET_LEVEL_LOOT_FUNCTION

	override fun apply(stack: ItemStack, context: LootContext): ItemStack {
		val player = when {
			context.hasParameter(net.minecraft.loot.context.LootContextParameters.LAST_DAMAGE_PLAYER) ->
				context.get(net.minecraft.loot.context.LootContextParameters.LAST_DAMAGE_PLAYER) as? net.minecraft.server.network.ServerPlayerEntity
			context.hasParameter(net.minecraft.loot.context.LootContextParameters.ATTACKING_ENTITY) ->
				context.get(net.minecraft.loot.context.LootContextParameters.ATTACKING_ENTITY) as? net.minecraft.server.network.ServerPlayerEntity
			else -> null
		}
		val hostileLevel = when {
			context.hasParameter(net.minecraft.loot.context.LootContextParameters.THIS_ENTITY) ->
				(context.get(net.minecraft.loot.context.LootContextParameters.THIS_ENTITY) as? HostileEntity)?.let(AdventureRankService::mobLevel)
			else -> null
		} ?: AdventureRankProgression.MIN_RANK

		val adventureRank = player?.let { AdventureRankService.getRank(it) } ?: AdventureRankProgression.MIN_RANK
		val effectiveLootRank = max(adventureRank, hostileLevel)
		val lootBonus = AdventureRankProfile.lootBonus(effectiveLootRank)
		val randomLevel = (levelProvider.nextInt(context) + lootBonus.levelBonus).coerceAtLeast(0)
		stack.set(ModDataComponents.LEVEL, randomLevel)

		if (EquipmentStackSupport.isEquipment(stack)) {
			val definition = EquipmentStackSupport.getDefinition(stack) ?: return stack
			val baseRarity = forcedRarity ?: EquipmentStackSupport.rarityForLevel(randomLevel)
			val scaledRarity = AdventureRankProfile.upgradeRarity(baseRarity, effectiveLootRank, context.random)
			val dropProfile = dropProfileId?.let(EquipmentContentRegistry::requireDropProfile)
			EquipmentStackSupport.syncEquipmentData(
				stack,
				EquipmentGenerationService.createEquipment(
					random = context.random,
					definition = definition,
					startingLevel = randomLevel,
					forcedRarity = scaledRarity,
					dropProfile = dropProfile
				)
			)
		}

		if (stack.isIn(ModItemTags.ADVENTURE_RANK_UPGRADE_MATERIALS)) {
			val extraCount = AdventureRankProfile.extraUpgradeMaterialCount(effectiveLootRank, context.random)
			if (extraCount > 0) {
				stack.count += extraCount
			}
		}
		return stack
	}

	companion object {
		fun builder(
			levelProvider: LootNumberProvider,
			forcedRarity: EquipmentRarity? = null,
			dropProfileId: String? = null
		): LootFunction.Builder {
			return LootFunction.Builder {
				SetLevelLootFunction(levelProvider, forcedRarity, dropProfileId)
			}
		}

		val CODEC: MapCodec<SetLevelLootFunction> = RecordCodecBuilder.mapCodec { instance ->
			instance.group(
				LootNumberProviderTypes.CODEC.fieldOf("level_provider").forGetter(SetLevelLootFunction::levelProvider),
				EquipmentRarity.CODEC.optionalFieldOf("forced_rarity")
					.forGetter { Optional.ofNullable(it.forcedRarity) },
				Codec.STRING.optionalFieldOf("drop_profile")
					.forGetter { Optional.ofNullable(it.dropProfileId) }
			).apply(instance) { levelProvider: LootNumberProvider, forcedRarity: Optional<EquipmentRarity>, dropProfileId: Optional<String> ->
				SetLevelLootFunction(levelProvider, forcedRarity.orElse(null), dropProfileId.orElse(null))
			}
		}
	}
}
