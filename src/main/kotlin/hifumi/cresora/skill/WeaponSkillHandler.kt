package hifumi.cresora.skill

import hifumi.cresora.WeaponData
import hifumi.cresora.WeaponDefinition
import hifumi.cresora.WeaponSkillAccess
import net.minecraft.entity.LivingEntity
import net.minecraft.server.MinecraftServer
import net.minecraft.server.network.ServerPlayerEntity
import net.minecraft.util.ActionResult

interface WeaponSkillHandler {
    fun activate(
        player: ServerPlayerEntity,
        definition: WeaponDefinition,
        data: WeaponData,
        access: WeaponSkillAccess
    ): ActionResult {
        return ActionResult.PASS
    }

    fun onTick(server: MinecraftServer) {}

    fun onPlayerTick(player: ServerPlayerEntity) {}

    fun onDamageAbsorbed(player: ServerPlayerEntity, amount: Float): Float {
        return amount
    }

    fun onDamageDealt(player: ServerPlayerEntity, target: LivingEntity, amount: Float, isTrueDamage: Boolean) {}

    fun onDamageTaken(player: ServerPlayerEntity, amount: Float): Float {
        return amount
    }
}
