package bili.dongsz.broadcastradio.item;

import bili.dongsz.broadcastradio.BroadcastRadio;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
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
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;

import javax.annotation.Nullable;
import java.util.List;

public class PortableWalkieTalkieItem extends Item {
    public static final String TAG_FREQUENCY = "Frequency"; // 频率（MHz）
    public static final String TAG_PASSWORD = "Password"; // 加密密码（默认空）
    public static final float DEFAULT_FREQUENCY = 433.0f; // 默认频率433MHz（常用民用频段）
    public static final int POWER_CONSUMPTION_SEND = 1; // 发送消息耗电
    public static final int POWER_CONSUMPTION_STANDBY = 1; // 待机每60秒耗电
    public static final float MIN_FREQ = 1.0f;      // 最小频率（MHz）
    public static final float MAX_FREQ = 999.9f;    // 最大频率（MHz）
    public static final float FREQ_STEP_LARGE = 5.0f;   // 大幅度调频步长（5MHz）
    public static final float FREQ_STEP_SMALL = 0.1f;   // 小幅度调频步长（0.1MHz）
    public static final int COMMUNICATION_RANGE = 128;   // 通讯距离（128格）
    public static final int POWER_CONSUMPTION_SWITCH = 1;   // 调整频率耗电
    public static final String TAG_SETTING_PWD = "SettingPassword"; // 设置密码标记

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
            tag.putFloat(TAG_FREQUENCY, DEFAULT_FREQUENCY);
        }
        if (!tag.contains(TAG_PASSWORD)) {
            tag.putString(TAG_PASSWORD, "");
        }
        if (!tag.contains(TAG_SETTING_PWD)) {
            tag.putBoolean(TAG_SETTING_PWD, false);
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


        // Shift+右键：大幅度加频（+5MHz）
        if (player.isShiftKeyDown() && !InputConstants.isKeyDown(Minecraft.getInstance().getWindow().getWindow(), InputConstants.KEY_LCONTROL)) {
            if (!level.isClientSide) {
                CompoundTag tag = stack.getTag();
                float currentFreq = tag.getFloat(TAG_FREQUENCY);
                float newFreq = currentFreq + FREQ_STEP_LARGE; // 加5MHz

                // 边界校验
                if (newFreq > MAX_FREQ) newFreq = MIN_FREQ;
                else if (newFreq < MIN_FREQ) newFreq = MAX_FREQ;

                tag.putFloat(TAG_FREQUENCY, newFreq);
                stack.setTag(tag);

                player.level().playSound(null, player.blockPosition(), SoundEvents.LEVER_CLICK, SoundSource.PLAYERS, 0.5F, 1.0F);
                consumePower(stack, POWER_CONSUMPTION_SWITCH, player);
                player.sendSystemMessage(Component.translatable("item.broadcast_radio.walkie_talkie.freq_adjust", String.format("%.1f", newFreq)).withStyle(ChatFormatting.BLUE));
            }
            return InteractionResultHolder.success(stack);
        }

        // Ctrl+右键：大幅度减频（-5MHz）
        if (InputConstants.isKeyDown(Minecraft.getInstance().getWindow().getWindow(), InputConstants.KEY_LCONTROL) && !player.isShiftKeyDown()) {
            if (!level.isClientSide) {
                CompoundTag tag = stack.getTag();
                float currentFreq = tag.getFloat(TAG_FREQUENCY);
                float newFreq = currentFreq - FREQ_STEP_LARGE; // 减5MHz

                // 边界校验
                if (newFreq < MIN_FREQ) newFreq = MAX_FREQ;
                else if (newFreq > MAX_FREQ) newFreq = MIN_FREQ;

                tag.putFloat(TAG_FREQUENCY, newFreq);
                stack.setTag(tag);

                player.level().playSound(null, player.blockPosition(), SoundEvents.LEVER_CLICK, SoundSource.PLAYERS, 0.5F, 1.0F);
                consumePower(stack, POWER_CONSUMPTION_SWITCH, player);
                player.sendSystemMessage(Component.translatable("item.broadcast_radio.walkie_talkie.freq_adjust", String.format("%.1f", newFreq)).withStyle(ChatFormatting.BLUE));
            }
            return InteractionResultHolder.success(stack);
        }

        // 3. Shift+Ctrl+右键：小幅度加频（+0.1MHz）
        if (player.isShiftKeyDown() && InputConstants.isKeyDown(Minecraft.getInstance().getWindow().getWindow(), InputConstants.KEY_LCONTROL)) {
            if (!level.isClientSide) {
                CompoundTag tag = stack.getTag();
                float currentFreq = tag.getFloat(TAG_FREQUENCY);
                float newFreq = currentFreq + FREQ_STEP_SMALL; // 加0.1MHz

                // 边界校验
                if (newFreq > MAX_FREQ) newFreq = MIN_FREQ;
                else if (newFreq < MIN_FREQ) newFreq = MAX_FREQ;

                tag.putFloat(TAG_FREQUENCY, newFreq);
                stack.setTag(tag);

                player.level().playSound(null, player.blockPosition(), SoundEvents.LEVER_CLICK, SoundSource.PLAYERS, 0.5F, 1.2F);
                consumePower(stack, POWER_CONSUMPTION_SWITCH, player);
                player.sendSystemMessage(Component.translatable("item.broadcast_radio.walkie_talkie.freq_adjust", String.format("%.1f", newFreq)).withStyle(ChatFormatting.BLUE));
            }
            return InteractionResultHolder.success(stack);
        }

        // 普通右键：发送消息
        if (!level.isClientSide && player instanceof ServerPlayer serverPlayer) {
            CompoundTag tag = stack.getOrCreateTag();
            serverPlayer.sendSystemMessage(Component.translatable("item.broadcast_radio.walkie_talkie.input_hint").withStyle(ChatFormatting.YELLOW));
            serverPlayer.getPersistentData().putBoolean(BroadcastRadio.MOD_ID + "_using_walkie", true);
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

    // ========== 悬浮提示 ==========
    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, level, tooltip, flag);
        initNBT(stack);

        // 获取NBT数据
        CompoundTag tag = stack.getTag();
        float frequency = tag.getFloat(TAG_FREQUENCY);
        String password = tag.getString(TAG_PASSWORD);
        int power = stack.getMaxDamage() - stack.getDamageValue(); // 剩余电量

        // 添加悬浮提示
        tooltip.add(Component.empty());
        tooltip.add(Component.translatable("item.broadcast_radio.walkie_talkie.frequency", String.format("%.1f", frequency)).withStyle(ChatFormatting.BLUE));
        tooltip.add(Component.translatable("item.broadcast_radio.walkie_talkie.power", power).withStyle(ChatFormatting.GREEN));
        if (!password.isEmpty()) {
            tooltip.add(Component.translatable("item.broadcast_radio.walkie_talkie.encrypted", password.length()).withStyle(ChatFormatting.RED));
        }

        // 更新操作提示
        tooltip.add(Component.translatable("item.broadcast_radio.walkie_talkie.new_operate_hint")
                .withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("item.broadcast_radio.portable_walkie_talkie.desc")
                .withStyle(ChatFormatting.GRAY));
    }

    // ========== 通讯处理 ==========
    @Mod.EventBusSubscriber(modid = BroadcastRadio.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
    public static class WalkieTalkieEvents {
        @SubscribeEvent
        public static void onServerChat(ServerChatEvent event) {
            ServerPlayer sender = event.getPlayer();
            ItemStack walkieStack = null;

            // 处理密码设置
            if (sender.getPersistentData().getBoolean(BroadcastRadio.MOD_ID + "_setting_pwd")) {
                for (InteractionHand hand : InteractionHand.values()) {
                    ItemStack stack = sender.getItemInHand(hand);
                    if (stack.getItem() instanceof PortableWalkieTalkieItem) {
                        walkieStack = stack;
                        break;
                    }
                }
                if (walkieStack != null) {
                    initNBT(walkieStack);
                    CompoundTag tag = walkieStack.getOrCreateTag();
                    String newPwd = String.valueOf(event.getMessage());
                    tag.putString(TAG_PASSWORD, newPwd);
                    walkieStack.setTag(tag);
                    sender.sendSystemMessage(Component.translatable("item.broadcast_radio.walkie_talkie.pwd_set_success", newPwd.length()).withStyle(ChatFormatting.GREEN));
                    consumePower(walkieStack, POWER_CONSUMPTION_STANDBY, sender);
                }
                sender.getPersistentData().remove(BroadcastRadio.MOD_ID + "_setting_pwd");
                event.setCanceled(true);
                return;
            }

            // 处理消息发送
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

            // 获取发送方频率/密码
            initNBT(walkieStack);
            CompoundTag senderTag = walkieStack.getTag();
            float senderFreq = senderTag.getFloat(TAG_FREQUENCY);
            String senderPwd = senderTag.getString(TAG_PASSWORD);

            // 发送者自身显示消息
            Component selfMessage = Component.translatable(
                    "item.broadcast_radio.walkie_talkie.self_message",
                    String.format("%.1f", senderFreq),
                    event.getMessage()
            ).withStyle(ChatFormatting.GREEN);
            sender.sendSystemMessage(selfMessage);

            // 遍历所有玩家，匹配同频率
            for (ServerPlayer target : sender.getServer().getPlayerList().getPlayers()) {
                if (target == sender || target instanceof FakePlayer) continue;

                // 距离检测
                double distance = sender.distanceToSqr(target);
                if (distance > COMMUNICATION_RANGE * COMMUNICATION_RANGE) {
                    continue;
                }

                // 检查目标对讲机频率匹配
                boolean hasValidWalkie = false;
                for (ItemStack targetStack : target.getInventory().items) {
                    if (targetStack.getItem() instanceof PortableWalkieTalkieItem) {
                        initNBT(targetStack);
                        CompoundTag targetTag = targetStack.getTag();
                        float targetFreq = targetTag.getFloat(TAG_FREQUENCY);
                        String targetPwd = targetTag.getString(TAG_PASSWORD);

                        // 精确匹配浮点频率（保留1位小数避免精度问题）
                        if (Math.abs(targetFreq - senderFreq) < 0.01f && targetPwd.equals(senderPwd)) {
                            Component message = Component.translatable(
                                    "item.broadcast_radio.walkie_talkie.message",
                                    sender.getName().getString(),
                                    String.format("%.1f", senderFreq),
                                    event.getMessage()
                            ).withStyle(ChatFormatting.LIGHT_PURPLE);
                            target.sendSystemMessage(message);

                            consumePower(targetStack, POWER_CONSUMPTION_STANDBY, target);
                            hasValidWalkie = true;
                            break;
                        }
                    }
                }
            }

            event.setCanceled(true);
            sender.getPersistentData().remove(BroadcastRadio.MOD_ID + "_using_walkie");
        }
    }
}