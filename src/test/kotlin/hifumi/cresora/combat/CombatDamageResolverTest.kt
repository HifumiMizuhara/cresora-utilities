package hifumi.cresora.combat

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class CombatDamageResolverTest {
    @Test
    fun appliesDamageBonusCritAndResistanceInOneOrder() {
        val resolved = CombatDamageResolver.resolve(
            CombatDamageRequest(
                baseDamage = 10.0,
                damageType = CombatDamageType.ARCANE,
                allDamageBonusRatio = 0.20,
                externalDamageMultiplier = 1.10,
                critRateRatio = 1.0,
                critDamageRatio = 1.0,
                targetResistanceRatio = 0.30,
                resistanceShredRatio = 0.10
            ),
            critRoll = 0.0
        )

        assertTrue(resolved.critical)
        assertEquals(0.20, resolved.effectiveResistanceRatio, 0.0001)
        assertEquals(20.8, resolved.damage, 0.0001)
    }

    @Test
    fun capsResistanceShredAtMinusFiftyPercent() {
        val resolved = CombatDamageResolver.resolve(
            CombatDamageRequest(
                baseDamage = 10.0,
                damageType = CombatDamageType.PHYSICAL,
                targetResistanceRatio = 0.0,
                resistanceShredRatio = 3.0,
                canCrit = false
            )
        )

        assertEquals(-0.50, resolved.effectiveResistanceRatio, 0.0001)
        assertEquals(15.0, resolved.damage, 0.0001)
    }

    @Test
    fun trueDamageSkipsOffenseAndMitigationLayers() {
        val resolved = CombatDamageResolver.resolve(
            CombatDamageRequest(
                baseDamage = 12.0,
                damageType = CombatDamageType.ARCANE,
                allDamageBonusRatio = 1.5,
                critRateRatio = 1.0,
                critDamageRatio = 2.5,
                targetResistanceRatio = 0.75,
                trueDamage = true
            ),
            critRoll = 0.0
        )

        assertTrue(resolved.trueDamage)
        assertFalse(resolved.critical)
        assertEquals(12.0, resolved.damage, 0.0001)
    }
}
