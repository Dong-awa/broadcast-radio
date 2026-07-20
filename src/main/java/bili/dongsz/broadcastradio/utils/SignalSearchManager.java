package bili.dongsz.broadcastradio.utils;

import bili.dongsz.broadcastradio.BroadcastRadio;
import bili.dongsz.broadcastradio.network.PlayerSignalStatusPacket;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.network.chat.Component;
import net.minecraftforge.fml.DistExecutor;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

public class SignalSearchManager {

    private static SignalSearchManager instance;

    private final ConcurrentHashMap<UUID, Boolean> playerValidCache = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<UUID, int[]> playerBaseStationPosCache = new ConcurrentHashMap<>();
    private final CopyOnWriteArrayList<Player> cachedOnlinePlayers = new CopyOnWriteArrayList<>();
    private final AtomicBoolean hasValidSignal = new AtomicBoolean(false);
    private final AtomicReference<String> cachedServiceName = new AtomicReference<>(
        Component.translatable("item.broadcast_radio.radio_terminal.no_signal").getString()
    );
    private final AtomicInteger cachedBaseStationX = new AtomicInteger(Integer.MIN_VALUE);
    private final AtomicInteger cachedBaseStationZ = new AtomicInteger(Integer.MIN_VALUE);
    private final RadioScheduledTask scheduledTask;
    private final AtomicBoolean isRunning = new AtomicBoolean(false);

    private SignalSearchManager() {
        this.scheduledTask = new RadioScheduledTask(this);
    }

    public static synchronized SignalSearchManager getInstance() {
        if (instance == null) {
            instance = new SignalSearchManager();
        }
        return instance;
    }

    public void startSignalSearch() {
        if (isRunning.compareAndSet(false, true)) {
            scheduledTask.start();
        }
    }

    public void stopSignalSearch() {
        if (isRunning.compareAndSet(true, false)) {
            scheduledTask.stop();
        }
    }

    public boolean isRunning() {
        return isRunning.get();
    }

    public void forceSignalSearch() {
        new Thread(() -> {
            try {
                Thread.sleep(50);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }, "RadioTerminal-ForceSearch").start();
    }

    public void setHasValidSignal(boolean value) {
        hasValidSignal.set(value);
    }

    public void updateCachedServiceName(String serviceName) {
        cachedServiceName.set(serviceName);
        hasValidSignal.set(!serviceName.equals(
            Component.translatable("item.broadcast_radio.radio_terminal.no_signal").getString()
        ));
    }

    public void updateCachedBaseStationPos(int x, int z) {
        cachedBaseStationX.set(x);
        cachedBaseStationZ.set(z);
    }

    public void clearCachedBaseStationPos() {
        cachedBaseStationX.set(Integer.MIN_VALUE);
        cachedBaseStationZ.set(Integer.MIN_VALUE);
    }

    public void setCachedOnlinePlayers(List<Player> players) {
        cachedOnlinePlayers.clear();
        if (players != null) {
            cachedOnlinePlayers.addAll(players);
        }
    }

    public void clearOnlinePlayers() {
        cachedOnlinePlayers.clear();
    }

    public void clearPlayerValidCache() {
        playerValidCache.clear();
    }

    public void triggerPlayerSearch() {
        DistExecutor.runWhenOn(net.minecraftforge.api.distmarker.Dist.CLIENT, () -> () -> {
            net.minecraft.client.Minecraft minecraft = net.minecraft.client.Minecraft.getInstance();
            if (minecraft.level == null || minecraft.player == null) {
                return;
            }
            UUID selfId = minecraft.player.getUUID();
            java.util.List<UUID> otherPlayerIds = new java.util.ArrayList<>();
            for (Player p : minecraft.level.players()) {
                if (!p.getUUID().equals(selfId)) {
                    otherPlayerIds.add(p.getUUID());
                }
            }
            playerValidCache.clear();
            minecraft.execute(() -> {
                for (UUID targetId : otherPlayerIds) {
                    BroadcastRadio.NETWORK.sendToServer(
                        new bili.dongsz.broadcastradio.network.QueryPlayerValidPacket(targetId)
                    );
                }
            });
        });
    }

    public void updatePlayerValidStatus(UUID playerId, boolean isValid) {
        playerValidCache.put(playerId, isValid);
    }

    public void updatePlayerBaseStationPos(UUID playerId, int x, int z) {
        playerBaseStationPosCache.put(playerId, new int[]{x, z});
    }

    public List<Player> getCachedOnlinePlayers() {
        return new java.util.ArrayList<>(cachedOnlinePlayers);
    }

    public boolean hasAvailablePlayers() {
        return !cachedOnlinePlayers.isEmpty();
    }

    public boolean hasValidSignal() {
        return hasValidSignal.get();
    }

    public String getCachedServiceName() {
        return cachedServiceName.get();
    }

    public int getCachedBaseStationX() {
        return cachedBaseStationX.get();
    }

    public int getCachedBaseStationZ() {
        return cachedBaseStationZ.get();
    }

    public int[] getPlayerBaseStationPos(UUID playerId) {
        return playerBaseStationPosCache.get(playerId);
    }

    public boolean isPlayerValidInCache(UUID playerId) {
        Boolean value = playerValidCache.get(playerId);
        return value != null && value;
    }

    public static boolean checkSIMCardValid(ItemStack terminalStack) {
        if (terminalStack == null || terminalStack.isEmpty()) {
            return false;
        }
        net.minecraft.nbt.CompoundTag tag = terminalStack.getTag();
        if (tag != null && tag.contains("SimCard")) {
            net.minecraft.nbt.CompoundTag simCardTag = tag.getCompound("SimCard");
            net.minecraft.world.item.ItemStack simCard = net.minecraft.world.item.ItemStack.of(simCardTag);
            return !simCard.isEmpty();
        }
        return false;
    }

    public void cleanup() {
        if (isRunning.compareAndSet(true, false)) {
            scheduledTask.stop();
        }
        playerValidCache.clear();
        playerBaseStationPosCache.clear();
        cachedOnlinePlayers.clear();
    }
}
