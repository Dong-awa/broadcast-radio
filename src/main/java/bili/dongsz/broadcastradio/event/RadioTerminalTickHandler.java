package bili.dongsz.broadcastradio.event;

import bili.dongsz.broadcastradio.item.RadioTerminalItem;
import bili.dongsz.broadcastradio.BroadcastRadio;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = BroadcastRadio.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class RadioTerminalTickHandler {

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }

        Player player = event.player;
        if (player == null) {
            return;
        }

        ItemStack mainHandStack = player.getMainHandItem();
        ItemStack offHandStack = player.getOffhandItem();
        if (!mainHandStack.isEmpty() && mainHandStack.getItem() instanceof RadioTerminalItem) {
            handleTerminalTick(mainHandStack, player);
        }
        if (!offHandStack.isEmpty() && offHandStack.getItem() instanceof RadioTerminalItem) {
            handleTerminalTick(offHandStack, player);
        }
    }

    private static void handleTerminalTick(ItemStack terminalStack, Player player) {
        if (!RadioTerminalItem.hasBattery(terminalStack)) {
            return;
        }

        // -1/60s
        RadioTerminalItem.consumeBatteryInTick(terminalStack, player);
    }
}