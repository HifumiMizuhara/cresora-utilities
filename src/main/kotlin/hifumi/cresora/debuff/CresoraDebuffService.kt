package hifumi.cresora.debuff
import net.minecraft.entity.mob.HostileEntity
import net.minecraft.entity.mob.MobEntity
import net.minecraft.entity.effect.StatusEffectInstance
import net.minecraft.entity.effect.StatusEffects
import net.minecraft.entity.LivingEntity
import net.minecraft.particle.ParticleTypes
import net.minecraft.server.network.ServerPlayerEntity
import net.minecraft.server.world.ServerWorld
import net.minecraft.text.Text
import net.minecraft.util.Formatting
import net.minecraft.util.TypeFilter
import net.minecraft.util.Identifier
import java.util.*
import java.util.concurrent.ConcurrentHashMap

object CresoraDebuffService {
    private val activeDebuffs = ConcurrentHashMap<UUID, MutableMap<Identifier, Int>>()
    private val targetElements = ConcurrentHashMap<UUID, ElementalState>()

    enum class Element(val displayKey: String) {
        FIRE("element.cresora.fire"),
        WATER("element.cresora.water"),
        ICE("element.cresora.ice"),
        THUNDER("element.cresora.thunder"),
        ARCANE("element.cresora.arcane")
    }

    private data class ElementalState(
        val element: Element,
        val expireTick: Long
    )

    fun addDebuff(player: ServerPlayerEntity, debuff: CresoraDebuff, durationTicks: Int) {
        val playerDebuffs = activeDebuffs.computeIfAbsent(player.uuid) { mutableMapOf() }
        val id = debuff.id
        val isNew = !playerDebuffs.containsKey(id)
        
        playerDebuffs[id] = durationTicks
        
        if (isNew) {
            debuff.onApply(player)
            player.sendMessage(Text.translatable("status.cresora.debuff.applied", Text.translatable(debuff.nameKey)).formatted(Formatting.RED), true)
        }
    }

    fun tick(player: ServerPlayerEntity) {
        cleanupExpiredElements(player.world.time)
        val playerDebuffs = activeDebuffs[player.uuid] ?: return
        val iterator = playerDebuffs.entries.iterator()
        
        while (iterator.hasNext()) {
            val entry = iterator.next()
            val id = entry.key
            val remaining = entry.value - 1
            
            val debuff = CresoraDebuffRegistry.get(id)
            if (debuff == null) {
                iterator.remove()
                continue
            }

            if (remaining <= 0) {
                debuff.onRemove(player)
                iterator.remove()
                player.sendMessage(Text.translatable("status.cresora.debuff.removed", Text.translatable(debuff.nameKey)).formatted(Formatting.GREEN), true)
            } else {
                entry.setValue(remaining)
                debuff.onTick(player, remaining)
            }
        }
        
        if (playerDebuffs.isEmpty()) {
            activeDebuffs.remove(player.uuid)
        }
    }

    fun canHeal(player: ServerPlayerEntity): Boolean {
        val playerDebuffs = activeDebuffs[player.uuid] ?: return true
        return playerDebuffs.keys.all { id ->
            CresoraDebuffRegistry.get(id)?.canHeal(player) ?: true
        }
    }

    fun getCooldownMultiplier(player: ServerPlayerEntity): Double {
        val playerDebuffs = activeDebuffs[player.uuid] ?: return 1.0
        return playerDebuffs.keys.fold(1.0) { acc, id ->
            acc * (CresoraDebuffRegistry.get(id)?.cooldownMultiplier(player) ?: 1.0)
        }
    }

    fun getAttackMultiplier(player: ServerPlayerEntity): Double {
        val playerDebuffs = activeDebuffs[player.uuid] ?: return 1.0
        return playerDebuffs.keys.fold(1.0) { acc, id ->
            acc * (CresoraDebuffRegistry.get(id)?.attackMultiplier(player) ?: 1.0)
        }
    }

    fun getIncomingDamageMultiplier(player: ServerPlayerEntity): Double {
        val playerDebuffs = activeDebuffs[player.uuid] ?: return 1.0
        return playerDebuffs.keys.fold(1.0) { acc, id ->
            acc * (CresoraDebuffRegistry.get(id)?.incomingDamageMultiplier(player) ?: 1.0)
        }
    }

    fun onEliteHit(player: ServerPlayerEntity, elite: MobEntity) {
        // Phase 1 removes passive random debuffs. Elemental reactions are now player-triggered.
    }

    fun triggerElementalSkill(player: ServerPlayerEntity, weaponId: String, effectId: String, radiusMeters: Double): Int {
        val element = elementForWeaponSkill(weaponId, effectId) ?: return 0
        val world = player.world as? ServerWorld ?: return 0
        val radius = radiusMeters.coerceAtLeast(6.0)
        val targets = world.getOtherEntities(player, player.boundingBox.expand(radius)) { entity ->
            entity is LivingEntity &&
                entity.isAlive &&
                entity !is ServerPlayerEntity &&
                (entity is MobEntity || entity is HostileEntity)
        }.filterIsInstance<LivingEntity>()

        var reactions = 0
        for (target in targets) {
            if (applyElement(player, target, element, 8 * 20)) {
                reactions++
            }
        }
        return reactions
    }

