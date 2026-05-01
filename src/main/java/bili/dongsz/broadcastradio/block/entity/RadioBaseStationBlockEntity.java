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

import javax.annotation.Nullable;

public class RadioBaseStationBlockEntity extends BlockEntity implements WorldlyContainer {
    public static final String TAG_SIGNAL_RANGE = "SignalRange";
    public static final String TAG_ENERGY = "Energy";
    public static final String TAG_SERVICE_NAME = "ServiceName";
    public static final String TAG_NETWORK_TYPE = "NetworkType";
    public static final String TAG_ITEMS = "Items";
    
    public static final int DEFAULT_SIGNAL_RANGE = 100;
    public static final int DEFAULT_ENERGY = 0;
    public static final String DEFAULT_SERVICE_NAME = "";
    public static final NetworkType DEFAULT_NETWORK_TYPE = NetworkType.FOUR_G;
    
    private int signalRange = DEFAULT_SIGNAL_RANGE;
    private int energy = DEFAULT_ENERGY;
    private String serviceName = DEFAULT_SERVICE_NAME;
    private NetworkType networkType = DEFAULT_NETWORK_TYPE;
    private int tickCounter = 0; // tick计数器
    private NonNullList<ItemStack> items = NonNullList.withSize(1, ItemStack.EMPTY);
    
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

    public static void tick(Level level, BlockPos pos, BlockState state, RadioBaseStationBlockEntity blockEntity) {
        if (level.isClientSide) {
            return;
        }
        
        blockEntity.tickCounter++;
        
        // 每 1200 tick = 60 秒 (20 tick/秒 * 60 秒)
        if (blockEntity.tickCounter >= 1200) {
            blockEntity.tickCounter = 0;
            
            // 只有基站工作时才消耗电量
            if (blockEntity.isWorking()) {
                blockEntity.tryConsumeEnergy();
            }
        }
    }

    private void tryConsumeEnergy() {
        float consumption = this.getEnergyConsumptionRate();

        ItemStack batteryStack = items.get(0);
        if (!batteryStack.isEmpty()) {
            int currentDurability = getBatteryCurrentDurability(batteryStack);
            
            if (currentDurability > 0) {
                int durabilityToConsume = (int)Math.ceil(consumption); // 1:1
                int newDurability = Math.max(0, currentDurability - durabilityToConsume);
                setBatteryDurability(batteryStack, newDurability);
                this.setChanged();
            }
        }
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
        boolean hasBattery = hasBatteryWithDurability();
        boolean hasServiceName = serviceName != null && !serviceName.trim().isEmpty();
        
        return hasBattery && hasServiceName;
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
        ItemStack batteryStack = items.get(0);
        if (!batteryStack.isEmpty()) {
            int currentDurability = getBatteryCurrentDurability(batteryStack);
            return currentDurability; // 1:1
        }
        
        return 0;
    }

    @Override
    public int getContainerSize() {
        return items.size();
    }

    @Override
    public boolean isEmpty() {
        for(ItemStack itemstack : items) {
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
            return player.distanceToSqr((double)this.worldPosition.getX() + 0.5, (double)this.worldPosition.getY() + 0.5, (double)this.worldPosition.getZ() + 0.5) <= 64.0;
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