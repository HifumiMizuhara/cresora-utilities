package hifumi.cresora.compiler

import com.squareup.kotlinpoet.FunSpec

/** Code-gen snippets shared between the weapon (CWC) and artifact (CAC) compilers. */
internal object CodegenSupport {

    fun beginAreaOfEffect(funSpec: FunSpec.Builder, radius: Double) {
        funSpec.addCode(
            """
            |player.world.getNonSpectatingEntities(net.minecraft.entity.LivingEntity::class.java, player.boundingBox.expand($radius.toDouble())).forEach { target ->
            |    if (target != player) {
            |        // area_of_effect block
            |""".trimMargin()
        )
    }

    fun endAreaOfEffect(funSpec: FunSpec.Builder) {
        funSpec.addCode(
            """
            |    }
            |}
            |
            """.trimMargin()
        )
    }
}
