package hifumi.cresora.resonance
import hifumi.cresora.weapon.WeaponContentRegistry
import hifumi.cresora.weapon.WeaponRarity
import hifumi.cresora.weapon.WeaponStackSupport
import hifumi.cresora.weapon.WeaponSkillService
import net.minecraft.item.ItemStack
import net.minecraft.server.network.ServerPlayerEntity
import kotlin.math.max

object ResonanceService {
    private const val CHORD_PROGRESSION_KEY = "cresora_resonance_chord_progression"
    private const val SUBSTITUTE_CHORD_KEY = "cresora_resonance_substitute_chord"
    private const val DAILY_LOGIN_EPOCH_DAY_KEY = "cresora_daily_login_epoch_day"
    private const val LIMITED_PITY_KEY = "cresora_resonance_limited_pity"
    private const val STANDARD_PULLS_KEY = "cresora_resonance_standard_pulls"
    private const val LIMITED_FOUR_STAR_PULLS_KEY = "cresora_resonance_limited_four_star_pulls"
    private const val STANDARD_FOUR_STAR_PULLS_KEY = "cresora_resonance_standard_four_star_pulls"
    private const val DEEP_PITY_STREAK_KEY = "cresora_resonance_deep_pity_streak"
    private const val LIMITED_FIVE_STAR_GUARANTEED_KEY = "cresora_resonance_limited_five_star_guaranteed"
    private const val ARPEGGIO_READY_KEY = "cresora_resonance_arpeggio_ready"
    private const val SELECTED_FEATURED_WEAPON_ID_KEY = "cresora_resonance_selected_featured_weapon_id"
    private const val SPIRIT_BOND_POINTS_KEY = "cresora_spirit_bond_points"

    private const val LIMITED_SOFT_PITY_START = 100
    private const val LIMITED_HARD_PITY = 150
    private const val ARPEGGIO_GUARANTEE_PULL = 100
    private val SPIRIT_BOND_STAGE_THRESHOLDS = intArrayOf(0, 40, 100, 200, 350, 550)

    data class Progress(
        val limitedPityPulls: Int,
        val standardPulls: Int,
        val limitedFourStarPulls: Int,
        val standardFourStarPulls: Int,
        val deepPityStreak: Int,
        val limitedFiveStarGuaranteed: Boolean,
        val arpeggioReady: Boolean,
        val selectedFeaturedWeaponId: String?
    )

    data class PullResult(
        val banner: ResonanceBannerDefinition,
        val pulledWeapon: ItemStack,
        val rarity: WeaponRarity,
        val wasLimitedFiveStar: Boolean,
        val obtainedFeaturedFiveStar: Boolean,
        val duplicateConverted: Boolean,
        val bondPointsAwarded: Int,
        val bondPointsAfter: Int,
        val progressAfter: Progress
    )

    sealed class PullOutcome {
        data class Success(val result: PullResult) : PullOutcome()
        data object NotEnoughCurrency : PullOutcome()
        data object FeaturedNotSelected : PullOutcome()
        data object NoFiveStarWeaponsAvailable : PullOutcome()
    }

    fun chordProgressionKey(): String = CHORD_PROGRESSION_KEY

    fun substituteChordKey(): String = SUBSTITUTE_CHORD_KEY

    fun limitedPityKey(): String = LIMITED_PITY_KEY

    fun standardPullsKey(): String = STANDARD_PULLS_KEY

    fun limitedFourStarPullsKey(): String = LIMITED_FOUR_STAR_PULLS_KEY

    fun standardFourStarPullsKey(): String = STANDARD_FOUR_STAR_PULLS_KEY

    fun deepPityStreakKey(): String = DEEP_PITY_STREAK_KEY

    fun limitedFiveStarGuaranteedKey(): String = LIMITED_FIVE_STAR_GUARANTEED_KEY

    fun arpeggioReadyKey(): String = ARPEGGIO_READY_KEY

    fun dailyLoginEpochDayKey(): String = DAILY_LOGIN_EPOCH_DAY_KEY

