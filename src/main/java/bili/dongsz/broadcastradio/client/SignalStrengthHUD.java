package bili.dongsz.broadcastradio.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderGuiOverlayEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import bili.dongsz.broadcastradio.BroadcastRadio;

/**
 * 客户端 HUD，显示最近一次无线电消息的信号强度与质量。
 *
 * 图层顺序（从下到上，后绘制覆盖先绘制）：
 *   1. 信号强度指示（本类，在 HOTBAR_OVERLAY 的 Pre 阶段绘制）
 *   2. 快捷栏背景纹理
 *   3. 快捷栏物品图标与数量
 *
 * 动画：
 *   - 基于帧时间（delta time）的平滑动画，帧率独立
 *   - 弹入 0.3 秒 + 停留 4 秒 + 滑出 0.3 秒
 *   - 缓动函数：ease-out cubic
 */
@Mod.EventBusSubscriber(modid = "broadcast_radio", bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public class SignalStrengthHUD {

    // ========== 时间轴配置（秒）==========
    private static final float SLIDE_IN_SECONDS  = 0.3f;  // 弹入 0.3 秒
    private static final float HOLD_SECONDS       = 4.0f;  // 停留 4 秒
    private static final float SLIDE_OUT_SECONDS = 0.3f;  // 滑出 0.3 秒
    private static final float TOTAL_SECONDS = SLIDE_IN_SECONDS + HOLD_SECONDS + SLIDE_OUT_SECONDS;

    // ========== 视觉常量（与模组 GUI 风格一致）==========
    private static final int BG_COLOR_RGB   = 0x1E1E1E;   // 近黑色深灰背景
    private static final int BG_ALPHA       = 0xB0;       // 约 69% 不透明
    private static final int BORDER_COLOR   = 0xFF333333; // 与模组 GUI 边框色一致
    private static final int LABEL_COLOR_RGB = 0xAAAAAA;  // 标签浅灰

    // ========== 尺寸与位置常量 ==========
    private static final int BOX_HEIGHT    = 22;
    private static final int PADDING_X     = 10;
    private static final int HOTBAR_HEIGHT = 19;   // 快捷栏高度
    private static final int GAP_ABOVE_HOTBAR = 4; // 指示器与快捷栏的间距
    private static final int SLIDE_DISTANCE = 100; // 弹入/滑出的垂直位移像素数

    // ========== 运行状态（使用纳秒时间戳，与帧率无关）==========
    private static long animationStartNs = 0;
    private static boolean animationActive = false;
    private static int currentStrength = 0;
    private static int currentInterference = 0;

    static {
        BroadcastRadio.LOGGER.info("[SignalStrengthHUD] 客户端 HUD 类已加载，事件监听器已注册");
    }

    /**
     * 供网络包处理器调用：更新信号数据并重置动画。
     */
    public static void updateSignal(int signalStrength, int interference) {
        int oldStrength = currentStrength;
        int oldInterference = currentInterference;

        currentStrength = clamp(signalStrength, 0, 100);
        currentInterference = clamp(interference, 0, 100);
        animationStartNs = System.nanoTime();
        animationActive = true;

        BroadcastRadio.LOGGER.info(
            "[SignalStrengthHUD] updateSignal 被调用 | " +
            "旧: 强度={} 干扰={} | 新: 强度={} 干扰={}",
            oldStrength, oldInterference,
            currentStrength, currentInterference
        );
    }

    /**
     * 测试用方法：手动触发显示。
     */
    public static void triggerTestDisplay() {
        currentStrength = 85;
        currentInterference = 10;
        animationStartNs = System.nanoTime();
        animationActive = true;
        BroadcastRadio.LOGGER.info("[SignalStrengthHUD] 测试显示已触发：强度=85 干扰=10");
    }

    private static int clamp(int v, int lo, int hi) {
        return v < lo ? lo : (v > hi ? hi : v);
    }

    /**
     * ease-out cubic: 1 - (1 - t)³
     */
    private static float easeOutCubic(float t) {
        if (t <= 0.0f) return 0.0f;
        if (t >= 1.0f) return 1.0f;
        float oneMinusT = 1.0f - t;
        return 1.0f - oneMinusT * oneMinusT * oneMinusT;
    }

    // ==================================================================
    // 渲染入口：仅在快捷栏覆盖层（ID = minecraft:hotbar）渲染之前绘制一次
    // 这确保图层顺序为：指示器（先）→ 快捷栏背景 → 快捷栏物品（后）
    // ==================================================================
    @SubscribeEvent
    public static void onRenderOverlay(RenderGuiOverlayEvent.Pre event) {
        // 仅在即将绘制快捷栏覆盖层时绘制（通过 overlay 的 id() 方法判断）
        // Forge 1.20.1 的 RenderGuiOverlayEvent.Pre.getOverlay() 返回 NamedGuiOverlay，
        // NamedGuiOverlay.id() 返回 ResourceLocation，其 toString() 为 "minecraft:hotbar"
        if (!isHotbarOverlay(event.getOverlay())) return;

        if (!animationActive) return;

        Minecraft mc = Minecraft.getInstance();
        if (mc == null) return;
        if (mc.player == null) return;
        if (mc.options.hideGui) return;

        GuiGraphics guiGraphics = event.getGuiGraphics();
        if (guiGraphics == null) return;
        Font font = mc.font;
        if (font == null) return;

        // ========== 基于 delta 时间的动画进度计算 ==========
        long nowNs = System.nanoTime();
        float elapsedSec = (nowNs - animationStartNs) / 1_000_000_000.0f;

        float alpha = 1.0f;
        float eased = 1.0f;
        boolean slideIn = false;
        boolean slideOut = false;

        if (elapsedSec < 0.0f) {
            return;
        } else if (elapsedSec < SLIDE_IN_SECONDS) {
            // ---------- 阶段 1：弹入（0 ~ 0.3 秒）----------
            float progress = elapsedSec / SLIDE_IN_SECONDS;
            eased = easeOutCubic(progress);
            alpha = eased;
            slideIn = true;
        } else if (elapsedSec < SLIDE_IN_SECONDS + HOLD_SECONDS) {
            // ---------- 阶段 2：静止显示（0.3 ~ 4.3 秒）----------
            eased = 1.0f;
            alpha = 1.0f;
        } else if (elapsedSec < TOTAL_SECONDS) {
            // ---------- 阶段 3：滑出（4.3 ~ 4.6 秒）----------
            float progress = (elapsedSec - SLIDE_IN_SECONDS - HOLD_SECONDS) / SLIDE_OUT_SECONDS;
            eased = easeOutCubic(progress);
            alpha = 1.0f - eased;
            slideOut = true;
        } else {
            // ---------- 动画结束 ----------
            animationActive = false;
            return;
        }

        // ========== 计算显示内容宽度 ==========
        String strengthLabel = Component.translatable("broadcast_radio.signal.hud_strength").getString();
        String qualityLabel  = Component.translatable("broadcast_radio.signal.hud_quality").getString();
        String strengthValue = currentStrength + "%";
        String qualityValue  = Component.translatable(getQualityKey(currentInterference)).getString();
        String strengthText  = strengthLabel + " " + strengthValue;
        String qualityText   = qualityLabel + " " + qualityValue;

        int strengthWidth = font.width(strengthText);
        int qualityWidth  = font.width(qualityText);
        int gapBetween = 24;
        int innerContentWidth = strengthWidth + gapBetween + qualityWidth;
        int boxWidth = innerContentWidth + PADDING_X * 2;

        int screenWidth  = mc.getWindow().getGuiScaledWidth();
        int screenHeight = mc.getWindow().getGuiScaledHeight();

        int boxX = (screenWidth - boxWidth) / 2;

        // ========== 位置计算（置于快捷栏上方，不重叠）==========
        // 快捷栏区域: [screenHeight - HOTBAR_HEIGHT, screenHeight]
        // 指示器区域: [targetY, targetY + BOX_HEIGHT]
        // 两者之间留有 GAP_ABOVE_HOTBAR 像素的间距
        int targetY = screenHeight - HOTBAR_HEIGHT - GAP_ABOVE_HOTBAR - BOX_HEIGHT;

        // 根据阶段计算实际 Y：从屏幕下方偏 SLIDE_DISTANCE 像素处滑入/滑出
        int boxY;
        if (slideIn) {
            int offsetY = (int)((1.0f - eased) * SLIDE_DISTANCE);
            boxY = targetY + offsetY;
        } else if (slideOut) {
            int offsetY = (int)(eased * SLIDE_DISTANCE);
            boxY = targetY + offsetY;
        } else {
            boxY = targetY;
        }

        // 透明度太低时不绘制
        if (alpha < 0.02f) return;

        // ========== 颜色计算（应用 alpha）==========
        int alphaInt = (int)(alpha * 255.0f);
        if (alphaInt < 4) alphaInt = 4;
        int bgAlphaInt = (int)(alpha * (BG_ALPHA & 0xFF));
        if (bgAlphaInt < 2) bgAlphaInt = 2;

        int bgColor     = (bgAlphaInt << 24) | (BG_COLOR_RGB & 0x00FFFFFF);
        int borderColor = (alphaInt  << 24) | (BORDER_COLOR & 0x00FFFFFF);
        int labelColor  = (alphaInt  << 24) | (LABEL_COLOR_RGB & 0x00FFFFFF);

        // ========== 绘制背景 ==========
        guiGraphics.fill(boxX, boxY, boxX + boxWidth, boxY + BOX_HEIGHT, bgColor);

        // ========== 绘制边框（上下左右各 1px）==========
        guiGraphics.fill(boxX, boxY, boxX + boxWidth, boxY + 1, borderColor);
        guiGraphics.fill(boxX, boxY + BOX_HEIGHT - 1, boxX + boxWidth, boxY + BOX_HEIGHT, borderColor);
        guiGraphics.fill(boxX, boxY, boxX + 1, boxY + BOX_HEIGHT, borderColor);
        guiGraphics.fill(boxX + boxWidth - 1, boxY, boxX + boxWidth, boxY + BOX_HEIGHT, borderColor);

        // ========== 绘制文本（标签 + 数值，数值根据信号/质量等级着色）==========
        int textHeight = 9;
        int textY = boxY + (BOX_HEIGHT - textHeight) / 2 + 1;

        // 左侧：信号强度（"信号 85%"）
        int textLeftX = boxX + PADDING_X;
        guiGraphics.drawString(font, strengthLabel, textLeftX, textY, labelColor, false);
        int labelW = font.width(strengthLabel);
        guiGraphics.drawString(font, " " + strengthValue, textLeftX + labelW, textY,
            getStrengthColor(currentStrength, alphaInt), false);

        // 右侧：信号质量（"质量 优"）
        int qualityTextTotalX = boxX + boxWidth - PADDING_X - font.width(qualityText);
        guiGraphics.drawString(font, qualityLabel, qualityTextTotalX, textY, labelColor, false);
        int qLabelW = font.width(qualityLabel);
        guiGraphics.drawString(font, " " + qualityValue, qualityTextTotalX + qLabelW, textY,
            getQualityColor(currentInterference, alphaInt), false);
    }

    // ========== 文本与颜色格式化（新版质量等级划分）==========

    private static String getQualityKey(int interference) {
        if (interference <= 0) return "broadcast_radio.signal.quality_excellent";
        if (interference <= 30) return "broadcast_radio.signal.quality_good";
        if (interference <= 70) return "broadcast_radio.signal.quality_poor";
        if (interference <= 90) return "broadcast_radio.signal.quality_terrible";
        return "broadcast_radio.signal.quality_none";
    }

    private static int getStrengthColor(int strength, int alphaInt) {
        int color;
        if (strength >= 81) color = 0x66FF99;
        else if (strength >= 61) color = 0xA8E063;
        else if (strength >= 41) color = 0xFFD84D;
        else if (strength >= 21) color = 0xFF9F43;
        else color = 0xFF6B6B;
        return (alphaInt << 24) | (color & 0x00FFFFFF);
    }

    /**
     * 判断当前 overlay 是否为快捷栏 overlay。
     * 通过反射调用 overlay.id() 方法获取 ResourceLocation，检查是否包含 "hotbar"。
     * 这对 Forge 1.20.1 的 NamedGuiOverlay 和其他版本的 IGuiOverlay 都兼容。
     */
    private static boolean isHotbarOverlay(Object overlay) {
        if (overlay == null) return false;
        try {
            java.lang.reflect.Method idMethod = overlay.getClass().getMethod("id");
            Object id = idMethod.invoke(overlay);
            if (id != null) {
                String idStr = id.toString().toLowerCase(java.util.Locale.ROOT);
                return idStr.contains("hotbar");
            }
        } catch (Exception ignored) {
        }
        return false;
    }

    private static int getQualityColor(int interference, int alphaInt) {
        int color;
        if (interference <= 0) color = 0x66FF99;
        else if (interference <= 30) color = 0xA8E063;
        else if (interference <= 70) color = 0xFFD84D;
        else if (interference <= 90) color = 0xFF9F43;
        else color = 0xFF6B6B;
        return (alphaInt << 24) | (color & 0x00FFFFFF);
    }
}