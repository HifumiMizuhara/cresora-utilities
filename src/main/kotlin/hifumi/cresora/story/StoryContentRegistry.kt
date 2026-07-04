package hifumi.cresora.story
import hifumi.cresora.CreSoraUtilities
import hifumi.cresora.adventurerank.AdventureRankProgression
import hifumi.cresora.domain.DomainContentRegistry
import hifumi.cresora.resonance.ResonanceCurrencyType
import hifumi.cresora.world.RegionContentRegistry
import hifumi.cresora.weapon.WeaponContentRegistry
import hifumi.cresora.weapon.WeaponRarity
import com.google.gson.JsonParser
import com.mojang.serialization.Codec
import com.mojang.serialization.JsonOps
import com.mojang.serialization.MapCodec
import com.mojang.serialization.codecs.RecordCodecBuilder
import org.slf4j.LoggerFactory
import java.io.InputStreamReader
import java.util.Optional

enum class StoryBattleObjectiveType(val id: String) {
    DEFEAT_ALL("defeat_all"),
    SURVIVE_TIME("survive_time");

    companion object {
        private val BY_ID = entries.associateBy(StoryBattleObjectiveType::id)

        val CODEC: Codec<StoryBattleObjectiveType> = Codec.STRING.xmap(
            { id -> BY_ID[id] ?: throw IllegalArgumentException("Unknown story battle objective type: $id") },
            StoryBattleObjectiveType::id
        )
    }
}

data class StoryBattleObjectiveDefinition(
    val type: StoryBattleObjectiveType = StoryBattleObjectiveType.DEFEAT_ALL,
    val durationSeconds: Int = 0
) {
    companion object {
        val CODEC: Codec<StoryBattleObjectiveDefinition> = RecordCodecBuilder.create { instance ->
            instance.group(
                StoryBattleObjectiveType.CODEC.optionalFieldOf("type", StoryBattleObjectiveType.DEFEAT_ALL).forGetter(StoryBattleObjectiveDefinition::type),
                Codec.INT.optionalFieldOf("durationSeconds", 0).forGetter(StoryBattleObjectiveDefinition::durationSeconds)
            ).apply(instance, ::StoryBattleObjectiveDefinition)
        }
    }
}

data class StoryBattleModifierDefinition(
    val damageReductionPercent: Double = 0.0,
    val trueDamageImmune: Boolean = false
) {
    companion object {
        val CODEC: Codec<StoryBattleModifierDefinition> = RecordCodecBuilder.create { instance ->
            instance.group(
                Codec.DOUBLE.optionalFieldOf("damageReductionPercent", 0.0).forGetter(StoryBattleModifierDefinition::damageReductionPercent),
                Codec.BOOL.optionalFieldOf("trueDamageImmune", false).forGetter(StoryBattleModifierDefinition::trueDamageImmune)
            ).apply(instance, ::StoryBattleModifierDefinition)
        }
    }
}

data class StoryGrantedWeaponDefinition(
    val weaponId: String,
    val rarity: WeaponRarity,
    val baseLevel: Int,
    val skillLevel: Int,
    val removeOnExit: Boolean = true
) {
    companion object {
        val CODEC: Codec<StoryGrantedWeaponDefinition> = RecordCodecBuilder.create { instance ->
            instance.group(
                Codec.STRING.fieldOf("weaponId").forGetter(StoryGrantedWeaponDefinition::weaponId),
                WeaponRarity.CODEC.fieldOf("rarity").forGetter(StoryGrantedWeaponDefinition::rarity),
                Codec.INT.fieldOf("baseLevel").forGetter(StoryGrantedWeaponDefinition::baseLevel),
                Codec.INT.fieldOf("skillLevel").forGetter(StoryGrantedWeaponDefinition::skillLevel),
                Codec.BOOL.optionalFieldOf("removeOnExit", true).forGetter(StoryGrantedWeaponDefinition::removeOnExit)
            ).apply(instance, ::StoryGrantedWeaponDefinition)
        }
    }
}

