package hifumi.cresora.credits
import hifumi.cresora.CreSoraUtilities
import net.minecraft.advancement.AdvancementEntry
import net.minecraft.entity.mob.MobEntity
import net.minecraft.entity.mob.HostileEntity
import net.minecraft.server.network.ServerPlayerEntity
import org.slf4j.LoggerFactory

object CreditsService {
    private const val PLAYER_CREDITS_KEY = "cresora_credits"
    private const val LARGE_CHANGE_LOG_THRESHOLD = 10_000
    private val logger = LoggerFactory.getLogger("${CreSoraUtilities.MOD_ID}/credits")

    fun playerCreditsKey(): String = PLAYER_CREDITS_KEY

    fun getCredits(player: ServerPlayerEntity): Int {
        val access = player as? CreditsAccess ?: return 0
        val normalized = access.cresoraGetCredits().coerceAtLeast(0)
        if (normalized != access.cresoraGetCredits()) {
            access.cresoraSetCredits(normalized)
        }
        return normalized
    }

    fun hasCredits(player: ServerPlayerEntity, amount: Int): Boolean {
        return getCredits(player) >= amount.coerceAtLeast(0)
    }

    fun addCredits(player: ServerPlayerEntity, amount: Int): Int {
        if (amount <= 0) {
            return getCredits(player)
        }
        val access = player as? CreditsAccess ?: return 0
        val updated = (getCredits(player).toLong() + amount.toLong()).coerceAtMost(Int.MAX_VALUE.toLong()).toInt()
        access.cresoraSetCredits(updated)
        logLargeChange(player, amount, updated, "add")
        return updated
    }

    fun spendCredits(player: ServerPlayerEntity, amount: Int): Boolean {
        val normalizedAmount = amount.coerceAtLeast(0)
        if (normalizedAmount == 0) {
            return true
        }
        val access = player as? CreditsAccess ?: return false
        val current = getCredits(player)
        if (current < normalizedAmount) {
            return false
        }
        access.cresoraSetCredits(current - normalizedAmount)
        logLargeChange(player, normalizedAmount, current - normalizedAmount, "spend")
        return true
    }

    fun setCredits(player: ServerPlayerEntity, amount: Int): Int {
        val access = player as? CreditsAccess ?: return 0
        val normalized = amount.coerceAtLeast(0)
        access.cresoraSetCredits(normalized)
        return normalized
    }

    fun copyTo(oldPlayer: ServerPlayerEntity, newPlayer: ServerPlayerEntity) {
        val oldAccess = oldPlayer as? CreditsAccess ?: return
        val newAccess = newPlayer as? CreditsAccess ?: return
        newAccess.cresoraSetCredits(oldAccess.cresoraGetCredits().coerceAtLeast(0))
    }

    fun addPickupReward(player: ServerPlayerEntity, source: CreditsRewardSource, count: Int = 1): Int {
        return addCredits(player, CreditsRewardProfile.pickupReward(source, count))
    }

    fun addHostileKillReward(player: ServerPlayerEntity, entity: HostileEntity): Int {
        return addCredits(player, CreditsRewardProfile.hostileKill(entity))
    }

    fun addFriendlyKillReward(player: ServerPlayerEntity, entity: MobEntity): Int {
        return addCredits(player, CreditsRewardProfile.friendlyKill(entity))
    }

    fun addAdvancementReward(player: ServerPlayerEntity, advancement: AdvancementEntry): Int {
        return addCredits(player, CreditsRewardProfile.advancementReward(advancement))
    }

    fun addExperienceReward(player: ServerPlayerEntity, amount: Int): Int {
        return addPickupReward(player, CreditsRewardSource.EXPERIENCE_GAIN, amount)
    }

    private fun logLargeChange(player: ServerPlayerEntity, amount: Int, totalAfter: Int, kind: String) {
        if (amount < LARGE_CHANGE_LOG_THRESHOLD) {
            return
        }
        logger.info(
            "Large CSC {} detected: player='{}' delta={} totalAfter={} source={}",
            kind,
            player.gameProfile.name,
            amount,
            totalAfter,
            detectCaller()
        )
    }

    private fun detectCaller(): String {
        val ignoredClasses = setOf(
            CreditsService::class.java.name,
            Thread::class.java.name
        )
        return Throwable().stackTrace
            .firstOrNull { frame ->
                frame.className !in ignoredClasses &&
                    !frame.className.startsWith("java.lang.reflect.") &&
                    !frame.className.startsWith("jdk.internal.reflect.")
            }
            ?.let { "${it.className}#${it.methodName}:${it.lineNumber}" }
            ?: "unknown"
    }
}
