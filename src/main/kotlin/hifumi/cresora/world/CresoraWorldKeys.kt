package hifumi.cresora.world

import hifumi.cresora.CreSoraUtilities
import net.minecraft.registry.RegistryKey
import net.minecraft.registry.RegistryKeys
import net.minecraft.util.Identifier
import net.minecraft.world.World

object CresoraWorldKeys {
    val CRESORA_WORLD: RegistryKey<World> = RegistryKey.of(
        RegistryKeys.WORLD,
        Identifier.of(CreSoraUtilities.MOD_ID, "cresora_world")
    )

    fun isCresoraWorld(worldKey: RegistryKey<World>): Boolean = worldKey == CRESORA_WORLD
}
