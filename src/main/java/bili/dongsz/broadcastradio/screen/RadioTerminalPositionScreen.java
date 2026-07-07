package bili.dongsz.broadcastradio.screen;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;

import java.util.ArrayList;
import java.util.List;

public class RadioTerminalPositionScreen extends Screen {
    private final Screen parentScreen;

    private final int imageWidth = 256;
    private final int imageHeight = 240;

    // 所有显示所需的数据在构造时一次性保存，打开界面后不再动态获取
    private final String savedPlayerName;
    private final String savedFacingText;
    private final String savedBaseStationText;
    private final List<String> savedPlayerList;

    public RadioTerminalPositionScreen(Screen parentScreen) {
        super(Component.translatable("item.broadcast_radio.radio_terminal.position_title"));
        this.parentScreen = parentScreen;

        // ====== 在构造时一次性保存所有需要显示的数据 ======

        // 1. 玩家名
        String name = "?";
        try {
            Player p = Minecraft.getInstance().player;
            if (p != null) {
                String scoreboardName = p.getScoreboardName();
                if (scoreboardName != null && !scoreboardName.trim().isEmpty()) {
                    name = scoreboardName;
                } else if (p.getGameProfile() != null) {
                    String profileName = p.getGameProfile().getName();
                    if (profileName != null && !profileName.trim().isEmpty()) {
                        name = profileName;
                    }
                }
            }
            if (name.equals("?") && Minecraft.getInstance().getUser() != null) {
                name = Minecraft.getInstance().getUser().getName();
            }
        } catch (Exception e) {
            // ignore
        }
        this.savedPlayerName = name;

        // 2. 玩家朝向
        String facingText = Component.translatable("item.broadcast_radio.radio_terminal.facing_unknown").getString();
        try {
            Player p = Minecraft.getInstance().player;
            if (p != null) {
                switch (p.getDirection()) {
                    case NORTH:
                        facingText = Component.translatable("item.broadcast_radio.radio_terminal.facing_north").getString();
                        break;
                    case SOUTH:
                        facingText = Component.translatable("item.broadcast_radio.radio_terminal.facing_south").getString();
                        break;
                    case EAST:
                        facingText = Component.translatable("item.broadcast_radio.radio_terminal.facing_east").getString();
                        break;
                    case WEST:
                        facingText = Component.translatable("item.broadcast_radio.radio_terminal.facing_west").getString();
                        break;
                    default:
                        break;
                }
            }
        } catch (Exception e) {
            // ignore
        }
        this.savedFacingText = facingText;

        // 3. 基站坐标
        String baseText = Component.translatable("item.broadcast_radio.radio_terminal.base_station_none").getString();
        try {
            bili.dongsz.broadcastradio.utils.SignalSearchManager sm = bili.dongsz.broadcastradio.utils.SignalSearchManager.getInstance();
            int bsX = sm.getCachedBaseStationX();
            int bsZ = sm.getCachedBaseStationZ();
            if (bsX != Integer.MIN_VALUE && bsZ != Integer.MIN_VALUE) {
                baseText = Component.translatable("item.broadcast_radio.radio_terminal.base_station_pos", bsX, bsZ).getString();
            }
        } catch (Exception e) {
            // ignore
        }
        this.savedBaseStationText = baseText;

        // 4. 在线玩家列表 — 直接从 Minecraft 读取当前世界玩家，不依赖后台任务的缓存
        List<String> playerEntries = new ArrayList<>();
        try {
            bili.dongsz.broadcastradio.utils.SignalSearchManager sm = bili.dongsz.broadcastradio.utils.SignalSearchManager.getInstance();
            Player self = Minecraft.getInstance().player;
            if (self != null && Minecraft.getInstance().level != null) {
                for (Player player : Minecraft.getInstance().level.players()) {
                    if (player.getUUID().equals(self.getUUID())) {
                        continue; // 跳过自己
                    }
                    try {
                        String pName = player.getScoreboardName();
                        if (pName == null || pName.trim().isEmpty()) {
                            pName = player.getName().getString();
                        }
                        if (pName == null || pName.trim().isEmpty()) {
                            continue;
                        }
                        int[] pos = sm.getPlayerBaseStationPos(player.getUUID());
                        if (pos != null) {
                            playerEntries.add(Component.translatable("item.broadcast_radio.radio_terminal.player_entry", pName, pos[0], pos[1]).getString());
                        } else {
                            playerEntries.add(Component.translatable("item.broadcast_radio.radio_terminal.player_entry_unknown", pName).getString());
                        }
                    } catch (Exception e) {
                        // skip this player
                    }
                }
            }
        } catch (Exception e) {
            // ignore
        }
        this.savedPlayerList = playerEntries;

        // 不启动任何后台搜索任务。已有的缓存直接使用，不再更新。
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

        // 只保留返回按钮，移除刷新按钮（刷新会触发后台逻辑，可能造成问题）
        this.addRenderableWidget(Button.builder(
            Component.translatable("item.broadcast_radio.radio_terminal.return_button"),
            button -> {
                if (this.minecraft != null) {
                    this.minecraft.setScreen(this.parentScreen);
                }
            }
        ).bounds(x + 8, y + 130, 60, 20).build());
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(guiGraphics);
        int x = (this.width - this.imageWidth) / 2;
        int y = (this.height - this.imageHeight) / 2;

        // 背景
        guiGraphics.fill(x, y, x + this.imageWidth, y + this.imageHeight, 0xFF8B8B8B);
        guiGraphics.fill(x, y, x + this.imageWidth, y + 1, 0xFF333333);
        guiGraphics.fill(x, y, x + 1, y + this.imageHeight, 0xFF333333);
        guiGraphics.fill(x + this.imageWidth - 1, y, x + this.imageWidth, y + this.imageHeight, 0xFF333333);
        guiGraphics.fill(x, y + this.imageHeight - 1, x + this.imageWidth, y + this.imageHeight, 0xFF333333);

        // ===== 顶部：基站坐标（使用构造时保存的值） =====
        int topLineWidth = this.font.width(savedBaseStationText);
        guiGraphics.drawString(this.font, savedBaseStationText, x + (this.imageWidth - topLineWidth) / 2, y + 6, 0xFFFFFF);

        // 水平分隔线
        guiGraphics.fill(x + 8, y + 20, x + this.imageWidth - 8, y + 21, 0xFF333333);

        // ===== 中间：在线玩家列表（使用构造时保存的值） =====
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

        int rowY = listTop + 4;
        int rowHeight = 14;
        int maxRows = (listBottom - listTop - 8) / rowHeight;

        if (savedPlayerList.isEmpty()) {
            Component noPlayersText = Component.translatable("item.broadcast_radio.radio_terminal.no_players");
            int textWidth = this.font.width(noPlayersText);
            guiGraphics.drawString(this.font, noPlayersText, x + (this.imageWidth - textWidth) / 2, listTop + 20, 0xCCCCCC);
        } else {
            for (int i = 0; i < Math.min(savedPlayerList.size(), maxRows); i++) {
                guiGraphics.drawString(this.font, savedPlayerList.get(i), listLeft + 6, rowY, 0xFFFFFF);
                rowY += rowHeight;
            }
        }

        // 按钮下方的水平分隔线
        guiGraphics.fill(x + 8, y + 158, x + this.imageWidth - 8, y + 159, 0xFF333333);

        super.render(guiGraphics, mouseX, mouseY, partialTick);

        // ===== 左下角：玩家名称（使用构造时保存的值，永不改变） =====
        String playerNameText = Component.translatable("item.broadcast_radio.radio_terminal.player_name", savedPlayerName).getString();
        guiGraphics.drawString(this.font, playerNameText, x + 8, y + this.imageHeight - 12, 0x404040);

        // ===== 右下角：玩家朝向（使用构造时保存的值，永不改变） =====
        int facingWidth = this.font.width(savedFacingText);
        guiGraphics.drawString(this.font, savedFacingText, x + this.imageWidth - facingWidth - 8, y + this.imageHeight - 12, 0x404040);
    }
}