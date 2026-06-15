package hifumi.cresora.equipment
import hifumi.cresora.ShopResourceOfferDefinition
import hifumi.cresora.StatType
import hifumi.cresora.adventurerank.AdventureRankService
import hifumi.cresora.domain.DomainCombatProfile
import hifumi.cresora.domain.DomainContentRegistry
import hifumi.cresora.domain.DomainDisplayStackFactory
import hifumi.cresora.domain.DomainRewardProfileRegistry
import hifumi.cresora.masquerade.MasqueradeSeasonRecord
import hifumi.cresora.masquerade.MasqueradeSupportBuffDefinition
import hifumi.cresora.resonance.ResonanceService.PullResult
import hifumi.cresora.resonance.ResonanceBannerDefinition
import hifumi.cresora.resonance.ResonanceBannerType
import hifumi.cresora.resonance.ResonanceCurrencyType
import hifumi.cresora.resonance.ResonanceService
import hifumi.cresora.story.StoryChapterDefinition
import hifumi.cresora.story.StoryContentRegistry
import hifumi.cresora.story.StoryDisplayText
import hifumi.cresora.story.StoryProgressService
import hifumi.cresora.story.StoryTextRegistry
import net.minecraft.component.DataComponentTypes
import net.minecraft.component.type.LoreComponent
import net.minecraft.item.Item
import net.minecraft.item.ItemStack
import net.minecraft.item.Items
import net.minecraft.registry.Registries
import net.minecraft.text.Text
import net.minecraft.util.Identifier

object ArtifactDisplayStackFactory {
    fun fillerDisplay(): ItemStack {
        return ItemStack(Items.GRAY_STAINED_GLASS_PANE).apply {
            set(DataComponentTypes.CUSTOM_NAME, Text.empty())
        }
    }

    fun cresoraMenuDisplay(optionId: String): ItemStack {
        val item = when (optionId) {
            "story" -> Items.WRITABLE_BOOK
            "domain" -> Items.IRON_SWORD
            "masquerade" -> Items.ENDER_EYE
            "shop" -> Items.CHEST
            "resonance" -> Items.NOTE_BLOCK
            else -> Items.BARRIER
        }
        return ItemStack(item).apply {
            set(DataComponentTypes.CUSTOM_NAME, Text.translatable("screen.cresora.menu.option.$optionId"))
        }
    }

    fun storyChapterGroupDisplay(groupId: String, chapterCount: Int): ItemStack {
        return ItemStack(Items.BOOK).apply {
            set(
                DataComponentTypes.CUSTOM_NAME,
                Text.translatable("screen.cresora.story.chapter_group_entry", groupId, chapterCount)
            )
        }
    }

