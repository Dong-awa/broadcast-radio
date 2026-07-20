package bili.dongsz.broadcastradio.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.fml.DistExecutor;

import java.util.function.Supplier;

public class UpdateClientServiceFlagPacket {
	private final boolean present;
	private final boolean flag;

	public UpdateClientServiceFlagPacket(boolean present, boolean flag) {
		this.present = present;
		this.flag = flag;
	}

	public UpdateClientServiceFlagPacket(FriendlyByteBuf buf) {
		this.present = buf.readBoolean();
		this.flag = buf.readBoolean();
	}

	public void encode(FriendlyByteBuf buf) {
		buf.writeBoolean(present);
		buf.writeBoolean(flag);
	}

	public static UpdateClientServiceFlagPacket decode(FriendlyByteBuf buf) {
		return new UpdateClientServiceFlagPacket(buf);
	}

	public static void handle(UpdateClientServiceFlagPacket packet, Supplier<NetworkEvent.Context> ctx) {
		ctx.get().enqueueWork(() -> {
			DistExecutor.runWhenOn(net.minecraftforge.api.distmarker.Dist.CLIENT, () -> () -> {
				bili.dongsz.broadcastradio.client.ClientProxy.updateClientServiceFlag(packet.present, packet.flag);
			});
		});
		ctx.get().setPacketHandled(true);
	}
}