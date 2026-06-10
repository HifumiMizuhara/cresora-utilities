package hifumi.cresora.bloodmoon
import com.mojang.serialization.Codec
import com.mojang.serialization.codecs.RecordCodecBuilder
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents
import net.minecraft.entity.LivingEntity
import net.minecraft.entity.mob.HostileEntity
import net.minecraft.server.MinecraftServer
import net.minecraft.server.world.ServerWorld
import net.minecraft.text.Text
import net.minecraft.util.math.Box
import net.minecraft.util.math.random.Random
import net.minecraft.world.PersistentState
import net.minecraft.world.PersistentStateType
import hifumi.cresora.adventurerank.AdventureRankService
import hifumi.cresora.combat.FieldMobPackService

enum class MoonPhase(val id: String, val translationKey: String, val displayName: String, val layer: Int) {
    NEW_MOON("new_moon", "moon.cresora.phase.new_moon", "朔", 0),
    CRESCENT_ONE("crescent_one", "moon.cresora.phase.crescent_one", "既朔", 1),
    FIRST_QUARTER("first_quarter", "moon.cresora.phase.first_quarter", "上弦", 2),
    WAXING_GIBBOUS("waxing_gibbous", "moon.cresora.phase.waxing_gibbous", "逾弦", 3),
    NEAR_FULL("near_full", "moon.cresora.phase.near_full", "几望", 4),
    FULL("full", "moon.cresora.phase.full", "望", 5),
    FULL_AFTER("full_after", "moon.cresora.phase.full_after", "既望", 4),
    WANE_AFTER("wane_after", "moon.cresora.phase.wane_after", "退望", 3),
    LAST_QUARTER("last_quarter", "moon.cresora.phase.last_quarter", "下弦", 2),
    WANING_CRESCENT("waning_crescent", "moon.cresora.phase.waning_crescent", "残月", 1),
    OLD_MOON("old_moon", "moon.cresora.phase.old_moon", "晦", 0);

    companion object {
        val ORDER: List<MoonPhase> = entries
        val IDS: String = entries.joinToString(", ") { it.id }

        fun fromDayIndex(dayIndex: Long): MoonPhase = ORDER[(dayIndex % ORDER.size).toInt()]

        fun fromId(id: String): MoonPhase? = entries.firstOrNull { it.id.equals(id, ignoreCase = true) }
    }
}

enum class SpecialMoonPhase(val id: String, val translationKey: String, val displayName: String, val rollChance: Double) {
    BLOOD_MOON("blood_moon", "moon.cresora.special.blood_moon", "血月", 0.11),
    SOLAR_ECLIPSE("solar_eclipse", "moon.cresora.special.solar_eclipse", "日食", 0.05),
    LUNAR_ECLIPSE("lunar_eclipse", "moon.cresora.special.lunar_eclipse", "月食", 0.05),
    DEATH_MOON("death_moon", "moon.cresora.special.death_moon", "死月", 0.02),
    UNKNOWN("unknown", "moon.cresora.special.unknown", "？？", 0.01);

    companion object {
        private val ROLLS: List<SpecialMoonPhase> = entries
        val IDS: String = entries.joinToString(", ") { it.id }

        fun roll(random: Random): SpecialMoonPhase? {
            val pick = random.nextDouble()
            var cursor = 0.0
            for (phase in ROLLS) {
                cursor += phase.rollChance
                if (pick < cursor) {
                    return phase
                }
            }
            return null
        }

        fun fromId(id: String): SpecialMoonPhase? = entries.firstOrNull { it.id.equals(id, ignoreCase = true) }
    }
}

data class MoonNight(
    val dayIndex: Long,
    val phase: MoonPhase,
    val specialPhase: SpecialMoonPhase? = null
) {
    val layer: Int = if (specialPhase == null) phase.layer else 0

    fun displayText(): Text = Text.translatable(specialPhase?.translationKey ?: phase.translationKey)
}

