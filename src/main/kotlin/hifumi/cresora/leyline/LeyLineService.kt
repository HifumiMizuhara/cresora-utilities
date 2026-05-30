package hifumi.cresora.leyline

import hifumi.cresora.CreSoraUtilities
import hifumi.cresora.InventoryGate
import hifumi.cresora.adventurerank.AdventureRankMobAccess
import hifumi.cresora.adventurerank.AdventureRankService
import hifumi.cresora.combat.FieldMobPackService
import hifumi.cresora.credits.CreditsService
import hifumi.cresora.domain.DomainCombatProfile
import hifumi.cresora.domain.DomainDisplayStackFactory
import hifumi.cresora.domain.DomainRewardResult
import hifumi.cresora.equipment.ArtifactSpecialItem
import hifumi.cresora.equipment.ArtifactUiFlow
import hifumi.cresora.equipment.EquipmentContentRegistry
import hifumi.cresora.equipment.EquipmentGenerationService
import hifumi.cresora.equipment.EquipmentRarity
import hifumi.cresora.equipment.EquipmentStackSupport
import net.minecraft.block.Blocks
import net.minecraft.entity.Entity
import net.minecraft.entity.EntityType
import net.minecraft.entity.SpawnReason
import net.minecraft.entity.effect.StatusEffectInstance
import net.minecraft.entity.effect.StatusEffects
import net.minecraft.entity.mob.HostileEntity
import net.minecraft.entity.mob.MobEntity
import net.minecraft.item.ItemStack
import net.minecraft.particle.ParticleTypes
import net.minecraft.registry.RegistryKey
import net.minecraft.server.MinecraftServer
import net.minecraft.server.network.ServerPlayerEntity
import net.minecraft.server.world.ServerWorld
import net.minecraft.text.Text
import net.minecraft.util.Identifier
import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.Box
import net.minecraft.util.math.Vec3d
import net.minecraft.world.World
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

data class LeyLineStartResult(
    val success: Boolean,
    val translationKey: String,
    val args: List<Any> = emptyList()
)

private class PendingLeyLine(
    val pos: BlockPos,
    val placerUuid: UUID,
    val element: LeyLineElement,
    val expiresAtTick: Long,
    val worldKey: RegistryKey<World>
)

private class LeyLineEventSession(
    val pos: BlockPos,
    val element: LeyLineElement,
    val tier: Int,
    val placerUuid: UUID,
    val worldKey: RegistryKey<World>
) {
    val spawnedMobUuids = mutableSetOf<UUID>()
    val mobInitialPositions = mutableMapOf<UUID, BlockPos>()
    var wave = 1
    var wavePreparing = true
    var nextSpawnTick = 0L
    var ticksWithoutPlayers = 0
}

object LeyLineService {
    private val pendingLeyLines = ConcurrentHashMap<BlockPos, PendingLeyLine>()
    private val activeSessions = ConcurrentHashMap<BlockPos, LeyLineEventSession>()
    private val mobRuntime = ConcurrentHashMap<UUID, Double>()

    fun placeLeyLine(
        world: ServerWorld,
        pos: BlockPos,
        player: ServerPlayerEntity,
        element: LeyLineElement
    ): Boolean {
        // Clear any existing at the position
        pendingLeyLines.remove(pos)
        activeSessions.remove(pos)

        val state = CreSoraUtilities.LEY_LINE_OVERFLOW_BLOCK.defaultState.with(LeyLineOverflowBlock.ELEMENT, element)
        world.setBlockState(pos, state)

        pendingLeyLines[pos] = PendingLeyLine(
            pos = pos,
            placerUuid = player.uuid,
            element = element,
            expiresAtTick = world.time + 1000, // 50 seconds
            worldKey = world.registryKey
        )

        player.sendMessage(Text.translatable("message.cresora.leyline.placed"), false)
        return true
    }

    fun onBlockRemoved(world: World, pos: BlockPos) {
        val pending = pendingLeyLines.remove(pos) ?: return
        refundKey(world, pos, pending.placerUuid, pending.element)
    }

