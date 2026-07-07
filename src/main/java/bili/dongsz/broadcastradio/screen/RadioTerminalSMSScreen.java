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

    // 使用与位置信息界面一致的视觉尺寸
    private final int imageWidth = 256;
    private final int imageHeight = 240;

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

        int x = (this.width - this.imageWidth) / 2;
        int y = (this.height - this.imageHeight) / 2;

        SignalSearchManager searchManager = SignalSearchManager.getInstance();
        boolean isSenderValid = searchManager.hasValidSignal();

        // ===== 玩家列表区域（与位置信息界面保持一致） =====
        int listLeft = x + 12;
        int listTop = y + 42;
        int listRight = x + this.imageWidth - 12;
        int listBottom = y + 118;
        int listWidth = listRight - listLeft;
        int rowHeight = 14;
        int listAreaHeight = listBottom - listTop - 8;
        int maxRows = listAreaHeight / rowHeight;

        if (!isSenderValid) {
            // 发送端无效：不创建玩家按钮
        } else {
            onlinePlayers.clear();
            onlinePlayers.addAll(searchManager.getCachedOnlinePlayers());

            int buttonY = listTop + 4;
            int buttonsPerScreen = Math.min(onlinePlayers.size(), maxRows);
            for (int i = 0; i < buttonsPerScreen; i++) {
                Player player = onlinePlayers.get(i);
                final int playerIndex = i;

                // 玩家按钮：宽度与列表区域一致，高度 14px（与位置信息界面行高一致）
                this.addRenderableWidget(Button.builder(
                    Component.literal(player.getScoreboardName()),
                    button -> {
                        selectedPlayer = onlinePlayers.get(playerIndex);
                        openMessageInputScreen();
                    }
                ).bounds(listLeft, buttonY, listWidth, rowHeight).build());

                buttonY += rowHeight;
            }
        }

        // ===== 底部按钮区域（与位置信息界面保持一致） =====
        // 刷新按钮（右侧，与返回按钮对称）
        this.addRenderableWidget(Button.builder(
            Component.translatable("item.broadcast_radio.radio_terminal.refresh_button"),
            button -> refreshPlayerList()
        ).bounds(x + this.imageWidth - 68, y + 130, 60, 20).build());

        // 返回按钮（左侧）
        this.addRenderableWidget(Button.builder(
            Component.translatable("item.broadcast_radio.radio_terminal.return_button"),
            button -> Minecraft.getInstance().setScreen(parentScreen)
        ).bounds(x + 8, y + 130, 60, 20).build());
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

        int x = (this.width - this.imageWidth) / 2;
        int y = (this.height - this.imageHeight) / 2;

        // ===== 主面板背景（与位置信息界面一致） =====
        guiGraphics.fill(x, y, x + this.imageWidth, y + this.imageHeight, 0xFF8B8B8B);
        guiGraphics.fill(x, y, x + this.imageWidth, y + 1, 0xFF333333);
        guiGraphics.fill(x, y, x + 1, y + this.imageHeight, 0xFF333333);
        guiGraphics.fill(x + this.imageWidth - 1, y, x + this.imageWidth, y + this.imageHeight, 0xFF333333);
        guiGraphics.fill(x, y + this.imageHeight - 1, x + this.imageWidth, y + this.imageHeight, 0xFF333333);

        // ===== 顶部：标题（与位置信息界面顶部文字一致风格） =====
        Component titleLine = Component.translatable("item.broadcast_radio.radio_terminal.sms_title");
        int titleLineWidth = this.font.width(titleLine);
        guiGraphics.drawString(this.font, titleLine, x + (this.imageWidth - titleLineWidth) / 2, y + 6, 0xFFFFFF);

        // 水平分隔线（与位置信息界面一致）
        guiGraphics.fill(x + 8, y + 20, x + this.imageWidth - 8, y + 21, 0xFF333333);

        // ===== 列表标题（与位置信息界面一致） =====
        Component listTitle = Component.translatable("item.broadcast_radio.radio_terminal.sms_online_players");
        guiGraphics.drawString(this.font, listTitle, x + 12, y + 28, 0xE0E0E0);

        // ===== 列表区域背景（与位置信息界面一致） =====
        int listTop = y + 42;
        int listBottom = y + 118;
        int listLeft = x + 12;
        int listRight = x + this.imageWidth - 12;

        guiGraphics.fill(listLeft, listTop, listRight, listBottom, 0xFF6E6E6E);
        guiGraphics.fill(listLeft, listTop, listRight, listTop + 1, 0xFF333333);
        guiGraphics.fill(listLeft, listBottom - 1, listRight, listBottom, 0xFF333333);
        guiGraphics.fill(listLeft, listTop, listLeft + 1, listBottom, 0xFF333333);
        guiGraphics.fill(listRight - 1, listTop, listRight, listBottom, 0xFF333333);

        // ===== 信号状态显示（无信号时显示提示，与位置信息界面逻辑一致但不改变原有条件） =====
        SignalSearchManager searchManager = SignalSearchManager.getInstance();
        boolean isSenderValid = searchManager.hasValidSignal();

        if (!isSenderValid) {
            Component noSignalText = Component.translatable("item.broadcast_radio.radio_terminal.sms_no_players");
            int textWidth = this.font.width(noSignalText);
            guiGraphics.drawString(this.font, noSignalText, x + (this.imageWidth - textWidth) / 2, listTop + 20, 0xCCCCCC);
        } else if (onlinePlayers.isEmpty()) {
            Component noPlayersText = Component.translatable("item.broadcast_radio.radio_terminal.sms_no_players");
            int textWidth = this.font.width(noPlayersText);
            guiGraphics.drawString(this.font, noPlayersText, x + (this.imageWidth - textWidth) / 2, listTop + 20, 0xCCCCCC);
        }

        // 按钮下方的水平分隔线
        guiGraphics.fill(x + 8, y + 158, x + this.imageWidth - 8, y + 159, 0xFF333333);

        // ===== 绘制按钮（按钮在列表背景之上） =====
        super.render(guiGraphics, mouseX, mouseY, partialTick);

        // ===== 左下角 / 右下角信息文字（与位置信息界面底部文字一致风格） =====
        // 左下角：当前玩家
        String playerName = "";
        if (this.minecraft != null && this.minecraft.player != null) {
            playerName = this.minecraft.player.getScoreboardName();
        }
        String playerNameText = Component.translatable("item.broadcast_radio.radio_terminal.player_name", playerName).getString();
        guiGraphics.drawString(this.font, playerNameText, x + 8, y + this.imageHeight - 12, 0x404040);

        // 右下角：信号状态
        String signalStatus = isSenderValid
            ? Component.translatable("item.broadcast_radio.radio_terminal.base_station_pos",
                searchManager.getCachedBaseStationX(),
                searchManager.getCachedBaseStationZ()).getString()
            : Component.translatable("item.broadcast_radio.radio_terminal.base_station_none").getString();
        int signalWidth = this.font.width(signalStatus);
        guiGraphics.drawString(this.font, signalStatus, x + this.imageWidth - signalWidth - 8, y + this.imageHeight - 12, 0x404040);
    }

    // 消息输入屏幕
    private class MessageInputScreen extends Screen {
        private final Screen parentScreen;
        private final Player targetPlayer;
        private EditBox messageBox;

        // 消息输入界面使用较小的面板，但颜色风格与主面板一致
        private final int imageWidth = 256;
        private final int imageHeight = 160;

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

            int x = (this.width - this.imageWidth) / 2;
            int y = (this.height - this.imageHeight) / 2;

            // 消息输入框：与列表区域等宽（x + 12 到 x + imageWidth - 12）
            int boxLeft = x + 12;
            int boxWidth = this.imageWidth - 24;
            this.messageBox = new EditBox(this.font, boxLeft, y + 55, boxWidth, 20,
                Component.translatable("item.broadcast_radio.radio_terminal.sms_input_hint"));
            this.messageBox.setMaxLength(100);
            this.messageBox.setFocused(true);
            this.addRenderableWidget(this.messageBox);

            // 按钮：与主界面按钮风格一致，左右分布
            // 发送按钮（右侧）
            this.addRenderableWidget(Button.builder(
                Component.translatable("item.broadcast_radio.radio_terminal.send_button"),
                button -> sendMessage()
            ).bounds(x + this.imageWidth - 68, y + 90, 60, 20).build());

            // 取消按钮（左侧）
            this.addRenderableWidget(Button.builder(
                Component.translatable("item.broadcast_radio.radio_terminal.cancel_button"),
                button -> Minecraft.getInstance().setScreen(parentScreen)
            ).bounds(x + 8, y + 90, 60, 20).build());
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

            int x = (this.width - this.imageWidth) / 2;
            int y = (this.height - this.imageHeight) / 2;

            // ===== 主面板背景（与主界面风格一致） =====
            guiGraphics.fill(x, y, x + this.imageWidth, y + this.imageHeight, 0xFF8B8B8B);
            guiGraphics.fill(x, y, x + this.imageWidth, y + 1, 0xFF333333);
            guiGraphics.fill(x, y, x + 1, y + this.imageHeight, 0xFF333333);
            guiGraphics.fill(x + this.imageWidth - 1, y, x + this.imageWidth, y + this.imageHeight, 0xFF333333);
            guiGraphics.fill(x, y + this.imageHeight - 1, x + this.imageWidth, y + this.imageHeight, 0xFF333333);

            // ===== 顶部：标题（与主界面一致） =====
            Component titleLine = Component.translatable("item.broadcast_radio.radio_terminal.sms_input_hint");
            int titleLineWidth = this.font.width(titleLine);
            guiGraphics.drawString(this.font, titleLine, x + (this.imageWidth - titleLineWidth) / 2, y + 6, 0xFFFFFF);

            // 水平分隔线
            guiGraphics.fill(x + 8, y + 20, x + this.imageWidth - 8, y + 21, 0xFF333333);

            // ===== 目标玩家信息（与主界面风格一致） =====
            if (targetPlayer != null) {
                Component targetLine = Component.translatable("item.broadcast_radio.radio_terminal.player_name",
                    targetPlayer.getScoreboardName());
                int targetWidth = this.font.width(targetLine);
                guiGraphics.drawString(this.font, targetLine, x + (this.imageWidth - targetWidth) / 2, y + 30, 0xFFFFFF);
            }

            // ===== 输入框区域背景装饰（与列表区域风格类似） =====
            int boxLeft = x + 12;
            int boxRight = x + this.imageWidth - 12;
            int boxBgTop = y + 48;
            int boxBgBottom = y + 80;

            guiGraphics.fill(boxLeft, boxBgTop, boxRight, boxBgBottom, 0xFF6E6E6E);
            guiGraphics.fill(boxLeft, boxBgTop, boxRight, boxBgTop + 1, 0xFF333333);
            guiGraphics.fill(boxLeft, boxBgBottom - 1, boxRight, boxBgBottom, 0xFF333333);
            guiGraphics.fill(boxLeft, boxBgTop, boxLeft + 1, boxBgBottom, 0xFF333333);
            guiGraphics.fill(boxRight - 1, boxBgTop, boxRight, boxBgBottom, 0xFF333333);

            // 按钮下方的水平分隔线
            guiGraphics.fill(x + 8, y + 118, x + this.imageWidth - 8, y + 119, 0xFF333333);

            // 绘制按钮和输入框
            super.render(guiGraphics, mouseX, mouseY, partialTick);

            // ===== 底部信息文字（与主界面一致） =====
            String playerName = "";
            if (this.minecraft != null && this.minecraft.player != null) {
                playerName = this.minecraft.player.getScoreboardName();
            }
            String playerNameText = Component.translatable("item.broadcast_radio.radio_terminal.player_name", playerName).getString();
            guiGraphics.drawString(this.font, playerNameText, x + 8, y + this.imageHeight - 12, 0x404040);

            // 右下角：提示
            Component hintText = Component.translatable("item.broadcast_radio.radio_terminal.sms_sending_hint", "");
            String hintStr = "";
            int hintWidth = this.font.width(hintStr);
            guiGraphics.drawString(this.font, hintStr, x + this.imageWidth - hintWidth - 8, y + this.imageHeight - 12, 0x404040);
        }
    }
}