package hifumi.cresora.weapon

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test

class WeaponSpiritRegistryTest {

    companion object {
        private val TEST_ONLY_FIVE_STAR_IDS = setOf(
            "crimson_flash",
            "stance_test",
            "lossless_crown"
        )

        @JvmStatic
        @BeforeAll
        fun loadWeapons() {
            WeaponContentRegistry.init()
        }
    }

    @Test
    fun everyProductionFiveStarWeaponDefinesCompleteSpiritMetadata() {
        val productionFiveStars = WeaponContentRegistry.weaponDefinitions()
            .filter { it.craft.craftedRarity == WeaponRarity.FIVE_STAR }
            .filterNot { it.id in TEST_ONLY_FIVE_STAR_IDS }

        assertFalse(productionFiveStars.isEmpty(), "Expected at least one production five-star weapon")

        for (definition in productionFiveStars) {
            val spirit = requireNotNull(definition.spirit) {
                "Weapon '${definition.id}' should define spirit metadata"
            }
            assertTrue(spirit.nameKey.isNotBlank(), "Weapon '${definition.id}' spirit nameKey must not be blank")
            assertTrue(
                !spirit.awakeningConditionKey.isNullOrBlank(),
                "Weapon '${definition.id}' should define an awakening condition"
            )
            assertTrue(
                !spirit.voiceLines["first_contact"].isNullOrBlank(),
                "Weapon '${definition.id}' should define a first_contact voice line"
            )
            assertEquals(
                listOf(1, 2, 3, 4, 5, 6),
                spirit.bondStages.map { it.stage },
                "Weapon '${definition.id}' should define all six spirit bond stages"
            )
            spirit.bondStages.forEach { stage ->
                assertTrue(stage.titleKey.isNotBlank(), "Weapon '${definition.id}' stage ${stage.stage} titleKey must not be blank")
                assertTrue(stage.storyKey.isNotBlank(), "Weapon '${definition.id}' stage ${stage.stage} storyKey must not be blank")
            }
        }
    }
}
