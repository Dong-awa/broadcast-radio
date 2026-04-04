package bili.dongsz.broadcastradio.screen;

import bili.dongsz.broadcastradio.menu.SimpleRadioMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

public class SimpleRadioScreen extends AbstractContainerScreen<SimpleRadioMenu> {

    public SimpleRadioScreen(SimpleRadioMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        this.imageWidth = 176;
        this.imageHeight = 100;
    }

    @Override
    protected void init() {
        super.init();
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        // 使用原版熔炉等容器的灰色背景
        int x = (this.width - this.imageWidth) / 2;
        int y = (this.height - this.imageHeight) / 2;
        // 渲染灰色背景
        guiGraphics.fill(x, y, x + this.imageWidth, y + this.imageHeight, 0xFF8B8B8B);
        // 渲染边框
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

        if (menu.getRadioEntity() != null) {
            float currentFreq = menu.getRadioEntity().getFrequency();
            guiGraphics.drawString(this.font, "当前频率: " + String.format("%.1f", currentFreq) + " MHz", x + 10, y + 20, 0xFFFFFF);
        } else {
            guiGraphics.drawString(this.font, "当前频率: 88.5 MHz", x + 10, y + 20, 0xFFFFFF);
        }

        this.renderTooltip(guiGraphics, mouseX, mouseY);
    }
    
    @Override
    public void containerTick() {
        super.containerTick();
        if (menu.getRadioEntity() != null) {
            // 这里可以添加额外的更新逻辑，如果需要的话
        }
    }
}