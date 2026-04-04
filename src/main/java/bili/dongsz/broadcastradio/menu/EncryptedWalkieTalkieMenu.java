package bili.dongsz.broadcastradio.menu;

import bili.dongsz.broadcastradio.registry.ModMenus;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;

public class EncryptedWalkieTalkieMenu extends AbstractContainerMenu {
    private final ItemStack walkieStack;

    public EncryptedWalkieTalkieMenu(int containerId, Inventory playerInventory, ItemStack walkieStack) {
        super(ModMenus.ENCRYPTED_WALKIE_TALKIE_MENU.get(), containerId);
        this.walkieStack = walkieStack;
    }

    public EncryptedWalkieTalkieMenu(int containerId, Inventory playerInventory, FriendlyByteBuf buf) {
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
        return true;
    }
}