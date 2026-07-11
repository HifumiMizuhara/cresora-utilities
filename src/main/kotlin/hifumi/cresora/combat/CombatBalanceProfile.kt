package hifumi.cresora.combat

import com.google.gson.JsonParser
import com.mojang.serialization.Codec
import com.mojang.serialization.JsonOps
import com.mojang.serialization.codecs.RecordCodecBuilder
import hifumi.cresora.CreSoraUtilities
import org.slf4j.LoggerFactory
import java.io.InputStreamReader

data class CombatFamilyBalance(
    val healthGrowth: Double,
    val healthPhaseBonus: Double,
    val armorBase: Double,
    val armorGrowth: Double,
    val armorPhaseBonus: Double,
    val toughnessBase: Double,
    val toughnessGrowth: Double,
    val toughnessPhaseBonus: Double,
    val damageGrowth: Double,
    val damagePhaseBonus: Double,
    val overflowArmorPerTenHealth: Double,
    val overflowToughnessPerTwentyFiveHealth: Double
) {
    companion object {
        val CODEC: Codec<CombatFamilyBalance> = RecordCodecBuilder.create { instance ->
            instance.group(
                Codec.DOUBLE.fieldOf("healthGrowth").forGetter(CombatFamilyBalance::healthGrowth),
                Codec.DOUBLE.fieldOf("healthPhaseBonus").forGetter(CombatFamilyBalance::healthPhaseBonus),
                Codec.DOUBLE.fieldOf("armorBase").forGetter(CombatFamilyBalance::armorBase),
                Codec.DOUBLE.fieldOf("armorGrowth").forGetter(CombatFamilyBalance::armorGrowth),
                Codec.DOUBLE.fieldOf("armorPhaseBonus").forGetter(CombatFamilyBalance::armorPhaseBonus),
                Codec.DOUBLE.fieldOf("toughnessBase").forGetter(CombatFamilyBalance::toughnessBase),
                Codec.DOUBLE.fieldOf("toughnessGrowth").forGetter(CombatFamilyBalance::toughnessGrowth),
                Codec.DOUBLE.fieldOf("toughnessPhaseBonus").forGetter(CombatFamilyBalance::toughnessPhaseBonus),
                Codec.DOUBLE.fieldOf("damageGrowth").forGetter(CombatFamilyBalance::damageGrowth),
                Codec.DOUBLE.fieldOf("damagePhaseBonus").forGetter(CombatFamilyBalance::damagePhaseBonus),
                Codec.DOUBLE.fieldOf("overflowArmorPerTenHealth").forGetter(CombatFamilyBalance::overflowArmorPerTenHealth),
                Codec.DOUBLE.fieldOf("overflowToughnessPerTwentyFiveHealth").forGetter(CombatFamilyBalance::overflowToughnessPerTwentyFiveHealth)
            ).apply(instance, ::CombatFamilyBalance)
        }
    }
}

