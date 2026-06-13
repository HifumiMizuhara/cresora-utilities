package hifumi.cresora.weapon

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

/**
 * Regression guard for held-weapon scoping (TODO L40). The passive-bonus accessors in
 * WeaponSkillService gate on this predicate so that an unequipped weapon's stats never
 * blend into the held weapon's attribute calculation.
 */
class WeaponScopeSupportTest {

    @Test
    fun unscopedRequestAlwaysAppliesToHeldWeapon() {
        // weaponId == null means "use whatever is held" — the held weapon must contribute.
        assertTrue(WeaponScopeSupport.isActiveWeaponScope("hanwu_juanxue", null))
    }

    @Test
    fun matchingWeaponIdApplies() {
        assertTrue(WeaponScopeSupport.isActiveWeaponScope("hanwu_juanxue", "hanwu_juanxue"))
    }

    @Test
    fun mismatchedWeaponIdIsExcluded() {
        // The held weapon differs from the requested one: an unequipped weapon must not
        // leak its passive bonus into the active weapon's stats.
        assertFalse(WeaponScopeSupport.isActiveWeaponScope("hanwu_juanxue", "kyokusui_no_ryusho"))
    }

    @Test
    fun emptyHeldIdStillHonoursExplicitRequest() {
        // Defensive: an empty active id should only match an empty requested id.
        assertFalse(WeaponScopeSupport.isActiveWeaponScope("", "hanwu_juanxue"))
        assertTrue(WeaponScopeSupport.isActiveWeaponScope("", null))
        assertTrue(WeaponScopeSupport.isActiveWeaponScope("", ""))
    }
}
