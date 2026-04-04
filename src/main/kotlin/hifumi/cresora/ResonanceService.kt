package hifumi.cresora

import net.minecraft.item.ItemStack
import net.minecraft.server.network.ServerPlayerEntity
import kotlin.math.max

object ResonanceService {
    private const val CHORD_PROGRESSION_KEY = "cresora_resonance_chord_progression"
    private const val SUBSTITUTE_CHORD_KEY = "cresora_resonance_substitute_chord"
    private const val LIMITED_PITY_KEY = "cresora_resonance_limited_pity"
    private const val STANDARD_PULLS_KEY = "cresora_resonance_standard_pulls"
    private const val DEEP_PITY_STREAK_KEY = "cresora_resonance_deep_pity_streak"
    private const val ARPEGGIO_READY_KEY = "cresora_resonance_arpeggio_ready"

    private const val LIMITED_SOFT_PITY_START = 100
    private const val LIMITED_HARD_PITY = 150
    private const val ARPEGGIO_GUARANTEE_PULL = 100

    data class Progress(
        val limitedPityPulls: Int,
        val standardPulls: Int,
        val deepPityStreak: Int,
        val arpeggioReady: Boolean
    )

    data class PullResult(
        val banner: ResonanceBannerDefinition,
        val pulledWeapon: ItemStack,
        val rarity: WeaponRarity,
        val wasLimitedFiveStar: Boolean,
        val progressAfter: Progress
    )

    fun chordProgressionKey(): String = CHORD_PROGRESSION_KEY

    fun substituteChordKey(): String = SUBSTITUTE_CHORD_KEY

    fun limitedPityKey(): String = LIMITED_PITY_KEY

    fun standardPullsKey(): String = STANDARD_PULLS_KEY

    fun deepPityStreakKey(): String = DEEP_PITY_STREAK_KEY

    fun arpeggioReadyKey(): String = ARPEGGIO_READY_KEY

    fun getProgress(player: ServerPlayerEntity): Progress {
        val access = player as? ResonanceAccess ?: return Progress(0, 0, 0, false)
        return Progress(
            limitedPityPulls = max(0, access.cresoraGetLimitedPityPulls()),
            standardPulls = max(0, access.cresoraGetStandardPulls()),
            deepPityStreak = max(0, access.cresoraGetDeepPityStreak()),
            arpeggioReady = access.cresoraGetArpeggioReady()
        )
    }

    fun getCurrency(player: ServerPlayerEntity, type: ResonanceCurrencyType): Int {
        val access = player as? ResonanceAccess ?: return 0
        val current = when (type) {
            ResonanceCurrencyType.CHORD_PROGRESSION -> access.cresoraGetChordProgression()
            ResonanceCurrencyType.SUBSTITUTE_CHORD -> access.cresoraGetSubstituteChord()
        }.coerceAtLeast(0)
        when (type) {
            ResonanceCurrencyType.CHORD_PROGRESSION -> if (current != access.cresoraGetChordProgression()) access.cresoraSetChordProgression(current)
            ResonanceCurrencyType.SUBSTITUTE_CHORD -> if (current != access.cresoraGetSubstituteChord()) access.cresoraSetSubstituteChord(current)
        }
        return current
    }

    fun addCurrency(player: ServerPlayerEntity, type: ResonanceCurrencyType, amount: Int): Int {
        if (amount <= 0) {
            return getCurrency(player, type)
        }
        val access = player as? ResonanceAccess ?: return 0
        val updated = (getCurrency(player, type).toLong() + amount.toLong()).coerceAtMost(Int.MAX_VALUE.toLong()).toInt()
        when (type) {
            ResonanceCurrencyType.CHORD_PROGRESSION -> access.cresoraSetChordProgression(updated)
            ResonanceCurrencyType.SUBSTITUTE_CHORD -> access.cresoraSetSubstituteChord(updated)
        }
        return updated
    }

    fun setCurrency(player: ServerPlayerEntity, type: ResonanceCurrencyType, amount: Int): Int {
        val access = player as? ResonanceAccess ?: return 0
        val normalized = amount.coerceAtLeast(0)
        when (type) {
            ResonanceCurrencyType.CHORD_PROGRESSION -> access.cresoraSetChordProgression(normalized)
            ResonanceCurrencyType.SUBSTITUTE_CHORD -> access.cresoraSetSubstituteChord(normalized)
        }
        return normalized
    }

