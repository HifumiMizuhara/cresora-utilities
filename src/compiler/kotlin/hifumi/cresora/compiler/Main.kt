package hifumi.cresora.compiler

import java.io.File

fun main(args: Array<String>) {
    val projectRoot = File(System.getProperty("user.dir"))
    val kotlinOutDir = File(System.getProperty("cresora.output.kotlin", "build/generated/cresora/kotlin"))
    val resourceOutDir = File(System.getProperty("cresora.output.resources", "build/generated/cresora/resources"))
    val sourceResourcesDir = File(System.getProperty("cresora.source.resources", "src/main/resources"))

    println("Starting Cresora Weapon Compiler...")
    println("Input: ${File(projectRoot, "src/main/cresora").absolutePath}")
    println("Kotlin output: ${kotlinOutDir.absolutePath}")
    println("Resource output: ${resourceOutDir.absolutePath}")

    compileCresoraAssets(projectRoot, kotlinOutDir, resourceOutDir, sourceResourcesDir)

    println("Compilation finished successfully.")
}

/**
 * Compiles all Cresora DSL assets into a clean generated-resource overlay.
 *
 * Source resources are intentionally treated as immutable input. The managed asset
 * directories are copied into the overlay before compilers add their DSL-derived
 * entries, so Gradle can package the overlay without mutating the checkout.
 */
fun compileCresoraAssets(
    projectRoot: File,
    kotlinOutDir: File,
    resourceOutDir: File,
    sourceResourcesDir: File
) {
    validateGeneratedOutputDirectories(kotlinOutDir, resourceOutDir, sourceResourcesDir)

    recreateDirectory(kotlinOutDir, "generated Kotlin")
    prepareGeneratedResourceOverlay(sourceResourcesDir, resourceOutDir)

    val inputDir = File(projectRoot, "src/main/cresora")
    val dataOutDir = File(resourceOutDir, "data/cresora-utilities/cresora")
    check(dataOutDir.mkdirs() || dataOutDir.isDirectory) {
        "Could not create generated data directory: ${dataOutDir.absolutePath}"
    }

    val weaponJsonFile = File(dataOutDir, "cwc_weapon_content.json")
    val weaponCompiler = CresoraCompiler(
        inputDir,
        kotlinOutDir,
        weaponJsonFile,
        sourceResourcesDir,
        resourceOutDir
    )
    weaponCompiler.compile()

    val artifactJsonFile = File(dataOutDir, "cac_artifact_content.json")
    val artifactCompiler = ArtifactCompiler(
        inputDir,
        kotlinOutDir,
        artifactJsonFile,
        sourceResourcesDir,
        resourceOutDir
    )
    artifactCompiler.compile()

    val storyJsonFile = File(dataOutDir, "story_content.json")
    val storyTextsFile = File(dataOutDir, "story_texts.json")
    val movementCompiler = MovementCompiler(inputDir, storyJsonFile, storyTextsFile)
    movementCompiler.compile()
}

private fun prepareGeneratedResourceOverlay(sourceResourcesDir: File, resourceOutDir: File) {
    recreateDirectory(resourceOutDir, "generated resource")

    val managedDirectories = listOf(
        "assets/cresora-utilities/lang",
        "assets/cresora-utilities/items",
        "assets/cresora-utilities/models/item"
    )
    for (relativePath in managedDirectories) {
        val sourceDir = File(sourceResourcesDir, relativePath)
        if (!sourceDir.isDirectory) continue

        val targetDir = File(resourceOutDir, relativePath)
        check(sourceDir.copyRecursively(targetDir, overwrite = true)) {
            "Could not copy generated resource base directory: ${sourceDir.absolutePath}"
        }
    }
}

private fun validateGeneratedOutputDirectories(
    kotlinOutDir: File,
    resourceOutDir: File,
    sourceResourcesDir: File
) {
    val kotlinOutputPath = kotlinOutDir.canonicalFile.toPath()
    val resourceOutputPath = resourceOutDir.canonicalFile.toPath()
    val sourceResourcesPath = sourceResourcesDir.canonicalFile.toPath()

    require(!kotlinOutputPath.overlaps(resourceOutputPath)) {
        "Generated Kotlin and resource output directories must not overlap"
    }
    require(!kotlinOutputPath.overlaps(sourceResourcesPath)) {
        "Generated Kotlin output must not overlap source resources"
    }
    require(!resourceOutputPath.overlaps(sourceResourcesPath)) {
        "Generated resource output must not overlap source resources"
    }
}

private fun java.nio.file.Path.overlaps(other: java.nio.file.Path): Boolean =
    startsWith(other) || other.startsWith(this)

private fun recreateDirectory(directory: File, description: String) {
    if (directory.exists()) {
        check(directory.deleteRecursively()) {
            "Could not clear $description directory: ${directory.absolutePath}"
        }
    }
    check(directory.mkdirs() || directory.isDirectory) {
        "Could not create $description directory: ${directory.absolutePath}"
    }
}
