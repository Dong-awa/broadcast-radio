package bili.dongsz.broadcastradio.menu;

import bili.dongsz.broadcastradio.block.entity.SimpleSignalJammerBlockEntity;
import bili.dongsz.broadcastradio.registry.ModItems;
import bili.dongsz.broadcastradio.registry.ModMenus;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.DataSlot;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

public class SimpleSignalJammerMenu extends AbstractContainerMenu {
    private final SimpleSignalJammerBlockEntity jammerEntity;
    private final ContainerLevelAccess access;

    private final DataSlot workingSlot;
    private final DataSlot feEnergySlot;
    private final DataSlot frequencyTenthsSlot;
    private final DataSlot energySourceSlot;
    private final DataSlot batteryEnergySlot;
    private final DataSlot maxBatteryEnergySlot;

    public SimpleSignalJammerMenu(int containerId, Inventory playerInventory, SimpleSignalJammerBlockEntity jammerEntity) {
        super(ModMenus.SIMPLE_SIGNAL_JAMMER_MENU.get(), containerId);
        this.jammerEntity = jammerEntity;
        this.access = ContainerLevelAccess.NULL;

        this.addSlot(new Slot(jammerEntity, 0, 152, 108) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return stack.is(ModItems.STORAGE_BATTERY.get());
            }
        });

        for(int row = 0; row < 3; ++row) {
            for(int col = 0; col < 9; ++col) {
                this.addSlot(new Slot(playerInventory, col + row * 9 + 9, 8 + col * 18, 198 + row * 18));
            }
        }

        for(int col = 0; col < 9; ++col) {
            this.addSlot(new Slot(playerInventory, col, 8 + col * 18, 252));
        }

        DataSlot workingSlotImpl = new DataSlot() {
            private int syncedValue = 0;
            @Override
            public int get() {
                if (jammerEntity != null && jammerEntity.getLevel() != null && !jammerEntity.getLevel().isClientSide) {
                    return jammerEntity.isWorking() ? 1 : 0;
                }
                return syncedValue;
            }
            @Override
            public void set(int value) {
                this.syncedValue = value;
            }
        };
        this.addDataSlot(workingSlotImpl);
        this.workingSlot = workingSlotImpl;

        DataSlot feEnergySlotImpl = new DataSlot() {
            private int syncedValue = 0;
            @Override
            public int get() {
                if (jammerEntity != null && jammerEntity.getLevel() != null && !jammerEntity.getLevel().isClientSide) {
                    return jammerEntity.getFeEnergy();
                }
                return syncedValue;
            }
            @Override
            public void set(int value) {
                this.syncedValue = value;
            }
        };
        this.addDataSlot(feEnergySlotImpl);
        this.feEnergySlot = feEnergySlotImpl;

        DataSlot frequencyTenthsSlotImpl = new DataSlot() {
            private int syncedValue = 0;
            @Override
            public int get() {
                if (jammerEntity != null && jammerEntity.getLevel() != null && !jammerEntity.getLevel().isClientSide) {
                    return Math.round(jammerEntity.getFrequency() * 10);
                }
                return syncedValue;
            }
            @Override
            public void set(int value) {
                this.syncedValue = value;
            }
        };
        this.addDataSlot(frequencyTenthsSlotImpl);
        this.frequencyTenthsSlot = frequencyTenthsSlotImpl;

        DataSlot energySourceSlotImpl = new DataSlot() {
            private int syncedValue = 0;
            @Override
            public int get() {
                if (jammerEntity != null && jammerEntity.getLevel() != null && !jammerEntity.getLevel().isClientSide) {
                    return jammerEntity.getCurrentEnergySource().ordinal();
                }
                return syncedValue;
            }
            @Override
            public void set(int value) {
                this.syncedValue = value;
            }
        };
        this.addDataSlot(energySourceSlotImpl);
        this.energySourceSlot = energySourceSlotImpl;

        DataSlot batteryEnergySlotImpl = new DataSlot() {
            private int syncedValue = 0;
            @Override
            public int get() {
                if (jammerEntity != null && jammerEntity.getLevel() != null && !jammerEntity.getLevel().isClientSide) {
                    return jammerEntity.getBatteryEnergy();
                }
                return syncedValue;
            }
            @Override
            public void set(int value) {
                this.syncedValue = value;
            }
        };
        this.addDataSlot(batteryEnergySlotImpl);
        this.batteryEnergySlot = batteryEnergySlotImpl;

        DataSlot maxBatteryEnergySlotImpl = new DataSlot() {
            private int syncedValue = 0;
            @Override
            public int get() {
                if (jammerEntity != null && jammerEntity.getLevel() != null && !jammerEntity.getLevel().isClientSide) {
                    return jammerEntity.getMaxBatteryEnergy();
                }
                return syncedValue;
            }
            @Override
            public void set(int value) {
                this.syncedValue = value;
            }
        };
        this.addDataSlot(maxBatteryEnergySlotImpl);
        this.maxBatteryEnergySlot = maxBatteryEnergySlotImpl;
    }

    public SimpleSignalJammerMenu(int containerId, Inventory playerInventory, FriendlyByteBuf buf) {
        this(containerId, playerInventory, getBlockEntity(playerInventory, buf));
    }

    private static SimpleSignalJammerBlockEntity getBlockEntity(Inventory playerInventory, FriendlyByteBuf buf) {
        BlockPos pos = buf.readBlockPos();
        net.minecraft.world.level.block.entity.BlockEntity entity = playerInventory.player.level().getBlockEntity(pos);
        if (entity instanceof SimpleSignalJammerBlockEntity jammerEntity) {
            return jammerEntity;
        } else {
            throw new IllegalArgumentException("Could not find SimpleSignalJammerBlockEntity at " + pos);
        }
    }

    public SimpleSignalJammerBlockEntity getJammerEntity() {
        return jammerEntity;
    }

    public boolean isWorking() {
        return this.workingSlot.get() == 1;
    }

    public int getFeEnergy() {
        return this.feEnergySlot.get();
    }

    public float getFrequencyFromData() {
        return this.frequencyTenthsSlot.get() / 10.0f;
    }

    public SimpleSignalJammerBlockEntity.EnergySource getEnergySource() {
        int ordinal = this.energySourceSlot.get();
        SimpleSignalJammerBlockEntity.EnergySource[] values = SimpleSignalJammerBlockEntity.EnergySource.values();
        if (ordinal >= 0 && ordinal < values.length) {
            return values[ordinal];
        }
        return SimpleSignalJammerBlockEntity.EnergySource.NONE;
    }

    public int getBatteryEnergyFromData() {
        return this.batteryEnergySlot.get();
    }

    public int getMaxBatteryEnergyFromData() {
        return this.maxBatteryEnergySlot.get();
    }

    @Override
    public boolean stillValid(Player player) {
        if (jammerEntity == null) {
            return false;
        }
        return player.level().isLoaded(jammerEntity.getBlockPos()) &&
               player.distanceToSqr(jammerEntity.getBlockPos().getX() + 0.5,
                                  jammerEntity.getBlockPos().getY() + 0.5,
                                  jammerEntity.getBlockPos().getZ() + 0.5) <= 64.0;
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        ItemStack itemstack = ItemStack.EMPTY;
        Slot slot = this.slots.get(index);

        if (slot != null && slot.hasItem()) {
            ItemStack itemstack1 = slot.getItem();
            itemstack = itemstack1.copy();

            if (index == 0) {
                if (!this.moveItemStackTo(itemstack1, 1, this.slots.size(), true)) {
                    return ItemStack.EMPTY;
                }
            } else if (itemstack1.is(ModItems.STORAGE_BATTERY.get())) {
                if (!this.moveItemStackTo(itemstack1, 0, 1, false)) {
                    return ItemStack.EMPTY;
                }
            } else if (index < 28) {
                if (!this.moveItemStackTo(itemstack1, 28, 37, false)) {
                    return ItemStack.EMPTY;
                }
            } else if (index < 37) {
                if (!this.moveItemStackTo(itemstack1, 1, 28, false)) {
                    return ItemStack.EMPTY;
                }
            }

            if (itemstack1.isEmpty()) {
                slot.set(ItemStack.EMPTY);
            } else {
                slot.setChanged();
            }
        }
        return itemstack;
    }

    @Override
    public boolean clickMenuButton(Player player, int buttonId) {
        if (player.level().isClientSide) {
            return true;
        }

        if (jammerEntity != null) {
            switch (buttonId) {
                case 0:
                    jammerEntity.setFrequency(jammerEntity.getFrequency() - 5.0f);
                    break;
                case 1:
                    jammerEntity.setFrequency(jammerEntity.getFrequency() + 5.0f);
                    break;
                case 2:
                    jammerEntity.setFrequency(jammerEntity.getFrequency() - 0.5f);
                    break;
                case 3:
                    jammerEntity.setFrequency(jammerEntity.getFrequency() + 0.5f);
                    break;
            }
        }

        return true;
    }
}