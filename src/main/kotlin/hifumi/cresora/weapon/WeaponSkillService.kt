package hifumi.cresora.weapon

import hifumi.cresora.debuff.CresoraDebuffService
import hifumi.cresora.skill.WeaponSkillRegistry
import hifumi.cresora.skill.WeaponSkillHandler
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
    const val HANWU_JUANXUE_ID = "hanwu_juanxue"
    const val HANWU_CRIT_DMG_PER_STACK_PERCENT = 10.0
    const val HANWU_MAX_STACKS = 5
    const val HANWU_SNOW_ATTACK_SCALAR = 0.5
    const val HANWU_FROST_SLOWNESS_AMPLIFIER = 1

    const val KYOKUSUI_NO_RYUSHO_ID = "kyokusui_no_ryusho"
    const val KYOKUSUI_ATTACK_PER_STACK = 0.10
    const val KYOKUSUI_CRIT_DMG_PER_STACK_PERCENT = 15.0
    const val KYOKUSUI_ARMOR_PER_STACK = 0.20
    const val KYOKUSUI_REGEN_STAGE_PER_STACK = 2
    const val KYOKUSUI_STONE_GUARD_HP = 7.0f
    const val KYOKUSUI_ZHI_TRUE_DAMAGE = 2.0f
    const val KYOKUSUI_INK_HEAL_HP = 8.0f
    const val KYOKUSUI_INK_INVULN_TICKS = 2 * 20L

    const val DARK_LUX_ID = "dark_lux"
    const val DARK_DURATION_TICKS = 10 * 20L
    const val LUX_DURATION_TICKS = 30 * 20L
    const val ENTANGLEMENT_DURATION_TICKS = 10 * 20L
    const val DARK_LUX_RESISTANCE_REDUCTION = 0.20
    const val ENTANGLEMENT_RESISTANCE_REDUCTION = 0.50

    private val cooldownBars: MutableMap<UUID, MutableMap<String, ServerBossBar>> = mutableMapOf()

    @JvmStatic
    fun serializeCooldowns(cooldowns: Map<String, Double>): String {
        return cooldowns.entries.joinToString(",") { "${it.key}:${it.value}" }
    }

    @JvmStatic
    fun deserializeCooldowns(serialized: String): Map<String, Double> {
        val map = LinkedHashMap<String, Double>()
        if (serialized.isEmpty()) return map
        val parts = serialized.split(",")
        for (part in parts) {
            val subparts = part.split(":")
            if (subparts.size == 2) {
                val weaponId = subparts[0]
                val ticks = subparts[1].toDoubleOrNull()
                if (ticks != null && ticks > 0.0) {
                    map[weaponId] = ticks
                }
            }
        }
        return map
    }

    private val temporaryGuardHpByPlayer: MutableMap<UUID, Float> = mutableMapOf()
    private val temporaryGuardExpireTickByPlayer: MutableMap<UUID, Long> = mutableMapOf()
    private val targetMarks: MutableMap<UUID, MutableMap<String, Long>> = mutableMapOf()
    private val invulnerabilityTicks: MutableMap<UUID, Long> = mutableMapOf()
    private val lastHeldWeaponIdByPlayer: MutableMap<UUID, String> = mutableMapOf()

    // soul Break (破魂) stacks and expiry
    private val soulBreakStacks: MutableMap<UUID, Int> = mutableMapOf()
    private val soulBreakExpireTick: MutableMap<UUID, Long> = mutableMapOf()

    // Tao (道) stacks for Tanmoku Chokuu (does not expire)
    private val taoStacks: MutableMap<UUID, Int> = mutableMapOf()

    private val isProcessingDamage = ThreadLocal.withInitial { false }
    private val isDealingTrueDamage = ThreadLocal.withInitial { false }

    @JvmStatic
    fun isDealingTrueDamage(): Boolean = isDealingTrueDamage.get()

    fun init() {
        ServerTickEvents.END_SERVER_TICK.register { server ->
            val now = server.overworld.time
            val onlinePlayers = server.playerManager.playerList
            val onlinePlayerIds = onlinePlayers.mapTo(linkedSetOf(), ServerPlayerEntity::getUuid)
            pruneOfflinePlayerState(onlinePlayerIds)

            WeaponSkillRegistry.allHandlers().forEach { (id, handler) ->
                handler.pruneTransientState(onlinePlayerIds)
                val def = WeaponSkillRegistry.getDefinition(id)
                if (def != null) {
                    handler.onTick(server, def, WeaponData.DUMMY.copy(weaponId = def.id, rarity = def.craft.craftedRarity))
                }
            }

            for (player in onlinePlayers) {
                val activeContext = activeWeaponContext(player)
                val currentWeaponId = activeContext?.first?.id ?: ""
                val lastWeaponId = lastHeldWeaponIdByPlayer[player.uuid] ?: ""
                if (currentWeaponId != lastWeaponId) {
                    // Clear transient skill state of the swapped-out weapon
                    if (lastWeaponId.isNotEmpty()) {
                        val oldDef = runCatching { WeaponContentRegistry.requireWeapon(lastWeaponId) }.getOrNull()
                        if (oldDef != null) {
                            WeaponSkillRegistry.getHandler(oldDef.skill.effectId)?.clearTransientState(player.uuid)
                            for (subSkillId in WeaponSkillRegistry.subSkillEffectIds(oldDef.skill.effectId)) {
                                WeaponSkillRegistry.getHandler(subSkillId)?.clearTransientState(player.uuid)
                            }
                        }
                        // Reset service-level transient stats associated with the swapped-out weapon
                        if (lastWeaponId == "tanboku_chokuu") {
                            taoStacks.remove(player.uuid)
                        }
                        val access = player as? WeaponSkillAccess
                        if (access != null && access.cresoraGetShieldWeaponId() == lastWeaponId) {
                            clearShield(player)
                        }
                    }
                    if (currentWeaponId.isNotEmpty()) {
                        lastHeldWeaponIdByPlayer[player.uuid] = currentWeaponId
                    } else {
                        lastHeldWeaponIdByPlayer.remove(player.uuid)
                    }
                }

                if (activeContext != null) {
                    val (def, data) = activeContext
                    runWeaponHandlers(def.skill.effectId, def, data) { handler, definition, weaponData ->
                        handler.onPlayerTick(player, definition, weaponData)
                    }
                }

                tickCooldowns(player)
                clearExpiredTemporaryGuard(player)
                clearExpiredShield(player)
                updateCooldownFeedback(player)
            }
            pruneTargetStates(now)
        }
    }

    private fun pruneTargetStates(now: Long) {
        val markIterator = targetMarks.entries.iterator()
        while (markIterator.hasNext()) {
            val entry = markIterator.next()
            val marks = entry.value
            marks.entries.removeIf { it.value <= now }
            if (marks.isEmpty()) markIterator.remove()
        }
        invulnerabilityTicks.entries.removeIf { it.value <= now }

        val soulBreakIterator = soulBreakStacks.entries.iterator()
        while (soulBreakIterator.hasNext()) {
            val entry = soulBreakIterator.next()
            val expire = soulBreakExpireTick[entry.key] ?: 0L
            if (now >= expire) {
                soulBreakIterator.remove()
                soulBreakExpireTick.remove(entry.key)
            }
        }
    }

    @JvmStatic
    fun applyMark(target: LivingEntity, markId: String, durationTicks: Long) {
        val now = target.world.time
        targetMarks.getOrPut(target.uuid) { mutableMapOf() }[markId] = now + durationTicks
    }

    @JvmStatic
    fun hasMark(target: LivingEntity, markId: String): Boolean {
        val expire = targetMarks[target.uuid]?.get(markId) ?: return false
        return target.world.time < expire
    }

    fun removeMark(target: LivingEntity, markId: String) {
        targetMarks[target.uuid]?.remove(markId)
    }

    fun applySoulBreak(target: LivingEntity, stacks: Int = 1, durationTicks: Long) {
        val current = soulBreakStacks[target.uuid] ?: 0
        soulBreakStacks[target.uuid] = (current + stacks).coerceAtMost(8)
        val now = target.world.time
        soulBreakExpireTick[target.uuid] = now + durationTicks
    }

    fun getSoulBreakStacks(target: LivingEntity): Int {
        return if (target.world.time < (soulBreakExpireTick[target.uuid] ?: 0L)) soulBreakStacks[target.uuid] ?: 0 else 0
    }

    fun addTao(player: ServerPlayerEntity, amount: Int) {
        val current = taoStacks[player.uuid] ?: 0
        taoStacks[player.uuid] = (current + amount).coerceAtMost(99)
    }

    fun getTao(player: ServerPlayerEntity): Int {
        return taoStacks[player.uuid] ?: 0
    }

    fun consumeTao(player: ServerPlayerEntity, amount: Int): Boolean {
        val current = taoStacks[player.uuid] ?: 0
        if (current < amount) return false
        taoStacks[player.uuid] = current - amount
        return true
    }

    fun grantInvulnerability(target: LivingEntity, durationTicks: Long) {
        val now = target.world.time
        invulnerabilityTicks[target.uuid] = maxOf(invulnerabilityTicks[target.uuid] ?: 0L, now + durationTicks)
    }

    fun isInvulnerable(target: LivingEntity): Boolean {
        val expire = invulnerabilityTicks[target.uuid] ?: return false
        return target.world.time < expire
    }

    private fun tickCooldowns(player: ServerPlayerEntity) {
        val cooldowns = (player as? WeaponSkillAccess)?.cresoraGetCooldowns() ?: return
        if (cooldowns.isEmpty()) return
        val iterator = cooldowns.entries.iterator()
        while (iterator.hasNext()) {
            val entry = iterator.next()
            val remaining = entry.value - 1.0
            if (remaining <= 0.0) {
                iterator.remove()
                removeCooldownBar(player, entry.key)
            } else {
                entry.setValue(remaining)
            }
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
        if (definition.skill.effectId == "none") {
            player.sendMessage(Text.translatable("item.cresora.weapon.skill.none").formatted(Formatting.GRAY), true)
            return ActionResult.SUCCESS
        }
        val result = WeaponSkillRegistry.getHandler(definition.skill.effectId)?.activate(player, definition, data, access)
            ?: ActionResult.PASS
        if (result == ActionResult.SUCCESS) {
            CresoraDebuffService.triggerElementalSkill(player, definition.skill.note, definition.skill.radiusMeters)
        }
        return result
    }

    fun absorbDamage(player: ServerPlayerEntity, amount: Float): Float {
        clearExpiredTemporaryGuard(player)
        clearExpiredShield(player)

        var remainingAmount = amount
        val activeContext = activeWeaponContext(player) ?: return remainingAmount
        val (heldDef, heldData) = activeContext

        runWeaponHandlers(heldDef.skill.effectId, heldDef, heldData) { handler, def, data ->
            remainingAmount = handler.onDamageAbsorbed(player, remainingAmount, def, data)
        }
        if (remainingAmount <= 0.0f) return 0.0f

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

    fun onDamageTaken(player: ServerPlayerEntity, amount: Float): Float {
        val activeContext = activeWeaponContext(player) ?: return amount
        var modifiedAmount = amount
        val (definition, data) = activeContext
        runWeaponHandlers(definition.skill.effectId, definition, data) { handler, def, weaponData ->
            modifiedAmount = handler.onDamageTaken(player, modifiedAmount, def, weaponData)
        }
        return modifiedAmount
    }

    // When weaponId is present, scope to that weapon. Otherwise scope to the held main-hand weapon.
    // Enhanced active weapon checks ensure inactive weapon stats do not blend.
    fun critDamageBonusPercent(player: ServerPlayerEntity, weaponId: String?): Double {
        val activeContext = activeWeaponContext(player) ?: return 0.0
        if (!WeaponScopeSupport.isActiveWeaponScope(activeContext.first.id, weaponId)) {
            return 0.0
        }
        val definition = activeContext.first
        val bonus = runWeaponBonus(definition.skill.effectId, definition) { handler, _ -> handler.getCritDamageBonus(player) }
        return bonus
    }

    // When weaponId is present, scope to that weapon. Otherwise scope to the held main-hand weapon.
    // Enhanced active weapon checks ensure inactive weapon stats do not blend.
    fun critRateBonusPercent(player: ServerPlayerEntity, weaponId: String?): Double {
        val activeContext = activeWeaponContext(player) ?: return 0.0
        if (!WeaponScopeSupport.isActiveWeaponScope(activeContext.first.id, weaponId)) {
            return 0.0
        }
        val definition = activeContext.first
        return runWeaponBonus(definition.skill.effectId, definition) { handler, _ -> handler.getCritRateBonus(player) }
    }

    // Held-weapon scoped dynamic attack modifier. This does not aggregate passive bonuses from unequipped weapons.
    fun attackDamageScalar(player: ServerPlayerEntity): Double {
        val definition = activeWeaponContext(player)?.first ?: return 0.0
        var scalar = runWeaponBonus(definition.skill.effectId, definition) { handler, _ -> handler.getAttackDamageScalar(player) }
        if (definition.id == "hanwu_juanxue" && isSnowEnvironment(player)) {
            scalar += 0.5
        }
        if (hasMark(player, "nageki")) {
            scalar -= 0.5
        }
        return scalar
    }

    // Held-weapon scoped dynamic armor modifier. This does not aggregate passive bonuses from unequipped weapons.
    fun armorScalar(player: ServerPlayerEntity): Double {
        val definition = activeWeaponContext(player)?.first ?: return 0.0
        var scalar = runWeaponBonus(definition.skill.effectId, definition) { handler, _ -> handler.getArmorScalar(player) }
        if (hasMark(player, "nageki")) {
            scalar += 1.0
        }
        return scalar
    }

    fun healthScalar(player: ServerPlayerEntity): Double {
        val activeContext = activeWeaponContext(player) ?: return 0.0
        val definition = activeContext.first
        val weaponHpPercent = definition.hpBonusPercent
        val skillHpPercent = runWeaponBonus(definition.skill.effectId, definition) { handler, _ ->
            handler.getHealthBonusPercent(player)
        }
        return (weaponHpPercent + skillHpPercent) / 100.0
    }

    @JvmStatic
    fun allDamageBonusPercent(player: ServerPlayerEntity, weaponId: String?): Double {
        val activeContext = activeWeaponContext(player) ?: return 0.0
        if (!WeaponScopeSupport.isActiveWeaponScope(activeContext.first.id, weaponId)) {
            return 0.0
        }
        val definition = activeContext.first
        val bonus = runWeaponBonus(definition.skill.effectId, definition) { handler, _ ->
            handler.getAllDamageBonus(player)
        }
        return bonus
    }

    fun onAttackDealt(player: ServerPlayerEntity, target: LivingEntity, damage: Double) {
        if (damage <= 0.0 || isProcessingDamage.get()) return

        isProcessingDamage.set(true)
        try {
            val activeContext = activeWeaponContext(player) ?: return
            val (heldDef, heldData) = activeContext
            val isTrue = isDealingTrueDamage.get()
            runWeaponHandlers(heldDef.skill.effectId, heldDef, heldData) { handler, def, data ->
                handler.onDamageDealt(player, target, damage.toFloat(), isTrue, def, data)
            }
        } finally {
            isProcessingDamage.set(false)
        }
    }

    @JvmStatic
    fun dealTrueDamage(player: ServerPlayerEntity, target: LivingEntity, amount: Float) {
        if (amount <= 0.0f) return
        isDealingTrueDamage.set(true)
        try {
            target.damage(player.world as ServerWorld, player.damageSources.indirectMagic(player, player), amount)
        } finally {
            isDealingTrueDamage.set(false)
        }
    }

    // Held-weapon scoped regen stage bonus.
    fun regenStageBonus(player: ServerPlayerEntity): Int {
        val definition = activeWeaponContext(player)?.first ?: return 0
        var total = 0
        runWeaponHandlers(definition.skill.effectId, definition, WeaponData.DUMMY) { handler, _ , _ ->
            total += handler.getRegenStageBonus(player)
        }
        return total
    }

    @JvmStatic
    fun getPhysicalResistanceOffset(target: LivingEntity): Double {
        var offset = 0.0
        if (hasMark(target, "entanglement")) offset += ENTANGLEMENT_RESISTANCE_REDUCTION
        if (hasMark(target, "dark")) offset += DARK_LUX_RESISTANCE_REDUCTION
        val soulBreak = getSoulBreakStacks(target)
        offset += soulBreak * 0.05 // 5% per stack
        return offset
    }

    @JvmStatic
    fun getArcaneResistanceOffset(target: LivingEntity): Double {
        var offset = 0.0
        if (hasMark(target, "entanglement")) offset += ENTANGLEMENT_RESISTANCE_REDUCTION
        if (hasMark(target, "lux")) offset += DARK_LUX_RESISTANCE_REDUCTION
        return offset
    }

    @JvmStatic
    fun hasStatus(target: LivingEntity, status: String): Boolean {
        return hasMark(target, status)
    }

    fun isSnowEnvironment(player: ServerPlayerEntity): Boolean {
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

    fun formatNumber(value: Double): String {
        val rounded = kotlin.math.round(value * 10.0) / 10.0
        return if (rounded % 1.0 == 0.0) rounded.toInt().toString() else rounded.toString()
    }

    fun currentWorldTime(player: ServerPlayerEntity): Long {
        return (player.world as? ServerWorld)?.time ?: 0L
    }

    fun showCooldownBar(player: ServerPlayerEntity, definition: WeaponDefinition) {
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

    fun startCooldown(player: ServerPlayerEntity, weaponId: String, durationTicks: Long) {
        (player as? WeaponSkillAccess)?.cresoraGetCooldowns()?.put(weaponId, durationTicks.toDouble())
    }

    fun clearShield(player: ServerPlayerEntity) {
        val access = player as? WeaponSkillAccess ?: return
        access.cresoraSetShieldHp(0.0f)
        access.cresoraSetShieldExpireTick(0L)
        access.cresoraSetShieldWeaponId(null)
    }

    fun grantShield(player: ServerPlayerEntity, amountHp: Float, durationTicks: Long, weaponId: String? = null) {
        val access = player as? WeaponSkillAccess ?: return
        val resolvedWeaponId = weaponId ?: activeWeaponContext(player)?.first?.id
        access.cresoraSetShieldHp(amountHp)
        access.cresoraSetShieldExpireTick(currentWorldTime(player) + durationTicks)
        access.cresoraSetShieldWeaponId(resolvedWeaponId)
    }

    fun getRemainingCooldownTicks(player: ServerPlayerEntity, weaponId: String): Double {
        return (player as? WeaponSkillAccess)?.cresoraGetCooldowns()?.get(weaponId) ?: 0.0
    }

    fun isCoolingDown(player: ServerPlayerEntity, weaponId: String): Boolean {
        val remaining = (player as? WeaponSkillAccess)?.cresoraGetCooldowns()?.get(weaponId) ?: return false
        return remaining > 0.0
    }

    fun restoreHealthOrGuard(player: ServerPlayerEntity, amountHp: Float, expireTick: Long): Pair<Float, Float> {
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

    fun addTemporaryGuard(player: ServerPlayerEntity, amountHp: Float, expireTick: Long) {
        if (amountHp <= 0.0f) {
            return
        }
        temporaryGuardHpByPlayer[player.uuid] = (temporaryGuardHpByPlayer[player.uuid] ?: 0.0f) + amountHp
        temporaryGuardExpireTickByPlayer[player.uuid] = maxOf(temporaryGuardExpireTickByPlayer[player.uuid] ?: 0L, expireTick)
    }

    fun healNearbyAllies(player: ServerPlayerEntity, radius: Double, amountHp: Float): Int {
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

    fun clearTemporaryGuard(player: ServerPlayerEntity) {
        temporaryGuardHpByPlayer.remove(player.uuid)
        temporaryGuardExpireTickByPlayer.remove(player.uuid)
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

    private fun updateCooldownFeedback(player: ServerPlayerEntity) {
        val cooldowns = (player as? WeaponSkillAccess)?.cresoraGetCooldowns()
        if (cooldowns.isNullOrEmpty()) {
            removeCooldownBars(player)
            return
        }

        val iterator = cooldowns.entries.iterator()
        while (iterator.hasNext()) {
            val (weaponId, remainingTicks) = iterator.next()
            val definition = runCatching { WeaponContentRegistry.requireWeapon(weaponId) }.getOrNull()
            if (definition == null) {
                iterator.remove()
                removeCooldownBar(player, weaponId)
                continue
            }
            val totalTicks = definition.skill.cooldownSeconds * 20.0
            val progress = (1.0f - (remainingTicks / totalTicks).toFloat()).coerceIn(0.0f, 1.0f)
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
            removeCooldownBars(player)
        }
    }

    fun removeCooldownBar(player: ServerPlayerEntity, weaponId: String) {
        val playerBars = cooldownBars[player.uuid] ?: return
        playerBars.remove(weaponId)?.removePlayer(player)
        if (playerBars.isEmpty()) {
            cooldownBars.remove(player.uuid)
        }
    }

    fun removeCooldownBars(player: ServerPlayerEntity) {
        cooldownBars.remove(player.uuid)?.values?.forEach { it.removePlayer(player) }
    }

    @JvmStatic
    fun hasWeaponInInventory(player: ServerPlayerEntity, weaponId: String): Boolean {
        for (i in 0 until player.inventory.size()) {
            val stack = player.inventory.getStack(i)
            val def = WeaponStackSupport.getDefinition(stack)
            if (def?.id == weaponId) return true
        }
        return false
    }

    fun clearTransientState(player: ServerPlayerEntity) {
        clearTemporaryGuard(player)
        clearShield(player)
        removeCooldownBars(player)
        soulBreakStacks.remove(player.uuid)
        soulBreakExpireTick.remove(player.uuid)
        taoStacks.remove(player.uuid)
        targetMarks.remove(player.uuid)
        invulnerabilityTicks.remove(player.uuid)
        lastHeldWeaponIdByPlayer.remove(player.uuid)
        WeaponSkillRegistry.allHandlers().values.forEach { handler ->
            handler.clearTransientState(player.uuid)
        }
    }

    private fun pruneOfflinePlayerState(onlinePlayerIds: Set<UUID>) {
        val offlinePlayers = cooldownBars.keys.filterNot(onlinePlayerIds::contains)
        for (playerId in offlinePlayers) {
            cooldownBars.remove(playerId)?.values?.forEach { bossBar ->
                bossBar.players.toList().forEach(bossBar::removePlayer)
            }
        }
        temporaryGuardHpByPlayer.keys.removeIf { !onlinePlayerIds.contains(it) }
        temporaryGuardExpireTickByPlayer.keys.removeIf { !onlinePlayerIds.contains(it) }
        lastHeldWeaponIdByPlayer.keys.removeIf { !onlinePlayerIds.contains(it) }
        taoStacks.keys.removeIf { !onlinePlayerIds.contains(it) }
    }

    private inline fun runWeaponHandlers(
        effectId: String,
        definition: WeaponDefinition,
        data: WeaponData,
        block: (WeaponSkillHandler, WeaponDefinition, WeaponData) -> Unit
    ) {
        WeaponSkillRegistry.getHandler(effectId)?.let { block(it, definition, data) }
        for (subSkillEffectId in WeaponSkillRegistry.subSkillEffectIds(effectId)) {
            WeaponSkillRegistry.getHandler(subSkillEffectId)?.let { block(it, definition, data) }
        }
    }

    private inline fun runWeaponBonus(
        effectId: String,
        definition: WeaponDefinition,
        block: (WeaponSkillHandler, WeaponDefinition) -> Double
    ): Double {
        var total = 0.0
        runWeaponHandlers(effectId, definition, WeaponData.DUMMY) { handler, def, _ ->
            total += block(handler, def)
        }
        return total
    }

    private fun activeWeaponContext(player: ServerPlayerEntity): Pair<WeaponDefinition, WeaponData>? {
        HotbarOverrideService.activeWeaponContext(player)?.let { return it }
        val stack = player.mainHandStack
        val definition = WeaponStackSupport.getDefinition(stack) ?: return null
        val data = WeaponStackSupport.getWeaponData(stack) ?: WeaponStackSupport.ensureWeaponData(stack)
        return definition to data
    }

    fun getDisplayStacks(player: ServerPlayerEntity, buffId: String, rawStacks: Int): Int {
        return hifumi.cresora.equipment.EquipmentEffectHookService.getDisplayStacks(player, buffId, rawStacks)
    }
}
