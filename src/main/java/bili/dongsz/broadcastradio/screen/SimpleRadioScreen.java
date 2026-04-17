package bili.dongsz.broadcastradio.screen;

import bili.dongsz.broadcastradio.block.entity.SimpleRadioBlockEntity;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public class SimpleRadioScreen extends Screen {
    private SimpleRadioBlockEntity radioEntity;
    private int imageWidth = 176;
    private int imageHeight = 166;

    public SimpleRadioScreen(SimpleRadioBlockEntity radioEntity) {
        super(Component.translatable("item.broadcast_radio.simple_radio"));
        this.radioEntity = radioEntity;
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
        return false;
    }

    @Override
    protected void init() {
        super.init();
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
        
        // 渲染标题
        guiGraphics.drawString(this.font, this.title.getString(), x + 8, y + 6, 0x404040);

        if (radioEntity != null) {
            float currentFreq = radioEntity.getFrequency();
            guiGraphics.drawString(this.font, Component.translatable("item.broadcast_radio.simple_radio_block.current_frequency", String.format("%.1f", currentFreq)), x + 10, y + 20, 0xFFFFFF);
        } else {
            guiGraphics.drawString(this.font, Component.translatable("item.broadcast_radio.simple_radio_block.default_frequency"), x + 10, y + 20, 0xFFFFFF);
        }

        // 渲染按钮
        super.render(guiGraphics, mouseX, mouseY, partialTick);
    }
    
    @Override
    public void tick() {
        super.tick();
        if (radioEntity != null) {
            // 这里可以添加额外的更新逻辑，如果需要的话
        }
    }
}