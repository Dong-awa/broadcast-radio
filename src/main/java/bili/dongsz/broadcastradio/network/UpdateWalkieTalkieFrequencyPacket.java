package bili.dongsz.broadcastradio.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class UpdateWalkieTalkieFrequencyPacket {
    private final float frequency;

    public UpdateWalkieTalkieFrequencyPacket(float frequency) {
        this.frequency = frequency;
    }

    public static void encode(UpdateWalkieTalkieFrequencyPacket packet, FriendlyByteBuf buffer) {
        buffer.writeFloat(packet.frequency);
    }

    public static UpdateWalkieTalkieFrequencyPacket decode(FriendlyByteBuf buffer) {
        return new UpdateWalkieTalkieFrequencyPacket(buffer.readFloat());
    }

    public static void handle(UpdateWalkieTalkieFrequencyPacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> {
            if (context.getDirection().getReceptionSide().isServer()) {
                ServerPlayer player = context.getSender();
                if (player != null) {
                    // 首先检查主手
                    ItemStack mainHandStack = player.getMainHandItem();
                    if (mainHandStack.getItem() instanceof bili.dongsz.broadcastradio.item.PortableWalkieTalkieItem) {
                        mainHandStack.getOrCreateTag().putFloat("Frequency", packet.frequency);
                        return;
                    }
                    
                    // 然后检查副手
                    ItemStack offHandStack = player.getOffhandItem();
                    if (offHandStack.getItem() instanceof bili.dongsz.broadcastradio.item.PortableWalkieTalkieItem) {
                        offHandStack.getOrCreateTag().putFloat("Frequency", packet.frequency);
                        return;
                    }
                    
                    // 最后检查物品栏
                    for (ItemStack stack : player.getInventory().items) {
                        if (stack.getItem() instanceof bili.dongsz.broadcastradio.item.PortableWalkieTalkieItem) {
                            stack.getOrCreateTag().putFloat("Frequency", packet.frequency);
                            break;
                        }
                    }
                }
            }
        });
        context.setPacketHandled(true);
    }
}