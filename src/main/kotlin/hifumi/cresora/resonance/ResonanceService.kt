package hifumi.cresora.resonance
import hifumi.cresora.weapon.WeaponContentRegistry
import hifumi.cresora.weapon.WeaponRarity
import hifumi.cresora.weapon.WeaponStackSupport
import net.minecraft.item.ItemStack
import net.minecraft.server.network.ServerPlayerEntity
import kotlin.math.max

object ResonanceService {
    private const val CHORD_PROGRESSION_KEY = "cresora_resonance_chord_progression"
    private const val SUBSTITUTE_CHORD_KEY = "cresora_resonance_substitute_chord"
    private const val LIMITED_PITY_KEY = "cresora_resonance_limited_pity"
    private const val STANDARD_PULLS_KEY = "cresora_resonance_standard_pulls"
    private const val LIMITED_FOUR_STAR_PULLS_KEY = "cresora_resonance_limited_four_star_pulls"
    private const val STANDARD_FOUR_STAR_PULLS_KEY = "cresora_resonance_standard_four_star_pulls"
    private const val DEEP_PITY_STREAK_KEY = "cresora_resonance_deep_pity_streak"
    private const val LIMITED_FIVE_STAR_GUARANTEED_KEY = "cresora_resonance_limited_five_star_guaranteed"
    private const val ARPEGGIO_READY_KEY = "cresora_resonance_arpeggio_ready"

    private const val LIMITED_SOFT_PITY_START = 100
    private const val LIMITED_HARD_PITY = 150
    private const val ARPEGGIO_GUARANTEE_PULL = 100

    data class Progress(
        val limitedPityPulls: Int,
        val standardPulls: Int,
        val limitedFourStarPulls: Int,
        val standardFourStarPulls: Int,
        val deepPityStreak: Int,
        val limitedFiveStarGuaranteed: Boolean,
        val arpeggioReady: Boolean
    )

    data class PullResult(
        val banner: ResonanceBannerDefinition,
        val pulledWeapon: ItemStack,
        val rarity: WeaponRarity,
        val wasLimitedFiveStar: Boolean,
        val obtainedFeaturedFiveStar: Boolean,
        val progressAfter: Progress
    )

    fun chordProgressionKey(): String = CHORD_PROGRESSION_KEY

    fun substituteChordKey(): String = SUBSTITUTE_CHORD_KEY

    fun limitedPityKey(): String = LIMITED_PITY_KEY

    fun standardPullsKey(): String = STANDARD_PULLS_KEY

    fun limitedFourStarPullsKey(): String = LIMITED_FOUR_STAR_PULLS_KEY

    fun standardFourStarPullsKey(): String = STANDARD_FOUR_STAR_PULLS_KEY

    fun deepPityStreakKey(): String = DEEP_PITY_STREAK_KEY

    fun limitedFiveStarGuaranteedKey(): String = LIMITED_FIVE_STAR_GUARANTEED_KEY

    fun arpeggioReadyKey(): String = ARPEGGIO_READY_KEY

