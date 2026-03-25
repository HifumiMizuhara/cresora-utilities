package hifumi.cresora

import dev.emi.trinkets.api.TrinketsApi
import net.minecraft.entity.player.PlayerEntity
import java.util.EnumMap

object EquipmentPlayerSupport {
    fun getEquippedEquipmentData(player: PlayerEntity): List<EquipmentData> {
        val component = TrinketsApi.getTrinketComponent(player).orElse(null) ?: return emptyList()
        return component.getAllEquipped().mapNotNull { equipped ->
            EquipmentStackSupport.getEquipmentData(equipped.right)
        }
    }

    @JvmStatic
    fun getAggregatedStats(player: PlayerEntity): Map<StatType, Double> {
        val totals = EnumMap<StatType, Double>(StatType::class.java)
        val equippedData = getEquippedEquipmentData(player)
        for (data in equippedData) {
            val aggregated = EquipmentStatCalculator.aggregate(data)
            for ((type, value) in aggregated) {
                totals[type] = (totals[type] ?: 0.0) + value
            }
        }

        val setCounts = equippedData.groupingBy(EquipmentData::setId).eachCount()
        for ((setId, count) in setCounts) {
            for (bonus in setId.activeBonuses(count)) {
                totals[bonus.type] = (totals[bonus.type] ?: 0.0) + bonus.value
            }
        }
        return totals
    }
}