private data class SavedMoonState(
    val phaseOffset: Int,
    val lastResolvedDay: Long,
    val currentSpecialDay: Long,
    val currentSpecialPhase: String,
    val bloodMoonSeenCycleIndex: Long,
    val forcedBloodMoonDay: Long
) {
    companion object {
        val CODEC: Codec<SavedMoonState> = RecordCodecBuilder.create { instance ->
            instance.group(
                Codec.INT.fieldOf("phaseOffset").forGetter(SavedMoonState::phaseOffset),
                Codec.LONG.fieldOf("lastResolvedDay").forGetter(SavedMoonState::lastResolvedDay),
                Codec.LONG.optionalFieldOf("currentSpecialDay", -1L).forGetter(SavedMoonState::currentSpecialDay),
                Codec.STRING.optionalFieldOf("currentSpecialPhase", "").forGetter(SavedMoonState::currentSpecialPhase),
                Codec.LONG.optionalFieldOf("bloodMoonSeenCycleIndex", -1L).forGetter(SavedMoonState::bloodMoonSeenCycleIndex),
                Codec.LONG.optionalFieldOf("forcedBloodMoonDay", -1L).forGetter(SavedMoonState::forcedBloodMoonDay)
            ).apply(instance) { phaseOffset, lastResolvedDay, currentSpecialDay, currentSpecialPhase, bloodMoonSeenCycleIndex, forcedBloodMoonDay ->
                SavedMoonState(phaseOffset, lastResolvedDay, currentSpecialDay, currentSpecialPhase, bloodMoonSeenCycleIndex, forcedBloodMoonDay)
            }
        }
    }
}

class MoonPhasePersistentState(
    var phaseOffset: Int = 0,
    var lastResolvedDay: Long = -1L,
    var currentSpecialDay: Long = -1L,
    var currentSpecialPhase: String = "",
    var bloodMoonSeenCycleIndex: Long = -1L,
    var forcedBloodMoonDay: Long = -1L
) : PersistentState() {
    companion object {
        private const val STATE_ID = "cresora_moon_phase"

        val TYPE: PersistentStateType<MoonPhasePersistentState> = PersistentStateType(
            STATE_ID,
            ::MoonPhasePersistentState,
            SavedMoonState.CODEC.xmap(
                { saved ->
                    MoonPhasePersistentState(
                        saved.phaseOffset,
                        saved.lastResolvedDay,
                        saved.currentSpecialDay,
                        saved.currentSpecialPhase,
                        saved.bloodMoonSeenCycleIndex,
                        saved.forcedBloodMoonDay
                    )
                },
                { state ->
                    SavedMoonState(
                        state.phaseOffset,
                        state.lastResolvedDay,
                        state.currentSpecialDay,
                        state.currentSpecialPhase,
                        state.bloodMoonSeenCycleIndex,
                        state.forcedBloodMoonDay
                    )
                }
            ),
            net.minecraft.datafixer.DataFixTypes.SAVED_DATA_COMMAND_STORAGE
        )
    }
}

object MoonPhaseService {
    private const val DAY_TIME_TRIGGER = 12000L

    private var state: MoonPhasePersistentState? = null
    private var stateLoaded = false

    fun init() {
        ServerTickEvents.END_SERVER_TICK.register(ServerTickEvents.EndTick { server -> tick(server) })
    }

    fun currentNight(server: MinecraftServer): MoonNight {
        ensureStateLoaded(server)
        val persisted = state ?: return MoonNight(0L, MoonPhase.NEW_MOON)
        val dayIndex = currentDayIndex(server.overworld.timeOfDay)
        val phase = MoonPhase.fromDayIndex(dayIndex + persisted.phaseOffset)
        val specialPhase = specialPhaseForDay(persisted, dayIndex)
        return MoonNight(dayIndex, phase, specialPhase)
    }

    fun isBloodMoon(server: MinecraftServer): Boolean = currentNight(server).specialPhase == SpecialMoonPhase.BLOOD_MOON

    fun isSolarEclipse(server: MinecraftServer): Boolean = currentNight(server).specialPhase == SpecialMoonPhase.SOLAR_ECLIPSE

    fun isLunarEclipse(server: MinecraftServer): Boolean = currentNight(server).specialPhase == SpecialMoonPhase.LUNAR_ECLIPSE

    // Lunar eclipse "blessing night": hostile-kill adventure XP / credits are doubled. Loot doubling is applied in LivingEntityMixin.
    fun killRewardMultiplier(server: MinecraftServer): Double = if (isLunarEclipse(server)) 2.0 else 1.0

    fun moonLayer(server: MinecraftServer): Int = currentNight(server).layer

    // Reserved hook for future non-blood special nights. Blood moon time scaling is applied in BloodMoonService.
    fun timeScaleMultiplier(server: MinecraftServer): Double = if (isBloodMoon(server)) 0.5 else 1.0

