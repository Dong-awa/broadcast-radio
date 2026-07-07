package bili.dongsz.broadcastradio.screen;

import bili.dongsz.broadcastradio.menu.RadioTerminalMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

public class RadioTerminalScreen extends AbstractContainerScreen<RadioTerminalMenu> {

    public RadioTerminalScreen(RadioTerminalMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        this.imageWidth = 176;
        this.imageHeight = 166;
        this.inventoryLabelX = 8;
        this.inventoryLabelY = 73;
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

        // 标签文字放在槽位右侧（避免与槽位重叠）
        guiGraphics.drawString(this.font, Component.translatable("item.broadcast_radio.radio_terminal.battery"), x + 28, y + 36, 0xE0E0E0);
        guiGraphics.drawString(this.font, Component.translatable("item.broadcast_radio.radio_terminal.universal_card"), x + 64, y + 36, 0xE0E0E0);

        // 渲染 widgets
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
}