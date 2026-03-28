package hifumi.cresora

import net.minecraft.client.gui.DrawContext
import net.minecraft.client.gui.screen.ingame.HandledScreen
import net.minecraft.client.gui.widget.ButtonWidget
import net.minecraft.entity.player.PlayerInventory
import net.minecraft.text.Text
import java.util.Locale

class WeaponUpgradeScreen(
    handler: WeaponUpgradeScreenHandler,
    inventory: PlayerInventory,
    title: Text
) : HandledScreen<WeaponUpgradeScreenHandler>(handler, inventory, title) {
    private lateinit var baseButton: ButtonWidget
    private lateinit var skillButton: ButtonWidget

    init {
        backgroundWidth = 176
        backgroundHeight = 166
        titleX = 8
        titleY = 6
        playerInventoryTitleX = 8
        playerInventoryTitleY = 72
    }

    override fun init() {
        super.init()
        baseButton = ButtonWidget.builder(Text.translatable("screen.cresora.weapon_upgrade.base_button")) {
            client?.interactionManager?.clickButton(handler.syncId, WeaponUpgradeScreenHandler.BUTTON_BASE_UPGRADE)
        }.dimensions(x + 100, y + 28, 68, 20).build()
        skillButton = ButtonWidget.builder(Text.translatable("screen.cresora.weapon_upgrade.skill_button")) {
            client?.interactionManager?.clickButton(handler.syncId, WeaponUpgradeScreenHandler.BUTTON_SKILL_UPGRADE)
        }.dimensions(x + 100, y + 52, 68, 20).build()
        addDrawableChild(baseButton)
        addDrawableChild(skillButton)
    }

    override fun handledScreenTick() {
        super.handledScreenTick()
        val player = client?.player ?: return
        val basePreview = handler.getBasePreview(player)
        val skillPreview = handler.getSkillPreview(player)
        baseButton.active = basePreview.canUpgrade
        skillButton.active = skillPreview.canUpgrade
        baseButton.message = if (basePreview.canUpgrade) {
            Text.translatable("screen.cresora.weapon_upgrade.base_button")
        } else {
            compactStatus(basePreview.messageKey, true)
        }
        skillButton.message = if (skillPreview.canUpgrade) {
            Text.translatable("screen.cresora.weapon_upgrade.skill_button")
        } else {
            compactStatus(skillPreview.messageKey, false)
        }
    }

    override fun drawBackground(context: DrawContext, delta: Float, mouseX: Int, mouseY: Int) {
        context.fill(x, y, x + backgroundWidth, y + backgroundHeight, 0xFF2A221A.toInt())
        context.fill(x + 2, y + 2, x + backgroundWidth - 2, y + backgroundHeight - 2, 0xFFF0E2C2.toInt())
        context.fill(x + 7, y + 18, x + 91, y + 69, 0xFFE4D3B0.toInt())
        context.fill(x + 95, y + 18, x + 169, y + 69, 0xFFE4D3B0.toInt())
    }

    override fun drawForeground(context: DrawContext, mouseX: Int, mouseY: Int) {
        val player = client?.player ?: return
        val stack = handler.getWeaponStack()
        val definition = WeaponStackSupport.getDefinition(stack)
        val data = WeaponStackSupport.getWeaponData(stack)
        val basePreview = handler.getBasePreview(player)
        val skillPreview = handler.getSkillPreview(player)

        context.drawText(textRenderer, title, titleX, titleY, 0x30261A, false)
        context.drawText(textRenderer, playerInventoryTitle, playerInventoryTitleX, playerInventoryTitleY, 0x5F503D, false)
        if (definition != null && data != null) {
            context.drawText(textRenderer, Text.translatable(definition.translationKey()), 8, 20, 0x2F1D0D, false)
            context.drawText(textRenderer, Text.translatable(data.rarity.translationKey()), 8, 31, 0x8B6A34, false)
            context.drawText(
                textRenderer,
                Text.translatable("screen.cresora.weapon_upgrade.fragments", formatWhole(basePreview.fragmentCost), formatWhole(handler.currentFragments())),
                8,
                42,
                0x2F1D0D,
                false
            )
        } else {
            context.drawText(textRenderer, Text.translatable("screen.cresora.weapon_upgrade.need_weapon"), 8, 20, 0x8F2E23, false)
        }

        context.drawText(
            textRenderer,
            Text.translatable("screen.cresora.weapon_upgrade.base_line", basePreview.currentLevel, basePreview.resultLevel),
            8,
            54,
            0x2F1D0D,
            false
        )
        context.drawText(
            textRenderer,
            Text.translatable("screen.cresora.weapon_upgrade.base_attack", formatOne(basePreview.currentAttack), formatOne(basePreview.resultAttack)),
            8,
            64,
            0x2F1D0D,
            false
        )
        context.drawText(
            textRenderer,
            Text.translatable("screen.cresora.weapon_upgrade.skill_line", skillPreview.currentLevel, skillPreview.resultLevel),
            95,
            78,
            0x2F1D0D,
            false
        )
        context.drawText(
            textRenderer,
            Text.translatable(
                "screen.cresora.weapon_upgrade.skill_value_line",
                Text.translatable("item.cresora.weapon.skill.${skillPreview.effectId}"),
                formatOne(skillPreview.currentValueHearts),
                formatOne(skillPreview.resultValueHearts),
                Text.translatable("item.cresora.weapon.unit.hearts")
            ),
            95,
            88,
            0x2F1D0D,
            false
        )
        context.drawText(
            textRenderer,
            Text.translatable("screen.cresora.weapon_upgrade.credits", formatWhole(basePreview.cscCost)),
            95,
            20,
            0x2F1D0D,
            false
        )
        context.drawText(
            textRenderer,
            Text.translatable("screen.cresora.weapon_upgrade.have_credits", formatWhole(handler.currentCredits())),
            95,
            30,
            0x2F1D0D,
            false
        )
        context.drawText(
            textRenderer,
            Text.translatable("screen.cresora.weapon_upgrade.skill_cost", formatWhole(skillPreview.cscCost)),
            95,
            98,
            0x2F1D0D,
            false
        )
        context.drawText(textRenderer, compactStatus(basePreview.messageKey, true), 8, 74, statusColor(basePreview.canUpgrade), false)
        context.drawText(textRenderer, compactStatus(skillPreview.messageKey, false), 95, 108, statusColor(skillPreview.canUpgrade), false)
    }

    override fun render(context: DrawContext, mouseX: Int, mouseY: Int, delta: Float) {
        renderBackground(context, mouseX, mouseY, delta)
        super.render(context, mouseX, mouseY, delta)
        drawMouseoverTooltip(context, mouseX, mouseY)
    }

    private fun formatWhole(value: Int): String = String.format(Locale.ROOT, "%,d", value)

    private fun formatOne(value: Double): String = String.format(Locale.ROOT, "%.1f", value)

    private fun statusColor(ready: Boolean): Int = if (ready) 0x1F6A52 else 0x8F2E23

    private fun compactStatus(messageKey: String?, base: Boolean): Text {
        return when (messageKey) {
            "screen.cresora.weapon_upgrade.need_weapon" -> Text.translatable("screen.cresora.weapon_upgrade.need_weapon")
            "screen.cresora.weapon_upgrade.max_base" -> Text.translatable("screen.cresora.weapon_upgrade.max_base")
            "screen.cresora.weapon_upgrade.max_skill" -> Text.translatable("screen.cresora.weapon_upgrade.max_skill")
            "screen.cresora.weapon_upgrade.no_fragments" -> Text.translatable("screen.cresora.weapon_upgrade.no_fragments")
            "screen.cresora.weapon_upgrade.ready_base" -> Text.translatable("screen.cresora.weapon_upgrade.ready_base")
            "screen.cresora.weapon_upgrade.ready_skill" -> Text.translatable("screen.cresora.weapon_upgrade.ready_skill")
            "item.cresora.not_enough_credits" -> Text.translatable("screen.cresora.weapon_upgrade.no_credits")
            else -> if (base) Text.translatable("screen.cresora.weapon_upgrade.base_button") else Text.translatable("screen.cresora.weapon_upgrade.skill_button")
        }
    }
}
