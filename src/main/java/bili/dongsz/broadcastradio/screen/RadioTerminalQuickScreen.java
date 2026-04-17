package bili.dongsz.broadcastradio.screen;

import bili.dongsz.broadcastradio.item.RadioTerminalItem;
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
        // E键也可以关闭界面
        if (keyCode == 69 || keyCode == 256) { // 69是E键，256是ESC键
            this.onClose();
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean isPauseScreen() {
        return false; // 打开时不暂停游戏
    }

    @Override
    protected void init() {
        super.init();
        this.lastServiceCheckTime = System.currentTimeMillis();
        
        int x = (this.width - this.imageWidth) / 2;
        int y = (this.height - this.imageHeight) / 2;
        
        // 添加SMS按钮
        this.addRenderableWidget(Button.builder(
            Component.translatable("item.broadcast_radio.radio_terminal.sms_button"),
            button -> openSMSScreen()
        ).bounds(x + 50, y + 30, 75, 20).build());
    }
    
    private void openSMSScreen() {
        if (Minecraft.getInstance().player != null) {
            Minecraft.getInstance().setScreen(new RadioTerminalSMSScreen(this, Minecraft.getInstance().player));
        }
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(guiGraphics);
        int x = (this.width - this.imageWidth) / 2;
        int y = (this.height - this.imageHeight) / 2;
        
        // 绘制GUI背景
        guiGraphics.fill(x, y, x + this.imageWidth, y + this.imageHeight, 0xFF8B8B8B);
        guiGraphics.fill(x, y, x + this.imageWidth, y + 1, 0xFF333333);
        guiGraphics.fill(x, y, x + 1, y + this.imageHeight, 0xFF333333);
        guiGraphics.fill(x + this.imageWidth - 1, y, x + this.imageWidth, y + this.imageHeight, 0xFF333333);
        guiGraphics.fill(x, y + this.imageHeight - 1, x + this.imageWidth, y + this.imageHeight, 0xFF333333);
        
        // 电量，左下角
        int batteryLevel = 0;
        if (this.minecraft != null && this.minecraft.player != null) {
            ItemStack terminalStack = this.minecraft.player.getMainHandItem();
            if (!terminalStack.isEmpty() && terminalStack.getItem() instanceof RadioTerminalItem) {
                batteryLevel = RadioTerminalItem.getBatteryLevel(terminalStack);
            }
        }
        String batteryText = batteryLevel + "%";
        guiGraphics.drawString(this.font, batteryText, x + 5, y + this.imageHeight - 15, 0x404040);
        
        // 服务名，右下角 - 连续自动检查
        String currentServiceName = this.cachedServiceName;
        if (this.minecraft != null && this.minecraft.level != null && this.minecraft.player != null) {
            // 如果不在搜索中，自动开始下一次搜索
            if (!this.isSearching) {
                this.isSearching = true;
                
                // 异步搜索基站，避免卡顿
                new Thread(() -> {
                    try {
                        // 模拟搜索延迟，调整为原先的1/6（约15ms）
                        Thread.sleep(15); // 固定延迟15ms
                        
                        String newServiceName = RadioTerminalItem.getCurrentServiceName(this.minecraft.level, this.minecraft.player);
                        
                        // 在主线程中更新结果
                        if (this.minecraft != null) {
                            this.minecraft.execute(() -> {
                                this.cachedServiceName = newServiceName;
                                this.isSearching = false; // 搜索完成，准备下一次
                                this.lastServiceCheckTime = System.currentTimeMillis();
                            });
                        }
                    } catch (Exception e) {
                        if (this.minecraft != null) {
                            this.minecraft.execute(() -> {
                                this.isSearching = false; // 搜索失败，准备下一次
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
        
        // 界面标题
        guiGraphics.drawString(this.font, this.title, x + 8, y + 6, 0x404040);
    }
}