package bili.dongsz.broadcastradio.client;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.minecraft.client.Minecraft;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterClientCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class ClientCommandRegistrar {
    @SubscribeEvent
    public static void onRegisterClientCommands(RegisterClientCommandsEvent event) {
        LiteralArgumentBuilder<?> root = Commands.literal("br_debugclient");
        root.executes(ctx -> {
            Minecraft mc = Minecraft.getInstance();
            if (mc.player == null) return 0;
            try {
                Class<?> cls = Class.forName("bili.dongsz.broadcastradio.utils.SignalSearchManager");
                java.lang.reflect.Method getInstance = cls.getMethod("getInstance");
                Object mgr = getInstance.invoke(null);
                java.lang.reflect.Method hasValidSignal = cls.getMethod("hasValidSignal");
                Object hs = hasValidSignal.invoke(mgr);
                mc.player.sendSystemMessage(Component.literal("BroadcastRadio.HAS_VALID_SERVICE=" + bili.dongsz.broadcastradio.BroadcastRadio.HAS_VALID_SERVICE));
                mc.player.sendSystemMessage(Component.literal("SignalSearchManager.hasValidSignal=" + String.valueOf(hs)));

                java.lang.reflect.Method getCached = cls.getMethod("getCachedOnlinePlayers");
                java.util.List<?> cached = (java.util.List<?>) getCached.invoke(mgr);
                mc.player.sendSystemMessage(Component.literal("cachedOnlinePlayers.size=" + cached.size()));
                for (Object po : cached) {
                    try {
                        java.lang.reflect.Method nameMeth = po.getClass().getMethod("getScoreboardName");
                        Object name = nameMeth.invoke(po);
                        mc.player.sendSystemMessage(Component.literal("cached: " + String.valueOf(name)));
                    } catch (Exception ignored) {
                    }
                }
                // Additional local scan: report nearby RadioBaseStationBlockEntity info for debugging
                try {
                    net.minecraft.world.level.Level level = mc.level;
                    if (level != null) {
                        net.minecraft.core.BlockPos playerPos = mc.player.blockPosition();
                        int chunkRange = 4;
                        int found = 0;
                        for (int cx = -chunkRange; cx <= chunkRange; cx++) {
                            for (int cz = -chunkRange; cz <= chunkRange; cz++) {
                                int chunkX = (playerPos.getX() >> 4) + cx;
                                int chunkZ = (playerPos.getZ() >> 4) + cz;
                                net.minecraft.world.level.chunk.LevelChunk chunk = (net.minecraft.world.level.chunk.LevelChunk) level.getChunk(chunkX, chunkZ);
                                if (chunk == null) continue;
                                for (BlockEntity be : chunk.getBlockEntities().values()) {
                                    if (be == null) continue;
                                    String cn = be.getClass().getName();
                                    if (cn.equals("bili.dongsz.broadcastradio.block.entity.RadioBaseStationBlockEntity")) {
                                        found++;
                                        // reflectively read energy, signal range and position
                                        try {
                                            java.lang.reflect.Method getEnergy = be.getClass().getMethod("getEnergy");
                                            java.lang.reflect.Method getSignalRange = be.getClass().getMethod("getSignalRange");
                                            java.lang.reflect.Method getBlockPos = be.getClass().getMethod("getBlockPos");
                                            Object energy = getEnergy.invoke(be);
                                            Object range = getSignalRange.invoke(be);
                                            Object pos = getBlockPos.invoke(be);
                                            mc.player.sendSystemMessage(Component.literal("BaseStation found: class=" + cn + " energy=" + energy + " range=" + range + " pos=" + pos));
                                        } catch (Exception e2) {
                                            mc.player.sendSystemMessage(Component.literal("BaseStation found but reflection failed: " + e2.getMessage()));
                                        }
                                    }
                                }
                            }
                        }
                        mc.player.sendSystemMessage(Component.literal("Nearby BaseStations found=" + found));
                    }
                } catch (Throwable t2) {
                    mc.player.sendSystemMessage(Component.literal("base station scan failed: " + t2.toString()));
                }
                // Report player's terminal/battery/SIM status
                try {
                    boolean hasTerminal = false;
                    int batteryLevel = -1;
                    boolean hasSim = false;
                    for (int i = 0; i < mc.player.getInventory().getContainerSize(); i++) {
                        net.minecraft.world.item.ItemStack stack = mc.player.getInventory().getItem(i);
                        if (stack.getItem() == bili.dongsz.broadcastradio.registry.ModItems.RADIO_TERMINAL.get()) {
                            hasTerminal = true;
                            try {
                                boolean hb = bili.dongsz.broadcastradio.item.RadioTerminalItem.hasBattery(stack);
                                int bl = bili.dongsz.broadcastradio.item.RadioTerminalItem.getBatteryLevel(stack);
                                batteryLevel = bl;
                                net.minecraft.nbt.CompoundTag tag = stack.getOrCreateTag();
                                if (tag.contains("SimCard")) {
                                    net.minecraft.nbt.CompoundTag simTag = tag.getCompound("SimCard");
                                    net.minecraft.world.item.ItemStack sim = net.minecraft.world.item.ItemStack.of(simTag);
                                    hasSim = !sim.isEmpty();
                                }
                            } catch (Throwable ignored) {}
                            break;
                        }
                    }
                    mc.player.sendSystemMessage(Component.literal("player hasTerminal=" + hasTerminal + " batteryLevel=" + batteryLevel + " hasSim=" + hasSim));
                } catch (Throwable t3) {
                    mc.player.sendSystemMessage(Component.literal("player terminal check failed: " + t3.toString()));
                }
            } catch (Throwable t) {
                mc.player.sendSystemMessage(Component.literal("br_debugclient failed: " + t.toString()));
            }
            return 1;
        });
        event.getDispatcher().register((LiteralArgumentBuilder<CommandSourceStack>) root);
    }
}

