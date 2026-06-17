package hifumi.cresora.debuff

import net.minecraft.entity.mob.HostileEntity
import net.minecraft.entity.mob.MobEntity
import net.minecraft.entity.effect.StatusEffectInstance
import net.minecraft.entity.effect.StatusEffects
import net.minecraft.entity.LivingEntity
import net.minecraft.particle.ParticleTypes
import net.minecraft.server.network.ServerPlayerEntity
import net.minecraft.server.world.ServerWorld
import net.minecraft.sound.SoundEvents
import net.minecraft.sound.SoundCategory
import net.minecraft.text.Text
import net.minecraft.util.Formatting
import net.minecraft.util.TypeFilter
import net.minecraft.util.Identifier
import java.util.*
import java.util.concurrent.ConcurrentHashMap

object CresoraDebuffService {
    private val activeDebuffs = ConcurrentHashMap<UUID, MutableMap<Identifier, Int>>()
    private val targetNotes = ConcurrentHashMap<UUID, NoteState>()
    private val crescendoTargets = ConcurrentHashMap<UUID, CrescendoMark>() // target UUID -> active mark

    private data class CrescendoMark(val markerPlayer: UUID, val expireTick: Long)

    enum class Note {
        TREBLE,
        BASS,
        MELODY,
        HARMONY
    }

    private data class NoteState(
        val note: Note,
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
        cleanupExpiredNotes(player.world.time)
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

    fun getCrescendoMultiplier(target: LivingEntity, attacker: LivingEntity?): Double {
        val mark = crescendoTargets[target.uuid] ?: return 1.0
        if (target.world.time >= mark.expireTick) return 1.0
        // Only the player who triggered the Crescendo reaction deals amplified damage.
        return if (attacker != null && attacker.uuid == mark.markerPlayer) 1.5 else 1.0
    }

    fun triggerElementalSkill(player: ServerPlayerEntity, weaponId: String, effectId: String, radiusMeters: Double): Int {
        val note = noteForWeaponSkill(weaponId, effectId) ?: return 0
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
            if (applyNote(player, target, note, 8 * 20)) {
                reactions++
            }
        }
        return reactions
    }

    private fun applyNote(player: ServerPlayerEntity, target: LivingEntity, note: Note, durationTicks: Int): Boolean {
        val world = target.world as? ServerWorld ?: return false
        val now = world.time
        val previous = targetNotes[target.uuid]?.takeIf { it.expireTick > now }
        targetNotes[target.uuid] = NoteState(note, now + durationTicks)
        spawnNoteParticles(world, target, note)
        
        // Play note sound
        val sound = when (note) {
            Note.TREBLE -> SoundEvents.BLOCK_NOTE_BLOCK_BELL
            Note.BASS -> SoundEvents.BLOCK_NOTE_BLOCK_BASS
            Note.MELODY -> SoundEvents.BLOCK_NOTE_BLOCK_FLUTE
            Note.HARMONY -> SoundEvents.BLOCK_NOTE_BLOCK_CHIME
        }
        world.playSound(null, target.x, target.y, target.z, sound, SoundCategory.PLAYERS, 0.7f, 1.2f)

        if (previous == null || previous.note == note) {
            return false
        }
        triggerReaction(player, target, previous.note, note)
        return true
    }

