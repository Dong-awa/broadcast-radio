package bili.dongsz.broadcastradio.item;

import bili.dongsz.broadcastradio.BroadcastRadio;
import bili.dongsz.broadcastradio.menu.WalkieTalkieMenu;
import bili.dongsz.broadcastradio.registry.ModBlocks;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import javax.annotation.Nullable;
import java.util.List;

import static bili.dongsz.broadcastradio.item.PortableWalkieTalkieItem.POWER_CONSUMPTION_SWITCH;
import static bili.dongsz.broadcastradio.item.PortableWalkieTalkieItem.consumePower;

public class SimpleRadioItem extends BlockItem {
    public static final String TAG_FREQUENCY = "Frequency";
    public static final float DEFAULT_FREQUENCY = 88.5f; // 默认
    public static final int POWER_CONSUMPTION_SWITCH = 1;

    public SimpleRadioItem(Properties pProperties) {
        super(ModBlocks.SIMPLE_RADIO_BLOCK.get(), pProperties);
    }

    // 初始化NBT
    public static void initNBT(ItemStack stack) {
        CompoundTag tag = stack.getOrCreateTag();
        if (!tag.contains(TAG_FREQUENCY)) {
            tag.putFloat(TAG_FREQUENCY, DEFAULT_FREQUENCY);
        }
    }

    @Override
    public InteractionResult useOn(net.minecraft.world.item.context.UseOnContext context) {
        ItemStack stack = context.getItemInHand();
        initNBT(stack);
        return super.useOn(context);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        initNBT(stack);
        return super.use(level, player, hand);
    }

    @Override
    protected boolean updateCustomBlockEntityTag(BlockPos pos, Level level, @Nullable Player player, ItemStack stack, BlockState state) {
        CompoundTag tag = stack.getTag();
        if (tag != null && tag.contains(TAG_FREQUENCY)) {
            BlockEntity blockEntity = level.getBlockEntity(pos);
            if (blockEntity instanceof bili.dongsz.broadcastradio.block.entity.SimpleRadioBlockEntity) {
                ((bili.dongsz.broadcastradio.block.entity.SimpleRadioBlockEntity) blockEntity).setFrequency(tag.getFloat(TAG_FREQUENCY));
                return true;
            }
        }
        return super.updateCustomBlockEntityTag(pos, level, player, stack, state);
    }

    private float getNextPresetFrequency(float current) {
        // 预设频段（虽然暂时搁置）
        float[] presets = {88.5f, 92.1f, 95.7f, 98.3f, 101.9f, 105.5f};
        for (int i = 0; i < presets.length; i++) {
            if (Math.abs(current - presets[i]) < 0.1f) {
                return presets[(i + 1) % presets.length];
            }
        }
        return presets[0]; // 默认
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, level, tooltip, flag);
        initNBT(stack);

        CompoundTag tag = stack.getTag();
        float frequency = tag.getFloat(TAG_FREQUENCY);

        tooltip.add(Component.empty());
        tooltip.add(Component.translatable("item.broadcast_radio.simple_radio_block.frequency", String.format("%.1f", frequency)).withStyle(ChatFormatting.BLUE));
        tooltip.add(Component.translatable("item.broadcast_radio.simple_radio_block.desc").withStyle(ChatFormatting.GRAY));
    }
}