    fun storyChapterEntryDisplay(chapter: StoryChapterDefinition, player: net.minecraft.server.network.ServerPlayerEntity?): ItemStack {
        val missingPrerequisite = player?.let { StoryProgressService.missingPrerequisite(it, chapter) }
        val locale = StoryTextRegistry.resolvePlayerLocale(player)
        val chapterLabel = StoryTextRegistry.chapterLabel(locale, chapter)
        val prerequisiteLabel = missingPrerequisite?.let {
            runCatching { StoryTextRegistry.chapterLabel(locale, StoryContentRegistry.requireChapter(it)) }.getOrDefault(it)
        }
        val linkedDomain = chapter.linkedDomainId?.let(DomainContentRegistry::requireDomain)
        val stack = if (linkedDomain != null) DomainDisplayStackFactory.storyLinkedDisplay(chapter, linkedDomain) else ItemStack(Items.WRITTEN_BOOK)
        return stack.apply {
            set(
                DataComponentTypes.CUSTOM_NAME,
                if (prerequisiteLabel != null) {
                    Text.translatable("screen.cresora.story.chapter_entry_prerequisite", chapterLabel, prerequisiteLabel)
                } else {
                    Text.translatable("screen.cresora.story.chapter_entry", chapterLabel, chapter.unlockRank)
                }
            )
            set(
                DataComponentTypes.LORE,
                LoreComponent(
                    buildList {
                        add(Text.translatable("screen.cresora.story.objective_label"))
                        if (linkedDomain != null) {
                            val rewardProfile = DomainRewardProfileRegistry.requireProfile(linkedDomain.rewardProfileId)
                            val playerRank = player?.let(AdventureRankService::getRank) ?: linkedDomain.unlockRank
                            val recommendedBand = DomainCombatProfile.recommendedBand(maxOf(playerRank, linkedDomain.unlockRank))
                            add(Text.translatable("screen.cresora.story.objective.domain_clear"))
                            add(Text.translatable("screen.cresora.story.domain_reward_line", DomainDisplayStackFactory.rewardLabel(rewardProfile)))
                            add(Text.translatable("screen.cresora.story.domain_cost_line", ArtifactSpecialItem.formatWholeNumber(linkedDomain.entryCostCsc)))
                            add(Text.translatable("screen.cresora.story.domain_band_line", recommendedBand.first, recommendedBand.second))
                        } else {
                            add(StoryDisplayText.objectiveText(chapter))
                            if (chapter.domainRewardIds.isNotEmpty()) {
                                for (domainRewardId in chapter.domainRewardIds) {
                                    val rewardDomain = DomainContentRegistry.requireDomain(domainRewardId)
                                    val rewardProfile = DomainRewardProfileRegistry.requireProfile(rewardDomain.rewardProfileId)
                                    add(
                                        Text.translatable(
                                            "screen.cresora.story.domain_reward_line",
                                            DomainDisplayStackFactory.rewardLabel(rewardProfile)
                                        )
                                    )
                                }
                            }
                        }
                        if (chapter.combatHints.isNotEmpty()) {
                            add(Text.translatable("screen.cresora.story.hints_label"))
                            addAll(StoryDisplayText.combatHintTexts(player, chapter))
                        }
                    }
                )
            )
        }
    }

    fun shopDisplay(definition: ArtifactSpecialItemDefinition): ItemStack {
        val item = ArtifactSpecialItemSupport.itemForDefinitionId(definition.id) ?: Items.BARRIER
        return ItemStack(item).apply {
            set(
                DataComponentTypes.CUSTOM_NAME,
                Text.translatable("screen.cresora.shop.entry", Text.translatable(definition.translationKeyId), ArtifactSpecialItem.formatWholeNumber(definition.shopPrice))
            )
        }
    }

    fun shopDisplay(definition: ShopResourceOfferDefinition): ItemStack {
        val item = Registries.ITEM.get(Identifier.of(definition.itemId))
        return ItemStack(item, definition.count).apply {
            set(
                DataComponentTypes.CUSTOM_NAME,
                Text.translatable("screen.cresora.shop.entry", Text.translatable(item.translationKey), ArtifactSpecialItem.formatWholeNumber(definition.price))
            )
        }
    }

    fun shopSellPreview(preview: ArtifactSellPreview): ItemStack {
        val item = if (preview.sellable) Items.SUNFLOWER else Items.BARRIER
        return ItemStack(item).apply {
            set(
                DataComponentTypes.CUSTOM_NAME,
                if (preview.sellable) {
                    Text.translatable("screen.cresora.shop.sell_ready", ArtifactSpecialItem.formatWholeNumber(preview.totalPrice))
                } else {
                    Text.translatable("screen.cresora.shop.sell_empty")
                }
            )
        }
    }

    fun shopSellButton(preview: ArtifactSellPreview): ItemStack {
        val item = if (preview.sellable) Items.EMERALD else Items.HOPPER
        return ItemStack(item).apply {
            set(
                DataComponentTypes.CUSTOM_NAME,
                if (preview.sellable) {
                    Text.translatable("screen.cresora.shop.sell_button", ArtifactSpecialItem.formatWholeNumber(preview.totalPrice))
                } else {
                    Text.translatable("screen.cresora.shop.sell_button_idle")
                }
            )
        }
    }

