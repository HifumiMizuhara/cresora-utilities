package hifumi.cresora

import com.mojang.serialization.MapCodec
import com.mojang.serialization.codecs.RecordCodecBuilder
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
import net.minecraft.resource.featuretoggle.FeatureFlags
import net.minecraft.screen.ScreenHandlerType
import net.minecraft.util.Identifier
import net.minecraft.registry.RegistryKey
import net.minecraft.registry.RegistryKeys
import org.slf4j.LoggerFactory

val version: String
	get() = FabricLoader.getInstance()
		.getModContainer(CreSoraUtilities.MOD_ID)
		.map { it.metadata.version.friendlyString }
		.orElse("unknown")

object CreSoraUtilities : ModInitializer {
	const val MOD_ID = "cresora-utilities"
	val PENDANT_ATTRIBUTE_ID: Identifier = Identifier.of(MOD_ID, "strength_pendant_bonus")
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
		modifyLootTables()

		Registry.register(Registries.ITEM, HINAGATASTUE_ID, STRENGTH_PENDANT)
		Registry.register(Registries.ITEM, TUESHOKAKU_ID, TUESHOKAKU)
		Registry.register(Registries.ITEM, VERSION_VERIFIER_ID, VERIFY)
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

			// ゾンビのルートテーブルIDと比較する
			// 以前の 'id' (Identifier) は、新しい 'key' (RegistryKey) の .value から取得する
			if (source.isBuiltin && (EntityType.ZOMBIE.getLootTableKey().orElse(null) == key || EntityType.SKELETON.getLootTableKey().orElse(null) == key)) {

				// 新しいドロップアイテムのプールを作成する (この部分のロジックは変更なし)
				val poolBuilder = LootPool.builder()
					.rolls(ConstantLootNumberProvider.create(1.0f))
					.conditionally(RandomChanceLootCondition.builder(0.2f))
					.with(ItemEntry.builder(STRENGTH_PENDANT))
					.apply(SetCountLootFunction.builder(
						UniformLootNumberProvider.create(1.0f,1.0f)
					)).build()

				// 作成したプールを既存のルートテーブルに追加
				tableBuilder.pool(poolBuilder)
				val poolBuilder2 = LootPool.builder()
					.rolls(ConstantLootNumberProvider.create(1.0f))
					.conditionally(RandomChanceLootCondition.builder(0.1f))
					.with(ItemEntry.builder(TUESHOKAKU))
					.apply(SetCountLootFunction.builder(
						UniformLootNumberProvider.create(1.0f,1.0f)
					)).apply(
						SetLevelLootFunction(UniformLootNumberProvider.create(1.0f, 2.0f))
					).build()
				tableBuilder.pool(poolBuilder2)
			}
			if (source.isBuiltin && (EntityType.WARDEN.getLootTableKey().orElse(null) == key)) {
				val poolBuilder2 = LootPool.builder()
					.rolls(ConstantLootNumberProvider.create(1.0f))
					.conditionally(RandomChanceLootCondition.builder(1.0f))
					.with(ItemEntry.builder(STRENGTH_PENDANT))
					.apply(SetCountLootFunction.builder(
						UniformLootNumberProvider.create(1.0f,1.0f)
					)).apply(
						SetLevelLootFunction(UniformLootNumberProvider.create(5.0f, 7.0f))
					).build()
				tableBuilder.pool(poolBuilder2)
			}
		}
	}
}
class SetLevelLootFunction(val levelProvider: LootNumberProvider) : LootFunction {

	// このLootFunctionのタイプ（登録に必要）
	override fun getType(): LootFunctionType<*> = CreSoraUtilities.SET_LEVEL_LOOT_FUNCTION

	// アイテムがドロップされる瞬間に呼び出されるメインの処理
	override fun apply(stack: ItemStack, context: LootContext): ItemStack {
		// levelProviderを使ってランダムな数値を生成し、Intに変換する
		val randomLevel = levelProvider.nextInt(context)
		// アイテムの'level'コンポーネントに設定する
		stack.set(ModDataComponents.LEVEL, randomLevel)
		return stack
	}

	// このLootFunctionをシリアライズ（データ化）/デシリアライズ（復元）する方法を定義
	companion object {
		// 'level_provider'という名前でLootNumberProviderをコーデックに含める
		val CODEC: MapCodec<SetLevelLootFunction> = RecordCodecBuilder.mapCodec { instance ->
			instance.group(
				LootNumberProviderTypes.CODEC.fieldOf("level_provider").forGetter(SetLevelLootFunction::levelProvider)
			).apply(instance, ::SetLevelLootFunction)
		}
	}
}
