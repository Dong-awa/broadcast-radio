package bili.dongsz.broadcastradio.utils;

import bili.dongsz.broadcastradio.block.entity.RadioBaseStationBlockEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;

public class SMSDelayUtils {
    public static RadioBaseStationBlockEntity.NetworkType getPlayerCurrentNetworkType() {
        if (Minecraft.getInstance().player == null || Minecraft.getInstance().level == null) {
            return RadioBaseStationBlockEntity.NetworkType.FOUR_G; // 默认4G
        }
        
        Level level = Minecraft.getInstance().level;
        BlockPos playerPos = Minecraft.getInstance().player.blockPosition();
        
        // 查找附近无线电基站
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
    public static int getPlayerCurrentNetworkDelay() {
        RadioBaseStationBlockEntity.NetworkType networkType = getPlayerCurrentNetworkType();
        return getNetworkDelay(networkType);
    }
}