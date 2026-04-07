package hifumi.cresora

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents
import net.minecraft.block.Blocks
import net.minecraft.entity.LivingEntity
import net.minecraft.entity.EntityType
import net.minecraft.entity.attribute.EntityAttributes
import net.minecraft.entity.passive.SheepEntity
import net.minecraft.entity.effect.StatusEffectInstance
import net.minecraft.entity.effect.StatusEffects
import net.minecraft.entity.boss.BossBar
import net.minecraft.entity.boss.ServerBossBar
import net.minecraft.entity.mob.HostileEntity
import net.minecraft.entity.mob.MobEntity
import net.minecraft.registry.RegistryKey
import net.minecraft.server.network.ServerPlayerEntity
import net.minecraft.server.MinecraftServer
import net.minecraft.server.world.ServerWorld
import net.minecraft.text.Text
import net.minecraft.util.ActionResult
import net.minecraft.util.Formatting
import net.minecraft.world.World
import net.minecraft.world.biome.Biome
import java.util.UUID

object WeaponSkillService {
    private enum class OrchidPavilionEffect {
        RAISE_A_CUP,
        RECITE_POETRY,
        PLACE_A_STONE,
        INK_BRUSH
    }

    private data class SunlitHasteState(
        val weaponId: String,
        var expireTick: Long,
        val normalAmplifier: Int,
        val sunlightAmplifier: Int
    )

    private data class SnowMistState(
        val weaponId: String,
        var expireTick: Long,
        var stackCount: Int
    )

    private data class FrostState(
        val worldKey: RegistryKey<World>,
        val targetUuid: UUID,
        var expireTick: Long,
        var nextPulseTick: Long,
        var nextDamagePoints: Float,
        var maxDamagePoints: Float
    )

    private data class BoyaState(
        val weaponId: String,
        var expireTick: Long,
        var nextPulseTick: Long,
        val intervalTicks: Long,
        val pulseHealHp: Float,
        val knockbackRadius: Double
    )

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

    private const val HANWU_JUANXUE_ID = "hanwu_juanxue"
    private const val HANWU_CRIT_DMG_PER_STACK_PERCENT = 10.0
    private const val HANWU_MAX_STACKS = 5
    private const val HANWU_SNOW_ATTACK_SCALAR = 0.5
    private const val HANWU_FROST_SLOWNESS_AMPLIFIER = 1

    private const val KYOKUSUI_NO_RYUSHO_ID = "kyokusui_no_ryusho"
    private const val KYOKUSUI_ATTACK_PER_STACK = 0.10
    private const val KYOKUSUI_CRIT_DMG_PER_STACK_PERCENT = 15.0
    private const val KYOKUSUI_ARMOR_PER_STACK = 0.20
    private const val KYOKUSUI_REGEN_STAGE_PER_STACK = 2
    private const val KYOKUSUI_STONE_GUARD_HP = 7.0f
    private const val KYOKUSUI_ZHI_TRUE_DAMAGE = 2.0f
    private const val KYOKUSUI_INK_HEAL_HP = 8.0f
    private const val KYOKUSUI_INK_INVULN_TICKS = 2 * 20L

    private const val DARK_LUX_ID = "dark_lux"
    private const val DARK_DURATION_TICKS = 10 * 20L
    private const val LUX_DURATION_TICKS = 30 * 20L
    private const val ENTANGLEMENT_DURATION_TICKS = 10 * 20L
    private const val DARK_LUX_RESISTANCE_REDUCTION = 0.20
    private const val ENTANGLEMENT_RESISTANCE_REDUCTION = 0.50

    private data class MobStatusState(
        var darkExpireTick: Long = 0,
        var luxExpireTick: Long = 0,
        var entanglementExpireTick: Long = 0
    )

    private val cooldownBars: MutableMap<UUID, MutableMap<String, ServerBossBar>> = mutableMapOf()
    private val cooldownsByPlayer: MutableMap<UUID, MutableMap<String, Long>> = mutableMapOf()
    private val temporaryGuardHpByPlayer: MutableMap<UUID, Float> = mutableMapOf()
    private val temporaryGuardExpireTickByPlayer: MutableMap<UUID, Long> = mutableMapOf()
    private val sunlitHasteStatesByPlayer: MutableMap<UUID, SunlitHasteState> = mutableMapOf()
    private val boyaStatesByPlayer: MutableMap<UUID, BoyaState> = mutableMapOf()
    private val snowMistStatesByPlayer: MutableMap<UUID, SnowMistState> = mutableMapOf()
    private val orchidPavilionStatesByPlayer: MutableMap<UUID, OrchidPavilionState> = mutableMapOf()
    private val frostStatesByTarget: MutableMap<UUID, FrostState> = mutableMapOf()
    private val mobStatusStatesByTarget: MutableMap<UUID, MobStatusState> = mutableMapOf()

    fun init() {
        ServerTickEvents.END_SERVER_TICK.register { server ->
            val onlinePlayers = server.playerManager.playerList
            pruneOfflineState(onlinePlayers.mapTo(linkedSetOf(), ServerPlayerEntity::getUuid))
            for (player in onlinePlayers) {
                tickSunlitHaste(player)
                clearExpiredSnowMist(player)
                clearExpiredTemporaryGuard(player)
                clearExpiredShield(player)
                tickOrchidPavilion(player)
                tickBoya(player)
                updateCooldownFeedback(player)
            }
            tickFrost(server)
        }
    }

