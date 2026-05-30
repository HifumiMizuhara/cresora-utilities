package hifumi.cresora.combat
import hifumi.cresora.adventurerank.AdventureRankMobAccess
import hifumi.cresora.adventurerank.AdventureRankProgression
import hifumi.cresora.adventurerank.AdventureRankService
import net.minecraft.entity.EntityType
import net.minecraft.entity.SpawnReason
import net.minecraft.entity.mob.HostileEntity
import net.minecraft.entity.mob.MobEntity
import net.minecraft.server.world.ServerWorld
import net.minecraft.text.Text
import net.minecraft.util.Formatting
import net.minecraft.util.math.BlockPos
import java.util.UUID
import kotlin.math.min

object FieldMobPackService {
    private const val ELITE_KEY = "cresora_mob_elite"
    private const val PACK_ID_KEY = "cresora_mob_pack_id"

    private const val NORMAL_COMMAND_TAG = "cresora_normal_mob"
    private const val ELITE_COMMAND_TAG = "cresora_elite_mob"
    private const val PACK_COMMAND_TAG = "cresora_pack_mob"

    private const val PACK_SPAWN_CHANCE = 0.18
    private const val PACK_EXTRA_MIN = 2
    private const val PACK_EXTRA_MAX = 4
    private const val PACK_ELITE_MIN = 1
    private const val PACK_ELITE_MAX = 2

    private const val ELITE_HEALTH_SCALAR = 1.32
    private const val ELITE_DEFENSE_SCALAR = 1.18
    private const val ELITE_TOUGHNESS_SCALAR = 1.12
    private const val ELITE_DAMAGE_SCALAR = 1.16

    private data class PendingPackMember(
        val packId: String,
        val rank: Int,
        val elite: Boolean
    )

    private val pendingPackMember: ThreadLocal<PendingPackMember?> = ThreadLocal.withInitial { null }

    fun eliteKey(): String = ELITE_KEY

    fun packIdKey(): String = PACK_ID_KEY

    fun initializeOnSpawn(hostile: MobEntity, world: ServerWorld, spawnReason: SpawnReason) {
        val queuedMember = pendingPackMember.get()
        if (queuedMember != null) {
            pendingPackMember.remove()
            classify(hostile, queuedMember.rank, queuedMember.elite, queuedMember.packId)
            return
        }

        if (!isManagedNaturalSpawn(spawnReason)) {
            return
        }

        val rank = AdventureRankService.getOrAssignMobRank(hostile, world)
        if (!supportsPack(hostile.type) || world.random.nextDouble() >= PACK_SPAWN_CHANCE) {
            classify(hostile, rank, false, "")
            return
        }

        val totalMembers = 1 + world.random.nextBetween(PACK_EXTRA_MIN, PACK_EXTRA_MAX)
        val eliteCount = world.random.nextBetween(PACK_ELITE_MIN, min(PACK_ELITE_MAX, totalMembers))
        val eliteSlots = mutableSetOf<Int>()
        while (eliteSlots.size < eliteCount) {
            eliteSlots += world.random.nextInt(totalMembers)
        }

        val packId = UUID.randomUUID().toString()
        classify(hostile, rank, eliteSlots.contains(0), packId)
        repeat(totalMembers - 1) { index ->
            spawnAdditionalPackMember(hostile, world, spawnReason, packId, rank, eliteSlots.contains(index + 1))
        }
    }

    fun ensureClassification(hostile: MobEntity) {
        val access = hostile as? AdventureRankMobAccess ?: return
        val normalizedPackId = access.cresoraGetMobPackId().trim()
        if (normalizedPackId != access.cresoraGetMobPackId()) {
            access.cresoraSetMobPackId(normalizedPackId)
        }
        syncCommandTags(hostile)
    }

    fun markExplicit(hostile: MobEntity, elite: Boolean) {
        val access = hostile as? AdventureRankMobAccess ?: return
        access.cresoraSetEliteMob(elite)
        access.cresoraSetMobPackId("")
        syncCommandTags(hostile)
    }

    fun eliteHealthScalar(hostile: MobEntity): Double = if (isFieldElite(hostile)) ELITE_HEALTH_SCALAR else 1.0

