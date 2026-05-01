package bili.dongsz.broadcastradio.screen;

import bili.dongsz.broadcastradio.BroadcastRadio;
import bili.dongsz.broadcastradio.network.SendSMSPacket;
import bili.dongsz.broadcastradio.utils.SignalSearchManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;

import java.util.ArrayList;
import java.util.List;

public class RadioTerminalSMSScreen extends Screen {
    private final Screen parentScreen;
    private final Player currentPlayer;
    private EditBox messageBox;
    private List<Player> onlinePlayers;
    private Player selectedPlayer;
    
    // 使用 SignalSearchManager 检查信号状态和获取缓存的玩家列表

    public RadioTerminalSMSScreen(Screen parentScreen, Player currentPlayer) {
        super(Component.translatable("item.broadcast_radio.radio_terminal.sms_title"));
        this.parentScreen = parentScreen;
        this.currentPlayer = currentPlayer;
        this.onlinePlayers = new ArrayList<>();
        
        // 确保后台扫描已启动
        bili.dongsz.broadcastradio.utils.SignalSearchManager searchManager = 
            bili.dongsz.broadcastradio.utils.SignalSearchManager.getInstance();
        if (!searchManager.isRunning()) {
            searchManager.startSignalSearch();
        }
        // 立即触发一次信号检测
        searchManager.forceSignalSearch();
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
        
        int x = (this.width - 200) / 2;
        int y = (this.height - 180) / 2;

        // 在线玩家标题
        this.addRenderableWidget(Button.builder(
            Component.translatable("item.broadcast_radio.radio_terminal.sms_online_players"), 
            button -> {}
        ).bounds(x, y, 200, 20).build()).active = false;


        SignalSearchManager searchManager = SignalSearchManager.getInstance();
        boolean isSenderValid = searchManager.hasValidSignal();
        
        if (!isSenderValid) {
            // 发送端无效，显示空列表提示
            this.addRenderableWidget(Button.builder(
                Component.translatable("item.broadcast_radio.radio_terminal.sms_no_players"),
                button -> {}
            ).bounds(x, y + 25, 200, 20).build()).active = false;
        } else {
            onlinePlayers.clear();
            onlinePlayers.addAll(searchManager.getCachedOnlinePlayers());

            int playerButtonY = y + 25;
            for (int i = 0; i < onlinePlayers.size(); i++) {
                Player player = onlinePlayers.get(i);
                final int playerIndex = i;
                
                this.addRenderableWidget(Button.builder(
                    Component.literal(player.getScoreboardName()),
                    button -> {
                        selectedPlayer = onlinePlayers.get(playerIndex);
                        openMessageInputScreen();
                    }
                ).bounds(x, playerButtonY, 200, 20).build());
                
                playerButtonY += 25;
            }
            if (onlinePlayers.isEmpty()) {
                this.addRenderableWidget(Button.builder(
                    Component.translatable("item.broadcast_radio.radio_terminal.sms_no_players"),
                    button -> {}
                ).bounds(x, y + 25, 200, 20).build()).active = false;
            }
        }

        // 刷新按钮
        this.addRenderableWidget(Button.builder(
            Component.translatable("item.broadcast_radio.radio_terminal.refresh_button"),
            button -> refreshPlayerList()
        ).bounds(x, y + 125, 200, 20).build());

        // 返回按钮
        this.addRenderableWidget(Button.builder(
            Component.translatable("item.broadcast_radio.radio_terminal.return_button"),
            button -> Minecraft.getInstance().setScreen(parentScreen)
        ).bounds(x, y + 150, 200, 20).build());
    }

    private void refreshPlayerList() {
        this.clearWidgets();
        onlinePlayers.clear();
        selectedPlayer = null;
        this.init();
    }
    
    
    
    private void openMessageInputScreen() {
        Minecraft.getInstance().setScreen(new MessageInputScreen(this, selectedPlayer));
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(guiGraphics);
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        
        // 绘制标题
        guiGraphics.drawCenteredString(this.font, this.title, this.width / 2, 20, 0xFFFFFF);
    }
    
