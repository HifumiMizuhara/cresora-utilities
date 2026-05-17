package hifumi.cresora.mixin;

import hifumi.cresora.AdventureRankMobAccess;
import hifumi.cresora.AdventureRankService;
import hifumi.cresora.CombatDamageType;
import hifumi.cresora.CombatDamageTypeSupport;
import hifumi.cresora.CresoraDebuffService;
import hifumi.cresora.EquipmentEffectHookService;
import hifumi.cresora.MasqueradeService;
import hifumi.cresora.MobCombatProfileRegistry;
import hifumi.cresora.MusicEchoContentRegistry;
import hifumi.cresora.NaturalRegenService;
import hifumi.cresora.StoryService;
import hifumi.cresora.WeaponSkillService;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.passive.SheepEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.DyeColor;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LivingEntity.class)
public class LivingEntityMixin {
    @Unique
    private float cresora$preDamageHealth;

    @ModifyVariable(method = "damage", at = @At("HEAD"), argsOnly = true)
    private float cresora$applyCombatScaling(float amount, net.minecraft.server.world.ServerWorld world, DamageSource source) {
        if ((Object) this instanceof MobEntity) {
            amount = (float) (amount * MusicEchoContentRegistry.INSTANCE.mobDamageTakenMultiplier());
        }
        if ((Object) this instanceof HostileEntity hostile) {
            amount = (float) (amount * MasqueradeService.INSTANCE.damageTakenMultiplier(hostile));
            amount = (float) (amount * StoryService.INSTANCE.damageTakenMultiplier(hostile));
            CombatDamageType damageType = CombatDamageTypeSupport.damageSourceType(source);
            double baseResistanceRatio = MobCombatProfileRegistry.INSTANCE.resistancePercent(hostile.getType(), damageType) / 100.0D;
            double resistanceOffset = (damageType == CombatDamageType.PHYSICAL)
                    ? WeaponSkillService.INSTANCE.getPhysicalResistanceOffset(hostile)
                    : WeaponSkillService.INSTANCE.getArcaneResistanceOffset(hostile);
            double resistanceRatio = baseResistanceRatio - resistanceOffset;
            if (resistanceRatio != 0.0D) {
                amount = (float) (amount * (1.0D - (float) resistanceRatio));
            }
        }
        double enemyMultiplier = AdventureRankService.INSTANCE.damageMultiplier(source);
        if (enemyMultiplier > 1.0D) {
            amount = (float) (amount * enemyMultiplier);
        }
        if (!((Object) this instanceof PlayerEntity)) {
            return amount;
        }
        PlayerEntity player = (PlayerEntity) (Object) this;
        if (player instanceof ServerPlayerEntity serverPlayer) {
            amount = WeaponSkillService.INSTANCE.onDamageTaken(serverPlayer, amount);
            amount = WeaponSkillService.INSTANCE.absorbDamage(serverPlayer, amount);
        }
        return amount;
    }

    @Inject(method = "damage", at = @At("HEAD"))
    private void cresora$captureIncomingDamage(net.minecraft.server.world.ServerWorld world, DamageSource source, float amount, CallbackInfoReturnable<Boolean> cir) {
        if (source.getAttacker() instanceof ServerPlayerEntity serverPlayer) {
            NaturalRegenService.INSTANCE.markCombat(serverPlayer);
        }
        if ((Object) this instanceof ServerPlayerEntity serverPlayer && source.getAttacker() instanceof LivingEntity) {
            NaturalRegenService.INSTANCE.markCombat(serverPlayer);
        }
        this.cresora$preDamageHealth = ((LivingEntity)(Object)this).getHealth();
    }

