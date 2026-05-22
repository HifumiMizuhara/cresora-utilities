package hifumi.cresora.debuff
import net.minecraft.entity.mob.HostileEntity
import net.minecraft.server.network.ServerPlayerEntity
import net.minecraft.server.world.ServerWorld
import net.minecraft.text.Text
import net.minecraft.util.Formatting
import net.minecraft.util.Identifier
import java.util.*
import java.util.concurrent.ConcurrentHashMap

object CresoraDebuffService {
    private val activeDebuffs = ConcurrentHashMap<UUID, MutableMap<Identifier, Int>>()

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

    fun onEliteHit(player: ServerPlayerEntity, elite: HostileEntity) {
        val world = player.world as? ServerWorld ?: return
        if (world.random.nextDouble() > 0.35) return // 35% chance to apply a debuff
        
        val possibleDebuffs = listOf(
            CresoraDebuffRegistry.NERVE_DAMAGE,
            CresoraDebuffRegistry.ROOT,
            CresoraDebuffRegistry.SMOKE,
            CresoraDebuffRegistry.BURN,
            CresoraDebuffRegistry.COOLDOWN_PENALTY,
            CresoraDebuffRegistry.HEAL_BLOCK
        )
        
        val debuff = possibleDebuffs[world.random.nextInt(possibleDebuffs.size)]
        val durationSeconds = world.random.nextBetween(5, 12)
        addDebuff(player, debuff, durationSeconds * 20)
    }

    fun clearTransientState(player: ServerPlayerEntity) {
        activeDebuffs.remove(player.uuid)
    }
}
