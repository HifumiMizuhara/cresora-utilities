package hifumi.cresora.weapon

import com.mojang.serialization.Codec

enum class WeaponRole(val id: String) {
    VANGUARD("vanguard"),
    GUARD("guard"),
    DEFENDER("defender"),
    SNIPER("sniper"),
    CASTER("caster"),
    MEDIC("medic"),
    SUPPORTER("supporter"),
    SPECIALIST("specialist"),
    CATALYST("catalyst");

    fun translationKey(): String = "item.cresora.weapon.role.$id"

    companion object {
        val CODEC: Codec<WeaponRole> = Codec.STRING.xmap(
            { value -> entries.firstOrNull { it.id == value.lowercase() } ?: error("Unknown weapon role: $value") },
            WeaponRole::id
        )
    }
}
