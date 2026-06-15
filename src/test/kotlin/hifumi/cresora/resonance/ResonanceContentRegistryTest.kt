package hifumi.cresora.resonance

import hifumi.cresora.weapon.WeaponContentRegistry
import hifumi.cresora.weapon.WeaponRarity
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import java.lang.reflect.InvocationTargetException

class ResonanceContentRegistryTest {

    companion object {
        @JvmStatic
        @BeforeAll
        fun loadWeapons() {
            // validatePool() calls WeaponContentRegistry.requireWeapon(), so the weapon
            // registry must be populated before exercising banner validation.
            WeaponContentRegistry.init()
        }

        private val applyBundle = ResonanceContentRegistry::class.java
            .getDeclaredMethod("applyBundle", ResonanceContentBundle::class.java)
            .apply { isAccessible = true }

        private fun invokeApply(bundle: ResonanceContentBundle): Throwable {
            val exception = assertThrows(InvocationTargetException::class.java) {
                applyBundle.invoke(ResonanceContentRegistry, bundle)
            }
            val cause = exception.cause
            assertTrue(cause is IllegalArgumentException, "Expected IllegalArgumentException, got $cause")
            return cause!!
        }

        private fun banner(
            id: String = "test_banner",
            type: ResonanceBannerType = ResonanceBannerType.STANDARD,
            cost: Int = 325,
            rates: ResonanceRateTable = ResonanceRateTable(0.01, 0.0325, 0.0799, 0.8776),
            fiveStarPool: List<ResonanceWeaponEntry> = listOf(ResonanceWeaponEntry("rondo_melody", WeaponRarity.FIVE_STAR, 1.0)),
            fourStarPool: List<ResonanceWeaponEntry> = listOf(ResonanceWeaponEntry("gaoshan_liushui", WeaponRarity.FOUR_STAR, 1.0)),
            threeStarPool: List<ResonanceWeaponEntry> = listOf(ResonanceWeaponEntry("pastoral_flute_reverie", WeaponRarity.THREE_STAR, 1.0)),
            twoStarPool: List<ResonanceWeaponEntry> = listOf(ResonanceWeaponEntry("masquerade_invitation", WeaponRarity.TWO_STAR, 1.0))
        ): ResonanceBannerDefinition = ResonanceBannerDefinition(
            id = id,
            type = type,
            familyId = "test",
            translationKey = "screen.cresora.resonance.banner.$id",
            currencyItemId = "substitute_chord",
            cost = cost,
            rates = rates,
            fiveStarPool = fiveStarPool,
            fourStarPool = fourStarPool,
            threeStarPool = threeStarPool,
            twoStarPool = twoStarPool
        )
    }

    @Test
    fun testValidBannerPassesValidation() {
        // A well-formed bundle with real weapon ids must not throw.
        applyBundle.invoke(ResonanceContentRegistry, ResonanceContentBundle(listOf(banner())))
    }

    @Test
    fun testEmptyFiveStarPoolRejectedForStandard() {
        // STANDARD タイプは fiveStarPool 必須
        val cause = invokeApply(ResonanceContentBundle(listOf(banner(fiveStarPool = emptyList()))))
        assertTrue(cause.message!!.contains("five-star pool"), "Unexpected message: ${cause.message}")
    }

    @Test
    fun testEmptyFiveStarPoolAcceptedForLimitedSelect() {
        // LIMITED は SELECT モードのため fiveStarPool 空でも通る
        applyBundle.invoke(
            ResonanceContentRegistry,
            ResonanceContentBundle(listOf(banner(type = ResonanceBannerType.LIMITED, fiveStarPool = emptyList())))
        )
    }

    @Test
    fun testRarityMismatchInPoolRejected() {
        // A four-star weapon sitting in the five-star pool must be rejected before requireWeapon.
        val bundle = ResonanceContentBundle(listOf(
            banner(fiveStarPool = listOf(ResonanceWeaponEntry("rondo_melody", WeaponRarity.FOUR_STAR, 1.0)))
        ))
        val cause = invokeApply(bundle)
        assertTrue(cause.message!!.contains("fiveStarPool"), "Unexpected message: ${cause.message}")
    }

    @Test
    fun testRatesSummingAboveOneRejected() {
        val bundle = ResonanceContentBundle(listOf(
            banner(rates = ResonanceRateTable(0.5, 0.5, 0.5, 0.5))
        ))
        val cause = invokeApply(bundle)
        assertTrue(cause.message!!.contains("rates summing above"), "Unexpected message: ${cause.message}")
    }

    @Test
    fun testNonPositiveCostRejected() {
        val cause = invokeApply(ResonanceContentBundle(listOf(banner(cost = 0))))
        assertTrue(cause.message!!.contains("positive cost"), "Unexpected message: ${cause.message}")
    }

    @Test
    fun testDuplicateBannerIdsRejected() {
        val cause = invokeApply(ResonanceContentBundle(listOf(banner(id = "dup"), banner(id = "dup"))))
        assertTrue(cause.message!!.contains("Duplicate resonance banner ids"), "Unexpected message: ${cause.message}")
    }

    @Test
    fun testUnknownWeaponIdRejected() {
        val bundle = ResonanceContentBundle(listOf(
            banner(fiveStarPool = listOf(ResonanceWeaponEntry("nonexistent_weapon", WeaponRarity.FIVE_STAR, 1.0)))
        ))
        // requireWeapon() raises IllegalStateException ("Unknown weapon definition") rather than
        // IllegalArgumentException, so assert directly on the wrapped cause.
        val exception = assertThrows(InvocationTargetException::class.java) {
            applyBundle.invoke(ResonanceContentRegistry, bundle)
        }
        assertTrue(
            exception.cause!!.message!!.contains("Unknown weapon definition"),
            "Unexpected message: ${exception.cause?.message}"
        )
    }
}
