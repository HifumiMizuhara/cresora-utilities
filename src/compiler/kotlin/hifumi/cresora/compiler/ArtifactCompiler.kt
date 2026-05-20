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
                bonus.handlers.forEach { handler ->
                    val effectId = "${artifact.id}_${bonus.requiredPieces}pc_${handler.eventName}"
                    val className = "ArtifactSkill_${artifact.id.replace("_", "")}_${bonus.requiredPieces}pc_${handler.eventName.replace("_", "")}"
                    generateArtifactHookClass(artifact, bonus, handler, effectId, className)
                    generatedHandlers.add(effectId to className)
                }
            }
        }

        generateRegistry(generatedHandlers)
        updateArtifactJson(allArtifacts)
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

        val classSpec = TypeSpec.classBuilder(className)
            .addSuperinterface(ClassName("hifumi.cresora.equipment", "ArtifactSkillHandler"))

        val methodName = mapEventName(handler.eventName)
        val methodSpec = FunSpec.builder(methodName)
            .addModifiers(KModifier.OVERRIDE)

        addParamsForEvent(handler.eventName, methodSpec)

        // Emit actions
        handler.actions.forEach { action ->
            emitAction(action, methodSpec)
        }

        classSpec.addFunction(methodSpec.build())
        file.addType(classSpec.build())
        file.build().writeTo(outputDir)
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

    private fun emitAction(action: ActionNode, funSpec: FunSpec.Builder) {
        when (action) {
            is ExecuteActionNode -> {
                action.statements.forEach { stmt ->
                    emitAction(stmt, funSpec)
                }
            }
            is InstructionCallNode -> {
                val expanded = InstructionMapping.expand(action.functionName, action.arguments)
                funSpec.addStatement("%L", expanded)
            }
            is ExpressionNode -> {
                funSpec.addStatement("%L", action.content)
            }
            else -> {}
        }
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
                    statObj.addProperty("type", type.uppercase())
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

    private fun mapTrigger(dslName: String): String = when (dslName) {
        "on_attack_dealt" -> "attack_dealt"
        "on_damage_taken" -> "damage_taken"
        "on_equip_changed" -> "equip_changed"
        "on_tick" -> "tick"
        "on_kill" -> "kill"
        else -> dslName.removePrefix("on_")
    }
}
