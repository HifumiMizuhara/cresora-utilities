package hifumi.cresora

data class EquipmentDefinition(
    val slotType: EquipmentSlotType,
    val setId: EquipmentSet
)

object EquipmentDefinitions {
    val HINAGATA_WAND = EquipmentDefinition(EquipmentSlotType.WAND, EquipmentSet.HINAGATA)
    val HINAGATA_HAT = EquipmentDefinition(EquipmentSlotType.HAT, EquipmentSet.HINAGATA)
    val HINAGATA_GLASSES = EquipmentDefinition(EquipmentSlotType.GLASSES, EquipmentSet.HINAGATA)
    val HINAGATA_ARMOR = EquipmentDefinition(EquipmentSlotType.ARMOR, EquipmentSet.HINAGATA)
    val HINAGATA_BOOTS = EquipmentDefinition(EquipmentSlotType.BOOTS, EquipmentSet.HINAGATA)
}
