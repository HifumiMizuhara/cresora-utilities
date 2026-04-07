package hifumi.cresora

import net.minecraft.entity.Entity
import net.minecraft.entity.LivingEntity
import net.minecraft.entity.attribute.EntityAttributeModifier
import net.minecraft.entity.attribute.EntityAttributes
import net.minecraft.entity.damage.DamageSource
import net.minecraft.entity.mob.HostileEntity
import net.minecraft.server.network.ServerPlayerEntity
import net.minecraft.server.world.ServerWorld
import net.minecraft.text.Text
import net.minecraft.util.Identifier
import kotlin.math.max
import kotlin.math.min
import kotlin.math.abs

object AdventureRankService {
    private const val PLAYER_RANK_KEY = "cresora_adventure_rank"
    private const val PLAYER_RANK_XP_KEY = "cresora_adventure_rank_xp"
    private const val MOB_RANK_KEY = "cresora_mob_adventure_rank"
    private const val SEARCH_RADIUS = 64.0
    private const val FIELD_MOB_RANK_VARIANCE = 5

    private val MOB_HEALTH_SCALAR_ID: Identifier = Identifier.of(CreSoraUtilities.MOD_ID, "mob_adventure_health_scalar")
    private val MOB_ARMOR_BONUS_ID: Identifier = Identifier.of(CreSoraUtilities.MOD_ID, "mob_adventure_armor_bonus")
    private val MOB_TOUGHNESS_BONUS_ID: Identifier = Identifier.of(CreSoraUtilities.MOD_ID, "mob_adventure_toughness_bonus")

    fun playerRankKey(): String = PLAYER_RANK_KEY

    fun playerRankXpKey(): String = PLAYER_RANK_XP_KEY

    fun mobRankKey(): String = MOB_RANK_KEY

    fun getProgress(player: ServerPlayerEntity): AdventureRankProgression.Progress {
        val access = player as? AdventureRankAccess ?: return AdventureRankProgression.normalize(AdventureRankProgression.MIN_RANK, 0)
        val normalized = AdventureRankProgression.normalize(
            access.cresoraGetAdventureRank(),
            access.cresoraGetAdventureRankXp()
        )
        if (normalized.rank != access.cresoraGetAdventureRank() || normalized.currentXp != access.cresoraGetAdventureRankXp()) {
            access.cresoraSetAdventureRank(normalized.rank)
            access.cresoraSetAdventureRankXp(normalized.currentXp)
        }
        return normalized
    }

    fun getRank(player: ServerPlayerEntity): Int = getProgress(player).rank

    fun addXp(player: ServerPlayerEntity, amount: Int) {
        if (amount <= 0) {
            return
        }

        val access = player as? AdventureRankAccess ?: return
        val before = getProgress(player)
        if (before.isMaxRank()) {
            return
        }

        var rank = before.rank
        var xp = before.currentXp + amount
        while (rank < AdventureRankProgression.MAX_RANK) {
            val required = AdventureRankProgression.requiredXpForRank(rank) ?: break
            if (xp < required) {
                break
            }
            xp -= required
            rank++
        }

        if (rank >= AdventureRankProgression.MAX_RANK) {
            access.cresoraSetAdventureRank(AdventureRankProgression.MAX_RANK)
            access.cresoraSetAdventureRankXp(0)
        } else {
            access.cresoraSetAdventureRank(rank)
            access.cresoraSetAdventureRankXp(xp)
        }

        if (rank > before.rank) {
            player.sendMessage(Text.translatable("commands.cresora.rank.rank_up", rank), false)
        }
    }

    fun addReward(player: ServerPlayerEntity, source: AdventureRankRewardSource, count: Int = 1) {
        val amount = source.xpPerUnit * count.coerceAtLeast(0)
        addXp(player, amount)
    }

    fun setRank(player: ServerPlayerEntity, rank: Int) {
        val access = player as? AdventureRankAccess ?: return
        access.cresoraSetAdventureRank(AdventureRankProgression.sanitizeRank(rank))
        access.cresoraSetAdventureRankXp(0)
    }

    fun addRawXp(player: ServerPlayerEntity, amount: Int) {
        addXp(player, amount)
    }