    private fun triggerReaction(player: ServerPlayerEntity, target: LivingEntity, first: Note, second: Note) {
        val world = target.world as? ServerWorld ?: return
        val pair = setOf(first, second)
        
        // Display reaction text using translatable key
        val reactionKey = reactionKey(first, second)
        
        when {
            // Treble + Bass = Discord
            pair == setOf(Note.TREBLE, Note.BASS) -> {
                target.damage(world, player.damageSources.indirectMagic(player, player), 10.0f)
                target.addStatusEffect(StatusEffectInstance(StatusEffects.SLOWNESS, 80, 3)) // Slowness IV
                
                // Sonic blast knockback and damage to nearby mobs
                val nearbyMobs = world.getOtherEntities(target, target.boundingBox.expand(5.0)) { entity ->
                    entity is LivingEntity &&
                        entity.isAlive &&
                        entity !is ServerPlayerEntity &&
                        (entity is MobEntity || entity is HostileEntity)
                }.filterIsInstance<LivingEntity>()
                for (mob in nearbyMobs) {
                    val dx = mob.x - target.x
                    val dz = mob.z - target.z
                    val dist = Math.sqrt(dx * dx + dz * dz).coerceAtLeast(0.1)
                    mob.takeKnockback(0.8, -dx / dist, -dz / dist)
                    mob.damage(world, player.damageSources.indirectMagic(player, player), 3.0f)
                }
                world.spawnParticles(ParticleTypes.EXPLOSION, target.x, target.y + 0.5, target.z, 1, 0.1, 0.1, 0.1, 0.0)
                world.spawnParticles(ParticleTypes.SONIC_BOOM, target.x, target.y + 0.5, target.z, 1, 0.0, 0.0, 0.0, 0.0)
                world.playSound(null, target.x, target.y, target.z, SoundEvents.ENTITY_GENERIC_EXPLODE, SoundCategory.PLAYERS, 0.8f, 1.4f)
            }

            // Treble + Melody = Crescendo
            pair == setOf(Note.TREBLE, Note.MELODY) -> {
                target.damage(world, player.damageSources.indirectMagic(player, player), 6.0f)
                target.addStatusEffect(StatusEffectInstance(StatusEffects.GLOWING, 120, 0))
                crescendoTargets[target.uuid] = CrescendoMark(player.uuid, world.time + 120)
                world.spawnParticles(ParticleTypes.NOTE, target.x, target.y + 1.2, target.z, 8, 0.3, 0.3, 0.3, 0.1)
                world.spawnParticles(ParticleTypes.GLOW, target.x, target.y + 0.8, target.z, 12, 0.4, 0.4, 0.4, 0.05)
                world.playSound(null, target.x, target.y, target.z, SoundEvents.BLOCK_NOTE_BLOCK_HARP, SoundCategory.PLAYERS, 0.9f, 1.5f)
            }

            // Bass + Melody = Decrescendo
            pair == setOf(Note.BASS, Note.MELODY) -> {
                target.damage(world, player.damageSources.indirectMagic(player, player), 4.0f)
                target.addStatusEffect(StatusEffectInstance(StatusEffects.WEAKNESS, 120, 2)) // Weakness III
                target.addStatusEffect(StatusEffectInstance(StatusEffects.SLOWNESS, 120, 1)) // Slowness II
                world.spawnParticles(ParticleTypes.SMOKE, target.x, target.y + 0.8, target.z, 15, 0.4, 0.4, 0.4, 0.02)
                world.playSound(null, target.x, target.y, target.z, SoundEvents.BLOCK_NOTE_BLOCK_GUITAR, SoundCategory.PLAYERS, 0.9f, 0.6f)
            }

            // Treble + Harmony = Cadenza
            pair == setOf(Note.TREBLE, Note.HARMONY) -> {
                target.damage(world, player.damageSources.indirectMagic(player, player), 12.0f)
                
                // Reduce active weapon skill cooldown by 20%
                val stack = player.mainHandStack
                val weaponDefinition = hifumi.cresora.weapon.WeaponStackSupport.getDefinition(stack)
                if (weaponDefinition != null) {
                    val cooldowns = (player as? hifumi.cresora.weapon.WeaponSkillAccess)?.cresoraGetCooldowns()
                    if (cooldowns != null) {
                        val currentCd = cooldowns[weaponDefinition.id] ?: 0.0
                        if (currentCd > 0.0) {
                            val totalTicks = weaponDefinition.skill.cooldownSeconds * 20.0
                            val reduction = totalTicks * 0.20
                            cooldowns[weaponDefinition.id] = Math.max(0.0, currentCd - reduction)
                        }
                    }
                }
                world.spawnParticles(ParticleTypes.SWEEP_ATTACK, target.x, target.y + 0.8, target.z, 1, 0.0, 0.0, 0.0, 0.0)
                world.spawnParticles(ParticleTypes.ENCHANT, target.x, target.y + 1.0, target.z, 20, 0.45, 0.45, 0.45, 0.1)
                world.playSound(null, target.x, target.y, target.z, SoundEvents.BLOCK_NOTE_BLOCK_PLING, SoundCategory.PLAYERS, 1.0f, 1.8f)
            }

            // Bass + Harmony = Requiem
            pair == setOf(Note.BASS, Note.HARMONY) -> {
                target.damage(world, player.damageSources.indirectMagic(player, player), 5.0f)
                target.addStatusEffect(StatusEffectInstance(StatusEffects.SLOWNESS, 60, 5)) // Slowness VI (Root)
                
                // Grant player Absorption III (adds 6 hearts / 12 HP)
                player.addStatusEffect(StatusEffectInstance(StatusEffects.ABSORPTION, 120, 2))
                world.spawnParticles(ParticleTypes.ANGRY_VILLAGER, target.x, target.y + 1.0, target.z, 5, 0.3, 0.3, 0.3, 0.0)
                world.spawnParticles(ParticleTypes.HEART, player.x, player.y + 1.0, player.z, 4, 0.2, 0.2, 0.2, 0.0)
                world.playSound(null, target.x, target.y, target.z, SoundEvents.BLOCK_RESPAWN_ANCHOR_CHARGE, SoundCategory.PLAYERS, 0.8f, 0.7f)
            }

            // Melody + Harmony = Serenade
            pair == setOf(Note.MELODY, Note.HARMONY) -> {
                target.damage(world, player.damageSources.indirectMagic(player, player), 4.0f)
                target.addStatusEffect(StatusEffectInstance(StatusEffects.WEAKNESS, 100, 0)) // Weakness I
                
                val allies = world.getEntitiesByClass(ServerPlayerEntity::class.java, player.boundingBox.expand(8.0)) { true }
                for (ally in allies) {
                    ally.heal(8.0f)
                    world.spawnParticles(ParticleTypes.HEART, ally.x, ally.y + 1.0, ally.z, 5, 0.2, 0.2, 0.2, 0.0)
                }
                world.playSound(null, target.x, target.y, target.z, SoundEvents.BLOCK_NOTE_BLOCK_BIT, SoundCategory.PLAYERS, 0.9f, 1.2f)
            }
        }
        player.sendMessage(Text.translatable("message.cresora.elemental_reaction", Text.translatable(reactionKey)).formatted(Formatting.AQUA), true)
    }

