package hifumi.cresora

import com.google.gson.JsonParser
import com.mojang.serialization.Codec
import com.mojang.serialization.JsonOps
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
    val preBattleStory: List<StoryDialogueLine>,
    val combatHints: List<StoryDialogueLine>,
    val grantedWeapons: List<StoryGrantedWeaponDefinition>,
    val battleObjective: StoryBattleObjectiveDefinition = StoryBattleObjectiveDefinition(),
    val battle: List<StoryBattleWaveDefinition>,
    val postBattleStory: List<StoryDialogueLine>,
    val rewards: StoryRewardDefinition
) {
    companion object {
        val CODEC: Codec<StoryChapterDefinition> = RecordCodecBuilder.create { instance ->
            instance.group(
                Codec.STRING.fieldOf("id").forGetter(StoryChapterDefinition::id),
                Codec.STRING.fieldOf("displayName").forGetter(StoryChapterDefinition::displayName),
                Codec.STRING.optionalFieldOf("groupId").forGetter { Optional.ofNullable(it.groupId) },
                Codec.INT.optionalFieldOf("sortOrder", 0).forGetter(StoryChapterDefinition::sortOrder),
                Codec.STRING.optionalFieldOf("titleTextId").forGetter { Optional.ofNullable(it.titleTextId) },
                Codec.STRING.optionalFieldOf("linkedDomainId").forGetter { Optional.ofNullable(it.linkedDomainId) },
                Codec.STRING.listOf().optionalFieldOf("domainRewardIds", emptyList()).forGetter(StoryChapterDefinition::domainRewardIds),
                Codec.INT.optionalFieldOf("unlockRank", AdventureRankProgression.MIN_RANK).forGetter(StoryChapterDefinition::unlockRank),
                Codec.STRING.optionalFieldOf("prerequisiteChapterId").forGetter { Optional.ofNullable(it.prerequisiteChapterId) },
                StoryDialogueLine.CODEC.listOf().optionalFieldOf("preBattleStory", emptyList()).forGetter(StoryChapterDefinition::preBattleStory),
                StoryDialogueLine.CODEC.listOf().optionalFieldOf("combatHints", emptyList()).forGetter(StoryChapterDefinition::combatHints),
                StoryGrantedWeaponDefinition.CODEC.listOf().optionalFieldOf("grantedWeapons", emptyList()).forGetter(StoryChapterDefinition::grantedWeapons),
                StoryBattleObjectiveDefinition.CODEC.optionalFieldOf("battleObjective", StoryBattleObjectiveDefinition()).forGetter(StoryChapterDefinition::battleObjective),
                StoryBattleWaveDefinition.CODEC.listOf().optionalFieldOf("battle", emptyList()).forGetter(StoryChapterDefinition::battle),
                StoryDialogueLine.CODEC.listOf().optionalFieldOf("postBattleStory", emptyList()).forGetter(StoryChapterDefinition::postBattleStory),
                StoryRewardDefinition.CODEC.optionalFieldOf("rewards", StoryRewardDefinition(0, emptyList())).forGetter(StoryChapterDefinition::rewards)
            ).apply(instance) { id, displayName, groupId, sortOrder, titleTextId, linkedDomainId, domainRewardIds, unlockRank, prerequisiteChapterId, preBattleStory, combatHints, grantedWeapons, battleObjective, battle, postBattleStory, rewards ->
                StoryChapterDefinition(
                    id = id,
                    displayName = displayName,
                    groupId = groupId.orElse(null),
                    sortOrder = sortOrder,
                    titleTextId = titleTextId.orElse(null),
                    linkedDomainId = linkedDomainId.orElse(null),
                    domainRewardIds = domainRewardIds,
                    unlockRank = unlockRank,
                    prerequisiteChapterId = prerequisiteChapterId.orElse(null),
                    preBattleStory = preBattleStory,
                    combatHints = combatHints,
                    grantedWeapons = grantedWeapons,
                    battleObjective = battleObjective,
                    battle = battle,
                    postBattleStory = postBattleStory,
                    rewards = rewards
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
        applyBundle(defaultBundle())
        runCatching { loadBundledContent() }
            .onSuccess { bundle ->
                applyBundle(bundle)
                logger.info("Loaded story content from {}", CONTENT_RESOURCE)
            }
            .onFailure { throwable ->
                logger.error("Failed to load story content from {}. Using built-in defaults.", CONTENT_RESOURCE, throwable)
            }
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
        return chapter.grantedWeapons.size.coerceAtLeast(1)
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
            val linkedDomainId = chapter.linkedDomainId?.takeUnless(String::isBlank)
            if (linkedDomainId != null) {
                DomainContentRegistry.requireDomain(linkedDomainId)
            }
            for (rewardDomainId in chapter.domainRewardIds) {
                require(rewardDomainId.isNotBlank()) { "Story chapter '${chapter.id}' contains blank domainRewardIds entry" }
                DomainContentRegistry.requireDomain(rewardDomainId)
            }
            for (grantedWeapon in chapter.grantedWeapons) {
                val definition = WeaponContentRegistry.requireWeapon(grantedWeapon.weaponId)
                require(grantedWeapon.baseLevel in 1..definition.maxBaseLevel) {
                    "Story chapter '${chapter.id}' has invalid baseLevel ${grantedWeapon.baseLevel} for '${grantedWeapon.weaponId}'"
                }
                require(grantedWeapon.skillLevel in 1..definition.maxSkillLevel) {
                    "Story chapter '${chapter.id}' has invalid skillLevel ${grantedWeapon.skillLevel} for '${grantedWeapon.weaponId}'"
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

    private fun defaultBundle(): StoryContentBundle {
        return StoryContentBundle(
            chapters = listOf(
                StoryChapterDefinition(
                    id = "0-0",
                    displayName = "0-0",
                    sortOrder = 0,
                    titleTextId = "story.cresora.chapter.0_0.title",
                    domainRewardIds = listOf("rondo_forge", "masquerade_soiree"),
                    unlockRank = AdventureRankProgression.MIN_RANK,
                    prerequisiteChapterId = null,
                    preBattleStory = listOf(
                        StoryDialogueLine(speakerId = "story.cresora.chapter.0_0.speaker", textId = "story.cresora.chapter.0_0.pre_0")
                    ),
                    combatHints = emptyList(),
                    grantedWeapons = emptyList(),
                    battle = listOf(
                        StoryBattleWaveDefinition(
                            enemyRank = 2,
                            spawnDelayTicks = 40,
                            spawns = listOf(
                                StoryBattleSpawnDefinition("minecraft:zombie", 1)
                            )
                        )
                    ),
                    postBattleStory = listOf(
                        StoryDialogueLine(speakerId = "story.cresora.chapter.0_0.speaker", textId = "story.cresora.chapter.0_0.post_0")
                    ),
                    rewards = StoryRewardDefinition(
                        credits = 3450,
                        resonanceCurrencies = listOf(
                            StoryCurrencyRewardDefinition(ResonanceCurrencyType.SUBSTITUTE_CHORD, 100),
                            StoryCurrencyRewardDefinition(ResonanceCurrencyType.CHORD_PROGRESSION, 100)
                        )
                    )
                ),
                StoryChapterDefinition(
                    id = "0-1",
                    displayName = "0-1",
                    sortOrder = 10,
                    titleTextId = "story.cresora.chapter.0_1.title",
                    domainRewardIds = listOf("credit_drill"),
                    unlockRank = AdventureRankProgression.MIN_RANK,
                    prerequisiteChapterId = "0-0",
                    preBattleStory = listOf(
                        StoryDialogueLine(speakerId = "story.cresora.chapter.0_1.speaker.unknown", textId = "story.cresora.chapter.0_1.pre_0"),
                        StoryDialogueLine(speakerId = "story.cresora.chapter.0_1.speaker.unknown", textId = "story.cresora.chapter.0_1.pre_1"),
                        StoryDialogueLine(textId = "story.cresora.chapter.0_1.pre_2"),
                        StoryDialogueLine(speakerId = "story.cresora.chapter.0_1.speaker.player", textId = "story.cresora.chapter.0_1.pre_3"),
                        StoryDialogueLine(textId = "story.cresora.chapter.0_1.pre_4"),
                        StoryDialogueLine(speakerId = "story.cresora.chapter.0_1.speaker.unknown", textId = "story.cresora.chapter.0_1.pre_5"),
                        StoryDialogueLine(textId = "story.cresora.chapter.0_1.pre_6"),
                        StoryDialogueLine(textId = "story.cresora.chapter.0_1.pre_7"),
                        StoryDialogueLine(speakerId = "story.cresora.chapter.0_1.speaker.unknown", textId = "story.cresora.chapter.0_1.pre_8"),
                        StoryDialogueLine(speakerId = "story.cresora.chapter.0_1.speaker.lumine", textId = "story.cresora.chapter.0_1.pre_9"),
                        StoryDialogueLine(speakerId = "story.cresora.chapter.0_1.speaker.lumine", textId = "story.cresora.chapter.0_1.pre_10"),
                        StoryDialogueLine(speakerId = "story.cresora.chapter.0_1.speaker.lumine", textId = "story.cresora.chapter.0_1.pre_11"),
                        StoryDialogueLine(textId = "story.cresora.chapter.0_1.pre_12"),
                        StoryDialogueLine(textId = "story.cresora.chapter.0_1.pre_13")
                    ),
                    combatHints = listOf(
                        StoryDialogueLine(textId = "story.cresora.chapter.0_1.hint_0"),
                        StoryDialogueLine(textId = "story.cresora.chapter.0_1.hint_1"),
                        StoryDialogueLine(textId = "story.cresora.chapter.0_1.hint_2")
                    ),
                    grantedWeapons = listOf(
                        StoryGrantedWeaponDefinition("lakeside_stride", WeaponRarity.FIVE_STAR, 60, 10, true),
                        StoryGrantedWeaponDefinition("rondo_melody", WeaponRarity.TWO_STAR, 60, 10, true)
                    ),
                    battle = listOf(
                        StoryBattleWaveDefinition(
                            enemyRank = 30,
                            spawnDelayTicks = 40,
                            spawns = listOf(
                                StoryBattleSpawnDefinition("minecraft:zombie", 1)
                            )
                        )
                    ),
                    postBattleStory = listOf(
                        StoryDialogueLine(speakerId = "story.cresora.chapter.0_1.speaker.player", textId = "story.cresora.chapter.0_1.post_0"),
                        StoryDialogueLine(speakerId = "story.cresora.chapter.0_1.speaker.player", textId = "story.cresora.chapter.0_1.post_1"),
                        StoryDialogueLine(speakerId = "story.cresora.chapter.0_1.speaker.lumine", textId = "story.cresora.chapter.0_1.post_2"),
                        StoryDialogueLine(textId = "story.cresora.chapter.0_1.post_3"),
                        StoryDialogueLine(speakerId = "story.cresora.chapter.0_1.speaker.lumine", textId = "story.cresora.chapter.0_1.post_4"),
                        StoryDialogueLine(speakerId = "story.cresora.chapter.0_1.speaker.player", textId = "story.cresora.chapter.0_1.post_5"),
                        StoryDialogueLine(speakerId = "story.cresora.chapter.0_1.speaker.lumine", textId = "story.cresora.chapter.0_1.post_6"),
                        StoryDialogueLine(speakerId = "story.cresora.chapter.0_1.speaker.lumine", textId = "story.cresora.chapter.0_1.post_7"),
                        StoryDialogueLine(textId = "story.cresora.chapter.0_1.post_8"),
                        StoryDialogueLine(speakerId = "story.cresora.chapter.0_1.speaker.player", textId = "story.cresora.chapter.0_1.post_9"),
                        StoryDialogueLine(speakerId = "story.cresora.chapter.0_1.speaker.lumine", textId = "story.cresora.chapter.0_1.post_10"),
                        StoryDialogueLine(speakerId = "story.cresora.chapter.0_1.speaker.player", textId = "story.cresora.chapter.0_1.post_11")
                    ),
                    rewards = StoryRewardDefinition(0, emptyList())
                ),
                StoryChapterDefinition(
                    id = "0-2",
                    displayName = "0-2",
                    sortOrder = 20,
                    titleTextId = "story.cresora.chapter.0_2.title",
                    unlockRank = AdventureRankProgression.MIN_RANK,
                    prerequisiteChapterId = "0-1",
                    preBattleStory = listOf(
                        StoryDialogueLine(textId = "story.cresora.chapter.0_2.pre_0"),
                        StoryDialogueLine(textId = "story.cresora.chapter.0_2.pre_1"),
                        StoryDialogueLine(textId = "story.cresora.chapter.0_2.pre_2"),
                        StoryDialogueLine(textId = "story.cresora.chapter.0_2.pre_3"),
                        StoryDialogueLine(speakerId = "story.cresora.chapter.0_2.speaker.oldman", textId = "story.cresora.chapter.0_2.pre_4"),
                        StoryDialogueLine(speakerId = "story.cresora.chapter.0_2.speaker.player", textId = "story.cresora.chapter.0_2.pre_5"),
                        StoryDialogueLine(speakerId = "story.cresora.chapter.0_2.speaker.oldman", textId = "story.cresora.chapter.0_2.pre_6"),
                        StoryDialogueLine(speakerId = "story.cresora.chapter.0_2.speaker.oldman", textId = "story.cresora.chapter.0_2.pre_7"),
                        StoryDialogueLine(textId = "story.cresora.chapter.0_2.pre_8"),
                        StoryDialogueLine(speakerId = "story.cresora.chapter.0_2.speaker.player", textId = "story.cresora.chapter.0_2.pre_9"),
                        StoryDialogueLine(speakerId = "story.cresora.chapter.0_2.speaker.oldman", textId = "story.cresora.chapter.0_2.pre_10"),
                        StoryDialogueLine(textId = "story.cresora.chapter.0_2.pre_11"),
                        StoryDialogueLine(textId = "story.cresora.chapter.0_2.pre_12")
                    ),
                    combatHints = listOf(
                        StoryDialogueLine(textId = "story.cresora.chapter.0_2.hint_0"),
                        StoryDialogueLine(textId = "story.cresora.chapter.0_2.hint_1"),
                        StoryDialogueLine(textId = "story.cresora.chapter.0_2.hint_2")
                    ),
                    grantedWeapons = listOf(
                        StoryGrantedWeaponDefinition("rondo_melody", WeaponRarity.TWO_STAR, 60, 10, true)
                    ),
                    battleObjective = StoryBattleObjectiveDefinition(
                        type = StoryBattleObjectiveType.SURVIVE_TIME,
                        durationSeconds = 45
                    ),
                    battle = listOf(
                        StoryBattleWaveDefinition(
                            enemyRank = 50,
                            spawnDelayTicks = 40,
                            spawns = listOf(
                                StoryBattleSpawnDefinition("minecraft:skeleton", 1)
                            ),
                            modifiers = StoryBattleModifierDefinition(
                                damageReductionPercent = 100.0,
                                trueDamageImmune = true
                            )
                        )
                    ),
                    postBattleStory = listOf(
                        StoryDialogueLine(textId = "story.cresora.chapter.0_2.post_0"),
                        StoryDialogueLine(speakerId = "story.cresora.chapter.0_2.speaker.lumine", textId = "story.cresora.chapter.0_2.post_1"),
                        StoryDialogueLine(speakerId = "story.cresora.chapter.0_2.speaker.player", textId = "story.cresora.chapter.0_2.post_2"),
                        StoryDialogueLine(textId = "story.cresora.chapter.0_2.post_3"),
                        StoryDialogueLine(speakerId = "story.cresora.chapter.0_2.speaker.lumine", textId = "story.cresora.chapter.0_2.post_4"),
                        StoryDialogueLine(speakerId = "story.cresora.chapter.0_2.speaker.player", textId = "story.cresora.chapter.0_2.post_5"),
                        StoryDialogueLine(speakerId = "story.cresora.chapter.0_2.speaker.lumine", textId = "story.cresora.chapter.0_2.post_6"),
                        StoryDialogueLine(textId = "story.cresora.chapter.0_2.post_7"),
                        StoryDialogueLine(speakerId = "story.cresora.chapter.0_2.speaker.player", textId = "story.cresora.chapter.0_2.post_8"),
                        StoryDialogueLine(speakerId = "story.cresora.chapter.0_2.speaker.lumine", textId = "story.cresora.chapter.0_2.post_9")
                    ),
                    rewards = StoryRewardDefinition(0, emptyList())
                )
            )
        )
    }

    private fun chapterGroupNumericKey(groupId: String): Int = groupId.toIntOrNull() ?: Int.MAX_VALUE
}