data class CombatDomainBalance(
    val normalHealthBase: Double,
    val normalHealthGrowth: Double,
    val eliteHealthBonus: Double,
    val bossHealthBonus: Double,
    val defenseGrowth: Double,
    val eliteDefenseBonus: Double,
    val bossDefenseBonus: Double,
    val toughnessGrowth: Double,
    val eliteToughnessBonus: Double,
    val bossToughnessBonus: Double,
    val damageGrowth: Double,
    val eliteDamageBonus: Double,
    val bossDamageBonus: Double
) {
    companion object {
        val CODEC: Codec<CombatDomainBalance> = RecordCodecBuilder.create { instance ->
            instance.group(
                Codec.DOUBLE.fieldOf("normalHealthBase").forGetter(CombatDomainBalance::normalHealthBase),
                Codec.DOUBLE.fieldOf("normalHealthGrowth").forGetter(CombatDomainBalance::normalHealthGrowth),
                Codec.DOUBLE.fieldOf("eliteHealthBonus").forGetter(CombatDomainBalance::eliteHealthBonus),
                Codec.DOUBLE.fieldOf("bossHealthBonus").forGetter(CombatDomainBalance::bossHealthBonus),
                Codec.DOUBLE.fieldOf("defenseGrowth").forGetter(CombatDomainBalance::defenseGrowth),
                Codec.DOUBLE.fieldOf("eliteDefenseBonus").forGetter(CombatDomainBalance::eliteDefenseBonus),
                Codec.DOUBLE.fieldOf("bossDefenseBonus").forGetter(CombatDomainBalance::bossDefenseBonus),
                Codec.DOUBLE.fieldOf("toughnessGrowth").forGetter(CombatDomainBalance::toughnessGrowth),
                Codec.DOUBLE.fieldOf("eliteToughnessBonus").forGetter(CombatDomainBalance::eliteToughnessBonus),
                Codec.DOUBLE.fieldOf("bossToughnessBonus").forGetter(CombatDomainBalance::bossToughnessBonus),
                Codec.DOUBLE.fieldOf("damageGrowth").forGetter(CombatDomainBalance::damageGrowth),
                Codec.DOUBLE.fieldOf("eliteDamageBonus").forGetter(CombatDomainBalance::eliteDamageBonus),
                Codec.DOUBLE.fieldOf("bossDamageBonus").forGetter(CombatDomainBalance::bossDamageBonus)
            ).apply(instance, ::CombatDomainBalance)
        }
    }
}

data class CombatFieldBalance(
    val eliteHealthScalar: Double,
    val eliteDefenseScalar: Double,
    val eliteToughnessScalar: Double,
    val eliteDamageScalar: Double,
    val eliteScaleBonus: Double,
    val bossHealthScalar: Double,
    val bossDefenseScalar: Double,
    val bossToughnessScalar: Double,
    val bossDamageScalar: Double,
    val bossScaleBonus: Double
) {
    companion object {
        val CODEC: Codec<CombatFieldBalance> = RecordCodecBuilder.create { instance ->
            instance.group(
                Codec.DOUBLE.fieldOf("eliteHealthScalar").forGetter(CombatFieldBalance::eliteHealthScalar),
                Codec.DOUBLE.fieldOf("eliteDefenseScalar").forGetter(CombatFieldBalance::eliteDefenseScalar),
                Codec.DOUBLE.fieldOf("eliteToughnessScalar").forGetter(CombatFieldBalance::eliteToughnessScalar),
                Codec.DOUBLE.fieldOf("eliteDamageScalar").forGetter(CombatFieldBalance::eliteDamageScalar),
                Codec.DOUBLE.fieldOf("eliteScaleBonus").forGetter(CombatFieldBalance::eliteScaleBonus),
                Codec.DOUBLE.fieldOf("bossHealthScalar").forGetter(CombatFieldBalance::bossHealthScalar),
                Codec.DOUBLE.fieldOf("bossDefenseScalar").forGetter(CombatFieldBalance::bossDefenseScalar),
                Codec.DOUBLE.fieldOf("bossToughnessScalar").forGetter(CombatFieldBalance::bossToughnessScalar),
                Codec.DOUBLE.fieldOf("bossDamageScalar").forGetter(CombatFieldBalance::bossDamageScalar),
                Codec.DOUBLE.fieldOf("bossScaleBonus").forGetter(CombatFieldBalance::bossScaleBonus)
            ).apply(instance, ::CombatFieldBalance)
        }
    }
}

data class CombatRegenBalance(
    val combatPercentMaxHealthPerSecond: Double,
    val nonCombatPercentMaxHealthPerSecond: Double
) {
    companion object {
        val CODEC: Codec<CombatRegenBalance> = RecordCodecBuilder.create { instance ->
            instance.group(
                Codec.DOUBLE.fieldOf("combatPercentMaxHealthPerSecond").forGetter(CombatRegenBalance::combatPercentMaxHealthPerSecond),
                Codec.DOUBLE.fieldOf("nonCombatPercentMaxHealthPerSecond").forGetter(CombatRegenBalance::nonCombatPercentMaxHealthPerSecond)
            ).apply(instance, ::CombatRegenBalance)
        }
    }
}

