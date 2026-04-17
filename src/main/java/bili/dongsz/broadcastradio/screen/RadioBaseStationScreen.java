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
        this.imageHeight = 210; // 总高度保持210不变
        
        // 修正物品栏文字位置
        this.inventoryLabelX = 8; // 左边距，默认值
        this.inventoryLabelY = 120; // 物品栏文字Y坐标设置为120
    }

    @Override
    protected void init() {
        super.init();
        int x = (this.width - this.imageWidth) / 2;
        int y = (this.height - this.imageHeight) / 2;

        serviceNameBox = new EditBox(this.font, x + 10, y + 30, 156, 20, Component.translatable("item.broadcast_radio.radio_terminal.service_name_hint"));
        serviceNameBox.setMaxLength(50);
        serviceNameBox.setValue(currentServiceName);
        serviceNameBox.setResponder(text -> {
            currentServiceName = text;
            sendUpdatePacket();
        });
        this.addRenderableWidget(serviceNameBox);

        int buttonWidth = 49;
        int buttonHeight = 20;
        int buttonY = y + 60;
        int spacing = 4;

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

        buttonY = y + 90;

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

        if (stationEntity != null) {
                guiGraphics.drawString(this.font, Component.translatable("item.broadcast_radio.radio_terminal.network_type", stationEntity.getNetworkType().getDisplayName()), x + 10, y + 51, 0x404040);
                guiGraphics.drawString(this.font, Component.translatable("item.broadcast_radio.radio_base_station.signal_range", stationEntity.getSignalRange()), x + 10, y + 80, 0x404040);
                guiGraphics.drawString(this.font, Component.translatable("item.broadcast_radio.radio_terminal.energy", stationEntity.getTotalEnergy()), x + 10, y + 112, 0xFFFFFF);
                guiGraphics.drawString(this.font, Component.translatable("item.broadcast_radio.radio_base_station.consumption", String.format("%.1f", stationEntity.getEnergyConsumptionRate())), x + 90, y + 112, 0xFFFFFF);
                guiGraphics.drawString(this.font, Component.translatable("item.broadcast_radio.radio_terminal.service_name"), x + 10, y + 20, 0x404040);
            }

        // 渲染按钮
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        
        // 界面标题
        guiGraphics.drawString(this.font, this.title, x + 8, y + 6, 0x404040);
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
    public boolean charTyped(char codePoint, int modifiers) {
        if (serviceNameBox.isFocused()) {
            return serviceNameBox.charTyped(codePoint, modifiers);
        }
        return super.charTyped(codePoint, modifiers);
    }
}