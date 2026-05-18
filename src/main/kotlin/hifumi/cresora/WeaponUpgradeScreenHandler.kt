package hifumi.cresora

import net.minecraft.entity.player.PlayerEntity
import net.minecraft.entity.player.PlayerInventory
import net.minecraft.inventory.Inventory
import net.minecraft.inventory.SimpleInventory
import net.minecraft.item.ItemStack
import net.minecraft.screen.ArrayPropertyDelegate
import net.minecraft.screen.PropertyDelegate
import net.minecraft.screen.ScreenHandler
import net.minecraft.screen.slot.Slot
import net.minecraft.server.network.ServerPlayerEntity
import net.minecraft.text.Text
import net.minecraft.util.Identifier

class WeaponUpgradeScreenHandler(
    syncId: Int,
    private val playerInventory: PlayerInventory,
    private val initialWeapon: ItemStack = ItemStack.EMPTY
) : ScreenHandler(CreSoraUtilities.WEAPON_UPGRADE_SCREEN_HANDLER, syncId) {
    companion object {
        const val BUTTON_BASE_UPGRADE = 0
        const val BUTTON_SKILL_UPGRADE = 1
        const val BUTTON_DISMANTLE = 2
        private const val REQUIRED_DISMANTLE_CLICKS = 3
        private const val WEAPON_SLOT = 0
        private const val CUSTOM_SLOT_COUNT = 1
        private const val PROPERTY_CREDITS_LOW = 0
        private const val PROPERTY_FRAGMENTS = 2
        private const val PROPERTY_ARTIFACTS = 3
        private const val PROPERTY_DISMANTLE_CONFIRM = 4
    }

    private val weaponInventory: Inventory = object : SimpleInventory(CUSTOM_SLOT_COUNT) {
        override fun markDirty() {
            super.markDirty()
            onContentChanged(this)
        }
    }
    private val properties: PropertyDelegate = ArrayPropertyDelegate(5)
    private var dismantleConfirmClicks: Int = 0

    init {
        addSlot(object : Slot(weaponInventory, WEAPON_SLOT, 44, 32) {
            override fun canInsert(stack: ItemStack): Boolean = WeaponStackSupport.isWeapon(stack)
            override fun getMaxItemCount(): Int = 1
            override fun getBackgroundSprite(): Identifier? = null
        })
        addProperties(properties)
        addPlayerSlots(playerInventory)
        if (!initialWeapon.isEmpty) {
            slots[WEAPON_SLOT].stack = initialWeapon
        }
        if (!playerInventory.player.world.isClient) {
            if (initialWeapon.isEmpty) {
                tryMoveSelectedWeapon()
            }
        }
        refreshProperties()
    }

    override fun canUse(player: PlayerEntity): Boolean = true

    override fun sendContentUpdates() {
        refreshProperties()
        properties.set(PROPERTY_DISMANTLE_CONFIRM, dismantleConfirmClicks)
        super.sendContentUpdates()
    }

    override fun onContentChanged(inventory: Inventory) {
        super.onContentChanged(inventory)
        resetDismantleConfirmation()
    }

    override fun quickMove(player: PlayerEntity, slotIndex: Int): ItemStack {
        val slot = slots.getOrNull(slotIndex) ?: return ItemStack.EMPTY
        if (!slot.hasStack()) {
            return ItemStack.EMPTY
        }
        val original = slot.stack
        val moved = original.copy()
        if (slotIndex < CUSTOM_SLOT_COUNT) {
            if (!insertItem(original, CUSTOM_SLOT_COUNT, slots.size, true)) {
                return ItemStack.EMPTY
            }
        } else {
            if (!WeaponStackSupport.isWeapon(original) || slots[WEAPON_SLOT].hasStack()) {
                return ItemStack.EMPTY
            }
            if (!insertItem(original, WEAPON_SLOT, WEAPON_SLOT + 1, false)) {
                return ItemStack.EMPTY
            }
        }
        if (original.isEmpty) {
            slot.stack = ItemStack.EMPTY
        } else {
            slot.markDirty()
        }
        return moved
    }

    override fun onButtonClick(player: PlayerEntity, id: Int): Boolean {
        val stack = slots[WEAPON_SLOT].stack
        val result = when (id) {
            BUTTON_BASE_UPGRADE -> {
                resetDismantleConfirmation()
                WeaponUpgradeLogic.attemptUpgrade(player, stack, WeaponUpgradeLogic.UpgradeType.BASE)
            }
            BUTTON_SKILL_UPGRADE -> {
                resetDismantleConfirmation()
                attemptSkillAction(player, stack)
            }
            BUTTON_DISMANTLE -> {
                val preview = getDismantlePreview()
                if (!preview.canDismantle) {
                    WeaponUpgradeLogic.AttemptResult(false, Text.translatable(preview.messageKey ?: "screen.cresora.weapon_upgrade.cannot_dismantle"))
                } else {
                    val remainingClicks = requireDismantleConfirmation()
                    if (remainingClicks > 0) {
                        player.sendMessage(Text.translatable("screen.cresora.weapon_upgrade.confirm_dismantle", remainingClicks), false)
                        sendContentUpdates()
                        return true
                    }
                    WeaponUpgradeLogic.attemptUpgrade(player, stack, WeaponUpgradeLogic.UpgradeType.DISMANTLE)
                }
            }
            else -> return super.onButtonClick(player, id)
        }
        player.sendMessage(result.message, false)
        slots[WEAPON_SLOT].markDirty()
        sendContentUpdates()
        return true
    }

    override fun onClosed(player: PlayerEntity) {
        super.onClosed(player)
        if (player.world.isClient) {
            return
        }
        for (slotIndex in 0 until CUSTOM_SLOT_COUNT) {
            val stack = weaponInventory.removeStack(slotIndex)
            if (!stack.isEmpty) {
                playerInventory.offerOrDrop(stack)
            }
        }
    }

    fun getWeaponStack(): ItemStack = slots[WEAPON_SLOT].stack

    fun getBasePreview(player: PlayerEntity): WeaponUpgradeLogic.BasePreview {
        return WeaponUpgradeLogic.getBasePreview(getWeaponStack(), availableCredits(player), availableFragments(player))
    }

    fun getSkillPreview(player: PlayerEntity): WeaponUpgradeLogic.SkillPreview {
        return WeaponUpgradeLogic.getSkillPreview(getWeaponStack(), availableCredits(player), availableArtifacts(player))
    }

    fun getDismantlePreview(): WeaponUpgradeLogic.DismantlePreview {
        return WeaponUpgradeLogic.getDismantlePreview(getWeaponStack())
    }

    fun getScreenTitle(): Text = Text.translatable("screen.cresora.weapon_upgrade")

    fun currentCredits(): Int = ScreenSyncSupport.readInt(properties, PROPERTY_CREDITS_LOW)

    fun currentFragments(): Int = properties.get(PROPERTY_FRAGMENTS)

    fun currentArtifacts(): Int = properties.get(PROPERTY_ARTIFACTS)

    fun dismantleConfirmRemaining(): Int {
        val clicks = properties.get(PROPERTY_DISMANTLE_CONFIRM)
        return if (clicks <= 0) 0 else (REQUIRED_DISMANTLE_CLICKS - clicks).coerceAtLeast(0)
    }

    private fun refreshProperties() {
        val player = playerInventory.player as? ServerPlayerEntity ?: return
        ScreenSyncSupport.writeInt(properties, PROPERTY_CREDITS_LOW, CreditsService.getCredits(player))
        val definition = WeaponStackSupport.getDefinition(getWeaponStack())
        properties.set(PROPERTY_FRAGMENTS, definition?.let { WeaponStackSupport.countFragments(player, it) } ?: 0)
        properties.set(PROPERTY_ARTIFACTS, definition?.let { WeaponSkillArtifactSupport.eligibleArtifacts(player, it).size } ?: 0)
    }

    private fun availableCredits(player: PlayerEntity): Int? {
        return if (player is ServerPlayerEntity) CreditsService.getCredits(player) else ScreenSyncSupport.readInt(properties, PROPERTY_CREDITS_LOW)
    }

    private fun availableFragments(player: PlayerEntity): Int? {
        return if (player is ServerPlayerEntity) {
            WeaponStackSupport.getDefinition(getWeaponStack())?.let { WeaponStackSupport.countFragments(player, it) } ?: 0
        } else {
            properties.get(PROPERTY_FRAGMENTS)
        }
    }

    private fun availableArtifacts(player: PlayerEntity): Int? {
        return if (player is ServerPlayerEntity) {
            WeaponStackSupport.getDefinition(getWeaponStack())?.let { WeaponSkillArtifactSupport.eligibleArtifacts(player, it).size } ?: 0
        } else {
            properties.get(PROPERTY_ARTIFACTS)
        }
    }

    private fun requireDismantleConfirmation(): Int {
        dismantleConfirmClicks += 1
        return if (dismantleConfirmClicks >= REQUIRED_DISMANTLE_CLICKS) {
            dismantleConfirmClicks = 0
            0
        } else {
            REQUIRED_DISMANTLE_CLICKS - dismantleConfirmClicks
        }
    }

    private fun resetDismantleConfirmation() {
        dismantleConfirmClicks = 0
        properties.set(PROPERTY_DISMANTLE_CONFIRM, 0)
    }

    private fun attemptSkillAction(player: PlayerEntity, stack: ItemStack): WeaponUpgradeLogic.AttemptResult {
        val serverPlayer = player as? ServerPlayerEntity
            ?: return WeaponUpgradeLogic.AttemptResult(false, Text.translatable("screen.cresora.weapon_upgrade.need_weapon"))
        val definition = WeaponStackSupport.getDefinition(stack)
            ?: return WeaponUpgradeLogic.AttemptResult(false, Text.translatable("screen.cresora.weapon_upgrade.need_weapon"))
        val preview = WeaponUpgradeLogic.getSkillPreview(
            stack,
            CreditsService.getCredits(serverPlayer),
            WeaponSkillArtifactSupport.eligibleArtifacts(serverPlayer, definition).size
        )
        if (!preview.canUpgrade) {
            return WeaponUpgradeLogic.AttemptResult(false, Text.translatable(preview.messageKey ?: "screen.cresora.weapon_upgrade.need_weapon"))
        }
        if (preview.artifactCost > 0) {
            val carriedWeapon = takeWeaponStack()
            if (carriedWeapon.isEmpty) {
                return WeaponUpgradeLogic.AttemptResult(false, Text.translatable("screen.cresora.weapon_upgrade.need_weapon"))
            }
            ArtifactUiFlow.openWeaponSkillMaterialSelection(serverPlayer, carriedWeapon)
            return WeaponUpgradeLogic.AttemptResult(true, Text.translatable("screen.cresora.weapon_upgrade.open_material_selection"))
        }
        return WeaponUpgradeLogic.attemptUpgrade(player, stack, WeaponUpgradeLogic.UpgradeType.SKILL)
    }

    private fun takeWeaponStack(): ItemStack {
        return weaponInventory.removeStack(WEAPON_SLOT)
    }

    private fun tryMoveSelectedWeapon() {
        val selectedSlot = playerInventory.selectedSlot
        val selectedStack = playerInventory.getStack(selectedSlot)
        if (!WeaponStackSupport.isWeapon(selectedStack) || slots[WEAPON_SLOT].hasStack()) {
            return
        }
        val extracted = playerInventory.removeStack(selectedSlot, 1)
        if (!extracted.isEmpty) {
            slots[WEAPON_SLOT].stack = extracted
        }
    }

    private fun addPlayerSlots(playerInventory: PlayerInventory) {
        for (row in 0 until 3) {
            for (column in 0 until 9) {
                addSlot(Slot(playerInventory, column + row * 9 + 9, 8 + column * 18, 84 + row * 18))
            }
        }
        for (column in 0 until 9) {
            addSlot(Slot(playerInventory, column, 8 + column * 18, 142))
        }
    }
}
