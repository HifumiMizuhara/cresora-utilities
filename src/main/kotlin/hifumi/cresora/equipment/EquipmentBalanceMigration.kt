package hifumi.cresora.equipment

import hifumi.cresora.combat.CombatBalanceProfileRegistry

object EquipmentBalanceMigration {
    fun migrate(data: EquipmentData): EquipmentData {
        val profile = CombatBalanceProfileRegistry.current()
        if (data.balanceVersion >= profile.version) {
            return data.normalized()
        }

        return data.copy(
            mainStat = data.mainStat.copy(value = data.mainStat.value * profile.artifactMainRollScalar),
            subStats = data.subStats.map { it.copy(value = it.value * profile.artifactSubRollScalar) },
            balanceVersion = profile.version
        ).normalized()
    }

    fun markCurrent(data: EquipmentData): EquipmentData {
        val version = CombatBalanceProfileRegistry.current().version
        return if (data.balanceVersion >= version) data.normalized() else data.copy(balanceVersion = version).normalized()
    }
}
