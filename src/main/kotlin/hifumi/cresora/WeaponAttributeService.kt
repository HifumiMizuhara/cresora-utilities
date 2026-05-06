package hifumi.cresora

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents
import net.minecraft.entity.attribute.EntityAttributeModifier
import net.minecraft.entity.attribute.EntityAttributes
import net.minecraft.util.Identifier

object WeaponAttributeService {
    private val ATTACK_DAMAGE_ID: Identifier = Identifier.of(CreSoraUtilities.MOD_ID, "weapon_attack_damage")
    private val ATTACK_SPEED_ID: Identifier = Identifier.of(CreSoraUtilities.MOD_ID, "weapon_attack_speed")
    private val DYNAMIC_ATTACK_SCALAR_ID: Identifier = Identifier.of(CreSoraUtilities.MOD_ID, "weapon_dynamic_attack_scalar")
    private val DYNAMIC_ARMOR_SCALAR_ID: Identifier = Identifier.of(CreSoraUtilities.MOD_ID, "weapon_dynamic_armor_scalar")

    fun init() {
        ServerTickEvents.END_SERVER_TICK.register { server ->
            for (player in server.playerManager.playerList) {
                val heldStack = player.mainHandStack
                val definition = WeaponStackSupport.getDefinition(heldStack)
                val data = WeaponStackSupport.getWeaponData(heldStack)


                updateModifier(
                    player.attributes.getCustomInstance(EntityAttributes.ATTACK_DAMAGE),
                    ATTACK_DAMAGE_ID,
                    if (definition != null && data != null) WeaponCombatSupport.attackDamageModifier(definition, data) else 0.0,
                    EntityAttributeModifier.Operation.ADD_VALUE
                )
                updateModifier(
                    player.attributes.getCustomInstance(EntityAttributes.ATTACK_SPEED),
                    ATTACK_SPEED_ID,
                    if (definition != null && data != null) WeaponCombatSupport.attackSpeedModifier(definition) else 0.0,
                    EntityAttributeModifier.Operation.ADD_VALUE
                )

                updateModifier(
                    player.attributes.getCustomInstance(EntityAttributes.ATTACK_DAMAGE),
                    DYNAMIC_ATTACK_SCALAR_ID,
                    WeaponSkillService.attackDamageScalar(player),
                    EntityAttributeModifier.Operation.ADD_MULTIPLIED_TOTAL
                )
                updateModifier(
                    player.attributes.getCustomInstance(EntityAttributes.ARMOR),
                    DYNAMIC_ARMOR_SCALAR_ID,
                    WeaponSkillService.armorScalar(player),
                    EntityAttributeModifier.Operation.ADD_MULTIPLIED_TOTAL
                )
            }
        }
    }

    private fun updateModifier(
        instance: net.minecraft.entity.attribute.EntityAttributeInstance?,
        id: Identifier,
        value: Double,
        operation: EntityAttributeModifier.Operation
    ) {
        if (instance == null) {
            return
        }
        if (value == 0.0) {
            instance.removeModifier(id)
            return
        }
        val existing = instance.getModifier(id)
        if (existing != null && existing.value == value && existing.operation == operation) {
            return
        }
        instance.removeModifier(id)
        instance.addTemporaryModifier(EntityAttributeModifier(id, value, operation))
    }
}
