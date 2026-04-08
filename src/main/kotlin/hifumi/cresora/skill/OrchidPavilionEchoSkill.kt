package hifumi.cresora.skill

import hifumi.cresora.*
import net.minecraft.entity.LivingEntity
import net.minecraft.entity.mob.HostileEntity
import net.minecraft.server.network.ServerPlayerEntity
import net.minecraft.server.world.ServerWorld
import net.minecraft.text.Text
import net.minecraft.util.ActionResult
import net.minecraft.util.Formatting
import java.util.*

object OrchidPavilionEchoSkill : WeaponSkillHandler {
    private enum class OrchidPavilionEffect {
        RAISE_A_CUP,
        RECITE_POETRY,
        PLACE_A_STONE,
        INK_BRUSH
    }

    private data class OrchidPavilionState(
        val weaponId: String,
        var expireTick: Long,
        var nextPulseTick: Long,
        var raiseACupStacks: Int = 0,
        var recitePoetryStacks: Int = 0,
        var placeAStoneStacks: Int = 0,
        var zhiStacks: Int = 0,
        var stoneGuardHp: Float = 0.0f,
        val invulnerableExpireTicks: MutableList<Long> = mutableListOf()
    )

    private val orchidPavilionStatesByPlayer: MutableMap<UUID, OrchidPavilionState> = mutableMapOf()

    override fun activate(
        player: ServerPlayerEntity,
        definition: WeaponDefinition,
        data: WeaponData,
        access: WeaponSkillAccess
    ): ActionResult {
        WeaponSkillService.startCooldown(player, definition.id, definition.skill.cooldownSeconds * 20L)
        WeaponSkillService.showCooldownBar(player, definition)
        WeaponSkillService.clearShield(player)
        access.cresoraSetShieldExpireTick(0L)
        access.cresoraSetShieldWeaponId(null)
        val now = WeaponSkillService.currentWorldTime(player)
        val intervalTicks = (definition.skill.tickIntervalSeconds.coerceAtLeast(0.5) * 20.0).toLong().coerceAtLeast(1L)
        orchidPavilionStatesByPlayer[player.uuid] = OrchidPavilionState(
            weaponId = definition.id,
            expireTick = now + definition.skill.durationSeconds.coerceAtLeast(1) * 20L,
            nextPulseTick = now + intervalTicks
        )
        player.sendMessage(
            Text.translatable(
                "item.cresora.weapon.skill.orchid_pavilion_echo_activated",
                Text.translatable(definition.translationKey()),
                definition.skill.durationSeconds,
                definition.skill.cooldownSeconds
            ).formatted(Formatting.LIGHT_PURPLE),
            true
        )
        return ActionResult.SUCCESS
    }

    override fun onPlayerTick(player: ServerPlayerEntity) {
        val state = orchidPavilionStatesByPlayer[player.uuid] ?: return
        val now = WeaponSkillService.currentWorldTime(player)
        pruneExpiredOrchidInvulnerability(player)
        if (now >= state.expireTick || !player.isAlive) {
            orchidPavilionStatesByPlayer.remove(player.uuid)
            player.sendMessage(Text.translatable("item.cresora.weapon.skill.orchid_pavilion_echo_expired").formatted(Formatting.GRAY), true)
            return
        }
        while (now >= state.nextPulseTick && state.nextPulseTick <= state.expireTick) {
            applyOrchidPavilionEffect(player, state)
            state.nextPulseTick += 40L
        }
    }

    override fun onDamageAbsorbed(player: ServerPlayerEntity, amount: Float): Float {
        pruneExpiredOrchidInvulnerability(player)
        val orchidState = activeOrchidPavilionState(player)
        if (orchidState != null && orchidState.invulnerableExpireTicks.isNotEmpty()) {
            player.sendMessage(Text.translatable("item.cresora.weapon.skill.orchid_pavilion_echo_invulnerable").formatted(Formatting.LIGHT_PURPLE), true)
            return 0.0f
        }
        var remainingAmount = amount
        if (orchidState != null && orchidState.stoneGuardHp > 0.0f) {
            if (remainingAmount <= orchidState.stoneGuardHp) {
                orchidState.stoneGuardHp -= remainingAmount
                player.sendMessage(
                    Text.translatable("item.cresora.weapon.skill.temp_guard_blocked", WeaponSkillService.formatNumber(remainingAmount / 2.0)).formatted(Formatting.BLUE),
                    true
                )
                return 0.0f
            }
            player.sendMessage(Text.translatable("item.cresora.weapon.skill.temp_guard_broken").formatted(Formatting.BLUE), true)
            remainingAmount -= orchidState.stoneGuardHp
            orchidState.stoneGuardHp = 0.0f
        }
        return remainingAmount
    }

    override fun onDamageDealt(player: ServerPlayerEntity, target: LivingEntity, amount: Float, isTrueDamage: Boolean) {
        val definition = WeaponStackSupport.getDefinition(player.mainHandStack) ?: return
        if (definition.id != WeaponSkillService.KYOKUSUI_NO_RYUSHO_ID) return
        
        val state = activeOrchidPavilionState(player) ?: return
        if (!target.isAlive) return
        
        val piercingDamage = state.zhiStacks * WeaponSkillService.KYOKUSUI_ZHI_TRUE_DAMAGE
        if (piercingDamage > 0.0f) {
            applyPiercingDamage(player, target, piercingDamage)
        }
    }

