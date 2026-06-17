package hifumi.cresora.npc

import hifumi.cresora.CreSoraUtilities
import net.minecraft.client.render.OverlayTexture
import net.minecraft.client.render.RenderLayer
import net.minecraft.client.render.VertexConsumerProvider
import net.minecraft.client.render.entity.EntityRenderer
import net.minecraft.client.render.entity.EntityRendererFactory
import net.minecraft.client.render.entity.state.EntityRenderState
import net.minecraft.client.util.math.MatrixStack
import net.minecraft.util.Identifier
import org.joml.Quaternionf

class SpiritGuideEntityRenderState : EntityRenderState()

class SpiritGuideRenderer(context: EntityRendererFactory.Context) :
    EntityRenderer<SpiritGuideEntity, SpiritGuideEntityRenderState>(context) {

    override fun createRenderState(): SpiritGuideEntityRenderState = SpiritGuideEntityRenderState()

    override fun render(
        state: SpiritGuideEntityRenderState,
        matrices: MatrixStack,
        vertexConsumers: VertexConsumerProvider,
        light: Int
    ) {
        matrices.push()

        matrices.translate(0.0, 1.0, 0.0)

        val camera = this.dispatcher.camera
        matrices.multiply(camera.rotation)
        matrices.multiply(Quaternionf().rotateY(3.1415927f))

        val vertexConsumer = vertexConsumers.getBuffer(RenderLayer.getEntityCutoutNoCull(TEXTURE))
        val matrix = matrices.peek()

        val size = 1.6f
        val halfSize = size / 2f
        val yOffset = size * 0.25f

        vertexConsumer.vertex(matrix, -halfSize, yOffset + size, 0f).color(255, 255, 255, 255).texture(0f, 0f)
            .overlay(OverlayTexture.DEFAULT_UV).light(light).normal(matrix, 0f, 0f, 1f)
        vertexConsumer.vertex(matrix, halfSize, yOffset + size, 0f).color(255, 255, 255, 255).texture(1f, 0f)
            .overlay(OverlayTexture.DEFAULT_UV).light(light).normal(matrix, 0f, 0f, 1f)
        vertexConsumer.vertex(matrix, halfSize, yOffset, 0f).color(255, 255, 255, 255).texture(1f, 1f)
            .overlay(OverlayTexture.DEFAULT_UV).light(light).normal(matrix, 0f, 0f, 1f)
        vertexConsumer.vertex(matrix, -halfSize, yOffset, 0f).color(255, 255, 255, 255).texture(0f, 1f)
            .overlay(OverlayTexture.DEFAULT_UV).light(light).normal(matrix, 0f, 0f, 1f)

        matrices.pop()
    }

    companion object {
        val TEXTURE: Identifier = Identifier.of(CreSoraUtilities.MOD_ID, "textures/entity/spirit_guide.png")
    }
}
