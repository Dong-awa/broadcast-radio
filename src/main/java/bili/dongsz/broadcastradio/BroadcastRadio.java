package bili.dongsz.broadcastradio;

import bili.dongsz.broadcastradio.registry.ModCreativeModeTabs;
import bili.dongsz.broadcastradio.registry.ModItems;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

@Mod("broadcast_radio")
public class BroadcastRadio {
    public static final String MOD_ID = "broadcast_radio";

    public BroadcastRadio() {
        FMLJavaModLoadingContext.get().getModEventBus().register(ModItems.class);
        ModItems.ITEMS.register(FMLJavaModLoadingContext.get().getModEventBus());
        ModCreativeModeTabs.CREATIVE_MODE_TABS.register(FMLJavaModLoadingContext.get().getModEventBus());
        MinecraftForge.EVENT_BUS.register(this);
    }
}