package hifumi.cresora.weapon
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

    object DUMMY : WeaponSkillAccess {
        override fun cresoraGetShieldHp(): Float = 0.0f
        override fun cresoraSetShieldHp(value: Float) {}
        override fun cresoraGetShieldExpireTick(): Long = 0L
        override fun cresoraSetShieldExpireTick(value: Long) {}
        override fun cresoraGetShieldWeaponId(): String? = null
        override fun cresoraSetShieldWeaponId(value: String?) {}
        override fun cresoraGetSkillCooldownExpireTick(): Long = 0L
        override fun cresoraSetSkillCooldownExpireTick(value: Long) {}
        override fun cresoraGetSkillCooldownWeaponId(): String? = null
        override fun cresoraSetSkillCooldownWeaponId(value: String?) {}
    }
}
