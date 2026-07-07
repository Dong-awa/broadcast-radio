package bili.dongsz.broadcastradio.utils;

import bili.dongsz.broadcastradio.registry.ModItems;
import bili.dongsz.broadcastradio.BroadcastRadio;
import bili.dongsz.broadcastradio.network.PlayerSignalStatusPacket;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Player;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.core.BlockPos;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * 无线电终端后台定时信号搜索管理器
 * 在GUI关闭后依然持续运行，每5秒执行一次信号搜索
 */
public class SignalSearchManager {
    private static SignalSearchManager instance;
    private final ScheduledExecutorService scheduler;
    private boolean isRunning = false;
    private List<Player> cachedOnlinePlayers = new ArrayList<>();
    private boolean hasValidSignal = false;
    private String cachedServiceName = Component.translatable("item.broadcast_radio.radio_terminal.no_signal").getString();
    private final Map<UUID, Boolean> playerValidCache = new HashMap<>();
    private final Map<UUID, int[]> playerBaseStationPosCache = new HashMap<>();
    private int cachedBaseStationX = Integer.MIN_VALUE;
    private int cachedBaseStationZ = Integer.MIN_VALUE;
    
    private SignalSearchManager() {
        this.scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread thread = new Thread(r, "RadioTerminal-SignalSearch");
            thread.setDaemon(true);
            return thread;
        });
    }
    
    public static SignalSearchManager getInstance() {
        if (instance == null) {
            instance = new SignalSearchManager();
        }
        return instance;
    }
    
    /**
     * 启动后台信号搜索任务
     */
    public void startSignalSearch() {
        if (isRunning) {
            return;
        }
        
        isRunning = true;
        
        // 每3秒执行一次信号搜索（60 tick）——同时以此频率更新全局 HAS_VALID_SERVICE
        // 初始延迟100ms避免启动时卡顿
        scheduler.scheduleAtFixedRate(() -> {
            // 所有对 Minecraft 对象的访问必须在主线程上执行，否则会导致实体状态被破坏
            Minecraft minecraft = Minecraft.getInstance();
            minecraft.execute(() -> {
                if (minecraft.level == null || minecraft.player == null) {
                    return;
                }

                // 检查玩家是否持有无线电终端
                if (!hasRadioTerminal(minecraft.player)) {
                    // 无终端时清空列表，并更新全局标志
                    cachedOnlinePlayers.clear();
                    hasValidSignal = false;
                    BroadcastRadio.HAS_VALID_SERVICE = false;
                    return;
                }

                // 先清空缓存并查询其他玩家的有效性状态（在主线程执行）
                playerValidCache.clear();
                for (Player player : minecraft.level.players()) {
                    if (!player.getUUID().equals(minecraft.player.getUUID())) {
                        BroadcastRadio.NETWORK.sendToServer(new bili.dongsz.broadcastradio.network.QueryPlayerValidPacket(player.getUUID()));
                    }
                }

                // 执行信号搜索逻辑（在主线程执行，避免破坏玩家/世界状态）
                performSignalSearch();
            });
        }, 100, 3000, TimeUnit.MILLISECONDS); // 初始延迟100ms，每3秒执行一次
    }

    public void stopSignalSearch() {
        if (!isRunning) {
            return;
        }
        
        isRunning = false;
        cachedOnlinePlayers.clear();
        scheduler.shutdown();
    }

    public void forceSignalSearch() {
        // 所有对 Minecraft 对象的访问必须在主线程上执行
        Minecraft minecraft = Minecraft.getInstance();
        minecraft.execute(() -> {
            if (minecraft.level == null || minecraft.player == null) {
                return;
            }

            // 检查玩家是否持有无线电终端
            if (!hasRadioTerminal(minecraft.player)) {
                cachedOnlinePlayers.clear();
                hasValidSignal = false;
                BroadcastRadio.HAS_VALID_SERVICE = false;
                return;
            }
            performSignalSearch();
        });
    }

    private boolean hasRadioTerminal(Player player) {
        // 检查手持物品
        if (player.getMainHandItem().getItem() == bili.dongsz.broadcastradio.registry.ModItems.RADIO_TERMINAL.get() ||
            player.getOffhandItem().getItem() == bili.dongsz.broadcastradio.registry.ModItems.RADIO_TERMINAL.get()) {
            return true;
        }
        
        // 检查背包中是否有终端
        return player.getInventory().hasAnyMatching(itemStack -> 
            itemStack.getItem() == bili.dongsz.broadcastradio.registry.ModItems.RADIO_TERMINAL.get()
        );
    }
    private boolean checkSignalStatus() {
        if (Minecraft.getInstance().level == null || Minecraft.getInstance().player == null) {
            return false;
        }
        
        BlockPos playerPos = Minecraft.getInstance().player.blockPosition();
        Level level = Minecraft.getInstance().level;
        int chunkRange = 4;
        
        for (int cx = -chunkRange; cx <= chunkRange; cx++) {
            for (int cz = -chunkRange; cz <= chunkRange; cz++) {
                int chunkX = (playerPos.getX() >> 4) + cx;
                int chunkZ = (playerPos.getZ() >> 4) + cz;
                ChunkAccess chunk = level.getChunk(chunkX, chunkZ);
                if (chunk == null) {
                    continue;
                }
                
                // 遍历区块中方块实体
                for (BlockEntity be : ((net.minecraft.world.level.chunk.LevelChunk) chunk).getBlockEntities().values()) {
                    if (be instanceof bili.dongsz.broadcastradio.block.entity.RadioBaseStationBlockEntity station) {
                        // 检查基站能量是否大于0
                        if (station.getEnergy() > 0) {
                            BlockPos stationPos = station.getBlockPos();
                            int distance = Math.abs(playerPos.getX() - stationPos.getX()) + 
                                         Math.abs(playerPos.getZ() - stationPos.getZ());
                            int signalRange = station.getSignalRange();
                            if (distance <= signalRange) {
                                return true;
                            }
                        }
                    }
                }
            }
        }
        
        return false;
    }

    private boolean isPlayerValidForSMS(Player player) {
        // 使用服务端检查的玩家有效性状态
        Boolean isValid = playerValidCache.get(player.getUUID());
        if (isValid != null) {
            return isValid;
        } else {
            // 服务端数据还没回来，暂时返回false（等待服务端响应）
            return false;
        }
    }
    
    /**
     * 备用信号检查方法（使用本地搜索）
     */
    private boolean isPositionHasValidSignalFallback(Player player) {
        if (Minecraft.getInstance().level == null) {
            return false;
        }
        
        String serviceName = bili.dongsz.broadcastradio.item.RadioTerminalItem.getCurrentServiceName(
            Minecraft.getInstance().level,
            player
        );
        
        return !serviceName.equals(Component.translatable("item.broadcast_radio.radio_terminal.no_signal").getString());
    }
    
    /**
     * 更新玩家有效性状态（从服务端响应）
     */
    public void updatePlayerValidStatus(UUID playerId, boolean isValid) {
        playerValidCache.put(playerId, isValid);
    }
    
    /**
     * 更新缓存的服务名称
     */
    public void updateCachedServiceName(String serviceName) {
        this.cachedServiceName = serviceName;
        this.hasValidSignal = !serviceName.equals(Component.translatable("item.broadcast_radio.radio_terminal.no_signal").getString());
        BroadcastRadio.HAS_VALID_SERVICE = this.hasValidSignal;
    }
    
    /**
     * 触发玩家搜索（查询其他玩家的有效性）
     */
    public void triggerPlayerSearch() {
        if (Minecraft.getInstance().level != null && Minecraft.getInstance().player != null) {
            playerValidCache.clear();
            for (Player player : Minecraft.getInstance().level.players()) {
                if (!player.getUUID().equals(Minecraft.getInstance().player.getUUID())) {
                    BroadcastRadio.NETWORK.sendToServer(new bili.dongsz.broadcastradio.network.QueryPlayerValidPacket(player.getUUID()));
                }
            }
        }
    }
    
    /**
     * 检查发送端玩家自身是否满足服务条件
     * 如果不满足，则返回空列表
     */
    private boolean isSenderValidForSMS() {
        if (Minecraft.getInstance().player == null) {
            return false;
        }
        Player sender = Minecraft.getInstance().player;

        // 检查发送端玩家是否持有无线电终端
        if (!hasRadioTerminal(sender)) {
            return false;
        }

        // 检查终端电池槽内有电池且电量＞0
        ItemStack terminalStack = findRadioTerminalInInventory(sender);
        if (terminalStack.isEmpty()) {
            return false;
        }

        if (!bili.dongsz.broadcastradio.item.RadioTerminalItem.hasBattery(terminalStack)) {
            return false;
        }

        int batteryLevel = bili.dongsz.broadcastradio.item.RadioTerminalItem.getBatteryLevel(terminalStack);
        if (batteryLevel <= 0) {
            return false;
        }

        // 检查终端SIM卡槽内已插入有效SIM卡
        if (!hasValidSIMCard(terminalStack)) {
            return false;
        }

        // 发送端需要同时有有效基站信号
        return hasValidSignal;
    }
    private ItemStack findRadioTerminalInInventory(Player player) {
        // 检查手持物品
        if (player.getMainHandItem().getItem() == bili.dongsz.broadcastradio.registry.ModItems.RADIO_TERMINAL.get()) {
            return player.getMainHandItem();
        }
        if (player.getOffhandItem().getItem() == bili.dongsz.broadcastradio.registry.ModItems.RADIO_TERMINAL.get()) {
            return player.getOffhandItem();
        }
        
        // 检查所有槽位（主背包 0-35 + 装备栏 36-40）
        // 确保与 hasAnyMatching 检查的范围一致
        for (int i = 0; i < 41; i++) {
            ItemStack stack = player.getInventory().getItem(i);
            if (stack.getItem() == bili.dongsz.broadcastradio.registry.ModItems.RADIO_TERMINAL.get()) {
                return stack;
            }
        }
        
        return ItemStack.EMPTY;
    }
    
    /**
     * 检查终端是否有有效的SIM卡
     */
    private boolean hasValidSIMCard(ItemStack terminalStack) {
        if (terminalStack.isEmpty()) {
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
    
    /**
     * 执行信号搜索逻辑
     */
    private void performSignalSearch() {
        String serviceName = Component.translatable("item.broadcast_radio.radio_terminal.no_signal").getString();
        net.minecraft.core.BlockPos baseStationPos = null;
        if (Minecraft.getInstance().player != null && Minecraft.getInstance().level != null) {
            bili.dongsz.broadcastradio.item.RadioTerminalItem.BaseStationInfo info =
                bili.dongsz.broadcastradio.item.RadioTerminalItem.getCurrentBaseStationInfo(
                    Minecraft.getInstance().level,
                    Minecraft.getInstance().player
                );
            serviceName = info.serviceName;
            baseStationPos = info.pos;
        }
        
        this.cachedServiceName = serviceName;
        
        if (baseStationPos != null) {
            updateCachedBaseStationPos(baseStationPos.getX(), baseStationPos.getZ());
        } else {
            clearCachedBaseStationPos();
        }
        
        hasValidSignal = !serviceName.equals(Component.translatable("item.broadcast_radio.radio_terminal.no_signal").getString());
        
        // 发送自己的信号状态到服务端
        if (Minecraft.getInstance().player != null) {
            BroadcastRadio.NETWORK.sendToServer(new PlayerSignalStatusPacket(
                Minecraft.getInstance().player.getUUID(),
                hasValidSignal
            ));
        }
        
        // 检查发送端玩家自身是否满足服务条件
        boolean isSenderValid = isSenderValidForSMS();

        if (!isSenderValid) {
            // 发送端无服务，清空玩家列表
            cachedOnlinePlayers.clear();
        } else {
            // 有信号且发送端满足条件时执行正常搜索逻辑
            List<Player> newOnlinePlayers = new ArrayList<>();
            
            if (Minecraft.getInstance().level != null) {
                for (Player player : Minecraft.getInstance().level.players()) {
                    if (!player.getUUID().equals(Minecraft.getInstance().player.getUUID()) && 
                        isPlayerValidForSMS(player)) {
                        newOnlinePlayers.add(player);
                    }
                }
            }
            
            // 更新缓存
            cachedOnlinePlayers = newOnlinePlayers;
        }
        
        // 更新全局客户端变量：扫描完成后根据发送端自身检查结果设置
        BroadcastRadio.HAS_VALID_SERVICE = isSenderValid;

        // 静默运行，不向用户展示任何扫描过程、结果或状态
    }
    
    /**
     * 获取缓存的在线玩家列表
     */
    public List<Player> getCachedOnlinePlayers() {
        return new ArrayList<>(cachedOnlinePlayers);
    }
    
    /**
     * 检查是否有可用的在线玩家
     */
    public boolean hasAvailablePlayers() {
        return !cachedOnlinePlayers.isEmpty();
    }
    
    /**
     * 检查当前是否有有效信号
     */
    public boolean hasValidSignal() {
        return hasValidSignal;
    }
    
    /**
     * 检查后台任务是否正在运行
     */
    public boolean isRunning() {
        return isRunning;
    }
    
    /**
     * 获取缓存的基站服务名称
     */
    public String getCachedServiceName() {
        return cachedServiceName;
    }
    
    /**
     * 更新指定玩家的基站坐标缓存
     */
    public void updatePlayerBaseStationPos(UUID playerId, int x, int z) {
        playerBaseStationPosCache.put(playerId, new int[]{x, z});
    }
    
    /**
     * 获取指定玩家的基站坐标
     */
    public int[] getPlayerBaseStationPos(UUID playerId) {
        return playerBaseStationPosCache.get(playerId);
    }
    
    /**
     * 更新当前玩家连接的基站坐标缓存
     */
    public void updateCachedBaseStationPos(int x, int z) {
        this.cachedBaseStationX = x;
        this.cachedBaseStationZ = z;
    }
    
    /**
     * 获取当前玩家连接的基站X坐标
     */
    public int getCachedBaseStationX() {
        return cachedBaseStationX;
    }
    
    /**
     * 获取当前玩家连接的基站Z坐标
     */
    public int getCachedBaseStationZ() {
        return cachedBaseStationZ;
    }
    
    /**
     * 清除当前玩家基站坐标缓存（当无信号时）
     */
    public void clearCachedBaseStationPos() {
        this.cachedBaseStationX = Integer.MIN_VALUE;
        this.cachedBaseStationZ = Integer.MIN_VALUE;
    }
}