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
import net.minecraft.world.item.ItemStack;

public class RadioBaseStationScreen extends AbstractContainerScreen<RadioBaseStationMenu> {
    private EditBox serviceNameBox;
    private String currentServiceName;
    private RadioBaseStationBlockEntity stationEntity;

    public RadioBaseStationScreen(RadioBaseStationMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        this.stationEntity = menu.getStationEntity();
        this.currentServiceName = stationEntity.getServiceName();
        this.imageWidth = 176;
        this.imageHeight = 280;
        this.inventoryLabelX = 8;
        this.inventoryLabelY = this.imageHeight - 94;
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
        int buttonY = y + 70;

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

        buttonY = y + 108;

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

        Component titleLine = this.title;
        int titleLineWidth = this.font.width(titleLine);
        guiGraphics.drawString(this.font, titleLine, x + (this.imageWidth - titleLineWidth) / 2, y + 6, 0xFFFFFF);

        guiGraphics.fill(x + 8, y + 20, x + this.imageWidth - 8, y + 21, 0xFF333333);

        if (stationEntity != null) {
            guiGraphics.drawString(this.font, Component.translatable("item.broadcast_radio.radio_terminal.network_type", stationEntity.getNetworkType().getDisplayName()), x + 10, y + 58, 0xE0E0E0);
            guiGraphics.drawString(this.font, Component.translatable("item.broadcast_radio.radio_base_station.signal_range", stationEntity.getSignalRange()), x + 10, y + 94, 0xE0E0E0);

            // FE能量标签 和 消耗文字 等高（y+130）
            guiGraphics.drawString(this.font, Component.translatable("item.broadcast_radio.radio_base_station.fe_energy"), x + 10, y + 130, 0xFFFFFF);
            guiGraphics.drawString(this.font, Component.translatable("item.broadcast_radio.radio_base_station.consumption", String.format("%.1f", stationEntity.getEnergyConsumptionRate())), x + 100, y + 130, 0xFFFFFF);

            // FE能量条
            int feBarX = x + 10;
            int feBarY = y + 140;
            int feBarWidth = 80;
            int feBarHeight = 10;
            int feEnergy = stationEntity.getFeEnergy();
            int feMax = stationEntity.getFeMaxStorage();
            int feFill = feMax > 0 ? (feEnergy * feBarWidth) / feMax : 0;
            guiGraphics.fill(feBarX - 1, feBarY - 1, feBarX + feBarWidth + 1, feBarY + feBarHeight + 1, 0xFF333333);
            guiGraphics.fill(feBarX, feBarY, feBarX + feBarWidth, feBarY + feBarHeight, 0xFF555555);
            guiGraphics.fill(feBarX, feBarY, feBarX + feFill, feBarY + feBarHeight, 0xFF00BFFF);
            guiGraphics.drawString(this.font, feEnergy + " / " + feMax, feBarX + feBarWidth + 4, feBarY + 1, 0xE0E0E0);

            // 电池能量条：在FE条下方
            int batBarY = feBarY + 22;
            int batBarWidth = 80;
            int batBarHeight = 10;
            int batEnergy = stationEntity.getBatteryEnergy();
            int batMax = stationEntity.getMaxBatteryEnergy();
            int batFill = batMax > 0 ? (batEnergy * batBarWidth) / batMax : 0;
            guiGraphics.drawString(this.font, Component.translatable("item.broadcast_radio.radio_base_station.battery_energy"), x + 10, batBarY - 10, 0xFFFFFF);
            guiGraphics.fill(x + 9, batBarY - 1, x + batBarWidth + 11, batBarY + batBarHeight + 1, 0xFF333333);
            guiGraphics.fill(x + 10, batBarY, x + batBarWidth + 10, batBarY + batBarHeight, 0xFF555555);
            guiGraphics.fill(x + 10, batBarY, x + batFill + 10, batBarY + batBarHeight, 0xFF228B22);
            guiGraphics.drawString(this.font, batEnergy + " / " + batMax, x + batBarWidth + 14, batBarY + 1, 0xE0E0E0);

            // 当前电源
            RadioBaseStationBlockEntity.EnergySource source = stationEntity.getCurrentEnergySource();
            guiGraphics.drawString(this.font, Component.translatable("item.broadcast_radio.radio_base_station.current_source", source.getName()), x + 10, batBarY + batBarHeight + 8, 0xFFE0E0E0);
        }

        this.renderables.forEach(widget -> widget.render(guiGraphics, mouseX, mouseY, partialTick));

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

        // 渲染跟随鼠标的拿起物品
        ItemStack carriedItem = this.menu.getCarried();
        if (!carriedItem.isEmpty()) {
            guiGraphics.renderItem(carriedItem, mouseX - 8, mouseY - 8);
            guiGraphics.renderItemDecorations(this.font, carriedItem, mouseX - 8, mouseY - 8);
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