    fun openSelectionGui(player: ServerPlayerEntity, pos: BlockPos, element: LeyLineElement) {
        player.openHandledScreen(object : net.minecraft.screen.NamedScreenHandlerFactory {
            override fun getDisplayName(): Text = Text.translatable("screen.cresora.leyline.selection_title")

            override fun createMenu(
                syncId: Int,
                playerInventory: net.minecraft.entity.player.PlayerInventory,
                player: net.minecraft.entity.player.PlayerEntity
            ): net.minecraft.screen.ScreenHandler {
                val handler = LeyLineSelectionScreenHandler(syncId, playerInventory)
                val properties = handler.properties
                properties.set(LeyLineSelectionScreenHandler.PROPERTY_ELEMENT, element.ordinal)
                properties.set(LeyLineSelectionScreenHandler.PROPERTY_POS_X, pos.x)
                properties.set(LeyLineSelectionScreenHandler.PROPERTY_POS_Y, pos.y)
                properties.set(LeyLineSelectionScreenHandler.PROPERTY_POS_Z, pos.z)
                return handler
            }
        })
    }

    fun startEvent(player: ServerPlayerEntity, pos: BlockPos, element: LeyLineElement, tier: Int): LeyLineStartResult {
        val unlockRank = when (tier) {
            1 -> 1
            2 -> 10
            3 -> 20
            4 -> 35
            else -> 1
        }
        val rank = AdventureRankService.getRank(player)
        if (rank < unlockRank) {
            return LeyLineStartResult(false, "screen.cresora.domain.locked", listOf(unlockRank))
        }

        if (!InventoryGate.hasFreeMainSlot(player)) {
            return LeyLineStartResult(false, "screen.cresora.domain.inventory_full", listOf(1))
        }

        val world = player.world as ServerWorld
        val state = world.getBlockState(pos)
        if (!state.isOf(CreSoraUtilities.LEY_LINE_OVERFLOW_BLOCK)) {
            return LeyLineStartResult(false, "screen.cresora.leyline.invalid_block")
        }

        if (activeSessions.containsKey(pos)) {
            return LeyLineStartResult(false, "screen.cresora.leyline.already_started")
        }

        // Remove from pending so we don't refund key when replacing the block with air
        pendingLeyLines.remove(pos)
        world.setBlockState(pos, Blocks.AIR.defaultState)

        val session = LeyLineEventSession(
            pos = pos,
            element = element,
            tier = tier,
            placerUuid = player.uuid,
            worldKey = world.registryKey
        )
        session.nextSpawnTick = world.time + 40 // 2 seconds delay
        activeSessions[pos] = session

        player.sendMessage(Text.translatable("message.cresora.leyline.started"), false)
        return LeyLineStartResult(true, "")
    }

