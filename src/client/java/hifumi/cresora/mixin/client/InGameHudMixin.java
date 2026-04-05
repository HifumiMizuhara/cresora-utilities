package hifumi.cresora.mixin.client;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.hud.InGameHud;
import net.minecraft.entity.player.PlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(InGameHud.class)
public class InGameHudMixin {

    /**
     * 1.21.7 の renderHealthBar シグネチャ（absorption引数あり）:
     * renderHealthBar(DrawContext, PlayerEntity, int x, int y, int lines, int regeneratingHeartIndex,
     *                 float maxHealth, int lastHealth, int health, int absorption, boolean blinking)
     */
    @Inject(method = "renderHealthBar", at = @At("HEAD"), cancellable = true)
    private void onRenderHealthBar(DrawContext context, PlayerEntity player,
                                   int x, int y, int lines, int regeneratingHeartIndex,
                                   float maxHealth, int lastHealth, int health,
                                   int absorption, boolean blinking, CallbackInfo ci) {
        ci.cancel();

        MinecraftClient client = MinecraftClient.getInstance();
        if (client == null || client.textRenderer == null) return;

        // renderHealthBar の health 引数はバニラHUD用の遅延演出値を含むため、
        // 数値表示はプレイヤー実値を直接読む。
        int currentHp = Math.max((int) Math.ceil(player.getHealth()), 0);
        int maxHp = Math.max((int) Math.ceil(player.getMaxHealth()), 1);
        int absorptionHp = Math.max((int) Math.ceil(player.getAbsorptionAmount()), 0);
        String fullText = absorptionHp > 0
                ? "HP: " + currentHp + " / " + maxHp + " +" + absorptionHp
                : "HP: " + currentHp + " / " + maxHp;

        // ハートが描画される y 座標にそのまま文字を描画（0xFF5555 にアルファチャンネル 0xFF を追加して不透明にする）
        context.drawText(client.textRenderer, fullText, x, y, 0xFFFF5555, true);
    }
}
