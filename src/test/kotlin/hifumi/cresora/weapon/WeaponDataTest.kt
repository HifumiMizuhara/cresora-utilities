package hifumi.cresora.weapon

import com.google.gson.JsonParser
import com.mojang.serialization.JsonOps
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class WeaponDataTest {

    @Test
    fun spiritBondStageIsClampedToSupportedRange() {
        val normalized = WeaponData(
            weaponId = "harukanaru_shojo_no_ketsui",
            rarity = WeaponRarity.FIVE_STAR,
            baseLevel = 1,
            skillLevel = 1,
            breakthrough = 0,
            spiritBondStage = 99
        ).normalized()

        assertEquals(6, normalized.spiritBondStage)
    }

    @Test
    fun codecDefaultsLegacyDataToBondStageOne() {
        val legacyJson = JsonParser.parseString(
            """
            {
              "weaponId": "harukanaru_shojo_no_ketsui",
              "rarity": "5_star",
              "baseLevel": 10,
              "skillLevel": 2,
              "breakthrough": 1
            }
            """.trimIndent()
        )

        val decoded = WeaponData.CODEC.parse(JsonOps.INSTANCE, legacyJson)
            .getOrThrow { message -> IllegalStateException(message) }

        assertEquals(1, decoded.spiritBondStage)
    }
}