    fun tick(server: MinecraftServer) {
        // 1. Tick pending registrations (Expiration check)
        val timeOverworld = server.overworld.time
        val pendingIterator = pendingLeyLines.values.iterator()
        while (pendingIterator.hasNext()) {
            val pending = pendingIterator.next()
            val world = server.getWorld(pending.worldKey) ?: server.overworld
            if (world.time >= pending.expiresAtTick) {
                pendingIterator.remove()
                world.setBlockState(pending.pos, Blocks.AIR.defaultState)
                refundKey(world, pending.pos, pending.placerUuid, pending.element)
            }
        }

        // 2. Tick active combat sessions
        val sessionIterator = activeSessions.values.iterator()
        while (sessionIterator.hasNext()) {
            val session = sessionIterator.next()
            val world = server.getWorld(session.worldKey)
            if (world == null) {
                sessionIterator.remove()
                continue
            }

            val box = Box(session.pos).expand(5.0, 3.0, 5.0)
            val nearbyPlayers = world.getPlayers { it.isAlive && it.boundingBox.intersects(box) }

            if (nearbyPlayers.isNotEmpty()) {
                session.ticksWithoutPlayers = 0
                // Spawn boundary particles every second
                if (world.time % 20L == 0L) {
                    spawnBoundaryParticles(world, session.pos, session.element)
                }
            } else {
                session.ticksWithoutPlayers++
                if (session.ticksWithoutPlayers % 20 == 0) {
                    val remainingSeconds = 10 - (session.ticksWithoutPlayers / 20)
                    if (remainingSeconds > 0) {
                        val warningText = Text.translatable("message.cresora.leyline.leave_warning", remainingSeconds)
                        // Warn placer
                        val placer = server.playerManager.getPlayer(session.placerUuid)
                        if (placer != null && placer.world == world) {
                            placer.sendMessage(warningText, true)
                        }
                        // Warn other players within 40 blocks
                        val playersToWarn = world.getPlayers {
                            it.isAlive && it.uuid != session.placerUuid &&
                            it.squaredDistanceTo(session.pos.x + 0.5, session.pos.y + 0.5, session.pos.z + 0.5) < 1600.0
                        }
                        for (p in playersToWarn) {
                            p.sendMessage(warningText, true)
                        }
                    }
                }
                if (session.ticksWithoutPlayers >= 200) { // 10 seconds empty
                    sessionIterator.remove()
                    // Discard remaining mobs
                    for (mobUuid in session.spawnedMobUuids) {
                        world.getEntity(mobUuid)?.discard()
                        mobRuntime.remove(mobUuid)
                        session.mobInitialPositions.remove(mobUuid)
                    }
                    val placer = server.playerManager.getPlayer(session.placerUuid)
                    placer?.sendMessage(Text.translatable("message.cresora.leyline.failed_leave"), false)
                    continue
                }
            }

            if (session.wavePreparing) {
                if (world.time >= session.nextSpawnTick) {
                    spawnWave(world, session, nearbyPlayers.firstOrNull() ?: server.playerManager.getPlayer(session.placerUuid))
                }
            } else {
                // Monitor wave mobs
                val mobIterator = session.spawnedMobUuids.iterator()
                while (mobIterator.hasNext()) {
                    val uuid = mobIterator.next()
                    val entity = world.getEntity(uuid)
                    if (entity == null || !entity.isAlive || entity.isRemoved) {
                        mobRuntime.remove(uuid)
                        session.mobInitialPositions.remove(uuid)
                        mobIterator.remove()
                    } else {
                        // Teleport mobs back if they wander out of bounds
                        val mobBox = Box(session.pos).expand(8.0, 4.0, 8.0)
                        if (!mobBox.contains(entity.x, entity.y, entity.z)) {
                            val initialPos = session.mobInitialPositions[uuid]
                            if (initialPos != null) {
                                (entity as? MobEntity)?.let { hostile ->
                                    hostile.requestTeleport(
                                        initialPos.x + 0.5,
                                        initialPos.y.toDouble(),
                                        initialPos.z + 0.5
                                    )
                                    hostile.setVelocity(Vec3d.ZERO)
                                    hostile.navigation.stop()
                                    val target = nearbyPlayers.firstOrNull() ?: server.playerManager.getPlayer(session.placerUuid)
                                    if (target != null) {
                                        hostile.target = target
                                    }
                                }
                            }
                        }
                    }
                }

                if (session.spawnedMobUuids.isEmpty()) {
                    if (session.wave == 1) {
                        session.wave = 2
                        session.wavePreparing = true
                        session.nextSpawnTick = world.time + 40
                    } else {
                        // Success!
                        sessionIterator.remove()
                        val completer = nearbyPlayers.firstOrNull() ?: server.playerManager.getPlayer(session.placerUuid)
                        if (completer != null) {
                            completeEvent(world, session, completer)
                        }
                    }
                }
            }
        }
    }

    fun onPlayerDeath(player: ServerPlayerEntity) {
        // If the player is the only participant in a nearby session, it will naturally timeout due to ticksWithoutPlayers.
        // We do not force instant-failure on player death here to allow multiplayer helpers to continue.
    }

    fun damageMultiplier(attacker: Entity?): Double {
        val uuid = attacker?.uuid ?: return 1.0
        return mobRuntime[uuid] ?: 1.0
    }

    // PendingLeyLine stores worldKey directly, helper removed

    private fun refundKey(world: World, pos: BlockPos, playerUuid: UUID, element: LeyLineElement) {
        val player = world.server?.playerManager?.getPlayer(playerUuid)
        val keyStack = ItemStack(CreSoraUtilities.LEY_LINE_KEYS[element] ?: return)
        if (player != null) {
            player.inventory.offerOrDrop(keyStack)
            player.sendMessage(Text.translatable("message.cresora.leyline.refunded"), false)
        } else {
            val itemEntity = net.minecraft.entity.ItemEntity(world, pos.x + 0.5, pos.y + 0.5, pos.z + 0.5, keyStack)
            world.spawnEntity(itemEntity)
        }
    }

