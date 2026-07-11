package hifumi.cresora.equipment

import hifumi.cresora.StatEntry
import hifumi.cresora.StatType
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class EquipmentBalanceMigrationTest {
    @Test
    fun preservesBuildStructureAndMigratesOnlyOnce() {
        val legacy = EquipmentData(
            rarity = EquipmentRarity.FIVE_STAR,
            level = 16,
            mainStat = StatEntry(StatType.ATK_PERCENT, 40.0),
            subStats = listOf(StatEntry(StatType.CRIT_RATE, 10.0), StatEntry(StatType.DAMAGE_REDUCTION, 8.0)),
            upgradeCount = 4,
            slotTypeId = "wand",
            setId = "hinagata"
        )

        val migrated = EquipmentBalanceMigration.migrate(legacy)
        val rerun = EquipmentBalanceMigration.migrate(migrated)

        assertEquals(legacy.rarity, migrated.rarity)
        assertEquals(legacy.level, migrated.level)
        assertEquals(legacy.upgradeCount, migrated.upgradeCount)
        assertEquals(legacy.mainStat.type, migrated.mainStat.type)
        assertEquals(legacy.subStats.map { it.type }, migrated.subStats.map { it.type })
        assertEquals(31.2, migrated.mainStat.value, 0.0001)
        assertEquals(7.2, migrated.subStats.first().value, 0.0001)
        assertEquals(migrated, rerun)
    }
}
