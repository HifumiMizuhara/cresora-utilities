package hifumi.cresora

import com.mojang.serialization.Codec
import com.mojang.serialization.MapCodec
import com.mojang.serialization.codecs.RecordCodecBuilder
import dev.emi.trinkets.api.TrinketsApi
import net.fabricmc.api.ModInitializer
import net.fabricmc.fabric.api.loot.v3.LootTableEvents
import net.fabricmc.loader.api.FabricLoader
import net.minecraft.entity.mob.HostileEntity
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
	private val TUESHOKAKU_ID: Identifier = Identifier.of(MOD_ID, "tueshokaku")
	private val VERSION_VERIFIER_ID: Identifier = Identifier.of("${MOD_ID}$version", "versionverifier")

	private val EQUIPMENT_ITEMS: MutableMap<String, ArtifactEquipmentItem> = linkedMapOf()
	private val WEAPON_ITEMS: MutableMap<String, CresoraWeaponItem> = linkedMapOf()
	private val WEAPON_FRAGMENT_ITEMS: MutableMap<String, WeaponFragmentItem> = linkedMapOf()
	private val ARTIFACT_SPECIAL_ITEMS: MutableMap<String, ArtifactSpecialItem> = linkedMapOf()
	val TUESHOKAKU: Item = tueshokaku(itemSettings(TUESHOKAKU_ID))
	val VERIFY: Item = Item(itemSettings(VERSION_VERIFIER_ID))

	lateinit var UPGRADE_SCREEN_HANDLER: ScreenHandlerType<UpgradeScreenHandler>
	lateinit var WEAPON_UPGRADE_SCREEN_HANDLER: ScreenHandlerType<WeaponUpgradeScreenHandler>
	lateinit var DOMAIN_SELECTION_SCREEN_HANDLER: ScreenHandlerType<DomainSelectionScreenHandler>
	lateinit var DOMAIN_REWARD_SCREEN_HANDLER: ScreenHandlerType<DomainRewardScreenHandler>
	lateinit var ARTIFACT_SHOP_SCREEN_HANDLER: ScreenHandlerType<ArtifactShopScreenHandler>
	lateinit var ARTIFACT_ALPHA_SCREEN_HANDLER: ScreenHandlerType<ArtifactAlphaScreenHandler>
	lateinit var ARTIFACT_BETA_SCREEN_HANDLER: ScreenHandlerType<ArtifactBetaScreenHandler>
	lateinit var SET_LEVEL_LOOT_FUNCTION: LootFunctionType<SetLevelLootFunction>

	override fun onInitialize() {
		ModDataComponents.initialize()
		EquipmentContentRegistry.init()
		WeaponContentRegistry.init()
		ArtifactSpecialItemRegistry.init()
		DomainRewardProfileRegistry.init()
		DomainContentRegistry.init()
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
		SET_LEVEL_LOOT_FUNCTION = Registry.register(
			Registries.LOOT_FUNCTION_TYPE,
			Identifier.of(MOD_ID, "set_level"),
			LootFunctionType(SetLevelLootFunction.CODEC)
		)

		Join.init()
		Commands.init()
		AdventureRankHooks.init()
		DomainHooks.init()
		EquipmentAttributeService.init()
		WeaponAttributeService.init()
		WeaponSkillService.init()
		registerEquipmentItems()
		registerWeaponItems()
		registerArtifactSpecialItems()
		modifyLootTables()

		Registry.register(Registries.ITEM, TUESHOKAKU_ID, TUESHOKAKU)
		Registry.register(Registries.ITEM, VERSION_VERIFIER_ID, VERIFY)

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

	private fun itemSettings(id: Identifier): Item.Settings {
		return Item.Settings().registryKey(RegistryKey.of(RegistryKeys.ITEM, id))
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

	private fun registerArtifactSpecialItems() {
		for (definition in ArtifactSpecialItemRegistry.definitions()) {
			val itemId = Identifier.of(MOD_ID, definition.id)
			val item = ArtifactSpecialItem(definition.id, itemSettings(itemId))
			ARTIFACT_SPECIAL_ITEMS[definition.id] = Registry.register(Registries.ITEM, itemId, item)
			ArtifactSpecialItemSupport.registerItem(item, definition.id)
		}
	}

	private fun modifyLootTables() {
		LootTableEvents.MODIFY.register { key, tableBuilder, source, _ ->
			if (!source.isBuiltin) {
				return@register
			}

			for (lootDefinition in EquipmentContentRegistry.mobLootRules()) {
				val entityType = Registries.ENTITY_TYPE.get(Identifier.of(lootDefinition.entityTypeId))
				if (entityType.getLootTableKey().orElse(null) != key) {
					continue
				}
				lootDefinition.artifactLoot?.let { artifactLoot ->
					createArtifactLootPool(artifactLoot)?.let(tableBuilder::pool)
				}
				lootDefinition.upgradeMaterialLoot?.let { upgradeToolLoot ->
					tableBuilder.pool(
						createUpgradeToolLootPool(
							chance = upgradeToolLoot.chance,
							levelProvider = UniformLootNumberProvider.create(
								upgradeToolLoot.levelMin.toFloat(),
								upgradeToolLoot.levelMax.toFloat()
							)
						)
					)
				}
			}
		}
	}

	private fun createArtifactLootPool(
		rule: EquipmentArtifactLootRule
	): LootPool? {
		val equipmentItems = if (rule.equipmentIds.isEmpty()) {
			EquipmentStackSupport.allEquipmentItems()
		} else {
			rule.equipmentIds.mapNotNull(EquipmentStackSupport::itemForDefinitionId)
		}
		if (equipmentItems.isEmpty()) {
			return null
		}

		val builder = LootPool.builder()
			.rolls(ConstantLootNumberProvider.create(1.0f))
			.conditionally(RandomChanceLootCondition.builder(rule.chance))
			.apply(SetCountLootFunction.builder(UniformLootNumberProvider.create(1.0f, 1.0f)))
			.apply(
				SetLevelLootFunction(
					levelProvider = UniformLootNumberProvider.create(rule.levelMin.toFloat(), rule.levelMax.toFloat()),
					forcedRarity = rule.forcedRarity,
					dropProfileId = rule.dropProfileId
				)
			)
		for (item in equipmentItems) {
			builder.with(ItemEntry.builder(item))
		}
		return builder.build()
	}

	private fun createUpgradeToolLootPool(
		chance: Float,
		levelProvider: LootNumberProvider
	): LootPool {
		return LootPool.builder()
			.rolls(ConstantLootNumberProvider.create(1.0f))
			.conditionally(RandomChanceLootCondition.builder(chance))
			.with(ItemEntry.builder(TUESHOKAKU))
			.apply(SetCountLootFunction.builder(UniformLootNumberProvider.create(1.0f, 1.0f)))
			.apply(SetLevelLootFunction(levelProvider))
			.build()
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
