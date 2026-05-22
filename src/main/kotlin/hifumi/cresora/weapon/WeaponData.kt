package hifumi.cresora.weapon
import com.mojang.serialization.Codec
import com.mojang.serialization.codecs.RecordCodecBuilder

data class WeaponData(
    val weaponId: String,
    val rarity: WeaponRarity,
    val baseLevel: Int,
    val skillLevel: Int
) {
    fun normalized(): WeaponData {
        return copy(
            weaponId = weaponId.ifBlank { "rondo_melody" },
            baseLevel = baseLevel.coerceIn(1, 60),
            skillLevel = skillLevel.coerceIn(1, 10)
        )
    }

    fun normalized(definition: WeaponDefinition): WeaponData {
        return copy(
            weaponId = definition.id,
            baseLevel = baseLevel.coerceIn(1, definition.maxBaseLevel),
            skillLevel = skillLevel.coerceIn(1, definition.maxSkillLevel)
        ).normalized()
    }

    companion object {
        val CODEC: Codec<WeaponData> = RecordCodecBuilder.create { instance ->
            instance.group(
                Codec.STRING.fieldOf("weaponId").forGetter(WeaponData::weaponId),
                WeaponRarity.CODEC.fieldOf("rarity").forGetter(WeaponData::rarity),
                Codec.INT.fieldOf("baseLevel").forGetter(WeaponData::baseLevel),
                Codec.INT.fieldOf("skillLevel").forGetter(WeaponData::skillLevel)
            ).apply(instance, ::WeaponData)
        }

        val DUMMY = WeaponData("dummy", WeaponRarity.TWO_STAR, 1, 1)
    }
}
