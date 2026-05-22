package hifumi.cresora.mixin;

import hifumi.cresora.combat.BaaMimicService;
import hifumi.cresora.weapon.WeaponSkillService;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.passive.SheepEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.ActionResult;
import net.minecraft.util.DyeColor;
import net.minecraft.util.Hand;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(SheepEntity.class)
public abstract class SheepEntityMixin {

    @Inject(
            method = "interactMob(Lnet/minecraft/entity/player/PlayerEntity;Lnet/minecraft/util/Hand;)Lnet/minecraft/util/ActionResult;",
            at = @At("TAIL")
    )
    private void cresoraDoubleWoolOnSheared(PlayerEntity player, Hand hand, CallbackInfoReturnable<ActionResult> cir) {
        SheepEntity sheep = (SheepEntity) (Object) this;
        if (BaaMimicService.INSTANCE.isMimicSheep(sheep)) {
            return;
        }
        if (!(player instanceof ServerPlayerEntity serverPlayer)) {
            return;
        }
        if (!cir.getReturnValue().isAccepted()) {
            return;
        }
        ItemStack interactionStack = player.getStackInHand(hand);
        if (!interactionStack.isOf(Items.SHEARS) || !sheep.isSheared()) {
            return;
        }
        if (!WeaponSkillService.INSTANCE.hasWeaponInInventory(serverPlayer, "cadenza_allegro")) {
            return;
        }
        if (!(sheep.getWorld() instanceof ServerWorld world)) {
            return;
        }
        sheep.dropStack(world, new ItemStack(getWoolItem(sheep.getColor()), 1));
    }

    private net.minecraft.item.Item getWoolItem(DyeColor color) {
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