    fun setProgressXp(player: ServerPlayerEntity, xp: Int) {
        val access = player as? AdventureRankAccess ?: return
        val progress = getProgress(player)
        if (progress.isMaxRank()) {
            access.cresoraSetAdventureRank(AdventureRankProgression.MAX_RANK)
            access.cresoraSetAdventureRankXp(0)
            return
        }
        val capped = xp.coerceIn(0, (progress.requiredXp ?: 0) - 1)
        access.cresoraSetAdventureRank(progress.rank)
        access.cresoraSetAdventureRankXp(capped)
    }

    fun copyTo(oldPlayer: ServerPlayerEntity, newPlayer: ServerPlayerEntity) {
        val oldAccess = oldPlayer as? AdventureRankAccess ?: return
        val newAccess = newPlayer as? AdventureRankAccess ?: return
        newAccess.cresoraSetAdventureRank(oldAccess.cresoraGetAdventureRank())
        newAccess.cresoraSetAdventureRankXp(oldAccess.cresoraGetAdventureRankXp())
    }

    fun hostileKillXp(entity: HostileEntity): Int {
        val rank = (entity as? AdventureRankMobAccess)?.cresoraGetMobAdventureRank() ?: AdventureRankProgression.MIN_RANK
        return AdventureRankProfile.killXp(entity.type, rank)
    }

    fun getOrAssignMobRank(entity: HostileEntity, world: ServerWorld): Int {
        val access = entity as? AdventureRankMobAccess ?: return AdventureRankProgression.MIN_RANK
        val existing = access.cresoraGetMobAdventureRank()
        if (existing > 0) {
            return AdventureRankProgression.sanitizeRank(existing)
        }

        val assigned = rollNearbyFieldRank(entity, world, entity.x, entity.y, entity.z)
        access.cresoraSetMobAdventureRank(assigned)
        return assigned
    }

    fun applyMobScaling(entity: HostileEntity, rank: Int) {
        applyMobScaling(entity, rank, 1.0, 1.0, 1.0)
    }

    fun applyMobScaling(
        entity: HostileEntity,
        rank: Int,
        healthScalar: Double,
        defenseScalar: Double,
        toughnessScalar: Double
    ) {
        val normalizedRank = AdventureRankProgression.sanitizeRank(rank)
        val maxHealthInstance = entity.attributes.getCustomInstance(EntityAttributes.MAX_HEALTH) ?: return
        val armorInstance = entity.attributes.getCustomInstance(EntityAttributes.ARMOR)
        val toughnessInstance = entity.attributes.getCustomInstance(EntityAttributes.ARMOR_TOUGHNESS)
        val oldMaxHealth = entity.maxHealth.toDouble().coerceAtLeast(1.0)
        val healthRatio = (entity.health.toDouble() / oldMaxHealth).coerceIn(0.0, 1.0)
        val baseMaxHealth = maxHealthInstance.baseValue.coerceAtLeast(1.0)
        val effectiveHealthScalar = healthScalar.coerceAtLeast(0.1) * FieldMobPackService.eliteHealthScalar(entity)
        val effectiveDefenseScalar = defenseScalar.coerceAtLeast(0.1) * FieldMobPackService.eliteDefenseScalar(entity)
        val effectiveToughnessScalar = toughnessScalar.coerceAtLeast(0.1) * FieldMobPackService.eliteToughnessScalar(entity)
        val rawMultiplier = AdventureRankProfile.healthMultiplier(entity.type, normalizedRank) * effectiveHealthScalar
        val rawTargetHealth = baseMaxHealth * rawMultiplier
        val targetHealth = min(rawTargetHealth, AdventureRankProfile.MOB_HEALTH_CAP)
        val overflowHealth = max(0.0, rawTargetHealth - targetHealth)
        val modifierValue = targetHealth / baseMaxHealth - 1.0

        maxHealthInstance.removeModifier(MOB_HEALTH_SCALAR_ID)
        if (abs(modifierValue) > 1.0e-6) {
            maxHealthInstance.addTemporaryModifier(
                EntityAttributeModifier(
                    MOB_HEALTH_SCALAR_ID,
                    modifierValue,
                    EntityAttributeModifier.Operation.ADD_MULTIPLIED_TOTAL
                )
            )
        }

        armorInstance?.removeModifier(MOB_ARMOR_BONUS_ID)
        toughnessInstance?.removeModifier(MOB_TOUGHNESS_BONUS_ID)

        val bonus = AdventureRankProfile.defenseBonus(entity.type, normalizedRank, overflowHealth)
        if (bonus.armorFlat > 0.0) {
            armorInstance?.addTemporaryModifier(
                EntityAttributeModifier(
                    MOB_ARMOR_BONUS_ID,
                    bonus.armorFlat * effectiveDefenseScalar,
                    EntityAttributeModifier.Operation.ADD_VALUE
                )
            )
        }
        if (bonus.toughnessFlat > 0.0) {
            toughnessInstance?.addTemporaryModifier(
                EntityAttributeModifier(
                    MOB_TOUGHNESS_BONUS_ID,
                    bonus.toughnessFlat * effectiveToughnessScalar,
                    EntityAttributeModifier.Operation.ADD_VALUE
                )
            )
        }

        val scaledHealth = max(1.0, targetHealth * healthRatio)
        entity.health = scaledHealth.toFloat()
        CombatMobDisplayService.updateMobStatus(entity)
    }