data class CombatCaps(
    val maxPlayerResistancePercent: Double,
    val minMobResistancePercent: Double,
    val maxMobResistancePercent: Double,
    val maxDamageBonusRatio: Double,
    val maxCritDamageRatio: Double
) {
    companion object {
        val CODEC: Codec<CombatCaps> = RecordCodecBuilder.create { instance ->
            instance.group(
                Codec.DOUBLE.fieldOf("maxPlayerResistancePercent").forGetter(CombatCaps::maxPlayerResistancePercent),
                Codec.DOUBLE.fieldOf("minMobResistancePercent").forGetter(CombatCaps::minMobResistancePercent),
                Codec.DOUBLE.fieldOf("maxMobResistancePercent").forGetter(CombatCaps::maxMobResistancePercent),
                Codec.DOUBLE.fieldOf("maxDamageBonusRatio").forGetter(CombatCaps::maxDamageBonusRatio),
                Codec.DOUBLE.fieldOf("maxCritDamageRatio").forGetter(CombatCaps::maxCritDamageRatio)
            ).apply(instance, ::CombatCaps)
        }
    }
}

data class CombatTuning(
    val bloodMoonWaveHealthGrowth: Double,
    val bloodMoonWaveDefenseGrowth: Double,
    val masqueradeHealthScalar: Double,
    val masqueradeDamageScalar: Double,
    val artifactMainRollScalar: Double,
    val artifactSubRollScalar: Double,
    val weaponBaseDamageScalar: Double,
    val weaponLevelGrowthScalar: Double,
    val weaponBreakthroughAttackFactor: Double,
    val weaponBreakthroughCritRateBonus: Double,
    val weaponBreakthroughAllDamageBonus: Double
) {
    companion object {
        val CODEC: Codec<CombatTuning> = RecordCodecBuilder.create { instance ->
            instance.group(
                Codec.DOUBLE.fieldOf("bloodMoonWaveHealthGrowth").forGetter(CombatTuning::bloodMoonWaveHealthGrowth),
                Codec.DOUBLE.fieldOf("bloodMoonWaveDefenseGrowth").forGetter(CombatTuning::bloodMoonWaveDefenseGrowth),
                Codec.DOUBLE.fieldOf("masqueradeHealthScalar").forGetter(CombatTuning::masqueradeHealthScalar),
                Codec.DOUBLE.fieldOf("masqueradeDamageScalar").forGetter(CombatTuning::masqueradeDamageScalar),
                Codec.DOUBLE.fieldOf("artifactMainRollScalar").forGetter(CombatTuning::artifactMainRollScalar),
                Codec.DOUBLE.fieldOf("artifactSubRollScalar").forGetter(CombatTuning::artifactSubRollScalar),
                Codec.DOUBLE.fieldOf("weaponBaseDamageScalar").forGetter(CombatTuning::weaponBaseDamageScalar),
                Codec.DOUBLE.fieldOf("weaponLevelGrowthScalar").forGetter(CombatTuning::weaponLevelGrowthScalar),
                Codec.DOUBLE.fieldOf("weaponBreakthroughAttackFactor").forGetter(CombatTuning::weaponBreakthroughAttackFactor),
                Codec.DOUBLE.fieldOf("weaponBreakthroughCritRateBonus").forGetter(CombatTuning::weaponBreakthroughCritRateBonus),
                Codec.DOUBLE.fieldOf("weaponBreakthroughAllDamageBonus").forGetter(CombatTuning::weaponBreakthroughAllDamageBonus)
            ).apply(instance, ::CombatTuning)
        }
    }
}