    fun bloodMoonHealthScalar(server: MinecraftServer): Double = if (isBloodMoon(server)) 1.2 else 1.0

    fun bloodMoonDamageScalar(server: MinecraftServer): Double = if (isBloodMoon(server)) 1.1 else 1.0

    fun healthScalar(server: MinecraftServer): Double = (1.0 + moonLayer(server) * 0.05) * bloodMoonHealthScalar(server)

    fun damageMultiplier(server: MinecraftServer): Double = (1.0 + moonLayer(server) * 0.05) * bloodMoonDamageScalar(server)

    fun levelBonus(server: MinecraftServer): Int = moonLayer(server)

    fun specialMoonText(server: MinecraftServer): Text? {
        val special = currentNight(server).specialPhase ?: return null
        return Text.translatable(special.translationKey)
    }

    fun setPhaseOffset(server: MinecraftServer, targetPhase: MoonPhase) {
        ensureStateLoaded(server)
        val persisted = state ?: return
        val currentDay = currentDayIndex(server.overworld.timeOfDay)
        val targetOffset = ((targetPhase.ordinal - currentDay) % MoonPhase.ORDER.size + MoonPhase.ORDER.size) % MoonPhase.ORDER.size
        persisted.phaseOffset = targetOffset.toInt()
        persisted.markDirty()
    }

    fun clearSpecialMoon(server: MinecraftServer) {
        ensureStateLoaded(server)
        val persisted = state ?: return
        persisted.currentSpecialDay = -1L
        persisted.currentSpecialPhase = ""
        persisted.markDirty()
    }

    fun clearBloodMoon(server: MinecraftServer): Boolean {
        ensureStateLoaded(server)
        val persisted = state ?: return false
        val dayIndex = currentDayIndex(server.overworld.timeOfDay)
        val isCurrentBloodMoon = persisted.currentSpecialDay == dayIndex && persisted.currentSpecialPhase == SpecialMoonPhase.BLOOD_MOON.name
        if (!isCurrentBloodMoon) {
            return false
        }
        persisted.currentSpecialDay = -1L
        persisted.currentSpecialPhase = ""
        persisted.markDirty()
        return true
    }

    fun setSpecialMoon(server: MinecraftServer, specialPhase: SpecialMoonPhase) {
        ensureStateLoaded(server)
        val persisted = state ?: return
        val dayIndex = currentDayIndex(server.overworld.timeOfDay)
        persisted.currentSpecialDay = dayIndex
        persisted.currentSpecialPhase = specialPhase.name
        if (persisted.forcedBloodMoonDay == dayIndex) {
            persisted.forcedBloodMoonDay = -1L
        }
        if (specialPhase == SpecialMoonPhase.BLOOD_MOON) {
            persisted.bloodMoonSeenCycleIndex = dayIndex / MoonPhase.ORDER.size
        }
        persisted.markDirty()
    }

    fun scheduleBloodMoonForNextNight(server: MinecraftServer): Long {
        ensureStateLoaded(server)
        val persisted = state ?: return -1L
        val dayIndex = currentDayIndex(server.overworld.timeOfDay)
        val timeInDay = server.overworld.timeOfDay % 24000L
        val targetDay = if (timeInDay < DAY_TIME_TRIGGER) dayIndex else dayIndex + 1L
        persisted.forcedBloodMoonDay = targetDay
        persisted.markDirty()
        return targetDay
    }

    fun summaryLine(server: MinecraftServer): Text {
        val night = currentNight(server)
        val persisted = state
        return Text.translatable(
            "commands.cresora.moon.summary",
            night.dayIndex,
            Text.translatable(night.phase.translationKey),
            night.specialPhase?.let { Text.translatable(it.translationKey) } ?: Text.translatable("commands.cresora.moon.none"),
            night.layer,
            persisted?.phaseOffset ?: 0
        )
    }

    fun refreshLoadedHostiles(server: MinecraftServer) {
        val solarEclipseActive = isSolarEclipse(server)
        for (world in server.worlds) {
            refreshLoadedHostiles(world, solarEclipseActive)
        }
    }

    // Reserved per-entity special-night damage modifier hook.
    fun specialDamageMultiplier(entity: LivingEntity): Double = 1.0

    // Reserved per-entity special-night health scalar hook.
    fun specialHealthScalar(entity: LivingEntity): Double = 1.0

