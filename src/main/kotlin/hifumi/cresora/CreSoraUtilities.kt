package hifumi.cresora

import com.mojang.serialization.MapCodec
import com.mojang.serialization.codecs.RecordCodecBuilder
import dev.emi.trinkets.api.TrinketsApi
import net.fabricmc.api.ModInitializer
import net.fabricmc.fabric.api.loot.v3.LootTableEvents
import net.fabricmc.loader.api.FabricLoader
import net.minecraft.entity.EntityType
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

val version: String
	get() = FabricLoader.getInstance()
		.getModContainer(CreSoraUtilities.MOD_ID)
		.map { it.metadata.version.friendlyString }
		.orElse("unknown")

object CreSoraUtilities : ModInitializer {
	const val MOD_ID = "cresora-utilities"

	private val logger = LoggerFactory.getLogger(MOD_ID)
	private val HINAGATASTUE_ID: Identifier = Identifier.of(MOD_ID, "hinagatastue")
	private val HINAGATA_HAT_ID: Identifier = Identifier.of(MOD_ID, "hinagata_hat")
	private val HINAGATA_GLASSES_ID: Identifier = Identifier.of(MOD_ID, "hinagata_glasses")
	private val HINAGATA_ARMOR_ID: Identifier = Identifier.of(MOD_ID, "hinagata_armor")
	private val HINAGATA_BOOTS_ID: Identifier = Identifier.of(MOD_ID, "hinagata_boots")
	private val TUESHOKAKU_ID: Identifier = Identifier.of(MOD_ID, "tueshokaku")
	private val VERSION_VERIFIER_ID: Identifier = Identifier.of("${MOD_ID}$version", "versionverifier")

	val STRENGTH_PENDANT: ArtifactEquipmentItem = Hinagatas_Tue(itemSettings(HINAGATASTUE_ID))
	val HINAGATA_HAT: ArtifactEquipmentItem = ArtifactEquipmentItem(EquipmentDefinitions.HINAGATA_HAT, itemSettings(HINAGATA_HAT_ID))
	val HINAGATA_GLASSES: ArtifactEquipmentItem = ArtifactEquipmentItem(EquipmentDefinitions.HINAGATA_GLASSES, itemSettings(HINAGATA_GLASSES_ID))
	val HINAGATA_ARMOR: ArtifactEquipmentItem = ArtifactEquipmentItem(EquipmentDefinitions.HINAGATA_ARMOR, itemSettings(HINAGATA_ARMOR_ID))
	val HINAGATA_BOOTS: ArtifactEquipmentItem = ArtifactEquipmentItem(EquipmentDefinitions.HINAGATA_BOOTS, itemSettings(HINAGATA_BOOTS_ID))
	val TUESHOKAKU: Item = tueshokaku(itemSettings(TUESHOKAKU_ID))
	val VERIFY: Item = Item(itemSettings(VERSION_VERIFIER_ID))

	lateinit var UPGRADE_SCREEN_HANDLER: ScreenHandlerType<UpgradeScreenHandler>
	lateinit var SET_LEVEL_LOOT_FUNCTION: LootFunctionType<SetLevelLootFunction>

	override fun onInitialize() {
		ModDataComponents.initialize()
		UPGRADE_SCREEN_HANDLER = Registry.register(
			Registries.SCREEN_HANDLER,
			Identifier.of(MOD_ID, "upgrade"),
			ScreenHandlerType(::UpgradeScreenHandler, FeatureFlags.VANILLA_FEATURES)
		)
		SET_LEVEL_LOOT_FUNCTION = Registry.register(
			Registries.LOOT_FUNCTION_TYPE,
			Identifier.of(MOD_ID, "set_level"),
			LootFunctionType(SetLevelLootFunction.CODEC)
		)

		Join.init()
		Commands.init()
		AdventureRankHooks.init()
		EquipmentAttributeService.init()
		modifyLootTables()

		Registry.register(Registries.ITEM, HINAGATASTUE_ID, STRENGTH_PENDANT)
		Registry.register(Registries.ITEM, HINAGATA_HAT_ID, HINAGATA_HAT)
		Registry.register(Registries.ITEM, HINAGATA_GLASSES_ID, HINAGATA_GLASSES)
		Registry.register(Registries.ITEM, HINAGATA_ARMOR_ID, HINAGATA_ARMOR)
		Registry.register(Registries.ITEM, HINAGATA_BOOTS_ID, HINAGATA_BOOTS)
		Registry.register(Registries.ITEM, TUESHOKAKU_ID, TUESHOKAKU)
		Registry.register(Registries.ITEM, VERSION_VERIFIER_ID, VERIFY)

		TrinketsApi.registerTrinket(STRENGTH_PENDANT, STRENGTH_PENDANT)
		TrinketsApi.registerTrinket(HINAGATA_HAT, HINAGATA_HAT)
		TrinketsApi.registerTrinket(HINAGATA_GLASSES, HINAGATA_GLASSES)
		TrinketsApi.registerTrinket(HINAGATA_ARMOR, HINAGATA_ARMOR)
		TrinketsApi.registerTrinket(HINAGATA_BOOTS, HINAGATA_BOOTS)

		logger.info("CreSora Utilities initialized!")
	}

