package hifumi.cresora.domain

import com.google.gson.JsonParser
import com.mojang.serialization.JsonOps
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import java.io.InputStreamReader

class DomainRewardProfileRegistryTest {

    @Test
    fun testDomainRewardProfilesJsonLoadsCleanly() {
        val resourcePath = "data/cresora-utilities/cresora/domain_reward_profiles.json"
        val stream = DomainRewardProfileRegistry::class.java.classLoader.getResourceAsStream(resourcePath)
            ?: fail("Missing resource: $resourcePath")

        val bundle = InputStreamReader(stream).use { reader ->
            val json = JsonParser.parseReader(reader)
            DomainRewardContentBundle.CODEC.parse(JsonOps.INSTANCE, json)
                .getOrThrow { message -> IllegalArgumentException("Invalid domain reward content: $message") }
        }

        assertNotNull(bundle)
        assertFalse(bundle.rewardProfiles.isEmpty(), "Reward profiles list should not be empty")

        // Verify that the specified profiles have proofReward or insightReward as requested
        val rondoForge = bundle.rewardProfiles.find { it.id == "rondo_forge" }
        assertNotNull(rondoForge, "rondo_forge profile should exist")
        assertNotNull(rondoForge!!.proofReward, "rondo_forge should have proofReward")
        assertEquals(hifumi.cresora.weapon.WeaponRole.DEFENDER, rondoForge.proofReward?.role)
        assertEquals(1, rondoForge.proofReward?.minCount)
        assertEquals(3, rondoForge.proofReward?.maxCount)

        val masqueradeSoiree = bundle.rewardProfiles.find { it.id == "masquerade_soiree" }
        assertNotNull(masqueradeSoiree, "masquerade_soiree profile should exist")
        assertNotNull(masqueradeSoiree!!.proofReward, "masquerade_soiree should have proofReward")
        assertEquals(hifumi.cresora.weapon.WeaponRole.MEDIC, masqueradeSoiree.proofReward?.role)
        assertEquals(1, masqueradeSoiree.proofReward?.minCount)
        assertEquals(3, masqueradeSoiree.proofReward?.maxCount)

        val requiemReliquary = bundle.rewardProfiles.find { it.id == "requiem_reliquary" }
        assertNotNull(requiemReliquary, "requiem_reliquary profile should exist")
        assertNotNull(requiemReliquary!!.insightReward, "requiem_reliquary should have insightReward")
        assertEquals(hifumi.cresora.weapon.WeaponRole.CASTER, requiemReliquary.insightReward?.role)
        assertEquals(1, requiemReliquary.insightReward?.minCount)
        assertEquals(2, requiemReliquary.insightReward?.maxCount)

        val lakesideSanctum = bundle.rewardProfiles.find { it.id == "lakeside_sanctum" }
        assertNotNull(lakesideSanctum, "lakeside_sanctum profile should exist")
        assertNotNull(lakesideSanctum!!.insightReward, "lakeside_sanctum should have insightReward")
        assertEquals(hifumi.cresora.weapon.WeaponRole.GUARD, lakesideSanctum.insightReward?.role)
        assertEquals(1, lakesideSanctum.insightReward?.minCount)
        assertEquals(2, lakesideSanctum.insightReward?.maxCount)

        val cscTraining = bundle.rewardProfiles.find { it.id == "csc_training" }
        assertNotNull(cscTraining, "csc_training profile should exist")
        assertNotNull(cscTraining!!.proofReward, "csc_training should have proofReward")
        assertNull(cscTraining.proofReward?.role, "csc_training proofReward should not specify a role")
        assertEquals(1, cscTraining.proofReward?.minCount)
        assertEquals(2, cscTraining.proofReward?.maxCount)

        val resonancePractice = bundle.rewardProfiles.find { it.id == "resonance_practice" }
        assertNotNull(resonancePractice, "resonance_practice profile should exist")
        assertNull(resonancePractice!!.artifactReward, "resonance_practice should not have artifactReward")
        assertNull(resonancePractice.weaponFragmentReward, "resonance_practice should not have weaponFragmentReward")
        assertNull(resonancePractice.proofReward, "resonance_practice should not have proofReward")
        assertNull(resonancePractice.insightReward, "resonance_practice should not have insightReward")
        assertEquals(300, resonancePractice.currencyReward.creditsBase)
        assertEquals(10, resonancePractice.currencyReward.rankXpBase)
        assertEquals(10, resonancePractice.chordProgressionReward)
        assertEquals(5, resonancePractice.substituteChordReward)

        // Verify backward compatibility of profiles without proofReward, insightReward, or weaponFragmentReward
        val hinagataHunt = bundle.rewardProfiles.find { it.id == "hinagata_hunt" }
        assertNotNull(hinagataHunt, "hinagata_hunt profile should exist")
        assertNull(hinagataHunt!!.proofReward, "hinagata_hunt should not have proofReward")
        assertNull(hinagataHunt.insightReward, "hinagata_hunt should not have insightReward")
        assertNull(hinagataHunt.weaponFragmentReward, "hinagata_hunt should not have weaponFragmentReward")
        assertNotNull(hinagataHunt.artifactReward, "hinagata_hunt should have artifactReward")

        val osananajimiReminiscence = bundle.rewardProfiles.find { it.id == "osananajimi_reminiscence" }
        assertNotNull(osananajimiReminiscence, "osananajimi_reminiscence profile should exist")
        assertNull(osananajimiReminiscence!!.proofReward, "osananajimi_reminiscence should not have proofReward")
        assertNull(osananajimiReminiscence.insightReward, "osananajimi_reminiscence should not have insightReward")
        assertNull(osananajimiReminiscence.weaponFragmentReward, "osananajimi_reminiscence should not have weaponFragmentReward")
        assertNotNull(osananajimiReminiscence.artifactReward, "osananajimi_reminiscence should have artifactReward")
    }

