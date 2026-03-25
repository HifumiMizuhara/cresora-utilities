package hifumi.cresora

import com.mojang.serialization.Codec

enum class EquipmentSlotType(
    val id: String,
    val trinketGroup: String,
    val trinketSlot: String,
    val mainStatCandidates: List<StatType>,
    private val mainWeights: Map<StatType, Double>,
    private val subWeights: Map<StatType, Double>,
    private val defaultMainStat: StatType
) {
    WAND(
        id = "wand",
        trinketGroup = "chest",
        trinketSlot = "necklace",
        mainStatCandidates = listOf(StatType.ATK_FLAT, StatType.ATK_PERCENT, StatType.ALL_DMG_BONUS),
        mainWeights = mapOf(
            StatType.ATK_FLAT to 3.2,
            StatType.ATK_PERCENT to 3.6,
            StatType.ALL_DMG_BONUS to 3.4
        ),
        subWeights = mapOf(
            StatType.ATK_FLAT to 2.8,
            StatType.ATK_PERCENT to 3.0,
            StatType.CRIT_RATE to 2.8,
            StatType.CRIT_DMG to 3.2,
            StatType.ALL_DMG_BONUS to 3.0
        ),
        defaultMainStat = StatType.ATK_FLAT
    ),
    HAT(
        id = "hat",
        trinketGroup = "head",
        trinketSlot = "hat",
        mainStatCandidates = listOf(StatType.HP_FLAT, StatType.HP_PERCENT, StatType.DEF_FLAT, StatType.DEF_PERCENT, StatType.DAMAGE_REDUCTION),
        mainWeights = mapOf(
            StatType.HP_FLAT to 2.8,
            StatType.HP_PERCENT to 2.6,
            StatType.DEF_FLAT to 3.8,
            StatType.DEF_PERCENT to 3.6,
            StatType.DAMAGE_REDUCTION to 2.4
        ),
        subWeights = mapOf(
            StatType.HP_FLAT to 2.6,
            StatType.HP_PERCENT to 2.4,
            StatType.DEF_FLAT to 3.2,
            StatType.DEF_PERCENT to 3.0,
            StatType.DAMAGE_REDUCTION to 2.4
        ),
        defaultMainStat = StatType.DEF_FLAT
    ),
    GLASSES(
        id = "glasses",
        trinketGroup = "head",
        trinketSlot = "face",
        mainStatCandidates = listOf(StatType.ATK_PERCENT, StatType.CRIT_RATE, StatType.CRIT_DMG, StatType.ALL_DMG_BONUS),
        mainWeights = mapOf(
            StatType.ATK_PERCENT to 2.6,
            StatType.CRIT_RATE to 3.4,
            StatType.CRIT_DMG to 3.2,
            StatType.ALL_DMG_BONUS to 3.0
        ),
        subWeights = mapOf(
            StatType.ATK_PERCENT to 2.6,
            StatType.CRIT_RATE to 3.6,
            StatType.CRIT_DMG to 3.8,
            StatType.ALL_DMG_BONUS to 2.8
        ),
        defaultMainStat = StatType.CRIT_RATE
    ),
    ARMOR(
        id = "armor",
        trinketGroup = "chest",
        trinketSlot = "back",
        mainStatCandidates = listOf(StatType.HP_FLAT, StatType.HP_PERCENT, StatType.DEF_FLAT, StatType.DEF_PERCENT, StatType.DAMAGE_REDUCTION),
        mainWeights = mapOf(
            StatType.HP_FLAT to 3.2,
            StatType.HP_PERCENT to 3.0,
            StatType.DEF_FLAT to 3.2,
            StatType.DEF_PERCENT to 3.0,
            StatType.DAMAGE_REDUCTION to 2.8
        ),
        subWeights = mapOf(
            StatType.HP_FLAT to 2.8,
            StatType.HP_PERCENT to 2.6,
            StatType.DEF_FLAT to 2.8,
            StatType.DEF_PERCENT to 2.8,
            StatType.DAMAGE_REDUCTION to 2.8
        ),
        defaultMainStat = StatType.HP_FLAT
    ),
    BOOTS(
        id = "boots",
        trinketGroup = "feet",
        trinketSlot = "shoes",
        mainStatCandidates = listOf(StatType.ATK_PERCENT, StatType.HP_PERCENT, StatType.DEF_PERCENT, StatType.CRIT_DMG, StatType.ALL_DMG_BONUS),
        mainWeights = mapOf(
            StatType.ATK_PERCENT to 2.8,
            StatType.HP_PERCENT to 2.2,
            StatType.DEF_PERCENT to 2.2,
            StatType.CRIT_DMG to 3.0,
            StatType.ALL_DMG_BONUS to 2.8
        ),
        subWeights = mapOf(
            StatType.ATK_PERCENT to 2.6,
            StatType.CRIT_RATE to 2.8,
            StatType.CRIT_DMG to 3.2,
            StatType.ALL_DMG_BONUS to 2.6,
            StatType.DEF_PERCENT to 2.0
        ),
        defaultMainStat = StatType.ATK_PERCENT
    );

    fun translationKey(): String = "item.cresora.equipment.slot.$id"

    fun mainWeight(type: StatType): Double = mainWeights[type] ?: 1.0

    fun subWeight(type: StatType): Double = subWeights[type] ?: 1.0

    fun defaultMainStat(): StatType = defaultMainStat

    companion object {
        private val BY_ID = entries.associateBy(EquipmentSlotType::id)

        val CODEC: Codec<EquipmentSlotType> = Codec.STRING.xmap(
            { id -> BY_ID[id] ?: throw IllegalArgumentException("Unknown equipment slot type: $id") },
            EquipmentSlotType::id
        )
    }
}