    fun eliteDefenseScalar(hostile: MobEntity): Double = if (isFieldElite(hostile)) ELITE_DEFENSE_SCALAR else 1.0

    fun eliteToughnessScalar(hostile: MobEntity): Double = if (isFieldElite(hostile)) ELITE_TOUGHNESS_SCALAR else 1.0

    fun eliteDamageScalar(hostile: MobEntity): Double = if (isFieldElite(hostile)) ELITE_DAMAGE_SCALAR else 1.0

    fun classificationTag(entity: MobEntity): Text {
        val access = entity as? AdventureRankMobAccess ?: return Text.empty()
        return when {
            access.cresoraIsEliteMob() -> Text.translatable("status.cresora.mob.elite").formatted(Formatting.GOLD)
            hasClassification(entity) -> Text.translatable("status.cresora.mob.normal").formatted(Formatting.GRAY)
            else -> Text.empty()
        }
    }

    private fun hasClassification(entity: MobEntity): Boolean {
        val access = entity as? AdventureRankMobAccess ?: return false
        return access.cresoraIsEliteMob() || access.cresoraGetMobPackId().isNotBlank() || entity.commandTags.contains(NORMAL_COMMAND_TAG)
    }

    private fun isFieldElite(entity: MobEntity): Boolean {
        val access = entity as? AdventureRankMobAccess ?: return false
        return access.cresoraIsEliteMob() && access.cresoraGetMobPackId().isNotBlank()
    }

    private fun classify(hostile: MobEntity, rank: Int, elite: Boolean, packId: String) {
        val access = hostile as? AdventureRankMobAccess ?: return
        access.cresoraSetMobAdventureRank(AdventureRankProgression.sanitizeRank(rank))
        access.cresoraSetEliteMob(elite)
        access.cresoraSetMobPackId(packId)
        syncCommandTags(hostile)
    }

    private fun syncCommandTags(hostile: MobEntity) {
        val access = hostile as? AdventureRankMobAccess ?: return
        hostile.removeCommandTag(NORMAL_COMMAND_TAG)
        hostile.removeCommandTag(ELITE_COMMAND_TAG)
        hostile.removeCommandTag(PACK_COMMAND_TAG)
        if (access.cresoraIsEliteMob()) {
            hostile.addCommandTag(ELITE_COMMAND_TAG)
        } else {
            hostile.addCommandTag(NORMAL_COMMAND_TAG)
        }
        if (access.cresoraGetMobPackId().isNotBlank()) {
            hostile.addCommandTag(PACK_COMMAND_TAG)
        }
    }

    private fun isManagedNaturalSpawn(spawnReason: SpawnReason): Boolean {
        return spawnReason == SpawnReason.NATURAL || spawnReason == SpawnReason.CHUNK_GENERATION
    }

    private fun supportsPack(entityType: EntityType<*>): Boolean {
        return when (HostileRewardFamilies.classify(entityType)) {
            HostileRewardFamily.SURVIVOR,
            HostileRewardFamily.ASSAULT,
            HostileRewardFamily.ARCANE -> true

            HostileRewardFamily.ELITE,
            HostileRewardFamily.RELIC -> false
        }
    }

    private fun spawnAdditionalPackMember(
        leader: MobEntity,
        world: ServerWorld,
        spawnReason: SpawnReason,
        packId: String,
        rank: Int,
        elite: Boolean
    ) {
        repeat(6) { attempt ->
            val offsetX = world.random.nextBetween(-5, 5)
            val offsetZ = world.random.nextBetween(-5, 5)
            if (offsetX == 0 && offsetZ == 0) {
                return@repeat
            }
            val yOffset = if (attempt < 3) 0 else world.random.nextBetween(-1, 1)
            val spawnPos = BlockPos.ofFloored(leader.x + offsetX, leader.y + yOffset, leader.z + offsetZ)
            pendingPackMember.set(PendingPackMember(packId, rank, elite))
            val spawned = leader.type.spawn(world, null, spawnPos, spawnReason, true, false) as? MobEntity
            if (spawned != null) {
                spawned.target = leader.target
                return
            }
            pendingPackMember.remove()
        }
    }
}
