package bili.dongsz.broadcastradio.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.fml.DistExecutor;

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
            DistExecutor.runWhenOn(net.minecraftforge.api.distmarker.Dist.CLIENT, () -> () -> {
                net.minecraft.client.Minecraft minecraft = net.minecraft.client.Minecraft.getInstance();
                if (minecraft.player != null) {
                    int delay = bili.dongsz.broadcastradio.utils.SMSDelayUtils.getPlayerCurrentNetworkDelay();
                    final String senderName = getSenderName(packet.senderUUID);
                    final String finalMessage = packet.message;

                    new Thread(() -> {
                        try {
                            Thread.sleep(delay);
                        } catch (InterruptedException e) {
                            Thread.currentThread().interrupt();
                            return;
                        }

                        minecraft.execute(() -> {
                            minecraft.player.sendSystemMessage(
                                Component.translatable(
                                    "item.broadcast_radio.radio_terminal.sms_received",
                                    senderName,
                                    finalMessage
                                )
                            );
                            bili.dongsz.broadcastradio.client.SignalStrengthHUD.updateSignal(
                                packet.signalStrength, packet.interference);
                        });
                    }).start();
                }
            });
        });
        ctx.get().setPacketHandled(true);
    }

    private static String getSenderName(UUID senderUUID) {
        final String[] result = {"未知玩家"};
        DistExecutor.unsafeRunWhenOn(net.minecraftforge.api.distmarker.Dist.CLIENT, () -> () -> {
            net.minecraft.client.Minecraft minecraft = net.minecraft.client.Minecraft.getInstance();
            if (minecraft.level != null) {
                var senderPlayer = minecraft.level.getPlayerByUUID(senderUUID);
                if (senderPlayer != null) {
                    result[0] = senderPlayer.getScoreboardName();
                }
            }
        });
        return result[0];
    }
}