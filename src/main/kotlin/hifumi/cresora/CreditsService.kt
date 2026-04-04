package hifumi.cresora

import net.minecraft.advancement.AdvancementEntry
import net.minecraft.entity.mob.MobEntity
import net.minecraft.entity.mob.HostileEntity
import net.minecraft.server.network.ServerPlayerEntity

object CreditsService {
    private const val PLAYER_CREDITS_KEY = "cresora_credits"

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
}
