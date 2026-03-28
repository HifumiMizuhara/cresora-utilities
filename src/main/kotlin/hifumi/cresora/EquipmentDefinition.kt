package hifumi.cresora

import com.mojang.serialization.Codec
import com.mojang.serialization.codecs.RecordCodecBuilder

data class EquipmentDefinitionRef(
    val id: String
) {
    fun resolve(): EquipmentDefinition = EquipmentContentRegistry.requireEquipment(id)
}

data class EquipmentDefinition(
    val id: String,
    val slotTypeId: String,
    val setId: String,
    val rarityWeights: Map<EquipmentRarity, Double> = emptyMap(),
    val baseItemId: String? = null,
    val opensUpgradeScreen: Boolean = false
) {
    fun slotType(): EquipmentSlotType = EquipmentContentRegistry.requireSlot(slotTypeId)

    fun setDefinition(): EquipmentSet = EquipmentContentRegistry.requireSet(setId)

    companion object {
        val CODEC: Codec<EquipmentDefinition> = RecordCodecBuilder.create { instance ->
            instance.group(
                Codec.STRING.fieldOf("id").forGetter(EquipmentDefinition::id),
                Codec.STRING.fieldOf("slotTypeId").forGetter(EquipmentDefinition::slotTypeId),
                Codec.STRING.fieldOf("setId").forGetter(EquipmentDefinition::setId),
                Codec.unboundedMap(EquipmentRarity.CODEC, Codec.DOUBLE)
                    .optionalFieldOf("rarityWeights", emptyMap())
                    .forGetter(EquipmentDefinition::rarityWeights),
                Codec.STRING.optionalFieldOf("baseItemId")
                    .forGetter { java.util.Optional.ofNullable(it.baseItemId) },
                Codec.BOOL.optionalFieldOf("opensUpgradeScreen", false)
                    .forGetter(EquipmentDefinition::opensUpgradeScreen)
            ).apply(instance) { id, slotTypeId, setId, rarityWeights, baseItemId, opensUpgradeScreen ->
                EquipmentDefinition(id, slotTypeId, setId, rarityWeights, baseItemId.orElse(null), opensUpgradeScreen)
            }
        }
    }
}

object EquipmentDefinitions {
    val HINAGATA_WAND = EquipmentDefinitionRef("hinagata_wand")
    val HINAGATA_HAT = EquipmentDefinitionRef("hinagata_hat")
    val HINAGATA_GLASSES = EquipmentDefinitionRef("hinagata_glasses")
    val HINAGATA_ARMOR = EquipmentDefinitionRef("hinagata_armor")
    val HINAGATA_BOOTS = EquipmentDefinitionRef("hinagata_boots")
}
