package hifumi.cresora.weapon
import hifumi.cresora.equipment.ArtifactChestScreenBase
import net.minecraft.client.gui.DrawContext
import net.minecraft.entity.player.PlayerInventory
import net.minecraft.text.Text

class WeaponSkillMaterialScreen(
    handler: WeaponSkillMaterialScreenHandler,
    inventory: PlayerInventory,
    title: Text
) : ArtifactChestScreenBase<WeaponSkillMaterialScreenHandler>(handler, inventory, title, 4) {
    override fun drawExtraForeground(context: DrawContext, mouseX: Int, mouseY: Int) {
        context.drawText(
            textRenderer,
            Text.translatable("screen.cresora.weapon_skill_material.count", handler.selectedCount(), handler.requiredCount()),
            86,
            6,
            0xFF241C14.toInt(),
            false
        )
    }
}
