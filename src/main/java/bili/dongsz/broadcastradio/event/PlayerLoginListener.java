package bili.dongsz.broadcastradio.event;

import bili.dongsz.broadcastradio.item.RadioTerminalItem;
import bili.dongsz.broadcastradio.utils.SignalSearchManager;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

public class PlayerLoginListener {
    
    public static void register() {
        MinecraftForge.EVENT_BUS.register(new PlayerLoginListener());
    }
    
    @SubscribeEvent
    public void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        Player player = event.getEntity();
        if (player.level().isClientSide()) {
            // 异步执行
            new Thread(() -> {
                try {
                    Thread.sleep(500);
                    String serviceName = RadioTerminalItem.getCurrentServiceName(
                        Minecraft.getInstance().level,
                        Minecraft.getInstance().player
                    );
                    SignalSearchManager.getInstance().updateCachedServiceName(serviceName);
                    SignalSearchManager.getInstance().triggerPlayerSearch();
                    
                } catch (Exception e) {
                    // 忽略
                }
            }).start();
        }
    }
}