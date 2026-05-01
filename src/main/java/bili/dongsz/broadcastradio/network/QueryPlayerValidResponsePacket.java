package bili.dongsz.broadcastradio.network;

import bili.dongsz.broadcastradio.BroadcastRadio;
import bili.dongsz.broadcastradio.utils.SignalSearchManager;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.UUID;
import java.util.function.Supplier;

public class QueryPlayerValidResponsePacket {
    private UUID playerId;
    private boolean isValid;
    
    public QueryPlayerValidResponsePacket(UUID playerId, boolean isValid) {
        this.playerId = playerId;
        this.isValid = isValid;
    }
    
    public static void encode(QueryPlayerValidResponsePacket packet, FriendlyByteBuf buf) {
        buf.writeUUID(packet.playerId);
        buf.writeBoolean(packet.isValid);
    }
    
    public static QueryPlayerValidResponsePacket decode(FriendlyByteBuf buf) {
        return new QueryPlayerValidResponsePacket(buf.readUUID(), buf.readBoolean());
    }
    
    public void handle(Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            SignalSearchManager.getInstance().updatePlayerValidStatus(playerId, isValid);
        });
        ctx.get().setPacketHandled(true);
    }
    
    public static void send(ServerPlayer player, UUID targetPlayerId, boolean isValid) {
        BroadcastRadio.NETWORK.send(
            net.minecraftforge.network.PacketDistributor.PLAYER.with(() -> player),
            new QueryPlayerValidResponsePacket(targetPlayerId, isValid)
        );
    }
}