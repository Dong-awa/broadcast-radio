package bili.dongsz.broadcastradio.network;

import bili.dongsz.broadcastradio.block.entity.SimpleRadioBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class UpdateFrequencyPacket {
    private final BlockPos pos;
    private final float frequency;

    public UpdateFrequencyPacket(BlockPos pos, float frequency) {
        this.pos = pos;
        this.frequency = frequency;
    }

    public static void encode(UpdateFrequencyPacket packet, FriendlyByteBuf buffer) {
        buffer.writeBlockPos(packet.pos);
        buffer.writeFloat(packet.frequency);
    }

    public static UpdateFrequencyPacket decode(FriendlyByteBuf buffer) {
        return new UpdateFrequencyPacket(buffer.readBlockPos(), buffer.readFloat());
    }

    public static void handle(UpdateFrequencyPacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> {
            if (context.getDirection().getReceptionSide().isServer()) {
                ServerPlayer player = context.getSender();
                if (player != null && player.level().isLoaded(packet.pos)) {
                    var blockEntity = player.level().getBlockEntity(packet.pos);
                    if (blockEntity instanceof SimpleRadioBlockEntity radioEntity) {
                        radioEntity.setFrequency(packet.frequency);
                    }
                }
            }
        });
        context.setPacketHandled(true);
    }
}