    private fun applyElement(player: ServerPlayerEntity, target: LivingEntity, element: Element, durationTicks: Int): Boolean {
        val world = target.world as? ServerWorld ?: return false
        val now = world.time
        val previous = targetElements[target.uuid]?.takeIf { it.expireTick > now }
        targetElements[target.uuid] = ElementalState(element, now + durationTicks)
        spawnElementParticles(world, target, element)
        if (previous == null || previous.element == element) {
            return false
        }
        triggerReaction(player, target, previous.element, element)
        return true
    }

    private fun triggerReaction(player: ServerPlayerEntity, target: LivingEntity, first: Element, second: Element) {
        val world = target.world as? ServerWorld ?: return
        val reactionKey = reactionKey(first, second)
        val damage = reactionDamage(first, second)
        if (damage > 0.0f) {
            target.damage(world, player.damageSources.indirectMagic(player, player), damage)
        }
        if ((first == Element.WATER && second == Element.ICE) || (first == Element.ICE && second == Element.WATER)) {
            target.addStatusEffect(StatusEffectInstance(StatusEffects.SLOWNESS, 80, 3))
        }
        if ((first == Element.FIRE && second == Element.THUNDER) || (first == Element.THUNDER && second == Element.FIRE)) {
            world.spawnParticles(ParticleTypes.EXPLOSION, target.x, target.y + 0.5, target.z, 1, 0.1, 0.1, 0.1, 0.0)
        } else {
            world.spawnParticles(ParticleTypes.ENCHANT, target.x, target.y + 1.0, target.z, 16, 0.45, 0.45, 0.45, 0.05)
        }
        player.sendMessage(Text.translatable("message.cresora.elemental_reaction", Text.translatable(reactionKey)).formatted(Formatting.AQUA), true)
    }

    private fun elementForWeaponSkill(weaponId: String, effectId: String): Element? {
        return when {
            weaponId == "harukanaru_shojo_no_ketsui" -> Element.ARCANE
            effectId.contains("flame") -> Element.FIRE
            effectId.contains("snow") || effectId.contains("frost") -> Element.ICE
            effectId.contains("healing") || effectId.contains("heal") || effectId.contains("lakeside") -> Element.WATER
            effectId.contains("thunder") || effectId.contains("danrai") -> Element.THUNDER
            effectId.contains("dark") || effectId.contains("lux") -> Element.ARCANE
            else -> null
        }
    }

    private fun reactionKey(first: Element, second: Element): String {
        val pair = setOf(first, second)
        return when {
            pair == setOf(Element.FIRE, Element.WATER) -> "reaction.cresora.vaporize"
            pair == setOf(Element.WATER, Element.ICE) -> "reaction.cresora.freeze"
            pair == setOf(Element.FIRE, Element.THUNDER) -> "reaction.cresora.overload"
            pair == setOf(Element.WATER, Element.THUNDER) -> "reaction.cresora.conduct"
            pair.contains(Element.ARCANE) -> "reaction.cresora.resonance"
            else -> "reaction.cresora.catalyze"
        }
    }

    private fun reactionDamage(first: Element, second: Element): Float {
        val pair = setOf(first, second)
        return when {
            pair == setOf(Element.FIRE, Element.THUNDER) -> 7.0f
            pair == setOf(Element.FIRE, Element.WATER) -> 5.0f
            pair == setOf(Element.WATER, Element.THUNDER) -> 4.0f
            pair.contains(Element.ARCANE) -> 3.0f
            else -> 2.0f
        }
    }

    private fun spawnElementParticles(world: ServerWorld, target: LivingEntity, element: Element) {
        val particle = when (element) {
            Element.FIRE -> ParticleTypes.FLAME
            Element.WATER -> ParticleTypes.SPLASH
            Element.ICE -> ParticleTypes.SNOWFLAKE
            Element.THUNDER -> ParticleTypes.ELECTRIC_SPARK
            Element.ARCANE -> ParticleTypes.WITCH
        }
        world.spawnParticles(particle, target.x, target.y + 0.8, target.z, 8, 0.35, 0.35, 0.35, 0.03)
    }

    private fun cleanupExpiredElements(now: Long) {
        targetElements.entries.removeIf { it.value.expireTick <= now }
    }

    fun clearTransientState(player: ServerPlayerEntity) {
        clearTransientState(player.uuid)
    }

    // UUID-keyed cleanup. Disconnect routes through here so a reconnecting player never
    // inherits stale debuffs (reskill protection must not carry across sessions).
    fun clearTransientState(playerId: UUID) {
        activeDebuffs.remove(playerId)
    }

    fun hasActiveDebuffs(playerId: UUID): Boolean = activeDebuffs.containsKey(playerId)
}
