package bili.dongsz.broadcastradio.item;

import bili.dongsz.broadcastradio.BroadcastRadio;
import bili.dongsz.broadcastradio.menu.WalkieTalkieMenu;
import bili.dongsz.broadcastradio.utils.CommunicationUtils;
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
import net.minecraft.world.SimpleMenuProvider;

public class PortableWalkieTalkieItem extends Item {
    public static final String TAG_FREQUENCY = "Frequency";
    public static final String TAG_PASSWORD = "Password";
    public static final String TAG_INTERFERENCE = "Interference";
    public static final float DEFAULT_FREQUENCY = 433.0f;
    public static final int DEFAULT_INTERFERENCE = 0;
    public static final int POWER_CONSUMPTION_SEND = 1;
    public static final float POWER_CONSUMPTION_STANDBY = 0.5f;
    public static final float MIN_FREQ = 1.0f;
    public static final float MAX_FREQ = 999.9f;
    public static final float FREQ_STEP_LARGE = 5.0f;
    public static final float FREQ_STEP_SMALL = 0.1f;
    public static final int COMMUNICATION_RANGE = 128;
    public static final int POWER_CONSUMPTION_SWITCH = 1;
    public static final String TAG_SETTING_PWD = "SettingPassword";

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
        if (!tag.contains(TAG_INTERFERENCE)) {
            tag.putInt(TAG_INTERFERENCE, DEFAULT_INTERFERENCE);
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

        // Shift+右键：打开调频GUI
        if (player.isShiftKeyDown()) {
            if (!level.isClientSide) {
                player.openMenu(new SimpleMenuProvider(
                    (containerId, playerInventory, playerEntity) -> new WalkieTalkieMenu(containerId, playerInventory, stack),
                    Component.translatable("item.broadcast_radio.walkie_talkie.gui_title")
                ));
                consumePower(stack, POWER_CONSUMPTION_SWITCH, player);
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

    // ========== 电量消耗逻辑 ==========
    public static void consumePower(ItemStack stack, float amount, Player player) {
        int currentDamage = stack.getDamageValue();
        float newDamage = currentDamage + amount;
        
        // 检查当前电量状态
        boolean wasEmpty = (currentDamage >= stack.getMaxDamage());
        
        // 更新电量
        if (newDamage <= stack.getMaxDamage()) {
            stack.setDamageValue((int) newDamage);
        } else {
            stack.setDamageValue(stack.getMaxDamage());
        }
        
        // 检查新的电量状态
        boolean isEmpty = (stack.getDamageValue() >= stack.getMaxDamage());
        
        // 只有当电量状态发生变化时才发送提醒
        if (!wasEmpty && isEmpty) {
            // 当电量从非耗尽状态变为耗尽状态时发送没电提醒
            player.sendSystemMessage(Component.translatable("item.broadcast_radio.walkie_talkie.no_power").withStyle(ChatFormatting.RED));
        } else if (!wasEmpty && !isEmpty && newDamage > stack.getMaxDamage() * 0.8 && currentDamage <= stack.getMaxDamage() * 0.8) {
            // 当电量从高于20%变为低于20%时发送低电量提醒
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

            // 获取发送方频率/密码/干扰值
            initNBT(walkieStack);
            CompoundTag senderTag = walkieStack.getTag();
            float senderFreq = senderTag.getFloat(TAG_FREQUENCY);
            String senderPwd = senderTag.getString(TAG_PASSWORD);
            int senderInterference = senderTag.getInt(TAG_INTERFERENCE);

            // 获取消息内容
            String messageContent = event.getMessage().getString();
            
            // 发送者自身显示消息
            Component selfMessage = Component.translatable(
                    "item.broadcast_radio.walkie_talkie.self_message",
                    String.format("%.1f", senderFreq),
                    messageContent
            ).withStyle(ChatFormatting.GREEN);
            sender.sendSystemMessage(selfMessage);

            // 遍历所有玩家，匹配同频率
            for (ServerPlayer target : sender.getServer().getPlayerList().getPlayers()) {
                if (target instanceof FakePlayer) continue;

                // 距离检测
                double distance = sender.distanceToSqr(target);
                if (distance > COMMUNICATION_RANGE * COMMUNICATION_RANGE) {
                    continue;
                }

                // 检查目标对讲机频率匹配
                if (target != sender) {
                    checkPlayerWalkieTalkie(target, sender.getName().getString(), senderFreq, senderPwd, messageContent, senderInterference, sender.level());
                }
                
                // 检查目标是否在收音机的范围内，并且收音机频率匹配
                CommunicationUtils.checkPlayerNearRadio(target, sender.getName().getString(), senderFreq, senderPwd, messageContent);
            }
            
            event.setCanceled(true);
            sender.getPersistentData().remove(BroadcastRadio.MOD_ID + "_using_walkie");
        }
        
        // 检查玩家是否持有同频率的对讲机
        private static boolean checkPlayerWalkieTalkie(ServerPlayer target, String senderName, float senderFreq, String senderPwd, String messageContent, int senderInterference, Level level) {
            // 检查主手
            ItemStack mainHandStack = target.getMainHandItem();
            if (checkWalkieTalkieFrequency(mainHandStack, target, senderName, senderFreq, senderPwd, messageContent, senderInterference, level)) {
                return true;
            }
            
            // 检查副手
            ItemStack offHandStack = target.getOffhandItem();
            if (checkWalkieTalkieFrequency(offHandStack, target, senderName, senderFreq, senderPwd, messageContent, senderInterference, level)) {
                return true;
            }
            
            // 检查物品栏
            for (ItemStack targetStack : target.getInventory().items) {
                if (checkWalkieTalkieFrequency(targetStack, target, senderName, senderFreq, senderPwd, messageContent, senderInterference, level)) {
                    return true;
                }
            }
            
            return false;
        }
        
        // 检查单个对讲机的频率是否匹配
        private static boolean checkWalkieTalkieFrequency(ItemStack stack, ServerPlayer target, String senderName, float senderFreq, String senderPwd, String messageContent, int senderInterference, Level level) {
            if (stack.getItem() instanceof PortableWalkieTalkieItem) {
                initNBT(stack);
                CompoundTag targetTag = stack.getTag();
                float targetFreq = targetTag.getFloat(TAG_FREQUENCY);
                String targetPwd = targetTag.getString(TAG_PASSWORD);
                int targetInterference = targetTag.getInt(TAG_INTERFERENCE);
                
                if (CommunicationUtils.isFrequencyMatch(targetFreq, senderFreq) && targetPwd.equals(senderPwd)) {
                    int totalInterference = Math.max(senderInterference, targetInterference);
                    String displayMessage = CommunicationUtils.applyInterference(messageContent, totalInterference, level);
                    CommunicationUtils.sendMessageToPlayer(target, senderName, senderFreq, displayMessage);
                    consumePower(stack, POWER_CONSUMPTION_STANDBY, target);
                    return true;
                }
            }
            return false;
        }
    }
}