    // 消息输入屏幕
    private class MessageInputScreen extends Screen {
        private final Screen parentScreen;
        private final Player targetPlayer;
        private EditBox messageBox;
        
        public MessageInputScreen(Screen parentScreen, Player targetPlayer) {
            super(Component.translatable("item.broadcast_radio.radio_terminal.sms_input_hint"));
            this.parentScreen = parentScreen;
            this.targetPlayer = targetPlayer;
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
            
            int x = (this.width - 200) / 2;
            int y = (this.height - 100) / 2;
            
            // 消息输入框
            this.messageBox = new EditBox(this.font, x, y, 200, 20, Component.translatable("item.broadcast_radio.radio_terminal.sms_input_hint"));
            this.messageBox.setMaxLength(100);
            this.messageBox.setFocused(true);
            this.addRenderableWidget(this.messageBox);
            
            // 发送按钮
            this.addRenderableWidget(Button.builder(
                Component.translatable("item.broadcast_radio.radio_terminal.send_button"),
                button -> sendMessage()
            ).bounds(x, y + 30, 200, 20).build());
            
            // 取消按钮
            this.addRenderableWidget(Button.builder(
                Component.translatable("item.broadcast_radio.radio_terminal.cancel_button"),
                button -> Minecraft.getInstance().setScreen(parentScreen)
            ).bounds(x, y + 60, 200, 20).build());
        }
        
        private void sendMessage() {
            String message = messageBox.getValue().trim();
            if (!message.isEmpty() && targetPlayer != null) {
                // 客户端本地检查是否有终端
                boolean hasTerminal = false;
                for (int i = 0; i < currentPlayer.getInventory().getContainerSize(); i++) {
                    if (currentPlayer.getInventory().getItem(i).getItem() == bili.dongsz.broadcastradio.registry.ModItems.RADIO_TERMINAL.get()) {
                        hasTerminal = true;
                        break;
                    }
                }
                
                if (!hasTerminal) {
                    currentPlayer.sendSystemMessage(
                        net.minecraft.network.chat.Component.translatable("item.broadcast_radio.radio_terminal.no_terminal")
                    );
                    return;
                }
                
                // "正在发送..."提示
                currentPlayer.sendSystemMessage(
                    net.minecraft.network.chat.Component.translatable("item.broadcast_radio.radio_terminal.sms_sending")
                );
                
                // 关闭屏幕
                Minecraft.getInstance().setScreen(null);
                // 获取当前网络频段延迟
                int delay = bili.dongsz.broadcastradio.utils.SMSDelayUtils.getPlayerCurrentNetworkDelay();
                // 启动客户端定时器，延迟发送
                new Thread(() -> {
                    try {
                        Thread.sleep(delay);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        return;
                    }
                    
                    // 定时器结束后，在主线程发送数据包
                    net.minecraft.client.Minecraft.getInstance().execute(() -> {
                        // 检查通过，发送数据包给服务器
                        BroadcastRadio.NETWORK.sendToServer(new bili.dongsz.broadcastradio.network.SendSMSPacket(
                            targetPlayer.getUUID(),
                            message,
                            currentPlayer.getUUID()
                        ));
                    });
                }).start();
            }
        }
        
        @Override
        public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
            this.renderBackground(guiGraphics);
            super.render(guiGraphics, mouseX, mouseY, partialTick);
            
            // 绘制标题
            guiGraphics.drawCenteredString(this.font, this.title, this.width / 2, 20, 0xFFFFFF);
            
            // 显示目标玩家
            if (targetPlayer != null) {
                guiGraphics.drawCenteredString(this.font, 
                    Component.literal("发送给: " + targetPlayer.getScoreboardName()),
                    this.width / 2, 40, 0xFFFFFF);
            }
        }
    }
}