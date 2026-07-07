package bili.dongsz.broadcastradio.utils;

import bili.dongsz.broadcastradio.BroadcastRadio;
import bili.dongsz.broadcastradio.item.RadioTerminalItem;
import bili.dongsz.broadcastradio.network.PlayerSignalStatusPacket;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

/**
 * 无线电终端定时信号搜索任务。
 *
 * 线程安全原则：
 * 1. **后台线程**：只做纯数据计算（遍历物品、遍历方块实体、计算距离、构建数据包参数）
 * 2. **主线程**：只做网络包发送（BroadcastRadio.NETWORK.sendToServer）和缓存写入
 * 3. 后台线程绝不调用 Minecraft.getInstance().execute(...) 以外的任何修改操作
 * 4. 后台线程可安全读取 Minecraft.getInstance().level 和 Minecraft.getInstance().player，
 *    但必须在读取前检查 null，并通过原子引用或线程安全集合传递结果到主线程
 */
public class RadioScheduledTask {

    private final SignalSearchManager cacheManager;

    /** 当前任务引用，由 RadioThreadPoolManager 调度 */
    private ScheduledFuture<?> scheduledFuture;

    /** 是否正在运行（AtomicBoolean 保证线程安全） */
    private final AtomicBoolean running = new AtomicBoolean(false);

    // ---------- 原子引用：后台线程计算，主线程读取 ----------
    /** 最近一次基站查找结果 */
    private final AtomicReference<RadioTerminalItem.BaseStationInfo> latestStationInfo = new AtomicReference<>(null);
    /** 最近一次扫描时的信号状态（是否有有效基站服务） */
    private final AtomicBoolean latestHasSignal = new AtomicBoolean(false);
    /** 最近一次扫描时缓存的基站 X 坐标 */
    private final AtomicInteger cachedBaseStationX = new AtomicInteger(Integer.MIN_VALUE);
    /** 最近一次扫描时缓存的基站 Z 坐标 */
    private final AtomicInteger cachedBaseStationZ = new AtomicInteger(Integer.MIN_VALUE);
    /** 最近一次扫描时的服务名称 */
    private final AtomicReference<String> cachedServiceName = new AtomicReference<>(
        net.minecraft.network.chat.Component.translatable("item.broadcast_radio.radio_terminal.no_signal").getString()
    );

    /** 最近一次扫描时，当前玩家的发送端有效性（综合检查：终端+电池+SIM+信号） */
    private final AtomicBoolean latestSenderValid = new AtomicBoolean(false);

    public RadioScheduledTask(SignalSearchManager cacheManager) {
        this.cacheManager = cacheManager;
    }

    /**
     * 启动定时任务：每 3 秒执行一次信号搜索。
     * 初始延迟 100ms 避免启动时与渲染线程冲突。
     */
    public void start() {
        if (!running.compareAndSet(false, true)) {
            return; // 已在运行
        }

        scheduledFuture = RadioThreadPoolManager.getInstance().scheduleAtFixedRate(
            this::runBackgroundTask,
            100,
            3000
        );
    }

    /** 停止单个任务（保留线程池） */
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