data class StoryResonantChordTutorialStepDefinition(
    val reactionKey: String,
    val effectKey: String,
    val weapons: List<StoryGrantedWeaponDefinition>
) {
    companion object {
        val CODEC: Codec<StoryResonantChordTutorialStepDefinition> = RecordCodecBuilder.create { instance ->
            instance.group(
                Codec.STRING.fieldOf("reactionKey").forGetter(StoryResonantChordTutorialStepDefinition::reactionKey),
                Codec.STRING.fieldOf("effectKey").forGetter(StoryResonantChordTutorialStepDefinition::effectKey),
                StoryGrantedWeaponDefinition.CODEC.listOf().fieldOf("weapons").forGetter(StoryResonantChordTutorialStepDefinition::weapons)
            ).apply(instance, ::StoryResonantChordTutorialStepDefinition)
        }
    }
}

data class StoryDialogueLine(
    val speaker: String? = null,
    val speakerId: String? = null,
    val text: String? = null,
    val textId: String? = null
) {
    companion object {
        val CODEC: Codec<StoryDialogueLine> = RecordCodecBuilder.create { instance ->
            instance.group(
                Codec.STRING.optionalFieldOf("speaker").forGetter { Optional.ofNullable(it.speaker) },
                Codec.STRING.optionalFieldOf("speakerId").forGetter { Optional.ofNullable(it.speakerId) },
                Codec.STRING.optionalFieldOf("text").forGetter { Optional.ofNullable(it.text) },
                Codec.STRING.optionalFieldOf("textId").forGetter { Optional.ofNullable(it.textId) }
            ).apply(instance) { speaker, speakerId, text, textId ->
                StoryDialogueLine(speaker.orElse(null), speakerId.orElse(null), text.orElse(null), textId.orElse(null))
            }
        }
    }
}

data class StoryBattleSpawnDefinition(
    val entityTypeId: String,
    val count: Int
) {
    companion object {
        val CODEC: Codec<StoryBattleSpawnDefinition> = RecordCodecBuilder.create { instance ->
            instance.group(
                Codec.STRING.fieldOf("entityTypeId").forGetter(StoryBattleSpawnDefinition::entityTypeId),
                Codec.INT.optionalFieldOf("count", 1).forGetter(StoryBattleSpawnDefinition::count)
            ).apply(instance, ::StoryBattleSpawnDefinition)
        }
    }
}

data class StoryBattleWaveDefinition(
    val enemyRank: Int,
    val spawnDelayTicks: Int,
    val spawns: List<StoryBattleSpawnDefinition>,
    val modifiers: StoryBattleModifierDefinition = StoryBattleModifierDefinition()
) {
    companion object {
        val CODEC: Codec<StoryBattleWaveDefinition> = RecordCodecBuilder.create { instance ->
            instance.group(
                Codec.INT.fieldOf("enemyRank").forGetter(StoryBattleWaveDefinition::enemyRank),
                Codec.INT.optionalFieldOf("spawnDelayTicks", 40).forGetter(StoryBattleWaveDefinition::spawnDelayTicks),
                StoryBattleSpawnDefinition.CODEC.listOf().fieldOf("spawns").forGetter(StoryBattleWaveDefinition::spawns),
                StoryBattleModifierDefinition.CODEC.optionalFieldOf("modifiers", StoryBattleModifierDefinition()).forGetter(StoryBattleWaveDefinition::modifiers)
            ).apply(instance, ::StoryBattleWaveDefinition)
        }
    }
}

data class StoryCurrencyRewardDefinition(
    val type: ResonanceCurrencyType,
    val amount: Int
) {
    companion object {
        val CODEC: Codec<StoryCurrencyRewardDefinition> = RecordCodecBuilder.create { instance ->
            instance.group(
                ResonanceCurrencyType.CODEC.fieldOf("type").forGetter(StoryCurrencyRewardDefinition::type),
                Codec.INT.fieldOf("amount").forGetter(StoryCurrencyRewardDefinition::amount)
            ).apply(instance, ::StoryCurrencyRewardDefinition)
        }
    }
}

data class StoryRewardDefinition(
    val credits: Int,
    val resonanceCurrencies: List<StoryCurrencyRewardDefinition>
) {
    companion object {
        val CODEC: Codec<StoryRewardDefinition> = RecordCodecBuilder.create { instance ->
            instance.group(
                Codec.INT.optionalFieldOf("credits", 0).forGetter(StoryRewardDefinition::credits),
                StoryCurrencyRewardDefinition.CODEC.listOf().optionalFieldOf("resonanceCurrencies", emptyList()).forGetter(StoryRewardDefinition::resonanceCurrencies)
            ).apply(instance, ::StoryRewardDefinition)
        }
    }
}