data class CombatBalanceProfile(
    val version: Int,
    val caps: CombatCaps,
    val survivor: CombatFamilyBalance,
    val assault: CombatFamilyBalance,
    val arcane: CombatFamilyBalance,
    val elite: CombatFamilyBalance,
    val relic: CombatFamilyBalance,
    val domain: CombatDomainBalance,
    val field: CombatFieldBalance,
    val regen: CombatRegenBalance,
    val tuning: CombatTuning
) {
    val maxPlayerResistancePercent: Double get() = caps.maxPlayerResistancePercent
    val minMobResistancePercent: Double get() = caps.minMobResistancePercent
    val maxMobResistancePercent: Double get() = caps.maxMobResistancePercent
    val maxDamageBonusRatio: Double get() = caps.maxDamageBonusRatio
    val maxCritDamageRatio: Double get() = caps.maxCritDamageRatio
    val bloodMoonWaveHealthGrowth: Double get() = tuning.bloodMoonWaveHealthGrowth
    val bloodMoonWaveDefenseGrowth: Double get() = tuning.bloodMoonWaveDefenseGrowth
    val masqueradeHealthScalar: Double get() = tuning.masqueradeHealthScalar
    val masqueradeDamageScalar: Double get() = tuning.masqueradeDamageScalar
    val artifactMainRollScalar: Double get() = tuning.artifactMainRollScalar
    val artifactSubRollScalar: Double get() = tuning.artifactSubRollScalar
    val weaponBaseDamageScalar: Double get() = tuning.weaponBaseDamageScalar
    val weaponLevelGrowthScalar: Double get() = tuning.weaponLevelGrowthScalar
    val weaponBreakthroughAttackFactor: Double get() = tuning.weaponBreakthroughAttackFactor
    val weaponBreakthroughCritRateBonus: Double get() = tuning.weaponBreakthroughCritRateBonus
    val weaponBreakthroughAllDamageBonus: Double get() = tuning.weaponBreakthroughAllDamageBonus

    companion object {
        val CODEC: Codec<CombatBalanceProfile> = RecordCodecBuilder.create { instance ->
            instance.group(
                Codec.INT.fieldOf("version").forGetter(CombatBalanceProfile::version),
                CombatCaps.CODEC.fieldOf("caps").forGetter(CombatBalanceProfile::caps),
                CombatFamilyBalance.CODEC.fieldOf("survivor").forGetter(CombatBalanceProfile::survivor),
                CombatFamilyBalance.CODEC.fieldOf("assault").forGetter(CombatBalanceProfile::assault),
                CombatFamilyBalance.CODEC.fieldOf("arcane").forGetter(CombatBalanceProfile::arcane),
                CombatFamilyBalance.CODEC.fieldOf("elite").forGetter(CombatBalanceProfile::elite),
                CombatFamilyBalance.CODEC.fieldOf("relic").forGetter(CombatBalanceProfile::relic),
                CombatDomainBalance.CODEC.fieldOf("domain").forGetter(CombatBalanceProfile::domain),
                CombatFieldBalance.CODEC.fieldOf("field").forGetter(CombatBalanceProfile::field),
                CombatRegenBalance.CODEC.fieldOf("regen").forGetter(CombatBalanceProfile::regen),
                CombatTuning.CODEC.fieldOf("tuning").forGetter(CombatBalanceProfile::tuning)
            ).apply(instance, ::CombatBalanceProfile)
        }
    }
}

object CombatBalanceProfileRegistry {
    private const val CONTENT_RESOURCE = "data/cresora-utilities/cresora/combat_balance.json"
    private val logger = LoggerFactory.getLogger("${CreSoraUtilities.MOD_ID}/combat-balance")

    @Volatile
    private var profile: CombatBalanceProfile = defaultProfile()

    fun init() {
        runCatching(::loadBundledProfile)
            .onSuccess {
                validate(it)
                profile = it
                logger.info("Loaded combat balance profile version {}", it.version)
            }
            .onFailure { logger.error("Failed to load {}. Using built-in combat balance defaults.", CONTENT_RESOURCE, it) }
    }

