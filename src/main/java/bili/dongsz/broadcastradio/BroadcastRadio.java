package bili.dongsz.broadcastradio;

import bili.dongsz.broadcastradio.network.UpdateFrequencyPacket;
import bili.dongsz.broadcastradio.network.UpdateRadioBaseStationPacket;
import bili.dongsz.broadcastradio.network.UpdateEncryptedWalkieTalkiePacket;
import bili.dongsz.broadcastradio.registry.ModBlocks;
import bili.dongsz.broadcastradio.registry.ModBlockEntities;
import bili.dongsz.broadcastradio.registry.ModCreativeModeTabs;
import bili.dongsz.broadcastradio.registry.ModItems;
import bili.dongsz.broadcastradio.registry.ModMenus;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import com.mojang.brigadier.arguments.BoolArgumentType;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.network.simple.SimpleChannel;

@Mod("broadcast_radio")
public class BroadcastRadio {
    public static final String MOD_ID = "broadcast_radio";
    public static boolean HAS_VALID_SERVICE = false;

    // Network channel
    public static final SimpleChannel NETWORK = net.minecraftforge.network.NetworkRegistry.ChannelBuilder
            .named(new net.minecraft.resources.ResourceLocation(MOD_ID, "network"))
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
    }

    @SubscribeEvent
    public void onRegisterCommands(RegisterCommandsEvent event) {
        // /br_setservice <true|false>  - set persistent per-player service flag
        event.getDispatcher().register(
            Commands.literal("br_setservice")
                // /br_setservice <state>  -> set for self (player)
                .then(Commands.argument("state", BoolArgumentType.bool())
                    .executes(ctx -> {
                        boolean state = BoolArgumentType.getBool(ctx, "state");
                        try {
                            ServerPlayer player = ctx.getSource().getPlayerOrException();
                            net.minecraft.nbt.CompoundTag pdata = player.getPersistentData();
                            pdata.putBoolean("BroadcastRadioForceValidService", state);
                            ctx.getSource().sendSuccess(() -> Component.literal("BroadcastRadio: set your service state to " + state), false);
                        } catch (Exception e) {
                            // silently ignore - command may be run from console
                        }
                        return 1;
                    })
                )
                // /br_setservice <target> <state> -> set for target player (requires OP)
                .then(Commands.argument("target", EntityArgument.player())
                    .then(Commands.argument("state", BoolArgumentType.bool())
                        .requires(src -> src.hasPermission(2))
                        .executes(ctx -> {
                            ServerPlayer target = EntityArgument.getPlayer(ctx, "target");
                            boolean state = BoolArgumentType.getBool(ctx, "state");
                            net.minecraft.nbt.CompoundTag pdata = target.getPersistentData();
                            pdata.putBoolean("BroadcastRadioForceValidService", state);
                            ctx.getSource().sendSuccess(() -> Component.literal("BroadcastRadio: set " + target.getName().getString() + " service state to " + state), false);
                            return 1;
                        })
                    )
                )
        );
    }
}