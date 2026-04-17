package bili.dongsz.broadcastradio.menu;

import bili.dongsz.broadcastradio.registry.ModMenus;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;

public class RadioTerminalQuickMenu extends AbstractContainerMenu {
    private final Inventory playerInventory;
    private final ItemStack terminalStack;

    public RadioTerminalQuickMenu(int containerId, Inventory playerInventory, ItemStack terminalStack) {
        super(ModMenus.RADIO_TERMINAL_QUICK_MENU.get(), containerId);
        this.playerInventory = playerInventory;
        this.terminalStack = terminalStack;
    }

    public RadioTerminalQuickMenu(int containerId, Inventory playerInventory, FriendlyByteBuf buf) {
        this(containerId, playerInventory, playerInventory.player.getMainHandItem());
    }

    @Override
    public boolean stillValid(Player player) {
        return true;
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        return ItemStack.EMPTY;
    }

    @Override
    public boolean clickMenuButton(Player player, int buttonId) {
        return true;
    }

    public ItemStack getTerminalStack() {
        return terminalStack;
    }
}