package hifumi.cresora.bloodmoon
import com.mojang.serialization.Codec
import com.mojang.serialization.codecs.RecordCodecBuilder
import net.minecraft.datafixer.DataFixTypes
import net.minecraft.registry.RegistryKey
import net.minecraft.registry.RegistryKeys
import net.minecraft.util.Identifier
import net.minecraft.util.math.BlockPos
import net.minecraft.world.PersistentState
import net.minecraft.world.PersistentStateType
import net.minecraft.world.World
import java.util.UUID

data class SavedBloodMoonBedKey(
    val worldId: String,
    val firstX: Int,
    val firstY: Int,
    val firstZ: Int,
    val secondX: Int,
    val secondY: Int,
    val secondZ: Int
) {
    companion object {
        val CODEC: Codec<SavedBloodMoonBedKey> = RecordCodecBuilder.create { instance ->
            instance.group(
                Codec.STRING.fieldOf("worldId").forGetter(SavedBloodMoonBedKey::worldId),
                Codec.INT.fieldOf("firstX").forGetter(SavedBloodMoonBedKey::firstX),
                Codec.INT.fieldOf("firstY").forGetter(SavedBloodMoonBedKey::firstY),
                Codec.INT.fieldOf("firstZ").forGetter(SavedBloodMoonBedKey::firstZ),
                Codec.INT.fieldOf("secondX").forGetter(SavedBloodMoonBedKey::secondX),
                Codec.INT.fieldOf("secondY").forGetter(SavedBloodMoonBedKey::secondY),
                Codec.INT.fieldOf("secondZ").forGetter(SavedBloodMoonBedKey::secondZ)
            ).apply(instance, ::SavedBloodMoonBedKey)
        }
    }
}

data class SavedBloodMoonSession(
    val id: UUID,
    val bedKey: SavedBloodMoonBedKey,
    val ownerUuid: UUID,
    val startedDayIndex: Long,
    val participants: List<UUID>,
    val clearedWaveCount: Int,
    val nextWaveNumber: Int,
    val bedDurabilityPercent: Double,
    val phase: String,
    val nextWaveTick: Long = 0L,
    val restUntilTick: Long = 0L,
    val activeMobUuids: List<UUID> = emptyList()
) {
    companion object {
        val CODEC: Codec<SavedBloodMoonSession> = RecordCodecBuilder.create { instance ->
            instance.group(
                net.minecraft.util.Uuids.INT_STREAM_CODEC.fieldOf("id").forGetter(SavedBloodMoonSession::id),
                SavedBloodMoonBedKey.CODEC.fieldOf("bedKey").forGetter(SavedBloodMoonSession::bedKey),
                net.minecraft.util.Uuids.INT_STREAM_CODEC.fieldOf("ownerUuid").forGetter(SavedBloodMoonSession::ownerUuid),
                Codec.LONG.fieldOf("startedDayIndex").forGetter(SavedBloodMoonSession::startedDayIndex),
                net.minecraft.util.Uuids.INT_STREAM_CODEC.listOf().fieldOf("participants").forGetter(SavedBloodMoonSession::participants),
                Codec.INT.fieldOf("clearedWaveCount").forGetter(SavedBloodMoonSession::clearedWaveCount),
                Codec.INT.fieldOf("nextWaveNumber").forGetter(SavedBloodMoonSession::nextWaveNumber),
                Codec.DOUBLE.fieldOf("bedDurabilityPercent").forGetter(SavedBloodMoonSession::bedDurabilityPercent),
                Codec.STRING.fieldOf("phase").forGetter(SavedBloodMoonSession::phase),
                Codec.LONG.optionalFieldOf("nextWaveTick", 0L).forGetter(SavedBloodMoonSession::nextWaveTick),
                Codec.LONG.optionalFieldOf("restUntilTick", 0L).forGetter(SavedBloodMoonSession::restUntilTick),
                net.minecraft.util.Uuids.INT_STREAM_CODEC.listOf().optionalFieldOf("activeMobUuids", emptyList()).forGetter(SavedBloodMoonSession::activeMobUuids)
            ).apply(instance, ::SavedBloodMoonSession)
        }
    }
}

class BloodMoonPersistentState(
    var lockedBattleDayIndex: Long = -1L,
    var originalMobGriefing: Boolean? = null,
    var activeSession: SavedBloodMoonSession? = null
) : PersistentState() {
    companion object {
        private const val STATE_ID = "cresora_blood_moon"

        val CODEC: Codec<BloodMoonPersistentState> = RecordCodecBuilder.create { instance ->
            instance.group(
                Codec.LONG.fieldOf("lockedBattleDayIndex").forGetter(BloodMoonPersistentState::lockedBattleDayIndex),
                Codec.BOOL.optionalFieldOf("originalMobGriefing").forGetter { it.originalMobGriefing?.let { b -> java.util.Optional.of(b) } ?: java.util.Optional.empty() },
                SavedBloodMoonSession.CODEC.optionalFieldOf("activeSession").forGetter { it.activeSession?.let { s -> java.util.Optional.of(s) } ?: java.util.Optional.empty() }
            ).apply(instance) { locked, griefing, session ->
                BloodMoonPersistentState(locked, griefing.orElse(null), session.orElse(null))
            }
        }

        val TYPE: PersistentStateType<BloodMoonPersistentState> = PersistentStateType(
            STATE_ID,
            ::BloodMoonPersistentState,
            CODEC,
            DataFixTypes.SAVED_DATA_COMMAND_STORAGE
        )
    }
}
