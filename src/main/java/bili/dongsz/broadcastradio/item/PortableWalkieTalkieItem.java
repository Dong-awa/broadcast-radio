package bili.dongsz.broadcastradio.item;

import bili.dongsz.broadcastradio.BroadcastRadio;
import bili.dongsz.broadcastradio.menu.WalkieTalkieMenu;
import bili.dongsz.broadcastradio.utils.CommunicationUtils;
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
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import javax.annotation.Nullable;
import java.util.List;
import net.minecraft.world.SimpleMenuProvider;

public class PortableWalkieTalkieItem extends Item {
    public static final String TAG_FREQUENCY = "Frequency";
    public static final String TAG_PASSWORD = "Password";
    public static final String TAG_INTERFERENCE = "Interference";
    public static final String TAG_LAST_CONSUME_TIME = "LastConsumeTime";
    public static final float DEFAULT_FREQUENCY = 433.0f;
    public static final int DEFAULT_INTERFERENCE = 0;
    public static final int POWER_CONSUMPTION_SEND = 1;
    public static final float POWER_CONSUMPTION_STANDBY = 0.5f;
    public static final int STANDBY_CONSUME_INTERVAL = 90;
    public static final int STANDBY_CONSUME_AMOUNT = 1;
    public static final float MIN_FREQ = 1.0f;
    public static final float MAX_FREQ = 999.9f;
    public static final float FREQ_STEP_LARGE = 5.0f;
    public static final float FREQ_STEP_SMALL = 0.1f;
    public static final double COMMUNICATION_RANGE = 256.0;
    public static final int POWER_CONSUMPTION_SWITCH = 1;
    public static final String TAG_SETTING_PWD = "SettingPassword";

    public PortableWalkieTalkieItem(Properties pProperties) {
        super(pProperties);
    }
    
    @Override
    public boolean isBarVisible(ItemStack stack) {
        CompoundTag tag = stack.getOrCreateTag();
        if (tag.contains("Battery")) {
            CompoundTag batteryTag = tag.getCompound("Battery");
            ItemStack battery = ItemStack.of(batteryTag);
            if (!battery.isEmpty() && battery.getItem() instanceof RadioBatteryItem) {
                int power = RadioBatteryItem.getPower(battery);
                return power < RadioBatteryItem.MAX_POWER;
            }
        }
        return false;
    }
    
    @Override
    public int getBarWidth(ItemStack stack) {
        CompoundTag tag = stack.getOrCreateTag();
        if (tag.contains("Battery")) {
            CompoundTag batteryTag = tag.getCompound("Battery");
            ItemStack battery = ItemStack.of(batteryTag);
            if (!battery.isEmpty() && battery.getItem() instanceof RadioBatteryItem) {
                int power = RadioBatteryItem.getPower(battery);
                return (int) (13.0 * power / RadioBatteryItem.MAX_POWER);
            }
        }
        return 0;
    }
    
    @Override
    public int getBarColor(ItemStack stack) {
        CompoundTag tag = stack.getOrCreateTag();
        if (tag.contains("Battery")) {
            CompoundTag batteryTag = tag.getCompound("Battery");
            ItemStack battery = ItemStack.of(batteryTag);
            if (!battery.isEmpty() && battery.getItem() instanceof RadioBatteryItem) {
                int power = RadioBatteryItem.getPower(battery);
                if (power > 60) return 0x4CAF50;
                if (power > 30) return 0xFFC107;
                return 0xF44336;
            }
        }
        return 0x4CAF50;
    }

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

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        initNBT(stack);
        
        CompoundTag tag = stack.getOrCreateTag();
        long currentTime = level.getGameTime();
        long lastConsumeTime = tag.getLong(TAG_LAST_CONSUME_TIME);
        
        if (currentTime - lastConsumeTime >= STANDBY_CONSUME_INTERVAL * 20) {
            consumePower(stack, STANDBY_CONSUME_AMOUNT, player);
            tag.putLong(TAG_LAST_CONSUME_TIME, currentTime);
            stack.setTag(tag);
        }

        if (player.isShiftKeyDown()) {
            if (!level.isClientSide) {
                player.openMenu(new SimpleMenuProvider(
                    (containerId, playerInventory, playerEntity) -> new WalkieTalkieMenu(containerId, playerInventory, stack),
                    Component.translatable("item.broadcast_radio.walkie_talkie.gui_title")
                ));
            }
            return InteractionResultHolder.success(stack);
        }