	private fun itemSettings(id: Identifier): Item.Settings {
		return Item.Settings().registryKey(RegistryKey.of(RegistryKeys.ITEM, id))
	}

	private fun modifyLootTables() {
		LootTableEvents.MODIFY.register { key, tableBuilder, source, _ ->
			if (!source.isBuiltin) {
				return@register
			}

			when (key) {
				EntityType.ZOMBIE.getLootTableKey().orElse(null) -> {
					tableBuilder.pool(
						createArtifactLootPool(
							chance = 0.2f,
							levelProvider = UniformLootNumberProvider.create(0.0f, 2.0f),
							rarity = EquipmentRarity.THREE_STAR,
							dropProfile = EquipmentDropProfile.ZOMBIE_SURVIVOR
						)
					)
					tableBuilder.pool(
						createUpgradeToolLootPool(
							chance = 0.1f,
							levelProvider = UniformLootNumberProvider.create(1.0f, 2.0f)
						)
					)
				}

				EntityType.SKELETON.getLootTableKey().orElse(null) -> {
					tableBuilder.pool(
						createArtifactLootPool(
							chance = 0.2f,
							levelProvider = UniformLootNumberProvider.create(2.0f, 5.0f),
							rarity = EquipmentRarity.FOUR_STAR,
							dropProfile = EquipmentDropProfile.SKELETON_ASSAULT
						)
					)
					tableBuilder.pool(
						createUpgradeToolLootPool(
							chance = 0.12f,
							levelProvider = UniformLootNumberProvider.create(2.0f, 3.0f)
						)
					)
				}

				EntityType.WARDEN.getLootTableKey().orElse(null) -> {
					tableBuilder.pool(
						createArtifactLootPool(
							chance = 1.0f,
							levelProvider = UniformLootNumberProvider.create(8.0f, 12.0f),
							rarity = EquipmentRarity.FIVE_STAR,
							dropProfile = EquipmentDropProfile.WARDEN_RELIC
						)
					)
				}
			}
		}
	}

	private fun createArtifactLootPool(
		chance: Float,
		levelProvider: LootNumberProvider,
		rarity: EquipmentRarity,
		dropProfile: EquipmentDropProfile
	): LootPool {
		return LootPool.builder()
			.rolls(ConstantLootNumberProvider.create(1.0f))
			.conditionally(RandomChanceLootCondition.builder(chance))
			.with(ItemEntry.builder(STRENGTH_PENDANT))
			.with(ItemEntry.builder(HINAGATA_HAT))
			.with(ItemEntry.builder(HINAGATA_GLASSES))
			.with(ItemEntry.builder(HINAGATA_ARMOR))
			.with(ItemEntry.builder(HINAGATA_BOOTS))
			.apply(SetCountLootFunction.builder(UniformLootNumberProvider.create(1.0f, 1.0f)))
			.apply(SetLevelLootFunction(levelProvider, rarity, dropProfile))
			.build()
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
	val dropProfile: EquipmentDropProfile? = null
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

		val adventureRank = player?.let { AdventureRankService.getRank(it) } ?: AdventureRankProgression.MIN_RANK
		val lootBonus = AdventureRankProfile.lootBonus(adventureRank)
		val randomLevel = (levelProvider.nextInt(context) + lootBonus.levelBonus).coerceAtLeast(0)
		stack.set(ModDataComponents.LEVEL, randomLevel)

		if (EquipmentStackSupport.isEquipment(stack)) {
			val definition = EquipmentStackSupport.getDefinition(stack) ?: return stack
			val baseRarity = forcedRarity ?: EquipmentStackSupport.rarityForLevel(randomLevel)
			val scaledRarity = AdventureRankProfile.upgradeRarity(baseRarity, adventureRank, context.random)
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
			val extraCount = AdventureRankProfile.extraUpgradeMaterialCount(adventureRank, context.random)
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
				EquipmentDropProfile.CODEC.optionalFieldOf("drop_profile")
					.forGetter { Optional.ofNullable(it.dropProfile) }
			).apply(instance) { levelProvider, forcedRarity, dropProfile ->
				SetLevelLootFunction(levelProvider, forcedRarity.orElse(null), dropProfile.orElse(null))
			}
		}
	}
}
