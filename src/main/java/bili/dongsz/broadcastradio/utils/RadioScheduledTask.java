package bili.dongsz.broadcastradio.utils;

import bili.dongsz.broadcastradio.BroadcastRadio;
import bili.dongsz.broadcastradio.item.RadioTerminalItem;
import bili.dongsz.broadcastradio.network.PlayerSignalStatusPacket;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraftforge.fml.DistExecutor;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

public class RadioScheduledTask {

    private final SignalSearchManager cacheManager;
    private ScheduledFuture<?> scheduledFuture;
    private final AtomicBoolean running = new AtomicBoolean(false);

    private final AtomicReference<RadioTerminalItem.BaseStationInfo> latestStationInfo = new AtomicReference<>(null);
    private final AtomicBoolean latestHasSignal = new AtomicBoolean(false);
    private final AtomicInteger cachedBaseStationX = new AtomicInteger(Integer.MIN_VALUE);
    private final AtomicInteger cachedBaseStationZ = new AtomicInteger(Integer.MIN_VALUE);
    private final AtomicReference<String> cachedServiceName = new AtomicReference<>(
        net.minecraft.network.chat.Component.translatable("item.broadcast_radio.radio_terminal.no_signal").getString()
    );
    private final AtomicBoolean latestSenderValid = new AtomicBoolean(false);

    public RadioScheduledTask(SignalSearchManager cacheManager) {
        this.cacheManager = cacheManager;
    }

    public void start() {
        if (!running.compareAndSet(false, true)) {
            return;
        }

        scheduledFuture = RadioThreadPoolManager.getInstance().scheduleAtFixedRate(
            this::runBackgroundTask,
            100,
            3000
        );
    }

    public void stop() {
        if (running.compareAndSet(true, false)) {
            if (scheduledFuture != null) {
                RadioThreadPoolManager.getInstance().cancelTask(scheduledFuture);
                scheduledFuture = null;
            }
        }
    }

    public boolean isRunning() {
        return running.get();
    }

    private void runBackgroundTask() {
        DistExecutor.runWhenOn(net.minecraftforge.api.distmarker.Dist.CLIENT, () -> () -> {
            net.minecraft.client.Minecraft minecraft = net.minecraft.client.Minecraft.getInstance();

            Level level = minecraft.level;
            Player player = minecraft.player;

            if (level == null || player == null) {
                return;
            }

            if (!hasRadioTerminalInInventory(player)) {
                submitToMainThread(() -> {
                    cacheManager.clearOnlinePlayers();
                    cacheManager.setHasValidSignal(false);
                    BroadcastRadio.HAS_VALID_SERVICE = false;
                });
                return;
            }

            UUID selfId = player.getUUID();
            List<UUID> otherPlayerIds = new java.util.ArrayList<>();
            for (Player p : level.players()) {
                if (!p.getUUID().equals(selfId)) {
                    otherPlayerIds.add(p.getUUID());
                }
            }

            RadioTerminalItem.BaseStationInfo stationInfo =
                RadioTerminalItem.findClosestBaseStation(level, player);

            String serviceName = stationInfo != null
                ? stationInfo.serviceName
                : net.minecraft.network.chat.Component.translatable("item.broadcast_radio.radio_terminal.no_signal").getString();
            boolean hasSignal = stationInfo != null && stationInfo.pos != null;
            int stationX = stationInfo != null && stationInfo.pos != null ? stationInfo.pos.getX() : Integer.MIN_VALUE;
            int stationZ = stationInfo != null && stationInfo.pos != null ? stationInfo.pos.getZ() : Integer.MIN_VALUE;

            ItemStack terminalStack = findTerminalStack(player);
            boolean hasBattery = terminalStack != null && !terminalStack.isEmpty()
                && RadioTerminalItem.hasBattery(terminalStack);
            int batteryLevel = hasBattery ? RadioTerminalItem.getBatteryLevel(terminalStack) : 0;
            boolean hasValidSIM = terminalStack != null && !terminalStack.isEmpty()
                && SignalSearchManager.checkSIMCardValid(terminalStack);
            boolean senderValid = hasBattery && batteryLevel > 0 && hasValidSIM && hasSignal;

            List<UUID> onlinePlayerCandidateIds = new java.util.ArrayList<>();
            if (senderValid) {
                onlinePlayerCandidateIds.addAll(otherPlayerIds);
            }

            submitToMainThread(() -> {
                latestStationInfo.set(stationInfo);
                latestHasSignal.set(hasSignal);
                cachedServiceName.set(serviceName);
                if (stationX != Integer.MIN_VALUE && stationZ != Integer.MIN_VALUE) {
                    cachedBaseStationX.set(stationX);
                    cachedBaseStationZ.set(stationZ);
                    cacheManager.updateCachedBaseStationPos(stationX, stationZ);
                } else {
                    cacheManager.clearCachedBaseStationPos();
                }
                cacheManager.setHasValidSignal(hasSignal);
                cacheManager.updateCachedServiceName(serviceName);
                latestSenderValid.set(senderValid);

                if (player != null) {
                    BroadcastRadio.NETWORK.sendToServer(new PlayerSignalStatusPacket(
                        player.getUUID(),
                        hasSignal
                    ));
                }

                cacheManager.clearPlayerValidCache();
                for (UUID targetId : onlinePlayerCandidateIds) {
                    BroadcastRadio.NETWORK.sendToServer(
                        new bili.dongsz.broadcastradio.network.QueryPlayerValidPacket(targetId)
                    );
                }

                BroadcastRadio.HAS_VALID_SERVICE = senderValid;

                if (senderValid) {
                    List<Player> newOnlinePlayers = new java.util.ArrayList<>();
                    if (minecraft.level != null) {
                        for (Player p : minecraft.level.players()) {
                            if (!p.getUUID().equals(selfId) && cacheManager.isPlayerValidInCache(p.getUUID())) {
                                newOnlinePlayers.add(p);
                            }
                        }
                    }
                    cacheManager.setCachedOnlinePlayers(newOnlinePlayers);
                } else {
                    cacheManager.clearOnlinePlayers();
                }
            });
        });
    }