    fun getProgress(player: ServerPlayerEntity): Progress {
        val access = player as? ResonanceAccess ?: return Progress(0, 0, 0, 0, 0, false, false)
        return Progress(
            limitedPityPulls = max(0, access.cresoraGetLimitedPityPulls()),
            standardPulls = max(0, access.cresoraGetStandardPulls()),
            limitedFourStarPulls = max(0, access.cresoraGetLimitedFourStarPulls()),
            standardFourStarPulls = max(0, access.cresoraGetStandardFourStarPulls()),
            deepPityStreak = max(0, access.cresoraGetDeepPityStreak()),
            limitedFiveStarGuaranteed = access.cresoraIsLimitedFiveStarGuaranteed(),
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
        newAccess.cresoraSetLimitedFourStarPulls(max(0, oldAccess.cresoraGetLimitedFourStarPulls()))
        newAccess.cresoraSetStandardFourStarPulls(max(0, oldAccess.cresoraGetStandardFourStarPulls()))
        newAccess.cresoraSetDeepPityStreak(max(0, oldAccess.cresoraGetDeepPityStreak()))
        newAccess.cresoraSetLimitedFiveStarGuaranteed(oldAccess.cresoraIsLimitedFiveStarGuaranteed())
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
        var rarity = rollRarity(player, banner, before)
        
        val fourStarPulls = if (banner.type == ResonanceBannerType.LIMITED) before.limitedFourStarPulls else before.standardFourStarPulls
        if (fourStarPulls >= 9 && (rarity == WeaponRarity.TWO_STAR || rarity == WeaponRarity.THREE_STAR)) {
            rarity = WeaponRarity.FOUR_STAR
        }

        var obtainedFeatured = false
        val entry = when (rarity) {
            WeaponRarity.FIVE_STAR -> {
                val (selected, featured, _) = chooseFiveStarEntry(player, banner, before)
                obtainedFeatured = featured
                selected
            }
            WeaponRarity.FOUR_STAR -> chooseEntry(player, banner.fourStarPool)
            WeaponRarity.THREE_STAR -> chooseEntry(player, banner.threeStarPool)
            WeaponRarity.TWO_STAR -> chooseEntry(player, banner.twoStarPool)
        }
        val definition = WeaponContentRegistry.requireWeapon(entry.weaponId)
        val pulledWeapon = WeaponStackSupport.createWeaponStack(definition, entry.rarity, 1, 1)
        player.giveItemStack(pulledWeapon.copy())

        val updated = updateProgress(player, banner, rarity, obtainedFeatured)
        return PullResult(
            banner = banner,
            pulledWeapon = pulledWeapon,
            rarity = rarity,
            wasLimitedFiveStar = banner.type == ResonanceBannerType.LIMITED && rarity == WeaponRarity.FIVE_STAR,
            obtainedFeaturedFiveStar = obtainedFeatured,
            progressAfter = updated
        )
    }

    private fun chooseFiveStarEntry(
        player: ServerPlayerEntity,
        banner: ResonanceBannerDefinition,
        progress: Progress
    ): Triple<ResonanceWeaponEntry, Boolean, Boolean> {
        if (banner.type == ResonanceBannerType.LIMITED) {
            val featuredId = banner.featuredFiveStarWeaponId
            if (featuredId != null) {
                // 確定枠または 50% の抽選
                val isGuaranteed = progress.limitedFiveStarGuaranteed
                val wonFiftyFifty = player.random.nextDouble() < 0.5
                
                if (isGuaranteed || wonFiftyFifty) {
                    val entry = banner.fiveStarPool.firstOrNull { it.weaponId == featuredId }
                        ?: ResonanceWeaponEntry(featuredId, WeaponRarity.FIVE_STAR, 1.0)
                    return Triple(entry, true, isGuaranteed)
                } else {
                    // すり抜け：恒常プールから選択（ただし、恒常バナーの★5プールを借りる、または限定バナーの★5プールから選ぶ）
                    // 今の実装では限定バナーの fiveStarPool にも他のが入っている前提
                    val otherPool = banner.fiveStarPool.filter { it.weaponId != featuredId }
                    return if (otherPool.isNotEmpty()) {
                        Triple(chooseEntry(player, otherPool), false, false)
                    } else {
                        // 恒常プールが空なら仕方なくピックアップ
                        val entry = banner.fiveStarPool.firstOrNull { it.weaponId == featuredId }
                            ?: ResonanceWeaponEntry(featuredId, WeaponRarity.FIVE_STAR, 1.0)
                        Triple(entry, true, isGuaranteed)
                    }
                }
            }
        }
        return Triple(chooseEntry(player, banner.fiveStarPool), false, false)
    }

    private fun chooseEntry(player: ServerPlayerEntity, entries: List<ResonanceWeaponEntry>): ResonanceWeaponEntry {
        val totalWeight = entries.sumOf { it.weight.coerceAtLeast(0.0) }.coerceAtLeast(0.0)
        val fallback = entries.first()
        if (totalWeight <= 0.0) {
            return fallback
        }
        var roll = player.random.nextDouble() * totalWeight
        for (entry in entries) {
            roll -= entry.weight.coerceAtLeast(0.0)
            if (roll <= 0.0) {
                return entry
            }
        }
        return entries.lastOrNull() ?: fallback
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
            return if (nextPull >= ARPEGGIO_GUARANTEE_PULL) 1.0 else 0.003
        }
        if (nextPull >= LIMITED_HARD_PITY) {
            return 1.0
        }
        if (nextPull < LIMITED_SOFT_PITY_START) {
            return 0.003
        }
        val spanProgress = (nextPull - LIMITED_SOFT_PITY_START).toDouble() / (LIMITED_HARD_PITY - LIMITED_SOFT_PITY_START).toDouble()
        return 0.003 + spanProgress.coerceIn(0.0, 1.0) * 0.997
    }