data class StoryChapterDefinition(
    val id: String,
    val displayName: String,
    val groupId: String? = null,
    val sortOrder: Int = 0,
    val titleTextId: String? = null,
    val linkedDomainId: String? = null,
    val domainRewardIds: List<String> = emptyList(),
    val unlockRank: Int,
    val prerequisiteChapterId: String?,
    val requiredRegionId: String? = null,
    val requiredSpiritId: String? = null,
    val requiredBondStage: Int = 0,
    val preBattleStory: List<StoryDialogueLine>,
    val combatHints: List<StoryDialogueLine>,
    val grantedWeapons: List<StoryGrantedWeaponDefinition>,
    val resonantChordTutorialSteps: List<StoryResonantChordTutorialStepDefinition> = emptyList(),
    val battleObjective: StoryBattleObjectiveDefinition = StoryBattleObjectiveDefinition(),
    val battle: List<StoryBattleWaveDefinition>,
    val postBattleStory: List<StoryDialogueLine>,
    val rewards: StoryRewardDefinition
) {
    companion object {
        private data class ChapterBase(
            val id: String,
            val displayName: String,
            val groupId: String?,
            val sortOrder: Int,
            val titleTextId: String?,
            val linkedDomainId: String?,
            val domainRewardIds: List<String>,
            val unlockRank: Int,
            val prerequisiteChapterId: String?,
            val requiredRegionId: String?,
            val requiredSpiritId: String?,
            val requiredBondStage: Int
        )

        private data class ChapterContent(
            val preBattleStory: List<StoryDialogueLine>,
            val combatHints: List<StoryDialogueLine>,
            val grantedWeapons: List<StoryGrantedWeaponDefinition>,
            val resonantChordTutorialSteps: List<StoryResonantChordTutorialStepDefinition>,
            val battleObjective: StoryBattleObjectiveDefinition,
            val battle: List<StoryBattleWaveDefinition>,
            val postBattleStory: List<StoryDialogueLine>,
            val rewards: StoryRewardDefinition
        )

        private val BASE_CODEC: MapCodec<ChapterBase> = RecordCodecBuilder.mapCodec { instance ->
            instance.group(
                Codec.STRING.fieldOf("id").forGetter(ChapterBase::id),
                Codec.STRING.fieldOf("displayName").forGetter(ChapterBase::displayName),
                Codec.STRING.optionalFieldOf("groupId", "").forGetter { it.groupId ?: "" },
                Codec.INT.optionalFieldOf("sortOrder", 0).forGetter(ChapterBase::sortOrder),
                Codec.STRING.optionalFieldOf("titleTextId", "").forGetter { it.titleTextId ?: "" },
                Codec.STRING.optionalFieldOf("linkedDomainId", "").forGetter { it.linkedDomainId ?: "" },
                Codec.STRING.listOf().optionalFieldOf("domainRewardIds", emptyList()).forGetter(ChapterBase::domainRewardIds),
                Codec.INT.optionalFieldOf("unlockRank", AdventureRankProgression.MIN_RANK).forGetter(ChapterBase::unlockRank),
                Codec.STRING.optionalFieldOf("prerequisiteChapterId", "").forGetter { it.prerequisiteChapterId ?: "" },
                Codec.STRING.optionalFieldOf("requiredRegionId", "").forGetter { it.requiredRegionId ?: "" },
                Codec.STRING.optionalFieldOf("requiredSpiritId", "").forGetter { it.requiredSpiritId ?: "" },
                Codec.INT.optionalFieldOf("requiredBondStage", 0).forGetter(ChapterBase::requiredBondStage)
            ).apply(instance) { id, displayName, groupId, sortOrder, titleTextId, linkedDomainId, domainRewardIds, unlockRank, prerequisiteChapterId, requiredRegionId, requiredSpiritId, requiredBondStage ->
                ChapterBase(
                    id, displayName, groupId.takeUnless(String::isBlank), sortOrder,
                    titleTextId.takeUnless(String::isBlank), linkedDomainId.takeUnless(String::isBlank),
                    domainRewardIds, unlockRank, prerequisiteChapterId.takeUnless(String::isBlank),
                    requiredRegionId.takeUnless(String::isBlank),
                    requiredSpiritId.takeUnless(String::isBlank),
                    requiredBondStage
                )
            }
        }

        private val CONTENT_CODEC: MapCodec<ChapterContent> = RecordCodecBuilder.mapCodec { instance ->
            instance.group(
                StoryDialogueLine.CODEC.listOf().optionalFieldOf("preBattleStory", emptyList()).forGetter(ChapterContent::preBattleStory),
                StoryDialogueLine.CODEC.listOf().optionalFieldOf("combatHints", emptyList()).forGetter(ChapterContent::combatHints),
                StoryGrantedWeaponDefinition.CODEC.listOf().optionalFieldOf("grantedWeapons", emptyList()).forGetter(ChapterContent::grantedWeapons),
                StoryResonantChordTutorialStepDefinition.CODEC.listOf().optionalFieldOf("resonantChordTutorialSteps", emptyList()).forGetter(ChapterContent::resonantChordTutorialSteps),
                StoryBattleObjectiveDefinition.CODEC.optionalFieldOf("battleObjective", StoryBattleObjectiveDefinition()).forGetter(ChapterContent::battleObjective),
                StoryBattleWaveDefinition.CODEC.listOf().optionalFieldOf("battle", emptyList()).forGetter(ChapterContent::battle),
                StoryDialogueLine.CODEC.listOf().optionalFieldOf("postBattleStory", emptyList()).forGetter(ChapterContent::postBattleStory),
                StoryRewardDefinition.CODEC.optionalFieldOf("rewards", StoryRewardDefinition(0, emptyList())).forGetter(ChapterContent::rewards)
            ).apply(instance, ::ChapterContent)
        }

        val CODEC: Codec<StoryChapterDefinition> = RecordCodecBuilder.create { instance ->
            instance.group(
                BASE_CODEC.forGetter { ch ->
                    ChapterBase(
                        ch.id, ch.displayName, ch.groupId, ch.sortOrder,
                        ch.titleTextId, ch.linkedDomainId, ch.domainRewardIds,
                        ch.unlockRank, ch.prerequisiteChapterId, ch.requiredRegionId,
                        ch.requiredSpiritId, ch.requiredBondStage
                    )
                },
                CONTENT_CODEC.forGetter { ch ->
                    ChapterContent(
                        ch.preBattleStory, ch.combatHints, ch.grantedWeapons,
                        ch.resonantChordTutorialSteps, ch.battleObjective, ch.battle, ch.postBattleStory, ch.rewards
                    )
                }
            ).apply(instance) { base, content ->
                StoryChapterDefinition(
                    base.id, base.displayName, base.groupId, base.sortOrder,
                    base.titleTextId, base.linkedDomainId, base.domainRewardIds,
                    base.unlockRank, base.prerequisiteChapterId, base.requiredRegionId,
                    base.requiredSpiritId, base.requiredBondStage,
                    content.preBattleStory, content.combatHints, content.grantedWeapons,
                    content.resonantChordTutorialSteps, content.battleObjective, content.battle, content.postBattleStory, content.rewards
                )
            }
        }
    }
}

