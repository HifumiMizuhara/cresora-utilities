package hifumi.cresora.combat
import hifumi.cresora.adventurerank.AdventureRankService
import hifumi.cresora.weapon.WeaponSkillService
import hifumi.cresora.equipment.EquipmentPlayerSupport
import hifumi.cresora.masquerade.MasqueradeService
import net.minecraft.entity.Entity
import net.minecraft.entity.EntityType
import net.minecraft.entity.LivingEntity
import net.minecraft.entity.damage.DamageSource
import net.minecraft.entity.decoration.DisplayEntity
import net.minecraft.entity.mob.HostileEntity
import net.minecraft.entity.mob.MobEntity
import net.minecraft.entity.mob.Monster
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.server.network.ServerPlayerEntity
import net.minecraft.server.world.ServerWorld
import net.minecraft.text.MutableText
import net.minecraft.text.Text
import net.minecraft.text.TranslatableTextContent
import net.minecraft.util.Formatting
import kotlin.math.abs
import kotlin.math.roundToInt

object CombatMobDisplayService {
    private const val DISPLAY_RANGE = 32.0
    private const val FLOAT_LIFETIME_TICKS = 16L
    private const val FLOAT_RISE_PER_TICK = 0.045
    private const val FLOAT_SIDE_OFFSET = 0.18
    private const val INDICATOR_COMMAND_TAG = "cresora_damage_indicator"
    private const val SHORT_DAMAGE_TYPE_KEY_PREFIX = "combat.cresora.damage_type.short."

    private data class DamageIndicator(
        val world: ServerWorld,
        val entityId: Int,
        val expireTick: Long
    )

    private val activeIndicators: MutableMap<Int, DamageIndicator> = LinkedHashMap()

    fun updateMobStatus(entity: LivingEntity) {
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

        val isMimic = (entity as? net.minecraft.entity.passive.SheepEntity)?.let(BaaMimicService::isMimicSheep) ?: false
        val rank = when {
            isMimic -> BaaMimicService.originalRank(entity as net.minecraft.entity.passive.SheepEntity)
            entity is MobEntity && (entity is Monster || entity is HostileEntity) -> AdventureRankService.mobLevel(entity)
            else -> 1
        }
        val classificationTag = (entity as? MobEntity)?.let { if (it is Monster || it is HostileEntity) FieldMobPackService.classificationTag(it) else Text.empty() } ?: Text.empty()
        val statusSuffix = getStatusSuffix(entity)
        
        val baseLabel = Text.translatable(
            "combat.cresora.mob_label",
            rank,
            entity.type.name,
            formatNumber(entity.health.toDouble().coerceAtLeast(0.0)),
            formatNumber(entity.maxHealth.toDouble().coerceAtLeast(1.0))
        )

        val finalLabel = if (isMimic) {
            Text.empty()
                .append(Text.translatable("combat.cresora.mimic_prefix").formatted(Formatting.RED, Formatting.BOLD))
                .append(baseLabel)
        } else {
            baseLabel.append(classificationTag).append(statusSuffix)
        }

        entity.customName = finalLabel
    }

    private fun getStatusSuffix(entity: LivingEntity): Text {
        val now = entity.world.time
        val status = WeaponSkillService.getPhysicalResistanceOffset(entity) // This is just one way, but let's be more explicit if possible
        // Better: let's add a proper check in WeaponSkillService for display
        
        val markers = mutableListOf<Text>()
        if (WeaponSkillService.hasStatus(entity, "dark")) markers.add(Text.translatable("status.cresora.dark").formatted(Formatting.DARK_GRAY))
        if (WeaponSkillService.hasStatus(entity, "lux")) markers.add(Text.translatable("status.cresora.lux").formatted(Formatting.AQUA))
        if (WeaponSkillService.hasStatus(entity, "entanglement")) markers.add(Text.translatable("status.cresora.entanglement").formatted(Formatting.RED))
        if (WeaponSkillService.hasStatus(entity, "kyundeath")) markers.add(Text.translatable("status.cresora.kyundeath").formatted(Formatting.LIGHT_PURPLE))
        
        val result = Text.empty()
        for (m in markers) {
            result.append(m)
        }
        return result
    }