    fun tickSpecialNight(server: MinecraftServer) {
        val night = currentNight(server)
        if (night.specialPhase != null) {
            onSpecialNightStarted(server, night)
        }
    }

    private fun onSpecialNightStarted(server: MinecraftServer, night: MoonNight) {
        if (night.specialPhase == null) {
            return
        }
    }

    private fun tick(server: MinecraftServer) {
        ensureStateLoaded(server)
        val persisted = state ?: return
        val dayIndex = currentDayIndex(server.overworld.timeOfDay)
        val timeInDay = server.overworld.timeOfDay % 24000L

        if (timeInDay < DAY_TIME_TRIGGER) {
            if (persisted.lastResolvedDay == dayIndex) {
                persisted.lastResolvedDay = -1L
                persisted.markDirty()
            }
            return
        }
        if (dayIndex == persisted.lastResolvedDay) {
            return
        }

        val cycleIndex = dayIndex / MoonPhase.ORDER.size
        val cycleDay = (dayIndex % MoonPhase.ORDER.size).toInt()
        val resolvedSpecial = specialPhaseForDay(persisted, dayIndex) ?: if (persisted.forcedBloodMoonDay == dayIndex) {
            SpecialMoonPhase.BLOOD_MOON
        } else {
            rollSpecialPhase(server.overworld.random, persisted, cycleIndex, cycleDay)
        }

        persisted.lastResolvedDay = dayIndex
        persisted.currentSpecialDay = if (resolvedSpecial != null) dayIndex else -1L
        persisted.currentSpecialPhase = resolvedSpecial?.name ?: ""
        if (resolvedSpecial == SpecialMoonPhase.BLOOD_MOON) {
            persisted.bloodMoonSeenCycleIndex = cycleIndex
        }
        if (persisted.forcedBloodMoonDay == dayIndex) {
            persisted.forcedBloodMoonDay = -1L
        }
        persisted.markDirty()

        val night = currentNight(server)
        server.playerManager.playerList.forEach { player ->
            player.sendMessage(Text.translatable("message.cresora.moon.night_announce", night.displayText()), false)
        }
        refreshLoadedHostiles(server)
        tickSpecialNight(server)
    }

    private fun rollSpecialPhase(
        random: Random,
        persisted: MoonPhasePersistentState,
        cycleIndex: Long,
        cycleDay: Int
    ): SpecialMoonPhase? {
        val forcedBloodMoon = cycleDay >= MoonPhase.ORDER.lastIndex && persisted.bloodMoonSeenCycleIndex != cycleIndex
        val special = if (forcedBloodMoon) {
            SpecialMoonPhase.BLOOD_MOON
        } else {
            SpecialMoonPhase.roll(random)
        }
        if (special == SpecialMoonPhase.BLOOD_MOON) {
            persisted.bloodMoonSeenCycleIndex = cycleIndex
        }
        return special
    }

    private fun specialPhaseForDay(persisted: MoonPhasePersistentState, dayIndex: Long): SpecialMoonPhase? {
        if (persisted.currentSpecialDay != dayIndex || persisted.currentSpecialPhase.isBlank()) {
            return null
        }
        return runCatching { SpecialMoonPhase.valueOf(persisted.currentSpecialPhase) }.getOrNull()
    }

    private fun refreshLoadedHostiles(world: ServerWorld, solarEclipseActive: Boolean) {
        val bounds = Box(-30_000_000.0, -2048.0, -30_000_000.0, 30_000_000.0, 2048.0, 30_000_000.0)
        for (hostile in world.getEntitiesByClass(HostileEntity::class.java, bounds) { true }) {
            if (solarEclipseActive) {
                FieldMobPackService.promoteToEclipseElite(hostile)
            } else {
                FieldMobPackService.demoteEclipseElite(hostile)
            }
            AdventureRankService.applyMobScaling(hostile, AdventureRankService.mobRank(hostile))
            AdventureRankService.refreshMobDisplay(hostile)
        }
    }

    private fun ensureStateLoaded(server: MinecraftServer) {
        if (stateLoaded) {
            return
        }
        val manager = server.overworld.persistentStateManager
        state = manager.getOrCreate(MoonPhasePersistentState.TYPE)
        stateLoaded = true
    }

    private fun currentDayIndex(time: Long): Long = time / 24000L
}
