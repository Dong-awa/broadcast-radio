package bili.dongsz.broadcastradio.network;

import bili.dongsz.broadcastradio.block.entity.RadioBaseStationBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class UpdateRadioBaseStationPacket {
    private final BlockPos pos;
    private final int signalRange;
    private final int networkType;
    private final String serviceName;

    public UpdateRadioBaseStationPacket() {
        this.pos = BlockPos.ZERO;
        this.signalRange = 0;
        this.networkType = 0;
        this.serviceName = "";
    }

    public UpdateRadioBaseStationPacket(BlockPos pos, int signalRange, int networkType, String serviceName) {
        this.pos = pos;
        this.signalRange = signalRange;
        this.networkType = networkType;
        this.serviceName = serviceName;
    }

    public static void encode(UpdateRadioBaseStationPacket packet, FriendlyByteBuf buffer) {
        buffer.writeBlockPos(packet.pos);
        buffer.writeInt(packet.signalRange);
        buffer.writeInt(packet.networkType);
        buffer.writeUtf(packet.serviceName, 50);
    }

    public static UpdateRadioBaseStationPacket decode(FriendlyByteBuf buffer) {
        return new UpdateRadioBaseStationPacket(
            buffer.readBlockPos(),
            buffer.readInt(),
            buffer.readInt(),
            buffer.readUtf(50)
        );
    }

    public static void handle(UpdateRadioBaseStationPacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> {
            if (context.getDirection().getReceptionSide().isServer()) {
                ServerPlayer player = context.getSender();
                if (player != null && player.level().isLoaded(packet.pos)) {
                    var blockEntity = player.level().getBlockEntity(packet.pos);
                    if (blockEntity instanceof RadioBaseStationBlockEntity stationEntity) {
                        stationEntity.setSignalRange(packet.signalRange);
                        stationEntity.setNetworkType(RadioBaseStationBlockEntity.NetworkType.values()[packet.networkType]);
                        stationEntity.setServiceName(packet.serviceName);
                        stationEntity.setChanged();
                    }
                }
            } else {
                // Client-side handling for data synchronization
                var level = net.minecraft.client.Minecraft.getInstance().level;
                if (level != null && level.isLoaded(packet.pos)) {
                    var blockEntity = level.getBlockEntity(packet.pos);
                    if (blockEntity instanceof RadioBaseStationBlockEntity stationEntity) {
                        stationEntity.setSignalRange(packet.signalRange);
                        stationEntity.setNetworkType(RadioBaseStationBlockEntity.NetworkType.values()[packet.networkType]);
                        stationEntity.setServiceName(packet.serviceName);
                    }
                }
            }
        });
        context.setPacketHandled(true);
    }
}