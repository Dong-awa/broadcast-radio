package bili.dongsz.broadcastradio.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

import bili.dongsz.broadcastradio.BroadcastRadio;
import bili.dongsz.broadcastradio.client.SignalStrengthHUD;

/**
 * 客户端收到此包后在快捷栏上方显示信号强度/质量指示。
 */
public class SignalStrengthIndicatorPacket {
    private final int signalStrength;  // 0-100 百分比
    private final int interference;    // 0-100 干扰值

    public SignalStrengthIndicatorPacket(int signalStrength, int interference) {
        this.signalStrength = signalStrength;
        this.interference = interference;
        BroadcastRadio.LOGGER.debug(
            "[SignalStrengthIndicatorPacket] 服务端构造数据包：strength={}, interference={}",
            signalStrength, interference
        );
    }

    public SignalStrengthIndicatorPacket(FriendlyByteBuf buf) {
        this.signalStrength = buf.readInt();
        this.interference = buf.readInt();
        BroadcastRadio.LOGGER.debug(
            "[SignalStrengthIndicatorPacket] 客户端从缓冲区解码：strength={}, interference={}",
            this.signalStrength, this.interference
        );
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeInt(signalStrength);
        buf.writeInt(interference);
        BroadcastRadio.LOGGER.debug(
            "[SignalStrengthIndicatorPacket] 编码到缓冲区：strength={}, interference={}",
            signalStrength, interference
        );
    }

    public static SignalStrengthIndicatorPacket decode(FriendlyByteBuf buf) {
        return new SignalStrengthIndicatorPacket(buf);
    }

    public static void handle(SignalStrengthIndicatorPacket packet, Supplier<NetworkEvent.Context> ctx) {
        NetworkEvent.Context context = ctx.get();
        context.enqueueWork(() -> {
            DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> {
                BroadcastRadio.LOGGER.info(
                    "[SignalStrengthIndicatorPacket] 客户端收到信号指示包：strength={}, interference={}",
                    packet.signalStrength, packet.interference
                );
                SignalStrengthHUD.updateSignal(packet.signalStrength, packet.interference);
            });
        });
        context.setPacketHandled(true);
    }
}