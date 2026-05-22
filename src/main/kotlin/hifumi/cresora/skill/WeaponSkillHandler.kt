package hifumi.cresora.skill

import hifumi.cresora.weapon.WeaponData
import hifumi.cresora.weapon.WeaponDefinition
import hifumi.cresora.weapon.WeaponSkillAccess

import net.minecraft.entity.LivingEntity
import net.minecraft.server.MinecraftServer
import net.minecraft.server.network.ServerPlayerEntity
import net.minecraft.util.ActionResult
import java.util.UUID

interface WeaponSkillHandler {
    fun activate(
        player: ServerPlayerEntity,
        definition: WeaponDefinition,
        data: WeaponData,
        access: WeaponSkillAccess
    ): ActionResult {
        return ActionResult.PASS
    }

    fun onTick(server: MinecraftServer, definition: WeaponDefinition, data: WeaponData) {}

    fun onPlayerTick(player: ServerPlayerEntity, definition: WeaponDefinition, data: WeaponData) {}

    fun onDamageAbsorbed(player: ServerPlayerEntity, amount: Float, definition: WeaponDefinition, data: WeaponData): Float {
        return amount
    }

    fun onDamageDealt(player: ServerPlayerEntity, target: LivingEntity, amount: Float, isTrueDamage: Boolean, definition: WeaponDefinition, data: WeaponData) {}

    fun onDamageTaken(player: ServerPlayerEntity, amount: Float, definition: WeaponDefinition, data: WeaponData): Float {
        return amount
    }

    fun getAttackDamageScalar(player: ServerPlayerEntity): Double = 0.0

    fun getArmorScalar(player: ServerPlayerEntity): Double = 0.0

    fun getCritRateBonus(player: ServerPlayerEntity): Double = 0.0

    fun getCritDamageBonus(player: ServerPlayerEntity): Double = 0.0

    fun getRegenStageBonus(player: ServerPlayerEntity): Int = 0

    fun clearTransientState(playerId: UUID) {}

    fun pruneTransientState(activePlayerIds: Set<UUID>) {}
}
