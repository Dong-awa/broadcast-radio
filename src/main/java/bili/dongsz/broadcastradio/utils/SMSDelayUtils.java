package bili.dongsz.broadcastradio.utils;

import bili.dongsz.broadcastradio.block.entity.RadioBaseStationBlockEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;

/**
 * SMS延迟工具类 - 处理基于网络频段的延迟效果
 */
public class SMSDelayUtils {
    
    /**
     * 获取玩家当前连接基站的网络频段
     */
    public static RadioBaseStationBlockEntity.NetworkType getPlayerCurrentNetworkType() {
        if (Minecraft.getInstance().player == null || Minecraft.getInstance().level == null) {
            return RadioBaseStationBlockEntity.NetworkType.FOUR_G; // 默认4G
        }
        
        Level level = Minecraft.getInstance().level;
        BlockPos playerPos = Minecraft.getInstance().player.blockPosition();
        
        // 在玩家周围查找无线电基站
        for (int dx = -16; dx <= 16; dx++) {
            for (int dy = -8; dy <= 8; dy++) {
                for (int dz = -16; dz <= 16; dz++) {
                    BlockPos checkPos = playerPos.offset(dx, dy, dz);
                    BlockEntity blockEntity = level.getBlockEntity(checkPos);
                    
                    if (blockEntity instanceof RadioBaseStationBlockEntity station) {
                        // 检查玩家是否在基站范围内
                        double distance = Minecraft.getInstance().player.distanceToSqr(
                            checkPos.getX() + 0.5, checkPos.getY() + 0.5, checkPos.getZ() + 0.5
                        );
                        
                        if (distance <= station.getSignalRange() * station.getSignalRange()) {
                            return station.getNetworkType();
                        }
                    }
                }
            }
        }
        
        return RadioBaseStationBlockEntity.NetworkType.FOUR_G; // 默认4G
    }
    
    /**
     * 根据网络频段获取延迟时间（毫秒）
     */
    public static int getNetworkDelay(RadioBaseStationBlockEntity.NetworkType networkType) {
        switch (networkType) {
            case TWO_G:
                return 2500; // 2G延迟2500ms
            case THREE_G:
                return 1500; // 3G延迟1500ms
            case FOUR_G:
            default:
                return 500;  // 4G延迟500ms
        }
    }
    
    /**
     * 获取玩家当前网络延迟（毫秒）
     */
    public static int getPlayerCurrentNetworkDelay() {
        RadioBaseStationBlockEntity.NetworkType networkType = getPlayerCurrentNetworkType();
        return getNetworkDelay(networkType);
    }
}