    @Inject(method = "damage", at = @At("RETURN"))
    private void cresora$showMobDamage(net.minecraft.server.world.ServerWorld world, DamageSource source, float amount, CallbackInfoReturnable<Boolean> cir) {
        if (!cir.getReturnValueZ()) {
            return;
        }
        LivingEntity entity = (LivingEntity) (Object) this;
        float damageDone = Math.max(0.0F, this.cresora$preDamageHealth - entity.getHealth());
        if (damageDone <= 0.0F) {
            return;
        }
        if (entity instanceof PlayerEntity player) {
            EquipmentEffectHookService.INSTANCE.onDamageTaken(player, source, damageDone);
            if (player instanceof ServerPlayerEntity serverPlayer) {
                AdventureRankService.INSTANCE.showPlayerDamageFeedback(serverPlayer, source, damageDone);
                if (source.getAttacker() instanceof HostileEntity hostile && hostile instanceof AdventureRankMobAccess access) {
                    if (access.cresoraIsEliteMob()) {
                        CresoraDebuffService.INSTANCE.onEliteHit(serverPlayer, hostile);
                    }
                }
            }
            return;
        }
        if (!(entity instanceof HostileEntity hostile)) {
            return;
        }
        if (source.getAttacker() instanceof PlayerEntity player) {
            EquipmentEffectHookService.INSTANCE.onAttackDealt(player, hostile, damageDone);
            if (player instanceof ServerPlayerEntity serverPlayer) {
                WeaponSkillService.INSTANCE.onAttackDealt(serverPlayer, hostile, damageDone);
            }
        }
        AdventureRankService.INSTANCE.showMobDamage(hostile, source, damageDone);
        AdventureRankService.INSTANCE.refreshMobDisplay(hostile);
    }

    @Inject(
            method = "dropLoot(Lnet/minecraft/server/world/ServerWorld;Lnet/minecraft/entity/damage/DamageSource;Z)V",
            at = @At("HEAD"),
            cancellable = true
    )
    private void cresora$replaceMimicSheepLoot(ServerWorld world, DamageSource source, boolean causedByPlayer, CallbackInfo ci) {
        if (!((Object) this instanceof SheepEntity sheep)) {
            return;
        }
        if (hifumi.cresora.BaaMimicService.INSTANCE.handleMimicSheepDeath(sheep, source)) {
            ci.cancel();
        }
    }

    @Inject(
            method = "dropLoot(Lnet/minecraft/server/world/ServerWorld;Lnet/minecraft/entity/damage/DamageSource;Z)V",
            at = @At("TAIL")
    )
    private void cresora$doubleWoolOnSheepDeath(ServerWorld world, DamageSource source, boolean causedByPlayer, CallbackInfo ci) {
        if (!((Object) this instanceof SheepEntity sheep)) {
            return;
        }
        if (hifumi.cresora.BaaMimicService.INSTANCE.isMimicSheep(sheep)) {
            return;
        }
        if (!(source.getAttacker() instanceof ServerPlayerEntity player)) {
            return;
        }
        if (!WeaponSkillService.INSTANCE.hasWeaponInInventory(player, "cadenza_allegro")) {
            return;
        }
        sheep.dropStack(world, new ItemStack(cresora$getWoolItem(sheep.getColor()), 1));
    }

    @Unique
    private Item cresora$getWoolItem(DyeColor color) {
        return switch (color) {
            case WHITE -> Items.WHITE_WOOL;
            case ORANGE -> Items.ORANGE_WOOL;
            case MAGENTA -> Items.MAGENTA_WOOL;
            case LIGHT_BLUE -> Items.LIGHT_BLUE_WOOL;
            case YELLOW -> Items.YELLOW_WOOL;
            case LIME -> Items.LIME_WOOL;
            case PINK -> Items.PINK_WOOL;
            case GRAY -> Items.GRAY_WOOL;
            case LIGHT_GRAY -> Items.LIGHT_GRAY_WOOL;
            case CYAN -> Items.CYAN_WOOL;
            case PURPLE -> Items.PURPLE_WOOL;
            case BLUE -> Items.BLUE_WOOL;
            case BROWN -> Items.BROWN_WOOL;
            case GREEN -> Items.GREEN_WOOL;
            case RED -> Items.RED_WOOL;
            case BLACK -> Items.BLACK_WOOL;
        };
    }
}
