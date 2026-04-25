package bili.dongsz.broadcastradio.menu;

import bili.dongsz.broadcastradio.item.PortableWalkieTalkieItem;
import bili.dongsz.broadcastradio.registry.ModItems;
import bili.dongsz.broadcastradio.registry.ModMenus;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.core.NonNullList;

import static net.minecraft.commands.arguments.EntityArgument.getPlayer;

public class WalkieTalkieMenu extends AbstractContainerMenu {
    private final ItemStack walkieStack;
    private final NonNullList<ItemStack> batteryInventory;

    public WalkieTalkieMenu(int containerId, Inventory playerInventory, ItemStack walkieStack) {
        super(ModMenus.WALKIE_TALKIE_MENU.get(), containerId);
        this.walkieStack = walkieStack;
        this.batteryInventory = NonNullList.withSize(1, ItemStack.EMPTY);
        
        // 从对讲机NBT中加载电池
        CompoundTag tag = walkieStack.getOrCreateTag();
        if (tag.contains("Battery")) {
            CompoundTag batteryTag = tag.getCompound("Battery");
            ItemStack battery = ItemStack.of(batteryTag);
            if (!battery.isEmpty()) {
                batteryInventory.set(0, battery);
            }
        }
        
        // 电池槽位置调整到电量右边
        addSlot(new Slot(new BatteryContainer(batteryInventory), 0, 157, 60) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return stack.is(ModItems.RADIO_BATTERY.get());
            }
            
            @Override
            public int getMaxStackSize() {
                return 1; // 最大堆叠限制为1
            }
            
            @Override
            public int getMaxStackSize(ItemStack stack) {
                return 1; // 最大堆叠限制为1
            }
            
            @Override
            public boolean mayPickup(Player player) {
                return true;
            }
            
            @Override
            public void set(ItemStack stack) {
                // 简单实现：限制堆叠为1，不处理物品覆盖问题
                // 物品覆盖问题由Minecraft标准交互逻辑自动处理
                if (!stack.isEmpty()) {
                    ItemStack singleBattery = stack.copy();
                    singleBattery.setCount(1);
                    super.set(singleBattery);
                } else {
                    super.set(stack);
                }
            }
        });
        
        // 玩家物品栏
        for (int i = 0; i < 3; ++i) {
            for (int j = 0; j < 9; ++j) {
                this.addSlot(new Slot(playerInventory, j + i * 9 + 9, 8 + j * 18, 124 + i * 18));
            }
        }
        
        for (int i = 0; i < 9; ++i) {
            this.addSlot(new Slot(playerInventory, i, 8 + i * 18, 178));
        }
    }
    
    private static class BatteryContainer implements net.minecraft.world.Container {
        private final NonNullList<ItemStack> items;
        
        public BatteryContainer(NonNullList<ItemStack> items) {
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

    public WalkieTalkieMenu(int containerId, Inventory playerInventory, FriendlyByteBuf buf) {
        this(containerId, playerInventory, playerInventory.player.getMainHandItem());
    }

    public ItemStack getWalkieStack() {
        return walkieStack;
    }
    
    public ItemStack getBattery() {
        return batteryInventory.get(0);
    }

    @Override
    public boolean stillValid(Player player) {
        return player.getInventory().contains(walkieStack);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        ItemStack itemstack = ItemStack.EMPTY;
        Slot slot = this.slots.get(index);
        if (slot.hasItem()) {
            ItemStack itemstack1 = slot.getItem();
            itemstack = itemstack1.copy();
            if (index < 1) {
                if (!this.moveItemStackTo(itemstack1, 1, 37, true)) {
                    return ItemStack.EMPTY;
                }
            } else if (!this.moveItemStackTo(itemstack1, 0, 1, false)) {
                return ItemStack.EMPTY;
            }
            
            if (itemstack1.isEmpty()) {
                slot.set(ItemStack.EMPTY);
            } else {
                slot.setChanged();
            }
            
            if (itemstack1.getCount() == itemstack.getCount()) {
                return ItemStack.EMPTY;
            }
            
            slot.onTake(player, itemstack1);
            this.broadcastChanges();
        }
        return itemstack;
    }

    @Override
    public boolean clickMenuButton(Player player, int buttonId) {
        return true;
    }
    
    @Override
    public void removed(Player player) {
        super.removed(player);
        // 将电池保存到对讲机NBT中
        if (!player.level().isClientSide) {
            ItemStack battery = batteryInventory.get(0);
            CompoundTag tag = walkieStack.getOrCreateTag();
            if (!battery.isEmpty()) {
                CompoundTag batteryTag = new CompoundTag();
                battery.save(batteryTag);
                tag.put("Battery", batteryTag);
            } else {
                tag.remove("Battery");
            }
            walkieStack.setTag(tag);
        }
    }
}