data class StoryContentBundle(
    val chapters: List<StoryChapterDefinition>
) {
    companion object {
        val CODEC: Codec<StoryContentBundle> = RecordCodecBuilder.create { instance ->
            instance.group(
                StoryChapterDefinition.CODEC.listOf().fieldOf("chapters").forGetter(StoryContentBundle::chapters)
            ).apply(instance, ::StoryContentBundle)
        }
    }
}

object StoryContentRegistry {
    private const val CONTENT_RESOURCE = "data/cresora-utilities/cresora/story_content.json"

    private val logger = LoggerFactory.getLogger("${CreSoraUtilities.MOD_ID}/story-content")

    @Volatile
    private var chapters: Map<String, StoryChapterDefinition> = emptyMap()

    fun init() {
        val bundle = loadBundledContent()
        applyBundle(bundle)
        logger.info("Loaded story content from {}", CONTENT_RESOURCE)
    }

    fun chapters(): List<StoryChapterDefinition> {
        return chapters.values.sortedWith(
            compareBy<StoryChapterDefinition>({ chapterGroupNumericKey(groupIdOf(it)) }, { groupIdOf(it) }, StoryChapterDefinition::sortOrder, StoryChapterDefinition::id)
        )
    }

    fun chapterGroups(): List<String> {
        return chapters().map(::groupIdOf).distinct().sortedWith(compareBy(StoryContentRegistry::chapterGroupNumericKey, { it }))
    }

    fun chaptersForGroup(groupId: String): List<StoryChapterDefinition> {
        return chapters().filter { groupIdOf(it) == groupId }
    }