    fun selectedFeaturedWeaponIdKey(): String = SELECTED_FEATURED_WEAPON_ID_KEY

    fun spiritBondPointsKey(): String = SPIRIT_BOND_POINTS_KEY

    fun getLastDailyLoginEpochDay(player: ServerPlayerEntity): Long {
        return (player as? ResonanceAccess)?.cresoraGetLastDailyLoginEpochDay() ?: -1L
    }

    fun setLastDailyLoginEpochDay(player: ServerPlayerEntity, day: Long) {
        (player as? ResonanceAccess)?.cresoraSetLastDailyLoginEpochDay(day)
    }

    fun getSelectedFeaturedWeaponId(player: ServerPlayerEntity): String? {
        return (player as? ResonanceAccess)?.cresoraGetSelectedFeaturedWeaponId()
    }

    fun selectableFeaturedWeaponIds(): List<String> {
        return WeaponContentRegistry.weaponDefinitions()
            .filter { it.craft.craftedRarity == WeaponRarity.FIVE_STAR }
            .map { it.id }
    }

    fun setSelectedFeaturedWeaponId(player: ServerPlayerEntity, weaponId: String?): Boolean {
        val access = player as? ResonanceAccess ?: return false
        if (weaponId != null) {
            // 未登録 ID は弾く。WeaponContentRegistry 経由で存在チェック。
            val definition = runCatching { WeaponContentRegistry.requireWeapon(weaponId) }.getOrNull() ?: return false
            if (definition.craft.craftedRarity != WeaponRarity.FIVE_STAR) {
                return false
            }
        }
        val previous = access.cresoraGetSelectedFeaturedWeaponId()
        if (previous == weaponId) {
            return true
        }
        access.cresoraSetSelectedFeaturedWeaponId(weaponId)
        // セレクト変更時は LIMITED 側のピティを全てリセット（仕様: 「いつでも変更可能だがピティはリセット」）
        access.cresoraSetLimitedPityPulls(0)
        access.cresoraSetLimitedFourStarPulls(0)
        access.cresoraSetLimitedFiveStarGuaranteed(false)
        access.cresoraSetDeepPityStreak(0)
        access.cresoraSetArpeggioReady(false)
        return true
    }

