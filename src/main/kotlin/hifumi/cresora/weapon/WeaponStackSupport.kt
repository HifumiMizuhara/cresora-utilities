package hifumi.cresora.weapon

import hifumi.cresora.CreSoraUtilities
import hifumi.cresora.ModDataComponents
import net.minecraft.item.Item
import net.minecraft.item.ItemStack
import net.minecraft.component.DataComponentTypes
import net.minecraft.component.type.CustomModelDataComponent

object WeaponStackSupport {
    private val weaponsByItem: MutableMap<Item, WeaponDefinitionRef> = linkedMapOf()
    private val legacyFragmentsByItem: MutableMap<Item, WeaponDefinitionRef> = linkedMapOf()
    private val itemsByWeaponId: MutableMap<String, Item> = linkedMapOf()
    private val legacyFragmentItemsByWeaponId: MutableMap<String, Item> = linkedMapOf()
    private val genericFragmentItemsByRarity: MutableMap<WeaponRarity, Item> = linkedMapOf()
    private val legacyFragmentItemsByRarity: MutableMap<WeaponRarity, MutableSet<Item>> = linkedMapOf()

    fun registerWeaponItem(item: Item, definition: WeaponDefinitionRef) {
        weaponsByItem[item] = definition
        itemsByWeaponId[definition.id] = item
    }

    fun registerFragmentItem(item: Item, definition: WeaponDefinitionRef) {
        val resolved = definition.resolve()
        legacyFragmentsByItem[item] = definition
        legacyFragmentItemsByWeaponId[definition.id] = item
        legacyFragmentItemsByRarity.getOrPut(resolved.craft.craftedRarity) { linkedSetOf() }.add(item)
    }

    fun registerRarityFragmentItem(item: Item, rarity: WeaponRarity) {
        genericFragmentItemsByRarity[rarity] = item
    }

    fun isWeapon(stack: ItemStack): Boolean = weaponsByItem.containsKey(stack.item)

    fun isWeaponFragment(stack: ItemStack): Boolean =
        legacyFragmentsByItem.containsKey(stack.item) || genericFragmentItemsByRarity.containsValue(stack.item)

    fun getDefinition(stack: ItemStack): WeaponDefinition? = weaponsByItem[stack.item]?.resolve()

    fun getFragmentDefinition(stack: ItemStack): WeaponDefinition? = legacyFragmentsByItem[stack.item]?.resolve()

    fun weaponItem(definitionId: String): Item? = itemsByWeaponId[definitionId]

    fun fragmentItem(definitionId: String): Item? {
        val definition = runCatching { WeaponContentRegistry.requireWeapon(definitionId) }.getOrNull() ?: return legacyFragmentItemsByWeaponId[definitionId]
        return genericFragmentItemsByRarity[definition.craft.craftedRarity] ?: legacyFragmentItemsByWeaponId[definitionId]
    }

    fun legacyFragmentItem(definitionId: String): Item? = legacyFragmentItemsByWeaponId[definitionId]

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

        definition.customModelData?.let {
            stack.set(DataComponentTypes.CUSTOM_MODEL_DATA, CustomModelDataComponent(listOf(it.toFloat()), emptyList(), emptyList(), emptyList()))
        }

