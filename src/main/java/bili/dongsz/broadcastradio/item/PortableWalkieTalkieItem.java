package bili.dongsz.broadcastradio.item;

import bili.dongsz.broadcastradio.BroadcastRadio;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.event.ServerChatEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;

import javax.annotation.Nullable;
import java.util.List;

public class PortableWalkieTalkieItem extends Item {
    public static final String TAG_FREQUENCY = "Frequency"; // 当前频段（默认1）
    public static final String TAG_PASSWORD = "Password"; // 加密密码（默认空）
    public static final int DEFAULT_FREQUENCY = 1; // 默认频段
    public static final int POWER_CONSUMPTION_SEND = 1; // 发送消息耗电
    public static final int POWER_CONSUMPTION_STANDBY = 1; // 待机每60秒耗电
    public static final int MAX_FREQUENCY = 10;             // 最大频段
    public static final int MIN_FREQUENCY = 1;              // 最小频段
    public static final int COMMUNICATION_RANGE = 128;       // 通讯距离（64格）
    public static final int POWER_CONSUMPTION_SWITCH = 1;   // 切换频道耗电

    public PortableWalkieTalkieItem(Properties pProperties) {
        super(pProperties);
    }

    // ========== NBT初始化 ==========
    @Override
    public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity livingEntity) {
        if (livingEntity instanceof Player player) {
            initNBT(stack); // 初始化NBT
        }
        return super.finishUsingItem(stack, level, livingEntity);
    }

    // 初始化NBT
    public static void initNBT(ItemStack stack) {
        CompoundTag tag = stack.getOrCreateTag();
        if (!tag.contains(TAG_FREQUENCY)) {
            tag.putInt(TAG_FREQUENCY, DEFAULT_FREQUENCY);
        }
        if (!tag.contains(TAG_PASSWORD)) {
            tag.putString(TAG_PASSWORD, "");
        }
    }

    // ========== 交互 ==========
    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        initNBT(stack);

        // 检查电量
        if (stack.getDamageValue() >= stack.getMaxDamage()) {
            player.sendSystemMessage(Component.translatable("item.broadcast_radio.walkie_talkie.no_power").withStyle(ChatFormatting.RED));
            return InteractionResultHolder.fail(stack);
        }

        // 切换频道
        if (player.isShiftKeyDown()) {
            if (!level.isClientSide) {
                CompoundTag tag = stack.getTag();
                int currentFreq = tag.getInt(TAG_FREQUENCY);
                // 频道循环切换
                int newFreq = currentFreq + 1 > MAX_FREQUENCY ? MIN_FREQUENCY : currentFreq + 1;
                tag.putInt(TAG_FREQUENCY, newFreq);
                stack.setTag(tag);

                player.level().playSound(null, player.blockPosition(), SoundEvents.LEVER_CLICK, SoundSource.PLAYERS, 0.5F, 1.0F);
                consumePower(stack, POWER_CONSUMPTION_SWITCH, player);
                player.sendSystemMessage(Component.translatable("item.broadcast_radio.walkie_talkie.freq_switch", newFreq).withStyle(ChatFormatting.BLUE));
            }
            return InteractionResultHolder.success(stack);
        }

        // 发送
        if (!level.isClientSide && player instanceof ServerPlayer serverPlayer) {
            serverPlayer.sendSystemMessage(Component.translatable("item.broadcast_radio.walkie_talkie.input_hint").withStyle(ChatFormatting.YELLOW));
            // 标记当前手持对讲机
            serverPlayer.getPersistentData().putBoolean(BroadcastRadio.MOD_ID + "_using_walkie", true);
            // 待机耗电
            consumePower(stack, POWER_CONSUMPTION_STANDBY, player);
        }
        return InteractionResultHolder.success(stack);
    }

    // ==========  电量消耗逻辑 ==========
    public static void consumePower(ItemStack stack, int amount, Player player) {
        int newDamage = stack.getDamageValue() + amount;
        if (newDamage <= stack.getMaxDamage()) {
            stack.setDamageValue(newDamage);
        } else {
            stack.setDamageValue(stack.getMaxDamage());
            player.sendSystemMessage(Component.translatable("item.broadcast_radio.walkie_talkie.power_low").withStyle(ChatFormatting.YELLOW));
        }
    }

    // ========== 悬浮提示（显示频段/电量） ==========
    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, level, tooltip, flag);
        initNBT(stack);

        // 获取NBT数据
        CompoundTag tag = stack.getTag();
        int frequency = tag.getInt(TAG_FREQUENCY);
        String password = tag.getString(TAG_PASSWORD);
        int power = stack.getMaxDamage() - stack.getDamageValue(); // 剩余电量

        // 添加悬浮提示
        tooltip.add(Component.empty());
        tooltip.add(Component.translatable("item.broadcast_radio.walkie_talkie.frequency", frequency).withStyle(ChatFormatting.BLUE));
        tooltip.add(Component.translatable("item.broadcast_radio.walkie_talkie.power", power).withStyle(ChatFormatting.GREEN));
        if (!password.isEmpty()) {
            tooltip.add(Component.translatable("item.broadcast_radio.walkie_talkie.encrypted").withStyle(ChatFormatting.RED));
        }
        tooltip.add(Component.translatable("item.broadcast_radio.portable_walkie_talkie.desc").withStyle(ChatFormatting.GRAY));
    }

    // ========== 通讯处理（收发消息） ==========
    @Mod.EventBusSubscriber(modid = BroadcastRadio.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
    public static class WalkieTalkieEvents {
        @SubscribeEvent
        public static void onServerChat(ServerChatEvent event) {
            ServerPlayer sender = event.getPlayer();
            ItemStack walkieStack = null;

            // 检查玩家是否正在使用对讲机
            if (!sender.getPersistentData().getBoolean(BroadcastRadio.MOD_ID + "_using_walkie")) {
                return;
            }

            // 找到玩家手中的对讲机
            for (InteractionHand hand : InteractionHand.values()) {
                ItemStack stack = sender.getItemInHand(hand);
                if (stack.getItem() instanceof PortableWalkieTalkieItem) {
                    walkieStack = stack;
                    break;
                }
            }
            if (walkieStack == null) {
                sender.getPersistentData().remove(BroadcastRadio.MOD_ID + "_using_walkie");
                return;
            }

            // 消耗发送电量
            consumePower(walkieStack, POWER_CONSUMPTION_SEND, sender);

            // 获取发送方频段/密码
            initNBT(walkieStack);
            CompoundTag senderTag = walkieStack.getTag();
            int senderFreq = senderTag.getInt(TAG_FREQUENCY);
            String senderPwd = senderTag.getString(TAG_PASSWORD);

            // 遍历所有玩家，发送同频段消息
            for (ServerPlayer target : sender.getServer().getPlayerList().getPlayers()) {
                if (target == sender || target instanceof FakePlayer) continue;

                // 检查目标是否持有对讲机
                for (ItemStack targetStack : target.getInventory().items) {
                    if (targetStack.getItem() instanceof PortableWalkieTalkieItem) {
                        initNBT(targetStack);
                        CompoundTag targetTag = targetStack.getTag();
                        int targetFreq = targetTag.getInt(TAG_FREQUENCY);
                        String targetPwd = targetTag.getString(TAG_PASSWORD);

                        if (targetFreq == senderFreq && targetPwd.equals(senderPwd)) {
                            Component message = Component.translatable(
                                    "item.broadcast_radio.walkie_talkie.message",
                                    sender.getName().getString(),
                                    senderFreq,
                                    event.getMessage()
                            ).withStyle(ChatFormatting.LIGHT_PURPLE);
                            target.sendSystemMessage(message);

                            // 目标待机耗电
                            consumePower(targetStack, POWER_CONSUMPTION_STANDBY, target);
                            break;
                        }
                    }
                }
            }

            // 取消原版聊天消息，只保留对讲机通讯
            event.setCanceled(true);
            sender.getPersistentData().remove(BroadcastRadio.MOD_ID + "_using_walkie");
        }
    }
}