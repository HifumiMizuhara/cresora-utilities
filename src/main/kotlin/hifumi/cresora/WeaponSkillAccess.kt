package hifumi.cresora

interface WeaponSkillAccess {
    fun cresoraGetShieldHp(): Float
    fun cresoraSetShieldHp(value: Float)
    fun cresoraGetShieldExpireTick(): Long
    fun cresoraSetShieldExpireTick(value: Long)
    fun cresoraGetShieldWeaponId(): String?
    fun cresoraSetShieldWeaponId(value: String?)
    fun cresoraGetSkillCooldownExpireTick(): Long
    fun cresoraSetSkillCooldownExpireTick(value: Long)
    fun cresoraGetSkillCooldownWeaponId(): String?
    fun cresoraSetSkillCooldownWeaponId(value: String?)
}