    fun showIncomingDamage(player: ServerPlayerEntity, source: DamageSource, damage: Double) {
        if (damage <= 0.0 || player.isRemoved) {
            return
        }
        val damageType = CombatDamageTypeSupport.damageSourceType(source)
        val totals = EquipmentPlayerSupport.getAggregatedStats(player)
        var resistanceRatio = CombatDamageTypeSupport.effectiveResistanceRatio(totals, damageType)
        resistanceRatio = MasqueradeService.clampPlayerDamageReduction(player, resistanceRatio)
        resistanceRatio = resistanceRatio.coerceIn(0.0, 0.95)
        player.sendMessage(
            Text.translatable(
                "combat.cresora.incoming_hit",
                formatNumber(damage),
                shortDamageTypeText(damageType),
                formatPercent(resistanceRatio * 100.0)
            ),
            true
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
        val damageType = CombatDamageTypeSupport.damageSourceType(source)
        val resistancePercent = if (target is MobEntity && (target is Monster || target is HostileEntity)) {
            MobCombatProfileRegistry.resistancePercent(target.type, damageType)
        } else {
            0.0
        }
        spawnDamageDisplay(world, target, damage, damageType, false)
        val attacker = source.attacker as? ServerPlayerEntity
        val hostileTarget = if (target is MobEntity && (target is Monster || target is HostileEntity)) target else null
        if (attacker != null && hostileTarget != null) {
            showOutgoingDamage(attacker, damage, damageType, resistancePercent, false)
        }
    }

    fun showDamage(target: LivingEntity, damage: Double) {
        if (damage <= 0.0 || target.isRemoved) {
            return
        }

        val world = target.world as? ServerWorld ?: return
        val hasViewer = world.players.any { !it.isSpectator && it.squaredDistanceTo(target) <= DISPLAY_RANGE * DISPLAY_RANGE }
        if (!hasViewer) {
            return
        }
        spawnDamageDisplay(world, target, damage, CombatDamageType.PHYSICAL, false)
    }

    fun showPlayerHitFeedback(attacker: ServerPlayerEntity, damage: Float, source: DamageSource) {
        if (damage <= 0.0f) return
        val damageType = CombatDamageTypeSupport.damageSourceType(source)
        showOutgoingDamage(attacker, damage.toDouble(), damageType, 0.0, false)
    }

    fun showTrueDamage(target: LivingEntity, attacker: ServerPlayerEntity, damage: Double) {
        if (damage <= 0.0 || target.isRemoved) {
            return
        }
        val world = target.world as? ServerWorld ?: return
        val hasViewer = world.players.any { !it.isSpectator && it.squaredDistanceTo(target) <= DISPLAY_RANGE * DISPLAY_RANGE }
        if (!hasViewer && attacker.squaredDistanceTo(target) > DISPLAY_RANGE * DISPLAY_RANGE) {
            return
        }
        spawnDamageDisplay(world, target, damage, null, true)
        showOutgoingDamage(attacker, damage, null, 0.0, true)
    }

    private fun showOutgoingDamage(
        player: ServerPlayerEntity,
        damage: Double,
        damageType: CombatDamageType?,
        resistancePercent: Double,
        trueDamage: Boolean
    ) {
        if (trueDamage) {
            player.sendMessage(
                Text.translatable(
                    "combat.cresora.outgoing_hit_true",
                    formatNumber(damage),
                    shortTrueDamageText()
                ),
                true
            )
            return
        }
        val critMultiplier = CombatFeedbackService.consumeCrit(player)
        val message = if (critMultiplier != null) {
            Text.translatable(
                "combat.cresora.outgoing_hit_crit",
                formatNumber(damage),
                shortDamageTypeText(damageType ?: CombatDamageType.PHYSICAL),
                formatPercent(resistancePercent),
                formatNumber(critMultiplier)
            )
        } else {
            Text.translatable(
                "combat.cresora.outgoing_hit",
                formatNumber(damage),
                shortDamageTypeText(damageType ?: CombatDamageType.PHYSICAL),
                formatPercent(resistancePercent)
            )
        }
        player.sendMessage(message, true)
    }

    private fun spawnDamageDisplay(
        world: ServerWorld,
        target: LivingEntity,
        damage: Double,
        damageType: CombatDamageType?,
        trueDamage: Boolean
    ) {
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
        val color = colorForDamage(damageType, trueDamage)
        display.setText(
            Text.empty()
                .append(Text.literal(formatNumber(damage)).formatted(color))
                .append(Text.literal(" ").formatted(color))
                .append(
                    if (trueDamage) {
                        shortTrueDamageText().formatted(color)
                    } else {
                        shortDamageTypeText(damageType ?: CombatDamageType.PHYSICAL).formatted(color)
                    }
                )
        )
        display.addCommandTag(INDICATOR_COMMAND_TAG)
        // Register before spawning: ENTITY_LOAD fires inside spawnEntity and treats
        // tagged-but-untracked indicators as orphans restored from chunk data.
        activeIndicators[display.id] = DamageIndicator(world, display.id, world.time + FLOAT_LIFETIME_TICKS)
        world.spawnEntity(display)
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

    fun discardOrphanedIndicator(entity: Entity): Boolean {
        val display = entity as? DisplayEntity.TextDisplayEntity ?: return false
        val orphaned = if (display.commandTags.contains(INDICATOR_COMMAND_TAG)) {
            !activeIndicators.containsKey(display.id)
        } else {
            // Indicators saved before the command tag existed: identify them by the
            // damage-type text so chunks containing stuck displays self-heal on load.
            isIndicatorText(display.text)
        }
        if (orphaned) {
            display.discard()
        }
        return orphaned
    }

    private fun isIndicatorText(text: Text?): Boolean {
        if (text == null) {
            return false
        }
        return (sequenceOf(text) + text.siblings.asSequence()).any {
            ((it.content as? TranslatableTextContent)?.key ?: "").startsWith(SHORT_DAMAGE_TYPE_KEY_PREFIX)
        }
    }

    private fun shortDamageTypeText(damageType: CombatDamageType): MutableText {
        return Text.translatable("$SHORT_DAMAGE_TYPE_KEY_PREFIX${damageType.id}")
    }

    private fun shortTrueDamageText(): MutableText {
        return Text.translatable("${SHORT_DAMAGE_TYPE_KEY_PREFIX}true_damage")
    }

    private fun colorForDamage(damageType: CombatDamageType?, trueDamage: Boolean): Formatting {
        if (trueDamage) {
            return Formatting.GOLD
        }
        return when {
            damageType == CombatDamageType.ARCANE -> Formatting.AQUA
            damageType == CombatDamageType.PHYSICAL -> Formatting.WHITE
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

    private fun formatPercent(value: Double): String {
        return "${formatNumber(value)}%"
    }
}
