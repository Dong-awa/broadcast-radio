package bili.dongsz.broadcastradio.item;

import bili.dongsz.broadcastradio.BroadcastRadio;
import bili.dongsz.broadcastradio.menu.EncryptedWalkieTalkieMenu;
import bili.dongsz.broadcastradio.registry.ModMenus;
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

public class EncryptedWalkieTalkieItem extends Item {
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
    public static final int COMMUNICATION_RANGE = 512;
    public static final int POWER_CONSUMPTION_SWITCH = 1;

    public EncryptedWalkieTalkieItem(Properties pProperties) {
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
            initNBT(stack);
        }
        return super.finishUsingItem(stack, level, livingEntity);
    }

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
                    (containerId, playerInventory, playerEntity) -> new EncryptedWalkieTalkieMenu(containerId, playerInventory, stack),
                    Component.translatable("item.broadcast_radio.encrypted_walkie_talkie.gui_title")
                ));
            }
            return InteractionResultHolder.success(stack);
        }

        if (!level.isClientSide && player instanceof ServerPlayer serverPlayer) {
            serverPlayer.sendSystemMessage(Component.translatable("item.broadcast_radio.walkie_talkie.input_hint").withStyle(ChatFormatting.YELLOW));
            serverPlayer.getPersistentData().putBoolean(BroadcastRadio.MOD_ID + "_using_encrypted_walkie", true);
        }
        return InteractionResultHolder.success(stack);
    }

    public static void consumePower(ItemStack stack, float amount, Player player) {
        ItemStack batteryStack = ItemStack.EMPTY;
        if (player.containerMenu instanceof EncryptedWalkieTalkieMenu) {
            EncryptedWalkieTalkieMenu menu = (EncryptedWalkieTalkieMenu) player.containerMenu;
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
                if (player.containerMenu instanceof EncryptedWalkieTalkieMenu) {
                    EncryptedWalkieTalkieMenu menu = (EncryptedWalkieTalkieMenu) player.containerMenu;
                    menu.getBattery().shrink(1);
                } else {
                    CompoundTag tag = stack.getOrCreateTag();
                    tag.remove("Battery");
                    stack.setTag(tag);
                }
            } else {
                if (!(player.containerMenu instanceof EncryptedWalkieTalkieMenu)) {
                    CompoundTag tag = stack.getOrCreateTag();
                    CompoundTag batteryTag = new CompoundTag();
                    batteryStack.save(batteryTag);
                    tag.put("Battery", batteryTag);
                    stack.setTag(tag);
                }
            }
        }
    }
    
    public static boolean hasBatteryPower(Player player) {
        if (player.containerMenu instanceof EncryptedWalkieTalkieMenu) {
            EncryptedWalkieTalkieMenu menu = (EncryptedWalkieTalkieMenu) player.containerMenu;
            ItemStack batteryStack = menu.getBattery();
            if (!batteryStack.isEmpty() && batteryStack.getItem() instanceof RadioBatteryItem) {
                return RadioBatteryItem.getPower(batteryStack) > 0;
            }
        } else {
            for (InteractionHand hand : InteractionHand.values()) {
                ItemStack stack = player.getItemInHand(hand);
                if (stack.getItem() instanceof EncryptedWalkieTalkieItem) {
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

        CompoundTag tag = stack.getTag();
        float frequency = tag.getFloat(TAG_FREQUENCY);
        String password = tag.getString(TAG_PASSWORD);
        
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
        tooltip.add(Component.translatable("item.broadcast_radio.encrypted_walkie_talkie.desc")
                .withStyle(ChatFormatting.GRAY));
    }

    @Mod.EventBusSubscriber(modid = BroadcastRadio.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
    public static class EncryptedWalkieTalkieEvents {
        @SubscribeEvent
        public static void onServerChat(ServerChatEvent event) {
            ServerPlayer sender = event.getPlayer();
            ItemStack walkieStack = null;

            if (!sender.getPersistentData().getBoolean(BroadcastRadio.MOD_ID + "_using_encrypted_walkie")) {
                return;
            }

            for (InteractionHand hand : InteractionHand.values()) {
                ItemStack stack = sender.getItemInHand(hand);
                if (stack.getItem() instanceof EncryptedWalkieTalkieItem) {
                    walkieStack = stack;
                    break;
                }
            }
            if (walkieStack == null) {
                sender.getPersistentData().remove(BroadcastRadio.MOD_ID + "_using_encrypted_walkie");
                return;
            }

            // ÃƒÂ¦Ã‚Â£Ã¢â€šÂ¬ÃƒÂ¦Ã…Â¸Ã‚Â¥ÃƒÂ¦Ã‹Å“Ã‚Â¯ÃƒÂ¥Ã‚ÂÃ‚Â¦ÃƒÂ¦Ã…â€œÃ¢â‚¬Â°ÃƒÂ§Ã¢â‚¬ÂÃ‚ÂµÃƒÂ¦Ã‚Â±Ã‚Â ÃƒÂ¤Ã‚Â¾Ã¢â‚¬ÂºÃƒÂ§Ã¢â‚¬ÂÃ‚Âµ
            if (!hasBatteryPower(sender)) {
                sender.sendSystemMessage(Component.translatable("item.broadcast_radio.walkie_talkie.no_power").withStyle(ChatFormatting.RED));
                sender.getPersistentData().remove(BroadcastRadio.MOD_ID + "_using_encrypted_walkie");
                return;
            }

            consumePower(walkieStack, POWER_CONSUMPTION_SEND, sender);

            initNBT(walkieStack);
            CompoundTag senderTag = walkieStack.getTag();
            float senderFreq = senderTag.getFloat(TAG_FREQUENCY);
            String senderPwd = senderTag.getString(TAG_PASSWORD);
            int senderInterference = senderTag.getInt(TAG_INTERFERENCE);

            String messageContent = event.getMessage().getString();
            
            Component selfMessage = Component.translatable(
                    "item.broadcast_radio.walkie_talkie.self_message",
                    String.format("%.1f", senderFreq),
                    messageContent
            ).withStyle(ChatFormatting.GREEN);
            sender.sendSystemMessage(selfMessage);

            for (ServerPlayer target : sender.getServer().getPlayerList().getPlayers()) {
                if (target instanceof FakePlayer) continue;

                double distance = sender.distanceToSqr(target);
                if (distance > COMMUNICATION_RANGE * COMMUNICATION_RANGE) {
                    continue;
                }

                if (target != sender) {
                    checkPlayerEncryptedWalkieTalkie(target, sender.getName().getString(), senderFreq, senderPwd, messageContent, senderInterference, sender.level());
                }
                
                CommunicationUtils.checkPlayerNearRadio(target, sender.getName().getString(), senderFreq, senderPwd, messageContent);
            }
            
            event.setCanceled(true);
            sender.getPersistentData().remove(BroadcastRadio.MOD_ID + "_using_encrypted_walkie");
        }
        
        private static boolean checkPlayerEncryptedWalkieTalkie(ServerPlayer target, String senderName, float senderFreq, String senderPwd, String messageContent, int senderInterference, Level level) {
            ItemStack mainHandStack = target.getMainHandItem();
            if (checkEncryptedWalkieTalkieFrequency(mainHandStack, target, senderName, senderFreq, senderPwd, messageContent, senderInterference, level)) {
                return true;
            }
            
            ItemStack offHandStack = target.getOffhandItem();
            if (checkEncryptedWalkieTalkieFrequency(offHandStack, target, senderName, senderFreq, senderPwd, messageContent, senderInterference, level)) {
                return true;
            }
            
            for (ItemStack targetStack : target.getInventory().items) {
                if (checkEncryptedWalkieTalkieFrequency(targetStack, target, senderName, senderFreq, senderPwd, messageContent, senderInterference, level)) {
                    return true;
                }
            }
            
            return false;
        }
        
        private static boolean checkEncryptedWalkieTalkieFrequency(ItemStack stack, ServerPlayer target, String senderName, float senderFreq, String senderPwd, String messageContent, int senderInterference, Level level) {
            if (stack.getItem() instanceof EncryptedWalkieTalkieItem) {
                initNBT(stack);
                CompoundTag targetTag = stack.getTag();
                float targetFreq = targetTag.getFloat(TAG_FREQUENCY);
                String targetPwd = targetTag.getString(TAG_PASSWORD);
                int targetInterference = targetTag.getInt(TAG_INTERFERENCE);
                
                if (CommunicationUtils.isFrequencyMatch(targetFreq, senderFreq)) {
                    String displayMessage;
                    if (targetPwd.equals(senderPwd)) {
                        displayMessage = messageContent;
                    } else {
                        int matchCount = 0;
                        int minLen = Math.min(targetPwd.length(), senderPwd.length());
                        for (int i = 0; i < minLen; i++) {
                            if (targetPwd.charAt(i) == senderPwd.charAt(i)) {
                                matchCount++;
                            }
                        }
                        
                        if (matchCount == 2) {
                            displayMessage = messageContent;
                        } else if (matchCount == 1) {
                            displayMessage = CommunicationUtils.generateGarbledText(messageContent, 1.0 / 6.0);
                        } else {
                            displayMessage = CommunicationUtils.generateGarbledText(messageContent, 0.0);
                        }
                    }
                    int totalInterference = Math.max(senderInterference, targetInterference);
                    displayMessage = CommunicationUtils.applyInterference(displayMessage, totalInterference, level);
                    CommunicationUtils.sendMessageToPlayer(target, senderName, senderFreq, displayMessage);
                    return true;
                }
            }
            return false;
        }
    }
}