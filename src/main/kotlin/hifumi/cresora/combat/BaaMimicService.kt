package hifumi.cresora.combat
import hifumi.cresora.adventurerank.AdventureRankMobAccess
import hifumi.cresora.adventurerank.AdventureRankProgression
import hifumi.cresora.adventurerank.AdventureRankService
import hifumi.cresora.equipment.ArtifactSpecialUpgradeService
import hifumi.cresora.weapon.WeaponDropService
import net.minecraft.entity.EntityType
import net.minecraft.entity.SpawnReason
import net.minecraft.entity.attribute.EntityAttributes
import net.minecraft.entity.damage.DamageSource
import net.minecraft.entity.mob.HostileEntity
import net.minecraft.entity.passive.SheepEntity
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.loot.LootTable
import net.minecraft.loot.context.LootContextParameters
import net.minecraft.loot.context.LootContextTypes
import net.minecraft.loot.context.LootWorldContext
import net.minecraft.registry.Registries
import net.minecraft.server.network.ServerPlayerEntity
import net.minecraft.server.world.ServerWorld
import net.minecraft.util.Identifier

object BaaMimicService {
    private const val ORIGINAL_LOOT_PREFIX = "cresora_original_loot:"
    private const val ORIGINAL_RANK_PREFIX = "cresora_original_rank:"

    @JvmStatic
    fun isMimicSheep(sheep: SheepEntity): Boolean {
        return originalTypeId(sheep) != null
    }

    @JvmStatic
    fun markTransformedSheep(sheep: SheepEntity, original: HostileEntity) {
        clearTransformTags(sheep)
        val typeId = EntityType.getId(original.type).toString()
        val rank = AdventureRankService.mobLevel(original)
        sheep.addCommandTag("$ORIGINAL_LOOT_PREFIX$typeId")
        sheep.addCommandTag("$ORIGINAL_RANK_PREFIX$rank")
        val maxHealthAttr = sheep.getAttributeInstance(EntityAttributes.MAX_HEALTH)
        if (maxHealthAttr != null) {
            maxHealthAttr.baseValue = currentNearbyMaxPlayerHealth(sheep)
            sheep.health = sheep.maxHealth
        }
    }

    @JvmStatic
    fun handleMimicSheepDeath(sheep: SheepEntity, source: DamageSource): Boolean {
        val world = sheep.world as? ServerWorld ?: return false
        val typeId = originalTypeId(sheep) ?: return false
        val entityType = Registries.ENTITY_TYPE.get(Identifier.of(typeId))
        val hostileType = entityType as? EntityType<out HostileEntity> ?: return false
        val dummy = hostileType.create(world, SpawnReason.COMMAND) ?: return false
        dummy.refreshPositionAndAngles(sheep.x, sheep.y, sheep.z, sheep.yaw, sheep.pitch)
        val access = dummy as? AdventureRankMobAccess
        if (access != null) {
            access.cresoraSetMobAdventureRank(originalRank(sheep))
            access.cresoraSetEliteMob(false)
            access.cresoraSetMobPackId("")
        }
        dropOriginalLoot(world, sheep, dummy, source)
        val player = source.attacker as? ServerPlayerEntity
        if (player != null) {
            WeaponDropService.emulateHostileKilled(player, dummy)
            ArtifactSpecialUpgradeService.tryDropSpecialItems(player, AdventureRankService.mobLevel(dummy))
        }
        return true
    }

    private fun dropOriginalLoot(world: ServerWorld, sheep: SheepEntity, dummy: HostileEntity, source: DamageSource) {
        val lootTableKey = dummy.type.lootTableKey.orElse(null) ?: return
        val lootTable: LootTable = world.server.reloadableRegistries.getLootTable(lootTableKey)
        val builder = LootWorldContext.Builder(world)
            .add(LootContextParameters.THIS_ENTITY, dummy)
            .add(LootContextParameters.ORIGIN, sheep.pos)
            .add(LootContextParameters.DAMAGE_SOURCE, source)
        val attacker = source.attacker
        if (attacker != null) {
            builder.addOptional(LootContextParameters.ATTACKING_ENTITY, attacker)
        }
        if (attacker is PlayerEntity) {
            builder.addOptional(LootContextParameters.LAST_DAMAGE_PLAYER, attacker)
            builder.luck(attacker.luck)
        }
        val params = builder.build(LootContextTypes.ENTITY)
        lootTable.generateLoot(params) { stack ->
            sheep.dropStack(world, stack)
        }
    }

    private fun currentNearbyMaxPlayerHealth(sheep: SheepEntity): Double {
        val world = sheep.world as? ServerWorld ?: return 20.0
        return world.players
            .filter { !it.isSpectator && it.squaredDistanceTo(sheep) <= 100.0 }
            .maxOfOrNull { it.maxHealth.toDouble() }
            ?: 20.0
    }

    private fun clearTransformTags(sheep: SheepEntity) {
        sheep.commandTags
            .filter { it.startsWith(ORIGINAL_LOOT_PREFIX) || it.startsWith(ORIGINAL_RANK_PREFIX) }
            .toList()
            .forEach(sheep::removeCommandTag)
    }

    fun originalTypeId(sheep: SheepEntity): String? {
        return sheep.commandTags.firstOrNull { it.startsWith(ORIGINAL_LOOT_PREFIX) }
            ?.removePrefix(ORIGINAL_LOOT_PREFIX)
            ?.takeIf(String::isNotBlank)
    }

    fun originalRank(sheep: SheepEntity): Int {
        return sheep.commandTags.firstOrNull { it.startsWith(ORIGINAL_RANK_PREFIX) }
            ?.removePrefix(ORIGINAL_RANK_PREFIX)
            ?.toIntOrNull()
            ?.let(AdventureRankProgression::sanitizeRank)
            ?: AdventureRankProgression.MIN_RANK
    }
}