    fun alphaCandidateDisplay(definition: EquipmentDefinition): ItemStack {
        val item = displayItemForSet(definition.setId, definition)
        return ItemStack(item).apply {
            set(
                DataComponentTypes.CUSTOM_NAME,
                Text.translatable(
                    "screen.cresora.alpha.option",
                    Text.translatable(definition.setDefinition().translationKey()),
                    Text.translatable(definition.slotType().translationKey())
                )
            )
        }
    }

    fun betaStatDisplay(type: StatType, selectedOrder: Int): ItemStack {
        val item = when {
            selectedOrder > 0 -> Items.ENCHANTED_BOOK
            else -> statIcon(type)
        }
        return ItemStack(item).apply {
            val name = if (selectedOrder > 0) {
                Text.translatable("screen.cresora.beta.selected_option", selectedOrder, Text.translatable(type.translationKey()))
            } else {
                Text.translatable("screen.cresora.beta.option", Text.translatable(type.translationKey()))
            }
            set(DataComponentTypes.CUSTOM_NAME, name)
        }
    }

    fun resonanceBannerDisplay(
        definition: ResonanceBannerDefinition,
        pityCount: Int,
        arpeggioReady: Boolean,
        selectedFeaturedWeaponId: String? = null
    ): ItemStack {
        val displayItem = definition.currencyType().icon()
        return ItemStack(displayItem).apply {
            set(
                DataComponentTypes.CUSTOM_NAME,
                Text.translatable(
                    "screen.cresora.resonance.banner_entry",
                    Text.translatable(definition.translationKey),
                    definition.cost,
                    pityCount,
                    if (arpeggioReady && definition.type == ResonanceBannerType.LIMITED) {
                        Text.translatable("screen.cresora.resonance.arpeggio_ready_short")
                    } else {
                        Text.translatable("screen.cresora.resonance.arpeggio_idle_short")
                    }
                )
            )
            if (definition.type == ResonanceBannerType.LIMITED) {
                val lore = if (selectedFeaturedWeaponId != null) {
                    listOf(
                        Text.translatable(
                            "screen.cresora.resonance.banner_lore_featured",
                            Text.translatable("item.cresora-utilities.$selectedFeaturedWeaponId")
                        )
                    )
                } else {
                    listOf(Text.translatable("screen.cresora.resonance.banner_lore_featured_unset"))
                }
                set(DataComponentTypes.LORE, LoreComponent(lore))
            }
        }
    }

    fun resonanceFeaturedSelectDisplay(selectedFeaturedWeaponId: String?): ItemStack {
        return ItemStack(Items.NAME_TAG).apply {
            set(
                DataComponentTypes.CUSTOM_NAME,
                Text.translatable("screen.cresora.resonance.featured_select_entry")
            )
            val lore = if (selectedFeaturedWeaponId != null) {
                listOf(
                    Text.translatable(
                        "screen.cresora.resonance.featured_select_current",
                        Text.translatable("item.cresora-utilities.$selectedFeaturedWeaponId")
                    ),
                    Text.translatable("screen.cresora.resonance.featured_select_hint_change")
                )
            } else {
                listOf(
                    Text.translatable("screen.cresora.resonance.featured_select_none"),
                    Text.translatable("screen.cresora.resonance.featured_select_hint_set")
                )
            }
            set(DataComponentTypes.LORE, LoreComponent(lore))
        }
    }

    fun resonanceFeaturedOptionDisplay(weaponStack: ItemStack, selected: Boolean): ItemStack {
        val display = weaponStack.copy()
        if (selected) {
            display.set(
                DataComponentTypes.CUSTOM_NAME,
                Text.translatable(
                    "screen.cresora.resonance.featured_option_selected",
                    display.name
                )
            )
        }
        val lore = if (selected) {
            listOf(Text.translatable("screen.cresora.resonance.featured_option_lore_selected"))
        } else {
            listOf(Text.translatable("screen.cresora.resonance.featured_option_lore_unselected"))
        }
        display.set(DataComponentTypes.LORE, LoreComponent(lore))
        return display
    }

