package hifumi.cresora.credits
import hifumi.cresora.adventurerank.AdventureRankService
import hifumi.cresora.combat.HostileRewardFamilies
import hifumi.cresora.combat.HostileRewardFamily
import net.minecraft.advancement.AdvancementEntry
import net.minecraft.advancement.AdvancementFrame
import net.minecraft.entity.mob.HostileEntity
import net.minecraft.entity.mob.MobEntity
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

    private const val FRIENDLY_KILL_BASE = 20
    private const val FRIENDLY_KILL_HEALTH_FACTOR = 4.0
    private const val FRIENDLY_KILL_MIN = 60
    private const val FRIENDLY_KILL_MAX = 600
    private const val ADVANCEMENT_TASK_REWARD = 3_000
    private const val ADVANCEMENT_GOAL_REWARD = 12_000
    private const val ADVANCEMENT_CHALLENGE_REWARD = 50_000
    private const val EXPERIENCE_REWARD_PER_POINT = 6

    private const val ARTIFACT_PICKUP_REWARD = 350
    private const val UPGRADE_MATERIAL_PICKUP_REWARD = 120

    fun hostileKill(entity: MobEntity): Int {
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
            CreditsRewardSource.EXPERIENCE_GAIN -> EXPERIENCE_REWARD_PER_POINT
            CreditsRewardSource.ARTIFACT_OBTAIN -> ARTIFACT_PICKUP_REWARD
            CreditsRewardSource.UPGRADE_MATERIAL_OBTAIN -> UPGRADE_MATERIAL_PICKUP_REWARD
            CreditsRewardSource.HOSTILE_KILL,
            CreditsRewardSource.FRIENDLY_KILL,
            CreditsRewardSource.ADVANCEMENT_COMPLETE -> 0
        }
        return (perUnit * normalizedCount.toDouble()).roundToInt().coerceAtLeast(0)
    }

    fun friendlyKill(entity: MobEntity): Int {
        val reward = FRIENDLY_KILL_BASE + entity.maxHealth.toDouble().coerceAtLeast(1.0) * FRIENDLY_KILL_HEALTH_FACTOR
        return reward.roundToInt().coerceIn(FRIENDLY_KILL_MIN, FRIENDLY_KILL_MAX)
    }

    fun advancementReward(advancement: AdvancementEntry): Int {
        val display = advancement.value().display().orElse(null)
        val frame = display?.frame ?: AdvancementFrame.TASK
        return when (frame) {
            AdvancementFrame.TASK -> ADVANCEMENT_TASK_REWARD
            AdvancementFrame.GOAL -> ADVANCEMENT_GOAL_REWARD
            AdvancementFrame.CHALLENGE -> ADVANCEMENT_CHALLENGE_REWARD
        }
    }
}
