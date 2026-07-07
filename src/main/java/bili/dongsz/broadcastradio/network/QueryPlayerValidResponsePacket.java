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
    private int baseStationX;
    private int baseStationZ;
    
    public QueryPlayerValidResponsePacket(UUID playerId, boolean isValid, int baseStationX, int baseStationZ) {
        this.playerId = playerId;
        this.isValid = isValid;
        this.baseStationX = baseStationX;
        this.baseStationZ = baseStationZ;
    }
    
    public static void encode(QueryPlayerValidResponsePacket packet, FriendlyByteBuf buf) {
        buf.writeUUID(packet.playerId);
        buf.writeBoolean(packet.isValid);
        buf.writeInt(packet.baseStationX);
        buf.writeInt(packet.baseStationZ);
    }
    
    public static QueryPlayerValidResponsePacket decode(FriendlyByteBuf buf) {
        return new QueryPlayerValidResponsePacket(buf.readUUID(), buf.readBoolean(), buf.readInt(), buf.readInt());
    }
    
    public void handle(Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            SignalSearchManager.getInstance().updatePlayerValidStatus(playerId, isValid);
            if (isValid) {
                SignalSearchManager.getInstance().updatePlayerBaseStationPos(playerId, baseStationX, baseStationZ);
            }
        });
        ctx.get().setPacketHandled(true);
    }
    
    public static void send(ServerPlayer player, UUID targetPlayerId, boolean isValid, int baseStationX, int baseStationZ) {
        BroadcastRadio.NETWORK.send(
            net.minecraftforge.network.PacketDistributor.PLAYER.with(() -> player),
            new QueryPlayerValidResponsePacket(targetPlayerId, isValid, baseStationX, baseStationZ)
        );
    }
}