        if (!level.isClientSide && player instanceof ServerPlayer serverPlayer) {
            serverPlayer.sendSystemMessage(Component.translatable("item.broadcast_radio.walkie_talkie.input_hint").withStyle(ChatFormatting.YELLOW));
            serverPlayer.getPersistentData().putBoolean(BroadcastRadio.MOD_ID + "_using_walkie", true);
        }
        return InteractionResultHolder.success(stack);
    }

    public static void consumePower(ItemStack stack, float amount, Player player) {
        ItemStack batteryStack = ItemStack.EMPTY;
        if (player.containerMenu instanceof WalkieTalkieMenu) {
            WalkieTalkieMenu menu = (WalkieTalkieMenu) player.containerMenu;
            batteryStack = menu.getBattery();
        } else {
            CompoundTag tag = stack.getOrCreateTag();
            if (tag.contains("Battery")) {
                CompoundTag batteryTag = tag.getCompound("Battery");
                batteryStack = ItemStack.of(batteryTag);
            }
        }
        
        if (!batteryStack.isEmpty() && batteryStack.getItem() instanceof RadioBatteryItem) {
            int currentPower = RadioBatteryItem.getPower(batteryStack);
            boolean wasEmpty = (currentPower == 0);
            
            RadioBatteryItem.consumePower(batteryStack, (int) amount);
            
            int newPower = RadioBatteryItem.getPower(batteryStack);
            boolean isEmpty = (newPower == 0);
            
            if (!wasEmpty && isEmpty) {
                player.sendSystemMessage(Component.translatable("item.broadcast_radio.walkie_talkie.no_power").withStyle(ChatFormatting.RED));
            } else if (!wasEmpty && !isEmpty && newPower <= 20 && currentPower > 20) {
                player.sendSystemMessage(Component.translatable("item.broadcast_radio.walkie_talkie.power_low").withStyle(ChatFormatting.YELLOW));
            }
            
            if (newPower == 0) {
                if (player.containerMenu instanceof WalkieTalkieMenu) {
                    WalkieTalkieMenu menu = (WalkieTalkieMenu) player.containerMenu;
                    menu.getBattery().shrink(1);
                } else {
                    CompoundTag tag = stack.getOrCreateTag();
                    tag.remove("Battery");
                    stack.setTag(tag);
                }
            } else {
                if (!(player.containerMenu instanceof WalkieTalkieMenu)) {
                    CompoundTag tag = stack.getOrCreateTag();
                    CompoundTag batteryTag = new CompoundTag();
                    batteryStack.save(batteryTag);
                    tag.put("Battery", batteryTag);
                    stack.setTag(tag);
                }
            }
        }
    }
    
    // 检查电池供电
    public static boolean hasBatteryPower(Player player) {
        if (player.containerMenu instanceof WalkieTalkieMenu) {
            WalkieTalkieMenu menu = (WalkieTalkieMenu) player.containerMenu;
            ItemStack batteryStack = menu.getBattery();
            if (!batteryStack.isEmpty() && batteryStack.getItem() instanceof RadioBatteryItem) {
                return RadioBatteryItem.getPower(batteryStack) > 0;
            }
        } else {
            for (InteractionHand hand : InteractionHand.values()) {
                ItemStack stack = player.getItemInHand(hand);
                if (stack.getItem() instanceof PortableWalkieTalkieItem) {
                    CompoundTag tag = stack.getOrCreateTag();
                    if (tag.contains("Battery")) {
                        CompoundTag batteryTag = tag.getCompound("Battery");
                        ItemStack battery = ItemStack.of(batteryTag);
                        if (!battery.isEmpty() && battery.getItem() instanceof RadioBatteryItem) {
                            return RadioBatteryItem.getPower(battery) > 0;
                        }
                    }
                }
            }
        }
        return false;
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, level, tooltip, flag);
        initNBT(stack);

        // 获取NBT
        CompoundTag tag = stack.getTag();
        float frequency = tag.getFloat(TAG_FREQUENCY);
        String password = tag.getString(TAG_PASSWORD);
        
        // 获取电量
        int power = 0;
        if (tag.contains("Battery")) {
            CompoundTag batteryTag = tag.getCompound("Battery");
            ItemStack battery = ItemStack.of(batteryTag);
            if (!battery.isEmpty() && battery.getItem() instanceof RadioBatteryItem) {
                power = RadioBatteryItem.getPower(battery);
            }
        }

        tooltip.add(Component.empty());
        tooltip.add(Component.translatable("item.broadcast_radio.walkie_talkie.frequency", String.format("%.1f", frequency)).withStyle(ChatFormatting.BLUE));
        tooltip.add(Component.translatable("item.broadcast_radio.walkie_talkie.power", power).withStyle(ChatFormatting.GREEN));
        if (!password.isEmpty()) {
            tooltip.add(Component.translatable("item.broadcast_radio.walkie_talkie.encrypted", password.length()).withStyle(ChatFormatting.RED));
        }

        tooltip.add(Component.translatable("item.broadcast_radio.portable_walkie_talkie.desc")
                .withStyle(ChatFormatting.GRAY));
    }

    @Mod.EventBusSubscriber(modid = BroadcastRadio.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
    public static class WalkieTalkieEvents {
        @SubscribeEvent
        public static void onServerChat(ServerChatEvent event) {
            ServerPlayer sender = event.getPlayer();
            ItemStack walkieStack = null;

            // 处理密码
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
                }
                sender.getPersistentData().remove(BroadcastRadio.MOD_ID + "_setting_pwd");
                event.setCanceled(true);
                return;
            }

            // 发送消息
            if (!sender.getPersistentData().getBoolean(BroadcastRadio.MOD_ID + "_using_walkie")) {
                return;
            }

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

            // 检查是否有电池
            if (!hasBatteryPower(sender)) {
                sender.sendSystemMessage(Component.translatable("item.broadcast_radio.walkie_talkie.no_power").withStyle(ChatFormatting.RED));
                sender.getPersistentData().remove(BroadcastRadio.MOD_ID + "_using_walkie");
                return;
            }
            consumePower(walkieStack, POWER_CONSUMPTION_SEND, sender);

            initNBT(walkieStack);
            CompoundTag senderTag = walkieStack.getTag();
            float senderFreq = senderTag.getFloat(TAG_FREQUENCY);
            String senderPwd = senderTag.getString(TAG_PASSWORD);
            int senderNBTInterference = senderTag.getInt(TAG_INTERFERENCE);
            int jammerAtSender = CommunicationUtils.getJammerInterference(sender.level(), sender.blockPosition(), senderFreq);
            int senderEffectiveInterference = Math.max(senderNBTInterference, jammerAtSender);
            int weatherAtSender = CommunicationUtils.getWeatherInterference(sender.level());
            senderEffectiveInterference = CommunicationUtils.clampInterference(senderEffectiveInterference + weatherAtSender);

            String messageContent = event.getMessage().getString();
            Component selfMessage = Component.translatable(
                    "item.broadcast_radio.walkie_talkie.self_message",
                    String.format("%.1f", senderFreq),
                    messageContent
            ).withStyle(ChatFormatting.GREEN);
            sender.sendSystemMessage(selfMessage);

            for (ServerPlayer target : sender.getServer().getPlayerList().getPlayers()) {
                if (target instanceof FakePlayer) continue;
                if (!CommunicationUtils.canSignalReachEye(sender.level(), sender, target, COMMUNICATION_RANGE,
                        sender.getName().getString(), target.getName().getString(), sender)) {
                    continue;
                }
                if (target != sender) {
                    checkPlayerWalkieTalkie(target, sender.getName().getString(), senderFreq, senderPwd, messageContent, senderEffectiveInterference, sender.level());
                }
                CommunicationUtils.checkPlayerNearRadio(target, sender, COMMUNICATION_RANGE, sender.getName().getString(), senderFreq, senderPwd, messageContent, senderEffectiveInterference, sender);
            }
            event.setCanceled(true);
            sender.getPersistentData().remove(BroadcastRadio.MOD_ID + "_using_walkie");
        }
        
        // 检查玩家是否持有同频率的对讲机
        private static boolean checkPlayerWalkieTalkie(ServerPlayer target, String senderName, float senderFreq, String senderPwd, String messageContent, int senderInterference, Level level) {
            int jammerAtTarget = CommunicationUtils.getJammerInterference(target.level(), target.blockPosition(), senderFreq);
            int effectiveSenderInterference = Math.max(senderInterference, jammerAtTarget);

            ItemStack mainHandStack = target.getMainHandItem();
            if (checkWalkieTalkieFrequency(mainHandStack, target, senderName, senderFreq, senderPwd, messageContent, effectiveSenderInterference, level)) {
                return true;
            }
            ItemStack offHandStack = target.getOffhandItem();
            if (checkWalkieTalkieFrequency(offHandStack, target, senderName, senderFreq, senderPwd, messageContent, effectiveSenderInterference, level)) {
                return true;
            }
            for (ItemStack targetStack : target.getInventory().items) {
                if (checkWalkieTalkieFrequency(targetStack, target, senderName, senderFreq, senderPwd, messageContent, effectiveSenderInterference, level)) {
                    return true;
                }
            }

            return false;
        }

        private static boolean checkWalkieTalkieFrequency(ItemStack stack, ServerPlayer target, String senderName, float senderFreq, String senderPwd, String messageContent, int senderInterference, Level level) {
            if (stack.getItem() instanceof PortableWalkieTalkieItem) {
                initNBT(stack);
                CompoundTag targetTag = stack.getTag();
                float targetFreq = targetTag.getFloat(TAG_FREQUENCY);
                String targetPwd = targetTag.getString(TAG_PASSWORD);
                int targetNBTInterference = targetTag.getInt(TAG_INTERFERENCE);

                if (CommunicationUtils.isFrequencyMatch(targetFreq, senderFreq) && targetPwd.equals(senderPwd)) {
                    int totalInterference = Math.max(senderInterference, targetNBTInterference);
                    String displayMessage = CommunicationUtils.applyInterference(messageContent, totalInterference, level);
                    CommunicationUtils.sendMessageToPlayer(target, senderName, senderFreq, displayMessage);
                    return true;
                }
            }
            return false;
        }
    }
}