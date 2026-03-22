package hifumi.cresora

import com.mojang.serialization.Codec

enum class EquipmentDropProfile(
    val id: String,
    private val mainWeights: Map<StatType, Double>,
    private val subWeights: Map<StatType, Double>
) {
    ZOMBIE_SURVIVOR(
        id = "zombie_survivor",
        mainWeights = mapOf(
            StatType.HP_FLAT to 4.0,
            StatType.HP_PERCENT to 3.6,
            StatType.DEF_FLAT to 3.2,
            StatType.DEF_PERCENT to 3.2,
            StatType.DAMAGE_REDUCTION to 2.8
        ),
        subWeights = mapOf(
            StatType.HP_FLAT to 3.4,
            StatType.HP_PERCENT to 3.2,
            StatType.DEF_FLAT to 3.0,
            StatType.DEF_PERCENT to 3.0,
            StatType.DAMAGE_REDUCTION to 2.8
        )
    ),
    SKELETON_ASSAULT(
        id = "skeleton_assault",
        mainWeights = mapOf(
            StatType.ATK_FLAT to 3.4,
            StatType.ATK_PERCENT to 3.4,
            StatType.ALL_DMG_BONUS to 3.0
        ),
        subWeights = mapOf(
            StatType.ATK_FLAT to 2.6,
            StatType.ATK_PERCENT to 2.8,
            StatType.CRIT_RATE to 3.6,
            StatType.CRIT_DMG to 3.8,
            StatType.ALL_DMG_BONUS to 2.8
        )
    ),
    WARDEN_RELIC(
        id = "warden_relic",
        mainWeights = mapOf(
            StatType.ATK_PERCENT to 3.0,
            StatType.HP_PERCENT to 2.4,
            StatType.DEF_PERCENT to 2.4,
            StatType.ALL_DMG_BONUS to 3.4,
            StatType.DAMAGE_REDUCTION to 2.8
        ),
        subWeights = mapOf(
            StatType.CRIT_RATE to 3.0,
            StatType.CRIT_DMG to 3.2,
            StatType.ALL_DMG_BONUS to 3.4,
            StatType.DAMAGE_REDUCTION to 3.0,
            StatType.HP_PERCENT to 2.2,
            StatType.DEF_PERCENT to 2.2
        )
    );

    fun mainWeight(type: StatType): Double = mainWeights[type] ?: 1.0

    fun subWeight(type: StatType): Double = subWeights[type] ?: 1.0

    companion object {
        private val BY_ID = entries.associateBy(EquipmentDropProfile::id)

        val CODEC: Codec<EquipmentDropProfile> = Codec.STRING.xmap(
            { id -> BY_ID[id] ?: throw IllegalArgumentException("Unknown equipment drop profile: $id") },
            EquipmentDropProfile::id
        )
    }
}
