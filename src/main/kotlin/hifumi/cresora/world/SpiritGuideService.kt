package hifumi.cresora.world

import hifumi.cresora.CreSoraUtilities
import hifumi.cresora.story.ResolvedStoryLine
import hifumi.cresora.story.StoryDialogueNetworking
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents
import net.fabricmc.fabric.api.event.player.UseEntityCallback
import net.minecraft.block.Blocks
import net.minecraft.entity.EntityType
import net.minecraft.entity.SpawnReason
import net.minecraft.entity.passive.VillagerEntity
import net.minecraft.particle.ParticleTypes
import net.minecraft.server.network.ServerPlayerEntity
import net.minecraft.server.world.ServerWorld
import net.minecraft.text.Text
import net.minecraft.util.ActionResult
import net.minecraft.util.Formatting
import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.Box

object SpiritGuideService {
    private const val GUIDE_COMMAND_TAG = "cresora_spirit_guide"
    val LANDING_POS: BlockPos = BlockPos(0, 72, 0)

    private val guidePos: BlockPos = LANDING_POS.add(0, 0, 5)
    private var nextEnsureTick: Long = 0L

    fun init() {
        UseEntityCallback.EVENT.register(UseEntityCallback { player, world, _, entity, _ ->
            if (world.isClient || !CresoraWorldKeys.isOverworldAlt(world.registryKey)) {
                return@UseEntityCallback ActionResult.PASS
            }
            if (entity !is VillagerEntity || !isGuide(entity)) {
                return@UseEntityCallback ActionResult.PASS
            }
            val serverPlayer = player as? ServerPlayerEntity ?: return@UseEntityCallback ActionResult.PASS
            openGuideDialogue(serverPlayer)
            ActionResult.SUCCESS
        })

        ServerTickEvents.END_SERVER_TICK.register { server ->
            val now = server.overworld.time
            if (now < nextEnsureTick) {
                return@register
            }
            nextEnsureTick = now + 100L
            val world = server.getWorld(CresoraWorldKeys.OVERWORLD_ALT) ?: return@register
            ensureLanding(world)
            ensureGuide(world)
        }
    }

    fun ensureLanding(world: ServerWorld) {
        if (!CresoraWorldKeys.isOverworldAlt(world.registryKey)) {
            return
        }

        val floorY = LANDING_POS.y - 1
        for (x in -4..4) {
            for (z in -4..4) {
                val floorPos = BlockPos(LANDING_POS.x + x, floorY, LANDING_POS.z + z)
                world.setBlockState(floorPos, Blocks.POLISHED_DEEPSLATE.defaultState)
                for (y in 0..3) {
                    world.setBlockState(floorPos.up(y + 1), Blocks.AIR.defaultState)
                }
            }
        }
        world.setBlockState(LANDING_POS.add(2, 0, 0), CreSoraUtilities.CRESORA_PORTAL_BLOCK.defaultState)
        world.setBlockState(LANDING_POS.add(-2, 0, 0), Blocks.SEA_LANTERN.defaultState)
        world.setBlockState(guidePos.down(), Blocks.CHISELED_DEEPSLATE.defaultState)
    }

    fun openGuideDialogue(player: ServerPlayerEntity) {
        StoryDialogueNetworking.showSimpleDialogue(
            player,
            Text.translatable("story.cresora.phase0.title"),
            listOf(
                ResolvedStoryLine(
                    Text.translatable("story.cresora.phase0.speaker.guide"),
                    Text.translatable("story.cresora.phase0.line.0")
                ),
                ResolvedStoryLine(
                    Text.translatable("story.cresora.phase0.speaker.guide"),
                    Text.translatable("story.cresora.phase0.line.1")
                ),
                ResolvedStoryLine(
                    Text.translatable("story.cresora.phase0.speaker.spirit"),
                    Text.translatable("story.cresora.phase0.line.2").copy().formatted(Formatting.LIGHT_PURPLE)
                )
            ),
            Text.translatable("story.cresora.phase0.objective"),
            listOf(Text.translatable("story.cresora.phase0.hint"))
        )
    }

    private fun ensureGuide(world: ServerWorld) {
        val box = Box(guidePos).expand(8.0)
        val existing = world.getEntitiesByClass(VillagerEntity::class.java, box) { it.isAlive && isGuide(it) }
        if (existing.isNotEmpty()) {
            val guide = existing.first()
            guide.refreshPositionAndAngles(guidePos.x + 0.5, guidePos.y.toDouble(), guidePos.z + 0.5, 180.0f, 0.0f)
            return
        }

        val guide = EntityType.VILLAGER.create(world, SpawnReason.EVENT) ?: return
        guide.refreshPositionAndAngles(guidePos.x + 0.5, guidePos.y.toDouble(), guidePos.z + 0.5, 180.0f, 0.0f)
        guide.customName = Text.translatable("entity.cresora.spirit_guide")
        guide.isCustomNameVisible = true
        guide.addCommandTag(GUIDE_COMMAND_TAG)
        guide.setAiDisabled(true)
        guide.setInvulnerable(true)
        world.spawnEntity(guide)
        world.spawnParticles(ParticleTypes.ENCHANT, guide.x, guide.y + 1.1, guide.z, 24, 0.5, 0.8, 0.5, 0.02)
    }

    private fun isGuide(entity: VillagerEntity): Boolean {
        return entity.commandTags.contains(GUIDE_COMMAND_TAG)
    }
}
