package bili.dongsz.broadcastradio.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class UpdateEncryptedWalkieTalkiePacket {
    private final float frequency;
    private final String password;

    public UpdateEncryptedWalkieTalkiePacket(float frequency, String password) {
        this.frequency = frequency;
        this.password = password;
    }

    public static void encode(UpdateEncryptedWalkieTalkiePacket packet, FriendlyByteBuf buffer) {
        buffer.writeFloat(packet.frequency);
        buffer.writeUtf(packet.password);
    }

    public static UpdateEncryptedWalkieTalkiePacket decode(FriendlyByteBuf buffer) {
        return new UpdateEncryptedWalkieTalkiePacket(buffer.readFloat(), buffer.readUtf());
    }

    public static void handle(UpdateEncryptedWalkieTalkiePacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> {
            if (context.getDirection().getReceptionSide().isServer()) {
                ServerPlayer player = context.getSender();
                if (player != null) {
                    // 检查主手
                    ItemStack mainHandStack = player.getMainHandItem();
                    if (mainHandStack.getItem() instanceof bili.dongsz.broadcastradio.item.EncryptedWalkieTalkieItem) {
                        mainHandStack.getOrCreateTag().putFloat("Frequency", packet.frequency);
                        mainHandStack.getOrCreateTag().putString("Password", packet.password);
                        return;
                    }
                    // 检查副手
                    ItemStack offHandStack = player.getOffhandItem();
                    if (offHandStack.getItem() instanceof bili.dongsz.broadcastradio.item.EncryptedWalkieTalkieItem) {
                        offHandStack.getOrCreateTag().putFloat("Frequency", packet.frequency);
                        offHandStack.getOrCreateTag().putString("Password", packet.password);
                        return;
                    }
                    // 检查物品栏
                    for (ItemStack stack : player.getInventory().items) {
                        if (stack.getItem() instanceof bili.dongsz.broadcastradio.item.EncryptedWalkieTalkieItem) {
                            stack.getOrCreateTag().putFloat("Frequency", packet.frequency);
                            stack.getOrCreateTag().putString("Password", packet.password);
                            break;
                        }
                    }
                }
            }
        });
        context.setPacketHandled(true);
    }
}