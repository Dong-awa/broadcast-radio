package bili.dongsz.broadcastradio.menu;

import bili.dongsz.broadcastradio.registry.ModMenus;
import net.minecraft.core.NonNullList;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.nbt.CompoundTag;
import bili.dongsz.broadcastradio.item.RadioBatteryItem;

public class RadioTerminalMenu extends AbstractContainerMenu {
    private final ItemStack terminalStack;
    private final NonNullList<ItemStack> terminalInventory;

    public RadioTerminalMenu(int containerId, Inventory playerInventory, ItemStack terminalStack) {
        super(ModMenus.RADIO_TERMINAL_MENU.get(), containerId);
        this.terminalStack = terminalStack;
        this.terminalInventory = NonNullList.withSize(2, ItemStack.EMPTY);

        CompoundTag tag = terminalStack.getOrCreateTag();
        if (tag.contains("Battery")) {
            CompoundTag batteryTag = tag.getCompound("Battery");
            ItemStack battery = ItemStack.of(batteryTag);
            if (!battery.isEmpty()) {
                terminalInventory.set(0, battery);
            }
        }
        if (tag.contains("SimCard")) {
            CompoundTag simTag = tag.getCompound("SimCard");
            ItemStack simCard = ItemStack.of(simTag);
            if (!simCard.isEmpty()) {
                terminalInventory.set(1, simCard);
            }
        }

        int slotX = 8;
        int slotY = 32;

        this.addSlot(new Slot(new TerminalContainer(terminalInventory), 0, slotX, slotY) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return stack.is(bili.dongsz.broadcastradio.registry.ModItems.RADIO_BATTERY.get());
            }

            @Override
            public int getMaxStackSize() {
                return 1;
            }
        });

        this.addSlot(new Slot(new TerminalContainer(terminalInventory), 1, slotX + 36, slotY) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return stack.is(bili.dongsz.broadcastradio.registry.ModItems.TWO_G_UNIVERSAL.get()) ||
                       stack.is(bili.dongsz.broadcastradio.registry.ModItems.THREE_G_UNIVERSAL.get()) ||
                       stack.is(bili.dongsz.broadcastradio.registry.ModItems.FOUR_G_UNIVERSAL.get());
            }

            @Override
            public int getMaxStackSize() {
                return 1;
            }
        });

        int playerInvX = 8;
        int playerInvY = 84;

        for (int i=0;i<3;++i) {
            for (int j = 0; j < 9; ++j) {
                this.addSlot(new Slot(playerInventory, j + i * 9 + 9, playerInvX + j * 18, playerInvY + i * 18));
            }
        }

        for (int i=0;i<9;++i) {
            this.addSlot(new Slot(playerInventory, i, playerInvX + i * 18, playerInvY + 58));
        }
    }

    public RadioTerminalMenu(int containerId, Inventory playerInventory, FriendlyByteBuf buf) {
        this(containerId, playerInventory, playerInventory.player.getMainHandItem());
    }

    private static class TerminalContainer implements net.minecraft.world.Container {
        private final NonNullList<ItemStack> items;

        public TerminalContainer(NonNullList<ItemStack> items) {
            this.items = items;
        }

        @Override
        public int getContainerSize() {
            return items.size();
        }

        @Override
        public boolean isEmpty() {
            for (ItemStack stack : items) {
                if (!stack.isEmpty()) return false;
            }
            return true;
        }

        @Override
        public ItemStack getItem(int slot) {
            return slot >= 0 && slot < items.size() ? items.get(slot) : ItemStack.EMPTY;
        }

        @Override
        public ItemStack removeItem(int slot, int amount) {
            ItemStack stack = getItem(slot);
            if (!stack.isEmpty()) {
                if (stack.getCount() <= amount) {
                    items.set(slot, ItemStack.EMPTY);
                } else {
                    stack = stack.split(amount);
                    if (stack.isEmpty()) {
                        items.set(slot, ItemStack.EMPTY);
                    }
                }
            }
            return stack;
        }

        @Override
        public ItemStack removeItemNoUpdate(int slot) {
            ItemStack stack = items.get(slot);
            items.set(slot, ItemStack.EMPTY);
            return stack;
        }

        @Override
        public void setItem(int slot, ItemStack stack) {
            if (slot >= 0 && slot < items.size()) {
                items.set(slot, stack);
            }
        }

        @Override
        public void setChanged() {
        }

        @Override
        public boolean stillValid(Player player) {
            return true;
        }

        @Override
        public void clearContent() {
            items.clear();
        }
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        ItemStack itemstack = ItemStack.EMPTY;
        Slot slot = this.slots.get(index);
        if (slot.hasItem()) {
            ItemStack originalStack = slot.getItem();
            itemstack = originalStack.copy();
            if (index < 2) {
                if (!this.moveItemStackTo(originalStack, 2, this.slots.size(), true)) {
                    return ItemStack.EMPTY;
                }
            } else {
                if (!this.moveItemStackTo(originalStack, 0, 2, false)) {
                    return ItemStack.EMPTY;
                }
            }

            if (originalStack.isEmpty()) {
                slot.set(ItemStack.EMPTY);
            } else {
                slot.setChanged();
            }
        }
        return itemstack;
    }

    @Override
    public boolean stillValid(Player player) {
        return true;
    }

    public ItemStack getTerminalStack() {
        return terminalStack;
    }

    public ItemStack getBattery() {
        return terminalInventory.get(0);
    }
    
    @Override
    public void removed(Player player) {
        super.removed(player);
        // 把电池和SIM卡存到NBT中
        if (!player.level().isClientSide) {
            ItemStack battery = terminalInventory.get(0);
            ItemStack simCard = terminalInventory.get(1);
            CompoundTag tag = terminalStack.getOrCreateTag();
            
            if (!battery.isEmpty() && battery.getItem() instanceof RadioBatteryItem) {
                CompoundTag batteryTag = new CompoundTag();
                battery.save(batteryTag);
                tag.put("Battery", batteryTag);
            } else {
                tag.remove("Battery");
            }
            
            if (!simCard.isEmpty()) {
                CompoundTag simTag = new CompoundTag();
                simCard.save(simTag);
                tag.put("SimCard", simTag);
            } else {
                tag.remove("SimCard");
            }
            
            terminalStack.setTag(tag);
        }
    }
}