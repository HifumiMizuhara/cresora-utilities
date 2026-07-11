package hifumi.cresora.compiler

import com.google.gson.GsonBuilder
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import java.io.File

class MovementCompiler(
    private val inputDir: File,
    private val storyJsonFile: File,
    private val storyTextsFile: File
) {
    private val gson = GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create()

    fun compile() {
        println("Starting Cresora Movement Compiler...")
        if (!inputDir.exists()) inputDir.mkdirs()

        val allMovements = mutableListOf<MovementDefNode>()

        inputDir.walk()
            .filter { it.extension == "movement" }
            .forEach { file ->
                val source = file.readText()
                val lexer = Lexer(source)
                val tokens = lexer.scanTokens()
                val parser = Parser(source, tokens)
                val nodes = parser.parse()
                allMovements.addAll(nodes.filterIsInstance<MovementDefNode>())
            }

        if (allMovements.isEmpty()) {
            println("No movement definitions found.")
            updateStoryJson(emptyList())
            updateStoryTexts(emptyList())
            return
        }

        // Sort chapters by sortOrder, then by id to ensure a deterministic JSON output order
        allMovements.sortWith(compareBy({ it.sortOrder }, { it.id }))

        updateStoryJson(allMovements)
        updateStoryTexts(allMovements)

        println("Movement compilation finished successfully.")
    }

    private fun updateStoryJson(movements: List<MovementDefNode>) {
        val root = JsonObject()
        val chaptersArray = JsonArray()

        for (movement in movements) {
            val chapterObj = JsonObject()
            chapterObj.addProperty("id", movement.id)
            chapterObj.addProperty("displayName", movement.displayName)

            if (!movement.groupId.isNullOrBlank()) {
                chapterObj.addProperty("groupId", movement.groupId)
            }

            chapterObj.addProperty("sortOrder", movement.sortOrder)

            if (!movement.titleTextId.isNullOrBlank()) {
                chapterObj.addProperty("titleTextId", movement.titleTextId)
            }
            if (!movement.linkedDomainId.isNullOrBlank()) {
                chapterObj.addProperty("linkedDomainId", movement.linkedDomainId)
            }

            if (movement.domainRewardIds.isNotEmpty()) {
                val rewardIdsArray = JsonArray()
                movement.domainRewardIds.forEach { rewardIdsArray.add(it) }
                chapterObj.add("domainRewardIds", rewardIdsArray)
            }

            chapterObj.addProperty("unlockRank", movement.unlockRank)

            if (!movement.prerequisiteChapterId.isNullOrBlank()) {
                chapterObj.addProperty("prerequisiteChapterId", movement.prerequisiteChapterId)
            }

            // preBattleStory
            if (movement.preBattleStory.isNotEmpty()) {
                val preBattleArray = JsonArray()
                for (line in movement.preBattleStory) {
                    val lineObj = JsonObject()
                    if (!line.speakerId.isNullOrBlank()) {
                        lineObj.addProperty("speakerId", line.speakerId)
                    }
                    lineObj.addProperty("textId", line.textId)
                    preBattleArray.add(lineObj)
                }
                chapterObj.add("preBattleStory", preBattleArray)
            }

            // combatHints
            if (movement.combatHints.isNotEmpty()) {
                val combatHintsArray = JsonArray()
                for (hint in movement.combatHints) {
                    val hintObj = JsonObject()
                    hintObj.addProperty("textId", hint)
                    combatHintsArray.add(hintObj)
                }
                chapterObj.add("combatHints", combatHintsArray)
            }

            // grantedWeapons
            if (movement.grantedWeapons.isNotEmpty()) {
                val grantedWeaponsArray = JsonArray()
                for (gw in movement.grantedWeapons) {
                    grantedWeaponsArray.add(grantedWeaponJson(gw))
                }
                chapterObj.add("grantedWeapons", grantedWeaponsArray)
            }

            if (movement.resonantChordTutorialSteps.isNotEmpty()) {
                val tutorialStepsArray = JsonArray()
                for (step in movement.resonantChordTutorialSteps) {
                    val stepObj = JsonObject()
                    stepObj.addProperty("reactionKey", step.reactionKey)
                    stepObj.addProperty("effectKey", step.effectKey)
                    val weaponsArray = JsonArray()
                    step.weapons.forEach { weaponsArray.add(grantedWeaponJson(it)) }
                    stepObj.add("weapons", weaponsArray)
                    tutorialStepsArray.add(stepObj)
                }
                chapterObj.add("resonantChordTutorialSteps", tutorialStepsArray)
            }

            // battleObjective - omit if defeat_all/0 default
            if (movement.battleObjective.type != "defeat_all" || movement.battleObjective.durationSeconds != 0) {
                val objJson = JsonObject()
                objJson.addProperty("type", movement.battleObjective.type)
                objJson.addProperty("durationSeconds", movement.battleObjective.durationSeconds)
                chapterObj.add("battleObjective", objJson)
            }

            // battle waves
            if (movement.battleWaves.isNotEmpty()) {
                val battleArray = JsonArray()
                for (wave in movement.battleWaves) {
                    val waveObj = JsonObject()
                    waveObj.addProperty("enemyRank", wave.enemyRank)
                    waveObj.addProperty("spawnDelayTicks", wave.spawnDelayTicks)

                    val spawnsArray = JsonArray()
                    for (spawn in wave.spawns) {
                        val spawnObj = JsonObject()
                        spawnObj.addProperty("entityTypeId", spawn.entityTypeId)
                        spawnObj.addProperty("count", spawn.count)
                        spawnsArray.add(spawnObj)
                    }
                    waveObj.add("spawns", spawnsArray)

                    // modifiers - only write if not default
                    if (wave.modifiers.damageReductionPercent != 0.0 || wave.modifiers.trueDamageImmune) {
                        val modObj = JsonObject()
                        modObj.addProperty("damageReductionPercent", wave.modifiers.damageReductionPercent)
                        modObj.addProperty("trueDamageImmune", wave.modifiers.trueDamageImmune)
                        waveObj.add("modifiers", modObj)
                    }

                    battleArray.add(waveObj)
                }
                chapterObj.add("battle", battleArray)
            }

            // postBattleStory
            if (movement.postBattleStory.isNotEmpty()) {
                val postBattleArray = JsonArray()
                for (line in movement.postBattleStory) {
                    val lineObj = JsonObject()
                    if (!line.speakerId.isNullOrBlank()) {
                        lineObj.addProperty("speakerId", line.speakerId)
                    }
                    lineObj.addProperty("textId", line.textId)
                    postBattleArray.add(lineObj)
                }
                chapterObj.add("postBattleStory", postBattleArray)
            }

            // rewards
            val rewardsObj = JsonObject()
            rewardsObj.addProperty("credits", movement.rewards.credits)
            val currenciesArray = JsonArray()
            for ((currencyType, amount) in movement.rewards.resonanceCurrencies) {
                val currObj = JsonObject()
                currObj.addProperty("type", currencyType)
                currObj.addProperty("amount", amount)
                currenciesArray.add(currObj)
            }
            rewardsObj.add("resonanceCurrencies", currenciesArray)
            chapterObj.add("rewards", rewardsObj)

            chaptersArray.add(chapterObj)
        }

        root.add("chapters", chaptersArray)
        storyJsonFile.parentFile.mkdirs()
        storyJsonFile.writeText(gson.toJson(root) + "\n")
    }

    private fun updateStoryTexts(movements: List<MovementDefNode>) {
        val root = JsonObject()
        root.addProperty("fallbackLocale", "ja_jp")
        root.add("texts", JsonObject())
        val textsObj = root.getAsJsonObject("texts")

        for (movement in movements) {
            for ((locale, translationsMap) in movement.translations) {
                if (!textsObj.has(locale)) {
                    textsObj.add(locale, JsonObject())
                }
                val localeObj = textsObj.getAsJsonObject(locale)
                for ((key, value) in translationsMap) {
                    if (localeObj.has(key)) {
                        val existingValue = localeObj.get(key).asString
                        if (existingValue != value) {
                            println("Warning: Translation key '$key' under locale '$locale' conflict detected! Overwriting '$existingValue' with '$value'.")
                        }
                    }
                    localeObj.addProperty(key, value)
                }
            }
        }

        storyTextsFile.parentFile.mkdirs()
        storyTextsFile.writeText(gson.toJson(root) + "\n")
    }

    private fun mapRarity(rarity: String): String = when (rarity.uppercase()) {
        "FIVE_STAR", "5_STAR" -> "5_star"
        "FOUR_STAR", "4_STAR" -> "4_star"
        "THREE_STAR", "3_STAR" -> "3_star"
        "TWO_STAR", "2_STAR" -> "2_star"
        else -> rarity.lowercase()
    }

    private fun grantedWeaponJson(weapon: GrantedWeaponNode): JsonObject = JsonObject().apply {
        addProperty("weaponId", weapon.weaponId)
        addProperty("rarity", mapRarity(weapon.rarity))
        addProperty("baseLevel", weapon.baseLevel)
        addProperty("skillLevel", weapon.skillLevel)
        addProperty("removeOnExit", weapon.removeOnExit)
    }
}
