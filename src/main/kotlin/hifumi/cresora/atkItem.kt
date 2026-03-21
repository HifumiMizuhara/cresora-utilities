package hifumi.cresora

import net.minecraft.item.Item

abstract class atkItem(settings: Settings): Item(settings) {

        abstract val bairitu: Double

    abstract fun register()
}