    private void submitToMainThread(Runnable task) {
        DistExecutor.runWhenOn(net.minecraftforge.api.distmarker.Dist.CLIENT, () -> () -> {
            net.minecraft.client.Minecraft minecraft = net.minecraft.client.Minecraft.getInstance();
            minecraft.execute(task);
        });
    }

    private static boolean hasRadioTerminalInInventory(Player player) {
        if (player.getMainHandItem().getItem() == bili.dongsz.broadcastradio.registry.ModItems.RADIO_TERMINAL.get() ||
            player.getOffhandItem().getItem() == bili.dongsz.broadcastradio.registry.ModItems.RADIO_TERMINAL.get()) {
            return true;
        }
        return player.getInventory().hasAnyMatching(itemStack ->
            itemStack.getItem() == bili.dongsz.broadcastradio.registry.ModItems.RADIO_TERMINAL.get()
        );
    }

    private static ItemStack findTerminalStack(Player player) {
        if (player.getMainHandItem().getItem() == bili.dongsz.broadcastradio.registry.ModItems.RADIO_TERMINAL.get()) {
            return player.getMainHandItem();
        }
        if (player.getOffhandItem().getItem() == bili.dongsz.broadcastradio.registry.ModItems.RADIO_TERMINAL.get()) {
            return player.getOffhandItem();
        }
        for (int i = 0; i < 41; i++) {
            ItemStack stack = player.getInventory().getItem(i);
            if (stack.getItem() == bili.dongsz.broadcastradio.registry.ModItems.RADIO_TERMINAL.get()) {
                return stack;
            }
        }
        return ItemStack.EMPTY;
    }

    public int getCachedBaseStationX() {
        return cachedBaseStationX.get();
    }

    public int getCachedBaseStationZ() {
        return cachedBaseStationZ.get();
    }

    public String getCachedServiceName() {
        return cachedServiceName.get();
    }

    public boolean hasValidSignal() {
        return latestHasSignal.get();
    }

    public boolean isSenderValid() {
        return latestSenderValid.get();
    }
}