        return normalized
    }

    fun defaultWeaponData(definition: WeaponDefinition): WeaponData {
        return WeaponData(
            weaponId = definition.id,
            rarity = definition.craft.craftedRarity,
            baseLevel = definition.craft.craftedBaseLevel,
            skillLevel = definition.craft.craftedSkillLevel,
            breakthrough = 0
        ).normalized(definition)
    }

    fun createWeaponStack(
        definition: WeaponDefinition,
        rarity: WeaponRarity,
        baseLevel: Int,
        skillLevel: Int,
        breakthrough: Int = 0
    ): ItemStack {
        val item = weaponItem(definition.id) ?: error("Weapon item not registered: ${definition.id}")
        val stack = ItemStack(item)
        syncWeaponData(
            stack,
            WeaponData(
                weaponId = definition.id,
                rarity = rarity,
                baseLevel = baseLevel,
                skillLevel = skillLevel,
                breakthrough = breakthrough
            )
        )
        return stack
    }

    fun countFragments(player: net.minecraft.entity.player.PlayerEntity, definition: WeaponDefinition): Int {
        val acceptedItems = acceptedFragmentItems(definition)
        if (acceptedItems.isEmpty()) return 0
        return (0 until player.inventory.size()).sumOf { slot ->
            val stack = player.inventory.getStack(slot)
            if (acceptedItems.contains(stack.item)) stack.count else 0
        }
    }

    fun removeFragments(player: net.minecraft.entity.player.PlayerEntity, definition: WeaponDefinition, amount: Int): Boolean {
        var remaining = amount.coerceAtLeast(0)
        if (remaining == 0) {
            return true
        }
        if (countFragments(player, definition) < remaining) {
            return false
        }
        val acceptedItems = acceptedFragmentItems(definition)
        for (slot in 0 until player.inventory.size()) {
            val stack = player.inventory.getStack(slot)
            if (!acceptedItems.contains(stack.item) || stack.isEmpty) {
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

    fun countRoleMaterials(player: net.minecraft.entity.player.PlayerEntity, role: WeaponRole, breakthroughLevel: Int): Int {
        val item = if (breakthroughLevel == 1) CreSoraUtilities.getRoleProofItem(role) else CreSoraUtilities.getRoleInsightItem(role)
        return (0 until player.inventory.size()).sumOf { slot ->
            val stack = player.inventory.getStack(slot)
            if (stack.item == item) stack.count else 0
        }
    }

    fun removeRoleMaterials(player: net.minecraft.entity.player.PlayerEntity, role: WeaponRole, breakthroughLevel: Int, amount: Int): Boolean {
        var remaining = amount.coerceAtLeast(0)
        if (remaining == 0) return true
        val item = if (breakthroughLevel == 1) CreSoraUtilities.getRoleProofItem(role) else CreSoraUtilities.getRoleInsightItem(role)
        if (countRoleMaterials(player, role, breakthroughLevel) < remaining) return false
        for (slot in 0 until player.inventory.size()) {
            val stack = player.inventory.getStack(slot)
            if (stack.item == item && !stack.isEmpty) {
                val decrement = minOf(remaining, stack.count)
                stack.decrement(decrement)
                remaining -= decrement
                if (remaining == 0) {
                    player.inventory.markDirty()
                    return true
                }
            }
        }
        player.inventory.markDirty()
        return remaining == 0
    }

    private fun acceptedFragmentItems(definition: WeaponDefinition): Set<Item> {
        val accepted = linkedSetOf<Item>()
        genericFragmentItemsByRarity[definition.craft.craftedRarity]?.let(accepted::add)
        legacyFragmentItemsByRarity[definition.craft.craftedRarity]?.let(accepted::addAll)
        return accepted
    }

    fun getEquippedArtifacts(stack: ItemStack): List<ItemStack> {
        val component = stack.get(ModDataComponents.WEAPON_ARTIFACTS)
        if (component != null && component.size == 4) return component
        return List(4) { ItemStack.EMPTY }
    }

    fun setEquippedArtifacts(stack: ItemStack, artifacts: List<ItemStack>) {
        val normalized = if (artifacts.size == 4) artifacts else {
            val list = artifacts.toMutableList()
            while (list.size < 4) list.add(ItemStack.EMPTY)
            list.take(4)
        }
        stack.set(ModDataComponents.WEAPON_ARTIFACTS, normalized)
    }

    fun hasAnyEquippedArtifact(stack: ItemStack): Boolean {
        return getEquippedArtifacts(stack).any { !it.isEmpty }
    }
}
