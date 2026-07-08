package bili.dongsz.broadcastradio.screen;

import bili.dongsz.broadcastradio.BroadcastRadio;
import bili.dongsz.broadcastradio.block.entity.SimpleSignalJammerBlockEntity;
import bili.dongsz.broadcastradio.menu.SimpleSignalJammerMenu;
import bili.dongsz.broadcastradio.network.UpdateSignalJammerPacket;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;

public class SimpleSignalJammerScreen extends AbstractContainerScreen<SimpleSignalJammerMenu> {
    private SimpleSignalJammerBlockEntity jammerEntity;

    public SimpleSignalJammerScreen(SimpleSignalJammerMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        this.jammerEntity = menu.getJammerEntity();
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

        int buttonWidth = 30;
        int buttonHeight = 16;
        int spacing = 2;
        int buttonY = y + 44;

        this.addRenderableWidget(Button.builder(Component.literal("-5"), button -> {
            sendUpdatePacket(-5.0f);
        }).bounds(x + 10, buttonY, buttonWidth, buttonHeight).build());

        this.addRenderableWidget(Button.builder(Component.literal("+5"), button -> {
            sendUpdatePacket(5.0f);
        }).bounds(x + 10 + buttonWidth + spacing, buttonY, buttonWidth, buttonHeight).build());

        this.addRenderableWidget(Button.builder(Component.literal("-0.5"), button -> {
            sendUpdatePacket(-0.5f);
        }).bounds(x + 10 + buttonWidth * 2 + spacing * 2, buttonY, buttonWidth, buttonHeight).build());

        this.addRenderableWidget(Button.builder(Component.literal("+0.5"), button -> {
            sendUpdatePacket(0.5f);
        }).bounds(x + 10 + buttonWidth * 3 + spacing * 3, buttonY, buttonWidth, buttonHeight).build());
    }

    private void sendUpdatePacket(float delta) {
        if (jammerEntity != null) {
            float currentFreq = this.menu.getFrequencyFromData();
            float newFreq = currentFreq + delta;
            BroadcastRadio.NETWORK.sendToServer(new UpdateSignalJammerPacket(
                    jammerEntity.getBlockPos(),
                    newFreq
            ));
        }
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

        float frequency = this.menu.getFrequencyFromData();
        guiGraphics.drawString(this.font, Component.translatable("block.broadcast_radio.simple_signal_jammer.frequency", String.format("%.1f MHz", frequency)), x + 10, y + 30, 0xE0E0E0);

        boolean working = this.menu.isWorking();
        int statusColor = working ? 0x00FF00 : 0xFF5555;
        String statusKey = working ? "active" : "inactive";
        guiGraphics.drawString(this.font, Component.translatable("block.broadcast_radio.simple_signal_jammer.status",
                Component.translatable("block.broadcast_radio.simple_signal_jammer.status." + statusKey)), x + 10, y + 70, statusColor);

        int feBarX = x + 10;
        int feBarY = y + 100;
        int feBarWidth = 80;
        int feBarHeight = 10;
        int feEnergy = this.menu.getFeEnergy();
        int feMax = SimpleSignalJammerBlockEntity.MAX_FE_STORAGE;
        int feFill = feMax > 0 ? (feEnergy * feBarWidth) / feMax : 0;
        guiGraphics.drawString(this.font, Component.translatable("block.broadcast_radio.simple_signal_jammer.fe_energy"), x + 10, y + 90, 0xFFFFFF);
        guiGraphics.fill(feBarX - 1, feBarY - 1, feBarX + feBarWidth + 1, feBarY + feBarHeight + 1, 0xFF333333);
        guiGraphics.fill(feBarX, feBarY, feBarX + feBarWidth, feBarY + feBarHeight, 0xFF555555);
        guiGraphics.fill(feBarX, feBarY, feBarX + feFill, feBarY + feBarHeight, 0xFF00BFFF);
        guiGraphics.drawString(this.font, feEnergy + " / " + feMax, feBarX + feBarWidth + 4, feBarY + 1, 0xE0E0E0);

        int batBarY = feBarY + 22;
        int batBarWidth = 80;
        int batBarHeight = 10;
        int batEnergy = this.menu.getBatteryEnergyFromData();
        int batMax = Math.max(100, batEnergy);
        int batFill = batMax > 0 ? (batEnergy * batBarWidth) / batMax : 0;
        guiGraphics.drawString(this.font, Component.translatable("block.broadcast_radio.simple_signal_jammer.battery_energy"), x + 10, batBarY - 10, 0xFFFFFF);
        guiGraphics.fill(x + 9, batBarY - 1, x + batBarWidth + 11, batBarY + batBarHeight + 1, 0xFF333333);
        guiGraphics.fill(x + 10, batBarY, x + batBarWidth + 10, batBarY + batBarHeight, 0xFF555555);
        guiGraphics.fill(x + 10, batBarY, x + batFill + 10, batBarY + batBarHeight, 0xFF228B22);
        guiGraphics.drawString(this.font, batEnergy + " / " + batMax, x + batBarWidth + 14, batBarY + 1, 0xE0E0E0);

        SimpleSignalJammerBlockEntity.EnergySource source = this.menu.getEnergySource();
        guiGraphics.drawString(this.font, Component.translatable("block.broadcast_radio.simple_signal_jammer.current_source",
                Component.translatable("block.broadcast_radio.simple_signal_jammer.source." + source.getName())), x + 10, batBarY + batBarHeight + 8, 0xFFE0E0E0);

        guiGraphics.drawString(this.font, Component.translatable("block.broadcast_radio.simple_signal_jammer.effective_radius", SimpleSignalJammerBlockEntity.EFFECTIVE_RADIUS), x + 10, batBarY + batBarHeight + 28, 0xE0E0E0);
        guiGraphics.drawString(this.font, Component.translatable("block.broadcast_radio.simple_signal_jammer.limit_radius", SimpleSignalJammerBlockEntity.LIMIT_RADIUS), x + 10, batBarY + batBarHeight + 42, 0xE0E0E0);

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
    public boolean isPauseScreen() {
        return false;
    }
}