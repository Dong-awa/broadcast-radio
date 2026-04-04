package bili.dongsz.broadcastradio.screen;

import bili.dongsz.broadcastradio.menu.EncryptedWalkieTalkieMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

public class EncryptedWalkieTalkieScreen extends AbstractContainerScreen<EncryptedWalkieTalkieMenu> {
    private float currentFrequency;
    private EditBox passwordBox;
    private String currentPassword;

    public EncryptedWalkieTalkieScreen(EncryptedWalkieTalkieMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        this.imageWidth = 176;
        this.imageHeight = 140;
        this.currentFrequency = menu.getWalkieStack().getOrCreateTag().getFloat("Frequency");
        this.currentPassword = menu.getWalkieStack().getOrCreateTag().getString("Password");
    }

    @Override
    protected void init() {
        super.init();
        int x = (this.width - this.imageWidth) / 2;
        int y = (this.height - this.imageHeight) / 2;

        // +5MHz 按钮
        this.addRenderableWidget(Button.builder(Component.literal("+5MHz"), button -> {
            adjustFrequency(5.0f);
        }).bounds(x + 10, y + 20, 60, 20).build());

        // -5MHz 按钮
        this.addRenderableWidget(Button.builder(Component.literal("-5MHz"), button -> {
            adjustFrequency(-5.0f);
        }).bounds(x + 10, y + 45, 60, 20).build());

        // +0.1MHz 按钮
        this.addRenderableWidget(Button.builder(Component.literal("+0.1MHz"), button -> {
            adjustFrequency(0.1f);
        }).bounds(x + 80, y + 20, 70, 20).build());

        // -0.1MHz 按钮
        this.addRenderableWidget(Button.builder(Component.literal("-0.1MHz"), button -> {
            adjustFrequency(-0.1f);
        }).bounds(x + 80, y + 45, 70, 20).build());

        // 密码输入框
        this.passwordBox = new EditBox(this.font, x + 10, y + 80, 156, 20, Component.literal("Password"));
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
        
        guiGraphics.drawString(this.font, this.title.getString(), x + 8, y + 6, 0x404040);
        
        guiGraphics.drawString(this.font, "Frequency: " + String.format("%.1f", currentFrequency) + " MHz", x + 10, y + 105, 0xFFFFFF);
        
        this.renderables.forEach(widget -> widget.render(guiGraphics, mouseX, mouseY, partialTick));
        
        this.renderTooltip(guiGraphics, mouseX, mouseY);
    }

    @Override
    public void onClose() {
        // 发送频率和密码到服务器
        bili.dongsz.broadcastradio.BroadcastRadio.NETWORK.sendToServer(new bili.dongsz.broadcastradio.network.UpdateEncryptedWalkieTalkiePacket(currentFrequency, currentPassword));
        super.onClose();
    }
}