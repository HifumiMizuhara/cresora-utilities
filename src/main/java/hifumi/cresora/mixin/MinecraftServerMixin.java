package hifumi.cresora.mixin;

import hifumi.cresora.CreSoraUtilities;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(MinecraftServer.class)
public class MinecraftServerMixin {
    @Inject(method = "isSpawnProtected", at = @At("HEAD"), cancellable = true)
    private void cresora$bypassPortalSpawnProtection(ServerWorld world, BlockPos pos, PlayerEntity player, CallbackInfoReturnable<Boolean> cir) {
        if (world.getBlockState(pos).isOf(CreSoraUtilities.INSTANCE.getCRESORA_PORTAL_BLOCK())) {
            cir.setReturnValue(false);
        }
    }
}
