package bili.dongsz.broadcastradio.network;

import bili.dongsz.broadcastradio.server.RadioServerData;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.UUID;
import java.util.function.Supplier;

public class PlayerSignalStatusPacket {
    private UUID playerId;
    private boolean hasSignal;
    
    public PlayerSignalStatusPacket(UUID playerId, boolean hasSignal) {
        this.playerId = playerId;
        this.hasSignal = hasSignal;
    }
    
    public static void encode(PlayerSignalStatusPacket packet, FriendlyByteBuf buf) {
        buf.writeUUID(packet.playerId);
        buf.writeBoolean(packet.hasSignal);
    }
    
    public static PlayerSignalStatusPacket decode(FriendlyByteBuf buf) {
        return new PlayerSignalStatusPacket(buf.readUUID(), buf.readBoolean());
    }
    
    public void handle(Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            RadioServerData.setPlayerSignalStatus(playerId, hasSignal);
        });
        ctx.get().setPacketHandled(true);
    }
}