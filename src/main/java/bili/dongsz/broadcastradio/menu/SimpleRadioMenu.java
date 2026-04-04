package bili.dongsz.broadcastradio.menu;

import bili.dongsz.broadcastradio.BroadcastRadio;
import bili.dongsz.broadcastradio.block.entity.SimpleRadioBlockEntity;
import bili.dongsz.broadcastradio.network.UpdateFrequencyPacket;
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

public class SimpleRadioMenu extends AbstractContainerMenu {
    private final SimpleRadioBlockEntity radioEntity;
    private final ContainerLevelAccess access;

    public SimpleRadioMenu(int containerId, Inventory playerInventory, SimpleRadioBlockEntity radioEntity) {
        super(ModMenus.SIMPLE_RADIO_MENU.get(), containerId);
        this.radioEntity = radioEntity;
        this.access = ContainerLevelAccess.NULL;
    }

    public SimpleRadioMenu(int containerId, Inventory playerInventory, FriendlyByteBuf buf) {
        this(containerId, playerInventory, getBlockEntity(playerInventory, buf));
    }
    
    private static SimpleRadioBlockEntity getBlockEntity(Inventory playerInventory, FriendlyByteBuf buf) {
        BlockPos pos = buf.readBlockPos();
        net.minecraft.world.level.block.entity.BlockEntity entity = playerInventory.player.level().getBlockEntity(pos);
        if (entity instanceof SimpleRadioBlockEntity radioEntity) {
            return radioEntity;
        } else {
            throw new IllegalArgumentException("Could not find SimpleRadioBlockEntity at " + pos);
        }
    }

    public SimpleRadioBlockEntity getRadioEntity() {
        return radioEntity;
    }

    @Override
    public boolean stillValid(Player player) {
        if (radioEntity == null) {
            return false;
        }
        // 检查方块是否仍然存在
        return player.level().isLoaded(radioEntity.getBlockPos()) && 
               player.distanceToSqr(radioEntity.getBlockPos().getX() + 0.5, 
                                  radioEntity.getBlockPos().getY() + 0.5, 
                                  radioEntity.getBlockPos().getZ() + 0.5) <= 64.0; // 8格范围
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        return ItemStack.EMPTY;
    }

    @Override
    public boolean clickMenuButton(Player player, int buttonId) {
        // 移除频率切换功能，只保留88.5MHz
        return true;
    }
}