package hifumi.cresora

import net.minecraft.entity.mob.HostileEntity
import kotlin.math.roundToInt

object CreditsRewardProfile {
    private const val SURVIVOR_BASE = 120
    private const val SURVIVOR_PER_LEVEL = 18
    private const val ASSAULT_BASE = 180
    private const val ASSAULT_PER_LEVEL = 24
    private const val ARCANE_BASE = 240
    private const val ARCANE_PER_LEVEL = 28
    private const val ELITE_BASE = 420
    private const val ELITE_PER_LEVEL = 38
    private const val WARDEN_BASE = 2500
    private const val WARDEN_PER_LEVEL = 90

    private const val ARTIFACT_PICKUP_REWARD = 350
    private const val UPGRADE_MATERIAL_PICKUP_REWARD = 120

    fun hostileKill(entity: HostileEntity): Int {
        val level = AdventureRankService.mobLevel(entity).coerceAtLeast(1)
        val reward = when (HostileRewardFamilies.classify(entity.type)) {
            HostileRewardFamily.SURVIVOR -> SURVIVOR_BASE + level * SURVIVOR_PER_LEVEL
            HostileRewardFamily.ASSAULT -> ASSAULT_BASE + level * ASSAULT_PER_LEVEL
            HostileRewardFamily.ARCANE -> ARCANE_BASE + level * ARCANE_PER_LEVEL
            HostileRewardFamily.ELITE -> ELITE_BASE + level * ELITE_PER_LEVEL
            HostileRewardFamily.RELIC -> WARDEN_BASE + level * WARDEN_PER_LEVEL
        }
        return reward.coerceAtLeast(0)
    }

    fun pickupReward(source: CreditsRewardSource, count: Int): Int {
        val normalizedCount = count.coerceAtLeast(0)
        val perUnit = when (source) {
            CreditsRewardSource.ARTIFACT_OBTAIN -> ARTIFACT_PICKUP_REWARD
            CreditsRewardSource.UPGRADE_MATERIAL_OBTAIN -> UPGRADE_MATERIAL_PICKUP_REWARD
            CreditsRewardSource.HOSTILE_KILL -> 0
        }
        return (perUnit * normalizedCount.toDouble()).roundToInt().coerceAtLeast(0)
    }
}
