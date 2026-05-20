package hifumi.cresora.compiler

import com.google.gson.GsonBuilder
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.squareup.kotlinpoet.*
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import java.io.File

class CresoraCompiler(
    private val inputDir: File,
    private val outputDir: File,
    private val weaponJsonFile: File
) {
    private val gson = GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create()

    fun compile() {
        // Clear generated directory before starting
        val generatedDir = File(outputDir, "hifumi/cresora/skill/generated")
        if (generatedDir.exists()) {
            println("Clearing old generated files in ${generatedDir.absolutePath}...")
            generatedDir.deleteRecursively()
        }
        generatedDir.mkdirs()

        if (!inputDir.exists()) inputDir.mkdirs()
        val cresoraFiles = inputDir.listFiles { _, name -> name.endsWith(".cresora") } ?: return
        val allWeapons = mutableListOf<WeaponDefNode>()
        val allDictionaries = mutableListOf<DictionaryDefNode>()

        for (file in cresoraFiles) {
            println("Compiling ${file.name}...")
            val source = file.readText()
            val lexer = Lexer(source)
            val tokens = lexer.scanTokens()
            val parser = Parser(source, tokens)
            val nodes = parser.parse()
            nodes.forEach { node ->
                when (node) {
                    is WeaponDefNode -> allWeapons.add(node)
                    is DictionaryDefNode -> allDictionaries.add(node)
                    else -> {}
                }
            }
        }

        if (allWeapons.isEmpty() && allDictionaries.isEmpty()) {
            println("No .cresora files found or no content defined.")
            // Still generate empty registry and JSON to ensure consistency
            generateKotlinCode(emptyList())
            updateWeaponJson(emptyList())
            updateRegistry(emptyList())
            return
        }

        generateKotlinCode(allWeapons)
        updateWeaponJson(allWeapons)
        updateRegistry(allWeapons)
        updateLangFiles(allWeapons, allDictionaries)
        updateItemModels(allWeapons)
    }

    private fun generateKotlinCode(weapons: List<WeaponDefNode>) {
        val packageName = "hifumi.cresora.skill.generated"
        for (weapon in weapons) {
            // Main Skill
            weapon.skill?.let { generateSkillClass(weapon, it, packageName) }

            // Sub Skills
            for (subSkill in weapon.subSkills) {
                generateSubSkillClass(weapon, subSkill, packageName)
            }
        }
    }

    private fun generateSkillClass(weapon: WeaponDefNode, skill: SkillNode, packageName: String) {
        val className = "${weapon.name.replace("[ ,']".toRegex(), "")}Skill"
        val typeSpec = TypeSpec.objectBuilder(className)
            .addSuperinterface(ClassName("hifumi.cresora.skill", "WeaponSkillHandler"))

        val funSpecs = mutableMapOf<String, FunSpec.Builder>()

        setupSkillCommon(typeSpec, skill, packageName, className, funSpecs)

        val handlerGroups = skill.handlers.groupBy { it.eventName }
        for ((eventName, handlers) in handlerGroups) {
            val methodName = mapEventName(eventName)
            val funSpec = funSpecs.getOrPut(methodName) {
                FunSpec.builder(methodName)
                    .addModifiers(KModifier.OVERRIDE).also { addParamsForEvent(eventName, it) }
            }

            for (handler in handlers) {
                handler.actions.forEach {
                    emitAction(it, funSpec, skill, packageName, className, eventName)
                    funSpec.addCode("\n")
                }
            }

            if (eventName == "on_activate") {
                funSpec.addCode("\n")
                funSpec.addStatement("return %T.SUCCESS", ClassName("net.minecraft.util", "ActionResult"))
            } else if ((eventName == "on_damage_absorbed" || eventName == "on_damage_taken") &&
                handlers.none { handler -> handler.actions.any { it is ExecuteActionNode } }
            ) {
                funSpec.addStatement("return amount")
            }
        }

        funSpecs.values.forEach { typeSpec.addFunction(it.build()) }

        FileSpec.builder(packageName, className)
            .addDefaultImports()
            .addType(typeSpec.build())
            .build()
            .writeTo(outputDir)
    }

    private fun generateSubSkillClass(weapon: WeaponDefNode, subSkill: SubSkillNode, packageName: String) {
        val className = "${subSkill.name.replace("[ ,']".toRegex(), "")}Skill"
        val mainSkillClassName = "${weapon.name.replace("[ ,']".toRegex(), "")}Skill"
        val typeSpec = TypeSpec.objectBuilder(className)
            .addSuperinterface(ClassName("hifumi.cresora.skill", "WeaponSkillHandler"))

        val funSpecs = mutableMapOf<String, FunSpec.Builder>()

        for (handler in subSkill.handlers) {
            val methodName = mapEventName(handler.eventName)
            val funSpec = funSpecs.getOrPut(methodName) {
                FunSpec.builder(methodName)
                    .addModifiers(KModifier.OVERRIDE).also { addParamsForEvent(handler.eventName, it) }
            }

            handler.actions.forEach {
                emitAction(it, funSpec, weapon.skill, packageName, mainSkillClassName, handler.eventName)
                funSpec.addCode("\n")
            }

            if (handler.eventName == "on_activate") {
                funSpec.addCode("\n")
                funSpec.addStatement("return %T.SUCCESS", ClassName("net.minecraft.util", "ActionResult"))
            } else if ((handler.eventName == "on_damage_absorbed" || handler.eventName == "on_damage_taken") &&
                handler.actions.none { it is ExecuteActionNode }
            ) {
                funSpec.addStatement("return amount")
            }
        }

        funSpecs.values.forEach { typeSpec.addFunction(it.build()) }

        FileSpec.builder(packageName, className)
            .addDefaultImports()
            .addType(typeSpec.build())
            .build()
            .writeTo(outputDir)
    }

    private fun setupSkillCommon(typeSpec: TypeSpec.Builder, skill: SkillNode, packageName: String, className: String, funSpecs: MutableMap<String, FunSpec.Builder>) {
        // 1. Generate Buff Data Classes and Maps
        for (buff in skill.buffs) {
            val stateClassName = "${buff.id.split("_").joinToString("") { it.replaceFirstChar { c -> c.uppercase() } }}State"
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

            val mapName = "${buff.id.split("_").joinToString("") { if (it == buff.id.split("_")[0]) it else it.replaceFirstChar { c -> c.uppercase() } }}States"
            val mapType = ClassName("kotlin.collections", "MutableMap").parameterizedBy(
                ClassName("java.util", "UUID"),
                ClassName(packageName, "$className.$stateClassName")
            )
            typeSpec.addProperty(PropertySpec.builder(mapName, mapType)
                .initializer("mutableMapOf()")
                .build())
        }

        // 2. onPlayerTick for expiry
        if (skill.buffs.isNotEmpty()) {
            val onPlayerTickFun = funSpecs.getOrPut("onPlayerTick") {
                FunSpec.builder("onPlayerTick")
                    .addModifiers(KModifier.OVERRIDE)
                    .addParameter("player", ClassName("net.minecraft.server.network", "ServerPlayerEntity"))
                    .addParameter("definition", ClassName("hifumi.cresora", "WeaponDefinition"))
                    .addParameter("data", ClassName("hifumi.cresora", "WeaponData"))
            }

            onPlayerTickFun.addStatement("val now = %T.currentWorldTime(player)", ClassName("hifumi.cresora", "WeaponSkillService"))
            for (buff in skill.buffs) {
                val mapName = "${buff.id.split("_").joinToString("") { if (it == buff.id.split("_")[0]) it else it.replaceFirstChar { c -> c.uppercase() } }}States"
                val buffNameKey = buff.translationKey ?: "item.cresora.weapon.skill.buff.${buff.id}.name"
                onPlayerTickFun.addCode("\n; ")
                onPlayerTickFun.beginControlFlow("if ($mapName.containsKey(player.uuid) && now >= $mapName[player.uuid]!!.expireTick)")
                onPlayerTickFun.addStatement("$mapName.remove(player.uuid)")
                onPlayerTickFun.addStatement("player.sendMessage(%T.translatable(%S, %T.translatable(%S)), true)",
                    ClassName("net.minecraft.text", "Text"), "item.cresora.weapon.skill.buff.${buff.id}.expired",
                    ClassName("net.minecraft.text", "Text"), buffNameKey)
                onPlayerTickFun.endControlFlow()
                onPlayerTickFun.addCode("; \n")
            }

            val clearTransientStateFun = FunSpec.builder("clearTransientState")
                .addModifiers(KModifier.OVERRIDE)
                .addParameter("playerId", ClassName("java.util", "UUID"))
            for (buff in skill.buffs) {
                val mapName = "${buff.id.split("_").joinToString("") { if (it == buff.id.split("_")[0]) it else it.replaceFirstChar { c -> c.uppercase() } }}States"
                clearTransientStateFun.addStatement("$mapName.remove(playerId)")
            }
            typeSpec.addFunction(clearTransientStateFun.build())

            val pruneTransientStateFun = FunSpec.builder("pruneTransientState")
                .addModifiers(KModifier.OVERRIDE)
                .addParameter(
                    "activePlayerIds",
                    ClassName("kotlin.collections", "Set").parameterizedBy(ClassName("java.util", "UUID"))
                )
            for (buff in skill.buffs) {
                val mapName = "${buff.id.split("_").joinToString("") { if (it == buff.id.split("_")[0]) it else it.replaceFirstChar { c -> c.uppercase() } }}States"
                pruneTransientStateFun.addStatement("$mapName.keys.removeIf { !activePlayerIds.contains(it) }")
            }
            typeSpec.addFunction(pruneTransientStateFun.build())
        }

        // 3. Scalar overrides
        generateScalarOverride(typeSpec, skill, "getAttackDamageScalar", "attack_percent_per_stack", packageName, className)
        generateScalarOverride(typeSpec, skill, "getArmorScalar", "armor_per_stack", packageName, className)
        generateScalarOverride(typeSpec, skill, "getCritRateBonus", "crit_rate_per_stack", packageName, className)
        generateScalarOverride(typeSpec, skill, "getCritDamageBonus", "crit_dmg_per_stack", packageName, className)
    }

    private fun emitAction(
        action: ActionNode,
        funSpec: FunSpec.Builder,
        skill: SkillNode?,
        packageName: String,
        className: String,
        eventName: String
    ) {
        when (action) {
            is CommandActionNode -> {
                when (action.commandName) {
                    "add_buff" -> {
                        if (skill != null) {
                            val buffId = action.arguments[0].removeSurrounding("\"")
                            val amountStr = action.arguments.getOrNull(1) ?: "1"
                            val buff = skill.buffs.find { it.id == buffId }
                            if (buff != null) {
                                val mapName = "${buff.id.split("_").joinToString("") { if (it == buff.id.split("_")[0]) it else it.replaceFirstChar { c -> c.uppercase() } }}States"
                                val stateClassName = "${buff.id.split("_").joinToString("") { it.replaceFirstChar { c -> c.uppercase() } }}State"
                                val buffNameKey = buff.translationKey ?: "item.cresora.weapon.skill.buff.${buff.id}.name"
                                funSpec.addCode("""
                                    |; {
                                    |    val now = %T.currentWorldTime(player)
                                    |    val state = $className.$mapName.getOrPut(player.uuid) { $className.$stateClassName(0L, 0) }
                                    |    state.expireTick = now + ${buff.durationSeconds.toInt()} * 20L
                                    |    state.stacks = (state.stacks + $amountStr).coerceAtMost(${buff.maxStacks})
                                    |    player.sendMessage(%T.translatable("item.cresora.weapon.skill.buff.${buff.id}.gained", %T.translatable("$buffNameKey"), state.stacks), true)
                                    |}
                                """.trimMargin(), ClassName("hifumi.cresora", "WeaponSkillService"), ClassName("net.minecraft.text", "Text"), ClassName("net.minecraft.text", "Text"))
                            } else {
                                funSpec.addStatement("// Buff $buffId not found")
                            }
                        }
                    }
                    "heal" -> {
                        val amount = action.arguments[0].let { if (it == "skill_value") "%T.healHp(definition, data)" else "$it.toFloat()" }
                        funSpec.addStatement("player.heal($amount)", ClassName("hifumi.cresora", "WeaponCombatSupport"))
                    }
                    "grant_shield" -> {
                        val amount = action.arguments[0].let { if (it == "skill_value") "%T.shieldHp(definition, data)" else "$it.toFloat()" }
                        val duration = action.arguments[1].removeSuffix("s").let { if (it == "skill_duration") "definition.skill.durationSeconds.toLong()" else "$it.toLong()" }
                        funSpec.addStatement("(player as %T).cresoraSetShieldHp($amount)", ClassName("hifumi.cresora", "WeaponSkillAccess"), ClassName("hifumi.cresora", "WeaponCombatSupport"))
                        funSpec.addStatement("(player as %T).cresoraSetShieldExpireTick(%T.currentWorldTime(player) + $duration * 20L)", ClassName("hifumi.cresora", "WeaponSkillAccess"), ClassName("hifumi.cresora", "WeaponSkillService"))
                    }
                    "start_cooldown" -> {
                        funSpec.addStatement("%T.startCooldown(player, definition.id, definition.skill.cooldownSeconds * 20L)", ClassName("hifumi.cresora", "WeaponSkillService"))
                        funSpec.addStatement("%T.showCooldownBar(player, definition)", ClassName("hifumi.cresora", "WeaponSkillService"))
                    }
                    "apply_mark" -> {
                        val target = action.arguments[0]
                        val markId = action.arguments[1].removeSurrounding("\"")
                        val duration = action.arguments[2].removeSuffix("s").let { if (it == "skill_duration") "definition.skill.durationSeconds.toLong()" else "$it.toLong()" }
                        funSpec.addStatement("%T.applyMark($target, %S, $duration * 20L)", ClassName("hifumi.cresora", "WeaponSkillService"), markId)
                    }
                    "grant_invulnerability" -> {
                        val target = action.arguments[0]
                        val duration = action.arguments[1].removeSuffix("s").let { if (it == "skill_duration") "definition.skill.durationSeconds.toLong()" else "$it.toLong()" }
                        funSpec.addStatement("%T.grantInvulnerability($target, $duration * 20L)", ClassName("hifumi.cresora", "WeaponSkillService"))
                    }
                    "send_message" -> {
                        val key = action.arguments[0].removeSurrounding("\"")
                        val color = action.arguments.getOrNull(1)?.removeSurrounding("\"")?.uppercase() ?: "WHITE"
                        funSpec.addStatement("player.sendMessage(%T.translatable(%S).formatted(%T.$color), true)",
                            ClassName("net.minecraft.text", "Text"), key, ClassName("net.minecraft.util", "Formatting"))
                    }
                    "apply_status_effect" -> {
                        val effectId = action.arguments[0].removeSurrounding("\"")
                        val duration = action.arguments[1].removeSuffix("s")
                        val amplifier = action.arguments.getOrNull(2) ?: "0"
                        funSpec.addStatement("player.addStatusEffect(%T(%T.STATUS_EFFECT.getEntry(%T.of(%S)).get(), $duration.toInt() * 20, $amplifier.toInt()))",
                            ClassName("net.minecraft.entity.effect", "StatusEffectInstance"),
                            ClassName("net.minecraft.registry", "Registries"),
                            ClassName("net.minecraft.util", "Identifier"),
                            effectId)
                    }
                    "area_of_effect" -> {
                        val radius = action.arguments[0]
                        val subActionsStr = action.arguments.last()
                        funSpec.addCode("%L", """
                            |player.world.getNonSpectatingEntities(net.minecraft.entity.LivingEntity::class.java, player.boundingBox.expand($radius.toDouble())).forEach { target ->
                            |    if (target != player) {
                            |        // Sub-actions for AOE
                            |        ${subActionsStr.split(";").joinToString("\n        ") { sub ->
                                        if (sub.isBlank()) ""
                                        else if (sub.startsWith("deal_true_damage")) {
                                            val args = sub.substringAfter("(").substringBefore(")").split(",")
                                            "target.damage(player.world as net.minecraft.server.world.ServerWorld, player.world.damageSources.magic(), ${args.getOrNull(1)?.trim() ?: "0.0"}.toFloat())"
                                        } else if (sub.startsWith("ignite")) {
                                             val args = sub.substringAfter("(").substringBefore(")").split(",")
                                            "target.setOnFireFor(${args.getOrNull(1)?.trim()?.removeSuffix("s") ?: "5"}.toFloat())"
                                        } else if (sub.startsWith("apply_mark")) {
                                            val args = sub.substringAfter("(").substringBefore(")").split(",")
                                            val t = args[0].trim()
                                            val m = args[1].trim().removeSurrounding("\"")
                                            val d = args[2].trim().removeSuffix("s")
                                            "hifumi.cresora.WeaponSkillService.applyMark($t, \"$m\", $d.toLong() * 20L)"
                                        } else if (sub.startsWith("grant_invulnerability")) {
                                            val args = sub.substringAfter("(").substringBefore(")").split(",")
                                            val t = args[0].trim()
                                            val d = args[1].trim().removeSuffix("s")
                                            "hifumi.cresora.WeaponSkillService.grantInvulnerability($t, $d.toLong() * 20L)"
                                        } else ""
                                    }}
                            |    }
                            |}
                        """.trimMargin())
                    }
                    else -> {
                        funSpec.addStatement("// Action: ${action.commandName}(${action.arguments.joinToString()})")
                    }
                }
            }
            is SendLocalizedMessageActionNode -> {
                val key = action.key
                val color = action.color.uppercase()
                val argsList = action.arguments.joinToString(", ") { arg ->
                    if (arg.startsWith("\"")) arg
                    else arg
                }
                val comma = if (argsList.isNotEmpty()) ", " else ""
                funSpec.addStatement("player.sendMessage(%T.translatable(%S$comma$argsList).formatted(%T.$color), true)",
                    ClassName("net.minecraft.text", "Text"), key, ClassName("net.minecraft.util", "Formatting"))
            }
            is OpenSkillMenuActionNode -> {
                val subSkillList = action.subSkillEffectIds.joinToString(", ") { "\"$it\"" }
                funSpec.addStatement("%T.overrideHotbar(player, definition.id, listOf($subSkillList), ${action.durationSeconds.toLong() * 20L})", ClassName("hifumi.cresora", "HotbarOverrideService"))
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
                    when (nested) {
                        is CommandActionNode -> emitAction(nested, funSpec, skill, packageName, className, eventName)
                        is SendLocalizedMessageActionNode -> emitAction(nested, funSpec, skill, packageName, className, eventName)
                        is ExecuteActionNode -> emitAction(nested, funSpec, skill, packageName, className, eventName)
                        else -> funSpec.addStatement("// Unsupported nested AOE action: ${nested::class.simpleName}")
                    }
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
            is ExecuteActionNode -> {
                funSpec.addCode("\n; ")
                if (eventName == "on_damage_absorbed" || eventName == "on_damage_taken") {
                    funSpec.addCode("return run execute@ {\n")
                    action.statements.forEach { stmt ->
                        emitAction(stmt, funSpec, skill, packageName, className, eventName)
                    }
                    funSpec.addCode("amount\n")
                    funSpec.addCode("}\n")
                } else {
                    funSpec.beginControlFlow("run execute@")
                    action.statements.forEach { stmt ->
                        emitAction(stmt, funSpec, skill, packageName, className, eventName)
                    }
                    funSpec.endControlFlow()
                }
            }
            is InstructionCallNode -> {
                val expanded = InstructionMapping.expand(action.functionName, action.arguments)
                funSpec.addStatement("%L", expanded)
            }
            is ExpressionNode -> {
                var content = action.content
                if (skill != null) {
                    for (buff in skill.buffs) {
                        val mapName = "${buff.id.split("_").joinToString("") { if (it == buff.id.split("_")[0]) it else it.replaceFirstChar { c -> c.uppercase() } }}States"
                        val stateClassName = "${buff.id.split("_").joinToString("") { it.replaceFirstChar { c -> c.uppercase() } }}State"
                        content = content.replace(Regex("\\b$mapName\\b"), "$className.$mapName")
                        content = content.replace(Regex("\\b$stateClassName\\b"), "$className.$stateClassName")
                    }
                }
                funSpec.addStatement("%L", content)
            }
        }
    }

    private fun generateScalarOverride(typeSpec: TypeSpec.Builder, skill: SkillNode, methodName: String, statKey: String, packageName: String, className: String) {
        val buffsWithStat = skill.buffs.filter { it.stats.containsKey(statKey) }
        if (buffsWithStat.isNotEmpty()) {
            val funSpec = FunSpec.builder(methodName)
                .addModifiers(KModifier.OVERRIDE)
                .addParameter("player", ClassName("net.minecraft.server.network", "ServerPlayerEntity"))
                .returns(Double::class)

            funSpec.addStatement("var total = 0.0")
            for (buff in buffsWithStat) {
                val mapName = "${buff.id.split("_").joinToString("") { if (it == buff.id.split("_")[0]) it else it.replaceFirstChar { c -> c.uppercase() } }}States"
                funSpec.addStatement("total += $mapName[player.uuid]?.let { it.stacks * ${buff.stats[statKey]} } ?: 0.0")
            }
            funSpec.addStatement("return total")
            typeSpec.addFunction(funSpec.build())
        }
    }

    private fun mapEventName(dslName: String): String = when (dslName) {
        "on_activate" -> "activate"
        "on_tick" -> "onTick"
        "on_player_tick" -> "onPlayerTick"
        "on_damage_dealt" -> "onDamageDealt"
        "on_damage_taken" -> "onDamageTaken"
        "on_damage_absorbed" -> "onDamageAbsorbed"
        else -> dslName
    }

    private fun addParamsForEvent(eventName: String, builder: FunSpec.Builder) {
        when (eventName) {
            "on_activate" -> {
                builder.addParameter("player", ClassName("net.minecraft.server.network", "ServerPlayerEntity"))
                builder.addParameter("definition", ClassName("hifumi.cresora", "WeaponDefinition"))
                builder.addParameter("data", ClassName("hifumi.cresora", "WeaponData"))
                builder.addParameter("access", ClassName("hifumi.cresora", "WeaponSkillAccess"))
                builder.returns(ClassName("net.minecraft.util", "ActionResult"))
            }
            "on_damage_dealt" -> {
                builder.addParameter("player", ClassName("net.minecraft.server.network", "ServerPlayerEntity"))
                builder.addParameter("target", ClassName("net.minecraft.entity", "LivingEntity"))
                builder.addParameter("amount", Float::class)
                builder.addParameter("isTrueDamage", Boolean::class)
                builder.addParameter("definition", ClassName("hifumi.cresora", "WeaponDefinition"))
                builder.addParameter("data", ClassName("hifumi.cresora", "WeaponData"))
            }
            "on_player_tick" -> {
                builder.addParameter("player", ClassName("net.minecraft.server.network", "ServerPlayerEntity"))
                builder.addParameter("definition", ClassName("hifumi.cresora", "WeaponDefinition"))
                builder.addParameter("data", ClassName("hifumi.cresora", "WeaponData"))
            }
            "on_damage_absorbed" -> {
                builder.addParameter("player", ClassName("net.minecraft.server.network", "ServerPlayerEntity"))
                builder.addParameter("amount", Float::class)
                builder.addParameter("definition", ClassName("hifumi.cresora", "WeaponDefinition"))
                builder.addParameter("data", ClassName("hifumi.cresora", "WeaponData"))
                builder.returns(Float::class)
            }
            "on_damage_taken" -> {
                builder.addParameter("player", ClassName("net.minecraft.server.network", "ServerPlayerEntity"))
                builder.addParameter("amount", Float::class)
                builder.addParameter("definition", ClassName("hifumi.cresora", "WeaponDefinition"))
                builder.addParameter("data", ClassName("hifumi.cresora", "WeaponData"))
                builder.returns(Float::class)
            }
            "on_tick" -> {
                builder.addParameter("server", ClassName("net.minecraft.server", "MinecraftServer"))
                builder.addParameter("definition", ClassName("hifumi.cresora", "WeaponDefinition"))
                builder.addParameter("data", ClassName("hifumi.cresora", "WeaponData"))
            }
        }
    }

    private fun updateWeaponJson(weapons: List<WeaponDefNode>) {
        val root = JsonObject().apply { add("weaponDefinitions", JsonArray()) }
        val defs = root.getAsJsonArray("weaponDefinitions")

        for (weapon in weapons) {
            val weaponObj = JsonObject()
            weaponObj.addProperty("id", weapon.id)
            weaponObj.addProperty("baseItemId", weapon.baseItem)
            weaponObj.addProperty("baseAttackDamage", weapon.stats.baseAttackDamage)
            weaponObj.addProperty("attackDamagePerLevel", weapon.stats.attackDamagePerLevel)
            weaponObj.addProperty("totalAttackSpeed", weapon.stats.totalAttackSpeed)
            weaponObj.addProperty("maxBaseLevel", weapon.stats.maxBaseLevel)
            weaponObj.addProperty("maxSkillLevel", weapon.stats.maxSkillLevel)
            weaponObj.addProperty("damageType", weapon.damageType)
            weapon.customModelData?.let { weaponObj.addProperty("custom_model_data", it) }

            val skillObj = JsonObject()
            if (weapon.skill != null) {
                val s = weapon.skill
                skillObj.addProperty("effectId", s.effectId)
                skillObj.addProperty("durationSeconds", s.durationSeconds.toInt())
                skillObj.addProperty("cooldownSeconds", s.cooldownSeconds.toInt())
                skillObj.addProperty("baseValue", s.baseValue)
                skillObj.addProperty("valuePerLevel", s.valuePerLevel)
                skillObj.addProperty("radiusMeters", s.radiusMeters)
            } else {
                skillObj.addProperty("effectId", "none")
                skillObj.addProperty("durationSeconds", 0)
                skillObj.addProperty("cooldownSeconds", 0)
                skillObj.addProperty("baseValue", 0.0)
                skillObj.addProperty("valuePerLevel", 0.0)
                skillObj.addProperty("radiusMeters", 0.0)
            }
            weaponObj.add("skill", skillObj)

            val upgrades = JsonObject()
            upgrades.addProperty("baseCscQuadraticCoefficient", 100)
            upgrades.addProperty("skillCscLinearCoefficient", 10000)
            upgrades.addProperty("skillCscQuadraticCoefficient", 0)
            upgrades.addProperty("baseFragmentCost", 1)
            weaponObj.add("upgrades", upgrades)

            val craft = JsonObject()
            craft.addProperty("fragmentItemId", "${weapon.id}_fragment")
            craft.addProperty("fragmentBaseItemId", "minecraft:prismarine_shard")
            craft.addProperty("fragmentsRequired", 8)
            craft.addProperty("craftedRarity", weapon.rarity.lowercase())
            craft.addProperty("craftedBaseLevel", 1)
            craft.addProperty("craftedSkillLevel", 1)
            weaponObj.add("craft", craft)

            val drops = JsonObject()
            val fragmentDrop = JsonObject()
            fragmentDrop.addProperty("minMobLevel", 999)
            fragmentDrop.addProperty("chance", 0.0)
            fragmentDrop.addProperty("minCount", 0)
            fragmentDrop.addProperty("maxCount", 0)
            fragmentDrop.addProperty("bonusCountAtLevel70", 0)
            drops.add("fragmentDrop", fragmentDrop)
            drops.add("directDropTiers", JsonArray())
            drops.addProperty("directDropBaseLevelMultiplier", 0.0)
            drops.addProperty("fiveStarChanceMultiplierAtLevel70", 1.0)
            weaponObj.add("drops", drops)

            defs.add(weaponObj)
        }

        weaponJsonFile.parentFile.mkdirs()
        weaponJsonFile.writeText(gson.toJson(root))
    }

    private fun updateRegistry(weapons: List<WeaponDefNode>) {
        val packageName = "hifumi.cresora.skill.generated"
        val className = "CompiledWeaponSkillRegistry"

        val funSpec = FunSpec.builder("registerAll")
            .addParameter("registry", ClassName("hifumi.cresora.skill", "WeaponSkillRegistry"))

        for (weapon in weapons) {
            weapon.skill?.let { s ->
                val handlerClassName = "${weapon.name.replace("[ ,']".toRegex(), "")}Skill"
                funSpec.addStatement("registry.register(%S, %T)", s.effectId, ClassName(packageName, handlerClassName))
                for (subSkill in weapon.subSkills) {
                    funSpec.addStatement("registry.registerSubSkill(%S, %S)", s.effectId, subSkill.effectId)
                }
            }
            for (subSkill in weapon.subSkills) {
                val handlerClassName = "${subSkill.name.replace("[ ,']".toRegex(), "")}Skill"
                funSpec.addStatement("registry.register(%S, %T)", subSkill.effectId, ClassName(packageName, handlerClassName))
            }
        }

        val file = FileSpec.builder(packageName, className)
            .addType(TypeSpec.objectBuilder(className)
                .addFunction(funSpec.build())
                .build())
            .build()

        file.writeTo(outputDir)
    }

    private fun updateLangFiles(weapons: List<WeaponDefNode>, dictionaries: List<DictionaryDefNode>) {
        val langDir = File(weaponJsonFile.parentFile.parentFile.parentFile.parentFile, "assets/cresora-utilities/lang")
        if (!langDir.exists()) return

        val locales = (weapons.flatMap { it.translations.keys } + dictionaries.flatMap { it.translations.keys }).distinct()
        for (locale in locales) {
            val langFile = File(langDir, "$locale.json")
            val langJson = if (langFile.exists()) {
                JsonParser.parseString(langFile.readText()).asJsonObject
            } else {
                JsonObject()
            }

            // Apply dictionaries first (base translations)
            for (dict in dictionaries) {
                dict.translations[locale]?.forEach { (k, v) ->
                    langJson.addProperty(k, v)
                }
            }

            for (weapon in weapons) {
                val trans = weapon.translations[locale] ?: continue
                trans["name"]?.let { langJson.addProperty("item.cresora-utilities.${weapon.id}", it) }
                trans["skill_name"]?.let { langJson.addProperty("item.cresora-utilities.${weapon.id}.skill", it) }
                trans["fragment_name"]?.let { langJson.addProperty("item.cresora-utilities.${weapon.id}_fragment", it) }
                weapon.subSkills.forEach { subSkill ->
                    trans["sub_skill_${subSkill.effectId}_name"]?.let { langJson.addProperty("item.cresora-utilities.sub_skill.${subSkill.effectId}", it) }
                }
                weapon.skill?.buffs?.forEach { buff ->
                    val buffNameKey = buff.translationKey ?: "item.cresora.weapon.skill.buff.${buff.id}.name"
                    val buffName = trans["buff_${buff.id}_name"] ?: buff.id.replace("_", " ").split(" ").joinToString(" ") { it.replaceFirstChar { c -> c.uppercase() } }
                    langJson.addProperty(buffNameKey, buffName)

                    val gained = trans["buff_${buff.id}_gained"] ?: when(locale) {
                        "zh_cn" -> "获得效果: %s (%s 层)"
                        "ja_jp" -> "効果獲得: %s (%s 層)"
                        else -> "Gained buff: %s (%s stacks)"
                    }
                    langJson.addProperty("item.cresora.weapon.skill.buff.${buff.id}.gained", gained)

                    val expired = trans["buff_${buff.id}_expired"] ?: when(locale) {
                        "zh_cn" -> "效果结束: %s"
                        "ja_jp" -> "効果終了: %s"
                        else -> "Buff expired: %s"
                    }
                    langJson.addProperty("item.cresora.weapon.skill.buff.${buff.id}.expired", expired)
                }
                trans.forEach { (k, v) ->
                    if (!k.startsWith("name") && !k.startsWith("skill_name") && !k.startsWith("fragment_name") && !k.startsWith("buff_") && !k.startsWith("sub_skill_")) {
                        langJson.addProperty(k, v)
                    }
                }
            }
            langFile.writeText(gson.toJson(langJson))
        }
    }

    private fun updateItemModels(weapons: List<WeaponDefNode>) {
        val assetsDir = File(weaponJsonFile.parentFile.parentFile.parentFile.parentFile, "assets/cresora-utilities")
        val itemsDir = File(assetsDir, "items")
        val modelsDir = File(assetsDir, "models/item")
        if (!itemsDir.exists()) itemsDir.mkdirs()
        if (!modelsDir.exists()) modelsDir.mkdirs()

        for (weapon in weapons) {
            // 1. Item Definition (Modern Minecraft)
            val itemDefFile = File(itemsDir, "${weapon.id}.json")
            if (!itemDefFile.exists()) {
                val itemDefJson = JsonObject()
                val modelObj = JsonObject()
                modelObj.addProperty("type", "minecraft:model")
                modelObj.addProperty("model", "cresora-utilities:item/${weapon.id}")
                itemDefJson.add("model", modelObj)
                itemDefFile.writeText(gson.toJson(itemDefJson))
            }

            // 2. Fragment Item Definition
            val fragmentDefFile = File(itemsDir, "${weapon.id}_fragment.json")
            val fragmentDefJson = JsonObject()
            val fragmentModelObj = JsonObject()
            fragmentModelObj.addProperty("type", "minecraft:model")
            fragmentModelObj.addProperty("model", "cresora-utilities:item/${weapon.id}_fragment")
            fragmentDefJson.add("model", fragmentModelObj)
            fragmentDefFile.writeText(gson.toJson(fragmentDefJson))

            // 3. Model
            val modelFile = File(modelsDir, "${weapon.id}.json")
            println("Writing item model for ${weapon.id}...")
            val modelJson = JsonObject()
            modelJson.addProperty("parent", "minecraft:item/handheld")
            val textures = JsonObject()
            val texturePath = weapon.texture ?: weapon.baseItem.let {
                if (it.contains(":")) {
                    val parts = it.split(":")
                    "${parts[0]}:item/${parts[1]}"
                } else {
                    "minecraft:item/$it"
                }
            }
            textures.addProperty("layer0", texturePath)
            modelJson.add("textures", textures)
            modelFile.writeText(gson.toJson(modelJson))

            // Fragment model
            val fragmentModelFile = File(modelsDir, "${weapon.id}_fragment.json")
            val fragmentModelJson = JsonObject()
            fragmentModelJson.addProperty("parent", "minecraft:item/generated")
            val fragmentTextures = JsonObject()
            fragmentTextures.addProperty("layer0", "minecraft:item/prismarine_shard")
            fragmentModelJson.add("textures", fragmentTextures)
            fragmentModelFile.writeText(gson.toJson(fragmentModelJson))
        }

        // 4. Sub Skill Dummy Item Definition
        val subSkillDummyFile = File(itemsDir, "sub_skill_dummy.json")
        val subSkillRoot = JsonObject()
        val subSkillModel = JsonObject()
        subSkillModel.addProperty("type", "minecraft:select")
        subSkillModel.addProperty("property", "minecraft:component")
        subSkillModel.addProperty("component", "cresora-utilities:sub_skill_effect_id")

        val cases = JsonArray()
        val allSubSkills = weapons.flatMap { it.subSkills }.distinctBy { it.effectId }
        for (subSkill in allSubSkills) {
            if (subSkill.icon.isBlank()) continue
            val case = JsonObject()
            case.addProperty("when", subSkill.effectId)
            val caseModel = JsonObject()
            caseModel.addProperty("type", "minecraft:model")
            val modelPath = if (subSkill.icon.contains(":")) {
                val parts = subSkill.icon.split(":")
                if (parts[0] == "minecraft") {
                    // For Minecraft, referring to the model of a block/item usually works better
                    // if we point directly to the item definition's intended model.
                    // In 1.21, "minecraft:lightning_rod" might just work if we use it correctly.
                    // However, to be safe, we keep the item/ suffix for items but detect blocks.
                    "${parts[0]}:item/${parts[1]}"
                } else {
                    "${parts[0]}:item/${parts[1]}"
                }
            } else {
                "minecraft:item/${subSkill.icon}"
            }
            // Fix for fallback missing texture too
            if (subSkill.icon == "minecraft:lightning_rod") {
                 // Special case for blocks that don't have a simple item model path
                 caseModel.addProperty("model", "minecraft:lightning_rod")
            } else if (subSkill.icon == "minecraft:blue_ice") {
                 caseModel.addProperty("model", "minecraft:blue_ice")
            } else {
                 caseModel.addProperty("model", modelPath)
            }
            case.add("model", caseModel)
            cases.add(case)
        }
        subSkillModel.add("cases", cases)

        val fallback = JsonObject()
        fallback.addProperty("type", "minecraft:model")
        fallback.addProperty("model", "minecraft:item/barrier")
        subSkillModel.add("fallback", fallback)

        subSkillRoot.add("model", subSkillModel)
        subSkillDummyFile.writeText(gson.toJson(subSkillRoot))
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
            .addImport("hifumi.cresora", "CreditsService")
            .addImport("hifumi.cresora", "HotbarOverrideService")
            .addImport("hifumi.cresora", "WeaponCombatSupport")
            .addImport("hifumi.cresora", "WeaponSkillAccess")
            .addImport("hifumi.cresora", "WeaponDefinition")
            .addImport("hifumi.cresora", "WeaponData")
            .addImport("hifumi.cresora", "AdventureRankMobAccess")
            .addImport("hifumi.cresora", "AdventureRankService")
    }
}