    fun current(): CombatBalanceProfile = profile

    private fun loadBundledProfile(): CombatBalanceProfile {
        val stream = CombatBalanceProfileRegistry::class.java.classLoader.getResourceAsStream(CONTENT_RESOURCE)
            ?: error("Missing resource: $CONTENT_RESOURCE")
        InputStreamReader(stream).use { reader ->
            return CombatBalanceProfile.CODEC.parse(JsonOps.INSTANCE, JsonParser.parseReader(reader))
                .getOrThrow { message -> IllegalArgumentException("Invalid combat balance profile: $message") }
        }
    }

    private fun validate(value: CombatBalanceProfile) {
        require(value.version >= 1) { "Combat balance version must be positive" }
        require(value.minMobResistancePercent >= -50.0) { "Mob resistance floor cannot be below -50" }
        require(value.maxMobResistancePercent in 0.0..75.0) { "Mob resistance cap must be within 0..75" }
        require(value.minMobResistancePercent <= value.maxMobResistancePercent) { "Mob resistance bounds are invalid" }
        require(value.maxPlayerResistancePercent in 0.0..75.0) { "Player resistance cap must be within 0..75" }
        require(value.maxDamageBonusRatio >= 0.0 && value.maxCritDamageRatio >= 0.0) { "Damage caps must be non-negative" }
        require(value.regen.combatPercentMaxHealthPerSecond >= 0.0 && value.regen.nonCombatPercentMaxHealthPerSecond >= 0.0) { "Regen rates must be non-negative" }
        require(value.artifactMainRollScalar > 0.0 && value.artifactSubRollScalar > 0.0) { "Artifact roll scalars must be positive" }
    }

    private fun defaultProfile(): CombatBalanceProfile {
        fun family(health: Double, armorBase: Double, armorGrowth: Double, toughnessBase: Double, toughnessGrowth: Double, damage: Double, phaseHealth: Double, phaseArmor: Double, phaseDamage: Double, overflowArmor: Double, overflowToughness: Double) =
            CombatFamilyBalance(health, phaseHealth, armorBase, armorGrowth, phaseArmor, toughnessBase, toughnessGrowth, phaseArmor * 0.25, damage, phaseDamage, overflowArmor, overflowToughness)

        return CombatBalanceProfile(
            version = 1,
            caps = CombatCaps(75.0, -50.0, 75.0, 1.50, 2.50),
            survivor = family(1.65, 0.8, 2.4, 0.15, 0.70, 0.48, 0.08, 0.65, 0.03, 0.10, 0.025),
            assault = family(1.85, 0.9, 2.6, 0.15, 0.70, 0.58, 0.08, 0.65, 0.03, 0.09, 0.022),
            arcane = family(1.55, 0.7, 2.2, 0.25, 0.80, 0.63, 0.08, 0.55, 0.03, 0.08, 0.020),
            elite = family(2.15, 1.3, 3.6, 0.5, 1.10, 0.74, 0.12, 0.90, 0.035, 0.12, 0.032),
            relic = family(0.08, 4.0, 3.2, 2.0, 1.30, 0.90, 0.0, 0.0, 0.0, 0.18, 0.055),
            domain = CombatDomainBalance(0.88, 0.36, 0.28, 0.48, 0.15, 0.15, 0.23, 0.10, 0.12, 0.18, 0.16, 0.13, 0.16),
            field = CombatFieldBalance(1.22, 1.12, 1.08, 1.12, 0.16, 1.75, 1.25, 1.18, 1.24, 0.30),
            regen = CombatRegenBalance(0.45, 1.60),
            tuning = CombatTuning(0.035, 0.018, 0.82, 0.86, 0.78, 0.72, 0.92, 1.10, 0.12, 5.0, 8.0)
        )
    }
}
