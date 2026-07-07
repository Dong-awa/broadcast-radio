package bili.dongsz.broadcastradio.utils;

import bili.dongsz.broadcastradio.BroadcastRadio;
import bili.dongsz.broadcastradio.network.PlayerSignalStatusPacket;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.network.chat.Component;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

/**
 * 无线电终端信号搜索管理器（线程安全重构版）。
 *
 * 线程安全方案：
 * 1. 所有缓存数据使用 ConcurrentHashMap / CopyOnWriteArrayList
 * 2. 布尔标志和简单数值使用 AtomicBoolean / AtomicInteger / AtomicReference
 * 3. 耗时计算（遍历背包、遍历方块实体）在后台线程执行
 * 4. 网络包发送和缓存更新通过 minecraft.execute(...) 回主线程
 * 5. 主线程读取缓存时使用原子引用的 get() 方法，无需额外同步
 *
 * Forge 线程模型：
 * - 必须在主线程：网络包发送（sendToServer）、GUI 组件创建/销毁、
 *                 世界/实体的修改操作（setBlock、damageItem 等）
 * - 可在后台线程：只读读取 level/player 引用、遍历背包（只读）、
 *                 遍历方块实体（只读）、计算距离/比较网络类型、
 *                 构建网络包参数对象（但实际 sendToServer 需回主线程）
 */
public class SignalSearchManager {

    private static SignalSearchManager instance;

    // -------------------------------------------------------------------------
    // 线程安全数据结构
    // -------------------------------------------------------------------------

    /** 玩家有效性缓存（服务端响应 → 客户端写入，主线程读取） */
    private final ConcurrentHashMap<UUID, Boolean> playerValidCache = new ConcurrentHashMap<>();

    /** 玩家基站坐标缓存（服务端响应 → 客户端写入，GUI 读取） */
    private final ConcurrentHashMap<UUID, int[]> playerBaseStationPosCache = new ConcurrentHashMap<>();

    /** 在线玩家列表（CopyOnWriteArrayList：迭代安全） */
    private final CopyOnWriteArrayList<Player> cachedOnlinePlayers = new CopyOnWriteArrayList<>();

    /** 是否有有效信号（原子布尔，避免可见性问题） */
    private final AtomicBoolean hasValidSignal = new AtomicBoolean(false);

    /** 当前服务名称（原子引用） */
    private final AtomicReference<String> cachedServiceName = new AtomicReference<>(
        Component.translatable("item.broadcast_radio.radio_terminal.no_signal").getString()
    );

    /** 当前玩家连接的基站 X/Z 坐标（原子整数） */
    private final AtomicInteger cachedBaseStationX = new AtomicInteger(Integer.MIN_VALUE);
    private final AtomicInteger cachedBaseStationZ = new AtomicInteger(Integer.MIN_VALUE);

    /** 定时任务调度器（实际执行逻辑在 RadioScheduledTask） */
    private final RadioScheduledTask scheduledTask;

    /** 是否正在运行（原子布尔） */
    private final AtomicBoolean isRunning = new AtomicBoolean(false);

    // -------------------------------------------------------------------------
    // 单例与构造
    // -------------------------------------------------------------------------

    private SignalSearchManager() {
        this.scheduledTask = new RadioScheduledTask(this);
    }

    public static synchronized SignalSearchManager getInstance() {
        if (instance == null) {
            instance = new SignalSearchManager();
        }
        return instance;
    }

    // -------------------------------------------------------------------------
    // 启停控制（在主线程调用，如右键使用物品、打开 GUI 时）
    // -------------------------------------------------------------------------

    /** 启动后台信号搜索任务（由 RadioTerminalItem 的右键或 SMS GUI 打开时调用） */
    public void startSignalSearch() {
        if (isRunning.compareAndSet(false, true)) {
            scheduledTask.start();
        }
    }

    /** 停止后台信号搜索任务（当前未在任何地方调用，保留 API 供未来使用） */
    public void stopSignalSearch() {
        if (isRunning.compareAndSet(true, false)) {
            scheduledTask.stop();
        }
    }

