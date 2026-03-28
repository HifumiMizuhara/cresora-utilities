package hifumi.cresora

object ScreenSyncSupport {
    fun writeInt(properties: net.minecraft.screen.PropertyDelegate, lowIndex: Int, value: Int) {
        properties.set(lowIndex, value and 0xFFFF)
        properties.set(lowIndex + 1, (value ushr 16) and 0xFFFF)
    }

    fun readInt(properties: net.minecraft.screen.PropertyDelegate, lowIndex: Int): Int {
        val low = properties.get(lowIndex) and 0xFFFF
        val high = properties.get(lowIndex + 1) and 0xFFFF
        return (high shl 16) or low
    }
}
