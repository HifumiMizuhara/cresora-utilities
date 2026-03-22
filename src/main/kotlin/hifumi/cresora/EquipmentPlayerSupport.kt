package hifumi.cresora

import dev.emi.trinkets.api.TrinketsApi
import net.minecraft.entity.player.PlayerEntity
import java.util.EnumMap

object EquipmentPlayerSupport {
    @JvmStatic
    fun getAggregatedStats(player: PlayerEntity): Map<StatType, Double> {
        val totals = EnumMap<StatType, Double>(StatType::class.java)
        val component = TrinketsApi.getTrinketComponent(player).orElse(null) ?: return totals
        for (equipped in component.getAllEquipped()) {
            val stack = equipped.right
            val data = EquipmentStackSupport.getEquipmentData(stack) ?: continue
            val aggregated = EquipmentStatCalculator.aggregate(data)
            for ((type, value) in aggregated) {
                totals[type] = (totals[type] ?: 0.0) + value
            }
        }
        return totals
    }
}