    fun resonanceResultDisplay(result: ResonanceService.PullResult): ItemStack {
        val stack = result.pulledWeapon.copy()
        stack.set(
            DataComponentTypes.CUSTOM_NAME,
            Text.translatable(
                "screen.cresora.resonance.result_entry",
                Text.translatable(result.banner.translationKey),
                Text.translatable(result.pulledWeapon.item.translationKey),
                Text.translatable(result.rarity.translationKey())
            )
        )
        return stack
    }

    fun resonanceReturnButtonDisplay(): ItemStack {
        return ItemStack(Items.ARROW).apply {
            set(DataComponentTypes.CUSTOM_NAME, Text.translatable("screen.cresora.resonance.return_button"))
        }
    }

    fun resonanceRepullButtonDisplay(definition: ResonanceBannerDefinition, canAfford: Boolean): ItemStack {
        val item = if (canAfford) Items.EMERALD else Items.BARRIER
        return ItemStack(item).apply {
            set(
                DataComponentTypes.CUSTOM_NAME,
                Text.translatable(
                    "screen.cresora.resonance.repull_button",
                    Text.translatable(definition.translationKey),
                    definition.cost
                )
            )
        }
    }

    fun weaponSkillMaterialDisplay(stack: ItemStack, selectedOrder: Int): ItemStack {
        val display = stack.copy()
        if (selectedOrder > 0) {
            display.set(
                DataComponentTypes.CUSTOM_NAME,
                Text.translatable("screen.cresora.weapon_skill_material.selected_entry", selectedOrder, display.name)
            )
        }
        return display
    }

    fun masqueradeLoadoutSelectedDisplay(stack: ItemStack, selectedOrder: Int): ItemStack {
        val display = stack.copy()
        display.set(
            DataComponentTypes.CUSTOM_NAME,
            Text.translatable("screen.cresora.masquerade.loadout.selected", selectedOrder, display.name)
        )
        return display
    }

    fun masqueradeLoadoutOptionDisplay(stack: ItemStack, selectedOrder: Int): ItemStack {
        val display = stack.copy()
        if (selectedOrder > 0) {
            display.set(
                DataComponentTypes.CUSTOM_NAME,
                Text.translatable("screen.cresora.masquerade.loadout.option_selected", selectedOrder, display.name)
            )
        }
        return display
    }

    fun masqueradeLoadoutStartButtonDisplay(canStart: Boolean): ItemStack {
        val item = if (canStart) Items.EMERALD else Items.BARRIER
        return ItemStack(item).apply {
            set(DataComponentTypes.CUSTOM_NAME, Text.translatable("screen.cresora.masquerade.loadout.start"))
        }
    }

    fun masqueradeSeasonDisplay(record: MasqueradeSeasonRecord): ItemStack {
        return ItemStack(Items.CLOCK).apply {
            set(
                DataComponentTypes.CUSTOM_NAME,
                Text.translatable("screen.cresora.masquerade.info.season", record.seasonId)
            )
        }
    }

    fun masqueradeCurrentRecordDisplay(record: MasqueradeSeasonRecord): ItemStack {
        return ItemStack(Items.DIAMOND_SWORD).apply {
            set(
                DataComponentTypes.CUSTOM_NAME,
                Text.translatable("screen.cresora.masquerade.info.current_best", record.bestWave)
            )
        }
    }

    fun masqueradePreviousRecordDisplay(record: MasqueradeSeasonRecord?): ItemStack {
        val item = if (record == null) Items.BOOK else Items.WRITTEN_BOOK
        return ItemStack(item).apply {
            set(
                DataComponentTypes.CUSTOM_NAME,
                if (record == null) {
                    Text.translatable("screen.cresora.masquerade.info.previous_none")
                } else {
                    Text.translatable("screen.cresora.masquerade.info.previous_best", record.seasonId, record.bestWave)
                }
            )
        }
    }

