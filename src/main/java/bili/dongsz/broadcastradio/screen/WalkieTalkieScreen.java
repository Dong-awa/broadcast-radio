package bili.dongsz.broadcastradio.screen;

import bili.dongsz.broadcastradio.item.RadioBatteryItem;
import bili.dongsz.broadcastradio.menu.WalkieTalkieMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;

public class WalkieTalkieScreen extends AbstractContainerScreen<WalkieTalkieMenu> {
    private float currentFrequency;
    private int startX;
    private int buttonY;
    private int buttonWidth;
    private int buttonHeight;
    private int spacing;

    public WalkieTalkieScreen(WalkieTalkieMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        this.imageWidth = 176;
        this.imageHeight = 198;
        this.currentFrequency = menu.getWalkieStack().getOrCreateTag().getFloat("Frequency");
        
        // 修正物品栏文字位置
        this.inventoryLabelX = 8; // 左边距，默认值
        this.inventoryLabelY = this.imageHeight - 94; // 底部物品栏上方的标准位置
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
        int x = (this.width - this.imageWidth) / 2;
        int y = (this.height - this.imageHeight) / 2;

        buttonWidth = 35;
        buttonHeight = 20;
        startX = x + 10;
        buttonY = y + 20;
        spacing = 5;

        // -5MHz 按钮
        this.addRenderableWidget(Button.builder(Component.translatable("item.broadcast_radio.walkie_talkie.decrease_5"), button -> {
            adjustFrequency(-5.0f);
        }).bounds(startX, buttonY, buttonWidth, buttonHeight).build());

        // -0.5MHz 按钮
        this.addRenderableWidget(Button.builder(Component.translatable("item.broadcast_radio.walkie_talkie.decrease_0_5"), button -> {
            adjustFrequency(-0.5f);
        }).bounds(startX + buttonWidth + spacing, buttonY, buttonWidth, buttonHeight).build());

        // +0.5MHz 按钮
        this.addRenderableWidget(Button.builder(Component.translatable("item.broadcast_radio.walkie_talkie.increase_0_5"), button -> {
            adjustFrequency(0.5f);
        }).bounds(startX + buttonWidth * 2 + spacing * 2, buttonY, buttonWidth, buttonHeight).build());

        // +5MHz 按钮
        this.addRenderableWidget(Button.builder(Component.translatable("item.broadcast_radio.walkie_talkie.increase_5"), button -> {
            adjustFrequency(5.0f);
        }).bounds(startX + buttonWidth * 3 + spacing * 3, buttonY, buttonWidth, buttonHeight).build());
    }

    private void adjustFrequency(float delta) {
        currentFrequency += delta;
        // 边界校验
        if (currentFrequency > 999.9f) currentFrequency = 1.0f;
        else if (currentFrequency < 1.0f) currentFrequency = 999.9f;
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        int x = (this.width - this.imageWidth) / 2;
        int y = (this.height - this.imageHeight) / 2;
        guiGraphics.fill(x, y, x + this.imageWidth, y + this.imageHeight, 0xFF8B8B8B);
        guiGraphics.fill(x, y, x + this.imageWidth, y + 1, 0xFF333333);
        guiGraphics.fill(x, y, x + 1, y + this.imageHeight, 0xFF333333);
        guiGraphics.fill(x + this.imageWidth - 1, y, x + this.imageWidth, y + this.imageHeight, 0xFF333333);
        guiGraphics.fill(x, y + this.imageHeight - 1, x + this.imageWidth, y + this.imageHeight, 0xFF333333);
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(guiGraphics);
        int x = (this.width - this.imageWidth) / 2;
        int y = (this.height - this.imageHeight) / 2;
        this.renderBg(guiGraphics, partialTick, mouseX, mouseY);
        // 渲染标题
        guiGraphics.drawString(this.font, this.title.getString(), x + 8, y + 6, 0x404040);
        // 渲染按钮
        this.renderables.forEach(widget -> widget.render(guiGraphics, mouseX, mouseY, partialTick));
        // 显示当前频率
        String freqText = String.format("%.1f", currentFrequency) + " MHz";
        int freqTextWidth = this.font.width(freqText);
        guiGraphics.drawString(this.font, freqText, startX + buttonWidth * 2 + spacing * 2 - freqTextWidth / 2, buttonY + buttonHeight + 8, 0xFFFFFF);
        // 显示电量
        int power = 0;
        ItemStack battery = menu.getBattery();
        if (!battery.isEmpty() && battery.getItem() instanceof RadioBatteryItem) {
            power = RadioBatteryItem.getPower(battery);
        }
        String powerText = Component.translatable("item.broadcast_radio.walkie_talkie.power", power).getString();
        int powerTextWidth = this.font.width(powerText);
        guiGraphics.drawString(this.font, powerText, x + 10, buttonY + buttonHeight + 25, 0xFFFFFF);
        // 渲染电池槽标签
        guiGraphics.drawString(this.font, Component.translatable("item.broadcast_radio.walkie_talkie.battery_label"), x + 24 + powerTextWidth + 20, buttonY + buttonHeight + 25, 0x404040);
        // 渲染电池槽背景
        int batterySlotX = x + 157;
        guiGraphics.fill(batterySlotX, buttonY + buttonHeight + 20, batterySlotX + 16, buttonY + buttonHeight + 36, 0xFF8B8B8B);
        guiGraphics.fill(batterySlotX, buttonY + buttonHeight + 20, batterySlotX + 1, buttonY + buttonHeight + 36, 0xFF333333);
        guiGraphics.fill(batterySlotX, buttonY + buttonHeight + 20, batterySlotX + 16, buttonY + buttonHeight + 21, 0xFF333333);
        guiGraphics.fill(batterySlotX, buttonY + buttonHeight + 35, batterySlotX + 16, buttonY + buttonHeight + 36, 0xFF333333);
        guiGraphics.fill(batterySlotX + 15, buttonY + buttonHeight + 20, batterySlotX + 16, buttonY + buttonHeight + 36, 0xFF333333);
        // 渲染物品栏
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        // 渲染工具提示
        this.renderTooltip(guiGraphics, mouseX, mouseY);
    }

    @Override
    public void onClose() {
        bili.dongsz.broadcastradio.BroadcastRadio.NETWORK.sendToServer(new bili.dongsz.broadcastradio.network.UpdateWalkieTalkieFrequencyPacket(currentFrequency));
        super.onClose();
    }
}