    @Test
    fun testDomainMaterialRewardDefinitionValidation() {
        val invalidDefinition = DomainMaterialRewardDefinition(
            role = hifumi.cresora.weapon.WeaponRole.GUARD,
            minCount = 5,
            maxCount = 2
        )

        val profile = DomainRewardProfile(
            id = "test_profile",
            proofReward = invalidDefinition,
            currencyReward = DomainCurrencyRewardDefinition(0, 0, 0, 0)
        )

        val bundle = DomainRewardContentBundle(listOf(profile))

        val method = DomainRewardProfileRegistry::class.java.getDeclaredMethod("applyBundle", DomainRewardContentBundle::class.java)
        method.isAccessible = true
        val exception = assertThrows(java.lang.reflect.InvocationTargetException::class.java) {
            method.invoke(DomainRewardProfileRegistry, bundle)
        }
        assertTrue(exception.cause is IllegalArgumentException)
        assertTrue(exception.cause!!.message!!.contains("Invalid proof reward count range"))
    }

    @Test
    fun testParsingMinimalProfileMissingOptionalRewards() {
        val jsonString = """
            {
              "id": "minimal_profile",
              "currencyReward": {
                "creditsBase": 1000,
                "creditsPerRank": 10,
                "rankXpBase": 100,
                "rankXpPerRank": 5
              }
            }
        """.trimIndent()

        val json = JsonParser.parseString(jsonString)
        val profile = DomainRewardProfile.CODEC.parse(JsonOps.INSTANCE, json)
            .getOrThrow { message -> IllegalArgumentException("Invalid domain reward profile: $message") }

        assertNotNull(profile)
        assertEquals("minimal_profile", profile.id)
        assertNull(profile.artifactReward, "artifactReward should be null if missing")
        assertNull(profile.weaponFragmentReward, "weaponFragmentReward should be null if missing")
        assertNull(profile.proofReward, "proofReward should be null if missing")
        assertNull(profile.insightReward, "insightReward should be null if missing")
        assertNotNull(profile.currencyReward)
        assertEquals(1000, profile.currencyReward.creditsBase)
    }
}
