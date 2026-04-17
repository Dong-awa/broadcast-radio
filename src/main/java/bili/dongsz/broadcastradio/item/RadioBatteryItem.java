package bili.dongsz.broadcastradio.item;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

public class RadioBatteryItem extends Item {
    public static final int MAX_POWER = 100;
    public static final String TAG_POWER = "Power";
    
    public RadioBatteryItem(Properties properties) {
        super(properties.stacksTo(64));
    }
    
    @Override
    public boolean isBarVisible(ItemStack stack) {
        int power = getPower(stack);
        return power < MAX_POWER;
    }
    
    @Override
    public int getBarWidth(ItemStack stack) {
        int power = getPower(stack);
        return (int) (13.0 * power / MAX_POWER);
    }
    
    @Override
    public int getBarColor(ItemStack stack) {
        int power = getPower(stack);
        if (power > 60) return 0x4CAF50;
        if (power > 30) return 0xFFC107;
        return 0xF44336;
    }
    
    @Override
    public int getMaxStackSize(ItemStack stack) {
        return getPower(stack) == MAX_POWER ? 64 : 1;
    }
    
    public static int getPower(ItemStack stack) {
        if (stack.isEmpty()) return 0;
        CompoundTag tag = stack.getTag();
        return tag != null ? tag.getInt(TAG_POWER) : MAX_POWER;
    }
    
    public static void setPower(ItemStack stack, int power) {
        if (stack.isEmpty()) return;
        CompoundTag tag = stack.getOrCreateTag();
        tag.putInt(TAG_POWER, Math.min(MAX_POWER, Math.max(0, power)));
        stack.setTag(tag);
    }
    
    public static void consumePower(ItemStack stack, int amount) {
        if (stack.isEmpty()) return;
        int currentPower = getPower(stack);
        setPower(stack, currentPower - amount);
    }
}