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
    private const val BOSS_KEY = "cresora_mob_boss"
    private const val PACK_ID_KEY = "cresora_mob_pack_id"

    private const val NORMAL_COMMAND_TAG = "cresora_normal_mob"
    private const val ELITE_COMMAND_TAG = "cresora_elite_mob"
    private const val BOSS_COMMAND_TAG = "cresora_boss_mob"
    private const val PACK_COMMAND_TAG = "cresora_pack_mob"
    private const val ECLIPSE_ELITE_PACK_ID = "cresora_eclipse_elite"
    private const val FIELD_BOSS_PACK_ID = "cresora_field_boss"
    private const val ECLIPSE_PROMOTED_COMMAND_TAG = "cresora_eclipse_promoted"
    private const val ECLIPSE_WAS_ELITE_COMMAND_TAG = "cresora_eclipse_was_elite"

    private const val BOSS_SPAWN_CHANCE = 0.0025
    private const val PACK_SPAWN_CHANCE = 0.18
    private const val PACK_EXTRA_MIN = 2
    private const val PACK_EXTRA_MAX = 4
    private const val PACK_ELITE_MIN = 1
    private const val PACK_ELITE_MAX = 2

    private const val ELITE_HEALTH_SCALAR = 1.32
    private const val ELITE_DEFENSE_SCALAR = 1.18
    private const val ELITE_TOUGHNESS_SCALAR = 1.12
    private const val ELITE_DAMAGE_SCALAR = 1.16
    private const val ELITE_SCALE_BONUS = 0.18

    private const val BOSS_HEALTH_SCALAR = 2.20
    private const val BOSS_DEFENSE_SCALAR = 1.45
    private const val BOSS_TOUGHNESS_SCALAR = 1.35
    private const val BOSS_DAMAGE_SCALAR = 1.35
    private const val BOSS_SCALE_BONUS = 0.35

    private data class PendingPackMember(
        val packId: String,
        val rank: Int,
        val elite: Boolean,
        val boss: Boolean
    )

    private val pendingPackMember: ThreadLocal<PendingPackMember?> = ThreadLocal.withInitial { null }

    fun eliteKey(): String = ELITE_KEY

    fun bossKey(): String = BOSS_KEY

    fun packIdKey(): String = PACK_ID_KEY

    fun initializeOnSpawn(hostile: MobEntity, world: ServerWorld, spawnReason: SpawnReason) {
        val queuedMember = pendingPackMember.get()
        if (queuedMember != null) {
            pendingPackMember.remove()
            classify(hostile, queuedMember.rank, queuedMember.elite, queuedMember.boss, queuedMember.packId)
            applyEclipseEliteIfActive(hostile, world)
            return
        }

        if (!isManagedNaturalSpawn(spawnReason)) {
            return
        }

        if (world.random.nextDouble() < BOSS_SPAWN_CHANCE) {
            val access = hostile as? AdventureRankMobAccess
            access?.cresoraSetBossMob(true)
            val bossRank = AdventureRankService.getOrAssignMobRank(hostile, world)
            classify(hostile, bossRank, true, true, FIELD_BOSS_PACK_ID)
            applyEclipseEliteIfActive(hostile, world)
            return
        }

        val rank = AdventureRankService.getOrAssignMobRank(hostile, world)
        if (!supportsPack(hostile.type) || world.random.nextDouble() >= PACK_SPAWN_CHANCE) {
            classify(hostile, rank, false, false, "")
            applyEclipseEliteIfActive(hostile, world)
            return
        }

        val totalMembers = 1 + world.random.nextBetween(PACK_EXTRA_MIN, PACK_EXTRA_MAX)
        val eliteCount = world.random.nextBetween(PACK_ELITE_MIN, min(PACK_ELITE_MAX, totalMembers))
        val eliteSlots = mutableSetOf<Int>()
        while (eliteSlots.size < eliteCount) {
            eliteSlots += world.random.nextInt(totalMembers)
        }

        val packId = UUID.randomUUID().toString()
        classify(hostile, rank, eliteSlots.contains(0), false, packId)
        applyEclipseEliteIfActive(hostile, world)
        repeat(totalMembers - 1) { index ->
            spawnAdditionalPackMember(hostile, world, spawnReason, packId, rank, eliteSlots.contains(index + 1), false)
        }
    }

    private fun applyEclipseEliteIfActive(hostile: MobEntity, world: ServerWorld) {
        val server = world.server ?: return
        if (hifumi.cresora.bloodmoon.MoonPhaseService.isSolarEclipse(server)) {
            promoteToEclipseElite(hostile)
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
        markExplicit(hostile, elite, false)
    }

    fun markExplicit(hostile: MobEntity, elite: Boolean, boss: Boolean) {
        val access = hostile as? AdventureRankMobAccess ?: return
        access.cresoraSetEliteMob(elite)
        access.cresoraSetBossMob(boss)
        access.cresoraSetMobPackId("")
        syncCommandTags(hostile)
    }

    fun promoteToEclipseElite(hostile: MobEntity) {
        val access = hostile as? AdventureRankMobAccess ?: return
        if (!hostile.commandTags.contains(ECLIPSE_PROMOTED_COMMAND_TAG)) {
            hostile.addCommandTag(ECLIPSE_PROMOTED_COMMAND_TAG)
            if (access.cresoraIsEliteMob()) {
                hostile.addCommandTag(ECLIPSE_WAS_ELITE_COMMAND_TAG)
            }
        }
        access.cresoraSetEliteMob(true)
        if (access.cresoraGetMobPackId().isBlank()) {
            access.cresoraSetMobPackId(ECLIPSE_ELITE_PACK_ID)
        }
        syncCommandTags(hostile)
    }

    fun demoteEclipseElite(hostile: MobEntity) {
        val access = hostile as? AdventureRankMobAccess ?: return
        if (!hostile.commandTags.contains(ECLIPSE_PROMOTED_COMMAND_TAG)) return
        val wasElite = hostile.commandTags.contains(ECLIPSE_WAS_ELITE_COMMAND_TAG)
        if (access.cresoraGetMobPackId() == ECLIPSE_ELITE_PACK_ID) {
            access.cresoraSetMobPackId("")
        }
        access.cresoraSetEliteMob(wasElite)
        hostile.removeCommandTag(ECLIPSE_PROMOTED_COMMAND_TAG)
        hostile.removeCommandTag(ECLIPSE_WAS_ELITE_COMMAND_TAG)
        syncCommandTags(hostile)
    }

    fun eliteHealthScalar(hostile: MobEntity): Double = when {
        isFieldBoss(hostile) -> BOSS_HEALTH_SCALAR
        isFieldElite(hostile) -> ELITE_HEALTH_SCALAR
        else -> 1.0
    }

    fun eliteDefenseScalar(hostile: MobEntity): Double = when {
        isFieldBoss(hostile) -> BOSS_DEFENSE_SCALAR
        isFieldElite(hostile) -> ELITE_DEFENSE_SCALAR
        else -> 1.0
    }

    fun eliteToughnessScalar(hostile: MobEntity): Double = when {
        isFieldBoss(hostile) -> BOSS_TOUGHNESS_SCALAR
        isFieldElite(hostile) -> ELITE_TOUGHNESS_SCALAR
        else -> 1.0
    }

    fun eliteDamageScalar(hostile: MobEntity): Double = when {
        isFieldBoss(hostile) -> BOSS_DAMAGE_SCALAR
        isFieldElite(hostile) -> ELITE_DAMAGE_SCALAR
        else -> 1.0
    }

    fun scaleBonus(hostile: MobEntity): Double = when {
        (hostile as? AdventureRankMobAccess)?.cresoraIsBossMob() == true -> BOSS_SCALE_BONUS
        (hostile as? AdventureRankMobAccess)?.cresoraIsEliteMob() == true -> ELITE_SCALE_BONUS
        else -> 0.0
    }

    fun classificationTag(entity: MobEntity): Text {
        val access = entity as? AdventureRankMobAccess ?: return Text.empty()
        return when {
            access.cresoraIsBossMob() -> Text.translatable("status.cresora.mob.boss").formatted(Formatting.DARK_RED, Formatting.BOLD)
            access.cresoraIsEliteMob() -> Text.translatable("status.cresora.mob.elite").formatted(Formatting.GOLD)
            hasClassification(entity) -> Text.translatable("status.cresora.mob.normal").formatted(Formatting.GRAY)
            else -> Text.empty()
        }
    }

    private fun hasClassification(entity: MobEntity): Boolean {
        val access = entity as? AdventureRankMobAccess ?: return false
        return access.cresoraIsBossMob() || access.cresoraIsEliteMob() || access.cresoraGetMobPackId().isNotBlank() || entity.commandTags.contains(NORMAL_COMMAND_TAG)
    }

    private fun isFieldElite(entity: MobEntity): Boolean {
        val access = entity as? AdventureRankMobAccess ?: return false
        return access.cresoraIsEliteMob() && access.cresoraGetMobPackId().isNotBlank()
    }

    private fun isFieldBoss(entity: MobEntity): Boolean {
        val access = entity as? AdventureRankMobAccess ?: return false
        return access.cresoraIsBossMob() && access.cresoraGetMobPackId().isNotBlank()
    }

    private fun classify(hostile: MobEntity, rank: Int, elite: Boolean, boss: Boolean, packId: String) {
        val access = hostile as? AdventureRankMobAccess ?: return
        access.cresoraSetMobAdventureRank(AdventureRankProgression.sanitizeRank(rank))
        access.cresoraSetEliteMob(elite)
        access.cresoraSetBossMob(boss)
        access.cresoraSetMobPackId(packId)
        syncCommandTags(hostile)
    }

    private fun syncCommandTags(hostile: MobEntity) {
        val access = hostile as? AdventureRankMobAccess ?: return
        hostile.removeCommandTag(NORMAL_COMMAND_TAG)
        hostile.removeCommandTag(ELITE_COMMAND_TAG)
        hostile.removeCommandTag(BOSS_COMMAND_TAG)
        hostile.removeCommandTag(PACK_COMMAND_TAG)
        if (access.cresoraIsBossMob()) {
            hostile.addCommandTag(BOSS_COMMAND_TAG)
        } else if (access.cresoraIsEliteMob()) {
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
        elite: Boolean,
        boss: Boolean
    ) {
        repeat(6) { attempt ->
            val offsetX = world.random.nextBetween(-5, 5)
            val offsetZ = world.random.nextBetween(-5, 5)
            if (offsetX == 0 && offsetZ == 0) {
                return@repeat
            }
            val yOffset = if (attempt < 3) 0 else world.random.nextBetween(-1, 1)
            val spawnPos = BlockPos.ofFloored(leader.x + offsetX, leader.y + yOffset, leader.z + offsetZ)
            pendingPackMember.set(PendingPackMember(packId, rank, elite, boss))
            val spawned = leader.type.spawn(world, null, spawnPos, spawnReason, true, false) as? MobEntity
            if (spawned != null) {
                spawned.target = leader.target
                return
            }
            pendingPackMember.remove()
        }
    }
}