    private fun spawnWave(world: ServerWorld, session: LeyLineEventSession, targetPlayer: ServerPlayerEntity?) {
        val random = world.random
        val spawnRank = when (session.tier) {
            1 -> 1
            2 -> 10
            3 -> 20
            4 -> 35
            else -> 1
        }
        val target = targetPlayer ?: return

        // Spawn 4 normal mobs
        val normalScaling = DomainCombatProfile.scaling(spawnRank, false)
        repeat(4) { index ->
            val type = getNormalMobType(session.element, random)
            val offsetVal = 2.5 + random.nextDouble() * 2.0
            val angle = random.nextDouble() * Math.PI * 2.0
            val spawnPos = BlockPos.ofFloored(
                session.pos.x + 0.5 + Math.cos(angle) * offsetVal,
                session.pos.y.toDouble() + 0.5,
                session.pos.z + 0.5 + Math.sin(angle) * offsetVal
            )
            val hostile = type.spawn(world, null, spawnPos, SpawnReason.EVENT, true, false) as? MobEntity ?: return@repeat
            val access = hostile as? AdventureRankMobAccess ?: return@repeat
            access.cresoraSetMobAdventureRank(spawnRank)
            FieldMobPackService.markExplicit(hostile, false)
            AdventureRankService.applyMobScaling(hostile, spawnRank, normalScaling.healthScalar, normalScaling.defenseScalar, normalScaling.toughnessScalar)
            hostile.addStatusEffect(StatusEffectInstance(StatusEffects.GLOWING, 12000, 0, false, false))
            hostile.target = target
            session.spawnedMobUuids += hostile.uuid
            session.mobInitialPositions[hostile.uuid] = spawnPos
            mobRuntime[hostile.uuid] = normalScaling.damageScalar
        }

        // Spawn 1 elite mob
        val eliteScaling = DomainCombatProfile.scaling(spawnRank, true)
        val eliteType = getEliteMobType(session.element, random)
        val eliteSpawnPos = BlockPos.ofFloored(session.pos.x + 0.5, session.pos.y.toDouble() + 0.5, session.pos.z + 0.5)
        val eliteHostile = eliteType.spawn(world, null, eliteSpawnPos, SpawnReason.EVENT, true, false) as? MobEntity
        if (eliteHostile != null) {
            val access = eliteHostile as? AdventureRankMobAccess
            if (access != null) {
                access.cresoraSetMobAdventureRank(spawnRank)
                FieldMobPackService.markExplicit(eliteHostile, true)
                AdventureRankService.applyMobScaling(eliteHostile, spawnRank, eliteScaling.healthScalar, eliteScaling.defenseScalar, eliteScaling.toughnessScalar)
                eliteHostile.addStatusEffect(StatusEffectInstance(StatusEffects.GLOWING, 12000, 0, false, false))
                eliteHostile.setPersistent()
                eliteHostile.target = target
                session.spawnedMobUuids += eliteHostile.uuid
                session.mobInitialPositions[eliteHostile.uuid] = eliteSpawnPos
                mobRuntime[eliteHostile.uuid] = eliteScaling.damageScalar
            }
        }

        session.wavePreparing = false
        target.sendMessage(
            Text.translatable(
                "message.cresora.leyline.wave_started",
                session.wave,
                2,
                spawnRank
            ),
            false
        )
    }

    private fun completeEvent(world: ServerWorld, session: LeyLineEventSession, player: ServerPlayerEntity) {
        val reward = generateRewards(player, session.element, session.tier)

        // Deliver item rewards
        var droppedCount = 0
        for (stack in reward.items) {
            val remaining = stack.copy()
            player.inventory.insertStack(remaining)
            if (!remaining.isEmpty) {
                droppedCount += remaining.count
                player.dropItem(remaining, false)
            }
        }
        player.playerScreenHandler.sendContentUpdates()

        if (reward.credits > 0) {
            CreditsService.addCredits(player, reward.credits)
        }
        if (reward.rankXp > 0) {
            AdventureRankService.addXp(player, reward.rankXp)
        }

        player.sendMessage(Text.translatable("message.cresora.leyline.cleared"), false)
        if (droppedCount > 0) {
            player.sendMessage(
                Text.translatable("screen.cresora.domain.reward_overflow", ArtifactSpecialItem.formatWholeNumber(droppedCount)),
                false
            )
        }

        // Open Chest UI
        ArtifactUiFlow.openDomainReward(player, DomainDisplayStackFactory.rewardDisplayStacks(reward))
    }

