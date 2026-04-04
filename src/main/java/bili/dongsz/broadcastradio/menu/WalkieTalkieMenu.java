package bili.dongsz.broadcastradio.menu;

import bili.dongsz.broadcastradio.item.PortableWalkieTalkieItem;
import bili.dongsz.broadcastradio.registry.ModMenus;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;

public class WalkieTalkieMenu extends AbstractContainerMenu {
    private final ItemStack walkieStack;

    public WalkieTalkieMenu(int containerId, Inventory playerInventory, ItemStack walkieStack) {
        super(ModMenus.WALKIE_TALKIE_MENU.get(), containerId);
        this.walkieStack = walkieStack;
    }

    public WalkieTalkieMenu(int containerId, Inventory playerInventory, FriendlyByteBuf buf) {
        this(containerId, playerInventory, playerInventory.player.getMainHandItem());
    }

    public ItemStack getWalkieStack() {
        return walkieStack;
    }

    @Override
    public boolean stillValid(Player player) {
        return player.getInventory().contains(walkieStack);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        return ItemStack.EMPTY;
    }

    @Override
    public boolean clickMenuButton(Player player, int buttonId) {
        // 实际保存将在关闭GUI时进行
        return true;
    }
}