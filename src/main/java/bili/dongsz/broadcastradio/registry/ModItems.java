package bili.dongsz.broadcastradio.registry;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTabs;
import bili.dongsz.broadcastradio.BroadcastRadio;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.minecraftforge.event.entity.player.ItemTooltipEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

import static com.ibm.icu.util.LocalePriorityList.add;

public class ModItems {
    public static final DeferredRegister<Item> ITEMS =
            DeferredRegister.create(ForgeRegistries.ITEMS, BroadcastRadio.MOD_ID);

    //下面注册物品
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
                    stacksTo(64)
            )
    );
    public static final RegistryObject<Item> PCB_BOARD = ITEMS.register("pcb_board",
            () -> new Item(new Item.Properties().
                    stacksTo(64)
            )
    );
    public static final RegistryObject<Item> PORTABLE_WALKIE_TALKIE = ITEMS.register("portable_walkie_talkie",
            () -> new bili.dongsz.broadcastradio.item.PortableWalkieTalkieItem(new Item.Properties()
                    .stacksTo(1) // 只能堆叠1个
                    .durability(100)
                    .rarity(Rarity.COMMON)));

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