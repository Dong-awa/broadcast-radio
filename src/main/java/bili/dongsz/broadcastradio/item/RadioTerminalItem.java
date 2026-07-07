package bili.dongsz.broadcastradio.item;

import bili.dongsz.broadcastradio.menu.RadioTerminalMenu;
import bili.dongsz.broadcastradio.menu.RadioTerminalQuickMenu;
import bili.dongsz.broadcastradio.screen.RadioTerminalQuickScreen;
import bili.dongsz.broadcastradio.utils.SignalSearchManager;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import bili.dongsz.broadcastradio.block.entity.RadioBaseStationBlockEntity;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.ChatFormatting;
import net.minecraft.server.level.ServerPlayer;

import java.util.concurrent.ThreadLocalRandom;

public class RadioTerminalItem extends Item {
    public static final String TAG_BATTERY = "Battery";
    public static final String TAG_SIM_CARD = "SimCard";
    public static final String TAG_LAST_CONSUME_TIME = "LastConsumeTime";
    public static final int STANDBY_CONSUME_INTERVAL = 60; // 60秒
    public static final int STANDBY_CONSUME_AMOUNT = 1; // 每次消耗1点电量

    public RadioTerminalItem(Properties pProperties) {
        super(pProperties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);

        if (player.isShiftKeyDown()) {
            if (!level.isClientSide) {
                player.openMenu(new SimpleMenuProvider(
                    (containerId, playerInventory, playerEntity) -> new RadioTerminalMenu(containerId, playerInventory, stack),
                    Component.translatable("item.broadcast_radio.radio_terminal")
                ));
            }
            return InteractionResultHolder.success(stack);
        }

        if (!hasBattery(stack)) {
            if (level.isClientSide) {
                player.displayClientMessage(Component.translatable("item.broadcast_radio.radio_terminal.no_battery"), true);
            }
            return InteractionResultHolder.fail(stack);
        }

        if (getBatteryLevel(stack) <= 0) {
            if (level.isClientSide) {
                player.displayClientMessage(Component.translatable("item.broadcast_radio.radio_terminal.power_depleted"), true);
            }
            return InteractionResultHolder.fail(stack);
        }

        // 对讲机的电池消耗
        CompoundTag tag = stack.getOrCreateTag();
        long currentTime = level.getGameTime();
        long lastConsumeTime = tag.getLong(TAG_LAST_CONSUME_TIME);
        
        if (currentTime - lastConsumeTime >= STANDBY_CONSUME_INTERVAL * 20) {
            consumeBattery(stack, player);
            tag.putLong(TAG_LAST_CONSUME_TIME, currentTime);
            stack.setTag(tag);
        }

        if (level.isClientSide) {
            SignalSearchManager searchManager = SignalSearchManager.getInstance();
            if (!searchManager.isRunning()) {
                searchManager.startSignalSearch();
            }
            Minecraft.getInstance().setScreen(new RadioTerminalQuickScreen());
        }
        return InteractionResultHolder.success(stack);
    }

    public static boolean hasBattery(ItemStack terminalStack) {
        if (terminalStack.isEmpty() || !(terminalStack.getItem() instanceof RadioTerminalItem)) {
            return false;
        }
        CompoundTag tag = terminalStack.getTag();
        if (tag != null && tag.contains(TAG_BATTERY)) {
            CompoundTag batteryTag = tag.getCompound(TAG_BATTERY);
            ItemStack battery = ItemStack.of(batteryTag);
            return !battery.isEmpty() && battery.getItem() instanceof RadioBatteryItem;
        }
        return false;
    }

    public static int getBatteryLevel(ItemStack terminalStack) {
        if (!hasBattery(terminalStack)) {
            return 0;
        }
        CompoundTag tag = terminalStack.getTag();
        if (tag == null) {
            return 0;
        }
        CompoundTag batteryTag = tag.getCompound(TAG_BATTERY);
        ItemStack battery = ItemStack.of(batteryTag);
        if (!battery.isEmpty() && battery.getItem() instanceof RadioBatteryItem) {
            return RadioBatteryItem.getPower(battery);
        }
        return 0;
    }

