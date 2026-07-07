package bili.dongsz.broadcastradio.network;

import bili.dongsz.broadcastradio.BroadcastRadio;
import bili.dongsz.broadcastradio.registry.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.network.NetworkEvent;

import java.util.UUID;
import java.util.function.Supplier;

public class QueryPlayerValidPacket {
    private UUID targetPlayerId;
    
    public QueryPlayerValidPacket(UUID targetPlayerId) {
        this.targetPlayerId = targetPlayerId;
    }
    
    public static void encode(QueryPlayerValidPacket packet, FriendlyByteBuf buf) {
        buf.writeUUID(packet.targetPlayerId);
    }
    
    public static QueryPlayerValidPacket decode(FriendlyByteBuf buf) {
        return new QueryPlayerValidPacket(buf.readUUID());
    }
    
    public void handle(Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer sender = ctx.get().getSender();
            if (sender != null) {
                ServerPlayer targetPlayer = sender.server.getPlayerList().getPlayer(targetPlayerId);
                ValidationResult result = checkPlayerValidOnServer(targetPlayer);
                QueryPlayerValidResponsePacket.send(sender, targetPlayerId, result.isValid, result.baseStationX, result.baseStationZ);
            }
        });
        ctx.get().setPacketHandled(true);
    }
    
    private static class ValidationResult {
        final boolean isValid;
        final int baseStationX;
        final int baseStationZ;
        
        ValidationResult(boolean isValid, int baseStationX, int baseStationZ) {
            this.isValid = isValid;
            this.baseStationX = baseStationX;
            this.baseStationZ = baseStationZ;
        }
    }
    
    private ValidationResult checkPlayerValidOnServer(ServerPlayer player) {
        if (player == null) {
            return new ValidationResult(false, 0, 0);
        }
        
        if (!hasRadioTerminal(player)) {
            return new ValidationResult(false, 0, 0);
        }
        
        ItemStack terminalStack = findRadioTerminalInInventory(player);
        if (terminalStack.isEmpty()) {
            return new ValidationResult(false, 0, 0);
        }
        
        if (!bili.dongsz.broadcastradio.item.RadioTerminalItem.hasBattery(terminalStack)) {
            return new ValidationResult(false, 0, 0);
        }
        
        int batteryLevel = bili.dongsz.broadcastradio.item.RadioTerminalItem.getBatteryLevel(terminalStack);
        if (batteryLevel <= 0) {
            return new ValidationResult(false, 0, 0);
        }
        
        net.minecraft.nbt.CompoundTag tag = terminalStack.getTag();
        if (tag == null || !tag.contains("SimCard")) {
            return new ValidationResult(false, 0, 0);
        }
        net.minecraft.nbt.CompoundTag simCardTag = tag.getCompound("SimCard");
        ItemStack simCard = ItemStack.of(simCardTag);
        if (simCard.isEmpty()) {
            return new ValidationResult(false, 0, 0);
        }
        
        BlockPos baseStationPos = bili.dongsz.broadcastradio.item.RadioTerminalItem.getCurrentBaseStationPos(player.level(), player);
        if (baseStationPos == null) {
            return new ValidationResult(false, 0, 0);
        }
        
        return new ValidationResult(true, baseStationPos.getX(), baseStationPos.getZ());
    }
    
    private boolean hasRadioTerminal(ServerPlayer player) {
        if (player.getMainHandItem().getItem() == ModItems.RADIO_TERMINAL.get() ||
            player.getOffhandItem().getItem() == ModItems.RADIO_TERMINAL.get()) {
            return true;
        }
        return player.getInventory().hasAnyMatching(itemStack -> 
            itemStack.getItem() == ModItems.RADIO_TERMINAL.get()
        );
    }
    
    private ItemStack findRadioTerminalInInventory(ServerPlayer player) {
        if (player.getMainHandItem().getItem() == ModItems.RADIO_TERMINAL.get()) {
            return player.getMainHandItem();
        }
        if (player.getOffhandItem().getItem() == ModItems.RADIO_TERMINAL.get()) {
            return player.getOffhandItem();
        }
        for (int i = 0; i < 41; i++) {
            ItemStack stack = player.getInventory().getItem(i);
            if (stack.getItem() == ModItems.RADIO_TERMINAL.get()) {
                return stack;
            }
        }
        return ItemStack.EMPTY;
    }
}