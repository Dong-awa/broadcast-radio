package bili.dongsz.broadcastradio.screen;

import bili.dongsz.broadcastradio.utils.SignalSearchManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;

import java.util.ArrayList;
import java.util.List;

public class RadioTerminalPositionScreen extends Screen {
    private final Screen parentScreen;
    private final Player currentPlayer;

    private final int imageWidth = 256;
    private final int imageHeight = 240;

    public RadioTerminalPositionScreen(Screen parentScreen, Player currentPlayer) {
        super(Component.translatable("item.broadcast_radio.radio_terminal.position_title"));
        this.parentScreen = parentScreen;
        this.currentPlayer = currentPlayer;

        SignalSearchManager searchManager = SignalSearchManager.getInstance();
        if (!searchManager.isRunning()) {
            searchManager.startSignalSearch();
        }
        searchManager.forceSignalSearch();
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

        this.addRenderableWidget(Button.builder(
            Component.translatable("item.broadcast_radio.radio_terminal.return_button"),
            button -> {
                if (this.minecraft != null) {
                    this.minecraft.setScreen(this.parentScreen);
                }
            }
        ).bounds(x + 8, y + 130, 60, 20).build());

        this.addRenderableWidget(Button.builder(
            Component.translatable("item.broadcast_radio.radio_terminal.refresh_button"),
            button -> {
                SignalSearchManager sm = SignalSearchManager.getInstance();
                sm.forceSignalSearch();
            }
        ).bounds(x + this.imageWidth - 68, y + 130, 60, 20).build());
    }

    private Component getPlayerFacingComponent(Player player) {
        if (player == null) {
            return Component.translatable("item.broadcast_radio.radio_terminal.facing_unknown");
        }
        Direction facing = player.getDirection();
        String key;
        switch (facing) {
            case NORTH:
                key = "item.broadcast_radio.radio_terminal.facing_north";
                break;
            case SOUTH:
                key = "item.broadcast_radio.radio_terminal.facing_south";
                break;
            case EAST:
                key = "item.broadcast_radio.radio_terminal.facing_east";
                break;
            case WEST:
                key = "item.broadcast_radio.radio_terminal.facing_west";
                break;
            default:
                key = "item.broadcast_radio.radio_terminal.facing_unknown";
        }
        return Component.translatable(key);
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(guiGraphics);
        int x = (this.width - this.imageWidth) / 2;
        int y = (this.height - this.imageHeight) / 2;

        guiGraphics.fill(x, y, x + this.imageWidth, y + this.imageHeight, 0xFF8B8B8B);
        guiGraphics.fill(x, y, x + this.imageWidth, y + 1, 0xFF333333);
        guiGraphics.fill(x, y, x + 1, y + this.imageHeight, 0xFF333333);
        guiGraphics.fill(x + this.imageWidth - 1, y, x + this.imageWidth, y + this.imageHeight, 0xFF333333);
        guiGraphics.fill(x, y + this.imageHeight - 1, x + this.imageWidth, y + this.imageHeight, 0xFF333333);

        // ===== 顶部：当前基站坐标 =====
        SignalSearchManager searchManager = SignalSearchManager.getInstance();
        int bsX = searchManager.getCachedBaseStationX();
        int bsZ = searchManager.getCachedBaseStationZ();
        Component topLine;
        if (bsX == Integer.MIN_VALUE || bsZ == Integer.MIN_VALUE) {
            topLine = Component.translatable("item.broadcast_radio.radio_terminal.base_station_none");
        } else {
            topLine = Component.translatable("item.broadcast_radio.radio_terminal.base_station_pos", bsX, bsZ);
        }
        int topLineWidth = this.font.width(topLine);
        guiGraphics.drawString(this.font, topLine, x + (this.imageWidth - topLineWidth) / 2, y + 6, 0xFFFFFF);

        guiGraphics.fill(x + 8, y + 20, x + this.imageWidth - 8, y + 21, 0xFF333333);

        // ===== 中间：在线玩家列表（竖向）=====
        Component listTitle = Component.translatable("item.broadcast_radio.radio_terminal.player_list_title");
        guiGraphics.drawString(this.font, listTitle, x + 12, y + 28, 0xE0E0E0);

        int listTop = y + 42;
        int listBottom = y + 118;
        int listLeft = x + 12;
        int listRight = x + this.imageWidth - 12;

        guiGraphics.fill(listLeft, listTop, listRight, listBottom, 0xFF6E6E6E);
        guiGraphics.fill(listLeft, listTop, listRight, listTop + 1, 0xFF333333);
        guiGraphics.fill(listLeft, listBottom - 1, listRight, listBottom, 0xFF333333);
        guiGraphics.fill(listLeft, listTop, listLeft + 1, listBottom, 0xFF333333);
        guiGraphics.fill(listRight - 1, listTop, listRight, listBottom, 0xFF333333);

        List<Player> players = new ArrayList<>();
        if (searchManager.hasValidSignal()) {
            List<Player> cachedPlayers = searchManager.getCachedOnlinePlayers();
            if (cachedPlayers != null) {
                players.addAll(cachedPlayers);
            }
        }

        int rowY = listTop + 4;
        int rowHeight = 14;
        int maxRows = (listBottom - listTop - 8) / rowHeight;

        if (players.isEmpty()) {
            Component noPlayersText;
            if (!searchManager.hasValidSignal()) {
                noPlayersText = Component.translatable("item.broadcast_radio.radio_terminal.no_signal");
            } else {
                noPlayersText = Component.translatable("item.broadcast_radio.radio_terminal.no_players");
            }
            int textWidth = this.font.width(noPlayersText);
            guiGraphics.drawString(this.font, noPlayersText, x + (this.imageWidth - textWidth) / 2, listTop + 20, 0xCCCCCC);
        } else {
            for (int i = 0; i < Math.min(players.size(), maxRows); i++) {
                Player p = players.get(i);
                String playerName = p.getScoreboardName();

                int[] pos = searchManager.getPlayerBaseStationPos(p.getUUID());
                Component line;
                if (pos != null) {
                    line = Component.translatable("item.broadcast_radio.radio_terminal.player_entry", playerName, pos[0], pos[1]);
                } else {
                    line = Component.translatable("item.broadcast_radio.radio_terminal.player_entry_unknown", playerName);
                }
                guiGraphics.drawString(this.font, line, listLeft + 6, rowY, 0xFFFFFF);
                rowY += rowHeight;
            }
        }

        // 分割线（在按钮下方）
        guiGraphics.fill(x + 8, y + 158, x + this.imageWidth - 8, y + 159, 0xFF333333);

        super.render(guiGraphics, mouseX, mouseY, partialTick);

        // ===== 左下角：玩家名称 =====
        Component playerNameComponent;
        if (currentPlayer != null) {
            playerNameComponent = Component.translatable("item.broadcast_radio.radio_terminal.player_name", currentPlayer.getScoreboardName());
        } else {
            playerNameComponent = Component.translatable("item.broadcast_radio.radio_terminal.player_name", "");
        }
        guiGraphics.drawString(this.font, playerNameComponent, x + 8, y + this.imageHeight - 12, 0x404040);

        // ===== 右下角：玩家朝向 =====
        Component facingComponent = getPlayerFacingComponent(currentPlayer);
        int facingWidth = this.font.width(facingComponent);
        guiGraphics.drawString(this.font, facingComponent, x + this.imageWidth - facingWidth - 8, y + this.imageHeight - 12, 0x404040);
    }
}