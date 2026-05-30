package hifumi.cresora.mixin;

import hifumi.cresora.bloodmoon.BloodMoonService;
import hifumi.cresora.combat.FieldMobPackService;

import net.minecraft.entity.EntityData;
import net.minecraft.entity.SpawnReason;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.world.LocalDifficulty;
import net.minecraft.world.ServerWorldAccess;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(MobEntity.class)
public abstract class MobEntitySpawnMixin {
    @Inject(method = "initialize", at = @At("TAIL"))
    private void cresora$initializeClassification(
        ServerWorldAccess world,
        LocalDifficulty difficulty,
        SpawnReason spawnReason,
        EntityData entityData,
        CallbackInfoReturnable<EntityData> cir
    ) {
        if (!((Object) this instanceof net.minecraft.entity.mob.Monster || (Object) this instanceof HostileEntity)) {
            return;
        }
        MobEntity hostile = (MobEntity) (Object) this;
        if (!(world instanceof ServerWorld serverWorld)) {
            return;
        }
        FieldMobPackService.INSTANCE.initializeOnSpawn(hostile, serverWorld, spawnReason);
        BloodMoonService.INSTANCE.maybeDuplicateNaturalSpawn(hostile, serverWorld, spawnReason);
    }
}
