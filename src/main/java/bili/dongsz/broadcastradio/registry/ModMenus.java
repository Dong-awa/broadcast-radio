package bili.dongsz.broadcastradio.registry;

import bili.dongsz.broadcastradio.BroadcastRadio;
import bili.dongsz.broadcastradio.menu.RadioBaseStationMenu;
import bili.dongsz.broadcastradio.menu.RadioTerminalMenu;
import bili.dongsz.broadcastradio.menu.RadioTerminalQuickMenu;
import bili.dongsz.broadcastradio.menu.SimpleRadioMenu;
import bili.dongsz.broadcastradio.menu.SimpleSignalJammerMenu;
import bili.dongsz.broadcastradio.menu.WalkieTalkieMenu;
import bili.dongsz.broadcastradio.menu.EncryptedWalkieTalkieMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraftforge.common.extensions.IForgeMenuType;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ModMenus {
    public static final DeferredRegister<MenuType<?>> MENUS =
            DeferredRegister.create(ForgeRegistries.MENU_TYPES, BroadcastRadio.MOD_ID);

    public static final RegistryObject<MenuType<WalkieTalkieMenu>> WALKIE_TALKIE_MENU = MENUS.register("walkie_talkie_menu",
            () -> IForgeMenuType.create(WalkieTalkieMenu::new));

    public static final RegistryObject<MenuType<SimpleRadioMenu>> SIMPLE_RADIO_MENU = MENUS.register("simple_radio_menu",
            () -> IForgeMenuType.create(SimpleRadioMenu::new));

    public static final RegistryObject<MenuType<EncryptedWalkieTalkieMenu>> ENCRYPTED_WALKIE_TALKIE_MENU = MENUS.register("encrypted_walkie_talkie_menu",
            () -> IForgeMenuType.create(EncryptedWalkieTalkieMenu::new));

    public static final RegistryObject<MenuType<RadioBaseStationMenu>> RADIO_BASE_STATION_MENU = MENUS.register("radio_base_station_menu",
            () -> IForgeMenuType.create(RadioBaseStationMenu::new));

    public static final RegistryObject<MenuType<RadioTerminalMenu>> RADIO_TERMINAL_MENU = MENUS.register("radio_terminal_menu",
            () -> IForgeMenuType.create(RadioTerminalMenu::new));

    public static final RegistryObject<MenuType<RadioTerminalQuickMenu>> RADIO_TERMINAL_QUICK_MENU = MENUS.register("radio_terminal_quick_menu",
            () -> IForgeMenuType.create(RadioTerminalQuickMenu::new));

    public static final RegistryObject<MenuType<SimpleSignalJammerMenu>> SIMPLE_SIGNAL_JAMMER_MENU = MENUS.register("simple_signal_jammer_menu",
            () -> IForgeMenuType.create(SimpleSignalJammerMenu::new));
}