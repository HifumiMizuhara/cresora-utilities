package hifumi.cresora.combat
import hifumi.cresora.CreSoraUtilities
import net.minecraft.block.Blocks
import net.minecraft.registry.RegistryKey
import net.minecraft.registry.RegistryKeys
import net.minecraft.server.MinecraftServer
import net.minecraft.server.world.ServerWorld
import net.minecraft.structure.StructurePlacementData
import net.minecraft.util.Identifier
import net.minecraft.util.math.BlockPos
import net.minecraft.util.BlockRotation
import net.minecraft.util.math.Vec3i
import net.minecraft.world.World
import java.util.*

object ArenaManager {
    val DOMAIN_WORLD_KEY: RegistryKey<World> = RegistryKey.of(RegistryKeys.WORLD, Identifier.of(CreSoraUtilities.MOD_ID, "domain"))
    val DOMAIN_VERDANT_WORLD_KEY: RegistryKey<World> = RegistryKey.of(RegistryKeys.WORLD, Identifier.of(CreSoraUtilities.MOD_ID, "domain_verdant"))
    val DOMAIN_METAL_WORLD_KEY: RegistryKey<World> = RegistryKey.of(RegistryKeys.WORLD, Identifier.of(CreSoraUtilities.MOD_ID, "domain_metal"))
    val DOMAIN_MOONLIT_WORLD_KEY: RegistryKey<World> = RegistryKey.of(RegistryKeys.WORLD, Identifier.of(CreSoraUtilities.MOD_ID, "domain_moonlit"))
    val DOMAIN_ARCANE_WORLD_KEY: RegistryKey<World> = RegistryKey.of(RegistryKeys.WORLD, Identifier.of(CreSoraUtilities.MOD_ID, "domain_arcane"))

    private val themedWorldKeys = setOf(
        DOMAIN_VERDANT_WORLD_KEY,
        DOMAIN_METAL_WORLD_KEY,
        DOMAIN_MOONLIT_WORLD_KEY,
        DOMAIN_ARCANE_WORLD_KEY
    )

    fun isDomainWorld(registryKey: RegistryKey<World>): Boolean =
        registryKey == DOMAIN_WORLD_KEY || registryKey in themedWorldKeys

    private fun worldKeyForTheme(themeId: String): RegistryKey<World> = when (themeId) {
        "verdant" -> DOMAIN_VERDANT_WORLD_KEY
        "metal" -> DOMAIN_METAL_WORLD_KEY
        "moonlit" -> DOMAIN_MOONLIT_WORLD_KEY
        "arcane" -> DOMAIN_ARCANE_WORLD_KEY
        else -> DOMAIN_WORLD_KEY
    }

    /**
     * プレイヤーごとに動的に座標を割り当てる (1024ブロックおき)
     * テーマ付きディメンションはすべて地上 (y=5)、未設定は従来の空中 (y=128)
     */
    fun getArenaPosForPlayer(playerUuid: UUID, themeId: String = ""): BlockPos {
        val index = (playerUuid.mostSignificantBits and 0xFFFF).toLong()
        val y = if (themeId.isNotEmpty()) 5 else 128
        return BlockPos((index * 1024).toInt(), y, 0)
    }

    fun getDomainWorld(server: MinecraftServer, themeId: String = ""): ServerWorld? {
        return server.getWorld(worldKeyForTheme(themeId))
    }

    /**
     * アリーナを構築する。NBTファイルが指定されていればそれをロードし、
     * なければテーマ別のフォールバックアリーナを配置する。
     */
    fun ensureArena(world: ServerWorld, center: BlockPos, themeId: String = "") {
        val server = world.server
        val structurePath = Identifier.of(CreSoraUtilities.MOD_ID, "domains/$themeId")
        val template = server.structureTemplateManager.getTemplate(structurePath)
        if (template.isPresent) {
            val size = template.get().size
            val origin = center.subtract(Vec3i(size.x / 2, 0, size.z / 2))
            val placementData = StructurePlacementData().setRotation(BlockRotation.NONE)
            template.get().place(world, origin, origin, placementData, world.random, 2)
            return
        }

        ensureFallbackArena(world, center, themeId)
    }

    private fun ensureFallbackArena(world: ServerWorld, center: BlockPos, themeId: String = "") {
        val radius = 7
        val height = 6
        val (floorBlock, borderBlock, lightBlock) = when (themeId) {
            "verdant" -> Triple(Blocks.MOSS_BLOCK, Blocks.MOSSY_COBBLESTONE, Blocks.SHROOMLIGHT)
            "metal" -> Triple(Blocks.BLACKSTONE, Blocks.POLISHED_BASALT, Blocks.GLOWSTONE)
            "moonlit" -> Triple(Blocks.PURPUR_BLOCK, Blocks.END_STONE_BRICKS, Blocks.SEA_LANTERN)
            "arcane" -> Triple(Blocks.DEEPSLATE, Blocks.OBSIDIAN, Blocks.SEA_LANTERN)
            else -> Triple(Blocks.SMOOTH_STONE, Blocks.POLISHED_DEEPSLATE, Blocks.SEA_LANTERN)
        }

        for (x in -radius..radius) {
            for (z in -radius..radius) {
                val floorPos = center.add(x, 0, z)
                val ceilingPos = center.add(x, height, z)
                val isBorder = kotlin.math.abs(x) == radius || kotlin.math.abs(z) == radius

                world.setBlockState(floorPos, if (isBorder) borderBlock.defaultState else floorBlock.defaultState)
                world.setBlockState(ceilingPos, floorBlock.defaultState)

                for (y in 1 until height) {
                    val airPos = floorPos.up(y)
                    if (isBorder && y <= 2) {
                        world.setBlockState(airPos, Blocks.TINTED_GLASS.defaultState)
                    } else {
                        world.setBlockState(airPos, Blocks.AIR.defaultState)
                    }
                }
            }
        }
        world.setBlockState(center, lightBlock.defaultState)
        world.setBlockState(center.up(height - 1), lightBlock.defaultState)
    }
}
