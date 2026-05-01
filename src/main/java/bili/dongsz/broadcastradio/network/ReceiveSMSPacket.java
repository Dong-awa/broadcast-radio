package bili.dongsz.broadcastradio.network;

import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraftforge.network.NetworkEvent;

import java.util.UUID;
import java.util.function.Supplier;

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
    public static void handle(ReceiveSMSPacket packet, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            // 发送端已完成前置过滤，直接处理消息
            if (net.minecraft.client.Minecraft.getInstance().player != null) {
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
        });
        ctx.get().setPacketHandled(true);
    }
    
    //获取发送者名称
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