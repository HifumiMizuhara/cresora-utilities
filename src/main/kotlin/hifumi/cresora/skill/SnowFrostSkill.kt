package hifumi.cresora.skill

import hifumi.cresora.WeaponCombatSupport
import hifumi.cresora.WeaponData
import hifumi.cresora.WeaponDefinition
import hifumi.cresora.WeaponSkillAccess
import hifumi.cresora.WeaponSkillService
import hifumi.cresora.WeaponSkillService.HANWU_CRIT_DMG_PER_STACK_PERCENT
import hifumi.cresora.WeaponSkillService.HANWU_FROST_SLOWNESS_AMPLIFIER
import hifumi.cresora.WeaponSkillService.HANWU_JUANXUE_ID
import hifumi.cresora.WeaponSkillService.HANWU_MAX_STACKS
import hifumi.cresora.WeaponStackSupport
import net.minecraft.entity.LivingEntity
import net.minecraft.entity.effect.StatusEffectInstance
import net.minecraft.entity.effect.StatusEffects
import net.minecraft.registry.RegistryKey
import net.minecraft.server.MinecraftServer
import net.minecraft.server.network.ServerPlayerEntity
import net.minecraft.server.world.ServerWorld
import net.minecraft.text.Text
import net.minecraft.util.ActionResult
import net.minecraft.util.Formatting
import net.minecraft.world.World
import java.util.UUID

data class SnowMistState(
    val weaponId: String,
    var expireTick: Long,
    var stackCount: Int
)

data class FrostState(
    val worldKey: RegistryKey<World>,
    val targetUuid: UUID,
    var expireTick: Long,
    var nextPulseTick: Long,
    var nextDamagePoints: Float,
    var maxDamagePoints: Float
)

object SnowFrostSkill : WeaponSkillHandler {
    val snowMistStatesByPlayer: MutableMap<UUID, SnowMistState> = mutableMapOf()
    val frostStatesByTarget: MutableMap<UUID, FrostState> = mutableMapOf()

    override fun activate(
        player: ServerPlayerEntity,
        definition: WeaponDefinition,
        data: WeaponData,
        access: WeaponSkillAccess
    ): ActionResult {
        WeaponSkillService.clearShield(player)
        access.cresoraSetShieldExpireTick(0L)
        access.cresoraSetShieldWeaponId(null)
        val now = WeaponSkillService.currentWorldTime(player)
        val expireTick = now + definition.skill.durationSeconds.coerceAtLeast(1) * 20L
        snowMistStatesByPlayer[player.uuid] = SnowMistState(
            weaponId = definition.id,
            expireTick = expireTick,
            stackCount = 0
        )
        player.sendMessage(
            Text.translatable(
                "item.cresora.weapon.skill.snow_frost_activated",
                Text.translatable(definition.translationKey()),
                definition.skill.durationSeconds,
                formatNumber(WeaponCombatSupport.skillValueHearts(definition, data))
            ).formatted(Formatting.AQUA),
            true
        )
        return ActionResult.SUCCESS
    }

    override fun onTick(server: MinecraftServer) {
        val iterator = frostStatesByTarget.entries.iterator()
        while (iterator.hasNext()) {
            val (_, state) = iterator.next()
            val world = server.getWorld(state.worldKey)
            if (world == null) {
                iterator.remove()
                continue
            }
            val target = world.getEntity(state.targetUuid) as? LivingEntity
            if (target == null || !target.isAlive) {
                iterator.remove()
                continue
            }
            val now = world.time
            if (now >= state.expireTick) {
                iterator.remove()
                continue
            }
            while (now >= state.nextPulseTick && state.nextPulseTick < state.expireTick) {
                target.addStatusEffect(StatusEffectInstance(StatusEffects.SLOWNESS, 40, HANWU_FROST_SLOWNESS_AMPLIFIER, false, true, true))
                val damagePoints = state.nextDamagePoints.coerceAtMost(state.maxDamagePoints).coerceAtLeast(1.0f)
                target.damage(world, world.damageSources.freeze(), damagePoints)
                state.nextDamagePoints = (state.nextDamagePoints + 1.0f).coerceAtMost(state.maxDamagePoints)
                state.nextPulseTick += 20L
            }
        }
    }

    override fun onPlayerTick(player: ServerPlayerEntity) {
        val state = snowMistStatesByPlayer[player.uuid] ?: return
        if (WeaponSkillService.currentWorldTime(player) < state.expireTick) {
            return
        }
        snowMistStatesByPlayer.remove(player.uuid)
        player.sendMessage(Text.translatable("item.cresora.weapon.skill.snow_mist_expired").formatted(Formatting.GRAY), true)
    }

    override fun onDamageDealt(player: ServerPlayerEntity, target: LivingEntity, amount: Float, isTrueDamage: Boolean) {
        val definition = WeaponStackSupport.getDefinition(player.mainHandStack) ?: return
        if (definition.id != HANWU_JUANXUE_ID) return
        
        val world = player.world as? ServerWorld ?: return
        val data = WeaponStackSupport.getWeaponData(player.mainHandStack) ?: return
        val state = snowMistStatesByPlayer[player.uuid] ?: return
        val now = WeaponSkillService.currentWorldTime(player)
        if (now >= state.expireTick) {
            return
        }
        if (state.stackCount < HANWU_MAX_STACKS) {
            state.stackCount += 1
            player.sendMessage(
                Text.translatable(
                    "item.cresora.weapon.skill.snow_mist_stack",
                    state.stackCount,
                    state.stackCount * HANWU_CRIT_DMG_PER_STACK_PERCENT
                ).formatted(Formatting.AQUA),
                true
            )
        }
        applyFrost(world, target, WeaponCombatSupport.skillValueHearts(definition, data))
    }

    private fun applyFrost(world: ServerWorld, target: LivingEntity, maxDamageHearts: Double) {
        val now = world.time
        val maxDamagePoints = (maxDamageHearts.coerceAtLeast(0.5) * 2.0).toFloat()
        frostStatesByTarget[target.uuid] = FrostState(
            worldKey = world.registryKey,
            targetUuid = target.uuid,
            expireTick = now + 200L,
            nextPulseTick = now + 20L,
            nextDamagePoints = 1.0f,
            maxDamagePoints = maxDamagePoints
        )
        target.addStatusEffect(StatusEffectInstance(StatusEffects.SLOWNESS, 200, HANWU_FROST_SLOWNESS_AMPLIFIER, false, true, true))
    }

    private fun formatNumber(value: Double): String {
        val rounded = kotlin.math.round(value * 10.0) / 10.0
        return if (rounded % 1.0 == 0.0) rounded.toInt().toString() else rounded.toString()
    }

    fun critDamageBonusPercent(player: ServerPlayerEntity): Double {
        val state = snowMistStatesByPlayer[player.uuid] ?: return 0.0
        if (WeaponSkillService.currentWorldTime(player) >= state.expireTick) {
            return 0.0
        }
        return state.stackCount.coerceIn(0, HANWU_MAX_STACKS) * HANWU_CRIT_DMG_PER_STACK_PERCENT
    }
}
