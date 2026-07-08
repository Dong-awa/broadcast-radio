package bili.dongsz.broadcastradio.screen;

import bili.dongsz.broadcastradio.item.RadioBatteryItem;
import bili.dongsz.broadcastradio.menu.EncryptedWalkieTalkieMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;

public class EncryptedWalkieTalkieScreen extends AbstractContainerScreen<EncryptedWalkieTalkieMenu> {
    private float currentFrequency;
    private EditBox passwordBox;
    private String currentPassword;
    private int startX;
    private int buttonY;
    private int buttonWidth;
    private int buttonHeight;
    private int spacing;

    public EncryptedWalkieTalkieScreen(EncryptedWalkieTalkieMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        this.imageWidth = 176;
        this.imageHeight = 222;
        this.currentFrequency = menu.getWalkieStack().getOrCreateTag().getFloat("Frequency");
        this.currentPassword = menu.getWalkieStack().getOrCreateTag().getString("Password");
        this.inventoryLabelX = 8;
        this.inventoryLabelY = this.imageHeight - 94;
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
        int x = (this.width - this.imageWidth) / 2;
        int y = (this.height - this.imageHeight) / 2;

        buttonWidth = 35;
        buttonHeight = 20;
        startX = x + 10;
        buttonY = y + 30;
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

        // 密码输入框
        this.passwordBox = new EditBox(this.font, x + 10, y + 110, 156, 20, Component.translatable("item.broadcast_radio.walkie_talkie.password"));
        this.passwordBox.setMaxLength(2);
        this.passwordBox.setValue(currentPassword);
        this.passwordBox.setResponder(text -> {
            currentPassword = text;
        });
        this.addRenderableWidget(this.passwordBox);
    }

    private void adjustFrequency(float delta) {
        currentFrequency += delta;
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
        
        // ===== 顶部：标题（水平居中白色） =====
        Component titleLine = this.title;
        int titleLineWidth = this.font.width(titleLine);
        guiGraphics.drawString(this.font, titleLine, x + (this.imageWidth - titleLineWidth) / 2, y + 6, 0xFFFFFF);

        // 水平分隔线
        guiGraphics.fill(x + 8, y + 20, x + this.imageWidth - 8, y + 21, 0xFF333333);
        
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
        guiGraphics.drawString(this.font, powerText, x + 10, buttonY + buttonHeight + 25, 0xFFFFFF);
        
        // 渲染电池槽标签（放在电池框左侧）
        guiGraphics.drawString(this.font, Component.translatable("item.broadcast_radio.walkie_talkie.battery_label"), x + 120, y + 61, 0xE0E0E0);

        // 渲染密码提示文本（在密码框上方）
        guiGraphics.drawString(this.font, Component.translatable("item.broadcast_radio.encrypted_walkie_talkie.encryption_key"), x + 10, y + 98, 0xE0E0E0);

        // ===== 渲染物品槽（Slots） =====
        net.minecraft.world.inventory.Slot hoveredSlot = null;
        for (net.minecraft.world.inventory.Slot slot : this.menu.slots) {
            int slotX = x + slot.x;
            int slotY = y + slot.y;

            boolean isHovered = mouseX >= slotX && mouseX < slotX + 16 && mouseY >= slotY && mouseY < slotY + 16;
            if (isHovered) {
                hoveredSlot = slot;
            }

            if (isHovered) {
                guiGraphics.fill(slotX - 1, slotY - 1, slotX + 17, slotY + 17, 0xFFFFFFFF);
                guiGraphics.fill(slotX, slotY, slotX + 16, slotY + 16, 0xFFCCCCCC);
            } else {
                guiGraphics.fill(slotX - 1, slotY - 1, slotX + 17, slotY + 17, 0xFF555555);
                guiGraphics.fill(slotX, slotY, slotX + 16, slotY + 16, 0xFF8B8B8B);
            }

            if (slot.hasItem()) {
                guiGraphics.renderItem(slot.getItem(), slotX, slotY);
                guiGraphics.renderItemDecorations(this.font, slot.getItem(), slotX, slotY);
            }
        }

        if (hoveredSlot != null && hoveredSlot.hasItem()) {
            guiGraphics.renderTooltip(this.font, hoveredSlot.getItem(), mouseX, mouseY);
        } else {
            this.renderTooltip(guiGraphics, mouseX, mouseY);
        }
    }

    @Override
    public void onClose() {
        bili.dongsz.broadcastradio.BroadcastRadio.NETWORK.sendToServer(new bili.dongsz.broadcastradio.network.UpdateEncryptedWalkieTalkiePacket(currentFrequency, currentPassword));
        super.onClose();
    }
}