package hifumi.cresora.treasure
import com.mojang.serialization.Codec
import com.mojang.serialization.codecs.RecordCodecBuilder
import net.minecraft.datafixer.DataFixTypes
import net.minecraft.world.PersistentState
import net.minecraft.world.PersistentStateType

data class SavedTreasureChest(
    val ownerUuid: String,
    val worldId: String,
    val x: Int,
    val y: Int,
    val z: Int,
    val stars: Int,
    val credits: Int,
    val chordProgression: Int,
    val expireTime: Long
) {
    companion object {
        val CODEC: Codec<SavedTreasureChest> = RecordCodecBuilder.create { instance ->
            instance.group(
                Codec.STRING.fieldOf("ownerUuid").forGetter(SavedTreasureChest::ownerUuid),
                Codec.STRING.fieldOf("worldId").forGetter(SavedTreasureChest::worldId),
                Codec.INT.fieldOf("x").forGetter(SavedTreasureChest::x),
                Codec.INT.fieldOf("y").forGetter(SavedTreasureChest::y),
                Codec.INT.fieldOf("z").forGetter(SavedTreasureChest::z),
                Codec.INT.fieldOf("stars").forGetter(SavedTreasureChest::stars),
                Codec.INT.fieldOf("credits").forGetter(SavedTreasureChest::credits),
                Codec.INT.fieldOf("chordProgression").forGetter(SavedTreasureChest::chordProgression),
                Codec.LONG.optionalFieldOf("expireTime", 0L).forGetter(SavedTreasureChest::expireTime)
            ).apply(instance, ::SavedTreasureChest)
        }
    }
}

class TreasureChestPersistentState(
    val chests: MutableList<SavedTreasureChest> = mutableListOf()
) : PersistentState() {
    companion object {
        private const val STATE_ID = "cresora_treasure_chests"

        val CODEC: Codec<TreasureChestPersistentState> = SavedTreasureChest.CODEC.listOf().xmap(
            { saved -> TreasureChestPersistentState(saved.toMutableList()) },
            TreasureChestPersistentState::chests
        )

        val TYPE: PersistentStateType<TreasureChestPersistentState> = PersistentStateType(
            STATE_ID,
            ::TreasureChestPersistentState,
            CODEC,
            DataFixTypes.SAVED_DATA_COMMAND_STORAGE
        )
    }
}
