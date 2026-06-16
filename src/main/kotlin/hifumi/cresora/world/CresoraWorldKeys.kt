package hifumi.cresora.world

import hifumi.cresora.CreSoraUtilities
import net.minecraft.registry.RegistryKey
import net.minecraft.registry.RegistryKeys
import net.minecraft.util.Identifier
import net.minecraft.world.World

object CresoraWorldKeys {
    val OVERWORLD_ALT: RegistryKey<World> = RegistryKey.of(
        RegistryKeys.WORLD,
        Identifier.of(CreSoraUtilities.MOD_ID, "overworld_alt")
    )

    fun isOverworldAlt(worldKey: RegistryKey<World>): Boolean = worldKey == OVERWORLD_ALT
}
