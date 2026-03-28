package hifumi.cresora

import net.minecraft.item.Item
import net.minecraft.item.ItemStack

object WeaponStackSupport {
    private val weaponsByItem: MutableMap<Item, WeaponDefinitionRef> = linkedMapOf()
    private val fragmentsByItem: MutableMap<Item, WeaponDefinitionRef> = linkedMapOf()
    private val itemsByWeaponId: MutableMap<String, Item> = linkedMapOf()
    private val fragmentItemsByWeaponId: MutableMap<String, Item> = linkedMapOf()

    fun registerWeaponItem(item: Item, definition: WeaponDefinitionRef) {
        weaponsByItem[item] = definition
        itemsByWeaponId[definition.id] = item
    }

    fun registerFragmentItem(item: Item, definition: WeaponDefinitionRef) {
        fragmentsByItem[item] = definition
        fragmentItemsByWeaponId[definition.id] = item
    }

    fun isWeapon(stack: ItemStack): Boolean = weaponsByItem.containsKey(stack.item)

    fun isWeaponFragment(stack: ItemStack): Boolean = fragmentsByItem.containsKey(stack.item)

    fun getDefinition(stack: ItemStack): WeaponDefinition? = weaponsByItem[stack.item]?.resolve()

    fun getFragmentDefinition(stack: ItemStack): WeaponDefinition? = fragmentsByItem[stack.item]?.resolve()

    fun weaponItem(definitionId: String): Item? = itemsByWeaponId[definitionId]

    fun fragmentItem(definitionId: String): Item? = fragmentItemsByWeaponId[definitionId]

    fun getWeaponData(stack: ItemStack): WeaponData? {
        val definition = getDefinition(stack) ?: return null
        val data = stack.get(ModDataComponents.WEAPON_DATA)
            ?.copy(weaponId = definition.id)
            ?.normalized(definition)
        if (data != null) {
            return data
        }
        return syncWeaponData(stack, defaultWeaponData(definition))
    }

    fun ensureWeaponData(stack: ItemStack): WeaponData {
        val definition = getDefinition(stack) ?: error("Non-weapon stack cannot receive weapon data: ${stack.item}")
        return syncWeaponData(stack, getWeaponData(stack) ?: defaultWeaponData(definition))
    }

    fun syncWeaponData(stack: ItemStack, data: WeaponData): WeaponData {
        val definition = getDefinition(stack) ?: return data.normalized()
        val normalized = data.normalized(definition)
        stack.set(ModDataComponents.WEAPON_DATA, normalized)
        return normalized
    }

    fun defaultWeaponData(definition: WeaponDefinition): WeaponData {
        return WeaponData(
            weaponId = definition.id,
            rarity = definition.craft.craftedRarity,
            baseLevel = definition.craft.craftedBaseLevel,
            skillLevel = definition.craft.craftedSkillLevel
        ).normalized(definition)
    }

    fun createWeaponStack(
        definition: WeaponDefinition,
        rarity: WeaponRarity,
        baseLevel: Int,
        skillLevel: Int
    ): ItemStack {
        val item = weaponItem(definition.id) ?: error("Weapon item not registered: ${definition.id}")
        val stack = ItemStack(item)
        syncWeaponData(
            stack,
            WeaponData(
                weaponId = definition.id,
                rarity = rarity,
                baseLevel = baseLevel,
                skillLevel = skillLevel
            )
        )
        return stack
    }

    fun countFragments(player: net.minecraft.entity.player.PlayerEntity, definition: WeaponDefinition): Int {
        val fragmentItem = fragmentItem(definition.id) ?: return 0
        return (0 until player.inventory.size()).sumOf { slot ->
            val stack = player.inventory.getStack(slot)
            if (stack.item == fragmentItem) stack.count else 0
        }
    }

    fun removeFragments(player: net.minecraft.entity.player.PlayerEntity, definition: WeaponDefinition, amount: Int): Boolean {
        var remaining = amount.coerceAtLeast(0)
        if (remaining == 0) {
            return true
        }
        val fragmentItem = fragmentItem(definition.id) ?: return false
        if (countFragments(player, definition) < remaining) {
            return false
        }
        for (slot in 0 until player.inventory.size()) {
            val stack = player.inventory.getStack(slot)
            if (stack.item != fragmentItem || stack.isEmpty) {
                continue
            }
            val decrement = minOf(remaining, stack.count)
            stack.decrement(decrement)
            remaining -= decrement
            if (remaining == 0) {
                player.inventory.markDirty()
                return true
            }
        }
        player.inventory.markDirty()
        return remaining == 0
    }
}