    fun spendCurrency(player: ServerPlayerEntity, type: ResonanceCurrencyType, amount: Int): Boolean {
        val normalizedAmount = amount.coerceAtLeast(0)
        if (normalizedAmount == 0) {
            return true
        }
        val access = player as? ResonanceAccess ?: return false
        val current = getCurrency(player, type)
        if (current < normalizedAmount) {
            return false
        }
        when (type) {
            ResonanceCurrencyType.CHORD_PROGRESSION -> access.cresoraSetChordProgression(current - normalizedAmount)
            ResonanceCurrencyType.SUBSTITUTE_CHORD -> access.cresoraSetSubstituteChord(current - normalizedAmount)
        }
        return true
    }

    fun copyTo(oldPlayer: ServerPlayerEntity, newPlayer: ServerPlayerEntity) {
        val oldAccess = oldPlayer as? ResonanceAccess ?: return
        val newAccess = newPlayer as? ResonanceAccess ?: return
        newAccess.cresoraSetChordProgression(max(0, oldAccess.cresoraGetChordProgression()))
        newAccess.cresoraSetSubstituteChord(max(0, oldAccess.cresoraGetSubstituteChord()))
        newAccess.cresoraSetLimitedPityPulls(max(0, oldAccess.cresoraGetLimitedPityPulls()))
        newAccess.cresoraSetStandardPulls(max(0, oldAccess.cresoraGetStandardPulls()))
        newAccess.cresoraSetDeepPityStreak(max(0, oldAccess.cresoraGetDeepPityStreak()))
        newAccess.cresoraSetArpeggioReady(oldAccess.cresoraGetArpeggioReady())
    }

    fun currencyCount(player: ServerPlayerEntity, banner: ResonanceBannerDefinition): Int {
        return getCurrency(player, banner.currencyType())
    }

    fun canPull(player: ServerPlayerEntity, banner: ResonanceBannerDefinition): Boolean {
        return currencyCount(player, banner) >= banner.cost
    }

    fun pull(player: ServerPlayerEntity, banner: ResonanceBannerDefinition): PullResult? {
        if (!spendCurrency(player, banner.currencyType(), banner.cost)) {
            return null
        }

        val before = getProgress(player)
        val rarity = rollRarity(player, banner, before)
        val entry = when (rarity) {
            WeaponRarity.FIVE_STAR -> chooseFiveStarEntry(player, banner)
            WeaponRarity.FOUR_STAR -> chooseEntry(player, banner.fourStarPool)
            WeaponRarity.THREE_STAR -> chooseEntry(player, banner.threeStarPool)
            WeaponRarity.TWO_STAR -> chooseEntry(player, banner.twoStarPool)
        }
        val definition = WeaponContentRegistry.requireWeapon(entry.weaponId)
        val pulledWeapon = WeaponStackSupport.createWeaponStack(definition, entry.rarity, 1, 1)
        player.giveItemStack(pulledWeapon.copy())

        val updated = updateProgress(player, banner, rarity)
        return PullResult(
            banner = banner,
            pulledWeapon = pulledWeapon,
            rarity = rarity,
            wasLimitedFiveStar = banner.type == ResonanceBannerType.LIMITED && rarity == WeaponRarity.FIVE_STAR,
            progressAfter = updated
        )
    }

    private fun chooseFiveStarEntry(player: ServerPlayerEntity, banner: ResonanceBannerDefinition): ResonanceWeaponEntry {
        if (banner.type == ResonanceBannerType.LIMITED) {
            val featured = banner.featuredFiveStarWeaponId
            if (featured != null) {
                return banner.fiveStarPool.firstOrNull { it.weaponId == featured }
                    ?: ResonanceWeaponEntry(featured, WeaponRarity.FIVE_STAR, 1.0)
            }
        }
        return chooseEntry(player, banner.fiveStarPool)
    }

