package hifumi.cresora.bloodmoon
import com.mojang.serialization.Codec
import com.mojang.serialization.codecs.RecordCodecBuilder
import net.minecraft.server.MinecraftServer
import net.minecraft.util.math.BlockPos
import net.minecraft.world.PersistentState
import net.minecraft.world.PersistentStateType
import java.util.UUID

data class BloodMoonRewardChestRecord(
    val worldId: String,
    val x: Int,
    val y: Int,
    val z: Int,
    val ownerUuid: String,
    val rewardSeed: Long
) {
    companion object {
        val CODEC: Codec<BloodMoonRewardChestRecord> = RecordCodecBuilder.create { instance ->
            instance.group(
                Codec.STRING.fieldOf("worldId").forGetter(BloodMoonRewardChestRecord::worldId),
                Codec.INT.fieldOf("x").forGetter(BloodMoonRewardChestRecord::x),
                Codec.INT.fieldOf("y").forGetter(BloodMoonRewardChestRecord::y),
                Codec.INT.fieldOf("z").forGetter(BloodMoonRewardChestRecord::z),
                Codec.STRING.fieldOf("ownerUuid").forGetter(BloodMoonRewardChestRecord::ownerUuid),
                Codec.LONG.fieldOf("rewardSeed").forGetter(BloodMoonRewardChestRecord::rewardSeed)
            ).apply(instance, ::BloodMoonRewardChestRecord)
        }
    }
}

private data class BloodMoonRewardChestSavedState(val entries: List<BloodMoonRewardChestRecord>) {
    companion object {
        val CODEC: Codec<BloodMoonRewardChestSavedState> = RecordCodecBuilder.create { instance ->
            instance.group(
                BloodMoonRewardChestRecord.CODEC.listOf().fieldOf("entries").forGetter(BloodMoonRewardChestSavedState::entries)
            ).apply(instance, ::BloodMoonRewardChestSavedState)
        }
    }
}

class BloodMoonRewardChestPersistentState(
    var entries: MutableList<BloodMoonRewardChestRecord> = mutableListOf()
) : PersistentState() {
    companion object {
        private const val STATE_ID = "cresora_blood_moon_reward_chests"
        val TYPE: PersistentStateType<BloodMoonRewardChestPersistentState> = PersistentStateType(
            STATE_ID,
            ::BloodMoonRewardChestPersistentState,
            BloodMoonRewardChestSavedState.CODEC.xmap(
                { saved -> BloodMoonRewardChestPersistentState(saved.entries.toMutableList()) },
                { state -> BloodMoonRewardChestSavedState(state.entries.toList()) }
            ),
            net.minecraft.datafixer.DataFixTypes.SAVED_DATA_COMMAND_STORAGE
        )
    }
}

object BloodMoonRewardChestStateService {
    fun get(server: MinecraftServer): BloodMoonRewardChestPersistentState {
        return server.overworld.persistentStateManager.getOrCreate(BloodMoonRewardChestPersistentState.TYPE)
    }

    fun find(
        state: BloodMoonRewardChestPersistentState,
        worldId: String,
        pos: BlockPos
    ): BloodMoonRewardChestRecord? {
        return state.entries.firstOrNull { it.worldId == worldId && it.x == pos.x && it.y == pos.y && it.z == pos.z }
    }

    fun put(
        state: BloodMoonRewardChestPersistentState,
        worldId: String,
        pos: BlockPos,
        ownerUuid: UUID,
        rewardSeed: Long
    ) {
        state.entries.removeIf { it.worldId == worldId && it.x == pos.x && it.y == pos.y && it.z == pos.z }
        state.entries.add(BloodMoonRewardChestRecord(worldId, pos.x, pos.y, pos.z, ownerUuid.toString(), rewardSeed))
        state.markDirty()
    }

    fun remove(state: BloodMoonRewardChestPersistentState, worldId: String, pos: BlockPos) {
        if (state.entries.removeIf { it.worldId == worldId && it.x == pos.x && it.y == pos.y && it.z == pos.z }) {
            state.markDirty()
        }
    }
}
