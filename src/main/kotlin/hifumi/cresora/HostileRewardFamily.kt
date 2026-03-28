package hifumi.cresora

import net.minecraft.entity.EntityType

enum class HostileRewardFamily {
    SURVIVOR,
    ASSAULT,
    ARCANE,
    ELITE,
    RELIC
}

object HostileRewardFamilies {
    fun classify(entityType: EntityType<*>): HostileRewardFamily {
        return when (entityType) {
            EntityType.ZOMBIE,
            EntityType.DROWNED,
            EntityType.HUSK,
            EntityType.SPIDER,
            EntityType.CAVE_SPIDER,
            EntityType.SLIME,
            EntityType.SILVERFISH,
            EntityType.ENDERMITE -> HostileRewardFamily.SURVIVOR

            EntityType.SKELETON,
            EntityType.STRAY,
            EntityType.CREEPER,
            EntityType.PHANTOM,
            EntityType.PILLAGER,
            EntityType.BREEZE,
            EntityType.ZOMBIFIED_PIGLIN -> HostileRewardFamily.ASSAULT

            EntityType.WITCH,
            EntityType.BLAZE,
            EntityType.MAGMA_CUBE,
            EntityType.GHAST,
            EntityType.GUARDIAN -> HostileRewardFamily.ARCANE

            EntityType.ENDERMAN,
            EntityType.HOGLIN,
            EntityType.VINDICATOR,
            EntityType.EVOKER,
            EntityType.RAVAGER,
            EntityType.PIGLIN_BRUTE,
            EntityType.ELDER_GUARDIAN,
            EntityType.SHULKER -> HostileRewardFamily.ELITE

            EntityType.WARDEN -> HostileRewardFamily.RELIC
            else -> HostileRewardFamily.ASSAULT
        }
    }
}
