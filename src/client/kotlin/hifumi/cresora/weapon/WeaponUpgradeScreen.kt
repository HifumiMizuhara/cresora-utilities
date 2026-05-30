package hifumi.cresora.weapon

import hifumi.cresora.CreSoraUtilities
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

    enum class Tab {
        STATUS,
        UPGRADE,
        SKILL,
        ARTIFACTS
    }

    private var activeTab = Tab.STATUS
    private lateinit var statusTabButton: ButtonWidget
    private lateinit var upgradeTabButton: ButtonWidget
    private lateinit var skillTabButton: ButtonWidget
    private lateinit var artifactsTabButton: ButtonWidget

    private lateinit var baseButton: ButtonWidget
    private lateinit var skillButton: ButtonWidget
    private lateinit var dismantleButton: ButtonWidget

    init {
        backgroundWidth = 312
        backgroundHeight = 166
        titleX = 8
        titleY = 6
        playerInventoryTitleX = 8
        playerInventoryTitleY = 76
    }

    override fun init() {
        super.init()

        statusTabButton = ButtonWidget.builder(Text.translatable("screen.cresora.weapon_upgrade.tab.status")) {
            activeTab = Tab.STATUS
            updateTabVisibility()
        }.dimensions(x + 176, y + 18, 31, 16).build()

        upgradeTabButton = ButtonWidget.builder(Text.translatable("screen.cresora.weapon_upgrade.tab.upgrade")) {
            activeTab = Tab.UPGRADE
            updateTabVisibility()
        }.dimensions(x + 208, y + 18, 31, 16).build()

        skillTabButton = ButtonWidget.builder(Text.translatable("screen.cresora.weapon_upgrade.tab.skill")) {
            activeTab = Tab.SKILL
            updateTabVisibility()
        }.dimensions(x + 240, y + 18, 31, 16).build()

        artifactsTabButton = ButtonWidget.builder(Text.translatable("screen.cresora.weapon_upgrade.tab.artifacts")) {
            activeTab = Tab.ARTIFACTS
            updateTabVisibility()
        }.dimensions(x + 272, y + 18, 31, 16).build()

        dismantleButton = ButtonWidget.builder(Text.translatable("screen.cresora.weapon_upgrade.dismantle_button")) {
            client?.interactionManager?.clickButton(handler.syncId, WeaponUpgradeScreenHandler.BUTTON_DISMANTLE)
        }.dimensions(x + 176, y + 141, 126, 15).build()

        baseButton = ButtonWidget.builder(Text.translatable("screen.cresora.weapon_upgrade.base_button")) {
            client?.interactionManager?.clickButton(handler.syncId, WeaponUpgradeScreenHandler.BUTTON_BASE_UPGRADE)
        }.dimensions(x + 176, y + 78, 126, 15).build()

        skillButton = ButtonWidget.builder(Text.translatable("screen.cresora.weapon_upgrade.skill_button")) {
            client?.interactionManager?.clickButton(handler.syncId, WeaponUpgradeScreenHandler.BUTTON_SKILL_UPGRADE)
        }.dimensions(x + 176, y + 120, 126, 15).build()

        addDrawableChild(statusTabButton)
        addDrawableChild(upgradeTabButton)
        addDrawableChild(skillTabButton)
        addDrawableChild(artifactsTabButton)
        addDrawableChild(dismantleButton)
        addDrawableChild(baseButton)
        addDrawableChild(skillButton)

        updateTabVisibility()
    }

    private fun updateTabVisibility() {
        statusTabButton.active = (activeTab != Tab.STATUS)
        upgradeTabButton.active = (activeTab != Tab.UPGRADE)
        skillTabButton.active = (activeTab != Tab.SKILL)
        artifactsTabButton.active = (activeTab != Tab.ARTIFACTS)

        val isUpgrade = (activeTab == Tab.UPGRADE)
        baseButton.visible = isUpgrade
        skillButton.visible = isUpgrade
        dismantleButton.visible = isUpgrade

        val isArtifacts = (activeTab == Tab.ARTIFACTS)
        for (i in 0 until 4) {
            val slot = handler.slots[1 + i]
            if (isArtifacts) {
                val col = i % 2
                val row = i / 2
                slot.x = 212 + col * 36
                slot.y = 60 + row * 36
            } else {
                slot.x = -2000
                slot.y = -2000
            }
        }
    }

    override fun handledScreenTick() {
        super.handledScreenTick()
        val player = client?.player ?: return
        val stack = handler.getWeaponStack()
        val hasWeapon = WeaponStackSupport.isWeapon(stack)

        statusTabButton.visible = hasWeapon
        upgradeTabButton.visible = hasWeapon
        skillTabButton.visible = hasWeapon
        artifactsTabButton.visible = hasWeapon

        if (!hasWeapon) {
            baseButton.visible = false
            skillButton.visible = false
            dismantleButton.visible = false
            return
        }

        updateTabVisibility()

        val basePreview = handler.getBasePreview(player)
        val skillPreview = handler.getSkillPreview(player)
        val dismantlePreview = handler.getDismantlePreview()

        baseButton.active = basePreview.canUpgrade
        skillButton.active = skillPreview.canUpgrade
        dismantleButton.active = dismantlePreview.canDismantle

        baseButton.message = if (basePreview.canUpgrade) {
            if (basePreview.messageKey == "screen.cresora.weapon_upgrade.ready_breakthrough") {
                Text.translatable("screen.cresora.weapon_upgrade.breakthrough_button")
            } else {
                Text.translatable("screen.cresora.weapon_upgrade.base_button")
            }
        } else {
            compactStatus(basePreview.messageKey, true)
        }

        skillButton.message = if (skillPreview.canUpgrade) {
            Text.translatable("screen.cresora.weapon_upgrade.skill_button")
        } else {
            compactStatus(skillPreview.messageKey, false)
        }

        dismantleButton.message = if (dismantlePreview.canDismantle) {
            if (handler.dismantleConfirmRemaining() in 1..2) {
                Text.translatable("screen.cresora.weapon_upgrade.dismantle_button_confirm", handler.dismantleConfirmRemaining())
            } else {
                Text.translatable("screen.cresora.weapon_upgrade.dismantle_button_value", formatWhole(dismantlePreview.returnCount))
            }
        } else {
            if (dismantlePreview.messageKey == "screen.cresora.weapon_upgrade.cannot_dismantle_has_artifacts") {
                Text.translatable("screen.cresora.weapon_upgrade.cannot_dismantle_has_artifacts")
            } else {
                Text.translatable("screen.cresora.weapon_upgrade.dismantle_button")
            }
        }
    }

    override fun drawBackground(context: DrawContext, delta: Float, mouseX: Int, mouseY: Int) {
        context.fill(x, y, x + backgroundWidth, y + backgroundHeight, 0xFF2A221A.toInt())
        context.fill(x + 2, y + 2, x + backgroundWidth - 2, y + backgroundHeight - 2, 0xFFF0E2C2.toInt())
        
        // Left showcase panel (showcases weapon slot at x+79, y+26)
        context.fill(x + 7, y + 18, x + 169, y + 78, 0xFFE4D3B0.toInt())
        drawSlotFrame(context, x + 78, y + 25)

        // Player inventory slot backgrounds
        drawInventorySlots(context)

        // Right side info panel under tabs
        context.fill(x + 174, y + 36, x + 304, y + 160, 0xFFE4D3B0.toInt())

        // Render Weapon Artifact slot frames
        if (activeTab == Tab.ARTIFACTS) {
            for (i in 0 until 4) {
                val col = i % 2
                val row = i / 2
                drawSlotFrame(context, x + 211 + col * 36, y + 59 + row * 36)
            }
        }
    }

    private fun drawInventorySlots(context: DrawContext) {
        for (row in 0 until 3) {
            for (column in 0 until 9) {
                drawSlotFrame(context, x + 7 + column * 18, y + 83 + row * 18)
            }
        }
        for (column in 0 until 9) {
            drawSlotFrame(context, x + 7 + column * 18, y + 141)
        }
    }

    private fun drawSlotFrame(context: DrawContext, slotX: Int, slotY: Int) {
        context.fill(slotX, slotY, slotX + 18, slotY + 18, 0xFF2A221A.toInt())
        context.fill(slotX + 1, slotY + 1, slotX + 17, slotY + 17, 0xFF140E0A.toInt())
    }

    override fun drawForeground(context: DrawContext, mouseX: Int, mouseY: Int) {
        val player = client?.player ?: return
        val stack = handler.getWeaponStack()
        val definition = WeaponStackSupport.getDefinition(stack)
        val data = WeaponStackSupport.getWeaponData(stack)
        val basePreview = handler.getBasePreview(player)
        val skillPreview = handler.getSkillPreview(player)

        context.drawText(textRenderer, title, titleX, titleY, 0xFF30261A.toInt(), false)
        context.drawText(textRenderer, playerInventoryTitle, playerInventoryTitleX, playerInventoryTitleY, 0xFF5F503D.toInt(), false)

        if (definition != null && data != null) {
            val displayName = Text.translatable("screen.cresora.weapon_upgrade.display_name", Text.translatable(definition.translationKey()), data.breakthrough)
            val nameWidth = textRenderer.getWidth(displayName)
            context.drawText(textRenderer, displayName, 88 - nameWidth / 2, 48, 0xFF2F1D0D.toInt(), false)

            val starStr = "★".repeat(data.rarity.stars)
            val starsWidth = textRenderer.getWidth(starStr)
            context.drawText(textRenderer, Text.literal(starStr), 88 - starsWidth / 2, 57, 0xFF8B6A34.toInt(), false)

            val creditsStr = Text.translatable("screen.cresora.weapon_upgrade.have_credits", formatWhole(handler.currentCredits()))
            val creditsWidth = textRenderer.getWidth(creditsStr)
            context.drawText(textRenderer, creditsStr, 88 - creditsWidth / 2, 66, 0xFF5F503D.toInt(), false)

            when (activeTab) {
                Tab.STATUS -> renderStatusTab(context, definition, data)
                Tab.UPGRADE -> renderUpgradeTab(context, basePreview, skillPreview, definition)
                Tab.SKILL -> renderSkillTab(context, definition, data, skillPreview)
                Tab.ARTIFACTS -> renderArtifactsTab(context)
            }
        } else {
            val needWeaponText = Text.translatable("screen.cresora.weapon_upgrade.need_weapon")
            val needWeaponWidth = textRenderer.getWidth(needWeaponText)
            context.drawText(textRenderer, needWeaponText, 88 - needWeaponWidth / 2, 48, 0xFF8F2E23.toInt(), false)

            val infoText = Text.translatable("screen.cresora.weapon_upgrade.need_weapon_info")
            val infoWidth = textRenderer.getWidth(infoText)
            context.drawText(textRenderer, infoText, 239 - infoWidth / 2, 88, 0xFF5F503D.toInt(), false)
        }
    }

    private fun renderArtifactsTab(context: DrawContext) {
        val tabTitle = Text.translatable("screen.cresora.weapon_upgrade.tab.artifacts.title")
        context.drawText(textRenderer, tabTitle, 178, 40, 0xFF2F1D0D.toInt(), false)

        val descText = Text.translatable("screen.cresora.weapon_upgrade.tab.artifacts.desc")
        context.drawWrappedText(textRenderer, descText, 178, 105, 122, 0xFF5F503D.toInt(), false)
    }

    private fun renderStatusTab(context: DrawContext, definition: WeaponDefinition, data: WeaponData) {
        var currentY = 40

        // Base ATK
        val atkVal = WeaponCombatSupport.attackDamage(definition, data)
        context.drawText(textRenderer, Text.translatable("screen.cresora.weapon_upgrade.stat.atk"), 178, currentY, 0xFF5F503D.toInt(), false)
        context.drawText(textRenderer, Text.literal(formatOne(atkVal)), 246, currentY, 0xFF2F1D0D.toInt(), false)
        currentY += 12

        // Crit Rate
        val critRate = WeaponCombatSupport.critRateBonusPercent(definition, data)
        if (definition.critRateBonusPercent > 0.0 || critRate > 0.0) {
            context.drawText(textRenderer, Text.translatable("screen.cresora.weapon_upgrade.stat.crit"), 178, currentY, 0xFF5F503D.toInt(), false)
            context.drawText(textRenderer, Text.literal("+${formatOne(critRate)}%"), 246, currentY, 0xFF2F1D0D.toInt(), false)
            currentY += 12
        }

        // All Damage Bonus
        val allDmg = WeaponCombatSupport.allDamageBonusPercent(definition, data)
        if (definition.maxAllDamageBonusPercent > 0.0 || allDmg > 0.0) {
            context.drawText(textRenderer, Text.translatable("screen.cresora.weapon_upgrade.stat.all_dmg"), 178, currentY, 0xFF5F503D.toInt(), false)
            context.drawText(textRenderer, Text.literal("+${formatOne(allDmg)}%"), 246, currentY, 0xFF2F1D0D.toInt(), false)
            currentY += 12
        }

        // HP Bonus
        if (definition.hpBonusPercent > 0.0) {
            context.drawText(textRenderer, Text.translatable("screen.cresora.weapon_upgrade.stat.hp"), 178, currentY, 0xFF5F503D.toInt(), false)
            context.drawText(textRenderer, Text.literal("+${formatOne(definition.hpBonusPercent)}%"), 246, currentY, 0xFF2F1D0D.toInt(), false)
            currentY += 12
        }

        // Speed
        context.drawText(textRenderer, Text.translatable("screen.cresora.weapon_upgrade.stat.speed"), 178, currentY, 0xFF5F503D.toInt(), false)
        context.drawText(textRenderer, Text.literal(formatOne(definition.totalAttackSpeed)), 246, currentY, 0xFF2F1D0D.toInt(), false)
        currentY += 12

        // Damage Type
        context.drawText(textRenderer, Text.translatable("screen.cresora.weapon_upgrade.stat.type"), 178, currentY, 0xFF5F503D.toInt(), false)
        context.drawText(textRenderer, Text.translatable(definition.damageType.translationKey()), 246, currentY, 0xFF2F1D0D.toInt(), false)
        currentY += 12

        // Role
        context.drawText(textRenderer, Text.translatable("screen.cresora.weapon_upgrade.stat.role"), 178, currentY, 0xFF5F503D.toInt(), false)
        context.drawText(textRenderer, Text.translatable(definition.role.translationKey()), 246, currentY, 0xFF2F1D0D.toInt(), false)
        currentY += 12

        // Rarity
        context.drawText(textRenderer, Text.translatable("screen.cresora.weapon_upgrade.stat.rarity"), 178, currentY, 0xFF5F503D.toInt(), false)
        context.drawText(textRenderer, Text.translatable(data.rarity.translationKey()), 246, currentY, 0xFF8B6A34.toInt(), false)
    }

    private fun renderUpgradeTab(
        context: DrawContext,
        basePreview: WeaponUpgradeLogic.BasePreview,
        skillPreview: WeaponUpgradeLogic.SkillPreview,
        definition: WeaponDefinition
    ) {
        // --- Base Upgrade Section ---
        context.drawText(
            textRenderer,
            Text.translatable("screen.cresora.weapon_upgrade.base_line", basePreview.currentLevel, basePreview.resultLevel),
            178,
            38,
            0xFF2F1D0D.toInt(),
            false
        )
        context.drawText(
            textRenderer,
            Text.translatable("screen.cresora.weapon_upgrade.base_attack", formatOne(basePreview.currentAttack), formatOne(basePreview.resultAttack)),
            178,
            46,
            0xFF2F1D0D.toInt(),
            false
        )
        context.drawText(
            textRenderer,
            Text.translatable("screen.cresora.weapon_upgrade.fragments", formatWhole(basePreview.fragmentCost), formatWhole(handler.currentFragments())),
            178,
            54,
            if (handler.currentFragments() >= basePreview.fragmentCost) 0xFF2F1D0D.toInt() else 0xFF8F2E23.toInt(),
            false
        )

        var costY = 62
        if (basePreview.roleMaterialCost > 0) {
            val data = WeaponStackSupport.getWeaponData(handler.getWeaponStack())
            val breakthroughLevel = data?.breakthrough ?: 0
            val roleItem = if (breakthroughLevel == 0) {
                CreSoraUtilities.getRoleProofItem(definition.role)
            } else {
                CreSoraUtilities.getRoleInsightItem(definition.role)
            }
            context.drawText(
                textRenderer,
                Text.translatable(
                    "screen.cresora.weapon_upgrade.role_materials",
                    roleItem.name,
                    formatWhole(basePreview.roleMaterialCost),
                    formatWhole(basePreview.availableRoleMaterials)
                ),
                178,
                62,
                if (basePreview.availableRoleMaterials >= basePreview.roleMaterialCost) 0xFF2F1D0D.toInt() else 0xFF8F2E23.toInt(),
                false
            )
            costY = 70
        }

        context.drawText(
            textRenderer,
            Text.translatable("screen.cresora.weapon_upgrade.credits", formatWhole(basePreview.cscCost)),
            178,
            costY,
            if (handler.currentCredits() >= basePreview.cscCost) 0xFF2F1D0D.toInt() else 0xFF8F2E23.toInt(),
            false
        )

        // --- Skill Upgrade Section ---
        context.drawText(
            textRenderer,
            Text.translatable("screen.cresora.weapon_upgrade.skill_line", skillPreview.currentLevel, skillPreview.resultLevel),
            178,
            94,
            0xFF2F1D0D.toInt(),
            false
        )
        context.drawText(
            textRenderer,
            Text.translatable(
                "screen.cresora.weapon_upgrade.skill_value_line",
                Text.empty(),
                formatOne(skillPreview.currentValueHearts),
                formatOne(skillPreview.resultValueHearts),
                skillUnit(skillPreview.effectId)
            ),
            178,
            102,
            0xFF2F1D0D.toInt(),
            false
        )

        var skillCostY = 110
        if (skillPreview.artifactCost > 0) {
            context.drawText(
                textRenderer,
                Text.translatable(
                    "screen.cresora.weapon_upgrade.skill_artifacts",
                    formatWhole(skillPreview.artifactCost),
                    formatWhole(handler.currentArtifacts())
                ),
                178,
                110,
                if (handler.currentArtifacts() >= skillPreview.artifactCost) 0xFF2F1D0D.toInt() else 0xFF8F2E23.toInt(),
                false
            )
            skillCostY = 118
        }

        context.drawText(
            textRenderer,
            Text.translatable("screen.cresora.weapon_upgrade.skill_cost", formatWhole(skillPreview.cscCost)),
            178,
            skillCostY,
            if (handler.currentCredits() >= skillPreview.cscCost) 0xFF2F1D0D.toInt() else 0xFF8F2E23.toInt(),
            false
        )
    }

    private fun renderSkillTab(
        context: DrawContext,
        definition: WeaponDefinition,
        data: WeaponData,
        skillPreview: WeaponUpgradeLogic.SkillPreview
    ) {
        val skillNameKey = if (net.minecraft.util.Language.getInstance().hasTranslation("item.cresora-utilities.${definition.id}.skill")) {
            "item.cresora-utilities.${definition.id}.skill"
        } else {
            "item.cresora.weapon.skill.${definition.skill.effectId}"
        }
        val effectName = Text.translatable(skillNameKey)
        context.drawText(textRenderer, effectName, 178, 38, 0xFF2F1D0D.toInt(), false)

        val lvText = Text.literal("Lv. ${data.skillLevel} / ${definition.maxSkillLevel}")
        context.drawText(textRenderer, lvText, 178, 48, 0xFF5F503D.toInt(), false)

        val fullDesc = buildSkillDescription(definition, data)
        context.drawWrappedText(textRenderer, fullDesc, 178, 58, 122, 0xFF2F1D0D.toInt(), false)

        if (data.skillLevel < definition.maxSkillLevel) {
            context.drawText(
                textRenderer,
                Text.translatable("screen.cresora.weapon_upgrade.tab.skill.next_preview"),
                178,
                114,
                0xFF5F503D.toInt(),
                false
            )
            val valPreviewText = Text.translatable(
                "screen.cresora.weapon_upgrade.skill_value_line",
                Text.empty(),
                formatOne(skillPreview.currentValueHearts),
                formatOne(skillPreview.resultValueHearts),
                skillUnit(skillPreview.effectId)
            )
            context.drawText(textRenderer, valPreviewText, 178, 124, 0xFF1F6A52.toInt(), false)

            val cdText = Text.translatable(
                "item.cresora.weapon.skill.cooldown_progress",
                Text.empty(),
                definition.skill.cooldownSeconds
            )
            context.drawText(textRenderer, cdText, 178, 134, 0xFF5F503D.toInt(), false)
        }
    }

    override fun render(context: DrawContext, mouseX: Int, mouseY: Int, delta: Float) {
        renderBackground(context, mouseX, mouseY, delta)
        super.render(context, mouseX, mouseY, delta)
        drawMouseoverTooltip(context, mouseX, mouseY)
    }

    private fun formatWhole(value: Int): String = String.format(Locale.ROOT, "%,d", value)

    private fun formatOne(value: Double): String = String.format(Locale.ROOT, "%.1f", value)

    private fun skillUnit(effectId: String): Text {
        return when (effectId) {
            "current_hp_true_damage" -> Text.translatable("item.cresora.weapon.unit.percent_current_hp")
            "flame_aura" -> Text.translatable("item.cresora.weapon.unit.seconds")
            "sunlit_haste" -> Text.translatable("item.cresora.weapon.unit.percent")
            else -> Text.translatable("item.cresora.weapon.unit.hearts")
        }
    }

    private fun buildSkillDescription(definition: WeaponDefinition, data: WeaponData): Text {
        val skillNameKey = if (net.minecraft.util.Language.getInstance().hasTranslation("item.cresora-utilities.${definition.id}.skill")) {
            "item.cresora-utilities.${definition.id}.skill"
        } else {
            "item.cresora.weapon.skill.${definition.skill.effectId}"
        }
        val effectName = Text.translatable(skillNameKey)

        val descKey1 = "item.cresora-utilities.${definition.id}.skill.desc"
        val descKey2 = "item.cresora.weapon.skill.${definition.skill.effectId}.desc"
        val lang = net.minecraft.util.Language.getInstance()
        if (lang.hasTranslation(descKey1)) {
            return Text.translatable(descKey1)
        }
        if (lang.hasTranslation(descKey2)) {
            return Text.translatable(descKey2)
        }

        val heartValue = formatOne(WeaponCombatSupport.skillValueHearts(definition, data))
        val secondaryHeartValue = formatOne(WeaponCombatSupport.secondarySkillValueHearts(definition, data))
        val percentValue = formatOne(WeaponCombatSupport.skillValuePercent(definition, data))
        val secondaryPercentValue = formatOne(WeaponCombatSupport.secondarySkillValuePercent(definition, data))
        return when (definition.skill.effectId) {
            "heal" -> Text.translatable(
                "item.cresora.weapon.skill_line_heal",
                effectName,
                heartValue,
                Text.translatable("item.cresora.weapon.unit.hearts"),
                definition.skill.cooldownSeconds
            )
            "flame_aura" -> Text.translatable(
                "item.cresora.weapon.skill_line_flame_aura",
                effectName,
                percentValue,
                Text.translatable("item.cresora.weapon.unit.seconds"),
                formatOne(definition.skill.radiusMeters),
                definition.skill.durationSeconds,
                definition.skill.cooldownSeconds
            )
            "current_hp_true_damage" -> Text.translatable(
                "item.cresora.weapon.skill_line_percent_burst",
                effectName,
                percentValue,
                Text.translatable("item.cresora.weapon.unit.percent_current_hp"),
                formatOne(definition.skill.radiusMeters),
                definition.skill.cooldownSeconds
            )
            "snow_frost" -> Text.translatable(
                "item.cresora.weapon.skill_line_snow_frost",
                effectName,
                heartValue,
                Text.translatable("item.cresora.weapon.unit.hearts"),
                definition.skill.durationSeconds,
                definition.skill.cooldownSeconds
            )
            "healing_aura" -> Text.translatable(
                "item.cresora.weapon.skill_line_healing_aura",
                effectName,
                heartValue,
                Text.translatable("item.cresora.weapon.unit.hearts"),
                secondaryHeartValue,
                Text.translatable("item.cresora.weapon.unit.hearts"),
                formatOne(definition.skill.radiusMeters),
                definition.skill.durationSeconds,
                definition.skill.cooldownSeconds
            )
            "dark_lux" -> Text.translatable(
                "item.cresora.weapon.skill_line_dark_lux",
                effectName,
                definition.skill.durationSeconds,
                definition.skill.cooldownSeconds
            )
            "sunlit_haste" -> Text.translatable(
                "item.cresora.weapon.skill_line_sunlit_haste",
                effectName,
                percentValue,
                secondaryPercentValue,
                definition.skill.durationSeconds,
                definition.skill.cooldownSeconds
            )
            "baa_mimic" -> {
                val targetCount = when {
                    data.baseLevel >= 61 -> 3
                    data.baseLevel >= 41 -> 2
                    else -> 1
                }
                Text.translatable(
                    "item.cresora.weapon.skill_line_baa_mimic",
                    effectName,
                    targetCount,
                    formatOne(definition.skill.radiusMeters),
                    definition.skill.cooldownSeconds
                )
            }
            "orchid_pavilion_echo" -> Text.translatable(
                "item.cresora.weapon.skill_line_orchid_pavilion_echo",
                effectName,
                definition.skill.durationSeconds,
                formatOne(definition.skill.tickIntervalSeconds),
                definition.skill.cooldownSeconds
            )
            "none" -> Text.translatable(
                "item.cresora.weapon.skill_line_none",
                effectName
            )
            else -> Text.translatable(
                "item.cresora.weapon.skill_line_shield",
                effectName,
                heartValue,
                Text.translatable("item.cresora.weapon.unit.hearts"),
                definition.skill.durationSeconds,
                definition.skill.cooldownSeconds
            )
        }
    }

    private fun compactStatus(messageKey: String?, base: Boolean): Text {
        return when (messageKey) {
            "screen.cresora.weapon_upgrade.need_weapon" -> Text.translatable("screen.cresora.weapon_upgrade.need_weapon")
            "screen.cresora.weapon_upgrade.max_base" -> Text.translatable("screen.cresora.weapon_upgrade.max_base")
            "screen.cresora.weapon_upgrade.max_skill" -> Text.translatable("screen.cresora.weapon_upgrade.max_skill")
            "screen.cresora.weapon_upgrade.no_fragments" -> Text.translatable("screen.cresora.weapon_upgrade.no_fragments")
            "screen.cresora.weapon_upgrade.no_role_materials" -> Text.translatable("screen.cresora.weapon_upgrade.no_role_materials")
            "screen.cresora.weapon_upgrade.no_artifact_materials" -> Text.translatable("screen.cresora.weapon_upgrade.no_artifact_materials")
            "screen.cresora.weapon_upgrade.ready_base" -> Text.translatable("screen.cresora.weapon_upgrade.ready_base")
            "screen.cresora.weapon_upgrade.ready_skill" -> Text.translatable("screen.cresora.weapon_upgrade.ready_skill")
            "screen.cresora.weapon_upgrade.ready_skill_select" -> Text.translatable("screen.cresora.weapon_upgrade.ready_skill_select")
            "screen.cresora.weapon_upgrade.ready_dismantle" -> {
                if (handler.dismantleConfirmRemaining() in 1..2) {
                    Text.translatable("screen.cresora.weapon_upgrade.confirm_short", handler.dismantleConfirmRemaining())
                } else {
                    Text.translatable("screen.cresora.weapon_upgrade.ready_dismantle")
                }
            }
            "item.cresora.not_enough_credits" -> Text.translatable("screen.cresora.weapon_upgrade.no_credits")
            "screen.cresora.weapon_upgrade.max_breakthrough" -> Text.translatable("screen.cresora.weapon_upgrade.max_breakthrough")
            "screen.cresora.weapon_upgrade.need_breakthrough" -> Text.translatable("screen.cresora.weapon_upgrade.need_breakthrough")
            "screen.cresora.weapon_upgrade.ready_breakthrough" -> Text.translatable("screen.cresora.weapon_upgrade.ready_breakthrough")
            else -> if (base) Text.translatable("screen.cresora.weapon_upgrade.base_button") else Text.translatable("screen.cresora.weapon_upgrade.skill_button")
        }
    }
}
