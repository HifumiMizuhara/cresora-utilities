package hifumi.cresora.masquerade

import com.mojang.serialization.Codec
import com.mojang.serialization.codecs.RecordCodecBuilder
import net.minecraft.datafixer.DataFixTypes
import net.minecraft.item.ItemStack
import net.minecraft.server.MinecraftServer
import net.minecraft.util.Uuids
import net.minecraft.world.PersistentState
import net.minecraft.world.PersistentStateType
import java.util.UUID

data class MasqueradeRecoveryRecord(
    val playerUuid: UUID,
    val mainInventory: List<ItemStack>,
    val offHand: ItemStack,
    val selectedSlot: Int,
    val returnWorldId: String,
    val returnX: Double,
    val returnY: Double,
    val returnZ: Double,
    val returnYaw: Float,
    val returnPitch: Float,
    val restoreOnRespawn: Boolean = false
) {
    companion object {
        val CODEC: Codec<MasqueradeRecoveryRecord> = RecordCodecBuilder.create { instance ->
            instance.group(
                Uuids.INT_STREAM_CODEC.fieldOf("playerUuid").forGetter(MasqueradeRecoveryRecord::playerUuid),
                ItemStack.OPTIONAL_CODEC.listOf().fieldOf("mainInventory").forGetter(MasqueradeRecoveryRecord::mainInventory),
                ItemStack.OPTIONAL_CODEC.fieldOf("offHand").forGetter(MasqueradeRecoveryRecord::offHand),
                Codec.INT.fieldOf("selectedSlot").forGetter(MasqueradeRecoveryRecord::selectedSlot),
                Codec.STRING.fieldOf("returnWorldId").forGetter(MasqueradeRecoveryRecord::returnWorldId),
                Codec.DOUBLE.fieldOf("returnX").forGetter(MasqueradeRecoveryRecord::returnX),
                Codec.DOUBLE.fieldOf("returnY").forGetter(MasqueradeRecoveryRecord::returnY),
                Codec.DOUBLE.fieldOf("returnZ").forGetter(MasqueradeRecoveryRecord::returnZ),
                Codec.FLOAT.fieldOf("returnYaw").forGetter(MasqueradeRecoveryRecord::returnYaw),
                Codec.FLOAT.fieldOf("returnPitch").forGetter(MasqueradeRecoveryRecord::returnPitch),
                Codec.BOOL.optionalFieldOf("restoreOnRespawn", false).forGetter(MasqueradeRecoveryRecord::restoreOnRespawn)
            ).apply(instance, ::MasqueradeRecoveryRecord)
        }
    }

    fun copied(): MasqueradeRecoveryRecord = copy(
        mainInventory = mainInventory.map(ItemStack::copy),
        offHand = offHand.copy()
    )
}

class MasqueradeRecoveryPersistentState(
    val entries: MutableList<MasqueradeRecoveryRecord> = mutableListOf()
) : PersistentState() {
    companion object {
        private const val STATE_ID = "cresora_masquerade_recovery"

        val TYPE: PersistentStateType<MasqueradeRecoveryPersistentState> = PersistentStateType(
            STATE_ID,
            ::MasqueradeRecoveryPersistentState,
            MasqueradeRecoveryRecord.CODEC.listOf().xmap(
                { entries -> MasqueradeRecoveryPersistentState(entries.toMutableList()) },
                MasqueradeRecoveryPersistentState::entries
            ),
            DataFixTypes.SAVED_DATA_COMMAND_STORAGE
        )
    }
}

object MasqueradeRecoveryStateService {
    fun find(server: MinecraftServer, playerId: UUID): MasqueradeRecoveryRecord? {
        return state(server).entries.firstOrNull { it.playerUuid == playerId }
    }

    fun save(server: MinecraftServer, record: MasqueradeRecoveryRecord) {
        val state = state(server)
        state.entries.removeIf { it.playerUuid == record.playerUuid }
        state.entries += record.copied()
        state.markDirty()
    }

    fun markForRespawn(server: MinecraftServer, playerId: UUID) {
        val state = state(server)
        val index = state.entries.indexOfFirst { it.playerUuid == playerId }
        if (index < 0) {
            return
        }
        state.entries[index] = state.entries[index].copy(restoreOnRespawn = true)
        state.markDirty()
    }

    fun remove(server: MinecraftServer, playerId: UUID) {
        val state = state(server)
        if (state.entries.removeIf { it.playerUuid == playerId }) {
            state.markDirty()
        }
    }

    private fun state(server: MinecraftServer): MasqueradeRecoveryPersistentState {
        return server.overworld.persistentStateManager.getOrCreate(MasqueradeRecoveryPersistentState.TYPE)
    }
}
