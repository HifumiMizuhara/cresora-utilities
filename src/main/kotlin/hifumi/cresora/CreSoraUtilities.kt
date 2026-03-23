package hifumi.cresora

import com.mojang.serialization.MapCodec
import com.mojang.serialization.codecs.RecordCodecBuilder
import dev.emi.trinkets.api.SlotReference
import dev.emi.trinkets.api.Trinket
import dev.emi.trinkets.api.TrinketsApi
import net.fabricmc.api.ModInitializer
import net.fabricmc.fabric.api.loot.v3.LootTableEvents
import net.fabricmc.loader.api.FabricLoader
import net.minecraft.entity.LivingEntity
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
	val PENDANT_ATTACK_FLAT_ID: Identifier = Identifier.of(MOD_ID, "equipment_attack_flat")
	val PENDANT_ATTACK_SCALAR_ID: Identifier = Identifier.of(MOD_ID, "equipment_attack_scalar")
	val PENDANT_HEALTH_FLAT_ID: Identifier = Identifier.of(MOD_ID, "equipment_health_flat")
	val PENDANT_HEALTH_SCALAR_ID: Identifier = Identifier.of(MOD_ID, "equipment_health_scalar")
	val PENDANT_ARMOR_FLAT_ID: Identifier = Identifier.of(MOD_ID, "equipment_armor_flat")
	val PENDANT_ARMOR_SCALAR_ID: Identifier = Identifier.of(MOD_ID, "equipment_armor_scalar")
	private val logger = LoggerFactory.getLogger(MOD_ID)
	private val HINAGATASTUE_ID: Identifier = Identifier.of(MOD_ID, "hinagatastue")
	private val TUESHOKAKU_ID: Identifier = Identifier.of(MOD_ID, "tueshokaku")
	private val VERSION_VERIFIER_ID: Identifier = Identifier.of("${MOD_ID}$version", "versionverifier")
	val STRENGTH_PENDANT: atkItem = Hinagatas_Tue(itemSettings(HINAGATASTUE_ID))
	val TUESHOKAKU: Item = tueshokaku(itemSettings(TUESHOKAKU_ID))
	val VERIFY: Item = Item(itemSettings(VERSION_VERIFIER_ID))
	lateinit var UPGRADE_SCREEN_HANDLER: ScreenHandlerType<UpgradeScreenHandler>
	lateinit var SET_LEVEL_LOOT_FUNCTION: LootFunctionType<SetLevelLootFunction>
	// MODチェック用のパケットID（他のクラスから参照するためpublic）
	override fun onInitialize() {
		// 既存の初期化処理
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
		modifyLootTables()

		Registry.register(Registries.ITEM, HINAGATASTUE_ID, STRENGTH_PENDANT)
		Registry.register(Registries.ITEM, TUESHOKAKU_ID, TUESHOKAKU)
		Registry.register(Registries.ITEM, VERSION_VERIFIER_ID, VERIFY)
		TrinketsApi.registerTrinket(STRENGTH_PENDANT, object : Trinket {
			override fun canEquip(stack: ItemStack, ref: SlotReference, entity: LivingEntity): Boolean {
				val slotType = ref.inventory().slotType
				return slotType.group == "chest" && slotType.name == "necklace"
			}
		})
		registerGlobalPlayerTickEvent()
		logger.info("CreSora Utilities initialized!")
	}

	private fun itemSettings(id: Identifier): Item.Settings {
		return Item.Settings().registryKey(RegistryKey.of(RegistryKeys.ITEM, id))
	}
	private fun registerGlobalPlayerTickEvent() {
		val atk_items=listOf<atkItem>(STRENGTH_PENDANT)
		for (i in atk_items) {
			i.register()
		}

	}
	private fun modifyLootTables() {
		// ルートテーブルがロードされるタイミングで処理を挟むイベントリスナー
		LootTableEvents.MODIFY.register { key, tableBuilder, source, lookup ->

			if (!source.isBuiltin) {
				return@register
			}

			when (key) {
					EntityType.ZOMBIE.getLootTableKey().orElse(null) -> {
						tableBuilder.pool(
							createPendantLootPool(
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
							createPendantLootPool(
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
							createPendantLootPool(
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

	private fun createPendantLootPool(
		chance: Float,
		levelProvider: LootNumberProvider,
		rarity: EquipmentRarity,
		dropProfile: EquipmentDropProfile
	): LootPool {
		return LootPool.builder()
			.rolls(ConstantLootNumberProvider.create(1.0f))
			.conditionally(RandomChanceLootCondition.builder(chance))
			.with(ItemEntry.builder(STRENGTH_PENDANT))
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

	// このLootFunctionのタイプ（登録に必要）
	override fun getType(): LootFunctionType<*> = CreSoraUtilities.SET_LEVEL_LOOT_FUNCTION

	// アイテムがドロップされる瞬間に呼び出されるメインの処理
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
		// levelProviderを使ってランダムな数値を生成し、Intに変換する
		val randomLevel = (levelProvider.nextInt(context) + lootBonus.levelBonus).coerceAtLeast(0)
		// アイテムの'level'コンポーネントに設定する
		stack.set(ModDataComponents.LEVEL, randomLevel)
		if (stack.isOf(CreSoraUtilities.STRENGTH_PENDANT)) {
			val baseRarity = forcedRarity ?: EquipmentStackSupport.rarityForLevel(randomLevel)
			val scaledRarity = AdventureRankProfile.upgradeRarity(baseRarity, adventureRank, context.random)
			EquipmentStackSupport.syncPendantData(
				stack,
					EquipmentGenerationService.createPendant(
						random = context.random,
						startingLevel = randomLevel,
						forcedRarity = scaledRarity,
						dropProfile = dropProfile
					)
				)
		}
		if (stack.isIn(ModItemTags.ADVENTURE_RANK_UPGRADE_MATERIALS)) {
			val extraCount = AdventureRankProfile.extraUpgradeMaterialCount(adventureRank, context.random)
			if (extraCount > 0) {
				stack.count = stack.count + extraCount
			}
		}
		return stack
	}

	// このLootFunctionをシリアライズ（データ化）/デシリアライズ（復元）する方法を定義
	companion object {
		// 'level_provider'という名前でLootNumberProviderをコーデックに含める
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
