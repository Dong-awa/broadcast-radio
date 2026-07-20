package bili.dongsz.broadcastradio.event;

import bili.dongsz.broadcastradio.item.RadioTerminalItem;
import bili.dongsz.broadcastradio.utils.SignalSearchManager;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.DistExecutor;

public class PlayerLoginListener {
    
    public static void register() {
        MinecraftForge.EVENT_BUS.register(new PlayerLoginListener());
    }
    
    @SubscribeEvent
    public void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        Player player = event.getEntity();
        if (player.level().isClientSide()) {
            new Thread(() -> {
                try {
                    Thread.sleep(500);
                    DistExecutor.runWhenOn(net.minecraftforge.api.distmarker.Dist.CLIENT, () -> () -> {
                        net.minecraft.client.Minecraft minecraft = net.minecraft.client.Minecraft.getInstance();
                        String serviceName = RadioTerminalItem.getCurrentServiceName(
                            minecraft.level,
                            minecraft.player
                        );
                        SignalSearchManager.getInstance().updateCachedServiceName(serviceName);
                        SignalSearchManager.getInstance().triggerPlayerSearch();
                    });
                } catch (Exception e) {
                }
            }).start();
        }
    }
}