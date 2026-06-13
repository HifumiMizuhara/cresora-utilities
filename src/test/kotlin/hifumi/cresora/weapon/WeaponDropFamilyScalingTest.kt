package hifumi.cresora.weapon

import hifumi.cresora.combat.HostileRewardFamily
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

/**
 * Regression guard for mob equipment/weapon drop injection scaling (TODO L45, drop side).
 * The reward-family multipliers shape how much more loot tougher mobs inject; the in-world
 * spawn path (RNG + ServerWorld) still needs a runClient pass, but the scaling table itself
 * is pure and locked down here.
 */
class WeaponDropFamilyScalingTest {

    @Test
    fun fragmentMultipliersIncreaseWithFamilyToughness() {
        val ordered = listOf(
            HostileRewardFamily.SURVIVOR,
            HostileRewardFamily.ASSAULT,
            HostileRewardFamily.ARCANE,
            HostileRewardFamily.ELITE,
            HostileRewardFamily.RELIC
        ).map(WeaponDropService::fragmentFamilyMultiplier)

        assertEquals(ordered.sorted(), ordered, "Fragment family multipliers must be non-decreasing")
        assertEquals(1.0, WeaponDropService.fragmentFamilyMultiplier(HostileRewardFamily.SURVIVOR))
        assertTrue(
            WeaponDropService.fragmentFamilyMultiplier(HostileRewardFamily.RELIC) >
                WeaponDropService.fragmentFamilyMultiplier(HostileRewardFamily.SURVIVOR)
        )
    }

    @Test
    fun weaponMultipliersPenaliseSurvivorAndRewardRelic() {
        // Survivor mobs are below baseline, relic mobs well above it.
        assertEquals(0.85, WeaponDropService.weaponFamilyMultiplier(HostileRewardFamily.SURVIVOR))
        assertEquals(1.0, WeaponDropService.weaponFamilyMultiplier(HostileRewardFamily.ASSAULT))
        assertEquals(1.35, WeaponDropService.weaponFamilyMultiplier(HostileRewardFamily.RELIC))

        val ordered = listOf(
            HostileRewardFamily.SURVIVOR,
            HostileRewardFamily.ASSAULT,
            HostileRewardFamily.ARCANE,
            HostileRewardFamily.ELITE,
            HostileRewardFamily.RELIC
        ).map(WeaponDropService::weaponFamilyMultiplier)
        assertEquals(ordered.sorted(), ordered, "Weapon family multipliers must be non-decreasing")
    }

    @Test
    fun everyFamilyHasAPositiveMultiplier() {
        for (family in HostileRewardFamily.entries) {
            assertTrue(WeaponDropService.fragmentFamilyMultiplier(family) > 0.0)
            assertTrue(WeaponDropService.weaponFamilyMultiplier(family) > 0.0)
        }
    }
}
