package hifumi.cresora

import com.mojang.serialization.Codec
import com.mojang.serialization.codecs.RecordCodecBuilder

data class StatEntry(
    val type: StatType,
    val value: Double
) {
    fun add(amount: Double): StatEntry {
        return copy(value = value + amount)
    }

    fun normalized(): StatEntry {
        return copy(value = value.coerceAtLeast(0.0))
    }

    companion object {
        val CODEC: Codec<StatEntry> = RecordCodecBuilder.create { instance ->
            instance.group(
                StatType.CODEC.fieldOf("type").forGetter(StatEntry::type),
                Codec.DOUBLE.fieldOf("value").forGetter(StatEntry::value)
            ).apply(instance, ::StatEntry)
        }
    }
}
