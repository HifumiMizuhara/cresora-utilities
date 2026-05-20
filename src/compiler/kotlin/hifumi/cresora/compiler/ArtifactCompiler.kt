package hifumi.cresora.compiler

import com.google.gson.GsonBuilder
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.squareup.kotlinpoet.*
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import java.io.File

class ArtifactCompiler(
    private val inputDir: File,
    private val outputDir: File,
    private val jsonFile: File
) {
    private val gson = GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create()
    private val artifactRegistryDir = File(outputDir, "hifumi/cresora/equipment/generated")

    fun compile() {
        println("Starting Cresora Artifact Compiler...")
        if (!artifactRegistryDir.exists()) artifactRegistryDir.mkdirs()

        // Clear old generated files
        artifactRegistryDir.listFiles()?.forEach { it.delete() }

        val allArtifacts = mutableListOf<ArtifactDefNode>()
        inputDir.walk()
            .filter { it.extension == "artifact" || it.extension == "cresora" }
            .forEach { file ->
                val source = file.readText()
                val lexer = Lexer(source)
                val tokens = lexer.scanTokens()
                val parser = Parser(source, tokens)
                val nodes = parser.parse()
                allArtifacts.addAll(nodes.filterIsInstance<ArtifactDefNode>())
            }

        if (allArtifacts.isEmpty()) {
            println("No artifact definitions found.")
            return
        }

        val generatedHandlers = mutableListOf<Pair<String, String>>() // effectId to ClassName

        allArtifacts.forEach { artifact ->
            println("Compiling artifact set: ${artifact.name}...")
            artifact.bonuses.forEach { bonus ->
                println("  Bonus ${bonus.requiredPieces}pc: ${bonus.handlers.size} handlers, ${bonus.buffs.size} buffs")
                bonus.handlers.forEach { handler ->
                    val effectId = "${artifact.id}_${bonus.requiredPieces}pc_${handler.eventName}"
                    val className = "ArtifactSkill${artifact.id.split("_").joinToString("") { it.replaceFirstChar { c -> c.uppercase() } }}${bonus.requiredPieces}pc${handler.eventName.split("_").joinToString("") { it.replaceFirstChar { c -> c.uppercase() } }}"
                    generateArtifactHookClass(artifact, bonus, handler, effectId, className)
                    generatedHandlers.add(effectId to className)
                }
            }
        }

        generateRegistry(generatedHandlers)
        updateArtifactJson(allArtifacts)
        updateLangFiles(allArtifacts)
        println("Artifact compilation finished successfully.")
    }

    private fun generateArtifactHookClass(
        artifact: ArtifactDefNode,
        bonus: ArtifactBonusNode,
        handler: SkillHandlerNode,
        effectId: String,
        className: String
    ) {
        val packageName = "hifumi.cresora.equipment.generated"
        val file = FileSpec.builder(packageName, className)
            .addDefaultImports()

        val classSpec = TypeSpec.classBuilder(className)
            .addSuperinterface(ClassName("hifumi.cresora.equipment", "ArtifactSkillHandler"))

        val funSpecs = mutableMapOf<String, FunSpec.Builder>()

        setupArtifactCommon(classSpec, bonus, packageName, className, funSpecs)

        val handlerGroups = bonus.handlers.groupBy { it.eventName }
        for ((eventName, handlers) in handlerGroups) {
            val methodName = mapEventName(eventName)
            val funSpec = funSpecs.getOrPut(methodName) {
                FunSpec.builder(methodName)
                    .addModifiers(KModifier.OVERRIDE).also { addParamsForEvent(eventName, it) }
            }

            for (h in handlers) {
                h.actions.forEach { action ->
                    emitAction(action, funSpec, bonus, packageName, className, eventName)
                }
            }
        }

        funSpecs.values.forEach { classSpec.addFunction(it.build()) }
        
        file.addType(classSpec.build())
        file.build().writeTo(outputDir)
    }

    private fun setupArtifactCommon(
        typeSpec: TypeSpec.Builder,
        bonus: ArtifactBonusNode,
        packageName: String,
        className: String,
        funSpecs: MutableMap<String, FunSpec.Builder>
    ) {
        // 1. Generate Buff Data Classes and Maps
        for (buff in bonus.buffs) {
            val stateClassName = buff.id.split("_").joinToString("") { it.replaceFirstChar { c -> c.uppercase() } } + "State"
            val stateClass = TypeSpec.classBuilder(stateClassName)
                .addModifiers(KModifier.DATA)
                .primaryConstructor(FunSpec.constructorBuilder()
                    .addParameter("expireTick", Long::class)
                    .addParameter("stacks", Int::class)
                    .build())
                .addProperty(PropertySpec.builder("expireTick", Long::class).initializer("expireTick").mutable(true).build())
                .addProperty(PropertySpec.builder("stacks", Int::class).initializer("stacks").mutable(true).build())
                .build()
            typeSpec.addType(stateClass)
            
            val mapName = camelCase(buff.id) + "States"
            val mapType = ClassName("kotlin.collections", "MutableMap").parameterizedBy(
                ClassName("java.util", "UUID"),
                ClassName(packageName, "$className.$stateClassName")
            )
            typeSpec.addProperty(PropertySpec.builder(mapName, mapType)
                .initializer("mutableMapOf()")
                .build())
        }

        // 2. onTick for expiry (Artifacts use onTick(player))
        if (bonus.buffs.isNotEmpty()) {
            val onTickFun = funSpecs.getOrPut("onTick") {
                FunSpec.builder("onTick")
                    .addModifiers(KModifier.OVERRIDE)
                    .addParameter("player", ClassName("net.minecraft.server.network", "ServerPlayerEntity"))
            }

            onTickFun.addStatement("val now = %T.currentWorldTime(player)", ClassName("hifumi.cresora", "WeaponSkillService"))
            for (buff in bonus.buffs) {
                val mapName = camelCase(buff.id) + "States"
                val buffNameKey = buff.translationKey ?: "item.cresora.artifact.skill.buff.${buff.id}.name"
                onTickFun.beginControlFlow("if ($mapName.containsKey(player.uuid) && now >= $mapName[player.uuid]!!.expireTick)")
                onTickFun.addStatement("$mapName.remove(player.uuid)")
                onTickFun.addStatement("player.sendMessage(%T.translatable(%S, %T.translatable(%S)), true)",
                    ClassName("net.minecraft.text", "Text"), "item.cresora.artifact.skill.buff.${buff.id}.expired",
                    ClassName("net.minecraft.text", "Text"), buffNameKey)
                onTickFun.endControlFlow()
            }

            val clearTransientStateFun = FunSpec.builder("clearTransientState")
                .addModifiers(KModifier.OVERRIDE)
                .addParameter("playerId", ClassName("java.util", "UUID"))
            for (buff in bonus.buffs) {
                val mapName = camelCase(buff.id) + "States"
                clearTransientStateFun.addStatement("$mapName.remove(playerId)")
            }
            typeSpec.addFunction(clearTransientStateFun.build())

            val pruneTransientStateFun = FunSpec.builder("pruneTransientState")
                .addModifiers(KModifier.OVERRIDE)
                .addParameter(
                    "activePlayerIds",
                    ClassName("kotlin.collections", "Set").parameterizedBy(ClassName("java.util", "UUID"))
                )
            for (buff in bonus.buffs) {
                val mapName = camelCase(buff.id) + "States"
                pruneTransientStateFun.addStatement("$mapName.keys.removeIf { !activePlayerIds.contains(it) }")
            }
            typeSpec.addFunction(pruneTransientStateFun.build())
        }

        // 3. Scalar overrides
        generateScalarOverride(typeSpec, bonus, "getAttackDamageScalar", listOf("attack_percent_per_stack", "atk_percent_per_stack"), packageName, className)
        generateScalarOverride(typeSpec, bonus, "getArmorScalar", listOf("armor_per_stack", "defense_per_stack", "def_per_stack"), packageName, className)
        generateScalarOverride(typeSpec, bonus, "getCritRateBonus", listOf("crit_rate_per_stack"), packageName, className)
        generateScalarOverride(typeSpec, bonus, "getCritDamageBonus", listOf("crit_dmg_per_stack", "crit_damage_per_stack"), packageName, className)
    }

    private fun emitAction(
        action: ActionNode,
        funSpec: FunSpec.Builder,
        bonus: ArtifactBonusNode?,
        packageName: String,
        className: String,
        eventName: String
    ) {
        when (action) {
            is ExecuteActionNode -> {
                action.statements.forEach { stmt ->
                    emitAction(stmt, funSpec, bonus, packageName, className, eventName)
                }
            }
            is CommandActionNode -> {
                when (action.commandName) {
                    "add_buff" -> emitAddBuff(action.arguments, funSpec, bonus, className)
                    "send_message" -> {
                        val key = action.arguments[0].removeSurrounding("\"")
                        val color = action.arguments.getOrNull(1)?.removeSurrounding("\"")?.uppercase() ?: "WHITE"
                        funSpec.addStatement("player.sendMessage(%T.translatable(%S).formatted(%T.$color), true)",
                            ClassName("net.minecraft.text", "Text"), key, ClassName("net.minecraft.util", "Formatting"))
                    }
                    else -> {
                         // Fallback for command actions that might be in InstructionMapping
                         val expanded = InstructionMapping.expand(action.commandName, action.arguments, CompilerContext.ARTIFACT)
                         funSpec.addStatement("%L", expanded)
                    }
                }
            }
            is InstructionCallNode -> {
                when (action.functionName) {
                    "add_buff" -> emitAddBuff(action.arguments, funSpec, bonus, className)
                    "send_message" -> {
                        val key = action.arguments[0].removeSurrounding("\"")
                        val color = action.arguments.getOrNull(1)?.removeSurrounding("\"")?.uppercase() ?: "WHITE"
                        funSpec.addStatement("player.sendMessage(%T.translatable(%S).formatted(%T.$color), true)",
                            ClassName("net.minecraft.text", "Text"), key, ClassName("net.minecraft.util", "Formatting"))
                    }
                    else -> {
                        val expanded = InstructionMapping.expand(action.functionName, action.arguments, CompilerContext.ARTIFACT)
                        funSpec.addStatement("%L", expanded)
                    }
                }
            }
            is SendLocalizedMessageActionNode -> {
                val key = action.key
                val color = action.color.uppercase()
                val argsList = action.arguments.joinToString(", ") { arg -> arg }
                val comma = if (argsList.isNotEmpty()) ", " else ""
                funSpec.addStatement("player.sendMessage(%T.translatable(%S$comma$argsList).formatted(%T.$color), true)",
                    ClassName("net.minecraft.text", "Text"), key, ClassName("net.minecraft.util", "Formatting"))
            }
            is AreaOfEffectActionNode -> {
                val radius = action.radius
                funSpec.addCode(
                    """
                    |player.world.getNonSpectatingEntities(net.minecraft.entity.LivingEntity::class.java, player.boundingBox.expand($radius.toDouble())).forEach { target ->
                    |    if (target != player) {
                    |        // area_of_effect block
                    |""".trimMargin()
                )
                action.actions.forEach { nested ->
                    emitAction(nested, funSpec, bonus, packageName, className, eventName)
                }
                funSpec.addCode(
                    """
                    |    }
                    |}
                    |
                    """.trimMargin()
                )
            }
            is CloseSkillMenuActionNode -> {
                funSpec.addStatement("%T.restoreHotbar(player)", ClassName("hifumi.cresora", "HotbarOverrideService"))
            }
            is ExpressionNode -> {
                var content = action.content
                if (bonus != null) {
                    for (buff in bonus.buffs) {
                        val mapName = camelCase(buff.id) + "States"
                        val stateClassName = buff.id.split("_").joinToString("") { it.replaceFirstChar { c -> c.uppercase() } } + "State"
                        content = content.replace(Regex("\\b$mapName\\b"), "$className.$mapName")
                        content = content.replace(Regex("\\b$stateClassName\\b"), "$className.$stateClassName")
                    }
                }
                funSpec.addStatement("%L", content)
            }
            else -> {}
        }
    }

    private fun emitAddBuff(arguments: List<String>, funSpec: FunSpec.Builder, bonus: ArtifactBonusNode?, className: String) {
        if (bonus != null) {
            val buffId = arguments[0].removeSurrounding("\"")
            val amountStr = arguments.getOrNull(1) ?: "1"
            val buff = bonus.buffs.find { it.id == buffId }
            if (buff != null) {
                val mapName = camelCase(buff.id) + "States"
                val stateClassName = buff.id.split("_").joinToString("") { it.replaceFirstChar { c -> c.uppercase() } } + "State"
                val buffNameKey = buff.translationKey ?: "item.cresora.artifact.skill.buff.${buff.id}.name"
                funSpec.addCode("""
                    |run {
                    |    val now = %T.currentWorldTime(player)
                    |    val state = this.$mapName.getOrPut(player.uuid) { $stateClassName(0L, 0) }
                    |    state.expireTick = now + ${buff.durationSeconds.toInt()} * 20L
                    |    state.stacks = (state.stacks + $amountStr).coerceAtMost(${buff.maxStacks})
                    |    player.sendMessage(%T.translatable("item.cresora.artifact.skill.buff.${buff.id}.gained", %T.translatable("$buffNameKey"), state.stacks), true)
                    |}
                    |
                """.trimMargin(), ClassName("hifumi.cresora", "WeaponSkillService"), ClassName("net.minecraft.text", "Text"), ClassName("net.minecraft.text", "Text"))
            } else {
                funSpec.addStatement("// Buff $buffId not found")
            }
        }
    }

    private fun camelCase(id: String): String {
        val parts = id.split("_")
        if (parts.isEmpty()) return ""
        return parts[0] + parts.drop(1).joinToString("") { it.replaceFirstChar { c -> c.uppercase() } }
    }

    private fun generateRegistry(handlers: List<Pair<String, String>>) {
        val packageName = "hifumi.cresora.equipment.generated"
        val file = FileSpec.builder(packageName, "CompiledArtifactRegistry")
        
        val typeSpec = TypeSpec.objectBuilder("CompiledArtifactRegistry")
        val registerFn = FunSpec.builder("registerAll")
            .addParameter("registry", ClassName("hifumi.cresora.equipment", "ArtifactSkillRegistry"))
        
        handlers.forEach { (effectId, className) ->
            registerFn.addStatement("registry.register(%S, %T())", effectId, ClassName(packageName, className))
        }
        
        typeSpec.addFunction(registerFn
            .addAnnotation(ClassName("kotlin.jvm", "JvmStatic"))
            .build())
        file.addType(typeSpec.build())
        file.build().writeTo(outputDir)
    }

    private fun updateArtifactJson(artifacts: List<ArtifactDefNode>) {
        val root = JsonObject()
        root.add("slots", JsonArray()) // Compiled artifacts don't define slots usually
        
        val setsArray = JsonArray()
        for (artifact in artifacts) {
            val setObj = JsonObject()
            setObj.addProperty("id", artifact.id)
            setObj.addProperty("translationKey", "item.cresora-utilities.equipment.set.${artifact.id}")
            
            val bonusesArray = JsonArray()
            for (bonus in artifact.bonuses) {
                val bonusObj = JsonObject()
                bonusObj.addProperty("requiredPieces", bonus.requiredPieces)
                
                val statsArray = JsonArray()
                bonus.stats.forEach { (type, value) ->
                    val statObj = JsonObject()
                    statObj.addProperty("type", type.lowercase())
                    statObj.addProperty("value", value)
                    statsArray.add(statObj)
                }
                bonusObj.add("stats", statsArray)
                
                val hooksArray = JsonArray()
                bonus.handlers.forEach { handler ->
                    val hookObj = JsonObject()
                    hookObj.addProperty("trigger", mapTrigger(handler.eventName))
                    hookObj.addProperty("effectId", "${artifact.id}_${bonus.requiredPieces}pc_${handler.eventName}")
                    hookObj.add("parameters", JsonObject())
                    hooksArray.add(hookObj)
                }
                bonusObj.add("effectHooks", hooksArray)
                
                bonusesArray.add(bonusObj)
            }
            setObj.add("bonuses", bonusesArray) // Matches EquipmentSet codec "bonuses" field
            setsArray.add(setObj)
        }
        root.add("sets", setsArray)
        root.add("equipmentDefinitions", JsonArray())
        root.add("dropProfiles", JsonArray())
        root.add("mobLoot", JsonArray())

        jsonFile.parentFile.mkdirs()
        jsonFile.writeText(gson.toJson(root))
    }

    private fun mapEventName(dslName: String): String = when (dslName) {
        "on_attack_dealt" -> "onAttackDealt"
        "on_damage_taken" -> "onDamageTaken"
        "on_equip_changed" -> "onEquipChanged"
        "on_tick" -> "onTick"
        "on_kill" -> "onKill"
        else -> dslName
    }

    private fun addParamsForEvent(eventName: String, builder: FunSpec.Builder) {
        when (eventName) {
            "on_attack_dealt" -> {
                builder.addParameter("player", ClassName("net.minecraft.server.network", "ServerPlayerEntity"))
                builder.addParameter("target", ClassName("net.minecraft.entity", "LivingEntity"))
                builder.addParameter("damage", Double::class)
            }
            "on_damage_taken" -> {
                builder.addParameter("player", ClassName("net.minecraft.server.network", "ServerPlayerEntity"))
                builder.addParameter("source", ClassName("net.minecraft.entity.damage", "DamageSource"))
                builder.addParameter("damage", Double::class)
            }
            "on_kill" -> {
                builder.addParameter("player", ClassName("net.minecraft.server.network", "ServerPlayerEntity"))
                builder.addParameter("target", ClassName("net.minecraft.entity", "LivingEntity"))
            }
            "on_equip_changed", "on_tick" -> {
                builder.addParameter("player", ClassName("net.minecraft.server.network", "ServerPlayerEntity"))
            }
        }
    }

    private fun mapTrigger(dslName: String): String = when (dslName) {
        "on_attack_dealt" -> "attack_dealt"
        "on_damage_taken" -> "damage_taken"
        "on_equip_changed" -> "equip_changed"
        "on_tick" -> "tick"
        "on_kill" -> "kill"
        else -> dslName.removePrefix("on_")
    }

    private fun FileSpec.Builder.addDefaultImports(): FileSpec.Builder {
        return this
            .addImport("net.minecraft.text", "Text")
            .addImport("net.minecraft.util", "Identifier")
            .addImport("net.minecraft.util", "Formatting")
            .addImport("net.minecraft.util", "ActionResult")
            .addImport("net.minecraft.server.world", "ServerWorld")
            .addImport("net.minecraft.server.network", "ServerPlayerEntity")
            .addImport("net.minecraft.entity", "LivingEntity")
            .addImport("net.minecraft.entity.effect", "StatusEffectInstance")
            .addImport("net.minecraft.entity.effect", "StatusEffects")
            .addImport("net.minecraft.registry", "Registries")
            .addImport("net.minecraft.particle", "ParticleTypes")
            .addImport("hifumi.cresora", "WeaponSkillService")
            .addImport("hifumi.cresora", "CresoraDebuffService")
            .addImport("hifumi.cresora", "EquipmentEffectHookService")
    }

    private fun generateScalarOverride(
        typeSpec: TypeSpec.Builder,
        bonus: ArtifactBonusNode,
        methodName: String,
        statKeys: List<String>,
        packageName: String,
        className: String
    ) {
        val buffsWithStat = bonus.buffs.filter { b -> statKeys.any { b.stats.containsKey(it) } }
        if (buffsWithStat.isNotEmpty()) {
            val funSpec = FunSpec.builder(methodName)
                .addModifiers(KModifier.OVERRIDE)
                .addParameter("player", ClassName("net.minecraft.server.network", "ServerPlayerEntity"))
                .returns(Double::class)

            funSpec.addStatement("var total = 0.0")
            for (buff in buffsWithStat) {
                val mapName = camelCase(buff.id) + "States"
                for (statKey in statKeys) {
                    if (buff.stats.containsKey(statKey)) {
                        funSpec.addStatement("total += $mapName[player.uuid]?.let { it.stacks * ${buff.stats[statKey]} } ?: 0.0")
                    }
                }
            }
            funSpec.addStatement("return total")
            typeSpec.addFunction(funSpec.build())
        }
    }

    private fun updateLangFiles(artifacts: List<ArtifactDefNode>) {
        val langDir = File(jsonFile.parentFile.parentFile.parentFile.parentFile, "assets/cresora-utilities/lang")
        if (!langDir.exists()) return

        val locales = artifacts.flatMap { it.translations.keys }.distinct()
        for (locale in locales) {
            val langFile = File(langDir, "$locale.json")
            val langJson = if (langFile.exists()) {
                JsonParser.parseString(langFile.readText()).asJsonObject
            } else {
                JsonObject()
            }

            for (artifact in artifacts) {
                // Set name
                langJson.addProperty("item.cresora-utilities.equipment.set.${artifact.id}", artifact.name)
                
                artifact.translations[locale]?.forEach { (k, v) ->
                    langJson.addProperty(k, v)
                }

                // Add buff names and messages
                for (bonus in artifact.bonuses) {
                    for (buff in bonus.buffs) {
                        val nameKey = "item.cresora.artifact.skill.buff.${buff.id}.name"
                        if (!langJson.has(nameKey)) {
                             langJson.addProperty(nameKey, buff.id.split("_").joinToString(" ") { it.replaceFirstChar { c -> c.uppercase() } })
                        }
                        val gainedKey = "item.cresora.artifact.skill.buff.${buff.id}.gained"
                        if (!langJson.has(gainedKey)) {
                            val msg = when (locale) {
                                "ja_jp" -> "効果獲得: %s (%s 層)"
                                "zh_cn" -> "获得效果: %s (%s 层)"
                                else -> "Gained effect: %s (%s stacks)"
                            }
                            langJson.addProperty(gainedKey, msg)
                        }
                        val expiredKey = "item.cresora.artifact.skill.buff.${buff.id}.expired"
                        if (!langJson.has(expiredKey)) {
                            val msg = when (locale) {
                                "ja_jp" -> "効果終了: %s"
                                "zh_cn" -> "效果结束: %s"
                                else -> "Effect expired: %s"
                            }
                            langJson.addProperty(expiredKey, msg)
                        }
                    }
                }
            }

            langFile.writeText(gson.toJson(langJson) + "\n")
        }
    }
}
