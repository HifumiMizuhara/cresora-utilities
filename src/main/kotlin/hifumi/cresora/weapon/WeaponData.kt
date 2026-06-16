package hifumi.cresora.weapon
import com.mojang.serialization.Codec
import com.mojang.serialization.codecs.RecordCodecBuilder

data class WeaponData(
    val weaponId: String,
    val rarity: WeaponRarity,
    val baseLevel: Int,
    val skillLevel: Int,
    val breakthrough: Int = 0,
    val spiritBondStage: Int = 1
) {
    fun normalized(): WeaponData {
        return copy(
            weaponId = weaponId.ifBlank { "rondo_melody" },
            baseLevel = baseLevel.coerceIn(1, 80),
            skillLevel = skillLevel.coerceIn(1, 10),
            breakthrough = breakthrough.coerceIn(0, 2),
            spiritBondStage = spiritBondStage.coerceIn(1, 6)
        )
    }

    fun normalized(definition: WeaponDefinition): WeaponData {
        val bt = breakthrough.coerceIn(0, 2)
        val maxBase = WeaponUpgradeService.levelCap(definition, bt)
        val maxSkill = WeaponUpgradeService.maxSkillLevelForBreakthrough(bt).coerceAtMost(definition.maxSkillLevel)

        return copy(
            weaponId = definition.id,
            baseLevel = baseLevel.coerceIn(1, maxBase),
            skillLevel = skillLevel.coerceIn(1, maxSkill),
            breakthrough = bt,
            spiritBondStage = spiritBondStage.coerceIn(1, 6)
        )
    }

    companion object {
        val CODEC: Codec<WeaponData> = RecordCodecBuilder.create { instance ->
            instance.group(
                Codec.STRING.fieldOf("weaponId").forGetter(WeaponData::weaponId),
                WeaponRarity.CODEC.fieldOf("rarity").forGetter(WeaponData::rarity),
                Codec.INT.fieldOf("baseLevel").forGetter(WeaponData::baseLevel),
                Codec.INT.fieldOf("skillLevel").forGetter(WeaponData::skillLevel),
                Codec.INT.optionalFieldOf("breakthrough", 0).forGetter(WeaponData::breakthrough),
                Codec.INT.optionalFieldOf("spiritBondStage", 1).forGetter(WeaponData::spiritBondStage)
            ).apply(instance) { id, rarity, base, skill, bt, bondStage ->
                WeaponData(id, rarity, base, skill, bt, bondStage)
            }
        }

        val DUMMY = WeaponData("dummy", WeaponRarity.TWO_STAR, 1, 1, 0, 1)
    }
}
