package hifumi.cresora.npc

import hifumi.cresora.CreSoraUtilities
import net.fabricmc.fabric.api.`object`.builder.v1.entity.FabricEntityTypeBuilder
import net.minecraft.entity.EntityDimensions
import net.minecraft.entity.EntityType
import net.minecraft.entity.SpawnGroup
import net.minecraft.entity.attribute.DefaultAttributeContainer
import net.minecraft.entity.attribute.EntityAttributes
import net.minecraft.entity.data.DataTracker
import net.minecraft.entity.data.TrackedData
import net.minecraft.entity.data.TrackedDataHandlerRegistry
import net.minecraft.entity.passive.PassiveEntity
import net.minecraft.registry.RegistryKey
import net.minecraft.registry.RegistryKeys
import net.minecraft.server.world.ServerWorld
import net.minecraft.util.Identifier
import net.minecraft.world.World
import net.minecraft.world.WorldView

class SpiritGuideEntity(
    type: EntityType<out PassiveEntity>,
    world: World
) : PassiveEntity(type, world) {
    companion object {
        private val DIALOGUE_TREE_ID: TrackedData<String> =
            DataTracker.registerData(SpiritGuideEntity::class.java, TrackedDataHandlerRegistry.STRING)
        private const val DEFAULT_TREE_ID = "spirit_guide_intro"

        val ENTITY_KEY = RegistryKey.of(RegistryKeys.ENTITY_TYPE, Identifier.of(CreSoraUtilities.MOD_ID, "spirit_guide"))
        val ENTITY_TYPE: EntityType<SpiritGuideEntity> = FabricEntityTypeBuilder.create(
            SpawnGroup.MISC, ::SpiritGuideEntity
        ).dimensions(EntityDimensions.fixed(0.6f, 1.8f)).trackRangeChunks(8)
            .build(ENTITY_KEY)

        fun createAttributes(): DefaultAttributeContainer {
            return createMobAttributes()
                .add(EntityAttributes.MAX_HEALTH, 20.0)
                .add(EntityAttributes.MOVEMENT_SPEED, 0.0)
                .add(EntityAttributes.KNOCKBACK_RESISTANCE, 1.0)
                .build()
        }
    }

    override fun initDataTracker(builder: DataTracker.Builder) {
        super.initDataTracker(builder)
        builder.add(DIALOGUE_TREE_ID, DEFAULT_TREE_ID)
    }

    fun getDialogueTreeId(): String = dataTracker.get(DIALOGUE_TREE_ID)

    fun setDialogueTreeId(treeId: String) {
        dataTracker.set(DIALOGUE_TREE_ID, treeId)
    }

    override fun createChild(world: ServerWorld, parent: PassiveEntity): PassiveEntity? = null

    override fun isPushable(): Boolean = false

    override fun canImmediatelyDespawn(distance: Double): Boolean = false

    override fun canSpawn(world: WorldView): Boolean = true

    override fun shouldSave(): Boolean = false
}
