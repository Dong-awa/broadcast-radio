package bili.dongsz.broadcastradio.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * Server -> Client packet to update the client's cached per-player service flag
 */
public class UpdateClientServiceFlagPacket {
	private final boolean present; // whether override exists
	private final boolean flag;    // the override value if present

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
			try {
				// Ensure we execute on client main thread
				net.minecraft.client.Minecraft.getInstance().execute(() -> {
					if (net.minecraft.client.Minecraft.getInstance().player != null) {
						net.minecraft.nbt.CompoundTag pdata = net.minecraft.client.Minecraft.getInstance().player.getPersistentData();
						if (packet.present) {
							pdata.putBoolean("BroadcastRadioForceValidService", packet.flag);
						} else {
							pdata.remove("BroadcastRadioForceValidService");
						}
					}
				});
			} catch (Exception ignored) {
			}
		});
		ctx.get().setPacketHandled(true);
	}
}


