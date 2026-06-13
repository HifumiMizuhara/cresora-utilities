package hifumi.cresora.weapon

/**
 * Pure scoping rules for weapon-skill passive bonuses. Kept free of any Minecraft
 * types so the held-weapon isolation guarantee can be regression-tested in plain JUnit.
 *
 * The invariant: a passive bonus is only contributed when the request either targets
 * the held weapon explicitly (matching id) or is unscoped (`requestedId == null`).
 * This prevents an unequipped weapon's stats from blending into the held weapon's
 * attribute calculation.
 */
object WeaponScopeSupport {
    fun isActiveWeaponScope(activeWeaponId: String, requestedWeaponId: String?): Boolean {
        return requestedWeaponId == null || activeWeaponId == requestedWeaponId
    }
}