    fun damageMultiplier(source: DamageSource): Double {
        val attacker = source.attacker ?: source.source
        return damageMultiplier(attacker)
    }

    fun damageMultiplier(attacker: Entity?): Double {
        val hostile = attacker as? HostileEntity ?: return 1.0
        val access = hostile as? AdventureRankMobAccess ?: return 1.0
        val storedRank = access.cresoraGetMobAdventureRank()
        val domainMultiplier = DomainService.damageMultiplier(attacker)
        val masqueradeMultiplier = MasqueradeService.damageMultiplier(attacker)
        if (storedRank <= 0) {
            return domainMultiplier * masqueradeMultiplier
        }
        return AdventureRankProfile.damageMultiplier(hostile.type, storedRank) *
            FieldMobPackService.eliteDamageScalar(hostile) *
            domainMultiplier *
            masqueradeMultiplier
    }

    fun mobRank(entity: HostileEntity): Int {
        val access = entity as? AdventureRankMobAccess ?: return AdventureRankProgression.MIN_RANK
        return AdventureRankProgression.sanitizeRank(access.cresoraGetMobAdventureRank())
    }

    fun mobLevel(entity: HostileEntity): Int = mobRank(entity)

    fun refreshMobDisplay(entity: LivingEntity) {
        CombatMobDisplayService.updateMobStatus(entity)
    }

    fun showMobDamage(target: LivingEntity, source: DamageSource, damage: Float) {
        if (damage <= 0.0f) {
            return
        }
        CombatMobDisplayService.showDamage(target, source, damage.toDouble())
    }

    fun showMobDamage(target: LivingEntity, damage: Float) {
        if (damage <= 0.0f) {
            return
        }
        CombatMobDisplayService.showDamage(target, damage.toDouble())
    }

    fun showMobTrueDamage(target: LivingEntity, attacker: ServerPlayerEntity, damage: Float) {
        if (damage <= 0.0f) {
            return
        }
        CombatMobDisplayService.showTrueDamage(target, attacker, damage.toDouble())
    }

    fun showPlayerDamageFeedback(player: ServerPlayerEntity, source: DamageSource, damage: Float) {
        if (damage <= 0.0f) {
            return
        }
        CombatMobDisplayService.showIncomingDamage(player, source, damage.toDouble())
    }

    private fun findNearestNearbyRank(world: ServerWorld, x: Double, y: Double, z: Double): Int {
        var nearestRank = AdventureRankProgression.MIN_RANK
        var nearestDistance = Double.MAX_VALUE
        val radiusSquared = SEARCH_RADIUS * SEARCH_RADIUS
        for (player in world.players) {
            val distance = player.squaredDistanceTo(x, y, z)
            if (distance > radiusSquared) {
                continue
            }
            if (distance < nearestDistance) {
                nearestDistance = distance
                nearestRank = getRank(player)
            }
        }
        return nearestRank
    }

    private fun rollNearbyFieldRank(entity: HostileEntity, world: ServerWorld, x: Double, y: Double, z: Double): Int {
        val anchorRank = findNearestNearbyRank(world, x, y, z)
        val variance = FIELD_MOB_RANK_VARIANCE.coerceAtLeast(0)
        if (variance == 0) {
            return anchorRank
        }
        val offset = entity.random.nextBetween(-variance, variance)
        return AdventureRankProgression.sanitizeRank(anchorRank + offset)
    }
}
