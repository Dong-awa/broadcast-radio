package bili.dongsz.broadcastradio.item;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

public class BatteryItem extends Item {
    public BatteryItem(Properties properties) {
        super(properties);
    }
    
    @Override
    public boolean isDamageable(ItemStack stack) {
        return true;
    }
    
    @Override
    public boolean isRepairable(ItemStack stack) {
        return false;
    }
}