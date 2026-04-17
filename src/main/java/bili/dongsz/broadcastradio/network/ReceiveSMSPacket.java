package bili.dongsz.broadcastradio.network;

import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraftforge.network.NetworkEvent;

import java.util.UUID;
import java.util.function.Supplier;

/**
 * 接收方SMS数据包 - 客户端本地处理所有逻辑
 */
public class ReceiveSMSPacket {
    private final UUID senderUUID;
    private final String message;
    
    public ReceiveSMSPacket(UUID senderUUID, String message) {
        this.senderUUID = senderUUID;
        this.message = message;
    }
    
    public ReceiveSMSPacket(FriendlyByteBuf buf) {
        this.senderUUID = buf.readUUID();
        this.message = buf.readUtf(100);
    }
    
    public void encode(FriendlyByteBuf buf) {
        buf.writeUUID(senderUUID);
        buf.writeUtf(message, 100);
    }
    
    public static ReceiveSMSPacket decode(FriendlyByteBuf buf) {
        return new ReceiveSMSPacket(buf);
    }
    
    /**
     * 客户端处理 - 本地检查背包终端并显示消息（带延迟效果）
     */
    public static void handle(ReceiveSMSPacket packet, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            // 客户端本地检查背包是否有终端
            if (net.minecraft.client.Minecraft.getInstance().player != null) {
                boolean hasTerminal = false;
                for (int i = 0; i < net.minecraft.client.Minecraft.getInstance().player.getInventory().getContainerSize(); i++) {
                    if (net.minecraft.client.Minecraft.getInstance().player.getInventory().getItem(i).getItem() == bili.dongsz.broadcastradio.registry.ModItems.RADIO_TERMINAL.get()) {
                        hasTerminal = true;
                        break;
                    }
                }

                if (hasTerminal) {
                    // 获取当前网络频段延迟
                    int delay = bili.dongsz.broadcastradio.utils.SMSDelayUtils.getPlayerCurrentNetworkDelay();
                    
                    // 获取发送者名称
                    final String senderName = getSenderName(packet.senderUUID);
                    final String finalMessage = packet.message;
                    
                    // 启动客户端定时器，延迟显示
                    new Thread(() -> {
                        try {
                            Thread.sleep(delay);
                        } catch (InterruptedException e) {
                            Thread.currentThread().interrupt();
                            return;
                        }
                        
                        // 定时器结束后，在主线程显示短信
                        net.minecraft.client.Minecraft.getInstance().execute(() -> {
                            // 显示短信
                            net.minecraft.client.Minecraft.getInstance().player.sendSystemMessage(
                                net.minecraft.network.chat.Component.translatable(
                                    "item.broadcast_radio.radio_terminal.sms_received",
                                    senderName,
                                    finalMessage
                                )
                            );
                        });
                    }).start();
                }
            }
        });
        ctx.get().setPacketHandled(true);
    }
    
    /**
     * 获取发送者名称
     */
    private static String getSenderName(UUID senderUUID) {
        if (net.minecraft.client.Minecraft.getInstance().level != null) {
            var senderPlayer = net.minecraft.client.Minecraft.getInstance().level.getPlayerByUUID(senderUUID);
            if (senderPlayer != null) {
                return senderPlayer.getScoreboardName();
            }
        }
        return "未知玩家";
    }
}