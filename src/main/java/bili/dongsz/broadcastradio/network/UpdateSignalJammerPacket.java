package bili.dongsz.broadcastradio.network;

import bili.dongsz.broadcastradio.block.entity.SimpleSignalJammerBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.fml.DistExecutor;

import java.util.function.Supplier;

public class UpdateSignalJammerPacket {
    private final BlockPos pos;
    private final float frequency;

    public UpdateSignalJammerPacket() {
        this.pos = BlockPos.ZERO;
        this.frequency = SimpleSignalJammerBlockEntity.DEFAULT_FREQUENCY;
    }

    public UpdateSignalJammerPacket(BlockPos pos, float frequency) {
        this.pos = pos;
        this.frequency = frequency;
    }

    public static void encode(UpdateSignalJammerPacket packet, FriendlyByteBuf buffer) {
        buffer.writeBlockPos(packet.pos);
        buffer.writeFloat(packet.frequency);
    }

    public static UpdateSignalJammerPacket decode(FriendlyByteBuf buffer) {
        return new UpdateSignalJammerPacket(
            buffer.readBlockPos(),
            buffer.readFloat()
        );
    }

    public static void handle(UpdateSignalJammerPacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> {
            if (context.getDirection().getReceptionSide().isServer()) {
                ServerPlayer player = context.getSender();
                if (player != null && player.level().isLoaded(packet.pos)) {
                    var blockEntity = player.level().getBlockEntity(packet.pos);
                    if (blockEntity instanceof SimpleSignalJammerBlockEntity jammerEntity) {
                        jammerEntity.setFrequency(packet.frequency);
                        jammerEntity.setChanged();
                    }
                }
            } else {
                DistExecutor.runWhenOn(net.minecraftforge.api.distmarker.Dist.CLIENT, () -> () -> {
                    bili.dongsz.broadcastradio.client.ClientProxy.updateSignalJammerFrequency(packet.pos, packet.frequency);
                });
            }
        });
        context.setPacketHandled(true);
    }
}