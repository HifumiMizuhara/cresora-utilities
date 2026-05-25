package hifumi.cresora.masquerade
import net.minecraft.server.network.ServerPlayerEntity

data class MasqueradeSeasonRecord(
    val seasonId: String,
    val bestWave: Int,
    val attemptCount: Int,
    val totalClearedWaves: Int
)

data class MasqueradeProgress(
    val current: MasqueradeSeasonRecord,
    val archive: List<MasqueradeSeasonRecord>
) {
    fun previousSeason(): MasqueradeSeasonRecord? = archive.lastOrNull()
}

object MasqueradeProgressService {
    private const val PLAYER_MASQUERADE_SEASON_ID_KEY = "cresora_masquerade_season_id"
    private const val PLAYER_MASQUERADE_BEST_WAVE_KEY = "cresora_masquerade_best_wave"
    private const val PLAYER_MASQUERADE_ATTEMPT_COUNT_KEY = "cresora_masquerade_attempt_count"
    private const val PLAYER_MASQUERADE_TOTAL_CLEARED_WAVES_KEY = "cresora_masquerade_total_cleared_waves"
    private const val PLAYER_MASQUERADE_ARCHIVE_KEY = "cresora_masquerade_archive"
    private const val RECORD_SEPARATOR = ";"
    private const val FIELD_SEPARATOR = ","

    fun playerSeasonIdKey(): String = PLAYER_MASQUERADE_SEASON_ID_KEY

    fun playerBestWaveKey(): String = PLAYER_MASQUERADE_BEST_WAVE_KEY

    fun playerAttemptCountKey(): String = PLAYER_MASQUERADE_ATTEMPT_COUNT_KEY

    fun playerTotalClearedWavesKey(): String = PLAYER_MASQUERADE_TOTAL_CLEARED_WAVES_KEY

    fun playerArchiveKey(): String = PLAYER_MASQUERADE_ARCHIVE_KEY

    fun getProgress(player: ServerPlayerEntity): MasqueradeProgress {
        val access = player as? MasqueradeProgressAccess ?: return defaultProgress()
        return normalize(
            player,
            access = access,
            activeSeasonId = MasqueradeContentRegistry.definition().seasonId
        )
    }

    fun recordRun(player: ServerPlayerEntity, clearedWaveCount: Int) {
        val access = player as? MasqueradeProgressAccess ?: return
        val activeSeasonId = MasqueradeContentRegistry.definition().seasonId
        val normalized = normalize(player, access, activeSeasonId)
        val current = normalized.current
        val updated = current.copy(
            bestWave = maxOf(current.bestWave, clearedWaveCount.coerceAtLeast(0)),
            attemptCount = current.attemptCount + 1,
            totalClearedWaves = current.totalClearedWaves + clearedWaveCount.coerceAtLeast(0)
        )
        writeCurrent(access, updated)
    }

    fun currentRecord(player: ServerPlayerEntity): MasqueradeSeasonRecord = getProgress(player).current

    fun previousRecord(player: ServerPlayerEntity): MasqueradeSeasonRecord? = getProgress(player).previousSeason()

    fun copyTo(oldPlayer: ServerPlayerEntity, newPlayer: ServerPlayerEntity) {
        val oldAccess = oldPlayer as? MasqueradeProgressAccess ?: return
        val newAccess = newPlayer as? MasqueradeProgressAccess ?: return
        newAccess.cresoraSetMasqueradeCurrentSeasonId(oldAccess.cresoraGetMasqueradeCurrentSeasonId())
        newAccess.cresoraSetMasqueradeBestWave(oldAccess.cresoraGetMasqueradeBestWave())
        newAccess.cresoraSetMasqueradeAttemptCount(oldAccess.cresoraGetMasqueradeAttemptCount())
        newAccess.cresoraSetMasqueradeTotalClearedWaves(oldAccess.cresoraGetMasqueradeTotalClearedWaves())
        newAccess.cresoraSetMasqueradeArchiveRaw(oldAccess.cresoraGetMasqueradeArchiveRaw())
    }

