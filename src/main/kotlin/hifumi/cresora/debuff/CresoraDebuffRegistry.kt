package hifumi.cresora.debuff
import hifumi.cresora.CreSoraUtilities
import net.minecraft.entity.attribute.EntityAttributeModifier
import net.minecraft.entity.attribute.EntityAttributes
import net.minecraft.entity.effect.StatusEffectInstance
import net.minecraft.entity.effect.StatusEffects
import net.minecraft.server.network.ServerPlayerEntity
import net.minecraft.server.world.ServerWorld
import net.minecraft.util.Identifier

object CresoraDebuffRegistry {
    private val debuffs = mutableMapOf<Identifier, CresoraDebuff>()

    val NERVE_DAMAGE = register(NerveDamageDebuff())
    val ROOT = register(RootDebuff())
    val SMOKE = register(SmokeDebuff())
    val BURN = register(BurnDebuff())
    val COOLDOWN_PENALTY = register(CooldownPenaltyDebuff())
    val HEAL_BLOCK = register(HealBlockDebuff())

    fun register(debuff: CresoraDebuff): CresoraDebuff {
        debuffs[debuff.id] = debuff
        return debuff
    }

    fun get(id: Identifier): CresoraDebuff? = debuffs[id]

    class NerveDamageDebuff : CresoraDebuff {
        override val id = Identifier.of(CreSoraUtilities.MOD_ID, "nerve_damage")
        override val nameKey = "debuff.cresora.nerve_damage"
        
        override fun attackMultiplier(player: ServerPlayerEntity): Double = 0.85
        override fun incomingDamageMultiplier(player: ServerPlayerEntity): Double = 1.12
    }

    class RootDebuff : CresoraDebuff {
        private val MODIFIER_ID = Identifier.of(CreSoraUtilities.MOD_ID, "root_speed_reduction")
        override val id = Identifier.of(CreSoraUtilities.MOD_ID, "root")
        override val nameKey = "debuff.cresora.root"

        override fun onApply(player: ServerPlayerEntity) {
            val speed = player.getAttributeInstance(EntityAttributes.MOVEMENT_SPEED)
            speed?.removeModifier(MODIFIER_ID)
            speed?.addTemporaryModifier(EntityAttributeModifier(MODIFIER_ID, -1.0, EntityAttributeModifier.Operation.ADD_MULTIPLIED_TOTAL))
        }

        override fun onRemove(player: ServerPlayerEntity) {
            player.getAttributeInstance(EntityAttributes.MOVEMENT_SPEED)?.removeModifier(MODIFIER_ID)
        }
    }

    class SmokeDebuff : CresoraDebuff {
        override val id = Identifier.of(CreSoraUtilities.MOD_ID, "smoke")
        override val nameKey = "debuff.cresora.smoke"

        override fun onTick(player: ServerPlayerEntity, remainingTicks: Int) {
            if (remainingTicks % 20 == 0) {
                player.addStatusEffect(StatusEffectInstance(StatusEffects.BLINDNESS, 40, 0, false, false, true))
            }
        }
    }

    class BurnDebuff : CresoraDebuff {
        override val id = Identifier.of(CreSoraUtilities.MOD_ID, "burn")
        override val nameKey = "debuff.cresora.burn"

        override fun onTick(player: ServerPlayerEntity, remainingTicks: Int) {
            if (remainingTicks % 20 == 0) {
                val world = player.world as? ServerWorld ?: return
                player.damage(world, player.damageSources.onFire(), 2.0f)
            }
        }
    }

    class CooldownPenaltyDebuff : CresoraDebuff {
        override val id = Identifier.of(CreSoraUtilities.MOD_ID, "cooldown_penalty")
        override val nameKey = "debuff.cresora.cooldown_penalty"
        
        override fun cooldownMultiplier(player: ServerPlayerEntity): Double = 0.5 // Progression is 50% slower
    }

    class HealBlockDebuff : CresoraDebuff {
        override val id = Identifier.of(CreSoraUtilities.MOD_ID, "heal_block")
        override val nameKey = "debuff.cresora.heal_block"
        
        override fun canHeal(player: ServerPlayerEntity): Boolean = false
    }
}
