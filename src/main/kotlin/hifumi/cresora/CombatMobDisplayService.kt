package hifumi.cresora

import net.minecraft.entity.EntityType
import net.minecraft.entity.LivingEntity
import net.minecraft.entity.damage.DamageSource
import net.minecraft.entity.decoration.DisplayEntity
import net.minecraft.entity.mob.HostileEntity
import net.minecraft.server.world.ServerWorld
import net.minecraft.text.Text
import net.minecraft.util.Formatting
import kotlin.math.abs
import kotlin.math.roundToInt

object CombatMobDisplayService {
    private const val DISPLAY_RANGE = 32.0
    private const val FLOAT_LIFETIME_TICKS = 16L
    private const val FLOAT_RISE_PER_TICK = 0.045
    private const val FLOAT_SIDE_OFFSET = 0.18

    private data class DamageIndicator(
        val world: ServerWorld,
        val entityId: Int,
        val expireTick: Long
    )

    private val activeIndicators: MutableMap<Int, DamageIndicator> = LinkedHashMap()

    fun updateMobStatus(entity: HostileEntity) {
        if (entity.isRemoved || !entity.isAlive) {
            entity.isCustomNameVisible = false
            return
        }

        val world = entity.world as? ServerWorld ?: return
        val hasViewer = world.players.any { !it.isSpectator && it.squaredDistanceTo(entity) <= DISPLAY_RANGE * DISPLAY_RANGE }
        entity.isCustomNameVisible = hasViewer
        if (!hasViewer) {
            return
        }

        val rank = AdventureRankService.mobLevel(entity)
        entity.customName = Text.translatable(
            "combat.cresora.mob_label",
            rank,
            entity.type.name,
            formatNumber(entity.health.toDouble().coerceAtLeast(0.0)),
            formatNumber(entity.maxHealth.toDouble().coerceAtLeast(1.0))
        )
    }

    fun showDamage(target: LivingEntity, source: DamageSource, damage: Double) {
        if (damage <= 0.0 || target.isRemoved) {
            return
        }

        val world = target.world as? ServerWorld ?: return
        val hasViewer = source.attacker != null || world.players.any { !it.isSpectator && it.squaredDistanceTo(target) <= DISPLAY_RANGE * DISPLAY_RANGE }
        if (!hasViewer) {
            return
        }

        val display = DisplayEntity.TextDisplayEntity(EntityType.TEXT_DISPLAY, world)
        val offsetX = if (target.id % 2 == 0) FLOAT_SIDE_OFFSET else -FLOAT_SIDE_OFFSET
        val offsetZ = if (target.age % 2 == 0) -FLOAT_SIDE_OFFSET else FLOAT_SIDE_OFFSET
        display.setPosition(target.x + offsetX, target.y + target.height + 0.8, target.z + offsetZ)
        display.setBillboardMode(DisplayEntity.BillboardMode.CENTER)
        display.setViewRange(0.9f)
        display.setDisplayWidth(0.0f)
        display.setDisplayHeight(0.0f)
        display.setShadowStrength(0.0f)
        display.setBackground(0)
        display.setTextOpacity((-1).toByte())
        val displayFlags =
            (DisplayEntity.TextDisplayEntity.SHADOW_FLAG.toInt() or DisplayEntity.TextDisplayEntity.SEE_THROUGH_FLAG.toInt()).toByte()
        display.setDisplayFlags(displayFlags)
        display.setNoGravity(true)
        display.isInvulnerable = true
        display.isSilent = true
        display.setText(Text.literal(formatNumber(damage)).formatted(colorForDamage(damage)))
        world.spawnEntity(display)
        activeIndicators[display.id] = DamageIndicator(world, display.id, world.time + FLOAT_LIFETIME_TICKS)
    }

    fun tick(world: ServerWorld) {
        val iterator = activeIndicators.values.iterator()
        while (iterator.hasNext()) {
            val indicator = iterator.next()
            if (indicator.world != world) {
                continue
            }

            val entity = world.getEntityById(indicator.entityId) as? DisplayEntity.TextDisplayEntity
            if (entity == null || entity.isRemoved || world.time >= indicator.expireTick) {
                entity?.discard()
                iterator.remove()
                continue
            }

            entity.setPosition(entity.x, entity.y + FLOAT_RISE_PER_TICK, entity.z)
        }
    }

    private fun colorForDamage(damage: Double): Formatting {
        return when {
            damage >= 150.0 -> Formatting.GOLD
            damage >= 50.0 -> Formatting.YELLOW
            damage >= 20.0 -> Formatting.WHITE
            else -> Formatting.GRAY
        }
    }

    private fun formatNumber(value: Double): String {
        val rounded = kotlin.math.round(value * 10.0) / 10.0
        return if (abs(rounded - rounded.roundToInt().toDouble()) < 1.0e-6) {
            rounded.roundToInt().toString()
        } else {
            rounded.toString()
        }
    }
}