    public static void setBatteryLevel(ItemStack terminalStack, int level) {
        if (!hasBattery(terminalStack)) {
            return;
        }
        CompoundTag tag = terminalStack.getOrCreateTag();
        CompoundTag batteryTag = tag.getCompound(TAG_BATTERY);
        ItemStack battery = ItemStack.of(batteryTag);
        if (!battery.isEmpty() && battery.getItem() instanceof RadioBatteryItem) {
            RadioBatteryItem.setPower(battery, level);
            CompoundTag updatedBatteryTag = new CompoundTag();
            battery.save(updatedBatteryTag);
            tag.put(TAG_BATTERY, updatedBatteryTag);
        }
    }

    public static void consumeBatteryInTick(ItemStack terminalStack, Player player) {
        if (!hasBattery(terminalStack)) {
            return;
        }
        
        CompoundTag tag = terminalStack.getOrCreateTag();
        long currentTime = player.level().getGameTime();
        long lastConsumeTime = tag.getLong(TAG_LAST_CONSUME_TIME);
        
        // -1/60s
        if (currentTime - lastConsumeTime >= STANDBY_CONSUME_INTERVAL * 20) {
            consumeBattery(terminalStack, player);
            tag.putLong(TAG_LAST_CONSUME_TIME, currentTime);
            terminalStack.setTag(tag);
        }
    }

    public static void consumeBatteryInGui(ItemStack terminalStack, Player player) {
        if (!hasBattery(terminalStack)) {
            return;
        }
        
        CompoundTag tag = terminalStack.getOrCreateTag();
        long currentTime = player.level().getGameTime();
        long lastConsumeTime = tag.getLong(TAG_LAST_CONSUME_TIME);
        
        // -1/60s
        if (currentTime - lastConsumeTime >= STANDBY_CONSUME_INTERVAL * 20) {
            consumeBattery(terminalStack, player);
            tag.putLong(TAG_LAST_CONSUME_TIME, currentTime);
            terminalStack.setTag(tag);
        }
    }

    public static void consumeBattery(ItemStack terminalStack, Player player) {
        if (!hasBattery(terminalStack)) {
            return;
        }
        
        ItemStack batteryStack = ItemStack.EMPTY;
        if (player.containerMenu instanceof RadioTerminalMenu) {
            RadioTerminalMenu menu = (RadioTerminalMenu) player.containerMenu;
            batteryStack = menu.getBattery();
        } else {
            CompoundTag tag = terminalStack.getOrCreateTag();
            if (tag.contains(TAG_BATTERY)) {
                CompoundTag batteryTag = tag.getCompound(TAG_BATTERY);
                batteryStack = ItemStack.of(batteryTag);
            }
        }
        
        if (!batteryStack.isEmpty() && batteryStack.getItem() instanceof RadioBatteryItem) {
            int currentPower = RadioBatteryItem.getPower(batteryStack);
            boolean wasEmpty = (currentPower == 0);
            
            RadioBatteryItem.consumePower(batteryStack, STANDBY_CONSUME_AMOUNT);
            
            int newPower = RadioBatteryItem.getPower(batteryStack);
            boolean isEmpty = (newPower == 0);
            
            if (!wasEmpty && isEmpty) {
                player.sendSystemMessage(Component.translatable("item.broadcast_radio.radio_terminal.power_depleted").withStyle(ChatFormatting.RED));
            } else if (!wasEmpty && !isEmpty && newPower <= 20 && currentPower > 20) {
                player.sendSystemMessage(Component.translatable("item.broadcast_radio.radio_terminal.low_power").withStyle(ChatFormatting.YELLOW));
            }
            
            if (newPower == 0) {
                if (player.containerMenu instanceof RadioTerminalMenu) {
                    RadioTerminalMenu menu = (RadioTerminalMenu) player.containerMenu;
                    menu.getBattery().shrink(1);
                } else {
                    CompoundTag tag = terminalStack.getOrCreateTag();
                    tag.remove(TAG_BATTERY);
                    terminalStack.setTag(tag);
                }
            } else {
                if (!(player.containerMenu instanceof RadioTerminalMenu)) {
                    CompoundTag tag = terminalStack.getOrCreateTag();
                    CompoundTag batteryTag = new CompoundTag();
                    batteryStack.save(batteryTag);
                    tag.put(TAG_BATTERY, batteryTag);
                    terminalStack.setTag(tag);
                }
            }
        }
    }

    public static class BaseStationInfo {
        public final String serviceName;
        public final BlockPos pos;
        
        public BaseStationInfo(String serviceName, BlockPos pos) {
            this.serviceName = serviceName;
            this.pos = pos;
        }
    }
    