    fun requireChapter(id: String): StoryChapterDefinition {
        return chapters[id] ?: error("Unknown story chapter definition: $id")
    }

    fun chapterGroupOf(chapterId: String): String = chapterId.substringBefore('-').ifBlank { chapterId }

    fun groupIdOf(chapter: StoryChapterDefinition): String = chapter.groupId?.takeUnless(String::isBlank) ?: chapterGroupOf(chapter.id)

    fun requiredFreeMainSlots(chapter: StoryChapterDefinition): Int {
        val tutorialWeaponCount = chapter.resonantChordTutorialSteps.maxOfOrNull { it.weapons.size } ?: 0
        return maxOf(chapter.grantedWeapons.size, tutorialWeaponCount, 1)
    }

    private fun loadBundledContent(): StoryContentBundle {
        val stream = StoryContentRegistry::class.java.classLoader.getResourceAsStream(CONTENT_RESOURCE)
            ?: error("Missing resource: $CONTENT_RESOURCE")
        InputStreamReader(stream).use { reader ->
            val json = JsonParser.parseReader(reader)
            return StoryContentBundle.CODEC.parse(JsonOps.INSTANCE, json)
                .getOrThrow { message -> IllegalArgumentException("Invalid story content: $message") }
        }
    }

    private fun applyBundle(bundle: StoryContentBundle) {
        val chapterMap = bundle.chapters.associateBy(StoryChapterDefinition::id)
        require(chapterMap.size == bundle.chapters.size) { "Duplicate story chapter ids found in content bundle" }
        for (chapter in chapterMap.values) {
            require(chapter.id.isNotBlank()) { "Story chapter id must not be blank" }
            require(chapter.displayName.isNotBlank()) { "Story chapter '${chapter.id}' displayName must not be blank" }
            require(chapter.groupId?.isNotBlank() != false) { "Story chapter '${chapter.id}' groupId must not be blank" }
            require(chapter.titleTextId?.isNotBlank() != false) { "Story chapter '${chapter.id}' titleTextId must not be blank" }
            require(chapter.linkedDomainId?.isNotBlank() != false) { "Story chapter '${chapter.id}' linkedDomainId must not be blank" }
            require(chapter.unlockRank in AdventureRankProgression.MIN_RANK..AdventureRankProgression.MAX_RANK) {
                "Invalid unlockRank on story chapter '${chapter.id}'"
            }
            when (chapter.battleObjective.type) {
                StoryBattleObjectiveType.DEFEAT_ALL -> Unit
                StoryBattleObjectiveType.SURVIVE_TIME -> require(chapter.battleObjective.durationSeconds > 0) {
                    "Story chapter '${chapter.id}' survive_time objective must define durationSeconds > 0"
                }
            }
            val prerequisiteChapterId = chapter.prerequisiteChapterId?.takeUnless(String::isBlank)
            if (prerequisiteChapterId != null) {
                require(prerequisiteChapterId != chapter.id) { "Story chapter '${chapter.id}' cannot require itself" }
                require(chapterMap.containsKey(prerequisiteChapterId)) {
                    "Story chapter '${chapter.id}' requires unknown prerequisite chapter '$prerequisiteChapterId'"
                }
            }
            val requiredRegionId = chapter.requiredRegionId?.takeUnless(String::isBlank)
            if (requiredRegionId != null) {
                RegionContentRegistry.requireRegion(requiredRegionId)
            }
            val requiredSpiritId = chapter.requiredSpiritId?.takeUnless(String::isBlank)
            if (requiredSpiritId != null) {
                val weaponDef = WeaponContentRegistry.requireWeapon(requiredSpiritId)
                require(weaponDef.spirit != null) {
                    "Story chapter '${chapter.id}' requiredSpiritId '$requiredSpiritId' refers to a weapon without a spirit"
                }
                require(chapter.requiredBondStage in 1..6) {
                    "Story chapter '${chapter.id}' must specify requiredBondStage in 1..6 when requiredSpiritId is set (got ${chapter.requiredBondStage})"
                }
            } else {
                require(chapter.requiredBondStage == 0) {
                    "Story chapter '${chapter.id}' cannot set requiredBondStage without requiredSpiritId"
                }
            }
            val linkedDomainId = chapter.linkedDomainId?.takeUnless(String::isBlank)
            if (linkedDomainId != null) {
                DomainContentRegistry.requireDomain(linkedDomainId)
            }
            for (rewardDomainId in chapter.domainRewardIds) {
                require(rewardDomainId.isNotBlank()) { "Story chapter '${chapter.id}' contains blank domainRewardIds entry" }
                DomainContentRegistry.requireDomain(rewardDomainId)
            }
            chapter.grantedWeapons.forEach { validateGrantedWeapon(chapter.id, it) }
            if (chapter.resonantChordTutorialSteps.isNotEmpty()) {
                require(chapter.grantedWeapons.isEmpty()) {
                    "Story chapter '${chapter.id}' cannot combine grantedWeapons with resonantChordTutorialSteps"
                }
                require(chapter.battleObjective == StoryBattleObjectiveDefinition()) {
                    "Story chapter '${chapter.id}' cannot combine battleObjective with resonantChordTutorialSteps"
                }
                val reactionKeys = chapter.resonantChordTutorialSteps.map(StoryResonantChordTutorialStepDefinition::reactionKey)
                require(reactionKeys.distinct().size == reactionKeys.size) {
                    "Story chapter '${chapter.id}' resonant chord tutorial reaction keys must be unique"
                }
                for (step in chapter.resonantChordTutorialSteps) {
                    require(step.reactionKey.isNotBlank()) {
                        "Story chapter '${chapter.id}' has a blank resonant chord tutorial reactionKey"
                    }
                    require(step.effectKey.isNotBlank()) {
                        "Story chapter '${chapter.id}' tutorial step '${step.reactionKey}' has a blank effectKey"
                    }
                    require(step.weapons.size == 2) {
                        "Story chapter '${chapter.id}' tutorial step '${step.reactionKey}' must grant exactly two weapons"
                    }
                    require(step.weapons.map(StoryGrantedWeaponDefinition::weaponId).distinct().size == step.weapons.size) {
                        "Story chapter '${chapter.id}' tutorial step '${step.reactionKey}' must grant two distinct weapons"
                    }
                    require(step.weapons.all(StoryGrantedWeaponDefinition::removeOnExit)) {
                        "Story chapter '${chapter.id}' tutorial step '${step.reactionKey}' weapons must set removeOnExit"
                    }
                    step.weapons.forEach { validateGrantedWeapon(chapter.id, it) }
                }
            }
            if (linkedDomainId == null) {
                require(chapter.battle.isNotEmpty()) { "Story chapter '${chapter.id}' must define at least one battle wave" }
                for (wave in chapter.battle) {
                    require(wave.enemyRank in AdventureRankProgression.MIN_RANK..AdventureRankProgression.MAX_RANK) {
                        "Story chapter '${chapter.id}' contains invalid enemyRank ${wave.enemyRank}"
                    }
                    require(wave.spawns.isNotEmpty()) { "Story chapter '${chapter.id}' contains an empty battle wave" }
                    require(wave.modifiers.damageReductionPercent in 0.0..100.0) {
                        "Story chapter '${chapter.id}' contains invalid damageReductionPercent ${wave.modifiers.damageReductionPercent}"
                    }
                    for (spawn in wave.spawns) {
                        require(spawn.count > 0) { "Story chapter '${chapter.id}' contains a non-positive spawn count" }
                    }
                }
            }
            require(chapter.rewards.credits >= 0) { "Story chapter '${chapter.id}' credits reward must be >= 0" }
            for (currency in chapter.rewards.resonanceCurrencies) {
                require(currency.amount >= 0) { "Story chapter '${chapter.id}' has a negative resonance reward amount" }
            }
        }
        chapters = chapterMap
    }

    private fun validateGrantedWeapon(chapterId: String, grantedWeapon: StoryGrantedWeaponDefinition) {
        val definition = WeaponContentRegistry.requireWeapon(grantedWeapon.weaponId)
        require(grantedWeapon.baseLevel in 1..definition.maxBaseLevel) {
            "Story chapter '$chapterId' has invalid baseLevel ${grantedWeapon.baseLevel} for '${grantedWeapon.weaponId}'"
        }
        require(grantedWeapon.skillLevel in 1..definition.maxSkillLevel) {
            "Story chapter '$chapterId' has invalid skillLevel ${grantedWeapon.skillLevel} for '${grantedWeapon.weaponId}'"
        }
    }

    private fun chapterGroupNumericKey(groupId: String): Int = groupId.toIntOrNull() ?: Int.MAX_VALUE
}
