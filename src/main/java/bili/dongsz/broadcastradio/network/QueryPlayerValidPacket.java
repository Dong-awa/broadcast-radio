package bili.dongsz.broadcastradio.network;

import bili.dongsz.broadcastradio.BroadcastRadio;
import bili.dongsz.broadcastradio.registry.ModItems;
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
                boolean isValid = checkPlayerValidOnServer(targetPlayer);
                QueryPlayerValidResponsePacket.send(sender, targetPlayerId, isValid);
            }
        });
        ctx.get().setPacketHandled(true);
    }
    
    private boolean checkPlayerValidOnServer(ServerPlayer player) {
        if (player == null) {
            return false;
        }
        
        // 检查玩家是否持有无线电终端
        if (!hasRadioTerminal(player)) {
            return false;
        }
        
        // 查找终端
        ItemStack terminalStack = findRadioTerminalInInventory(player);
        if (terminalStack.isEmpty()) {
            return false;
        }
        
        // 检查电池
        if (!bili.dongsz.broadcastradio.item.RadioTerminalItem.hasBattery(terminalStack)) {
            return false;
        }
        
        int batteryLevel = bili.dongsz.broadcastradio.item.RadioTerminalItem.getBatteryLevel(terminalStack);
        if (batteryLevel <= 0) {
            return false;
        }
        
        // 检查SIM卡
        net.minecraft.nbt.CompoundTag tag = terminalStack.getOrCreateTag();
        if (!tag.contains("SimCard")) {
            return false;
        }
        net.minecraft.nbt.CompoundTag simCardTag = tag.getCompound("SimCard");
        ItemStack simCard = ItemStack.of(simCardTag);
        if (simCard.isEmpty()) {
            return false;
        }
        
        // 检查信号状态（必须有信号才能被搜索到）
        String serviceName = bili.dongsz.broadcastradio.item.RadioTerminalItem.getCurrentServiceName(player.level(), player);
        return !serviceName.equals(net.minecraft.network.chat.Component.translatable("item.broadcast_radio.radio_terminal.no_signal").getString());
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