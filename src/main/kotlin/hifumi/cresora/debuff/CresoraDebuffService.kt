package hifumi.cresora.debuff

import net.minecraft.entity.mob.HostileEntity
import net.minecraft.entity.mob.Monster
import net.minecraft.entity.effect.StatusEffectInstance
import net.minecraft.entity.effect.StatusEffects
import net.minecraft.entity.LivingEntity
import net.minecraft.particle.ParticleTypes
import net.minecraft.server.MinecraftServer
import net.minecraft.server.network.ServerPlayerEntity
import net.minecraft.server.world.ServerWorld
import net.minecraft.sound.SoundEvents
import net.minecraft.sound.SoundCategory
import net.minecraft.text.Text
import net.minecraft.util.Formatting
import hifumi.cresora.story.StoryService
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

object CresoraDebuffService {
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

    fun cleanupAll(server: MinecraftServer) {
        val now = server.overworld.time
        targetNotes.entries.removeIf { it.value.expireTick <= now }
        crescendoTargets.entries.removeIf { it.value.expireTick <= now }
    }

    fun getCrescendoMultiplier(target: LivingEntity, attacker: LivingEntity?): Double {
        val mark = crescendoTargets[target.uuid] ?: return 1.0
        val world = target.world as? ServerWorld ?: return 1.0
        if (world.server.overworld.time >= mark.expireTick) return 1.0
        // Only the player who triggered the Crescendo reaction deals amplified damage.
        return if (attacker != null && attacker.uuid == mark.markerPlayer) 1.5 else 1.0
    }

    fun triggerElementalSkill(player: ServerPlayerEntity, noteId: String?, radiusMeters: Double): Int {
        val note = parseNote(noteId) ?: return 0
        if (radiusMeters <= 0.0) return 0
        val world = player.world as? ServerWorld ?: return 0
        val targets = world.getOtherEntities(player, player.boundingBox.expand(radiusMeters)) { entity ->
            entity is LivingEntity &&
                entity.isAlive &&
                entity !is ServerPlayerEntity &&
                (entity is HostileEntity || entity is Monster)
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
        val now = serverTime(player)
        val previous = targetNotes[target.uuid]?.takeIf { it.expireTick > now }
        targetNotes[target.uuid] = NoteState(note, now + durationTicks)
        spawnNoteParticles(world, target, note)

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
                        (entity is HostileEntity || entity is Monster)
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
                crescendoTargets[target.uuid] = CrescendoMark(player.uuid, serverTime(player) + 120)
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

                // Reduce active weapon skill cooldown by 20% of its total cooldown.
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
        player.sendMessage(Text.translatable("message.cresora.resonant_chord", Text.translatable(reactionKey)).formatted(Formatting.AQUA), true)
        StoryService.onResonantChordTriggered(player, reactionKey)
    }

    private fun parseNote(noteId: String?): Note? {
        return when (noteId?.lowercase()) {
            "treble" -> Note.TREBLE
            "bass" -> Note.BASS
            "melody" -> Note.MELODY
            "harmony" -> Note.HARMONY
            else -> null
        }
    }

    private fun serverTime(player: ServerPlayerEntity): Long = player.server?.overworld?.time ?: player.world.time

    private fun reactionKey(first: Note, second: Note): String {
        val pair = setOf(first, second)
        return when (pair) {
            setOf(Note.TREBLE, Note.BASS) -> "reaction.cresora.discord"
            setOf(Note.TREBLE, Note.MELODY) -> "reaction.cresora.crescendo"
            setOf(Note.BASS, Note.MELODY) -> "reaction.cresora.decrescendo"
            setOf(Note.TREBLE, Note.HARMONY) -> "reaction.cresora.cadenza"
            setOf(Note.BASS, Note.HARMONY) -> "reaction.cresora.requiem"
            setOf(Note.MELODY, Note.HARMONY) -> "reaction.cresora.serenade"
            else -> error("Invalid note pair: $first + $second")
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
}