    fun masqueradeAttemptDisplay(record: MasqueradeSeasonRecord): ItemStack {
        return ItemStack(Items.PAPER).apply {
            set(
                DataComponentTypes.CUSTOM_NAME,
                Text.translatable("screen.cresora.masquerade.info.attempts", record.attemptCount, record.totalClearedWaves)
            )
        }
    }

    fun masqueradeSupportDisplay(definition: MasqueradeSupportBuffDefinition): ItemStack {
        val item = when (definition.rarity) {
            EquipmentRarity.THREE_STAR -> statIcon(definition.statType ?: StatType.ALL_DMG_BONUS)
            EquipmentRarity.FOUR_STAR -> Items.ENCHANTED_GOLDEN_APPLE
            EquipmentRarity.FIVE_STAR -> Items.NETHER_STAR
        }
        val rarityText = "★".repeat(
            when (definition.rarity) {
                EquipmentRarity.THREE_STAR -> 3
                EquipmentRarity.FOUR_STAR -> 4
                EquipmentRarity.FIVE_STAR -> 5
            }
        )
        return ItemStack(item).apply {
            set(
                DataComponentTypes.CUSTOM_NAME,
                Text.literal("$rarityText ").append(Text.translatable(definition.nameKey)).append(Text.literal(" | ")).append(Text.translatable(definition.descriptionKey))
            )
        }
    }

    fun masqueradeResultDisplayStacks(clearedWaveCount: Int, credits: Int, chordProgression: Int): List<ItemStack> {
        val displays = mutableListOf<ItemStack>()
        displays += ItemStack(Items.CLOCK).apply {
            set(DataComponentTypes.CUSTOM_NAME, Text.translatable("screen.cresora.masquerade.result.wave", clearedWaveCount))
        }
        if (credits > 0) {
            displays += ItemStack(Items.GOLD_INGOT).apply {
                set(DataComponentTypes.CUSTOM_NAME, Text.translatable("screen.cresora.masquerade.result.credits", ArtifactSpecialItem.formatWholeNumber(credits)))
            }
        }
        if (chordProgression > 0) {
            displays += ItemStack(ResonanceCurrencyType.CHORD_PROGRESSION.icon()).apply {
                set(
                    DataComponentTypes.CUSTOM_NAME,
                    Text.translatable("screen.cresora.masquerade.result.chord", ArtifactSpecialItem.formatWholeNumber(chordProgression))
                )
            }
        }
        return displays
    }

    private fun displayItemForSet(setId: String, fallbackDefinition: EquipmentDefinition): Item {
        val preferred = EquipmentContentRegistry.equipmentDefinitions().firstOrNull { candidate ->
            candidate.setId == setId && candidate.slotTypeId == "wand"
        }
        return EquipmentStackSupport.itemForDefinitionId(preferred?.id ?: fallbackDefinition.id) ?: Items.STICK
    }

    private fun statIcon(type: StatType): Item {
        return when (type) {
            StatType.ATK_FLAT -> Items.WOODEN_SWORD
            StatType.ATK_PERCENT -> Items.IRON_SWORD
            StatType.HP_FLAT -> Items.APPLE
            StatType.HP_PERCENT -> Items.GOLDEN_APPLE
            StatType.DEF_FLAT -> Items.LEATHER_CHESTPLATE
            StatType.DEF_PERCENT -> Items.IRON_CHESTPLATE
            StatType.CRIT_RATE -> Items.ARROW
            StatType.CRIT_DMG -> Items.CROSSBOW
            StatType.ALL_DMG_BONUS -> Items.NETHER_STAR
            StatType.PHYSICAL_RESISTANCE -> Items.SHIELD
            StatType.ARCANE_RESISTANCE -> Items.END_CRYSTAL
            StatType.DAMAGE_REDUCTION -> Items.TOTEM_OF_UNDYING
        }
    }
}
