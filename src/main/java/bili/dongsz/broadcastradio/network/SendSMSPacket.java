package bili.dongsz.broadcastradio.network;

import bili.dongsz.broadcastradio.BroadcastRadio;
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

    public static void handle(SendSMSPacket packet, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer sender = ctx.get().getSender();
            if (sender != null) {
                ServerPlayer receiver = sender.server.getPlayerList().getPlayer(packet.getTargetPlayerUUID());
                if (receiver != null) {
                    double baseRange = bili.dongsz.broadcastradio.utils.CommunicationUtils.BASE_COMMUNICATION_RANGE;
                    int signalStrength = bili.dongsz.broadcastradio.utils.CommunicationUtils.calculateSignalStrength(
                            sender, receiver, baseRange, sender.level());

                    int weatherAtReceiver = bili.dongsz.broadcastradio.utils.CommunicationUtils.getWeatherInterference(receiver.level());
                    int jammerAtReceiver = bili.dongsz.broadcastradio.utils.CommunicationUtils.getJammerInterference(
                            receiver.level(), receiver.blockPosition(), 0f);
                    int totalInterference = bili.dongsz.broadcastradio.utils.CommunicationUtils.clampInterference(
                            Math.max(jammerAtReceiver, weatherAtReceiver));

                    if (signalStrength <= 0 || totalInterference >= 100) {
                        sender.sendSystemMessage(
                            net.minecraft.network.chat.Component.translatable(
                                "item.broadcast_radio.radio_terminal.sms_failed",
                                net.minecraft.network.chat.Component.translatable(
                                    "item.broadcast_radio.radio_terminal.sms_failed_signal")
                            )
                        );
                        return;
                    }

                    BroadcastRadio.NETWORK.send(
                        net.minecraftforge.network.PacketDistributor.PLAYER.with(() -> receiver),
                        new ReceiveSMSPacket(packet.getSenderUUID(), packet.getMessage(), signalStrength, totalInterference)
                    );

                    sender.sendSystemMessage(
                        net.minecraft.network.chat.Component.translatable(
                            "item.broadcast_radio.radio_terminal.sms_sent",
                            receiver.getScoreboardName()
                        )
                    );
                } else {
                    sender.sendSystemMessage(
                        net.minecraft.network.chat.Component.translatable(
                            "item.broadcast_radio.radio_terminal.sms_failed",
                            net.minecraft.network.chat.Component.translatable(
                                "item.broadcast_radio.radio_terminal.sms_failed_offline")
                        )
                    );
                }
            }
        });
        ctx.get().setPacketHandled(true);
    }
}