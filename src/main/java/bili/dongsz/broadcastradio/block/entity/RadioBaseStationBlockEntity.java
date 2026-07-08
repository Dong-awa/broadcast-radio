package bili.dongsz.broadcastradio.block.entity;

import bili.dongsz.broadcastradio.registry.ModBlockEntities;
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

public class RadioBaseStationBlockEntity extends BlockEntity implements WorldlyContainer {
    public static final String TAG_SIGNAL_RANGE = "SignalRange";
    public static final String TAG_ENERGY = "Energy";
    public static final String TAG_FE_ENERGY = "FE_Energy";
    public static final String TAG_FE_MAX = "FE_Max";
    public static final String TAG_SERVICE_NAME = "ServiceName";
    public static final String TAG_NETWORK_TYPE = "NetworkType";
    public static final String TAG_ITEMS = "Items";

    public static final int DEFAULT_SIGNAL_RANGE = 100;
    public static final int DEFAULT_ENERGY = 0;
    public static final String DEFAULT_SERVICE_NAME = "";
    public static final NetworkType DEFAULT_NETWORK_TYPE = NetworkType.FOUR_G;

    public static final int MAX_FE_STORAGE = 10000;
    public static final int FE_TRANSFER_RATE = 100;

    private int signalRange = DEFAULT_SIGNAL_RANGE;
    private int energy = DEFAULT_ENERGY;
    private int feEnergy = 0;
    private int feMaxStorage = MAX_FE_STORAGE;
    private String serviceName = DEFAULT_SERVICE_NAME;
    private NetworkType networkType = DEFAULT_NETWORK_TYPE;
    private int tickCounter = 0;
    private EnergySource currentEnergySource = EnergySource.NONE;
    private NonNullList<ItemStack> items = NonNullList.withSize(1, ItemStack.EMPTY);

    private final LazyOptional<IEnergyStorage> energyHandler = LazyOptional.of(() -> new IEnergyStorage() {
        @Override
        public int receiveEnergy(int maxReceive, boolean simulate) {
            if (!canReceive()) return 0;
            int energyReceived = Math.min(feMaxStorage - feEnergy, Math.min(FE_TRANSFER_RATE, maxReceive));
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
            return feMaxStorage;
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

    public enum NetworkType {
        TWO_G("2G", 1.0f),
        THREE_G("3G", 1.5f),
        FOUR_G("4G", 2.0f);

        private final String displayName;
        private final float energyMultiplier;

        NetworkType(String displayName, float energyMultiplier) {
            this.displayName = displayName;
            this.energyMultiplier = energyMultiplier;
        }

        public String getDisplayName() {
            return displayName;
        }

        public float getEnergyMultiplier() {
            return energyMultiplier;
        }
    }

    public RadioBaseStationBlockEntity(BlockPos pPos, BlockState pBlockState) {
        super(ModBlockEntities.RADIO_BASE_STATION_BLOCK_ENTITY.get(), pPos, pBlockState);
    }

    public int getSignalRange() {
        if (getTotalEnergy() <= 0) {
            return 0;
        }
        return signalRange;
    }

    public void setSignalRange(int signalRange) {
        this.signalRange = signalRange;
        setChanged();
        if (level != null) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
        }
    }

    public int getEnergy() {
        return energy;
    }

    public void setEnergy(int energy) {
        this.energy = energy;
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
        if (level != null) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
        }
    }

    public int getFeMaxStorage() {
        return feMaxStorage;
    }

    public EnergySource getCurrentEnergySource() {
        return currentEnergySource;
    }

    public String getServiceName() {
        return serviceName;
    }

    public void setServiceName(String serviceName) {
        this.serviceName = serviceName;
        setChanged();
        if (level != null) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
        }
    }

    public NetworkType getNetworkType() {
        return networkType;
    }

