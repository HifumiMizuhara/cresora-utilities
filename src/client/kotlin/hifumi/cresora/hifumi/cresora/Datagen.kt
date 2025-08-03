package hifumi.cresora

import net.fabricmc.fabric.api.datagen.v1.DataGeneratorEntrypoint
import net.fabricmc.fabric.api.datagen.v1.FabricDataGenerator

class Datagen : DataGeneratorEntrypoint {
    override fun onInitializeDataGenerator(p0: FabricDataGenerator) {
        var pack= p0.createPack()

    }
}