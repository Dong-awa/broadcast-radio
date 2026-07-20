package bili.dongsz.broadcastradio.client;

import bili.dongsz.broadcastradio.screen.RadioTerminalQuickScreen;
import bili.dongsz.broadcastradio.block.entity.RadioBaseStationBlockEntity;
import bili.dongsz.broadcastradio.block.entity.SimpleSignalJammerBlockEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;

public class ClientProxy {
    private ClientProxy() {}

    public static void openRadioTerminalQuickScreen() {
        Minecraft.getInstance().setScreen(new RadioTerminalQuickScreen());
    }

    public static Minecraft getMinecraft() {
        return Minecraft.getInstance();
    }

    public static Level getClientLevel() {
        return Minecraft.getInstance().level;
    }

    public static Player getClientPlayer() {
        return Minecraft.getInstance().player;
    }

    public static RadioBaseStationBlockEntity.NetworkType getPlayerCurrentNetworkType() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) {
            return RadioBaseStationBlockEntity.NetworkType.FOUR_G;
        }

        Level level = mc.level;
        BlockPos playerPos = mc.player.blockPosition();

        for (int dx = -16; dx <= 16; dx++) {
            for (int dy = -8; dy <= 8; dy++) {
                for (int dz = -16; dz <= 16; dz++) {
                    BlockPos checkPos = playerPos.offset(dx, dy, dz);
                    BlockEntity blockEntity = level.getBlockEntity(checkPos);

                    if (blockEntity instanceof RadioBaseStationBlockEntity station) {
                        double distance = mc.player.distanceToSqr(
                            checkPos.getX() + 0.5, checkPos.getY() + 0.5, checkPos.getZ() + 0.5
                        );

                        if (distance <= station.getSignalRange() * station.getSignalRange()) {
                            return station.getNetworkType();
                        }
                    }
                }
            }
        }

        return RadioBaseStationBlockEntity.NetworkType.FOUR_G;
    }

    public static int getNetworkDelay(RadioBaseStationBlockEntity.NetworkType networkType) {
        switch (networkType) {
            case TWO_G:
                return 2500;
            case THREE_G:
                return 1500;
            case FOUR_G:
            default:
                return 500;
        }
    }

    public static int getPlayerCurrentNetworkDelay() {
        return getNetworkDelay(getPlayerCurrentNetworkType());
    }

    public static void executeOnMainThread(Runnable task) {
        Minecraft.getInstance().execute(task);
    }

    public static void sendSystemMessage(Component message) {
        Player player = Minecraft.getInstance().player;
        if (player != null) {
            player.sendSystemMessage(message);
        }
    }

    public static void updateSignalJammerFrequency(BlockPos pos, float frequency) {
        Level level = Minecraft.getInstance().level;
        if (level != null && level.isLoaded(pos)) {
            BlockEntity blockEntity = level.getBlockEntity(pos);
            if (blockEntity instanceof SimpleSignalJammerBlockEntity jammerEntity) {
                jammerEntity.setFrequency(frequency);
            }
        }
    }

    public static void updateClientServiceFlag(boolean present, boolean flag) {
        Player player = Minecraft.getInstance().player;
        if (player != null) {
            CompoundTag pdata = player.getPersistentData();
            if (present) {
                pdata.putBoolean("BroadcastRadioForceValidService", flag);
            } else {
                pdata.remove("BroadcastRadioForceValidService");
            }
        }
    }

    public static void updateRadioBaseStation(BlockPos pos, int signalRange, int networkType, String serviceName) {
        Level level = Minecraft.getInstance().level;
        if (level != null && level.isLoaded(pos)) {
            BlockEntity blockEntity = level.getBlockEntity(pos);
            if (blockEntity instanceof RadioBaseStationBlockEntity stationEntity) {
                stationEntity.setSignalRange(signalRange);
                stationEntity.setNetworkType(RadioBaseStationBlockEntity.NetworkType.values()[networkType]);
                stationEntity.setServiceName(serviceName);
            }
        }
    }

    public static void updateSignalStrength(int signalStrength, int interference) {
        SignalStrengthHUD.updateSignal(signalStrength, interference);
    }

    public static Player getPlayerByUUID(Level level, java.util.UUID uuid) {
        if (level != null) {
            return level.getPlayerByUUID(uuid);
        }
        return null;
    }
}
