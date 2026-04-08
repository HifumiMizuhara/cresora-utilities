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

object DarkLuxSkill : WeaponSkillHandler {
    private data class MobStatusState(
        var darkExpireTick: Long = 0,
        var luxExpireTick: Long = 0,
        var entanglementExpireTick: Long = 0
    )

    private val mobStatusStatesByTarget: MutableMap<UUID, MobStatusState> = mutableMapOf()

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
        val world = player.world as? ServerWorld ?: return ActionResult.FAIL
        val radius = 5.0
        val targets = world.getOtherEntities(player, player.boundingBox.expand(radius)) { entity ->
            entity is LivingEntity && entity.isAlive
        }.mapNotNull { it as? LivingEntity }

        for (target in targets) {
            applyLux(world, target, player)
        }

        player.sendMessage(
            Text.translatable(
                "item.cresora.weapon.skill.dark_lux_activated",
                Text.translatable(definition.translationKey()),
                targets.size
            ).formatted(Formatting.DARK_PURPLE),
            true
        )
        return ActionResult.SUCCESS
    }

    override fun onDamageDealt(player: ServerPlayerEntity, target: LivingEntity, amount: Float, isTrueDamage: Boolean) {
        val definition = WeaponStackSupport.getDefinition(player.mainHandStack) ?: return
        if (definition.id != WeaponSkillService.DARK_LUX_ID) return
        val world = player.world as? ServerWorld ?: return
        applyDark(world, target, player)
    }

    private fun applyDark(world: ServerWorld, target: LivingEntity, player: ServerPlayerEntity?) {
        val now = world.time
        val state = mobStatusStatesByTarget.getOrPut(target.uuid) { MobStatusState() }
        if (now < state.entanglementExpireTick) return
        state.darkExpireTick = now + WeaponSkillService.DARK_DURATION_TICKS
        handleInteraction(world, target, state, player)
    }

    private fun applyLux(world: ServerWorld, target: LivingEntity, player: ServerPlayerEntity?) {
        val now = world.time
        val state = mobStatusStatesByTarget.getOrPut(target.uuid) { MobStatusState() }
        if (now < state.entanglementExpireTick) return
        state.luxExpireTick = now + WeaponSkillService.LUX_DURATION_TICKS
        handleInteraction(world, target, state, player)
    }

    private fun handleInteraction(world: ServerWorld, target: LivingEntity, state: MobStatusState, player: ServerPlayerEntity?) {
        val now = world.time
        if (now < state.darkExpireTick && now < state.luxExpireTick) {
            val roll = world.random.nextDouble()
            when {
                roll < 0.2 -> triggerAnnihilation(world, target, state, player)
                roll < 0.4 -> triggerEntanglement(world, target, state, player)
                else -> triggerDarkCollapse(world, target, state, player)
            }
        }
    }

    private fun triggerAnnihilation(world: ServerWorld, target: LivingEntity, state: MobStatusState, player: ServerPlayerEntity?) {
        player?.sendMessage(Text.translatable("message.cresora.weapon.dark_lux.annihilation").formatted(Formatting.DARK_RED), true)
        state.darkExpireTick = 0
        state.luxExpireTick = 0
        val damage = target.health * 0.9f
        target.damage(world, world.damageSources.magic(), damage)

        val nearby = world.getOtherEntities(target, target.boundingBox.expand(5.0)) { it is LivingEntity && it.isAlive }
            .take(3)
        for (e in nearby) {
            if (e is LivingEntity) {
                val aoeDamage = e.maxHealth * 0.2f
                e.damage(world, world.damageSources.magic(), aoeDamage)
            }
        }
        (target as? HostileEntity)?.let { AdventureRankService.refreshMobDisplay(it) }
    }

    private fun triggerEntanglement(world: ServerWorld, target: LivingEntity, state: MobStatusState, player: ServerPlayerEntity?) {
        player?.sendMessage(Text.translatable("message.cresora.weapon.dark_lux.entanglement").formatted(Formatting.GOLD), true)
        state.darkExpireTick = 0
        state.luxExpireTick = 0
        state.entanglementExpireTick = world.time + WeaponSkillService.ENTANGLEMENT_DURATION_TICKS
        (target as? HostileEntity)?.let { AdventureRankService.refreshMobDisplay(it) }
    }

    private fun triggerDarkCollapse(world: ServerWorld, target: LivingEntity, state: MobStatusState, player: ServerPlayerEntity?) {
        if (player != null) {
            player.sendMessage(Text.translatable("message.cresora.weapon.dark_lux.collapse").formatted(Formatting.DARK_PURPLE), true)
            player.heal(player.maxHealth * 0.05f)
            val hpRatio = player.health / player.maxHealth
            val reductionRatio = (1.0f - hpRatio).coerceIn(0.0f, 1.0f)

            val remainingTicks = WeaponSkillService.getRemainingCooldownTicks(player, WeaponSkillService.DARK_LUX_ID)
            if (remainingTicks > 0.0) {
                val newRemaining = (remainingTicks * (1.0f - reductionRatio)).toLong()
                WeaponSkillService.startCooldown(player, WeaponSkillService.DARK_LUX_ID, newRemaining)
            }
        }
    }

    fun getPhysicalResistanceOffset(target: LivingEntity): Double {
        val state = mobStatusStatesByTarget[target.uuid] ?: return 0.0
        val now = target.world.time
        if (now < state.entanglementExpireTick) return WeaponSkillService.ENTANGLEMENT_RESISTANCE_REDUCTION
        if (now < state.darkExpireTick) return WeaponSkillService.DARK_LUX_RESISTANCE_REDUCTION
        return 0.0
    }

    fun getArcaneResistanceOffset(target: LivingEntity): Double {
        val state = mobStatusStatesByTarget[target.uuid] ?: return 0.0
        val now = target.world.time
        if (now < state.entanglementExpireTick) return WeaponSkillService.ENTANGLEMENT_RESISTANCE_REDUCTION
        if (now < state.luxExpireTick) return WeaponSkillService.DARK_LUX_RESISTANCE_REDUCTION
        return 0.0
    }

    fun hasStatus(target: LivingEntity, status: String): Boolean {
        val state = mobStatusStatesByTarget[target.uuid] ?: return false
        val now = target.world.time
        return when (status) {
            "dark" -> now < state.darkExpireTick
            "lux" -> now < state.luxExpireTick
            "entanglement" -> now < state.entanglementExpireTick
            else -> false
        }
    }
}
