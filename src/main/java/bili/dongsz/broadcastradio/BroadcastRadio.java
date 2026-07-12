package bili.dongsz.broadcastradio;

import bili.dongsz.broadcastradio.network.UpdateFrequencyPacket;
import bili.dongsz.broadcastradio.network.UpdateRadioBaseStationPacket;
import bili.dongsz.broadcastradio.network.UpdateEncryptedWalkieTalkiePacket;
import bili.dongsz.broadcastradio.network.UpdateSignalJammerPacket;
import bili.dongsz.broadcastradio.network.PlayerSignalStatusPacket;
import bili.dongsz.broadcastradio.network.QueryPlayerValidPacket;
import bili.dongsz.broadcastradio.network.QueryPlayerValidResponsePacket;
import bili.dongsz.broadcastradio.event.PlayerLoginListener;
import bili.dongsz.broadcastradio.registry.ModBlocks;
import bili.dongsz.broadcastradio.registry.ModBlockEntities;
import bili.dongsz.broadcastradio.registry.ModCreativeModeTabs;
import bili.dongsz.broadcastradio.registry.ModItems;
import bili.dongsz.broadcastradio.registry.ModMenus;
import bili.dongsz.broadcastradio.utils.AbsorptionManager;
import bili.dongsz.broadcastradio.utils.ReflectionManager;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.server.ServerStartingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.network.simple.SimpleChannel;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

@Mod("broadcast_radio")
public class BroadcastRadio {
    public static final String MOD_ID = "broadcast_radio";
    public static final Logger LOGGER = LogManager.getLogger();
    public static boolean HAS_VALID_SERVICE = false;

    // Network channel
    public static final SimpleChannel NETWORK = net.minecraftforge.network.NetworkRegistry.ChannelBuilder
            .named(ResourceLocation.fromNamespaceAndPath(MOD_ID, "network"))
            .networkProtocolVersion(() -> "1")
            .clientAcceptedVersions(s -> true)
            .serverAcceptedVersions(s -> true)
            .simpleChannel();

    public BroadcastRadio() {
        FMLJavaModLoadingContext.get().getModEventBus().register(ModItems.class);
        ModBlocks.BLOCKS.register(FMLJavaModLoadingContext.get().getModEventBus());
        ModItems.ITEMS.register(FMLJavaModLoadingContext.get().getModEventBus());
        ModBlockEntities.BLOCK_ENTITIES.register(FMLJavaModLoadingContext.get().getModEventBus());
        ModCreativeModeTabs.CREATIVE_MODE_TABS.register(FMLJavaModLoadingContext.get().getModEventBus());
        ModMenus.MENUS.register(FMLJavaModLoadingContext.get().getModEventBus());
        MinecraftForge.EVENT_BUS.register(this);
        
        // 注册玩家登录事件监听器
        PlayerLoginListener.register();

        // Register network messages
        NETWORK.registerMessage(0, UpdateFrequencyPacket.class, UpdateFrequencyPacket::encode, UpdateFrequencyPacket::decode, UpdateFrequencyPacket::handle);
        NETWORK.registerMessage(1, bili.dongsz.broadcastradio.network.UpdateWalkieTalkieFrequencyPacket.class,
            bili.dongsz.broadcastradio.network.UpdateWalkieTalkieFrequencyPacket::encode,
            bili.dongsz.broadcastradio.network.UpdateWalkieTalkieFrequencyPacket::decode,
            bili.dongsz.broadcastradio.network.UpdateWalkieTalkieFrequencyPacket::handle);
        NETWORK.registerMessage(2, UpdateEncryptedWalkieTalkiePacket.class,
            UpdateEncryptedWalkieTalkiePacket::encode,
            UpdateEncryptedWalkieTalkiePacket::decode,
            UpdateEncryptedWalkieTalkiePacket::handle);
        NETWORK.registerMessage(3, UpdateRadioBaseStationPacket.class,
            UpdateRadioBaseStationPacket::encode,
            UpdateRadioBaseStationPacket::decode,
            UpdateRadioBaseStationPacket::handle);
        NETWORK.registerMessage(4, bili.dongsz.broadcastradio.network.SendSMSPacket.class,
            bili.dongsz.broadcastradio.network.SendSMSPacket::encode,
            bili.dongsz.broadcastradio.network.SendSMSPacket::decode,
            bili.dongsz.broadcastradio.network.SendSMSPacket::handle);
        NETWORK.registerMessage(5, bili.dongsz.broadcastradio.network.ReceiveSMSPacket.class,
            bili.dongsz.broadcastradio.network.ReceiveSMSPacket::encode,
            bili.dongsz.broadcastradio.network.ReceiveSMSPacket::decode,
            bili.dongsz.broadcastradio.network.ReceiveSMSPacket::handle);
        NETWORK.registerMessage(6, PlayerSignalStatusPacket.class,
            PlayerSignalStatusPacket::encode,
            PlayerSignalStatusPacket::decode,
            PlayerSignalStatusPacket::handle);
        NETWORK.registerMessage(7, QueryPlayerValidPacket.class,
            QueryPlayerValidPacket::encode,
            QueryPlayerValidPacket::decode,
            QueryPlayerValidPacket::handle);
        NETWORK.registerMessage(8, QueryPlayerValidResponsePacket.class,
            QueryPlayerValidResponsePacket::encode,
            QueryPlayerValidResponsePacket::decode,
            QueryPlayerValidResponsePacket::handle);
        NETWORK.registerMessage(9, UpdateSignalJammerPacket.class,
            UpdateSignalJammerPacket::encode,
            UpdateSignalJammerPacket::decode,
            UpdateSignalJammerPacket::handle);
        NETWORK.registerMessage(10, bili.dongsz.broadcastradio.network.SignalStrengthIndicatorPacket.class,
            bili.dongsz.broadcastradio.network.SignalStrengthIndicatorPacket::encode,
            bili.dongsz.broadcastradio.network.SignalStrengthIndicatorPacket::decode,
            bili.dongsz.broadcastradio.network.SignalStrengthIndicatorPacket::handle);

        // 注册 JVM 关闭钩子
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            bili.dongsz.broadcastradio.utils.SignalSearchManager.getInstance().cleanup();
            bili.dongsz.broadcastradio.utils.RadioThreadPoolManager.getInstance().shutdown();
        }, "BroadcastRadio-ShutdownHook"));
    }

    @SubscribeEvent
    public void onServerStarting(ServerStartingEvent event) {
        AbsorptionManager.loadFromResources(event.getServer());
        LOGGER.info("[BroadcastRadio] Absorption values loaded: {} entries, default {}",
                AbsorptionManager.getAbsorptionMapSize(), AbsorptionManager.getDefaultAbsorption());
        ReflectionManager.loadFromResources(event.getServer());
        LOGGER.info("[BroadcastRadio] Reflection values loaded: {} entries, default {}",
                ReflectionManager.getReflectionMapSize(), ReflectionManager.getDefaultReflection());
    }
}