    fun tryActivate(player: ServerPlayerEntity, stack: net.minecraft.item.ItemStack): ActionResult {
        val definition = WeaponStackSupport.getDefinition(stack) ?: return ActionResult.PASS
        val data = WeaponStackSupport.ensureWeaponData(stack)
        if (isCoolingDown(player, definition.id)) {
            player.sendMessage(Text.translatable("item.cresora.weapon.skill.cooldown").formatted(Formatting.RED), true)
            return ActionResult.FAIL
        }
        val access = player as? WeaponSkillAccess ?: return ActionResult.FAIL
        return when (definition.skill.effectId) {
            "none" -> {
                player.sendMessage(Text.translatable("item.cresora.weapon.skill.none").formatted(Formatting.GRAY), true)
                ActionResult.SUCCESS
            }
            "current_hp_true_damage" -> {
                startCooldown(player, definition.id, currentWorldTime(player) + definition.skill.cooldownSeconds * 20L)
                showCooldownBar(player, definition)
                activateCurrentHpBurst(player, definition, data, access)
            }
            "flame_aura" -> {
                startCooldown(player, definition.id, currentWorldTime(player) + definition.skill.cooldownSeconds * 20L)
                showCooldownBar(player, definition)
                activateFlameAura(player, definition, data, access)
            }
            "snow_frost" -> {
                startCooldown(player, definition.id, currentWorldTime(player) + definition.skill.cooldownSeconds * 20L)
                showCooldownBar(player, definition)
                activateSnowFrost(player, definition, data, access)
            }
            "healing_aura" -> {
                startCooldown(player, definition.id, currentWorldTime(player) + definition.skill.cooldownSeconds * 20L)
                showCooldownBar(player, definition)
                activateHealingAura(player, definition, data, access)
            }
            "heal" -> activateHeal(player, definition, data, access)
            "dark_lux" -> {
                startCooldown(player, definition.id, currentWorldTime(player) + definition.skill.cooldownSeconds * 20L)
                showCooldownBar(player, definition)
                activateDarkLux(player, definition, data, access)
            }
            "sunlit_haste" -> {
                startCooldown(player, definition.id, currentWorldTime(player) + definition.skill.cooldownSeconds * 20L)
                showCooldownBar(player, definition)
                activateSunlitHaste(player, definition, data, access)
            }
            "baa_mimic" -> {
                startCooldown(player, definition.id, currentWorldTime(player) + definition.skill.cooldownSeconds * 20L)
                showCooldownBar(player, definition)
                activateBaaMimic(player, definition, data, access)
            }
            "orchid_pavilion_echo" -> {
                startCooldown(player, definition.id, currentWorldTime(player) + definition.skill.cooldownSeconds * 20L)
                showCooldownBar(player, definition)
                activateOrchidPavilionEcho(player, definition, data, access)
            }
            else -> {
                startCooldown(player, definition.id, currentWorldTime(player) + definition.skill.cooldownSeconds * 20L)
                showCooldownBar(player, definition)
                activateShield(player, definition, data, access)
            }
        }
    }

