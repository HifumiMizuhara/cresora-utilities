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

        for (file in cresoraFiles) {
            println("Compiling ${file.name}...")
            val source = file.readText()
            val lexer = Lexer(source)
            val tokens = lexer.scanTokens()
            val parser = Parser(source, tokens)
            val weapons = parser.parse()
            allWeapons.addAll(weapons)
        }

        if (allWeapons.isEmpty()) {
            println("No .cresora files found or no weapons defined.")
            // Still generate empty registry and JSON to ensure consistency
            generateKotlinCode(emptyList())
            updateWeaponJson(emptyList())
            updateRegistry(emptyList())
            return
        }

        generateKotlinCode(allWeapons)
        updateWeaponJson(allWeapons)
        updateRegistry(allWeapons)
        updateLangFiles(allWeapons)
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
        val className = "${weapon.name.replace(" ", "")}Skill"
        val typeSpec = TypeSpec.objectBuilder(className)
            .addSuperinterface(ClassName("hifumi.cresora.skill", "WeaponSkillHandler"))

        setupSkillCommon(typeSpec, skill, packageName, className)
        
        for (handler in skill.handlers) {
            val funSpec = FunSpec.builder(mapEventName(handler.eventName))
                .addModifiers(KModifier.OVERRIDE)
            addParamsForEvent(handler.eventName, funSpec)
            
            handler.actions.forEach { 
                emitAction(it, funSpec, skill, packageName, className)
                funSpec.addCode("\n")
            }
            
            if (handler.eventName == "on_activate") {
                funSpec.addCode("\n")
                funSpec.addStatement("return %T.SUCCESS", ClassName("net.minecraft.util", "ActionResult"))
            } else if (handler.eventName == "on_damage_absorbed" || handler.eventName == "on_damage_taken") {
                funSpec.addStatement("return amount")
            }
            typeSpec.addFunction(funSpec.build())
        }

        FileSpec.builder(packageName, className).addType(typeSpec.build()).build().writeTo(outputDir)
    }

    private fun generateSubSkillClass(weapon: WeaponDefNode, subSkill: SubSkillNode, packageName: String) {
        val className = "${subSkill.name.replace(" ", "")}Skill"
        val typeSpec = TypeSpec.objectBuilder(className)
            .addSuperinterface(ClassName("hifumi.cresora.skill", "WeaponSkillHandler"))

        for (handler in subSkill.handlers) {
            val funSpec = FunSpec.builder(mapEventName(handler.eventName))
                .addModifiers(KModifier.OVERRIDE)
            addParamsForEvent(handler.eventName, funSpec)
            
            handler.actions.forEach { emitAction(it, funSpec, null, packageName, className) }
            
            if (handler.eventName == "on_activate") {
                funSpec.addCode("\n")
                funSpec.addStatement("return %T.SUCCESS", ClassName("net.minecraft.util", "ActionResult"))
            } else if (handler.eventName == "on_damage_absorbed" || handler.eventName == "on_damage_taken") {
                funSpec.addStatement("return amount")
            }
            typeSpec.addFunction(funSpec.build())
        }

        FileSpec.builder(packageName, className).addType(typeSpec.build()).build().writeTo(outputDir)
    }

    private fun setupSkillCommon(typeSpec: TypeSpec.Builder, skill: SkillNode, packageName: String, className: String) {
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
            val onPlayerTickFun = FunSpec.builder("onPlayerTick")
                .addModifiers(KModifier.OVERRIDE)
                .addParameter("player", ClassName("net.minecraft.server.network", "ServerPlayerEntity"))
            
            onPlayerTickFun.addStatement("val now = %T.currentWorldTime(player)", ClassName("hifumi.cresora", "WeaponSkillService"))
            for (buff in skill.buffs) {
                val mapName = "${buff.id.split("_").joinToString("") { if (it == buff.id.split("_")[0]) it else it.replaceFirstChar { c -> c.uppercase() } }}States"
                val buffNameKey = buff.translationKey ?: "item.cresora.weapon.skill.buff.${buff.id}.name"
                onPlayerTickFun.addCode("""
                    |if ($mapName.containsKey(player.uuid) && now >= $mapName[player.uuid]!!.expireTick) {
                    |    $mapName.remove(player.uuid)
                    |    player.sendMessage(%T.translatable("item.cresora.weapon.skill.buff.${buff.id}.expired", %T.translatable("$buffNameKey")), true)
                    |}
                """.trimMargin(), ClassName("net.minecraft.text", "Text"), ClassName("net.minecraft.text", "Text"))
            }
            typeSpec.addFunction(onPlayerTickFun.build())
        }

        // 3. Scalar overrides
        generateScalarOverride(typeSpec, skill, "getAttackDamageScalar", "attack_percent_per_stack", packageName, className)
        generateScalarOverride(typeSpec, skill, "getArmorScalar", "armor_per_stack", packageName, className)
        generateScalarOverride(typeSpec, skill, "getCritRateBonus", "crit_rate_per_stack", packageName, className)
        generateScalarOverride(typeSpec, skill, "getCritDamageBonus", "crit_dmg_per_stack", packageName, className)
    }

    private fun emitAction(action: ActionNode, funSpec: FunSpec.Builder, skill: SkillNode?, packageName: String, className: String) {
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
                                    |{
                                    |    val now = %T.currentWorldTime(player)
                                    |    val state = $mapName.getOrPut(player.uuid) { $stateClassName(0L, 0) }
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
                        funSpec.addCode("""
                            |player.world.getNonSpectatingEntities(%T::class.java, player.boundingBox.expand($radius.toDouble())).forEach { target ->
                            |    if (target != player) {
                            |        // Sub-actions for AOE
                            |        ${subActionsStr.split(";").joinToString("\n        ") { sub ->
                                        if (sub.startsWith("deal_true_damage")) {
                                            val args = sub.substringAfter("(").substringBefore(")").split(",")
                                            "target.damage(player.world.damageSources.magic(), ${args.getOrNull(1) ?: "0"}.toFloat())"
                                        } else if (sub.startsWith("ignite")) {
                                             val args = sub.substringAfter("(").substringBefore(")").split(",")
                                            "target.setOnFireFor(${args.getOrNull(1)?.removeSuffix("s") ?: "5"}.toFloat())"
                                        } else ""
                                    }}
                            |    }
                            |}
                        """.trimMargin(), ClassName("net.minecraft.entity", "LivingEntity"))
                    }
                    else -> {
                        funSpec.addStatement("// Action: ${action.commandName}(${action.arguments.joinToString()})")
                    }
                }
            }
            is OpenSkillMenuActionNode -> {
                val subSkillList = action.subSkillEffectIds.joinToString(", ") { "\"$it\"" }
                funSpec.addStatement("%T.overrideHotbar(player, definition.id, listOf($subSkillList), ${action.durationSeconds.toLong() * 20L})", ClassName("hifumi.cresora", "HotbarOverrideService"))
            }
            is CloseSkillMenuActionNode -> {
                funSpec.addStatement("%T.restoreHotbar(player)", ClassName("hifumi.cresora", "HotbarOverrideService"))
            }
            is ExecuteActionNode -> {
                funSpec.beginControlFlow("run execute@")
                funSpec.addCode(action.content + "\n")
                funSpec.endControlFlow()
            }
            is ExpressionNode -> {
                funSpec.addStatement(action.content)
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
            }
            "on_player_tick" -> {
                builder.addParameter("player", ClassName("net.minecraft.server.network", "ServerPlayerEntity"))
            }
            "on_damage_absorbed" -> {
                builder.addParameter("player", ClassName("net.minecraft.server.network", "ServerPlayerEntity"))
                builder.addParameter("amount", Float::class)
                builder.returns(Float::class)
            }
            "on_damage_taken" -> {
                builder.addParameter("player", ClassName("net.minecraft.server.network", "ServerPlayerEntity"))
                builder.addParameter("amount", Float::class)
                builder.returns(Float::class)
            }
            "on_tick" -> {
                builder.addParameter("server", ClassName("net.minecraft.server", "MinecraftServer"))
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
            
            weapon.skill?.let { s ->
                val skillObj = JsonObject()
                skillObj.addProperty("effectId", s.effectId)
                skillObj.addProperty("durationSeconds", s.durationSeconds.toInt())
                skillObj.addProperty("cooldownSeconds", s.cooldownSeconds.toInt())
                skillObj.addProperty("baseValue", s.baseValue)
                skillObj.addProperty("valuePerLevel", s.valuePerLevel)
                skillObj.addProperty("radiusMeters", s.radiusMeters)
                weaponObj.add("skill", skillObj)
            }
            
            val upgrades = JsonObject()
            upgrades.addProperty("baseCscQuadraticCoefficient", 100)
            upgrades.addProperty("skillCscLinearCoefficient", 10000)
            upgrades.addProperty("skillCscQuadraticCoefficient", 0)
            upgrades.addProperty("baseFragmentCost", 1)
            weaponObj.add("upgrades", upgrades)

            val craft = JsonObject()
            craft.addProperty("fragmentsRequired", 8)
            craft.addProperty("craftedRarity", weapon.rarity.lowercase())
            weaponObj.add("craft", craft)

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
                val handlerClassName = "${weapon.name.replace(" ", "")}Skill"
                funSpec.addStatement("registry.register(%S, %T)", s.effectId, ClassName(packageName, handlerClassName))
            }
            for (subSkill in weapon.subSkills) {
                val handlerClassName = "${subSkill.name.replace(" ", "")}Skill"
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

    private fun updateLangFiles(weapons: List<WeaponDefNode>) {
        val langDir = File(weaponJsonFile.parentFile.parentFile.parentFile.parentFile, "assets/cresora-utilities/lang")
        if (!langDir.exists()) return

        val locales = weapons.flatMap { it.translations.keys }.distinct()
        for (locale in locales) {
            val langFile = File(langDir, "$locale.json")
            val langJson = if (langFile.exists()) {
                JsonParser.parseString(langFile.readText()).asJsonObject
            } else {
                JsonObject()
            }

            for (weapon in weapons) {
                val trans = weapon.translations[locale] ?: continue
                trans["name"]?.let { langJson.addProperty("item.cresora-utilities.${weapon.id}", it) }
                trans["skill_name"]?.let { langJson.addProperty("item.cresora-utilities.${weapon.id}.skill", it) }
                weapon.subSkills.forEach { subSkill ->
                    trans["sub_skill_${subSkill.effectId}_name"]?.let { langJson.addProperty("item.cresora-utilities.sub_skill.${subSkill.effectId}", it) }
                }
                weapon.skill?.buffs?.forEach { buff ->
                    val buffNameKey = buff.translationKey ?: "item.cresora.weapon.skill.buff.${buff.id}.name"
                    trans["buff_${buff.id}_name"]?.let { langJson.addProperty(buffNameKey, it) }
                    trans["buff_${buff.id}_gained"]?.let { langJson.addProperty("item.cresora.weapon.skill.buff.${buff.id}.gained", it) }
                    trans["buff_${buff.id}_expired"]?.let { langJson.addProperty("item.cresora.weapon.skill.buff.${buff.id}.expired", it) }
                }
                trans.forEach { (k, v) ->
                    if (!k.startsWith("name") && !k.startsWith("skill_name") && !k.startsWith("buff_") && !k.startsWith("sub_skill_")) {
                        langJson.addProperty(k, v)
                    }
                }
            }
            langFile.writeText(gson.toJson(langJson))
        }
    }
}
