package hifumi.cresora.compiler

import java.io.File

fun main(args: Array<String>) {
    val projectRoot = File(System.getProperty("user.dir"))
    val inputDir = File(projectRoot, "src/main/cresora")
    val outputDir = File(projectRoot, "src/main/kotlin")
    val weaponJsonFile = File(projectRoot, "src/main/resources/data/cresora-utilities/cresora/cwc_weapon_content.json")

    println("Starting Cresora Weapon Compiler...")
    println("Input: ${inputDir.absolutePath}")
    println("Output: ${outputDir.absolutePath}")
    println("JSON: ${weaponJsonFile.absolutePath}")

    val weaponCompiler = CresoraCompiler(inputDir, outputDir, weaponJsonFile)
    weaponCompiler.compile()

    val artifactJsonFile = File(projectRoot, "src/main/resources/data/cresora-utilities/cresora/cac_artifact_content.json")
    val artifactCompiler = ArtifactCompiler(inputDir, outputDir, artifactJsonFile)
    artifactCompiler.compile()
    println("Compilation finished successfully.")
}
