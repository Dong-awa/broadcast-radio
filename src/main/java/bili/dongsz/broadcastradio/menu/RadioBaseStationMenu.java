package bili.dongsz.broadcastradio.menu;

import bili.dongsz.broadcastradio.BroadcastRadio;
import bili.dongsz.broadcastradio.block.entity.RadioBaseStationBlockEntity;
import bili.dongsz.broadcastradio.network.UpdateRadioBaseStationPacket;
import bili.dongsz.broadcastradio.registry.ModItems;
import bili.dongsz.broadcastradio.registry.ModMenus;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

public class RadioBaseStationMenu extends AbstractContainerMenu {
    private final RadioBaseStationBlockEntity stationEntity;
    private final ContainerLevelAccess access;

    public RadioBaseStationMenu(int containerId, Inventory playerInventory, RadioBaseStationBlockEntity stationEntity) {
        super(ModMenus.RADIO_BASE_STATION_MENU.get(), containerId);
        this.stationEntity = stationEntity;
        this.access = ContainerLevelAccess.NULL;

        this.addSlot(new Slot(stationEntity, 0, 152, 108) {
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
    }

    public RadioBaseStationMenu(int containerId, Inventory playerInventory, FriendlyByteBuf buf) {
        this(containerId, playerInventory, getBlockEntity(playerInventory, buf));
    }
    
    private static RadioBaseStationBlockEntity getBlockEntity(Inventory playerInventory, FriendlyByteBuf buf) {
        BlockPos pos = buf.readBlockPos();
        net.minecraft.world.level.block.entity.BlockEntity entity = playerInventory.player.level().getBlockEntity(pos);
        if (entity instanceof RadioBaseStationBlockEntity stationEntity) {
            return stationEntity;
        } else {
            throw new IllegalArgumentException("Could not find RadioBaseStationBlockEntity at " + pos);
        }
    }

    public RadioBaseStationBlockEntity getStationEntity() {
        return stationEntity;
    }

    @Override
    public boolean stillValid(Player player) {
        if (stationEntity == null) {
            return false;
        }
        return player.level().isLoaded(stationEntity.getBlockPos()) && 
               player.distanceToSqr(stationEntity.getBlockPos().getX() + 0.5, 
                                  stationEntity.getBlockPos().getY() + 0.5, 
                                  stationEntity.getBlockPos().getZ() + 0.5) <= 64.0;
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
            }
            else if (itemstack1.is(ModItems.STORAGE_BATTERY.get())) {
                if (!this.moveItemStackTo(itemstack1, 0, 1, false)) {
                    return ItemStack.EMPTY;
                }
            }
            else if (index < 28) {
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
        
        if (stationEntity != null) {
            switch (buttonId) {
                case 0:
                    stationEntity.setNetworkType(RadioBaseStationBlockEntity.NetworkType.TWO_G);
                    break;
                case 1:
                    stationEntity.setNetworkType(RadioBaseStationBlockEntity.NetworkType.THREE_G);
                    break;
                case 2:
                    stationEntity.setNetworkType(RadioBaseStationBlockEntity.NetworkType.FOUR_G);
                    break;
                case 3:
                    stationEntity.setSignalRange(Math.max(10, stationEntity.getSignalRange() - 10));
                    break;
                case 4:
                    stationEntity.setSignalRange(Math.min(1000, stationEntity.getSignalRange() + 10));
                    break;
            }
        }
        
        return true;
    }
}