    fun getProgress(player: ServerPlayerEntity): Progress {
        val access = player as? ResonanceAccess ?: return Progress(0, 0, 0, 0, 0, false, false, null)
        return Progress(
            limitedPityPulls = max(0, access.cresoraGetLimitedPityPulls()),
            standardPulls = max(0, access.cresoraGetStandardPulls()),
            limitedFourStarPulls = max(0, access.cresoraGetLimitedFourStarPulls()),
            standardFourStarPulls = max(0, access.cresoraGetStandardFourStarPulls()),
            deepPityStreak = max(0, access.cresoraGetDeepPityStreak()),
            limitedFiveStarGuaranteed = access.cresoraIsLimitedFiveStarGuaranteed(),
            arpeggioReady = access.cresoraGetArpeggioReady(),
            selectedFeaturedWeaponId = access.cresoraGetSelectedFeaturedWeaponId()
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
        newAccess.cresoraSetLastDailyLoginEpochDay(oldAccess.cresoraGetLastDailyLoginEpochDay())
        newAccess.cresoraSetSelectedFeaturedWeaponId(oldAccess.cresoraGetSelectedFeaturedWeaponId())
        newAccess.cresoraSetSpiritBondPointsRaw(oldAccess.cresoraGetSpiritBondPointsRaw())
    }

    fun getSpiritBondPoints(player: ServerPlayerEntity, weaponId: String): Int {
        return readSpiritBondPoints(player)[weaponId]?.coerceAtLeast(0) ?: 0
    }

    fun addSpiritBondPoints(player: ServerPlayerEntity, weaponId: String, amount: Int): Int {
        if (amount <= 0 || weaponId.isBlank()) {
            return getSpiritBondPoints(player, weaponId)
        }
        val access = player as? ResonanceAccess ?: return 0
        val points = readSpiritBondPoints(player).toMutableMap()
        val updated = ((points[weaponId] ?: 0).toLong() + amount.toLong()).coerceAtMost(Int.MAX_VALUE.toLong()).toInt()
        points[weaponId] = updated
        access.cresoraSetSpiritBondPointsRaw(writeSpiritBondPoints(points))
        return updated
    }

    fun spiritBondStage(player: ServerPlayerEntity, weaponId: String): Int {
        return spiritBondStageForPoints(getSpiritBondPoints(player, weaponId))
    }

    fun spiritBondStageForPoints(points: Int): Int {
        val normalized = points.coerceAtLeast(0)
        for (index in SPIRIT_BOND_STAGE_THRESHOLDS.indices.reversed()) {
            if (normalized >= SPIRIT_BOND_STAGE_THRESHOLDS[index]) {
                return index + 1
            }
        }
        return 1
    }

    /** Highest reachable spirit bond stage (1-indexed). */
    fun maxSpiritBondStage(): Int = SPIRIT_BOND_STAGE_THRESHOLDS.size

    /**
     * Bond points required to reach [stage] (1-indexed). Stages outside `1..maxSpiritBondStage()`
     * clamp to the nearest valid threshold, so callers can ask for "next stage" without bounds checks.
     */
    fun spiritBondStageThreshold(stage: Int): Int {
        val index = (stage - 1).coerceIn(0, SPIRIT_BOND_STAGE_THRESHOLDS.size - 1)
        return SPIRIT_BOND_STAGE_THRESHOLDS[index]
    }

    fun currencyCount(player: ServerPlayerEntity, banner: ResonanceBannerDefinition): Int {
        return getCurrency(player, banner.currencyType())
    }

    fun canPull(player: ServerPlayerEntity, banner: ResonanceBannerDefinition): Boolean {
        if (currencyCount(player, banner) < banner.cost) return false
        if (banner.type == ResonanceBannerType.LIMITED && getSelectedFeaturedWeaponId(player) == null) return false
        return true
    }

    fun pull(player: ServerPlayerEntity, banner: ResonanceBannerDefinition): PullOutcome {
        if (banner.type == ResonanceBannerType.LIMITED && getSelectedFeaturedWeaponId(player) == null) {
            return PullOutcome.FeaturedNotSelected
        }
        if (currencyCount(player, banner) < banner.cost) {
            return PullOutcome.NotEnoughCurrency
        }

        val before = getProgress(player)
        var rarity = rollRarity(player, banner, before)

        val fourStarPulls = if (banner.type == ResonanceBannerType.LIMITED) before.limitedFourStarPulls else before.standardFourStarPulls
        if (fourStarPulls >= 9 && (rarity == WeaponRarity.TWO_STAR || rarity == WeaponRarity.THREE_STAR)) {
            rarity = WeaponRarity.FOUR_STAR
        }

        var obtainedFeatured = false
        val entry: ResonanceWeaponEntry? = when (rarity) {
            WeaponRarity.FIVE_STAR -> {
                val choice = chooseLimitedFiveStarEntry(player, banner, before)
                    ?: return PullOutcome.NoFiveStarWeaponsAvailable
                obtainedFeatured = choice.second
                choice.first
            }
            WeaponRarity.FOUR_STAR -> chooseEntry(player, banner.fourStarPool)
            WeaponRarity.THREE_STAR -> chooseEntry(player, banner.threeStarPool)
            WeaponRarity.TWO_STAR -> chooseEntry(player, banner.twoStarPool)
        }
        requireNotNull(entry)

        // ★5 を確定してから通貨を消費する（プールが空などで失敗した時に持ち出さない）
        if (!spendCurrency(player, banner.currencyType(), banner.cost)) {
            return PullOutcome.NotEnoughCurrency
        }

        val definition = WeaponContentRegistry.requireWeapon(entry.weaponId)
        val pulledWeapon = WeaponStackSupport.createWeaponStack(definition, entry.rarity, 1, 1)
        val duplicateConverted = WeaponSkillService.hasWeaponInInventory(player, definition.id)
        val bondPointsAwarded = if (duplicateConverted) bondPointsForRarity(entry.rarity) else 0
        val bondPointsAfter = if (duplicateConverted) {
            addSpiritBondPoints(player, definition.id, bondPointsAwarded)
        } else {
            player.giveItemStack(pulledWeapon.copy())
            getSpiritBondPoints(player, definition.id)
        }

        val updated = updateProgress(player, banner, rarity, obtainedFeatured)
        hifumi.cresora.guide.GuideService.onResonancePull(player, 1)
        hifumi.cresora.guide.GuideService.onSpiritObtained(player, definition.id)
        return PullOutcome.Success(
            PullResult(
                banner = banner,
                pulledWeapon = pulledWeapon,
                rarity = rarity,
                wasLimitedFiveStar = banner.type == ResonanceBannerType.LIMITED && rarity == WeaponRarity.FIVE_STAR,
                obtainedFeaturedFiveStar = obtainedFeatured,
                duplicateConverted = duplicateConverted,
                bondPointsAwarded = bondPointsAwarded,
                bondPointsAfter = bondPointsAfter,
                progressAfter = updated
            )
        )
    }

    private fun chooseLimitedFiveStarEntry(
        player: ServerPlayerEntity,
        banner: ResonanceBannerDefinition,
        progress: Progress
    ): Pair<ResonanceWeaponEntry, Boolean>? {
        if (banner.type == ResonanceBannerType.LIMITED) {
            val featuredId = progress.selectedFeaturedWeaponId ?: return null
            val isGuaranteed = progress.limitedFiveStarGuaranteed
            val wonFiftyFifty = player.random.nextDouble() < 0.5

            if (isGuaranteed || wonFiftyFifty) {
                return ResonanceWeaponEntry(featuredId, WeaponRarity.FIVE_STAR, 1.0) to true
            }
            // すり抜け: 登録済み全★5から PU を除外して抽選
            val slipPool = selectableFeaturedWeaponIds()
                .filter { it != featuredId }
                .map { ResonanceWeaponEntry(it, WeaponRarity.FIVE_STAR, 1.0) }
            if (slipPool.isEmpty()) {
                // PU 以外の★5が登録されていない場合は PU を排出（最終フォールバック）
                return ResonanceWeaponEntry(featuredId, WeaponRarity.FIVE_STAR, 1.0) to true
            }
            return chooseEntry(player, slipPool) to false
        }
        return chooseEntry(player, banner.fiveStarPool) to false
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
                    before.arpeggioReady,
                    before.selectedFeaturedWeaponId
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
                    },
                    selectedFeaturedWeaponId = before.selectedFeaturedWeaponId
                )
            }
        }
    }

    private fun bondPointsForRarity(rarity: WeaponRarity): Int {
        return when (rarity) {
            WeaponRarity.TWO_STAR -> 1
            WeaponRarity.THREE_STAR -> 3
            WeaponRarity.FOUR_STAR -> 12
            WeaponRarity.FIVE_STAR -> 40
        }
    }

    private fun readSpiritBondPoints(player: ServerPlayerEntity): Map<String, Int> {
        val raw = (player as? ResonanceAccess)?.cresoraGetSpiritBondPointsRaw().orEmpty()
        if (raw.isBlank()) {
            return emptyMap()
        }
        return raw.split(';')
            .mapNotNull { entry ->
                val parts = entry.split('=', limit = 2)
                if (parts.size != 2) {
                    return@mapNotNull null
                }
                val weaponId = parts[0].trim()
                val points = parts[1].toIntOrNull()?.coerceAtLeast(0) ?: return@mapNotNull null
                if (weaponId.isBlank()) null else weaponId to points
            }
            .toMap()
    }

    private fun writeSpiritBondPoints(points: Map<String, Int>): String {
        return points.entries
            .filter { it.key.isNotBlank() && it.value > 0 }
            .sortedBy { it.key }
            .joinToString(";") { "${it.key}=${it.value}" }
    }
}
