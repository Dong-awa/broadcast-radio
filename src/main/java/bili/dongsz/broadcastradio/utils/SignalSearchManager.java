package bili.dongsz.broadcastradio.utils;

import bili.dongsz.broadcastradio.registry.ModItems;
import bili.dongsz.broadcastradio.BroadcastRadio;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Player;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.core.BlockPos;

import java.util.ArrayList;
import java.util.List;
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
    private boolean hasValidSignal = false; // 信号状态：true=有信号，false=无信号
    
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
        
        // 每5秒执行一次信号搜索（100 tick）
        scheduler.scheduleAtFixedRate(() -> {
            if (Minecraft.getInstance().level == null || Minecraft.getInstance().player == null) {
                return;
            }
            
            // 检查玩家是否持有无线电终端
            if (!hasRadioTerminal(Minecraft.getInstance().player)) {
                // 无终端时清空列表，并更新全局标志
                cachedOnlinePlayers.clear();
                hasValidSignal = false;
                BroadcastRadio.HAS_VALID_SERVICE = false; // 依据要求：扫描完成后更新全局变量
                return;
            }
            
            // 执行信号搜索逻辑
            performSignalSearch();
            
        }, 0, 5, TimeUnit.SECONDS); // 初始延迟0秒，每5秒执行一次
    }
    
    /**
     * 停止后台信号搜索任务
     */
    public void stopSignalSearch() {
        if (!isRunning) {
            return;
        }
        
        isRunning = false;
        cachedOnlinePlayers.clear();
        scheduler.shutdown();
    }
    
    /**
     * 立即执行一次信号搜索
     */
    public void forceSignalSearch() {
        if (Minecraft.getInstance().level == null || Minecraft.getInstance().player == null) {
            return;
        }

        // 检查玩家是否持有无线电终端
        if (!hasRadioTerminal(Minecraft.getInstance().player)) {
            // 无终端时清空列表，并更新全局标志
            cachedOnlinePlayers.clear();
            hasValidSignal = false;
            BroadcastRadio.HAS_VALID_SERVICE = false;
            return;
        }

        // 执行信号搜索逻辑
        performSignalSearch();
    }
    
    /**
     * 检查玩家是否持有无线电终端
     */
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
                            }
                        }
                    }
                }
            }
        }
        
        return false;
    }
    
    /**
     * 检查玩家是否满足短信发送的所有条件
     * 1. 玩家当前在线
     * 2. 玩家背包/手持中存在无线电终端
     * 3. 终端电池槽内有电池且电量＞0
     * 4. 终端SIM卡槽内已插入有效SIM卡
     * 5. 该玩家最近一次后台信号扫描，检测到至少1个能量＞0的有效基站（即有信号）
     */
    private boolean isPlayerValidForSMS(Player player) {
        // 玩家当前在线
        
        // 检查玩家是否持有无线电终端
        if (!hasRadioTerminal(player)) {
            return false;
        }
        
        // 检查终端电池槽内有电池且电量＞0
        ItemStack terminalStack = findRadioTerminalInInventory(player);
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
        
        // 检查该玩家是否有有效基站信号
        return true;
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
        
        // 检查发送端玩家是否有有效基站信号
        return hasValidSignal;
    }
    
    /**
     * 在玩家背包中查找无线电终端
     */
    private ItemStack findRadioTerminalInInventory(Player player) {
        // 检查手持物品
        if (player.getMainHandItem().getItem() == bili.dongsz.broadcastradio.registry.ModItems.RADIO_TERMINAL.get()) {
            return player.getMainHandItem();
        }
        if (player.getOffhandItem().getItem() == bili.dongsz.broadcastradio.registry.ModItems.RADIO_TERMINAL.get()) {
            return player.getOffhandItem();
        }
        
        // 检查背包
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
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
        
        net.minecraft.nbt.CompoundTag tag = terminalStack.getOrCreateTag();
        if (tag.contains("SimCard")) {
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
        // 先检测信号状态
        hasValidSignal = checkSignalStatus();
        
        // 检查发送端玩家自身是否满足服务条��
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
}