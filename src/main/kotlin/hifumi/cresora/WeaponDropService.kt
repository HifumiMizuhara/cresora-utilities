package hifumi.cresora

import net.minecraft.entity.ItemEntity
import net.minecraft.entity.mob.HostileEntity
import net.minecraft.server.network.ServerPlayerEntity
import net.minecraft.server.world.ServerWorld

object WeaponDropService {
    fun onHostileKilled(player: ServerPlayerEntity, hostile: HostileEntity) {
        val world = hostile.world as? ServerWorld ?: return
        val mobLevel = AdventureRankService.mobLevel(hostile)
        val family = HostileRewardFamilies.classify(hostile.type)
        for (definition in WeaponContentRegistry.weaponDefinitions()) {
            maybeDropFragments(world, hostile, definition, family, mobLevel)
            maybeDropWeapon(world, hostile, definition, family, mobLevel)
        }
    }

    private fun maybeDropFragments(
        world: net.minecraft.server.world.ServerWorld,
        hostile: HostileEntity,
        definition: WeaponDefinition,
        family: HostileRewardFamily,
        mobLevel: Int
    ) {
        val fragmentDrop = definition.drops.fragmentDrop
        if (mobLevel < fragmentDrop.minMobLevel) {
            return
        }
        val familyMultiplier = when (family) {
            HostileRewardFamily.SURVIVOR -> 1.0
            HostileRewardFamily.ASSAULT -> 1.05
            HostileRewardFamily.ARCANE -> 1.08
            HostileRewardFamily.ELITE -> 1.15
            HostileRewardFamily.RELIC -> 1.25
        }
        if (world.random.nextDouble() >= fragmentDrop.chance * familyMultiplier) {
            return
        }
        val count = if (fragmentDrop.maxCount <= fragmentDrop.minCount) {
            fragmentDrop.minCount
        } else {
            world.random.nextBetween(fragmentDrop.minCount, fragmentDrop.maxCount)
        } + if (mobLevel >= 70) fragmentDrop.bonusCountAtLevel70 else 0
        val fragmentItem = WeaponStackSupport.fragmentItem(definition.id) ?: return
        spawnDrop(world, hostile, net.minecraft.item.ItemStack(fragmentItem, count))
    }

    private fun maybeDropWeapon(
        world: net.minecraft.server.world.ServerWorld,
        hostile: HostileEntity,
        definition: WeaponDefinition,
        family: HostileRewardFamily,
        mobLevel: Int
    ) {
        val familyMultiplier = when (family) {
            HostileRewardFamily.SURVIVOR -> 0.85
            HostileRewardFamily.ASSAULT -> 1.0
            HostileRewardFamily.ARCANE -> 1.08
            HostileRewardFamily.ELITE -> 1.18
            HostileRewardFamily.RELIC -> 1.35
        }
        for (tier in definition.drops.directDropTiers.sortedByDescending { it.minMobLevel }) {
            if (mobLevel < tier.minMobLevel) {
                continue
            }
            var chance = tier.chance * familyMultiplier
            if (mobLevel >= 70 && tier.rarity == WeaponRarity.FIVE_STAR) {
                chance *= definition.drops.fiveStarChanceMultiplierAtLevel70
            }
            if (world.random.nextDouble() >= chance) {
                continue
            }
            val baseLevel = (mobLevel * definition.drops.directDropBaseLevelMultiplier).toInt()
                .coerceIn(1, definition.maxBaseLevel)
            val stack = WeaponStackSupport.createWeaponStack(definition, tier.rarity, baseLevel, 1)
            spawnDrop(world, hostile, stack)
            return
        }
    }

    private fun spawnDrop(
        world: net.minecraft.server.world.ServerWorld,
        hostile: HostileEntity,
        stack: net.minecraft.item.ItemStack
    ) {
        val entity = ItemEntity(world, hostile.x, hostile.y + 0.5, hostile.z, stack)
        world.spawnEntity(entity)
    }
}
