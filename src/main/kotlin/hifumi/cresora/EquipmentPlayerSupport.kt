package hifumi.cresora

import dev.emi.trinkets.api.TrinketsApi
import hifumi.cresora.equipment.ArtifactSkillRegistry
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.server.network.ServerPlayerEntity
import java.util.EnumMap

object EquipmentPlayerSupport {
    data class ActiveSetBonus(
        val set: EquipmentSet,
        val pieceCount: Int,
        val bonus: EquipmentSetBonus
    )

    data class ActiveSetSummary(
        val set: EquipmentSet,
        val pieceCount: Int,
        val activeThresholds: List<Int>
    )

    fun getEquippedEquipmentData(player: PlayerEntity): List<EquipmentData> {
        val component = TrinketsApi.getTrinketComponent(player).orElse(null) ?: return emptyList()
        return component.getAllEquipped().mapNotNull { equipped ->
            EquipmentStackSupport.getEquipmentData(equipped.right)
        }
    }

    fun getActiveSetBonuses(player: PlayerEntity): List<ActiveSetBonus> {
        return getActiveSetBonuses(getEquippedEquipmentData(player))
    }

    fun getActiveSetSummaries(player: PlayerEntity): List<ActiveSetSummary> {
        val bonusesBySet = getActiveSetBonuses(player).groupBy { it.set.id }
        return bonusesBySet.values.map { bonuses ->
            val first = bonuses.first()
            ActiveSetSummary(
                set = first.set,
                pieceCount = first.pieceCount,
                activeThresholds = bonuses.map { it.bonus.requiredPieces }.distinct().sorted()
            )
        }.sortedBy { it.set.id }
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

        for (activeSetBonus in getActiveSetBonuses(equippedData)) {
            for (bonus in activeSetBonus.bonus.stats) {
                totals[bonus.type] = (totals[bonus.type] ?: 0.0) + bonus.value
            }
            if (player is ServerPlayerEntity) {
                for (hook in activeSetBonus.bonus.effectHooks) {
                    val handler = ArtifactSkillRegistry.getHandler(hook.effectId) ?: continue
                    val atkScalar = handler.getAttackDamageScalar(player)
                    if (atkScalar != 0.0) {
                        totals[StatType.ATK_PERCENT] = (totals[StatType.ATK_PERCENT] ?: 0.0) + atkScalar
                    }
                    val armorScalar = handler.getArmorScalar(player)
                    if (armorScalar != 0.0) {
                        totals[StatType.DEF_PERCENT] = (totals[StatType.DEF_PERCENT] ?: 0.0) + armorScalar
                    }
                    val critRate = handler.getCritRateBonus(player)
                    if (critRate != 0.0) {
                        totals[StatType.CRIT_RATE] = (totals[StatType.CRIT_RATE] ?: 0.0) + critRate
                    }
                    val critDmg = handler.getCritDamageBonus(player)
                    if (critDmg != 0.0) {
                        totals[StatType.CRIT_DMG] = (totals[StatType.CRIT_DMG] ?: 0.0) + critDmg
                    }
                }
            }
        }
        for ((type, value) in MasqueradeService.getAggregatedSupportStats(player)) {
            totals[type] = (totals[type] ?: 0.0) + value
        }
        return totals
    }


    private fun getActiveSetBonuses(equippedData: List<EquipmentData>): List<ActiveSetBonus> {
        val setCounts = equippedData.groupingBy(EquipmentData::setId).eachCount()
        return setCounts.flatMap { (setId, count) ->
            val set = runCatching { EquipmentContentRegistry.requireSet(setId) }.getOrNull() ?: return@flatMap emptyList()
            set.activeBonuses(count).map { bonus ->
                ActiveSetBonus(
                    set = set,
                    pieceCount = count,
                    bonus = bonus
                )
            }
        }
    }
}
