package hifumi.cresora.domain

import com.google.gson.JsonParser
import com.mojang.serialization.JsonOps
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import java.io.InputStreamReader

class DomainContentRegistryTest {

    @Test
    fun testDomainContentJsonLoadsCleanly() {
        val resourcePath = "data/cresora-utilities/cresora/domain_content.json"
        val stream = DomainContentRegistry::class.java.classLoader.getResourceAsStream(resourcePath)
            ?: fail("Missing resource: $resourcePath")

        val bundle = InputStreamReader(stream).use { reader ->
            val json = JsonParser.parseReader(reader)
            DomainContentBundle.CODEC.parse(JsonOps.INSTANCE, json)
                .getOrThrow { message -> IllegalArgumentException("Invalid domain content: $message") }
        }

        assertNotNull(bundle)
        assertFalse(bundle.domains.isEmpty(), "Domain list should not be empty")

        val resonancePractice = bundle.domains.find { it.id == "resonance_practice" }
        assertNotNull(resonancePractice, "resonance_practice domain should exist")
        assertEquals("resonance_trial", resonancePractice!!.mobPoolId)
        assertEquals("resonance_practice", resonancePractice.rewardProfileId)
        assertEquals(0, resonancePractice.entryCostCsc)
        assertEquals(1, resonancePractice.stages.size)
        assertEquals(2, resonancePractice.stages.single().waves.size)
        assertEquals(3, resonancePractice.stages.single().waves.first().count)

        val mobPool = bundle.mobPools.find { it.id == "resonance_trial" }
        assertNotNull(mobPool, "resonance_trial mob pool should exist")
        assertEquals(4, mobPool!!.entries.size)
    }
}
