package bili.dongsz.broadcastradio.utils;

import bili.dongsz.broadcastradio.block.entity.SimpleRadioBlockEntity;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;

public class CommunicationUtils {
    public static final int RADIO_RANGE = 8;
    public static final int RAIN_INTERFERENCE = 4;
    public static final int THUNDER_INTERFERENCE = 8;
    
    /**
     * 检查玩家是否在收音机的范围内，并且收音机频率匹配
     * @param target 目标玩家
     * @param senderName 发送者名称
     * @param senderFreq 发送者频率
     * @param senderPwd 发送者密码
     * @param message 消息内容
     * @return 是否找到匹配的收音机
     */
    public static boolean checkPlayerNearRadio(ServerPlayer target, String senderName, float senderFreq, String senderPwd, String message) {
        Level level = target.level();
        
        // 检查玩家
        BlockPos playerPos = target.blockPosition();
        for (int dx = -8; dx <= 8; dx++) {
            for (int dy = -8; dy <= 8; dy++) {
                for (int dz = -8; dz <= 8; dz++) {
                    BlockPos checkPos = playerPos.offset(dx, dy, dz);
                    
                    // 检查方块状态
                    net.minecraft.world.level.block.state.BlockState blockState = level.getBlockState(checkPos);
                    if (!blockState.isAir()) {
                        if (blockState.getBlock() instanceof bili.dongsz.broadcastradio.block.SimpleRadioBlock) {
                            BlockEntity blockEntity = level.getBlockEntity(checkPos);
                            if (blockEntity instanceof SimpleRadioBlockEntity radioEntity) {
                                // 检查频率匹配
                                float radioFreq = radioEntity.getFrequency();
                                if (Math.abs(radioFreq - senderFreq) < 0.01f) {
                                    // 计算玩家与收音机的实际距离
                                    double distance = target.distanceToSqr(checkPos.getX() + 0.5, checkPos.getY() + 0.5, checkPos.getZ() + 0.5);
                                    
                                    if (distance <= RADIO_RANGE * RADIO_RANGE) {
                                        String displayMessage;
                                        if (senderPwd.isEmpty()) {
                                            displayMessage = message;
                                        } else {
                                            displayMessage = generateGarbledText(message, 0.0);
                                        }
                                        displayMessage = applyInterference(displayMessage, radioEntity.getInterference(), level);
                                        sendMessageToPlayer(target, senderName, senderFreq, displayMessage);
                                        return true;
                                    }
                                }
                            } else {
                                // 使用默认频率
                                float defaultFreq = SimpleRadioBlockEntity.DEFAULT_FREQUENCY;
                                if (Math.abs(defaultFreq - senderFreq) < 0.01f) {
                                    // 计算玩家与收音机的实际距离
                                    double distance = target.distanceToSqr(checkPos.getX() + 0.5, checkPos.getY() + 0.5, checkPos.getZ() + 0.5);
                                    
                                    if (distance <= RADIO_RANGE * RADIO_RANGE) {
                                        String displayMessage;
                                        if (senderPwd.isEmpty()) {
                                            displayMessage = message;
                                        } else {
                                            displayMessage = generateGarbledText(message, 0.0);
                                        }
                                        displayMessage = applyInterference(displayMessage, SimpleRadioBlockEntity.DEFAULT_INTERFERENCE, level);
                                        sendMessageToPlayer(target, senderName, senderFreq, displayMessage);
                                        return true;
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        
        return false;
    }
    
    /**
     * 向玩家发送通讯消息
     * @param player 目标玩家
     * @param senderName 发送者名称
     * @param frequency 频率
     * @param message 消息内容
     */
    public static void sendMessageToPlayer(ServerPlayer player, String senderName, float frequency, String message) {
        Component radioMessage = Component.translatable(
                "item.broadcast_radio.walkie_talkie.message",
                senderName,
                String.format("%.1f", frequency),
                message
        ).withStyle(ChatFormatting.LIGHT_PURPLE);
        player.sendSystemMessage(radioMessage);
    }
    
    /**
     * 检查两个频率是否匹配
     * @param freq1 第一个频率
     * @param freq2 第二个频率
     * @return 是否匹配
     */
    public static boolean isFrequencyMatch(float freq1, float freq2) {
        return Math.abs(freq1 - freq2) < 0.01f;
    }
    
    /**
     * 生成部分原文+随机字符的文本
     * @param originalMessage 原始消息
     * @param originalCharProbability 显示原文字符的概率
     * @return 混合文本
     */
    public static String generateGarbledText(String originalMessage, double originalCharProbability) {
        StringBuilder sb = new StringBuilder();
        String garbledChars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789!@#$%^&*()_+-=[]{}|;:,.<>?";
        for (int i = 0; i < originalMessage.length(); i++) {
            if (Math.random() < originalCharProbability) {
                sb.append(originalMessage.charAt(i));
            } else {
                int index = (int) (Math.random() * garbledChars.length());
                sb.append(garbledChars.charAt(index));
            }
        }
        return sb.toString();
    }
    
    /**
     * 干扰效果
     * @param message 原始消息
     * @param baseInterference 基础干扰值
     * @param level 世界
     * @return 应用干扰后的消息
     */
    public static String applyInterference(String message, int baseInterference, Level level) {
        int weatherInterference = 0;
        if (level.isRaining()) {
            weatherInterference += RAIN_INTERFERENCE;
        }
        if (level.isThundering()) {
            weatherInterference += THUNDER_INTERFERENCE;
        }
        
        int totalInterference = baseInterference + weatherInterference;
        if (totalInterference <= 0) {
            return message;
        }
        
        double interferenceProbability = totalInterference / 100.0;
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < message.length(); i++) {
            if (Math.random() < interferenceProbability) {
                sb.append("#");
            } else {
                sb.append(message.charAt(i));
            }
        }
        return sb.toString();
    }
}