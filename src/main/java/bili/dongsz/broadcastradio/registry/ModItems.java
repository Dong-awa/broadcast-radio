package bili.dongsz.broadcastradio.registry;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import bili.dongsz.broadcastradio.BroadcastRadio;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.minecraftforge.event.entity.player.ItemTooltipEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.level.block.Block;

public class ModItems {
    public static final DeferredRegister<Item> ITEMS =
            DeferredRegister.create(ForgeRegistries.ITEMS, BroadcastRadio.MOD_ID);

    //注册物品
    public static final RegistryObject<Item> TEST_ITEM = ITEMS.register("test",
            () -> new Item(new Item.Properties()
                    .stacksTo(64)
            )
    );
    public static final RegistryObject<Item> COPPER_CONDUCTIVE_SLICE = ITEMS.register("copper_conductive_slice",
            () -> new Item(new Item.Properties()
            .stacksTo(64)
            )
    );
    public static final RegistryObject<Item> INSULATED_RUBBER = ITEMS.register("insulated_rubber",
            () -> new Item(new Item.Properties().
                    stacksTo(8)
            )
    );
    public static final RegistryObject<Item> PCB_BOARD = ITEMS.register("pcb_board",
            () -> new Item(new Item.Properties().
                    stacksTo(64)
            )
    );
    public static final RegistryObject<Item> ELECTRONIC_COMPONENT = ITEMS.register("electronic_component",
            () -> new Item(new Item.Properties().
                    stacksTo(64)
            )
    );
    public static final RegistryObject<Item> RADIO_MODULE = ITEMS.register("radio_module",
            () -> new Item(new Item.Properties().
                    stacksTo(64)
            )
    );
    public static final RegistryObject<Item> RADIO_COMPONENTS = ITEMS.register("radio_components",
            () -> new Item(new Item.Properties().
                    stacksTo(64)
            )
    );
    public static final RegistryObject<Item> ANTENNA = ITEMS.register("antenna",
            () -> new Item(new Item.Properties().
                    stacksTo(64)
            )
    );
    public static final RegistryObject<Item> CIRCUIT_BOARD = ITEMS.register("circuit_board",
            () -> new Item(new Item.Properties().
                    stacksTo(64)
            )
    );
    public static final RegistryObject<Item> PORTABLE_WALKIE_TALKIE = ITEMS.register("portable_walkie_talkie",
            () -> new bili.dongsz.broadcastradio.item.PortableWalkieTalkieItem(new Item.Properties()
                    .stacksTo(1)
                    .durability(100)
                    .rarity(Rarity.COMMON)));
    public static final RegistryObject<Item> ENCRYPTED_WALKIE_TALKIE = ITEMS.register("encrypted_walkie_talkie",
            () -> new bili.dongsz.broadcastradio.item.EncryptedWalkieTalkieItem(new Item.Properties()
                    .stacksTo(1)
                    .durability(100)
                    .rarity(Rarity.UNCOMMON)));
    public static final RegistryObject<Item> SIMPLE_RADIO = ITEMS.register("simple_radio_block",
            () -> new BlockItem(ModBlocks.SIMPLE_RADIO_BLOCK.get(), new Item.Properties()
                    .stacksTo(1)
                    .rarity(Rarity.COMMON)));
    public static final RegistryObject<Item> RADIO_BATTERY = ITEMS.register("radio_battery",
            () -> new bili.dongsz.broadcastradio.item.RadioBatteryItem(new Item.Properties()
                    .stacksTo(64)
                    .rarity(Rarity.COMMON)));

    public static final RegistryObject<Item> RADIO_BASE_STATION = ITEMS.register("radio_base_station",
            () -> new BlockItem(ModBlocks.RADIO_BASE_STATION_BLOCK.get(), new Item.Properties()
                    .stacksTo(1)
                    .rarity(Rarity.UNCOMMON)));

    public static final RegistryObject<Item> TWO_G_UNIVERSAL = ITEMS.register("two_g_universal",
            () -> new Item(new Item.Properties()
                    .stacksTo(1)
                    .rarity(Rarity.COMMON)));

    public static final RegistryObject<Item> THREE_G_UNIVERSAL = ITEMS.register("three_g_universal",
            () -> new Item(new Item.Properties()
                    .stacksTo(1)
                    .rarity(Rarity.UNCOMMON)));

    public static final RegistryObject<Item> FOUR_G_UNIVERSAL = ITEMS.register("four_g_universal",
            () -> new Item(new Item.Properties()
                    .stacksTo(1)
                    .rarity(Rarity.RARE)));

    public static final RegistryObject<Item> RADIO_TERMINAL = ITEMS.register("radio_terminal",
            () -> new bili.dongsz.broadcastradio.item.RadioTerminalItem(new Item.Properties()
                    .stacksTo(1)
                    .rarity(Rarity.UNCOMMON)));

    public static final RegistryObject<Item> STORAGE_BATTERY = ITEMS.register("storage_battery",
            () -> new bili.dongsz.broadcastradio.item.BatteryItem(new Item.Properties()
                    .stacksTo(64)
                    .rarity(Rarity.UNCOMMON)
                    .durability(700))); // 最大耐久700

    @SubscribeEvent
    public void onItemTooltip(ItemTooltipEvent event) {
        if (event.getItemStack().is(ModItems.COPPER_CONDUCTIVE_SLICE.get())) {
            event.getToolTip().add(Component.translatable("item.broadcast_radio.copper_conductive_slice.desc").withStyle(ChatFormatting.GRAY));
        }
        if (event.getItemStack().is(ModItems.INSULATED_RUBBER.get())) {
            event.getToolTip().add(Component.translatable("item.broadcast_radio.insulated_rubber.desc").withStyle(ChatFormatting.GRAY));
        }
    }
}