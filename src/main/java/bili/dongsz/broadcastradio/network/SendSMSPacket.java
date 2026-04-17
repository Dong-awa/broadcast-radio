package bili.dongsz.broadcastradio.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.UUID;
import java.util.function.Supplier;

/**
 * SMS数据包 - 客户端本地处理所有逻辑，服务器只负责转发
 */
public class SendSMSPacket {
    private final UUID targetPlayerUUID;
    private final String message;
    private final UUID senderUUID;
    
    public SendSMSPacket(UUID targetPlayerUUID, String message, UUID senderUUID) {
        this.targetPlayerUUID = targetPlayerUUID;
        this.message = message;
        this.senderUUID = senderUUID;
    }
    
    // Getter方法
    public UUID getTargetPlayerUUID() {
        return targetPlayerUUID;
    }
    
    public String getMessage() {
        return message;
    }
    
    public UUID getSenderUUID() {
        return senderUUID;
    }
    
    public SendSMSPacket(FriendlyByteBuf buf) {
        this.targetPlayerUUID = buf.readUUID();
        this.message = buf.readUtf(100);
        this.senderUUID = buf.readUUID();
    }
    
    public void encode(FriendlyByteBuf buf) {
        buf.writeUUID(targetPlayerUUID);
        buf.writeUtf(message, 100);
        buf.writeUUID(senderUUID);
    }
    
    public static SendSMSPacket decode(FriendlyByteBuf buf) {
        return new SendSMSPacket(buf);
    }
    
    /**
     * 服务器端处理 - 只做转发，不做任何检查
     */
    public static void handle(SendSMSPacket packet, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer sender = ctx.get().getSender();
            if (sender != null) {
                // 服务器只做转发，不检查任何逻辑
                // 直接转发给目标玩家（如果在线）
                ServerPlayer receiver = sender.server.getPlayerList().getPlayer(packet.getTargetPlayerUUID());
                if (receiver != null) {
                    // 发送接收方数据包 - 使用正确的API
                    bili.dongsz.broadcastradio.BroadcastRadio.NETWORK.send(
                        net.minecraftforge.network.PacketDistributor.PLAYER.with(() -> receiver),
                        new ReceiveSMSPacket(packet.getSenderUUID(), packet.getMessage())
                    );
                    
                    // 发送确认消息给发送者
                    sender.sendSystemMessage(
                        net.minecraft.network.chat.Component.translatable(
                            "item.broadcast_radio.radio_terminal.sms_sent",
                            receiver.getScoreboardName()
                        )
                    );
                } else {
                    // 如果目标玩家不在线，给发送者提示
                    sender.sendSystemMessage(
                        net.minecraft.network.chat.Component.translatable(
                            "item.broadcast_radio.radio_terminal.sms_failed",
                            "目标玩家不在线"
                        )
                    );
                }
            }
        });
        ctx.get().setPacketHandled(true);
    }
}