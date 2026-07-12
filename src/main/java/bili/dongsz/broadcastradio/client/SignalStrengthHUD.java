package bili.dongsz.broadcastradio.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderGuiOverlayEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * 客户端 HUD，显示最近一次无线电消息的信号强度与质量。
 */
@Mod.EventBusSubscriber(modid = "broadcast_radio", bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public class SignalStrengthHUD {

    private static final int DISPLAY_TICKS_TOTAL = 200;      // 显示 10 秒（前 5 秒不透明，后 5 秒淡出）
    private static final int FADE_TICKS = 100;               // 最后 5 秒渐隐
    private static int remainingTicks = 0;
    private static int currentStrength = 0;
    private static int currentInterference = 0;

    /** 供网络包处理器调用：更新并重置计时器。 */
    public static void updateSignal(int signalStrength, int interference) {
        currentStrength = clamp(signalStrength, 0, 100);
        currentInterference = clamp(interference, 0, 100);
        remainingTicks = DISPLAY_TICKS_TOTAL;
    }

    private static int clamp(int v, int lo, int hi) {
        return v < lo ? lo : (v > hi ? hi : v);
    }

    @SubscribeEvent
    public static void onRenderOverlay(RenderGuiOverlayEvent.Post event) {
        if (remainingTicks <= 0) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;

        GuiGraphics guiGraphics = event.getGuiGraphics();
        Font font = mc.font;

        float alpha = 1.0f;
        if (remainingTicks <= FADE_TICKS) {
            alpha = remainingTicks / (float) FADE_TICKS;
        }
        int alphaInt = (int) (alpha * 255.0f);
        if (alphaInt < 8) alphaInt = 8;

        Component strengthComp = formatStrength(currentStrength);
        Component qualityComp = formatQuality(currentInterference);

        String combined = strengthComp.getString() + "   " + qualityComp.getString();
        int totalWidth = font.width(combined);

        int screenWidth = mc.getWindow().getGuiScaledWidth();
        int screenHeight = mc.getWindow().getGuiScaledHeight();

        int y = screenHeight - 59;
        int x = (screenWidth - totalWidth) / 2;

        int bgColor = (alphaInt / 2) << 24 | 0x000000;
        guiGraphics.fill(x - 6, y - 4, x + totalWidth + 6, y + 10, bgColor);

        drawColoredText(guiGraphics, font, strengthComp, x, y, currentStrength, alphaInt, true);
        int strengthWidth = font.width(strengthComp.getString()) + 12;

        drawColoredText(guiGraphics, font, qualityComp, x + strengthWidth, y, currentInterference, alphaInt, false);

        remainingTicks--;
    }

    private static Component formatStrength(int strength) {
        String key;
        if (strength >= 81) key = "broadcast_radio.signal.strength_high";
        else if (strength >= 61) key = "broadcast_radio.signal.strength_mid_high";
        else if (strength >= 41) key = "broadcast_radio.signal.strength_mid";
        else if (strength >= 21) key = "broadcast_radio.signal.strength_mid_low";
        else key = "broadcast_radio.signal.strength_low";
        return Component.translatable(key, String.valueOf(strength));
    }

    private static Component formatQuality(int interference) {
        String key;
        if (interference <= 20) key = "broadcast_radio.signal.quality_excellent";
        else if (interference <= 50) key = "broadcast_radio.signal.quality_good";
        else if (interference <= 80) key = "broadcast_radio.signal.quality_poor";
        else if (interference <= 90) key = "broadcast_radio.signal.quality_terrible";
        else key = "broadcast_radio.signal.quality_none";
        return Component.translatable(key);
    }

    private static void drawColoredText(GuiGraphics guiGraphics, Font font, Component component,
                                        int x, int y, int value, int alpha, boolean isStrength) {
        int color;
        if (isStrength) {
            if (value >= 81) color = 0x00FF55;
            else if (value >= 61) color = 0x99FF33;
            else if (value >= 41) color = 0xFFFF00;
            else if (value >= 21) color = 0xFF9900;
            else color = 0xFF3333;
        } else {
            if (value <= 20) color = 0x00FF55;
            else if (value <= 50) color = 0xFFFF00;
            else if (value <= 80) color = 0xFF9900;
            else color = 0xFF3333;
        }
        int colorWithAlpha = (alpha << 24) | (color & 0x00FFFFFF);
        guiGraphics.drawString(font, component, x, y, colorWithAlpha, false);
    }
}