    private fun updateProgress(
        player: ServerPlayerEntity,
        banner: ResonanceBannerDefinition,
        rarity: WeaponRarity,
        obtainedFeaturedFiveStar: Boolean = false
    ): Progress {
        val access = player as? ResonanceAccess ?: return getProgress(player)
        val before = getProgress(player)
        val isFourStarOrHigher = rarity == WeaponRarity.FOUR_STAR || rarity == WeaponRarity.FIVE_STAR

        return when (banner.type) {
            ResonanceBannerType.STANDARD -> {
                access.cresoraSetStandardPulls(before.standardPulls + 1)
                val nextFourStarPulls = if (isFourStarOrHigher) 0 else before.standardFourStarPulls + 1
                access.cresoraSetStandardFourStarPulls(nextFourStarPulls)
                Progress(
                    before.limitedPityPulls,
                    before.standardPulls + 1,
                    before.limitedFourStarPulls,
                    nextFourStarPulls,
                    before.deepPityStreak,
                    before.limitedFiveStarGuaranteed,
                    before.arpeggioReady
                )
            }

            ResonanceBannerType.LIMITED -> {
                val nextFourStarPulls = if (isFourStarOrHigher) 0 else before.limitedFourStarPulls + 1
                access.cresoraSetLimitedFourStarPulls(nextFourStarPulls)

                val newLimitedPity = if (rarity == WeaponRarity.FIVE_STAR) 0 else before.limitedPityPulls + 1
                access.cresoraSetLimitedPityPulls(newLimitedPity)

                var nextGuaranteed = before.limitedFiveStarGuaranteed
                if (rarity == WeaponRarity.FIVE_STAR) {
                    nextGuaranteed = !obtainedFeaturedFiveStar
                    access.cresoraSetLimitedFiveStarGuaranteed(nextGuaranteed)
                    
                    if (before.arpeggioReady) {
                        access.cresoraSetArpeggioReady(false)
                        access.cresoraSetDeepPityStreak(0)
                    } else {
                        val newStreak = if (before.limitedPityPulls + 1 >= LIMITED_SOFT_PITY_START) before.deepPityStreak + 1 else 0
                        access.cresoraSetDeepPityStreak(newStreak)
                        access.cresoraSetArpeggioReady(newStreak >= 3)
                    }
                }
                Progress(
                    limitedPityPulls = newLimitedPity,
                    standardPulls = before.standardPulls,
                    limitedFourStarPulls = nextFourStarPulls,
                    standardFourStarPulls = before.standardFourStarPulls,
                    deepPityStreak = if (rarity == WeaponRarity.FIVE_STAR) {
                        if (before.arpeggioReady) 0 else {
                            if (before.limitedPityPulls + 1 >= LIMITED_SOFT_PITY_START) before.deepPityStreak + 1 else 0
                        }
                    } else {
                        before.deepPityStreak
                    },
                    limitedFiveStarGuaranteed = nextGuaranteed,
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
