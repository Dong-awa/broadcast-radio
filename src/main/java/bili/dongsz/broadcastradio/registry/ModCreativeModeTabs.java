package bili.dongsz.broadcastradio.registry;

import bili.dongsz.broadcastradio.BroadcastRadio;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;

public class ModCreativeModeTabs {
    public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, BroadcastRadio.MOD_ID);

    public static final RegistryObject<CreativeModeTab> BROADCAST_RADIO_TAB = CREATIVE_MODE_TABS.register("broadcast_radio_tab",
            () -> CreativeModeTab.builder()
                    .icon(() -> new ItemStack(ModItems.COPPER_CONDUCTIVE_SLICE.get()))
                    .title(Component.translatable("itemGroup.broadcast_radio"))
                    .displayItems((parameters, output) -> {
                        // 功能物品/方块
                        output.accept(ModItems.PORTABLE_WALKIE_TALKIE.get());
                        output.accept(ModItems.ENCRYPTED_WALKIE_TALKIE.get());
                        output.accept(ModItems.SIMPLE_RADIO.get());
                        output.accept(ModItems.RADIO_BASE_STATION.get());
                        output.accept(ModItems.RADIO_TERMINAL.get());

                        // 原材料
                        output.accept(ModItems.COPPER_CONDUCTIVE_SLICE.get());
                        output.accept(ModItems.INSULATED_RUBBER.get());
                        output.accept(ModItems.PCB_BOARD.get());
                        output.accept(ModItems.ELECTRONIC_COMPONENT.get());
                        output.accept(ModItems.RADIO_MODULE.get());
                        output.accept(ModItems.RADIO_COMPONENTS.get());
                        output.accept(ModItems.ANTENNA.get());
                        output.accept(ModItems.CIRCUIT_BOARD.get());
                        
                        // 消耗品
                        output.accept(ModItems.RADIO_BATTERY.get());
                        output.accept(ModItems.STORAGE_BATTERY.get());
                        output.accept(ModItems.TWO_G_UNIVERSAL.get());
                        output.accept(ModItems.THREE_G_UNIVERSAL.get());
                        output.accept(ModItems.FOUR_G_UNIVERSAL.get());
                    })
                    .build()
    );
}