    private fun applyOrchidPavilionEffect(player: ServerPlayerEntity, state: OrchidPavilionState) {
        val world = player.world as? ServerWorld ?: return
        when (OrchidPavilionEffect.entries[world.random.nextInt(OrchidPavilionEffect.entries.size)]) {
            OrchidPavilionEffect.RAISE_A_CUP -> {
                state.raiseACupStacks += 1
                state.zhiStacks += 1
                player.sendMessage(
                    Text.translatable(
                        "item.cresora.weapon.skill.orchid_pavilion_echo.raise_a_cup",
                        state.raiseACupStacks,
                        WeaponSkillService.formatNumber(state.raiseACupStacks * WeaponSkillService.KYOKUSUI_ATTACK_PER_STACK * 100.0),
                        WeaponSkillService.formatNumber(state.raiseACupStacks * WeaponSkillService.KYOKUSUI_CRIT_DMG_PER_STACK_PERCENT),
                        state.zhiStacks,
                        WeaponSkillService.formatNumber(state.zhiStacks * WeaponSkillService.KYOKUSUI_ZHI_TRUE_DAMAGE / 2.0)
                    ).formatted(Formatting.RED),
                    true
                )
            }

            OrchidPavilionEffect.RECITE_POETRY -> {
                state.recitePoetryStacks += 1
                player.sendMessage(
                    Text.translatable(
                        "item.cresora.weapon.skill.orchid_pavilion_echo.recite_poetry",
                        state.recitePoetryStacks,
                        WeaponSkillService.formatNumber(state.recitePoetryStacks * WeaponSkillService.KYOKUSUI_ARMOR_PER_STACK * 100.0),
                        state.recitePoetryStacks * WeaponSkillService.KYOKUSUI_REGEN_STAGE_PER_STACK
                    ).formatted(Formatting.BLUE),
                    true
                )
            }

            OrchidPavilionEffect.PLACE_A_STONE -> {
                state.placeAStoneStacks += 1
                state.stoneGuardHp += WeaponSkillService.KYOKUSUI_STONE_GUARD_HP
                player.sendMessage(
                    Text.translatable(
                        "item.cresora.weapon.skill.orchid_pavilion_echo.place_a_stone",
                        state.placeAStoneStacks,
                        WeaponSkillService.formatNumber(state.stoneGuardHp / 2.0)
                    ).formatted(Formatting.AQUA),
                    true
                )
            }

            OrchidPavilionEffect.INK_BRUSH -> {
                state.invulnerableExpireTicks += WeaponSkillService.currentWorldTime(player) + WeaponSkillService.KYOKUSUI_INK_INVULN_TICKS
                val healedTargets = WeaponSkillService.healNearbyAllies(player, 5.0, WeaponSkillService.KYOKUSUI_INK_HEAL_HP)
                player.sendMessage(
                    Text.translatable(
                        "item.cresora.weapon.skill.orchid_pavilion_echo.ink_brush",
                        healedTargets,
                        WeaponSkillService.formatNumber(WeaponSkillService.KYOKUSUI_INK_HEAL_HP / 2.0),
                        WeaponSkillService.formatNumber(totalOrchidInvulnerableSeconds(player))
                    ).formatted(Formatting.LIGHT_PURPLE),
                    true
                )
            }
        }
    }

    private fun applyPiercingDamage(player: ServerPlayerEntity, target: LivingEntity, amountHp: Float) {
        if (amountHp <= 0.0f) {
            return
        }
        val world = player.world as? ServerWorld ?: return
        val remainingHealth = target.health - amountHp
        AdventureRankService.showMobTrueDamage(target, player, amountHp)
        if (remainingHealth > 0.0f) {
            target.health = remainingHealth
        } else {
            target.health = 0.001f
            target.damage(world, world.damageSources.playerAttack(player), Float.MAX_VALUE)
        }
        (target as? HostileEntity)?.let(AdventureRankService::refreshMobDisplay)
    }

    private fun activeOrchidPavilionState(player: ServerPlayerEntity): OrchidPavilionState? {
        val state = orchidPavilionStatesByPlayer[player.uuid] ?: return null
        val now = WeaponSkillService.currentWorldTime(player)
        return if (now < state.expireTick && player.isAlive) state else null
    }

    private fun pruneExpiredOrchidInvulnerability(player: ServerPlayerEntity) {
        val state = orchidPavilionStatesByPlayer[player.uuid] ?: return
        val now = WeaponSkillService.currentWorldTime(player)
        state.invulnerableExpireTicks.removeIf { it <= now }
    }

    private fun totalOrchidInvulnerableSeconds(player: ServerPlayerEntity): Double {
        val state = orchidPavilionStatesByPlayer[player.uuid] ?: return 0.0
        pruneExpiredOrchidInvulnerability(player)
        val now = WeaponSkillService.currentWorldTime(player)
        return state.invulnerableExpireTicks.sumOf { (it - now).coerceAtLeast(0L).toDouble() } / 20.0
    }

    fun critDamageBonusPercent(player: ServerPlayerEntity): Double {
        val orchidState = activeOrchidPavilionState(player)
        return orchidState?.let { it.raiseACupStacks * WeaponSkillService.KYOKUSUI_CRIT_DMG_PER_STACK_PERCENT } ?: 0.0
    }

    fun orchidPavilionAttackScalar(player: ServerPlayerEntity): Double {
        val state = activeOrchidPavilionState(player) ?: return 0.0
        return state.raiseACupStacks * WeaponSkillService.KYOKUSUI_ATTACK_PER_STACK
    }

    fun orchidPavilionArmorScalar(player: ServerPlayerEntity): Double {
        val state = activeOrchidPavilionState(player) ?: return 0.0
        return state.recitePoetryStacks * WeaponSkillService.KYOKUSUI_ARMOR_PER_STACK
    }

    fun orchidPavilionRegenStageBonus(player: ServerPlayerEntity): Int {
        val state = activeOrchidPavilionState(player) ?: return 0
        return state.recitePoetryStacks * WeaponSkillService.KYOKUSUI_REGEN_STAGE_PER_STACK
    }
}
