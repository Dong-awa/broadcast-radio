package bili.dongsz.broadcastradio.screen;

import bili.dongsz.broadcastradio.BroadcastRadio;
import bili.dongsz.broadcastradio.block.entity.RadioBaseStationBlockEntity;
import bili.dongsz.broadcastradio.menu.RadioBaseStationMenu;
import bili.dongsz.broadcastradio.network.UpdateRadioBaseStationPacket;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

public class RadioBaseStationScreen extends AbstractContainerScreen<RadioBaseStationMenu> {
    private EditBox serviceNameBox;
    private String currentServiceName;
    private RadioBaseStationBlockEntity stationEntity;

    public RadioBaseStationScreen(RadioBaseStationMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        this.stationEntity = menu.getStationEntity();
        this.currentServiceName = stationEntity.getServiceName();
        this.imageWidth = 176;
        this.imageHeight = 210;
        this.inventoryLabelX = 8;
        this.inventoryLabelY = this.imageHeight - 88;
    }

    @Override
    protected void init() {
        super.init();
        int x = (this.width - this.imageWidth) / 2;
        int y = (this.height - this.imageHeight) / 2;

        serviceNameBox = new EditBox(this.font, x + 10, y + 32, 156, 18, Component.translatable("item.broadcast_radio.radio_terminal.service_name_hint"));
        serviceNameBox.setMaxLength(50);
        serviceNameBox.setValue(currentServiceName);
        serviceNameBox.setResponder(text -> {
            currentServiceName = text;
            sendUpdatePacket();
        });
        this.addRenderableWidget(serviceNameBox);

        int buttonWidth = 49;
        int buttonHeight = 16;
        int spacing = 4;
        int buttonY = y + 56;

        this.addRenderableWidget(Button.builder(Component.translatable("item.broadcast_radio.radio_terminal.2g_button"), button -> {
            stationEntity.setNetworkType(RadioBaseStationBlockEntity.NetworkType.TWO_G);
            sendUpdatePacket();
        }).bounds(x + 10, buttonY, buttonWidth, buttonHeight).build());

        this.addRenderableWidget(Button.builder(Component.translatable("item.broadcast_radio.radio_terminal.3g_button"), button -> {
            stationEntity.setNetworkType(RadioBaseStationBlockEntity.NetworkType.THREE_G);
            sendUpdatePacket();
        }).bounds(x + 10 + buttonWidth + spacing, buttonY, buttonWidth, buttonHeight).build());

        this.addRenderableWidget(Button.builder(Component.translatable("item.broadcast_radio.radio_terminal.4g_button"), button -> {
            stationEntity.setNetworkType(RadioBaseStationBlockEntity.NetworkType.FOUR_G);
            sendUpdatePacket();
        }).bounds(x + 10 + buttonWidth * 2 + spacing * 2, buttonY, buttonWidth, buttonHeight).build());

        buttonY = y + 88;

        this.addRenderableWidget(Button.builder(Component.translatable("item.broadcast_radio.radio_terminal.decrease_10"), button -> {
            stationEntity.setSignalRange(Math.max(10, stationEntity.getSignalRange() - 10));
            sendUpdatePacket();
        }).bounds(x + 10, buttonY, buttonWidth, buttonHeight).build());

        this.addRenderableWidget(Button.builder(Component.translatable("item.broadcast_radio.radio_terminal.increase_10"), button -> {
            stationEntity.setSignalRange(Math.min(1000, stationEntity.getSignalRange() + 10));
            sendUpdatePacket();
        }).bounds(x + 10 + buttonWidth + spacing, buttonY, buttonWidth, buttonHeight).build());
    }

    private void sendUpdatePacket() {
        if (stationEntity != null) {
            BroadcastRadio.NETWORK.sendToServer(new UpdateRadioBaseStationPacket(
                stationEntity.getBlockPos(),
                stationEntity.getSignalRange(),
                stationEntity.getNetworkType().ordinal(),
                currentServiceName
            ));
        }
    }

    @Override
    public void onClose() {
        if (stationEntity != null) {
            stationEntity.setServiceName(currentServiceName);
        }
        super.onClose();
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        int x = (this.width - this.imageWidth) / 2;
        int y = (this.height - this.imageHeight) / 2;
        
        // 绘制GUI背景
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

        if (stationEntity != null) {
            // 网络类型（在输入框上方）
            guiGraphics.drawString(this.font, Component.translatable("item.broadcast_radio.radio_terminal.network_type", stationEntity.getNetworkType().getDisplayName()), x + 10, y + 26, 0xE0E0E0);
            // 信号范围（在两行按钮之间）
            guiGraphics.drawString(this.font, Component.translatable("item.broadcast_radio.radio_base_station.signal_range", stationEntity.getSignalRange()), x + 10, y + 78, 0xE0E0E0);
            // 能量和消耗（在第二行按钮下方）
            guiGraphics.drawString(this.font, Component.translatable("item.broadcast_radio.radio_terminal.energy", stationEntity.getTotalEnergy()), x + 10, y + 110, 0xFFFFFF);
            guiGraphics.drawString(this.font, Component.translatable("item.broadcast_radio.radio_base_station.consumption", String.format("%.1f", stationEntity.getEnergyConsumptionRate())), x + 90, y + 110, 0xFFFFFF);
        }

        // 手动渲染 widgets
        this.renderables.forEach(widget -> widget.render(guiGraphics, mouseX, mouseY, partialTick));

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
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (serviceNameBox.isFocused()) {
            if (keyCode == 256) {
                serviceNameBox.setFocused(false);
                return super.keyPressed(keyCode, scanCode, modifiers);
            }
            serviceNameBox.keyPressed(keyCode, scanCode, modifiers);
            return true;
        }

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
    public boolean charTyped(char codePoint, int modifiers) {
        if (serviceNameBox.isFocused()) {
            return serviceNameBox.charTyped(codePoint, modifiers);
        }
        return super.charTyped(codePoint, modifiers);
    }
}