    private fun generateRewards(player: ServerPlayerEntity, element: LeyLineElement, tier: Int): DomainRewardResult {
        val random = player.random
        val items = mutableListOf<ItemStack>()
        var credits = 0
        var rankXp = 0

        when (element) {
            LeyLineElement.WOOD -> {
                val baseCount = when (tier) {
                    1 -> random.nextBetween(2, 4)
                    2 -> random.nextBetween(4, 7)
                    3 -> random.nextBetween(7, 11)
                    4 -> random.nextBetween(12, 18)
                    else -> 2
                }
                val item = net.minecraft.registry.Registries.ITEM.get(Identifier.of(CreSoraUtilities.MOD_ID, "weapon_fragment_3_star"))
                if (item != net.minecraft.block.Blocks.AIR.asItem()) {
                    items.add(ItemStack(item, baseCount))
                }
            }
            LeyLineElement.FIRE, LeyLineElement.WATER -> {
                val baseCount = when (tier) {
                    1 -> random.nextBetween(1, 2)
                    2 -> random.nextBetween(2, 4)
                    3 -> random.nextBetween(4, 7)
                    4 -> random.nextBetween(7, 11)
                    else -> 1
                }
                val item = net.minecraft.registry.Registries.ITEM.get(Identifier.of(CreSoraUtilities.MOD_ID, "weapon_fragment_4_star"))
                if (item != net.minecraft.block.Blocks.AIR.asItem()) {
                    items.add(ItemStack(item, baseCount))
                }
            }
            LeyLineElement.GOLD -> {
                val baseCount = when (tier) {
                    1 -> 1
                    2 -> random.nextBetween(1, 2)
                    3 -> random.nextBetween(2, 4)
                    4 -> random.nextBetween(4, 6)
                    else -> 1
                }
                val item = net.minecraft.registry.Registries.ITEM.get(Identifier.of(CreSoraUtilities.MOD_ID, "weapon_fragment_5_star"))
                if (item != net.minecraft.block.Blocks.AIR.asItem()) {
                    items.add(ItemStack(item, baseCount))
                }
            }
            LeyLineElement.SUN, LeyLineElement.MOON -> {
                val setId = if (element == LeyLineElement.SUN) "hinagata" else "osananajimi"
                val candidateDefinitions = EquipmentContentRegistry.equipmentDefinitions().filter { it.setId == setId }
                if (candidateDefinitions.isNotEmpty()) {
                    val count = when (tier) {
                        1 -> random.nextBetween(1, 2)
                        2 -> random.nextBetween(1, 2)
                        3 -> random.nextBetween(1, 2)
                        4 -> 2
                        else -> 1
                    }
                    val rarity = when (tier) {
                        1 -> EquipmentRarity.THREE_STAR
                        2 -> if (random.nextDouble() < 0.3) EquipmentRarity.FOUR_STAR else EquipmentRarity.THREE_STAR
                        3 -> if (random.nextDouble() < 0.2) EquipmentRarity.FIVE_STAR else EquipmentRarity.FOUR_STAR
                        4 -> EquipmentRarity.FIVE_STAR
                        else -> EquipmentRarity.THREE_STAR
                    }
                    val minLevel = when (tier) {
                        1 -> 0
                        2 -> 2
                        3 -> 4
                        4 -> 6
                        else -> 0
                    }
                    val maxLevel = when (tier) {
                        1 -> 4
                        2 -> 8
                        3 -> 12
                        4 -> 16
                        else -> 4
                    }
                    repeat(count) {
                        val definition = candidateDefinitions[random.nextInt(candidateDefinitions.size)]
                        val item = EquipmentStackSupport.itemForDefinitionId(definition.id) ?: return@repeat
                        val level = random.nextBetween(minLevel, maxLevel)
                        val stack = ItemStack(item)
                        val data = EquipmentGenerationService.createEquipment(
                            random = random,
                            definition = definition,
                            startingLevel = level,
                            forcedRarity = rarity
                        )
                        EquipmentStackSupport.syncEquipmentData(stack, data)
                        items.add(stack)
                    }
                }
            }
            LeyLineElement.EARTH -> {
                credits = when (tier) {
                    1 -> 10000
                    2 -> 25000
                    3 -> 60000
                    4 -> 150000
                    else -> 10000
                }
                rankXp = when (tier) {
                    1 -> 200
                    2 -> 500
                    3 -> 1200
                    4 -> 3000
                    else -> 200
                }
            }
        }

        // Add base rewards for other element challenges too
        if (element != LeyLineElement.EARTH) {
            credits += when (tier) {
                1 -> 1000
                2 -> 2500
                3 -> 6000
                4 -> 15000
                else -> 1000
            }
            rankXp += when (tier) {
                1 -> 20
                2 -> 50
                3 -> 120
                4 -> 300
                else -> 20
            }
        }

        return DomainRewardResult(items, credits, rankXp)
    }

