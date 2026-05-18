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
import net.minecraft.screen.slot.SlotActionType
import net.minecraft.server.network.ServerPlayerEntity
import net.minecraft.util.Identifier

class WeaponSkillMaterialScreenHandler(
    syncId: Int,
    private val playerInventory: PlayerInventory
) : ScreenHandler(CreSoraUtilities.WEAPON_SKILL_MATERIAL_SCREEN_HANDLER, syncId) {

    companion object {
        private const val ROWS = 4
        private const val OPTION_SLOT_COUNT = ROWS * 9
        private const val PROPERTY_SELECTED_COUNT = 0
        private const val PROPERTY_REQUIRED_COUNT = 1
    }

    private val optionInventory: Inventory = object : SimpleInventory(OPTION_SLOT_COUNT) {}
    private val properties: PropertyDelegate = ArrayPropertyDelegate(2)
    private val eligibleArtifacts: MutableList<WeaponSkillArtifactSupport.EligibleArtifact> = mutableListOf()
    private val selectedInventorySlots: MutableList<Int> = mutableListOf()
    private var weaponStack: ItemStack = ItemStack.EMPTY
    private var completed = false

    constructor(
        syncId: Int,
        playerInventory: PlayerInventory,
        weaponStack: ItemStack
    ) : this(syncId, playerInventory) {
        this.weaponStack = weaponStack
        refreshEligibleArtifacts()
        refreshOptions()
    }

    init {
        addProperties(properties)
        for (index in 0 until OPTION_SLOT_COUNT) {
            val row = index / 9
            val column = index % 9
            addSlot(object : Slot(optionInventory, index, 8 + column * 18, 18 + row * 18) {
                override fun canInsert(stack: ItemStack): Boolean = false

                override fun canTakeItems(playerEntity: PlayerEntity): Boolean = false

                override fun getBackgroundSprite(): Identifier? = null
            })
        }
        addPlayerSlots(playerInventory, ROWS)
        refreshOptions()
    }

    override fun canUse(player: PlayerEntity): Boolean = true

    override fun quickMove(player: PlayerEntity, slotIndex: Int): ItemStack = ItemStack.EMPTY

    override fun onSlotClick(slotIndex: Int, button: Int, actionType: SlotActionType, player: PlayerEntity) {
        if (slotIndex in 0 until OPTION_SLOT_COUNT) {
            if (actionType == SlotActionType.PICKUP || actionType == SlotActionType.QUICK_MOVE) {
                toggleSelection(slotIndex, player)
            }
            return
        }
    }

    override fun onClosed(player: PlayerEntity) {
        super.onClosed(player)
        if (player.world.isClient || completed || weaponStack.isEmpty) {
            return
        }

        playerInventory.offerOrDrop(weaponStack)
        weaponStack = ItemStack.EMPTY
    }

    fun selectedCount(): Int = properties.get(PROPERTY_SELECTED_COUNT)

    fun requiredCount(): Int = properties.get(PROPERTY_REQUIRED_COUNT)

    private fun toggleSelection(slotIndex: Int, player: PlayerEntity) {
        val eligible = eligibleArtifacts.getOrNull(slotIndex) ?: return
        if (selectedInventorySlots.contains(eligible.inventorySlot)) {
            selectedInventorySlots.remove(eligible.inventorySlot)
        } else if (selectedInventorySlots.size < requiredCount()) {
            selectedInventorySlots += eligible.inventorySlot
        }
        refreshOptions()
        sendContentUpdates()
        if (selectedInventorySlots.size >= requiredCount() && requiredCount() > 0) {
            executeUpgrade(player)
        }
    }

    private fun executeUpgrade(player: PlayerEntity) {
        val serverPlayer = player as? ServerPlayerEntity ?: return
        val result = WeaponUpgradeLogic.attemptSkillUpgradeWithArtifacts(serverPlayer, weaponStack, selectedInventorySlots.toList())
        player.sendMessage(result.message, false)
        if (result.success) {
            completed = true
        }
        selectedInventorySlots.clear()
        if (result.success) {
            ArtifactUiFlow.openWeaponUpgrade(serverPlayer, weaponStack)
            weaponStack = ItemStack.EMPTY
        } else {
            refreshEligibleArtifacts()
            refreshOptions()
            sendContentUpdates()
        }
    }

    private fun refreshEligibleArtifacts() {
        eligibleArtifacts.clear()
        val definition = WeaponStackSupport.getDefinition(weaponStack) ?: return
        eligibleArtifacts += WeaponSkillArtifactSupport.eligibleArtifacts(playerInventory.player, definition).take(OPTION_SLOT_COUNT)
    }

    private fun refreshOptions() {
        val definition = WeaponStackSupport.getDefinition(weaponStack)
        val weaponData = WeaponStackSupport.getWeaponData(weaponStack)
        val requiredCount = if (definition != null && weaponData != null) {
            WeaponSkillArtifactSupport.requiredArtifactCount(definition, weaponData)
        } else {
            0
        }
        properties.set(PROPERTY_SELECTED_COUNT, selectedInventorySlots.size)
        properties.set(PROPERTY_REQUIRED_COUNT, requiredCount)
        for (index in 0 until OPTION_SLOT_COUNT) {
            val artifact = eligibleArtifacts.getOrNull(index)
            val selectedOrder = artifact?.let { selectedInventorySlots.indexOf(it.inventorySlot) + 1 } ?: 0
            optionInventory.setStack(
                index,
                artifact?.let { ArtifactDisplayStackFactory.weaponSkillMaterialDisplay(it.stack, selectedOrder) } ?: ArtifactDisplayStackFactory.fillerDisplay()
            )
        }
    }

    private fun addPlayerSlots(playerInventory: PlayerInventory, rows: Int) {
        val inventoryStartY = 18 + rows * 18 + 14
        val hotbarY = inventoryStartY + 58
        for (row in 0 until 3) {
            for (column in 0 until 9) {
                addSlot(Slot(playerInventory, column + row * 9 + 9, 8 + column * 18, inventoryStartY + row * 18))
            }
        }

        for (column in 0 until 9) {
            addSlot(Slot(playerInventory, column, 8 + column * 18, hotbarY))
        }
    }
}
