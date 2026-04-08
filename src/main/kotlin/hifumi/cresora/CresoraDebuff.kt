package hifumi.cresora

import net.minecraft.server.network.ServerPlayerEntity
import net.minecraft.util.Identifier

interface CresoraDebuff {
    val id: Identifier
    val nameKey: String
    
    fun onApply(player: ServerPlayerEntity) {}
    
    fun onTick(player: ServerPlayerEntity, remainingTicks: Int) {}
    
    fun onRemove(player: ServerPlayerEntity) {}
    
    fun canHeal(player: ServerPlayerEntity): Boolean = true
    
    fun cooldownMultiplier(player: ServerPlayerEntity): Double = 1.0

    fun attackMultiplier(player: ServerPlayerEntity): Double = 1.0

    fun incomingDamageMultiplier(player: ServerPlayerEntity): Double = 1.0
}
