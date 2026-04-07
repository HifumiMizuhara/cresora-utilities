package hifumi.cresora

import com.google.gson.JsonParser
import com.mojang.serialization.Codec
import com.mojang.serialization.JsonOps
import com.mojang.serialization.codecs.RecordCodecBuilder
import net.minecraft.item.Item
import net.minecraft.item.ItemStack
import net.minecraft.registry.Registries
import net.minecraft.server.network.ServerPlayerEntity
import net.minecraft.text.Text
import net.minecraft.util.Identifier
import org.slf4j.LoggerFactory
import java.io.InputStreamReader

data class ShopResourceOfferDefinition(
    val id: String,
    val itemId: String,
    val count: Int,
    val price: Int,
    val visible: Boolean = true
) {
    companion object {
        val CODEC: Codec<ShopResourceOfferDefinition> = RecordCodecBuilder.create { instance ->
            instance.group(
                Codec.STRING.fieldOf("id").forGetter(ShopResourceOfferDefinition::id),
                Codec.STRING.fieldOf("itemId").forGetter(ShopResourceOfferDefinition::itemId),
                Codec.INT.fieldOf("count").forGetter(ShopResourceOfferDefinition::count),
                Codec.INT.fieldOf("price").forGetter(ShopResourceOfferDefinition::price),
                Codec.BOOL.optionalFieldOf("visible", true).forGetter(ShopResourceOfferDefinition::visible)
            ).apply(instance, ::ShopResourceOfferDefinition)
        }
    }

    fun item(): Item = Registries.ITEM.get(Identifier.of(itemId))
}

data class ShopContentBundle(
    val resourceOffers: List<ShopResourceOfferDefinition>
) {
    companion object {
        val CODEC: Codec<ShopContentBundle> = RecordCodecBuilder.create { instance ->
            instance.group(
                ShopResourceOfferDefinition.CODEC.listOf().fieldOf("resourceOffers").forGetter(ShopContentBundle::resourceOffers)
            ).apply(instance, ::ShopContentBundle)
        }
    }
}

sealed interface ShopOfferDefinition {
    val id: String
    val price: Int

    fun createDisplayStack(): ItemStack

    fun displayName(): Text

    fun grant(player: ServerPlayerEntity)
}

data class ArtifactSpecialShopOffer(
    val definition: ArtifactSpecialItemDefinition
) : ShopOfferDefinition {
    override val id: String = definition.id
    override val price: Int = definition.shopPrice

    override fun createDisplayStack(): ItemStack = ArtifactDisplayStackFactory.shopDisplay(definition)

    override fun displayName(): Text = Text.translatable(definition.translationKeyId)

    override fun grant(player: ServerPlayerEntity) {
        val item = ArtifactSpecialItemSupport.itemForDefinitionId(definition.id) ?: return
        player.inventory.offerOrDrop(ItemStack(item))
    }
}

data class ResourceShopOffer(
    val definition: ShopResourceOfferDefinition
) : ShopOfferDefinition {
    override val id: String = definition.id
    override val price: Int = definition.price

    override fun createDisplayStack(): ItemStack = ArtifactDisplayStackFactory.shopDisplay(definition)

    override fun displayName(): Text = Text.translatable(definition.item().translationKey)

    override fun grant(player: ServerPlayerEntity) {
        player.inventory.offerOrDrop(ItemStack(definition.item(), definition.count))
    }
}

object ShopContentRegistry {
    private const val CONTENT_RESOURCE = "data/cresora-utilities/cresora/shop_content.json"

    private val logger = LoggerFactory.getLogger("${CreSoraUtilities.MOD_ID}/shop-content")

    @Volatile
    private var resourceOffers: Map<String, ShopResourceOfferDefinition> = emptyMap()

    fun init() {
        applyBundle(defaultBundle())
        runCatching { loadBundledContent() }
            .onSuccess { bundle ->
                applyBundle(bundle)
                logger.info("Loaded shop content from {}", CONTENT_RESOURCE)
            }
            .onFailure { throwable ->
                logger.error("Failed to load shop content from {}. Using built-in defaults.", CONTENT_RESOURCE, throwable)
            }
    }

    fun shopOffers(): List<ShopOfferDefinition> {
        val artifactOffers = ArtifactSpecialItemRegistry.shopDefinitions().map(::ArtifactSpecialShopOffer)
        val resourceEntries = resourceOffers.values
            .filter(ShopResourceOfferDefinition::visible)
            .sortedBy(ShopResourceOfferDefinition::price)
            .map(::ResourceShopOffer)
        return artifactOffers + resourceEntries
    }

    private fun loadBundledContent(): ShopContentBundle {
        val stream = ShopContentRegistry::class.java.classLoader.getResourceAsStream(CONTENT_RESOURCE)
            ?: error("Missing resource: $CONTENT_RESOURCE")
        InputStreamReader(stream).use { reader ->
            val json = JsonParser.parseReader(reader)
            return ShopContentBundle.CODEC.parse(JsonOps.INSTANCE, json)
                .getOrThrow { message -> IllegalArgumentException("Invalid shop content: $message") }
        }
    }

    private fun applyBundle(bundle: ShopContentBundle) {
        val offerMap = bundle.resourceOffers.associateBy(ShopResourceOfferDefinition::id)
        require(offerMap.size == bundle.resourceOffers.size) { "Duplicate shop offer ids found in content bundle" }
        for (offer in offerMap.values) {
            require(offer.count > 0) { "Shop offer '${offer.id}' must have positive count" }
            require(offer.price >= 0) { "Shop offer '${offer.id}' must have non-negative price" }
            val itemId = Identifier.of(offer.itemId)
            require(Registries.ITEM.containsId(itemId)) { "Shop offer '${offer.id}' references unknown item '${offer.itemId}'" }
        }
        resourceOffers = offerMap
    }

    private fun defaultBundle(): ShopContentBundle {
        return ShopContentBundle(
            resourceOffers = listOf(
                ShopResourceOfferDefinition("iron_ingot_pack", "minecraft:iron_ingot", 16, 24_000),
                ShopResourceOfferDefinition("coal_pack", "minecraft:coal", 32, 20_000),
                ShopResourceOfferDefinition("redstone_pack", "minecraft:redstone", 32, 26_000),
                ShopResourceOfferDefinition("lapis_pack", "minecraft:lapis_lazuli", 32, 26_000),
                ShopResourceOfferDefinition("gold_ingot_pack", "minecraft:gold_ingot", 16, 34_000),
                ShopResourceOfferDefinition("quartz_pack", "minecraft:quartz", 24, 38_000),
                ShopResourceOfferDefinition("emerald_pack", "minecraft:emerald", 8, 72_000),
                ShopResourceOfferDefinition("diamond_pack", "minecraft:diamond", 4, 120_000)
            )
        )
    }
}
