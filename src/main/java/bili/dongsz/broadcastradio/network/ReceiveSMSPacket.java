package bili.dongsz.broadcastradio.network;

import bili.dongsz.broadcastradio.BroadcastRadio;
import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraftforge.network.NetworkEvent;

import java.util.UUID;
import java.util.function.Supplier;

public class ReceiveSMSPacket {
    private final UUID senderUUID;
    private final String message;
    private final int signalStrength;
    private final int interference;

    public ReceiveSMSPacket(UUID senderUUID, String message) {
        this(senderUUID, message, 100, 0);
    }

    public ReceiveSMSPacket(UUID senderUUID, String message, int signalStrength, int interference) {
        this.senderUUID = senderUUID;
        this.message = message;
        this.signalStrength = signalStrength;
        this.interference = interference;
    }

    public ReceiveSMSPacket(FriendlyByteBuf buf) {
        this.senderUUID = buf.readUUID();
        this.message = buf.readUtf(100);
        this.signalStrength = buf.readInt();
        this.interference = buf.readInt();
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeUUID(senderUUID);
        buf.writeUtf(message, 100);
        buf.writeInt(signalStrength);
        buf.writeInt(interference);
    }

    public static ReceiveSMSPacket decode(FriendlyByteBuf buf) {
        return new ReceiveSMSPacket(buf);
    }

    public static void handle(ReceiveSMSPacket packet, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            if (Minecraft.getInstance().player != null) {
                int delay = bili.dongsz.broadcastradio.utils.SMSDelayUtils.getPlayerCurrentNetworkDelay();
                BroadcastRadio.LOGGER.info(
                    "[ReceiveSMSPacket] 客户端收到SMS: 发送者UUID={}, 消息长度={}, 信号强度={}, 干扰={}, 网络延迟={}ms",
                    packet.senderUUID, packet.message == null ? "null" : packet.message.length(),
                    packet.signalStrength, packet.interference, delay
                );

                final String senderName = getSenderName(packet.senderUUID);
                final String finalMessage = packet.message;

                new Thread(() -> {
                    try {
                        Thread.sleep(delay);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        return;
                    }

                    Minecraft.getInstance().execute(() -> {
                        // 显示短信
                        Minecraft.getInstance().player.sendSystemMessage(
                            Component.translatable(
                                "item.broadcast_radio.radio_terminal.sms_received",
                                senderName,
                                finalMessage
                            )
                        );
                        // 显示信号指示
                        BroadcastRadio.LOGGER.info(
                            "[ReceiveSMSPacket] 客户端触发HUD显示: strength={}, interference={}",
                            packet.signalStrength, packet.interference
                        );
                        bili.dongsz.broadcastradio.client.SignalStrengthHUD.updateSignal(
                            packet.signalStrength, packet.interference);
                    });
                }).start();
            } else {
                BroadcastRadio.LOGGER.warn("[ReceiveSMSPacket] 客户端玩家为null，无法处理SMS");
            }
        });
        ctx.get().setPacketHandled(true);
    }

    private static String getSenderName(UUID senderUUID) {
        if (Minecraft.getInstance().level != null) {
            var senderPlayer = Minecraft.getInstance().level.getPlayerByUUID(senderUUID);
            if (senderPlayer != null) {
                return senderPlayer.getScoreboardName();
            }
        }
        return "未知玩家";
    }
}