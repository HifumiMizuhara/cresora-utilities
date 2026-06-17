package hifumi.cresora.world

import hifumi.cresora.CreSoraUtilities
import hifumi.cresora.npc.SpiritGuideEntity
import hifumi.cresora.story.StoryDialogueNetworking
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents
import net.fabricmc.fabric.api.event.player.UseEntityCallback
import net.minecraft.block.Blocks
import net.minecraft.entity.SpawnReason
import net.minecraft.particle.ParticleTypes
import net.minecraft.server.network.ServerPlayerEntity
import net.minecraft.server.world.ServerWorld
import net.minecraft.text.Text
import net.minecraft.util.ActionResult
import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.Box

object SpiritGuideService {
    val LANDING_POS: BlockPos = BlockPos(0, 72, 0)

    private val guidePos: BlockPos = LANDING_POS.add(0, 0, 5)
    private const val GUIDE_INTERVAL_TICKS = 100L
    private const val LANDING_INTERVAL_TICKS = 1200L
    private var nextGuideTick: Long = 0L
    private var nextLandingTick: Long = 0L

    fun init() {
        UseEntityCallback.EVENT.register(UseEntityCallback { player, world, _, entity, _ ->
            if (world.isClient || !CresoraWorldKeys.isCresoraWorld(world.registryKey)) {
                return@UseEntityCallback ActionResult.PASS
            }
            if (entity !is SpiritGuideEntity) {
                return@UseEntityCallback ActionResult.PASS
            }
            val serverPlayer = player as? ServerPlayerEntity ?: return@UseEntityCallback ActionResult.PASS
            StoryDialogueNetworking.startNpcDialogue(serverPlayer, entity.getDialogueTreeId())
            ActionResult.SUCCESS
        })

        ServerTickEvents.END_SERVER_TICK.register { server ->
            val now = server.overworld.time
            val needsGuide = now >= nextGuideTick
            val needsLanding = now >= nextLandingTick
            if (!needsGuide && !needsLanding) {
                return@register
            }
            val world = server.getWorld(CresoraWorldKeys.CRESORA_WORLD) ?: return@register
            val chunkX = LANDING_POS.x shr 4
            val chunkZ = LANDING_POS.z shr 4
            if (!world.chunkManager.isChunkLoaded(chunkX, chunkZ)) {
                return@register
            }
            if (needsLanding) {
                nextLandingTick = now + LANDING_INTERVAL_TICKS
                ensureLanding(world)
            }
            if (needsGuide) {
                nextGuideTick = now + GUIDE_INTERVAL_TICKS
                ensureGuide(world)
            }
        }
    }

    fun ensureLanding(world: ServerWorld) {
        if (!CresoraWorldKeys.isCresoraWorld(world.registryKey)) {
            return
        }

        val floorY = LANDING_POS.y - 1
        for (x in -4..4) {
            for (z in -4..4) {
                val floorPos = BlockPos(LANDING_POS.x + x, floorY, LANDING_POS.z + z)
                val polishedState = Blocks.POLISHED_DEEPSLATE.defaultState
                if (world.getBlockState(floorPos) != polishedState) {
                    world.setBlockState(floorPos, polishedState)
                }
                for (y in 0..3) {
                    val airPos = floorPos.up(y + 1)
                    val airState = Blocks.AIR.defaultState
                    if (world.getBlockState(airPos) != airState) {
                        world.setBlockState(airPos, airState)
                    }
                }
            }
        }
        val portalPos = LANDING_POS.add(2, 0, 0)
        val portalState = CreSoraUtilities.CRESORA_PORTAL_BLOCK.defaultState
        if (world.getBlockState(portalPos) != portalState) {
            world.setBlockState(portalPos, portalState)
        }
        val lanternPos = LANDING_POS.add(-2, 0, 0)
        val lanternState = Blocks.SEA_LANTERN.defaultState
        if (world.getBlockState(lanternPos) != lanternState) {
            world.setBlockState(lanternPos, lanternState)
        }
        val guideUnderPos = guidePos.down()
        val guideUnderState = Blocks.CHISELED_DEEPSLATE.defaultState
        if (world.getBlockState(guideUnderPos) != guideUnderState) {
            world.setBlockState(guideUnderPos, guideUnderState)
        }
    }

    fun ensureGuide(world: ServerWorld) {
        val box = Box(guidePos).expand(8.0)

        // Clean up legacy VillagerEntity guides from previous versions
        val legacyGuides = world.getEntitiesByClass(net.minecraft.entity.passive.VillagerEntity::class.java, box) { true }
        for (legacy in legacyGuides) {
            legacy.discard()
        }

        val existing = world.getEntitiesByClass(SpiritGuideEntity::class.java, box) { it.isAlive }
        if (existing.isNotEmpty()) {
            val guide = existing.first()
            guide.refreshPositionAndAngles(guidePos.x + 0.5, guidePos.y.toDouble(), guidePos.z + 0.5, 180.0f, 0.0f)
            // Discard any duplicate guide entities in the area to keep it clean
            for (i in 1 until existing.size) {
                existing[i].discard()
            }
            return
        }

        val guide = SpiritGuideEntity.ENTITY_TYPE.create(world, SpawnReason.EVENT) ?: return
        guide.refreshPositionAndAngles(guidePos.x + 0.5, guidePos.y.toDouble(), guidePos.z + 0.5, 180.0f, 0.0f)
        guide.customName = Text.translatable("entity.cresora.spirit_guide")
        guide.isCustomNameVisible = true
        guide.setAiDisabled(true)
        guide.setInvulnerable(true)
        world.spawnEntity(guide)
        world.spawnParticles(ParticleTypes.ENCHANT, guide.x, guide.y + 1.1, guide.z, 24, 0.5, 0.8, 0.5, 0.02)
    }
}