    /**
     * 后台线程的入口。
     *
     * 注意：这里是后台线程，绝对不能：
     *   - 调用 Minecraft.getInstance().execute(...) 以外的任何修改操作
     *   - 直接修改 Player / World / BlockEntity 的状态
     *   - 调用可能触发区块加载的方法
     *
     * 可以安全地：
     *   - 读取 level 和 player 引用（读取前判空）
     *   - 遍历物品、检查 ItemStack（只读）
     *   - 读取方块实体的只读字段（如坐标、能量值——注意这些字段可能在主线程同时写入）
     *   - 计算距离、比较网络类型等纯计算操作
     */
    private void runBackgroundTask() {
        Minecraft minecraft = Minecraft.getInstance();

        // 1. 在后台线程读取 level 和 player 引用（这是安全的，因为这些引用在客户端始终存在）
        Level level = minecraft.level;
        Player player = minecraft.player;

        if (level == null || player == null) {
            return;
        }

        // 2. 任务1：遍历背包检查是否持有终端（后台线程安全）
        //    注意：hasRadioTerminal 只做读取，不修改物品
        if (!hasRadioTerminalInInventory(player)) {
            // 无终端：通知主线程清空缓存和标志
            submitToMainThread(() -> {
                cacheManager.clearOnlinePlayers();
                cacheManager.setHasValidSignal(false);
                BroadcastRadio.HAS_VALID_SERVICE = false;
            });
            return;
        }

        // 3. 任务2：遍历世界玩家，准备向服务端查询有效性（遍历在后台线程，发送在主线程）
        //    先在后台线程收集玩家 UUID（避免在主线程做集合遍历）
        UUID selfId = player.getUUID();
        List<UUID> otherPlayerIds = new java.util.ArrayList<>();
        for (Player p : level.players()) {
            if (!p.getUUID().equals(selfId)) {
                otherPlayerIds.add(p.getUUID());
            }
        }

        // 4. 任务3：查找最近基站（这是最耗时的操作，整个放在后台线程）
        //    findClosestBaseStation 只做只读操作：遍历方块实体、比较距离
        RadioTerminalItem.BaseStationInfo stationInfo =
            RadioTerminalItem.findClosestBaseStation(level, player);

        // 5. 在后台线程计算结果（纯计算，不涉及 Minecraft 对象修改）
        String serviceName = stationInfo != null
            ? stationInfo.serviceName
            : net.minecraft.network.chat.Component.translatable("item.broadcast_radio.radio_terminal.no_signal").getString();
        boolean hasSignal = stationInfo != null && stationInfo.pos != null;
        int stationX = stationInfo != null && stationInfo.pos != null ? stationInfo.pos.getX() : Integer.MIN_VALUE;
        int stationZ = stationInfo != null && stationInfo.pos != null ? stationInfo.pos.getZ() : Integer.MIN_VALUE;

        // 6. 任务4：检查发送端有效性（综合：终端+电池+SIM+信号）
        //    这涉及读取终端物品的 NBT 标签（只读），在后台线程做
        ItemStack terminalStack = findTerminalStack(player);
        boolean hasBattery = terminalStack != null && !terminalStack.isEmpty()
            && RadioTerminalItem.hasBattery(terminalStack);
        int batteryLevel = hasBattery ? RadioTerminalItem.getBatteryLevel(terminalStack) : 0;
        boolean hasValidSIM = terminalStack != null && !terminalStack.isEmpty()
            && SignalSearchManager.checkSIMCardValid(terminalStack);
        boolean senderValid = hasBattery && batteryLevel > 0 && hasValidSIM && hasSignal;

        // 7. 在后台线程构建"在线玩家"候选列表（只是 UUID 列表，真正的有效性由服务端判断）
        //    注意：我们不在这里判断玩家是否有效，只把 UUID 列表交给主线程，让服务端响应来更新
        List<UUID> onlinePlayerCandidateIds = new java.util.ArrayList<>();
        if (senderValid) {
            onlinePlayerCandidateIds.addAll(otherPlayerIds);
        }

        // 8. 提交到主线程：更新缓存 + 发送网络包
        //    网络包发送（sendToServer）必须在主线程执行
        submitToMainThread(() -> {
            // 更新缓存（线程安全的写入）
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

            // 发送网络包：自身信号状态（必须在主线程）
            if (player != null) {
                BroadcastRadio.NETWORK.sendToServer(new PlayerSignalStatusPacket(
                    player.getUUID(),
                    hasSignal
                ));
            }

            // 发送网络包：查询其他玩家有效性（必须在主线程，逐个发送）
            cacheManager.clearPlayerValidCache();
            for (UUID targetId : onlinePlayerCandidateIds) {
                BroadcastRadio.NETWORK.sendToServer(
                    new bili.dongsz.broadcastradio.network.QueryPlayerValidPacket(targetId)
                );
            }

            // 更新全局标志
            BroadcastRadio.HAS_VALID_SERVICE = senderValid;

            // 构建在线玩家列表（基于服务端返回的有效性缓存）—— 在主线程迭代 level.players() 是安全的
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
    }

    /**
     * 将 Runnable 提交到 Minecraft 主线程队列。
     * Minecraft.getInstance().execute(...) 是 Forge 推荐的回主线程方式。
     */
    private void submitToMainThread(Runnable task) {
        Minecraft minecraft = Minecraft.getInstance();
        minecraft.execute(task);
    }

    // -------------------------------------------------------------------------
    // 以下方法在后台线程安全执行（只读操作）
    // -------------------------------------------------------------------------

    /** 检查玩家是否持有终端物品（在后台线程遍历背包） */
    private static boolean hasRadioTerminalInInventory(Player player) {
        if (player.getMainHandItem().getItem() == bili.dongsz.broadcastradio.registry.ModItems.RADIO_TERMINAL.get() ||
            player.getOffhandItem().getItem() == bili.dongsz.broadcastradio.registry.ModItems.RADIO_TERMINAL.get()) {
            return true;
        }
        return player.getInventory().hasAnyMatching(itemStack ->
            itemStack.getItem() == bili.dongsz.broadcastradio.registry.ModItems.RADIO_TERMINAL.get()
        );
    }

    /** 从玩家背包中查找终端物品（在后台线程执行，只读） */
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

    // 原子引用访问器（供主线程读取）
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