package hifumi.cresora.compiler

import java.io.File

fun main(args: Array<String>) {
    val projectRoot = File(System.getProperty("user.dir"))
    val inputDir = File(projectRoot, "src/main/cresora")

    // Output directories: accept overrides via system properties, fall back to build/generated/cresora
    val kotlinOutDir = File(System.getProperty("cresora.output.kotlin", "build/generated/cresora/kotlin"))
    val resourceOutDir = File(System.getProperty("cresora.output.resources", "build/generated/cresora/resources"))
    val sourceResourcesDir = File(System.getProperty("cresora.source.resources", "src/main/resources"))

    val outputDir = kotlinOutDir
    val dataOutDir = File(resourceOutDir, "data/cresora-utilities/cresora")
    dataOutDir.mkdirs()

    val weaponJsonFile = File(dataOutDir, "cwc_weapon_content.json")

    println("Starting Cresora Weapon Compiler...")
    println("Input: ${inputDir.absolutePath}")
    println("Kotlin output: ${kotlinOutDir.absolutePath}")
    println("Resource output: ${resourceOutDir.absolutePath}")

    val weaponCompiler = CresoraCompiler(inputDir, outputDir, weaponJsonFile, sourceResourcesDir)
    weaponCompiler.compile()

    val artifactJsonFile = File(dataOutDir, "cac_artifact_content.json")
    val artifactCompiler = ArtifactCompiler(inputDir, outputDir, artifactJsonFile, sourceResourcesDir)
    artifactCompiler.compile()

    val storyJsonFile = File(dataOutDir, "story_content.json")
    val storyTextsFile = File(dataOutDir, "story_texts.json")
    val movementCompiler = MovementCompiler(inputDir, storyJsonFile, storyTextsFile)
    movementCompiler.compile()

    println("Compilation finished successfully.")
}
