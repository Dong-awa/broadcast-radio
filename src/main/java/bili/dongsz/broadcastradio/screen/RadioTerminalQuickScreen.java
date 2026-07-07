package bili.dongsz.broadcastradio.screen;

import bili.dongsz.broadcastradio.item.RadioTerminalItem;
import bili.dongsz.broadcastradio.utils.SignalSearchManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

public class RadioTerminalQuickScreen extends Screen {
    private long lastServiceCheckTime = 0;
    private boolean isSearching = false;
    private String cachedServiceName = Component.translatable("item.broadcast_radio.radio_terminal.no_signal").getString();
    private int imageWidth = 175;
    private int imageHeight = 100;

    public RadioTerminalQuickScreen() {
        super(Component.translatable("item.broadcast_radio.radio_terminal.gui_title"));
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == 69 || keyCode == 256) {
            this.onClose();
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    protected void init() {
        super.init();
        this.lastServiceCheckTime = System.currentTimeMillis();

        SignalSearchManager searchManager = SignalSearchManager.getInstance();
        if (searchManager.isRunning()) {
            this.cachedServiceName = searchManager.getCachedServiceName();
        }

        int x = (this.width - this.imageWidth) / 2;
        int y = (this.height - this.imageHeight) / 2;

        this.addRenderableWidget(Button.builder(
            Component.translatable("item.broadcast_radio.radio_terminal.sms_button"),
            button -> openSMSScreen()
        ).bounds(x + 50, y + 30, 75, 20).build());

        this.addRenderableWidget(Button.builder(
            Component.translatable("item.broadcast_radio.radio_terminal.position_button"),
            button -> openPositionScreen()
        ).bounds(x + 50, y + 55, 75, 20).build());
    }

    private void openSMSScreen() {
        if (Minecraft.getInstance().player != null) {
            Minecraft.getInstance().setScreen(new RadioTerminalSMSScreen(this, Minecraft.getInstance().player));
        }
    }

    private void openPositionScreen() {
        Minecraft.getInstance().setScreen(new RadioTerminalPositionScreen(this));
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(guiGraphics);
        int x = (this.width - this.imageWidth) / 2;
        int y = (this.height - this.imageHeight) / 2;
        
        // GUI背景
        guiGraphics.fill(x, y, x + this.imageWidth, y + this.imageHeight, 0xFF8B8B8B);
        guiGraphics.fill(x, y, x + this.imageWidth, y + 1, 0xFF333333);
        guiGraphics.fill(x, y, x + 1, y + this.imageHeight, 0xFF333333);
        guiGraphics.fill(x + this.imageWidth - 1, y, x + this.imageWidth, y + this.imageHeight, 0xFF333333);
        guiGraphics.fill(x, y + this.imageHeight - 1, x + this.imageWidth, y + this.imageHeight, 0xFF333333);
        
        // ===== 顶部：标题（水平居中白色） =====
        Component titleLine = this.title;
        int titleLineWidth = this.font.width(titleLine);
        guiGraphics.drawString(this.font, titleLine, x + (this.imageWidth - titleLineWidth) / 2, y + 6, 0xFFFFFF);

        // 水平分隔线
        guiGraphics.fill(x + 8, y + 20, x + this.imageWidth - 8, y + 21, 0xFF333333);
        
        // 电量
        int batteryLevel = 0;
        if (this.minecraft != null && this.minecraft.player != null) {
            ItemStack terminalStack = this.minecraft.player.getMainHandItem();
            if (!terminalStack.isEmpty() && terminalStack.getItem() instanceof RadioTerminalItem) {
                batteryLevel = RadioTerminalItem.getBatteryLevel(terminalStack);
            }
        }
        String batteryText = batteryLevel + "%";
        guiGraphics.drawString(this.font, batteryText, x + 5, y + this.imageHeight - 15, 0x404040);
        
        // 服务名
        String currentServiceName = this.cachedServiceName;
        if (this.minecraft != null && this.minecraft.level != null && this.minecraft.player != null) {
            if (!this.isSearching) {
                this.isSearching = true;
                // 搜索基站
                new Thread(() -> {
                    try {
                        Thread.sleep(15);
                        
                        String newServiceName = RadioTerminalItem.getCurrentServiceName(this.minecraft.level, this.minecraft.player);
                        if (this.minecraft != null) {
                            this.minecraft.execute(() -> {
                                this.cachedServiceName = newServiceName;
                                this.isSearching = false;
                                this.lastServiceCheckTime = System.currentTimeMillis();
                            });
                        }
                    } catch (Exception e) {
                        if (this.minecraft != null) {
                            this.minecraft.execute(() -> {
                                this.isSearching = false;
                                this.lastServiceCheckTime = System.currentTimeMillis();
                            });
                        }
                    }
                }).start();
            }
        }
        int serviceNameWidth = this.font.width(currentServiceName);
        int serviceNameX = x + this.imageWidth - serviceNameWidth - 5;
        int serviceNameY = y + this.imageHeight - 15;
        guiGraphics.drawString(this.font, currentServiceName, serviceNameX, serviceNameY, 0x404040);
        
        // 渲染按钮
        super.render(guiGraphics, mouseX, mouseY, partialTick);
    }
}