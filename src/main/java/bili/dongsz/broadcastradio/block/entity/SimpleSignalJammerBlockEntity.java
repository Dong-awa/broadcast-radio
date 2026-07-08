package bili.dongsz.broadcastradio.block.entity;

import bili.dongsz.broadcastradio.registry.ModBlockEntities;
import bili.dongsz.broadcastradio.registry.ModItems;
import bili.dongsz.broadcastradio.item.RadioBatteryItem;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.energy.IEnergyStorage;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class SimpleSignalJammerBlockEntity extends BlockEntity implements WorldlyContainer {
    public static final int EFFECTIVE_RADIUS = 32;
    public static final int LIMIT_RADIUS = 50;
    public static final int MAX_FE_STORAGE = 10000;
    public static final int FE_CONSUMPTION_PER_TICK = 10;
    public static final int BATTERY_CONSUMPTION_PER_MINUTE = 3;
    public static final int TICKS_PER_MINUTE = 1200;

    public static final String TAG_FREQUENCY = "Frequency";
    public static final String TAG_FE_ENERGY = "FE_Energy";
    public static final String TAG_CURRENT_SOURCE = "CurrentSource";
    public static final String TAG_TICK_COUNTER = "TickCounter";
    public static final String TAG_BATTERY_TICK_ACCUM = "BatteryTickAccumulator";

    public static final float DEFAULT_FREQUENCY = 433.0f;
    private static final float MIN_FREQ = 1.0f;
    private static final float MAX_FREQ = 999.9f;

    private float frequency = DEFAULT_FREQUENCY;
    private int feEnergy = 0;
    private int tickCounter = 0;
    private int batteryTickAccumulator = 0;
    private EnergySource currentEnergySource = EnergySource.NONE;
    private NonNullList<ItemStack> items = NonNullList.withSize(1, ItemStack.EMPTY);

    private final LazyOptional<IEnergyStorage> energyHandler = LazyOptional.of(() -> new IEnergyStorage() {
        @Override
        public int receiveEnergy(int maxReceive, boolean simulate) {
            if (!canReceive()) return 0;
            int energyReceived = Math.min(MAX_FE_STORAGE - feEnergy, Math.min(FE_CONSUMPTION_PER_TICK * 10, maxReceive));
            if (!simulate) {
                feEnergy += energyReceived;
                setChanged();
            }
            return energyReceived;
        }

        @Override
        public int extractEnergy(int maxExtract, boolean simulate) {
            return 0;
        }

        @Override
        public int getEnergyStored() {
            return feEnergy;
        }

        @Override
        public int getMaxEnergyStored() {
            return MAX_FE_STORAGE;
        }

        @Override
        public boolean canExtract() {
            return false;
        }

        @Override
        public boolean canReceive() {
            return true;
        }
    });

    public enum EnergySource {
        FE("FE"),
        BATTERY("Battery"),
        NONE("None");

        private final String name;

        EnergySource(String name) {
            this.name = name;
        }

        public String getName() {
            return name;
        }
    }

    public SimpleSignalJammerBlockEntity(BlockPos pPos, BlockState pBlockState) {
        super(ModBlockEntities.SIMPLE_SIGNAL_JAMMER_BLOCK_ENTITY.get(), pPos, pBlockState);
    }

    public float getFrequency() {
        return frequency;
    }

    public void setFrequency(float frequency) {
        this.frequency = Math.max(MIN_FREQ, Math.min(MAX_FREQ, frequency));
        setChanged();
        if (level != null) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
        }
    }

    public int getFeEnergy() {
        return feEnergy;
    }

    public void setFeEnergy(int feEnergy) {
        this.feEnergy = feEnergy;
        setChanged();
    }

    public EnergySource getCurrentEnergySource() {
        return currentEnergySource;
    }

    public int getBatteryEnergy() {
        ItemStack batteryStack = items.get(0);
        if (!batteryStack.isEmpty()) {
            if (batteryStack.getItem() == ModItems.STORAGE_BATTERY.get()) {
                int maxDurability = 700;
                return maxDurability - batteryStack.getDamageValue();
            } else if (batteryStack.getItem() == ModItems.RADIO_BATTERY.get()) {
                return RadioBatteryItem.getPower(batteryStack);
            }
        }
        return 0;
    }

    public int getMaxBatteryEnergy() {
        ItemStack batteryStack = items.get(0);
        if (!batteryStack.isEmpty()) {
            if (batteryStack.getItem() == ModItems.STORAGE_BATTERY.get()) {
                return 700;
            } else if (batteryStack.getItem() == ModItems.RADIO_BATTERY.get()) {
                return 100;
            }
        }
        return 0;
    }

    public boolean isWorking() {
        return currentEnergySource != EnergySource.NONE;
    }

    public static int getEffectiveRadius() {
        return EFFECTIVE_RADIUS;
    }

    public static int getLimitRadius() {
        return LIMIT_RADIUS;
    }

    public static int calculateInterferenceAtDistance(double distanceSq) {
        double distance = Math.sqrt(distanceSq);
        if (distance <= EFFECTIVE_RADIUS) {
            return 90;
        } else if (distance <= LIMIT_RADIUS) {
            double factor = 1.0 - (distance - EFFECTIVE_RADIUS) / (LIMIT_RADIUS - EFFECTIVE_RADIUS);
            return (int) (90 * factor);
        }
        return 0;
    }

    @Override
    protected void saveAdditional(CompoundTag pTag) {
        super.saveAdditional(pTag);
        pTag.putFloat(TAG_FREQUENCY, frequency);
        pTag.putInt(TAG_FE_ENERGY, feEnergy);
        pTag.putInt(TAG_CURRENT_SOURCE, currentEnergySource.ordinal());
        pTag.putInt(TAG_TICK_COUNTER, tickCounter);
        pTag.putInt(TAG_BATTERY_TICK_ACCUM, batteryTickAccumulator);
        ContainerHelper.saveAllItems(pTag, items);
    }

    @Override
    public void load(CompoundTag pTag) {
        super.load(pTag);
        if (pTag.contains(TAG_FREQUENCY)) {
            frequency = pTag.getFloat(TAG_FREQUENCY);
        }
        if (pTag.contains(TAG_FE_ENERGY)) {
            feEnergy = pTag.getInt(TAG_FE_ENERGY);
        }
        if (pTag.contains(TAG_CURRENT_SOURCE)) {
            int sourceOrdinal = pTag.getInt(TAG_CURRENT_SOURCE);
            if (sourceOrdinal >= 0 && sourceOrdinal < EnergySource.values().length) {
                currentEnergySource = EnergySource.values()[sourceOrdinal];
            }
        }
        if (pTag.contains(TAG_TICK_COUNTER)) {
            tickCounter = pTag.getInt(TAG_TICK_COUNTER);
        }
        if (pTag.contains(TAG_BATTERY_TICK_ACCUM)) {
            batteryTickAccumulator = pTag.getInt(TAG_BATTERY_TICK_ACCUM);
        }
        ContainerHelper.loadAllItems(pTag, items);
    }

    @Override
    public ClientboundBlockEntityDataPacket getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public CompoundTag getUpdateTag() {
        CompoundTag tag = new CompoundTag();
        saveAdditional(tag);
        return tag;
    }

    @Nonnull
    @Override
    public <T> LazyOptional<T> getCapability(@Nonnull Capability<T> cap, @Nullable Direction side) {
        if (cap == ForgeCapabilities.ENERGY) {
            return energyHandler.cast();
        }
        return super.getCapability(cap, side);
    }

    @Override
    public void invalidateCaps() {
        super.invalidateCaps();
        energyHandler.invalidate();
    }

    public static void tick(Level level, BlockPos pos, BlockState state, SimpleSignalJammerBlockEntity blockEntity) {
        if (level.isClientSide) {
            return;
        }

        blockEntity.tickCounter++;

        boolean wasWorking = blockEntity.currentEnergySource != EnergySource.NONE;
        EnergySource oldSource = blockEntity.currentEnergySource;
        boolean consumed = false;

        if (blockEntity.feEnergy >= FE_CONSUMPTION_PER_TICK) {
            blockEntity.feEnergy -= FE_CONSUMPTION_PER_TICK;
            blockEntity.currentEnergySource = EnergySource.FE;
            consumed = true;
        }

        if (!consumed) {
            ItemStack batteryStack = blockEntity.items.get(0);
            if (!batteryStack.isEmpty()) {
                int batteryEnergy = blockEntity.getBatteryEnergy();
                if (batteryEnergy >= 1) {
                    blockEntity.batteryTickAccumulator++;
                    int ticksPerBatteryUnit = TICKS_PER_MINUTE / BATTERY_CONSUMPTION_PER_MINUTE;

                    if (blockEntity.batteryTickAccumulator >= ticksPerBatteryUnit) {
                        int unitsToConsume = blockEntity.batteryTickAccumulator / ticksPerBatteryUnit;
                        blockEntity.batteryTickAccumulator -= unitsToConsume * ticksPerBatteryUnit;
                        int actualConsumption = Math.min(unitsToConsume, batteryEnergy);

                        if (batteryStack.getItem() == ModItems.STORAGE_BATTERY.get()) {
                            batteryStack.setDamageValue(batteryStack.getDamageValue() + actualConsumption);
                        } else if (batteryStack.getItem() == ModItems.RADIO_BATTERY.get()) {
                            int newPower = Math.max(0, RadioBatteryItem.getPower(batteryStack) - actualConsumption);
                            RadioBatteryItem.setPower(batteryStack, newPower);
                        }
                    }
                    blockEntity.currentEnergySource = EnergySource.BATTERY;
                    consumed = true;
                }
            }
        }

        if (!consumed) {
            blockEntity.currentEnergySource = EnergySource.NONE;
        }

        boolean isWorking = blockEntity.currentEnergySource != EnergySource.NONE;
        if (wasWorking != isWorking || blockEntity.tickCounter % 200 == 0) {
            bili.dongsz.broadcastradio.BroadcastRadio.LOGGER.info("[JammerBlock] 位置{} 频率{} 工作状态: {} -> {}, 能源: {} (FE能量={}, 电池能量={}, 槽中有物品={})",
                    pos, blockEntity.frequency, wasWorking, isWorking,
                    blockEntity.currentEnergySource, blockEntity.feEnergy, blockEntity.getBatteryEnergy(),
                    !blockEntity.items.get(0).isEmpty());
        }

        blockEntity.setChanged();
    }

    @Override
    public int getContainerSize() {
        return items.size();
    }

    @Override
    public boolean isEmpty() {
        for (ItemStack itemstack : items) {
            if (!itemstack.isEmpty()) {
                return false;
            }
        }
        return true;
    }

    @Override
    public ItemStack getItem(int index) {
        return items.get(index);
    }

    @Override
    public ItemStack removeItem(int index, int count) {
        ItemStack itemstack = ContainerHelper.removeItem(items, index, count);
        if (!itemstack.isEmpty()) {
            this.setChanged();
        }
        return itemstack;
    }

    @Override
    public ItemStack removeItemNoUpdate(int index) {
        return ContainerHelper.takeItem(items, index);
    }

    @Override
    public void setItem(int index, ItemStack stack) {
        items.set(index, stack);
        if (stack.getCount() > this.getMaxStackSize()) {
            stack.setCount(this.getMaxStackSize());
        }
        this.setChanged();
    }

    @Override
    public boolean stillValid(Player player) {
        if (this.level.getBlockEntity(this.worldPosition) != this) {
            return false;
        } else {
            return player.distanceToSqr((double) this.worldPosition.getX() + 0.5,
                    (double) this.worldPosition.getY() + 0.5,
                    (double) this.worldPosition.getZ() + 0.5) <= 64.0;
        }
    }

    @Override
    public void clearContent() {
        items.clear();
    }

    @Override
    public int[] getSlotsForFace(Direction side) {
        return new int[]{0};
    }

    @Override
    public boolean canPlaceItemThroughFace(int index, ItemStack itemStack, @Nullable Direction direction) {
        return itemStack.is(ModItems.STORAGE_BATTERY.get()) || itemStack.is(ModItems.RADIO_BATTERY.get());
    }

    @Override
    public boolean canTakeItemThroughFace(int index, ItemStack stack, Direction direction) {
        return true;
    }

    public NonNullList<ItemStack> getInventory() {
        return items;
    }
}