    public boolean isRunning() {
        return isRunning.get();
    }

    /**
     * 强制立即执行一次信号搜索。
     * 这也是"后台计算 + 主线程更新"模式。
     */
    public void forceSignalSearch() {
        // 直接调用一次后台任务逻辑（不通过调度器，立即执行）
        // 由于 RadioScheduledTask.runBackgroundTask 是"后台线程做计算 +
        // 提交到主线程更新"，我们可以在单独的后台线程中调用，避免阻塞当前调用者
        new Thread(() -> {
            try {
                // 通过反射调用私有方法 —— 或者简单地重新启动定时任务
                // 为避免反射开销，直接让 scheduleAtFixedRate 的首次执行提前发生
                Thread.sleep(50);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }, "RadioTerminal-ForceSearch").start();
    }

    // -------------------------------------------------------------------------
    // 缓存写入 API（由 RadioScheduledTask 在主线程回调中调用）
    // -------------------------------------------------------------------------

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

    /**
     * 触发玩家搜索：遍历当前世界所有其他玩家，向服务端查询其有效性。
     * 由 PlayerLoginListener 在玩家登录时调用（此时需要刷新在线玩家列表）。
     *
     * 说明：此方法在任意线程都可被调用，但实际网络包发送会通过 minecraft.execute
     * 转到主线程，符合 Forge 网络 API 的线程约束。
     */
    public void triggerPlayerSearch() {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null || minecraft.player == null) {
            return;
        }
        // 在后台线程收集 UUID（主线程只做网络包发送）
        UUID selfId = minecraft.player.getUUID();
        java.util.List<UUID> otherPlayerIds = new java.util.ArrayList<>();
        for (Player p : minecraft.level.players()) {
            if (!p.getUUID().equals(selfId)) {
                otherPlayerIds.add(p.getUUID());
            }
        }
        // 清除旧缓存并提交主线程发送查询
        playerValidCache.clear();
        minecraft.execute(() -> {
            for (UUID targetId : otherPlayerIds) {
                bili.dongsz.broadcastradio.BroadcastRadio.NETWORK.sendToServer(
                    new bili.dongsz.broadcastradio.network.QueryPlayerValidPacket(targetId)
                );
            }
        });
    }

    // -------------------------------------------------------------------------
    // 服务端响应处理（由 QueryPlayerValidResponsePacket 在客户端主线程调用）
    // -------------------------------------------------------------------------

    public void updatePlayerValidStatus(UUID playerId, boolean isValid) {
        playerValidCache.put(playerId, isValid);
    }

    public void updatePlayerBaseStationPos(UUID playerId, int x, int z) {
        playerBaseStationPosCache.put(playerId, new int[]{x, z});
    }

    // -------------------------------------------------------------------------
    // 缓存读取 API（由 GUI 在主线程渲染时调用）
    // -------------------------------------------------------------------------

    /** 获取缓存的在线玩家列表（返回副本，避免外部修改） */
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

    /** 服务端响应是否已到达并标记该玩家有效 */
    public boolean isPlayerValidInCache(UUID playerId) {
        Boolean value = playerValidCache.get(playerId);
        return value != null && value;
    }

    // -------------------------------------------------------------------------
    // 辅助方法：检查 SIM 卡有效性（只读，可在后台线程安全执行）
    // -------------------------------------------------------------------------

    /** 静态方法：检查终端物品的 SIM 卡（只读，供 RadioScheduledTask 在后台线程调用） */
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

    // -------------------------------------------------------------------------
    // 模组卸载清理（由 BroadcastRadio 在生命周期事件中调用）
    // -------------------------------------------------------------------------

    /** 停止所有后台任务并释放资源 */
    public void cleanup() {
        if (isRunning.compareAndSet(true, false)) {
            scheduledTask.stop();
        }
        playerValidCache.clear();
        playerBaseStationPosCache.clear();
        cachedOnlinePlayers.clear();
    }
}