    private fun chooseEntry(player: ServerPlayerEntity, entries: List<ResonanceWeaponEntry>): ResonanceWeaponEntry {
        val totalWeight = entries.sumOf { it.weight.coerceAtLeast(0.0) }.coerceAtLeast(0.0)
        if (entries.isEmpty()) {
            error("Cannot choose from an empty resonance pool")
        }
        if (totalWeight <= 0.0) {
            return entries.first()
        }
        var roll = player.random.nextDouble() * totalWeight
        for (entry in entries) {
            roll -= entry.weight.coerceAtLeast(0.0)
            if (roll <= 0.0) {
                return entry
            }
        }
        return entries.last()
    }

    private fun rollRarity(player: ServerPlayerEntity, banner: ResonanceBannerDefinition, progress: Progress): WeaponRarity {
        val rates = banner.rates
        val fiveStarChance = when (banner.type) {
            ResonanceBannerType.LIMITED -> limitedFiveStarChance(progress)
            ResonanceBannerType.STANDARD -> rates.fiveStarChance
        }
        val roll = player.random.nextDouble()
        var cursor = fiveStarChance
        if (roll < cursor) {
            return WeaponRarity.FIVE_STAR
        }
        cursor += rates.fourStarChance
        if (roll < cursor) {
            return WeaponRarity.FOUR_STAR
        }
        cursor += rates.threeStarChance
        if (roll < cursor) {
            return WeaponRarity.THREE_STAR
        }
        return WeaponRarity.TWO_STAR
    }

    private fun limitedFiveStarChance(progress: Progress): Double {
        val nextPull = progress.limitedPityPulls + 1
        if (progress.arpeggioReady) {
            return if (nextPull >= ARPEGGIO_GUARANTEE_PULL) 1.0 else 0.01
        }
        if (nextPull >= LIMITED_HARD_PITY) {
            return 1.0
        }
        if (nextPull < LIMITED_SOFT_PITY_START) {
            return 0.01
        }
        val spanProgress = (nextPull - LIMITED_SOFT_PITY_START).toDouble() / (LIMITED_HARD_PITY - LIMITED_SOFT_PITY_START).toDouble()
        return 0.01 + spanProgress.coerceIn(0.0, 1.0) * 0.99
    }

    private fun updateProgress(player: ServerPlayerEntity, banner: ResonanceBannerDefinition, rarity: WeaponRarity): Progress {
        val access = player as? ResonanceAccess ?: return getProgress(player)
        val before = getProgress(player)
        return when (banner.type) {
            ResonanceBannerType.STANDARD -> {
                access.cresoraSetStandardPulls(before.standardPulls + 1)
                Progress(before.limitedPityPulls, before.standardPulls + 1, before.deepPityStreak, before.arpeggioReady)
            }

            ResonanceBannerType.LIMITED -> {
                if (rarity == WeaponRarity.FIVE_STAR) {
                    if (before.arpeggioReady) {
                        access.cresoraSetArpeggioReady(false)
                        access.cresoraSetDeepPityStreak(0)
                    } else {
                        val newStreak = if (before.limitedPityPulls + 1 >= LIMITED_SOFT_PITY_START) before.deepPityStreak + 1 else 0
                        access.cresoraSetDeepPityStreak(newStreak)
                        access.cresoraSetArpeggioReady(newStreak >= 3)
                    }
                    access.cresoraSetLimitedPityPulls(0)
                } else {
                    access.cresoraSetLimitedPityPulls(before.limitedPityPulls + 1)
                }
                Progress(
                    limitedPityPulls = if (rarity == WeaponRarity.FIVE_STAR) 0 else before.limitedPityPulls + 1,
                    standardPulls = before.standardPulls,
                    deepPityStreak = if (rarity == WeaponRarity.FIVE_STAR) {
                        if (before.arpeggioReady) 0 else {
                            if (before.limitedPityPulls + 1 >= LIMITED_SOFT_PITY_START) before.deepPityStreak + 1 else 0
                        }
                    } else {
                        before.deepPityStreak
                    },
                    arpeggioReady = if (rarity == WeaponRarity.FIVE_STAR) {
                        if (before.arpeggioReady) {
                            false
                        } else {
                            before.limitedPityPulls + 1 >= LIMITED_SOFT_PITY_START && before.deepPityStreak + 1 >= 3
                        }
                    } else {
                        before.arpeggioReady
                    }
                )
            }
        }
    }
}