    fun absorbDamage(player: ServerPlayerEntity, amount: Float): Float {
        clearExpiredTemporaryGuard(player)
        clearExpiredShield(player)
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
                    Text.translatable("item.cresora.weapon.skill.temp_guard_blocked", formatNumber(remainingAmount / 2.0)).formatted(Formatting.BLUE),
                    true
                )
                return 0.0f
            }
            player.sendMessage(Text.translatable("item.cresora.weapon.skill.temp_guard_broken").formatted(Formatting.BLUE), true)
            remainingAmount -= orchidState.stoneGuardHp
            orchidState.stoneGuardHp = 0.0f
        }
        val remainingGuard = temporaryGuardHpByPlayer[player.uuid] ?: 0.0f
        if (remainingGuard > 0.0f) {
            if (remainingAmount <= remainingGuard) {
                temporaryGuardHpByPlayer[player.uuid] = remainingGuard - remainingAmount
                if ((temporaryGuardHpByPlayer[player.uuid] ?: 0.0f) <= 0.0f) {
                    clearTemporaryGuard(player)
                }
                player.sendMessage(
                    Text.translatable("item.cresora.weapon.skill.temp_guard_blocked", formatNumber(remainingAmount / 2.0)).formatted(Formatting.BLUE),
                    true
                )
                return 0.0f
            }
            clearTemporaryGuard(player)
            player.sendMessage(Text.translatable("item.cresora.weapon.skill.temp_guard_broken").formatted(Formatting.BLUE), true)
            remainingAmount -= remainingGuard
        }
        val access = player as? WeaponSkillAccess ?: return remainingAmount
        val remainingShield = access.cresoraGetShieldHp()
        if (remainingShield <= 0.0f) {
            return remainingAmount
        }
        if (remainingAmount <= remainingShield) {
            access.cresoraSetShieldHp(remainingShield - remainingAmount)
            if (access.cresoraGetShieldHp() <= 0.0f) {
                clearShield(player)
            }
            player.sendMessage(
                Text.translatable("item.cresora.weapon.skill.blocked", formatNumber(remainingAmount / 2.0)).formatted(Formatting.AQUA),
                true
            )
            return 0.0f
        }
        access.cresoraSetShieldHp(0.0f)
        clearShield(player)
        player.sendMessage(
            Text.translatable("item.cresora.weapon.skill.broken").formatted(Formatting.RED),
            true
        )
        return remainingAmount - remainingShield
    }

    fun clearExpiredTemporaryGuard(player: ServerPlayerEntity) {
        val expireTick = temporaryGuardExpireTickByPlayer[player.uuid] ?: return
        if ((temporaryGuardHpByPlayer[player.uuid] ?: 0.0f) <= 0.0f) {
            clearTemporaryGuard(player)
            return
        }
        if (currentWorldTime(player) >= expireTick) {
            clearTemporaryGuard(player)
            player.sendMessage(Text.translatable("item.cresora.weapon.skill.temp_guard_expired").formatted(Formatting.BLUE), true)
        }
    }

    fun clearExpiredSnowMist(player: ServerPlayerEntity) {
        val state = snowMistStatesByPlayer[player.uuid] ?: return
        if (currentWorldTime(player) < state.expireTick) {
            return
        }
        snowMistStatesByPlayer.remove(player.uuid)
        player.sendMessage(Text.translatable("item.cresora.weapon.skill.snow_mist_expired").formatted(Formatting.GRAY), true)
    }

    fun clearExpiredShield(player: ServerPlayerEntity) {
        val access = player as? WeaponSkillAccess ?: return
        if (access.cresoraGetShieldHp() <= 0.0f) {
            clearShield(player)
            return
        }
        if (currentWorldTime(player) >= access.cresoraGetShieldExpireTick()) {
            clearShield(player)
            player.sendMessage(Text.translatable("item.cresora.weapon.skill.expired").formatted(Formatting.GRAY), true)
        }
    }

    fun isCoolingDown(player: ServerPlayerEntity, weaponId: String): Boolean {
        val expireTick = cooldownsByPlayer[player.uuid]?.get(weaponId) ?: return false
        return currentWorldTime(player) < expireTick
    }

    fun critDamageBonusPercent(player: ServerPlayerEntity, weaponId: String?): Double {
        var bonus = 0.0
        if (weaponId == HANWU_JUANXUE_ID) {
            clearExpiredSnowMist(player)
            val state = snowMistStatesByPlayer[player.uuid]
            if (state != null) {
                bonus += state.stackCount.coerceIn(0, HANWU_MAX_STACKS) * HANWU_CRIT_DMG_PER_STACK_PERCENT
            }
        }
        val orchidState = activeOrchidPavilionState(player)
        if (orchidState != null) {
            bonus += orchidState.raiseACupStacks * KYOKUSUI_CRIT_DMG_PER_STACK_PERCENT
        }
        return bonus
    }

    fun snowEnvironmentAttackScalar(player: ServerPlayerEntity, weaponId: String?): Double {
        if (weaponId != HANWU_JUANXUE_ID) {
            return 0.0
        }
        return if (isSnowEnvironment(player)) HANWU_SNOW_ATTACK_SCALAR else 0.0
    }

    fun orchidPavilionAttackScalar(player: ServerPlayerEntity): Double {
        val state = activeOrchidPavilionState(player) ?: return 0.0
        return state.raiseACupStacks * KYOKUSUI_ATTACK_PER_STACK
    }

    fun orchidPavilionArmorScalar(player: ServerPlayerEntity): Double {
        val state = activeOrchidPavilionState(player) ?: return 0.0
        return state.recitePoetryStacks * KYOKUSUI_ARMOR_PER_STACK
    }

    fun orchidPavilionRegenStageBonus(player: ServerPlayerEntity): Int {
        val state = activeOrchidPavilionState(player) ?: return 0
        return state.recitePoetryStacks * KYOKUSUI_REGEN_STAGE_PER_STACK
    }

    fun onAttackDealt(player: ServerPlayerEntity, target: LivingEntity, damage: Double) {
        if (damage <= 0.0) {
            return
        }
        val definition = WeaponStackSupport.getDefinition(player.mainHandStack) ?: return
        val world = player.world as? ServerWorld ?: return
        when (definition.id) {
            DARK_LUX_ID -> {
                applyDark(world, target, player)
            }

            KYOKUSUI_NO_RYUSHO_ID -> {
                val state = activeOrchidPavilionState(player) ?: return
                if (!target.isAlive) {
                    return
                }
                val piercingDamage = state.zhiStacks * KYOKUSUI_ZHI_TRUE_DAMAGE
                if (piercingDamage > 0.0f) {
                    applyPiercingDamage(player, target, piercingDamage)
                }
            }

            HANWU_JUANXUE_ID -> {
                val data = WeaponStackSupport.getWeaponData(player.mainHandStack) ?: return
                val state = snowMistStatesByPlayer[player.uuid] ?: return
                val now = currentWorldTime(player)
                if (now >= state.expireTick) {
                    clearExpiredSnowMist(player)
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
        }
    }

    @JvmStatic
    fun getPhysicalResistanceOffset(target: LivingEntity): Double {
        val state = mobStatusStatesByTarget[target.uuid] ?: return 0.0
        val now = target.world.time
        if (now < state.entanglementExpireTick) return ENTANGLEMENT_RESISTANCE_REDUCTION
        if (now < state.darkExpireTick) return DARK_LUX_RESISTANCE_REDUCTION
        return 0.0
    }

    @JvmStatic
    fun getArcaneResistanceOffset(target: LivingEntity): Double {
        val state = mobStatusStatesByTarget[target.uuid] ?: return 0.0
        val now = target.world.time
        if (now < state.entanglementExpireTick) return ENTANGLEMENT_RESISTANCE_REDUCTION
        if (now < state.luxExpireTick) return DARK_LUX_RESISTANCE_REDUCTION
        return 0.0
    }

    @JvmStatic
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

    private fun applyDark(world: ServerWorld, target: LivingEntity, player: ServerPlayerEntity?) {
        val now = world.time
        val state = mobStatusStatesByTarget.getOrPut(target.uuid) { MobStatusState() }
        if (now < state.entanglementExpireTick) return
        state.darkExpireTick = now + DARK_DURATION_TICKS
        handleInteraction(world, target, state, player)
    }

    private fun applyLux(world: ServerWorld, target: LivingEntity, player: ServerPlayerEntity?) {
        val now = world.time
        val state = mobStatusStatesByTarget.getOrPut(target.uuid) { MobStatusState() }
        if (now < state.entanglementExpireTick) return
        state.luxExpireTick = now + LUX_DURATION_TICKS
        handleInteraction(world, target, state, player)
    }

    private fun handleInteraction(world: ServerWorld, target: LivingEntity, state: MobStatusState, player: ServerPlayerEntity?) {
        val now = world.time
        if (now < state.darkExpireTick && now < state.luxExpireTick) {
            val players = world.players.filter { it.mainHandStack.copy().let { s -> WeaponStackSupport.getDefinition(s)?.id == DARK_LUX_ID } }
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
        target.damage(world, world.damageSources.magic(), damage) // Using magic for 90% loss
        // AdventureRankService.showMobTrueDamage(target, null, damage) // Cannot pass null to non-nullable
        
        val nearby = world.getOtherEntities(target, target.boundingBox.expand(5.0)) { it is LivingEntity && it.isAlive }
            .take(3)
        for (e in nearby) {
            if (e is LivingEntity) {
                val aoeDamage = e.maxHealth * 0.2f
                e.damage(world, world.damageSources.magic(), aoeDamage)
                // AdventureRankService.showMobTrueDamage(e, null, aoeDamage)
            }
        }
        (target as? HostileEntity)?.let { AdventureRankService.refreshMobDisplay(it) }
    }

    private fun triggerEntanglement(world: ServerWorld, target: LivingEntity, state: MobStatusState, player: ServerPlayerEntity?) {
        player?.sendMessage(Text.translatable("message.cresora.weapon.dark_lux.entanglement").formatted(Formatting.GOLD), true)
        state.darkExpireTick = 0
        state.luxExpireTick = 0
        state.entanglementExpireTick = world.time + ENTANGLEMENT_DURATION_TICKS
        (target as? HostileEntity)?.let { AdventureRankService.refreshMobDisplay(it) }
    }

    private fun triggerDarkCollapse(world: ServerWorld, target: LivingEntity, state: MobStatusState, player: ServerPlayerEntity?) {
        if (player != null) {
            player.sendMessage(Text.translatable("message.cresora.weapon.dark_lux.collapse").formatted(Formatting.DARK_PURPLE), true)
            player.heal(player.maxHealth * 0.05f)
            val hpRatio = player.health / player.maxHealth
            val reductionRatio = (1.0f - hpRatio).coerceIn(0.0f, 1.0f)
            
            val cooldowns = cooldownsByPlayer[player.uuid] ?: return
            val expireTick = cooldowns[DARK_LUX_ID] ?: return
            val now = world.time
            val remainingTicks = expireTick - now
            if (remainingTicks > 0) {
                val newRemaining = (remainingTicks * (1.0f - reductionRatio)).toLong()
                cooldowns[DARK_LUX_ID] = now + newRemaining
            }
        }
    }

    private fun activateDarkLux(
        player: ServerPlayerEntity,
        definition: WeaponDefinition,
        data: WeaponData,
        access: WeaponSkillAccess
    ): ActionResult {
        clearShield(player)
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

    private fun activateSunlitHaste(
        player: ServerPlayerEntity,
        definition: WeaponDefinition,
        data: WeaponData,
        access: WeaponSkillAccess
    ): ActionResult {
        clearShield(player)
        access.cresoraSetShieldExpireTick(0L)
        access.cresoraSetShieldWeaponId(null)
        val now = currentWorldTime(player)
        val expireTick = now + definition.skill.durationSeconds.coerceAtLeast(1) * 20L
        val normalPercent = WeaponCombatSupport.skillValuePercent(definition, data).coerceAtLeast(0.0)
        val sunlightPercent = WeaponCombatSupport.secondarySkillValuePercent(definition, data).coerceAtLeast(normalPercent)
        val state = SunlitHasteState(
            weaponId = definition.id,
            expireTick = expireTick,
            normalAmplifier = speedAmplifier(normalPercent),
            sunlightAmplifier = speedAmplifier(sunlightPercent)
        )
        sunlitHasteStatesByPlayer[player.uuid] = state
        refreshSunlitHasteEffect(player, state)
        player.sendMessage(
            Text.translatable(
                "item.cresora.weapon.skill.sunlit_haste_activated",
                Text.translatable(definition.translationKey()),
                definition.skill.durationSeconds,
                formatNumber(normalPercent),
                formatNumber(sunlightPercent)
            ).formatted(Formatting.YELLOW),
            true
        )
        return ActionResult.SUCCESS
    }

    private fun updateCooldownFeedback(player: ServerPlayerEntity) {
        val cooldowns = cooldownsByPlayer[player.uuid]
        if (cooldowns.isNullOrEmpty()) {
            removeCooldownBars(player)
            return
        }

        val now = currentWorldTime(player)
        val iterator = cooldowns.entries.iterator()
        while (iterator.hasNext()) {
            val (weaponId, cooldownExpireTick) = iterator.next()
            if (now >= cooldownExpireTick) {
                iterator.remove()
                removeCooldownBar(player, weaponId)
                continue
            }
            val definition = runCatching { WeaponContentRegistry.requireWeapon(weaponId) }.getOrNull()
            if (definition == null) {
                iterator.remove()
                removeCooldownBar(player, weaponId)
                continue
            }
            val totalTicks = definition.skill.cooldownSeconds * 20L
            val remainingTicks = (cooldownExpireTick - now).coerceAtLeast(0L)
            val progress = (1.0f - remainingTicks.toFloat() / totalTicks.toFloat()).coerceIn(0.0f, 1.0f)
            val playerBars = cooldownBars.getOrPut(player.uuid) { linkedMapOf() }
            val bossBar = playerBars.getOrPut(weaponId) {
                ServerBossBar(Text.empty(), BossBar.Color.BLUE, BossBar.Style.NOTCHED_10).apply {
                    addPlayer(player)
                }
            }
            bossBar.name = Text.translatable(
                "item.cresora.weapon.skill.cooldown_progress",
                Text.translatable(definition.translationKey()),
                formatNumber(remainingTicks / 20.0)
            )
            bossBar.percent = progress
            bossBar.color = BossBar.Color.BLUE
            bossBar.style = BossBar.Style.NOTCHED_10
            if (!bossBar.players.contains(player)) {
                bossBar.addPlayer(player)
            }
        }
        if (cooldowns.isEmpty()) {
            cooldownsByPlayer.remove(player.uuid)
            removeCooldownBars(player)
        }
    }

    private fun showCooldownBar(player: ServerPlayerEntity, definition: WeaponDefinition) {
        val playerBars = cooldownBars.getOrPut(player.uuid) { linkedMapOf() }
        val bossBar = playerBars.getOrPut(definition.id) { ServerBossBar(Text.empty(), BossBar.Color.BLUE, BossBar.Style.NOTCHED_10) }
        bossBar.name = Text.translatable(
            "item.cresora.weapon.skill.cooldown_progress",
            Text.translatable(definition.translationKey()),
            formatNumber(definition.skill.cooldownSeconds.toDouble())
        )
        bossBar.percent = 0.0f
        if (!bossBar.players.contains(player)) {
            bossBar.addPlayer(player)
        }
    }

    private fun startCooldown(player: ServerPlayerEntity, weaponId: String, expireTick: Long) {
        cooldownsByPlayer.getOrPut(player.uuid) { linkedMapOf() }[weaponId] = expireTick
    }

    private fun removeCooldownBar(player: ServerPlayerEntity, weaponId: String) {
        val playerBars = cooldownBars[player.uuid] ?: return
        playerBars.remove(weaponId)?.removePlayer(player)
        if (playerBars.isEmpty()) {
            cooldownBars.remove(player.uuid)
        }
    }

    private fun removeCooldownBars(player: ServerPlayerEntity) {
        cooldownBars.remove(player.uuid)?.values?.forEach { it.removePlayer(player) }
    }

    private fun pruneOfflineState(onlinePlayerIds: Set<UUID>) {
        val offlinePlayers = cooldownBars.keys.filterNot(onlinePlayerIds::contains)
        for (playerId in offlinePlayers) {
            cooldownBars.remove(playerId)?.values?.forEach { bossBar ->
                bossBar.players.toList().forEach(bossBar::removePlayer)
            }
        }
        cooldownsByPlayer.keys.removeIf { !onlinePlayerIds.contains(it) }
        temporaryGuardHpByPlayer.keys.removeIf { !onlinePlayerIds.contains(it) }
        temporaryGuardExpireTickByPlayer.keys.removeIf { !onlinePlayerIds.contains(it) }
        sunlitHasteStatesByPlayer.keys.removeIf { !onlinePlayerIds.contains(it) }
        boyaStatesByPlayer.keys.removeIf { !onlinePlayerIds.contains(it) }
        snowMistStatesByPlayer.keys.removeIf { !onlinePlayerIds.contains(it) }
        orchidPavilionStatesByPlayer.keys.removeIf { !onlinePlayerIds.contains(it) }
        // Prune entries for entities that are no longer loaded or alive
        // This is a bit tricky with UUIDs alone, but we can do it periodically or via events.
    }

    private fun activateShield(
        player: ServerPlayerEntity,
        definition: WeaponDefinition,
        data: WeaponData,
        access: WeaponSkillAccess
    ): ActionResult {
        access.cresoraSetShieldHp(WeaponCombatSupport.shieldHp(definition, data))
        access.cresoraSetShieldExpireTick(currentWorldTime(player) + definition.skill.durationSeconds * 20L)
        access.cresoraSetShieldWeaponId(definition.id)
        player.sendMessage(
            Text.translatable(
                "item.cresora.weapon.skill.activated",
                Text.translatable(definition.translationKey()),
                formatNumber(WeaponCombatSupport.shieldHearts(definition, data))
            ).formatted(Formatting.AQUA),
            true
        )
        return ActionResult.SUCCESS
    }

    private fun activateHeal(
        player: ServerPlayerEntity,
        definition: WeaponDefinition,
        data: WeaponData,
        access: WeaponSkillAccess
    ): ActionResult {
        startCooldown(player, definition.id, currentWorldTime(player) + definition.skill.cooldownSeconds * 20L)
        showCooldownBar(player, definition)
        clearShield(player)
        access.cresoraSetShieldExpireTick(0L)
        access.cresoraSetShieldWeaponId(null)
        val before = player.health
        player.heal(WeaponCombatSupport.healHp(definition, data))
        val healedHearts = ((player.health - before).coerceAtLeast(0.0f) / 2.0f).toDouble()
        player.sendMessage(
            Text.translatable(
                "item.cresora.weapon.skill.heal_activated",
                Text.translatable(definition.translationKey()),
                formatNumber(healedHearts)
            ).formatted(Formatting.GREEN),
            true
        )
        return ActionResult.SUCCESS
    }

    private fun activateHealingAura(
        player: ServerPlayerEntity,
        definition: WeaponDefinition,
        data: WeaponData,
        access: WeaponSkillAccess
    ): ActionResult {
        clearShield(player)
        access.cresoraSetShieldExpireTick(0L)
        access.cresoraSetShieldWeaponId(null)
        val world = player.world as? ServerWorld ?: return ActionResult.FAIL
        val now = currentWorldTime(player)
        val auraExpireTick = now + definition.skill.durationSeconds.coerceAtLeast(1) * 20L
        val immediateHealHp = WeaponCombatSupport.skillValueHp(definition, data)
        val pulseHealHp = WeaponCombatSupport.secondarySkillValueHp(definition, data).coerceAtLeast(0.0f)
        val recipients = linkedSetOf(player)
        val radius = definition.skill.radiusMeters.coerceAtLeast(0.0)
        if (radius > 0.0) {
            world.players
                .filterIsInstance<ServerPlayerEntity>()
                .filter { it.isAlive && !it.isSpectator && it.squaredDistanceTo(player) <= radius * radius }
                .forEach(recipients::add)
        }
        val (healedHp, overflowHp) = restoreHealthOrGuard(player, immediateHealHp, auraExpireTick)
        for (recipient in recipients) {
            applyBoya(recipient, definition, pulseHealHp, auraExpireTick, now)
        }
        player.sendMessage(
            Text.translatable(
                "item.cresora.weapon.skill.healing_aura_activated",
                Text.translatable(definition.translationKey()),
                formatNumber(healedHp / 2.0),
                formatNumber(overflowHp / 2.0),
                recipients.size,
                formatNumber(pulseHealHp / 2.0),
                definition.skill.durationSeconds
            ).formatted(Formatting.GREEN),
            true
        )
        return ActionResult.SUCCESS
    }

    private fun activateSnowFrost(
        player: ServerPlayerEntity,
        definition: WeaponDefinition,
        data: WeaponData,
        access: WeaponSkillAccess
    ): ActionResult {
        clearShield(player)
        access.cresoraSetShieldExpireTick(0L)
        access.cresoraSetShieldWeaponId(null)
        val now = currentWorldTime(player)
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

    private fun activateCurrentHpBurst(
        player: ServerPlayerEntity,
        definition: WeaponDefinition,
        data: WeaponData,
        access: WeaponSkillAccess
    ): ActionResult {
        clearShield(player)
        access.cresoraSetShieldExpireTick(0L)
        access.cresoraSetShieldWeaponId(null)
        val radius = definition.skill.radiusMeters.coerceAtLeast(0.0)
        val ratio = WeaponCombatSupport.currentHpTrueDamageRatio(definition, data)
        if (radius <= 0.0 || ratio <= 0.0) {
            player.sendMessage(Text.translatable("item.cresora.weapon.skill.none").formatted(Formatting.GRAY), true)
            return ActionResult.SUCCESS
        }
        val world = player.world as? ServerWorld ?: return ActionResult.FAIL
        val targets = world.getOtherEntities(player, player.boundingBox.expand(radius)) { entity ->
            entity is HostileEntity && entity.isAlive
        }.mapNotNull { it as? HostileEntity }
        var hitCount = 0
        var totalDamage = 0.0
        for (target in targets) {
            if (!StoryService.allowsTrueDamage(target)) {
                continue
            }
            val damage = (target.health.toDouble() * ratio).coerceAtLeast(0.0)
            if (damage <= 0.0) {
                continue
            }
            target.health = (target.health.toDouble() - damage).coerceAtLeast(0.001).toFloat()
            AdventureRankService.showMobTrueDamage(target, player, damage.toFloat())
            AdventureRankService.refreshMobDisplay(target)
            hitCount++
            totalDamage += damage
        }
        player.sendMessage(
            Text.translatable(
                "item.cresora.weapon.skill.current_hp_true_damage_activated",
                Text.translatable(definition.translationKey()),
                hitCount,
                formatNumber(totalDamage / 2.0)
            ).formatted(Formatting.AQUA),
            true
        )
        return ActionResult.SUCCESS
    }

    private fun activateFlameAura(
        player: ServerPlayerEntity,
        definition: WeaponDefinition,
        data: WeaponData,
        access: WeaponSkillAccess
    ): ActionResult {
        clearShield(player)
        access.cresoraSetShieldExpireTick(0L)
        access.cresoraSetShieldWeaponId(null)
        val world = player.world as? ServerWorld ?: return ActionResult.FAIL
        val radius = definition.skill.radiusMeters.coerceAtLeast(0.0)
        val burnSeconds = WeaponCombatSupport.skillValuePercent(definition, data).coerceAtLeast(0.0)
        val burnTicks = (burnSeconds * 20.0).toInt().coerceAtLeast(20)
        val resistanceSeconds = definition.skill.durationSeconds.coerceAtLeast(1)
        player.addStatusEffect(StatusEffectInstance(StatusEffects.FIRE_RESISTANCE, resistanceSeconds * 20, 0, false, true, true))
        val targets = world.getOtherEntities(player, player.boundingBox.expand(radius)) { entity ->
            entity is MobEntity && entity.isAlive
        }.mapNotNull { it as? MobEntity }
        var ignitedCount = 0
        for (target in targets) {
            target.setOnFireForTicks(burnTicks)
            ignitedCount++
        }
        player.sendMessage(
            Text.translatable(
                "item.cresora.weapon.skill.flame_aura_activated",
                Text.translatable(definition.translationKey()),
                ignitedCount,
                formatNumber(burnSeconds),
                resistanceSeconds
            ).formatted(Formatting.GOLD),
            true
        )
        return ActionResult.SUCCESS
    }

    private fun tickBoya(player: ServerPlayerEntity) {
        val state = boyaStatesByPlayer[player.uuid] ?: return
        val now = currentWorldTime(player)
        if (now >= state.expireTick || !player.isAlive) {
            boyaStatesByPlayer.remove(player.uuid)
            return
        }
        while (now >= state.nextPulseTick && state.nextPulseTick < state.expireTick) {
            restoreHealthOrGuard(player, state.pulseHealHp, state.expireTick)
            knockbackNearbyHostiles(player, state.knockbackRadius)
            state.nextPulseTick += state.intervalTicks
        }
    }

    private fun tickSunlitHaste(player: ServerPlayerEntity) {
        val state = sunlitHasteStatesByPlayer[player.uuid] ?: return
        val now = currentWorldTime(player)
        if (now >= state.expireTick || !player.isAlive) {
            sunlitHasteStatesByPlayer.remove(player.uuid)
            return
        }
        refreshSunlitHasteEffect(player, state)
    }

    private fun tickOrchidPavilion(player: ServerPlayerEntity) {
        val state = orchidPavilionStatesByPlayer[player.uuid] ?: return
        val now = currentWorldTime(player)
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

    private fun tickFrost(server: MinecraftServer) {
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

    private fun isSnowEnvironment(player: ServerPlayerEntity): Boolean {
        val world = player.world as? ServerWorld ?: return false
        val pos = player.blockPos
        val biome = world.getBiome(pos).value()
        if (biome.getPrecipitation(pos, world.seaLevel) == Biome.Precipitation.SNOW || biome.isCold(pos, world.seaLevel)) {
            return true
        }
        val checkPositions = arrayOf(pos, pos.down(), pos.up())
        for (checkPos in checkPositions) {
            val block = world.getBlockState(checkPos).block
            if (
                block == Blocks.SNOW ||
                block == Blocks.SNOW_BLOCK ||
                block == Blocks.POWDER_SNOW ||
                block == Blocks.ICE ||
                block == Blocks.PACKED_ICE ||
                block == Blocks.BLUE_ICE
            ) {
                return true
            }
        }
        return false
    }

    private fun isSunlitEnvironment(player: ServerPlayerEntity): Boolean {
        val world = player.world as? ServerWorld ?: return false
        if (!world.dimension.hasSkyLight() || !world.isDay || world.isRaining || world.isThundering) {
            return false
        }
        return world.isSkyVisible(player.blockPos.up())
    }

    private fun refreshSunlitHasteEffect(player: ServerPlayerEntity, state: SunlitHasteState) {
        val amplifier = if (isSunlitEnvironment(player)) state.sunlightAmplifier else state.normalAmplifier
        val current = player.getStatusEffect(StatusEffects.SPEED)
        if (current != null && current.amplifier == amplifier && current.duration > 10) {
            return
        }
        player.addStatusEffect(StatusEffectInstance(StatusEffects.SPEED, 30, amplifier, false, false, true))
    }

    private fun speedAmplifier(percent: Double): Int {
        return kotlin.math.ceil(percent.coerceAtLeast(0.0) / 20.0).toInt().coerceAtLeast(1) - 1
    }

    private fun applyBoya(
        player: ServerPlayerEntity,
        definition: WeaponDefinition,
        pulseHealHp: Float,
        expireTick: Long,
        now: Long
    ) {
        val intervalTicks = (definition.skill.tickIntervalSeconds.coerceAtLeast(0.5) * 20.0).toLong().coerceAtLeast(1L)
        boyaStatesByPlayer[player.uuid] = BoyaState(
            weaponId = definition.id,
            expireTick = expireTick,
            nextPulseTick = now + intervalTicks,
            intervalTicks = intervalTicks,
            pulseHealHp = pulseHealHp,
            knockbackRadius = definition.skill.radiusMeters.coerceAtLeast(0.0)
        )
    }

    private fun activateOrchidPavilionEcho(
        player: ServerPlayerEntity,
        definition: WeaponDefinition,
        data: WeaponData,
        access: WeaponSkillAccess
    ): ActionResult {
        clearShield(player)
        access.cresoraSetShieldExpireTick(0L)
        access.cresoraSetShieldWeaponId(null)
        val now = currentWorldTime(player)
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
                        formatNumber(state.raiseACupStacks * KYOKUSUI_ATTACK_PER_STACK * 100.0),
                        formatNumber(state.raiseACupStacks * KYOKUSUI_CRIT_DMG_PER_STACK_PERCENT),
                        state.zhiStacks,
                        formatNumber(state.zhiStacks * KYOKUSUI_ZHI_TRUE_DAMAGE / 2.0)
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
                        formatNumber(state.recitePoetryStacks * KYOKUSUI_ARMOR_PER_STACK * 100.0),
                        state.recitePoetryStacks * KYOKUSUI_REGEN_STAGE_PER_STACK
                    ).formatted(Formatting.BLUE),
                    true
                )
            }

            OrchidPavilionEffect.PLACE_A_STONE -> {
                state.placeAStoneStacks += 1
                state.stoneGuardHp += KYOKUSUI_STONE_GUARD_HP
                player.sendMessage(
                    Text.translatable(
                        "item.cresora.weapon.skill.orchid_pavilion_echo.place_a_stone",
                        state.placeAStoneStacks,
                        formatNumber(state.stoneGuardHp / 2.0)
                    ).formatted(Formatting.AQUA),
                    true
                )
            }

            OrchidPavilionEffect.INK_BRUSH -> {
                state.invulnerableExpireTicks += currentWorldTime(player) + KYOKUSUI_INK_INVULN_TICKS
                val healedTargets = healNearbyAllies(player, 5.0, KYOKUSUI_INK_HEAL_HP)
                player.sendMessage(
                    Text.translatable(
                        "item.cresora.weapon.skill.orchid_pavilion_echo.ink_brush",
                        healedTargets,
                        formatNumber(KYOKUSUI_INK_HEAL_HP / 2.0),
                        formatNumber(totalOrchidInvulnerableSeconds(player))
                    ).formatted(Formatting.LIGHT_PURPLE),
                    true
                )
            }
        }
    }

    private fun healNearbyAllies(player: ServerPlayerEntity, radius: Double, amountHp: Float): Int {
        val world = player.world as? ServerWorld ?: return 1
        val recipients = linkedSetOf(player)
        world.players
            .filterIsInstance<ServerPlayerEntity>()
            .filter { it.isAlive && !it.isSpectator && it.squaredDistanceTo(player) <= radius * radius }
            .forEach(recipients::add)
        for (recipient in recipients) {
            recipient.heal(amountHp)
        }
        return recipients.size
    }

    private fun knockbackNearbyHostiles(player: ServerPlayerEntity, radius: Double) {
        if (radius <= 0.0) {
            return
        }
        val world = player.world as? ServerWorld ?: return
        val targets = world.getOtherEntities(player, player.boundingBox.expand(radius)) { entity ->
            entity is HostileEntity && entity.isAlive
        }.mapNotNull { it as? HostileEntity }
        for (target in targets) {
            target.takeKnockback(1.15, player.x - target.x, player.z - target.z)
        }
    }

    private fun restoreHealthOrGuard(player: ServerPlayerEntity, amountHp: Float, expireTick: Long): Pair<Float, Float> {
        if (amountHp <= 0.0f) {
            return 0.0f to 0.0f
        }
        val before = player.health
        player.heal(amountHp)
        val healedHp = (player.health - before).coerceAtLeast(0.0f)
        val overflowHp = (amountHp - healedHp).coerceAtLeast(0.0f)
        if (overflowHp > 0.0f) {
            addTemporaryGuard(player, overflowHp, expireTick)
        }
        return healedHp to overflowHp
    }

    private fun addTemporaryGuard(player: ServerPlayerEntity, amountHp: Float, expireTick: Long) {
        if (amountHp <= 0.0f) {
            return
        }
        temporaryGuardHpByPlayer[player.uuid] = (temporaryGuardHpByPlayer[player.uuid] ?: 0.0f) + amountHp
        temporaryGuardExpireTickByPlayer[player.uuid] = maxOf(temporaryGuardExpireTickByPlayer[player.uuid] ?: 0L, expireTick)
    }

    private fun clearTemporaryGuard(player: ServerPlayerEntity) {
        temporaryGuardHpByPlayer.remove(player.uuid)
        temporaryGuardExpireTickByPlayer.remove(player.uuid)
    }

    private fun clearShield(player: ServerPlayerEntity) {
        val access = player as? WeaponSkillAccess ?: return
        access.cresoraSetShieldHp(0.0f)
        access.cresoraSetShieldExpireTick(0L)
        access.cresoraSetShieldWeaponId(null)
    }

    private fun currentWorldTime(player: ServerPlayerEntity): Long {
        return (player.world as? ServerWorld)?.time ?: 0L
    }

    fun hasWeaponInInventory(player: ServerPlayerEntity, weaponId: String): Boolean {
        for (i in 0 until player.inventory.size()) {
            val stack = player.inventory.getStack(i)
            val def = WeaponStackSupport.getDefinition(stack)
            if (def?.id == weaponId) return true
        }
        return false
    }

    private fun activateBaaMimic(
        player: ServerPlayerEntity,
        definition: WeaponDefinition,
        data: WeaponData,
        access: WeaponSkillAccess
    ): ActionResult {
        val world = player.world as? ServerWorld ?: return ActionResult.FAIL
        val radius = definition.skill.radiusMeters.coerceAtLeast(0.0)
        
        val n = when {
            data.baseLevel >= 61 -> 3
            data.baseLevel >= 41 -> 2
            else -> 1
        }

        val maxPlayerHp = world.players
            .filter { it.squaredDistanceTo(player.x, player.y, player.z) < 100.0 }
            .maxOfOrNull { it.maxHealth } ?: 20.0f

        val targets = world.getOtherEntities(player, player.boundingBox.expand(radius)) { entity ->
            val hostile = entity as? HostileEntity ?: return@getOtherEntities false
            if (!hostile.isAlive) {
                return@getOtherEntities false
            }
            val access = hostile as? AdventureRankMobAccess
            access?.cresoraIsEliteMob() != true
        }
            .mapNotNull { it as? HostileEntity }
            .sortedBy { it.squaredDistanceTo(player) }
            .take(n)

        for (target in targets) {
            val sheep = EntityType.SHEEP.spawn(world, null, target.blockPos, net.minecraft.entity.SpawnReason.COMMAND, true, false) ?: continue
            sheep.refreshPositionAndAngles(target.x, target.y, target.z, target.yaw, target.pitch)
            BaaMimicService.markTransformedSheep(sheep, target)
            val maxHealthAttr = sheep.getAttributeInstance(EntityAttributes.MAX_HEALTH)
            if (maxHealthAttr != null) {
                maxHealthAttr.baseValue = maxPlayerHp.toDouble()
                sheep.health = maxPlayerHp
            }
            AdventureRankService.refreshMobDisplay(sheep)
            target.discard()
        }

        player.sendMessage(
            Text.translatable(
                "item.cresora.weapon.skill.baa_mimic_activated",
                Text.translatable(definition.translationKey()),
                targets.size
            ).formatted(Formatting.GREEN),
            true
        )
        return ActionResult.SUCCESS
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
        val now = currentWorldTime(player)
        return if (now < state.expireTick && player.isAlive) state else null
    }

    private fun pruneExpiredOrchidInvulnerability(player: ServerPlayerEntity) {
        val state = orchidPavilionStatesByPlayer[player.uuid] ?: return
        val now = currentWorldTime(player)
        state.invulnerableExpireTicks.removeIf { it <= now }
    }

    private fun totalOrchidInvulnerableSeconds(player: ServerPlayerEntity): Double {
        val state = orchidPavilionStatesByPlayer[player.uuid] ?: return 0.0
        pruneExpiredOrchidInvulnerability(player)
        val now = currentWorldTime(player)
        return state.invulnerableExpireTicks.sumOf { (it - now).coerceAtLeast(0L).toDouble() } / 20.0
    }

    private fun formatNumber(value: Double): String {
        val rounded = kotlin.math.round(value * 10.0) / 10.0
        return if (rounded % 1.0 == 0.0) rounded.toInt().toString() else rounded.toString()
    }
}
