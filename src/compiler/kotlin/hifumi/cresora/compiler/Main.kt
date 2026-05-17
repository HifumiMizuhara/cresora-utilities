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

    val compiler = CresoraCompiler(inputDir, outputDir, weaponJsonFile)
    compiler.compile()
    println("Compilation finished successfully.")
}