    public void setNetworkType(NetworkType networkType) {
        this.networkType = networkType;
        setChanged();
        if (level != null) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
        }
    }

    public float getEnergyConsumptionRate() {
        return networkType.getEnergyMultiplier() * (signalRange / 100.0f);
    }

    @Override
    protected void saveAdditional(CompoundTag pTag) {
        super.saveAdditional(pTag);
        pTag.putInt(TAG_SIGNAL_RANGE, signalRange);
        pTag.putInt(TAG_ENERGY, energy);
        pTag.putInt(TAG_FE_ENERGY, feEnergy);
        pTag.putInt(TAG_FE_MAX, feMaxStorage);
        pTag.putString(TAG_SERVICE_NAME, serviceName);
        pTag.putInt(TAG_NETWORK_TYPE, networkType.ordinal());
        pTag.putInt("TickCounter", tickCounter);
        ContainerHelper.saveAllItems(pTag, items);
    }

    @Override
    public void load(CompoundTag pTag) {
        super.load(pTag);
        if (pTag.contains(TAG_SIGNAL_RANGE)) {
            signalRange = pTag.getInt(TAG_SIGNAL_RANGE);
        }
        if (pTag.contains(TAG_ENERGY)) {
            energy = pTag.getInt(TAG_ENERGY);
        }
        if (pTag.contains(TAG_FE_ENERGY)) {
            feEnergy = pTag.getInt(TAG_FE_ENERGY);
        }
        if (pTag.contains(TAG_FE_MAX)) {
            feMaxStorage = pTag.getInt(TAG_FE_MAX);
        }
        if (pTag.contains(TAG_SERVICE_NAME)) {
            serviceName = pTag.getString(TAG_SERVICE_NAME);
        }
        if (pTag.contains(TAG_NETWORK_TYPE)) {
            int typeOrdinal = pTag.getInt(TAG_NETWORK_TYPE);
            if (typeOrdinal >= 0 && typeOrdinal < NetworkType.values().length) {
                networkType = NetworkType.values()[typeOrdinal];
            }
        }
        if (pTag.contains("TickCounter")) {
            tickCounter = pTag.getInt("TickCounter");
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

    public static void tick(Level level, BlockPos pos, BlockState state, RadioBaseStationBlockEntity blockEntity) {
        if (level.isClientSide) {
            return;
        }

        blockEntity.tickCounter++;

        if (blockEntity.tickCounter >= 1200) {
            blockEntity.tickCounter = 0;

            if (blockEntity.isWorking()) {
                blockEntity.tryConsumeEnergy();
            }
        }
    }

    private void tryConsumeEnergy() {
        float consumption = this.getEnergyConsumptionRate();

        boolean consumed = false;

        if (feEnergy >= consumption) {
            feEnergy -= consumption;
            currentEnergySource = EnergySource.FE;
            consumed = true;
        }

        if (!consumed) {
            ItemStack batteryStack = items.get(0);
            if (!batteryStack.isEmpty()) {
                int currentDurability = getBatteryCurrentDurability(batteryStack);

                if (currentDurability > 0) {
                    int durabilityToConsume = (int) Math.ceil(consumption);
                    int newDurability = Math.max(0, currentDurability - durabilityToConsume);
                    setBatteryDurability(batteryStack, newDurability);
                    currentEnergySource = EnergySource.BATTERY;
                    consumed = true;
                }
            }
        }

        if (!consumed) {
            currentEnergySource = EnergySource.NONE;
        }

        this.setChanged();
    }

    private int getBatteryMaxDurability(ItemStack batteryStack) {
        if (batteryStack.getItem() == bili.dongsz.broadcastradio.registry.ModItems.STORAGE_BATTERY.get()) {
            return 700;
        } else if (batteryStack.getItem() == bili.dongsz.broadcastradio.registry.ModItems.RADIO_BATTERY.get()) {
            return 100;
        }
        return 0;
    }

    private int getBatteryCurrentDurability(ItemStack batteryStack) {
        if (batteryStack.getItem() == bili.dongsz.broadcastradio.registry.ModItems.STORAGE_BATTERY.get()) {
            int maxDurability = getBatteryMaxDurability(batteryStack);
            return maxDurability - batteryStack.getDamageValue();
        } else if (batteryStack.getItem() == bili.dongsz.broadcastradio.registry.ModItems.RADIO_BATTERY.get()) {
            return bili.dongsz.broadcastradio.item.RadioBatteryItem.getPower(batteryStack);
        }
        return 0;
    }

    private void setBatteryDurability(ItemStack batteryStack, int durability) {
        if (batteryStack.getItem() == bili.dongsz.broadcastradio.registry.ModItems.STORAGE_BATTERY.get()) {
            int maxDurability = getBatteryMaxDurability(batteryStack);
            batteryStack.setDamageValue(maxDurability - durability);
        } else if (batteryStack.getItem() == bili.dongsz.broadcastradio.registry.ModItems.RADIO_BATTERY.get()) {
            bili.dongsz.broadcastradio.item.RadioBatteryItem.setPower(batteryStack, durability);
        }
    }

    public boolean isWorking() {
        boolean hasFeEnergy = feEnergy > 0;
        boolean hasBattery = hasBatteryWithDurability();
        boolean hasServiceName = serviceName != null && !serviceName.trim().isEmpty();

        return (hasFeEnergy || hasBattery) && hasServiceName;
    }

    private boolean hasBatteryWithDurability() {
        ItemStack batteryStack = items.get(0);
        if (!batteryStack.isEmpty()) {
            int currentDurability = getBatteryCurrentDurability(batteryStack);
            return currentDurability > 0;
        }
        return false;
    }

    public int getTotalEnergy() {
        int total = 0;

        if (feEnergy > 0) {
            total += feEnergy;
        }

        ItemStack batteryStack = items.get(0);
        if (!batteryStack.isEmpty()) {
            int currentDurability = getBatteryCurrentDurability(batteryStack);
            total += currentDurability;
        }

        return total;
    }

    public int getBatteryEnergy() {
        ItemStack batteryStack = items.get(0);
        if (!batteryStack.isEmpty()) {
            return getBatteryCurrentDurability(batteryStack);
        }
        return 0;
    }

    public int getMaxBatteryEnergy() {
        ItemStack batteryStack = items.get(0);
        if (!batteryStack.isEmpty()) {
            return getBatteryMaxDurability(batteryStack);
        }
        return 0;
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
            return player.distanceToSqr((double) this.worldPosition.getX() + 0.5, (double) this.worldPosition.getY() + 0.5, (double) this.worldPosition.getZ() + 0.5) <= 64.0;
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
        return true;
    }

    @Override
    public boolean canTakeItemThroughFace(int index, ItemStack stack, Direction direction) {
        return true;
    }

    public NonNullList<ItemStack> getInventory() {
        return items;
    }
}