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
        
        // 只在客户端执行
        if (player.level().isClientSide()) {
            // 异步执行初始化，避免卡顿登录过程
            new Thread(() -> {
                try {
                    // 延迟0.5秒，确保世界加载完成
                    Thread.sleep(500);
                    
                    // 执行信号搜索
                    String serviceName = RadioTerminalItem.getCurrentServiceName(
                        Minecraft.getInstance().level,
                        Minecraft.getInstance().player
                    );
                    
                    // 更新缓存
                    SignalSearchManager.getInstance().updateCachedServiceName(serviceName);
                    
                    // 同时查询其他玩家的有效性
                    SignalSearchManager.getInstance().triggerPlayerSearch();
                    
                } catch (Exception e) {
                    // 忽略异常
                }
            }).start();
        }
    }
}