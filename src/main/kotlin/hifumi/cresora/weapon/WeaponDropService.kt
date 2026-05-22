package hifumi.cresora.weapon
import hifumi.cresora.adventurerank.AdventureRankService
import hifumi.cresora.combat.HostileRewardFamilies
import hifumi.cresora.combat.HostileRewardFamily
import net.minecraft.entity.ItemEntity
import net.minecraft.entity.EntityType
import net.minecraft.entity.mob.HostileEntity
import net.minecraft.server.network.ServerPlayerEntity
import net.minecraft.server.world.ServerWorld

object WeaponDropService {
    fun onHostileKilled(player: ServerPlayerEntity, hostile: HostileEntity) {
        emulateHostileKilled(player, hostile)
    }

    fun emulateHostileKilled(player: ServerPlayerEntity, hostile: HostileEntity) {
        val world = hostile.world as? ServerWorld ?: return
        emulateDrops(world, hostile.type, AdventureRankService.mobLevel(hostile), hostile.x, hostile.y, hostile.z)
    }

    fun emulateDrops(
        world: ServerWorld,
        entityType: EntityType<*>,
        mobLevel: Int,
        x: Double,
        y: Double,
        z: Double
    ) {
        val family = HostileRewardFamilies.classify(entityType)
        for (definition in WeaponContentRegistry.weaponDefinitions()) {
            maybeDropFragments(world, definition, family, mobLevel, x, y, z)
            maybeDropWeapon(world, definition, family, mobLevel, x, y, z)
        }
    }

    private fun maybeDropFragments(
        world: ServerWorld,
        definition: WeaponDefinition,
        family: HostileRewardFamily,
        mobLevel: Int,
        x: Double,
        y: Double,
        z: Double
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
        spawnDrop(world, x, y, z, net.minecraft.item.ItemStack(fragmentItem, count))
    }

    private fun maybeDropWeapon(
        world: ServerWorld,
        definition: WeaponDefinition,
        family: HostileRewardFamily,
        mobLevel: Int,
        x: Double,
        y: Double,
        z: Double
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
            spawnDrop(world, x, y, z, stack)
            return
        }
    }

    private fun spawnDrop(
        world: ServerWorld,
        x: Double,
        y: Double,
        z: Double,
        stack: net.minecraft.item.ItemStack
    ) {
        val entity = ItemEntity(world, x, y + 0.5, z, stack)
        world.spawnEntity(entity)
    }
}
