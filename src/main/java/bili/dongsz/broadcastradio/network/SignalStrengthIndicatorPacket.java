package bili.dongsz.broadcastradio.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * 客户端收到此包后在快捷栏上方显示信号强度/质量指示。
 */
public class SignalStrengthIndicatorPacket {
    private final int signalStrength;  // 0-100 百分比
    private final int interference;    // 0-100 干扰值

    public SignalStrengthIndicatorPacket(int signalStrength, int interference) {
        this.signalStrength = signalStrength;
        this.interference = interference;
    }

    public SignalStrengthIndicatorPacket(FriendlyByteBuf buf) {
        this.signalStrength = buf.readInt();
        this.interference = buf.readInt();
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeInt(signalStrength);
        buf.writeInt(interference);
    }

    public static SignalStrengthIndicatorPacket decode(FriendlyByteBuf buf) {
        return new SignalStrengthIndicatorPacket(buf);
    }

    public static void handle(SignalStrengthIndicatorPacket packet, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> {
                bili.dongsz.broadcastradio.client.SignalStrengthHUD.updateSignal(packet.signalStrength, packet.interference);
            });
        });
        ctx.get().setPacketHandled(true);
    }
}