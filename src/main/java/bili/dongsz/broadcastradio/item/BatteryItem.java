package bili.dongsz.broadcastradio.item;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/**
 * 蓄电池物品类，实现耐久度系统
 */
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