    private fun getNormalMobType(element: LeyLineElement, random: net.minecraft.util.math.random.Random): EntityType<*> {
        return when (element) {
            LeyLineElement.FIRE -> if (random.nextBoolean()) EntityType.BLAZE else EntityType.MAGMA_CUBE
            LeyLineElement.WATER -> if (random.nextBoolean()) EntityType.DROWNED else EntityType.WITCH
            LeyLineElement.WOOD -> if (random.nextBoolean()) EntityType.SPIDER else EntityType.CAVE_SPIDER
            LeyLineElement.GOLD -> if (random.nextBoolean()) EntityType.SKELETON else EntityType.STRAY
            LeyLineElement.SUN -> if (random.nextBoolean()) EntityType.ZOMBIE else EntityType.HUSK
            LeyLineElement.MOON -> if (random.nextBoolean()) EntityType.ENDERMAN else EntityType.ENDERMITE
            LeyLineElement.EARTH -> if (random.nextBoolean()) EntityType.SLIME else EntityType.SILVERFISH
        }
    }

    private fun getEliteMobType(element: LeyLineElement, random: net.minecraft.util.math.random.Random): EntityType<*> {
        return when (element) {
            LeyLineElement.FIRE -> if (random.nextBoolean()) EntityType.WITHER_SKELETON else EntityType.PIGLIN_BRUTE
            LeyLineElement.WATER -> if (random.nextBoolean()) EntityType.DROWNED else EntityType.WITCH
            LeyLineElement.WOOD -> if (random.nextBoolean()) EntityType.CREEPER else EntityType.PHANTOM
            LeyLineElement.GOLD -> if (random.nextBoolean()) EntityType.VINDICATOR else EntityType.EVOKER
            LeyLineElement.SUN -> if (random.nextBoolean()) EntityType.RAVAGER else EntityType.VINDICATOR
            LeyLineElement.MOON -> if (random.nextBoolean()) EntityType.SHULKER else EntityType.WITCH
            LeyLineElement.EARTH -> if (random.nextBoolean()) EntityType.BREEZE else EntityType.RAVAGER
        }
    }

    private fun spawnBoundaryParticles(world: ServerWorld, center: BlockPos, element: LeyLineElement) {
        val particle = getParticleType(element)
        val y = center.y + 0.15
        
        // Render 11x11 square boundaries (expand 5 blocks out in X and Z)
        for (i in -5..5) {
            world.spawnParticles(particle, center.x + i + 0.5, y, center.z - 5.0 + 0.5, 1, 0.0, 0.0, 0.0, 0.0)
            world.spawnParticles(particle, center.x + i + 0.5, y, center.z + 5.0 + 0.5, 1, 0.0, 0.0, 0.0, 0.0)
            world.spawnParticles(particle, center.x - 5.0 + 0.5, y, center.z + i + 0.5, 1, 0.0, 0.0, 0.0, 0.0)
            world.spawnParticles(particle, center.x + 5.0 + 0.5, y, center.z + i + 0.5, 1, 0.0, 0.0, 0.0, 0.0)
        }
    }

    private fun getParticleType(element: LeyLineElement): net.minecraft.particle.ParticleEffect {
        return when (element) {
            LeyLineElement.SUN -> ParticleTypes.GLOW
            LeyLineElement.MOON -> ParticleTypes.WHITE_ASH
            LeyLineElement.FIRE -> ParticleTypes.FLAME
            LeyLineElement.WATER -> ParticleTypes.SPLASH
            LeyLineElement.WOOD -> ParticleTypes.SPORE_BLOSSOM_AIR
            LeyLineElement.GOLD -> ParticleTypes.CRIT
            LeyLineElement.EARTH -> ParticleTypes.ASH
        }
    }
}
