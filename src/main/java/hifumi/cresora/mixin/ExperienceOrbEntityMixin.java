package hifumi.cresora.mixin;

import hifumi.cresora.CreditsService;
import net.minecraft.entity.ExperienceOrbEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(ExperienceOrbEntity.class)
public abstract class ExperienceOrbEntityMixin {
    @Redirect(
        method = "onPlayerCollision",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/entity/player/PlayerEntity;addExperience(I)V"
        )
    )
    private void cresora$awardCreditsOnlyForOrbPickup(PlayerEntity player, int experience) {
        if (experience > 0 && player instanceof ServerPlayerEntity) {
            ServerPlayerEntity serverPlayer = (ServerPlayerEntity) player;
            CreditsService.INSTANCE.addExperienceReward(serverPlayer, experience);
        }
        player.addExperience(experience);
    }
}
