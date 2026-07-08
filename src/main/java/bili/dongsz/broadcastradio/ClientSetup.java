package bili.dongsz.broadcastradio;

import bili.dongsz.broadcastradio.registry.ModBlocks;
import bili.dongsz.broadcastradio.registry.ModMenus;
import bili.dongsz.broadcastradio.screen.SimpleRadioScreen;
import bili.dongsz.broadcastradio.screen.SimpleSignalJammerScreen;
import bili.dongsz.broadcastradio.screen.WalkieTalkieScreen;
import bili.dongsz.broadcastradio.screen.EncryptedWalkieTalkieScreen;
import bili.dongsz.broadcastradio.screen.RadioBaseStationScreen;
import bili.dongsz.broadcastradio.screen.RadioTerminalScreen;
import bili.dongsz.broadcastradio.screen.RadioTerminalQuickScreen;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;

@Mod.EventBusSubscriber(modid = BroadcastRadio.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class ClientSetup {
    @SubscribeEvent
    public static void onClientSetup(FMLClientSetupEvent event) {
        event.enqueueWork(() -> {
            MenuScreens.register(ModMenus.WALKIE_TALKIE_MENU.get(), WalkieTalkieScreen::new);
            MenuScreens.register(ModMenus.ENCRYPTED_WALKIE_TALKIE_MENU.get(), EncryptedWalkieTalkieScreen::new);
            MenuScreens.register(ModMenus.RADIO_TERMINAL_MENU.get(), RadioTerminalScreen::new);
            MenuScreens.register(ModMenus.RADIO_BASE_STATION_MENU.get(), RadioBaseStationScreen::new);
            MenuScreens.register(ModMenus.SIMPLE_SIGNAL_JAMMER_MENU.get(), SimpleSignalJammerScreen::new);
        });
    }
}