    public static BaseStationInfo findClosestBaseStation(Level level, Player player) {
        double closestDistance = Double.MAX_VALUE;
        String serviceName = Component.translatable("item.broadcast_radio.radio_terminal.no_signal").getString();
        BlockPos closestPos = null;
        
        ItemStack terminalStack = player.getMainHandItem();
        if (terminalStack.isEmpty() || !(terminalStack.getItem() instanceof RadioTerminalItem)) {
            terminalStack = player.getOffhandItem();
        }
        if (terminalStack.isEmpty() || !(terminalStack.getItem() instanceof RadioTerminalItem)) {
            for (int i = 0; i < 41; i++) {
                ItemStack stack = player.getInventory().getItem(i);
                if (stack.getItem() instanceof RadioTerminalItem) {
                    terminalStack = stack;
                    break;
                }
            }
        }
        
        if (!terminalStack.isEmpty() && terminalStack.getItem() instanceof RadioTerminalItem) {
            CompoundTag tag = terminalStack.getTag();
            if (tag != null && tag.contains(TAG_SIM_CARD)) {
                CompoundTag simTag = tag.getCompound(TAG_SIM_CARD);
                ItemStack simCard = ItemStack.of(simTag);
                if (!simCard.isEmpty()) {
                    BlockPos playerPos = player.blockPosition();
                    int searchRadius = 100;
                    for (int radius = 0; radius <= searchRadius; radius++) {
                        boolean foundInRadius = false;
                        for (int dx = -radius; dx <= radius; dx++) {
                            for (int dy = -Math.min(32, radius); dy <= Math.min(32, radius); dy++) {
                                for (int dz = -radius; dz <= radius; dz++) {
                                    if (Math.abs(dx) != radius && Math.abs(dy) != Math.min(32, radius) && Math.abs(dz) != radius) {
                                        continue;
                                    }
                                    
                                    BlockPos pos = playerPos.offset(dx, dy, dz);
                                    BlockEntity blockEntity = level.getBlockEntity(pos);
                                    if (blockEntity instanceof RadioBaseStationBlockEntity) {
                                        RadioBaseStationBlockEntity baseStation = (RadioBaseStationBlockEntity) blockEntity;
                                        double distance = player.distanceToSqr(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5);
                                        int signalRange = baseStation.getSignalRange();
                                        if (distance <= signalRange * signalRange) {
                                            if (isNetworkTypeMatch(simCard, baseStation.getNetworkType())) {
                                                if (distance < closestDistance) {
                                                    closestDistance = distance;
                                                    String stationServiceName = baseStation.getServiceName();
                                                    serviceName = stationServiceName.isEmpty() ? Component.translatable("item.broadcast_radio.radio_terminal.unknown_service").getString() : stationServiceName;
                                                    closestPos = pos;
                                                    foundInRadius = true;
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                        
                        if (closestDistance < Double.MAX_VALUE && radius * radius > closestDistance) {
                            break;
                        }
                    }
                }
            }
        }
        
        return new BaseStationInfo(serviceName, closestPos);
    }
    
    public static String getCurrentServiceName(Level level, Player player) {
        return findClosestBaseStation(level, player).serviceName;
    }
    
    public static BlockPos getCurrentBaseStationPos(Level level, Player player) {
        return findClosestBaseStation(level, player).pos;
    }
    
    public static BaseStationInfo getCurrentBaseStationInfo(Level level, Player player) {
        return findClosestBaseStation(level, player);
    }
    
    private static boolean isNetworkTypeMatch(ItemStack simCard, RadioBaseStationBlockEntity.NetworkType baseStationType) {
        if (simCard.is(bili.dongsz.broadcastradio.registry.ModItems.TWO_G_UNIVERSAL.get())) {
            return baseStationType == RadioBaseStationBlockEntity.NetworkType.TWO_G;
        } else if (simCard.is(bili.dongsz.broadcastradio.registry.ModItems.THREE_G_UNIVERSAL.get())) {
            return baseStationType == RadioBaseStationBlockEntity.NetworkType.THREE_G;
        } else if (simCard.is(bili.dongsz.broadcastradio.registry.ModItems.FOUR_G_UNIVERSAL.get())) {
            return baseStationType == RadioBaseStationBlockEntity.NetworkType.FOUR_G;
        }
        return false;
    }
}