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

    /**
     * プレイヤーごとに動的に座標を割り当てる (1024ブロックおき)
     */
    fun getArenaPosForPlayer(playerUuid: UUID): BlockPos {
        val index = (playerUuid.mostSignificantBits and 0xFFFF).toLong()
        return BlockPos((index * 1024).toInt(), 128, 0)
    }

    fun getDomainWorld(server: MinecraftServer): ServerWorld? {
        return server.getWorld(DOMAIN_WORLD_KEY)
    }

    /**
     * アリーナを構築する。NBTファイルが指定されていればそれをロードし、
     * なければ従来の石ブロックによるフォールバックを配置する。
     */
    fun ensureArena(world: ServerWorld, center: BlockPos, structureId: String? = null) {
        val server = world.server
        if (structureId != null) {
            val templateManager = server.structureTemplateManager
            val templatePath = Identifier.of(CreSoraUtilities.MOD_ID, structureId)
            val template = templateManager.getTemplate(templatePath)

            if (template.isPresent) {
                val size = template.get().size
                // 中央配置にするためのオフセット計算
                val origin = center.subtract(Vec3i(size.x / 2, 0, size.z / 2))
                val placementData = StructurePlacementData().setRotation(BlockRotation.NONE)
                template.get().place(world, origin, origin, placementData, world.random, 2)
                return
            }
        }

        // フォールバック: 従来のプログラム生成アリーナ
        ensureFallbackArena(world, center)
    }

    private fun ensureFallbackArena(world: ServerWorld, center: BlockPos) {
        val radius = 7
        val height = 6 // アリーナの高さ
        for (x in -radius..radius) {
            for (z in -radius..radius) {
                val floorPos = center.add(x, 0, z)
                val ceilingPos = center.add(x, height, z)
                val isBorder = kotlin.math.abs(x) == radius || kotlin.math.abs(z) == radius
                
                // 地面
                world.setBlockState(floorPos, if (isBorder) Blocks.POLISHED_DEEPSLATE.defaultState else Blocks.SMOOTH_STONE.defaultState)
                
                // 天井 (ゾンビの炎上防止)
                world.setBlockState(ceilingPos, Blocks.SMOOTH_STONE.defaultState)
                
                // 空間 (境界はガラスの壁)
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
        // 照明の配置
        world.setBlockState(center, Blocks.SEA_LANTERN.defaultState) // 床中央
        world.setBlockState(center.up(height - 1), Blocks.SEA_LANTERN.defaultState) // 天井中央付近
    }
}