    private fun normalize(
        player: ServerPlayerEntity,
        access: MasqueradeProgressAccess,
        activeSeasonId: String
    ): MasqueradeProgress {
        val currentSeasonId = access.cresoraGetMasqueradeCurrentSeasonId()
        val archive = decodeArchive(access.cresoraGetMasqueradeArchiveRaw()).toMutableList()
        val normalized = when {
            currentSeasonId.isBlank() -> {
                val initial = MasqueradeSeasonRecord(activeSeasonId, 0, 0, 0)
                writeCurrent(access, initial)
                MasqueradeProgress(initial, archive)
            }
            currentSeasonId != activeSeasonId -> {
                val previous = MasqueradeSeasonRecord(
                    seasonId = currentSeasonId,
                    bestWave = access.cresoraGetMasqueradeBestWave().coerceAtLeast(0),
                    attemptCount = access.cresoraGetMasqueradeAttemptCount().coerceAtLeast(0),
                    totalClearedWaves = access.cresoraGetMasqueradeTotalClearedWaves().coerceAtLeast(0)
                )
                archive.removeIf { it.seasonId == previous.seasonId }
                archive += previous
                access.cresoraSetMasqueradeArchiveRaw(encodeArchive(archive))
                val reset = MasqueradeSeasonRecord(activeSeasonId, 0, 0, 0)
                writeCurrent(access, reset)
                MasqueradeProgress(reset, archive)
            }
            else -> {
                MasqueradeProgress(
                    current = MasqueradeSeasonRecord(
                        seasonId = currentSeasonId,
                        bestWave = access.cresoraGetMasqueradeBestWave().coerceAtLeast(0),
                        attemptCount = access.cresoraGetMasqueradeAttemptCount().coerceAtLeast(0),
                        totalClearedWaves = access.cresoraGetMasqueradeTotalClearedWaves().coerceAtLeast(0)
                    ),
                    archive = archive
                )
            }
        }
        return normalized
    }

    private fun writeCurrent(access: MasqueradeProgressAccess, record: MasqueradeSeasonRecord) {
        access.cresoraSetMasqueradeCurrentSeasonId(record.seasonId)
        access.cresoraSetMasqueradeBestWave(record.bestWave.coerceAtLeast(0))
        access.cresoraSetMasqueradeAttemptCount(record.attemptCount.coerceAtLeast(0))
        access.cresoraSetMasqueradeTotalClearedWaves(record.totalClearedWaves.coerceAtLeast(0))
    }

    private fun decodeArchive(raw: String): List<MasqueradeSeasonRecord> {
        if (raw.isBlank()) {
            return emptyList()
        }
        return raw.split(RECORD_SEPARATOR)
            .mapNotNull { encoded ->
                val fields = encoded.split(FIELD_SEPARATOR)
                if (fields.size != 4) {
                    return@mapNotNull null
                }
                val seasonId = fields[0].trim()
                if (seasonId.isBlank()) {
                    return@mapNotNull null
                }
                MasqueradeSeasonRecord(
                    seasonId = seasonId,
                    bestWave = fields[1].toIntOrNull()?.coerceAtLeast(0) ?: 0,
                    attemptCount = fields[2].toIntOrNull()?.coerceAtLeast(0) ?: 0,
                    totalClearedWaves = fields[3].toIntOrNull()?.coerceAtLeast(0) ?: 0
                )
            }
    }

    private fun encodeArchive(records: List<MasqueradeSeasonRecord>): String {
        return records.joinToString(RECORD_SEPARATOR) { record ->
            listOf(
                record.seasonId,
                record.bestWave.coerceAtLeast(0).toString(),
                record.attemptCount.coerceAtLeast(0).toString(),
                record.totalClearedWaves.coerceAtLeast(0).toString()
            ).joinToString(FIELD_SEPARATOR)
        }
    }

    private fun defaultProgress(): MasqueradeProgress {
        return MasqueradeProgress(
            current = MasqueradeSeasonRecord(MasqueradeContentRegistry.definition().seasonId, 0, 0, 0),
            archive = emptyList()
        )
    }
}
