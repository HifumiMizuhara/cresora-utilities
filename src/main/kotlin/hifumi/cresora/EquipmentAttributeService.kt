package hifumi.cresora

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents
import net.minecraft.entity.attribute.EntityAttributeModifier
import net.minecraft.entity.attribute.EntityAttributes
import net.minecraft.util.Identifier
import java.util.UUID

object EquipmentAttributeService {
    private val ATTACK_FLAT_ID: Identifier = Identifier.of(CreSoraUtilities.MOD_ID, "equipment_attack_flat")
    private val ATTACK_SCALAR_ID: Identifier = Identifier.of(CreSoraUtilities.MOD_ID, "equipment_attack_scalar")
    private val HEALTH_FLAT_ID: Identifier = Identifier.of(CreSoraUtilities.MOD_ID, "equipment_health_flat")
    private val HEALTH_SCALAR_ID: Identifier = Identifier.of(CreSoraUtilities.MOD_ID, "equipment_health_scalar")
    private val ARMOR_FLAT_ID: Identifier = Identifier.of(CreSoraUtilities.MOD_ID, "equipment_armor_flat")
    private val ARMOR_SCALAR_ID: Identifier = Identifier.of(CreSoraUtilities.MOD_ID, "equipment_armor_scalar")
    private val equippedFingerprints: MutableMap<UUID, String> = mutableMapOf()

    fun init() {
        ServerTickEvents.END_SERVER_TICK.register { server ->
            for (player in server.playerManager.playerList) {
                val attackInstance = player.attributes.getCustomInstance(EntityAttributes.ATTACK_DAMAGE)
                val healthInstance = player.attributes.getCustomInstance(EntityAttributes.MAX_HEALTH)
                val armorInstance = player.attributes.getCustomInstance(EntityAttributes.ARMOR)
                val totals = EquipmentPlayerSupport.getAggregatedStats(player)
                val bonuses = EquipmentStatCalculator.calculateAttributeBonuses(totals)

                updateModifier(attackInstance, ATTACK_FLAT_ID, bonuses.attackFlat, EntityAttributeModifier.Operation.ADD_VALUE)
                updateModifier(attackInstance, ATTACK_SCALAR_ID, bonuses.attackScalar, EntityAttributeModifier.Operation.ADD_MULTIPLIED_TOTAL)
                updateModifier(healthInstance, HEALTH_FLAT_ID, bonuses.healthFlat, EntityAttributeModifier.Operation.ADD_VALUE)
                updateModifier(healthInstance, HEALTH_SCALAR_ID, bonuses.healthScalar, EntityAttributeModifier.Operation.ADD_MULTIPLIED_TOTAL)
                updateModifier(armorInstance, ARMOR_FLAT_ID, bonuses.armorFlat, EntityAttributeModifier.Operation.ADD_VALUE)
                updateModifier(armorInstance, ARMOR_SCALAR_ID, bonuses.armorScalar, EntityAttributeModifier.Operation.ADD_MULTIPLIED_TOTAL)
                handleEquipChanged(player)
                EquipmentEffectHookService.onTick(player)

                if (player.health > player.maxHealth) {
                    player.health = player.maxHealth
                }
            }
        }
    }

    private fun handleEquipChanged(player: net.minecraft.server.network.ServerPlayerEntity) {
        val fingerprint = EquipmentPlayerSupport.getEquippedEquipmentData(player)
            .sortedBy { it.slotTypeId }
            .joinToString("|") { data ->
                buildString {
                    append(data.slotTypeId)
                    append(':')
                    append(data.setId)
                    append(':')
                    append(data.rarity.id)
                    append(':')
                    append(data.level)
                    append(':')
                    append(data.mainStat.type.id)
                    append(':')
                    append(data.mainStat.value)
                }
            }
        val previous = equippedFingerprints.put(player.uuid, fingerprint)
        if (previous != fingerprint) {
            EquipmentEffectHookService.onEquipChanged(player)
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