    private fun noteForWeaponSkill(weaponId: String, effectId: String): Note? {
        return when {
            // Treble: Bright, fast, high frequency, wind/lightning/haste
            effectId.contains("sunlit_haste") ||
            effectId.contains("snow_frost") -> Note.TREBLE

            // Bass: Heavy, dark, protective
            effectId.contains("dark_lux") ||
            effectId.contains("shield") ||
            weaponId == "rondo_melody" -> Note.BASS

            // Melody: Flowing water, lakeside, ink/straight rain, orchids
            effectId.contains("healing_aura") ||
            effectId.contains("current_hp") ||
            effectId.contains("orchid_pavilion") ||
            effectId.contains("rougan_kenpo") -> Note.MELODY

            // Harmony: Resonance, blessing, dawn/flame harmony, masquerade
            weaponId == "harukanaru_shojo_no_ketsui" ||
            weaponId == "harukanaru_shonen_no_omoi" ||
            weaponId == "cadenza_allegro" ||
            effectId.contains("baa_mimic") ||
            effectId.contains("flame_aura") ||
            effectId.contains("heal") ||
            weaponId == "masquerade_invitation" -> Note.HARMONY

            else -> null
        }
    }

    private fun reactionKey(first: Note, second: Note): String {
        val pair = setOf(first, second)
        return when {
            pair == setOf(Note.TREBLE, Note.BASS) -> "reaction.cresora.discord"
            pair == setOf(Note.TREBLE, Note.MELODY) -> "reaction.cresora.crescendo"
            pair == setOf(Note.BASS, Note.MELODY) -> "reaction.cresora.decrescendo"
            pair == setOf(Note.TREBLE, Note.HARMONY) -> "reaction.cresora.cadenza"
            pair == setOf(Note.BASS, Note.HARMONY) -> "reaction.cresora.requiem"
            pair == setOf(Note.MELODY, Note.HARMONY) -> "reaction.cresora.serenade"
            else -> "reaction.cresora.resonance"
        }
    }

    private fun spawnNoteParticles(world: ServerWorld, target: LivingEntity, note: Note) {
        val particle = when (note) {
            Note.TREBLE -> ParticleTypes.ELECTRIC_SPARK
            Note.BASS -> ParticleTypes.ENCHANT
            Note.MELODY -> ParticleTypes.SPLASH
            Note.HARMONY -> ParticleTypes.HAPPY_VILLAGER
        }
        world.spawnParticles(particle, target.x, target.y + 0.8, target.z, 8, 0.35, 0.35, 0.35, 0.03)
    }

    private fun cleanupExpiredNotes(now: Long) {
        targetNotes.entries.removeIf { it.value.expireTick <= now }
        crescendoTargets.entries.removeIf { it.value.expireTick <= now }
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
