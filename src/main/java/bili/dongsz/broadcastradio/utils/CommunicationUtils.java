package bili.dongsz.broadcastradio.utils;

import bili.dongsz.broadcastradio.BroadcastRadio;
import bili.dongsz.broadcastradio.block.entity.SimpleRadioBlockEntity;
import bili.dongsz.broadcastradio.block.entity.SimpleSignalJammerBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;

public class CommunicationUtils {
    public static final int RADIO_RANGE = 8;
    public static final int RAIN_INTERFERENCE = 4;
    public static final int THUNDER_INTERFERENCE = 8;

    public static int getJammerInterference(Level level, BlockPos pos, float frequency) {
        if (level == null || pos == null) return 0;
        if (level.isClientSide) return 0;

        int maxInterference = 0;
        int range = SimpleSignalJammerBlockEntity.LIMIT_RADIUS;
        int rangeSq = range * range;

        int chunkRadius = (range >> 4) + 1;
        int centerChunkX = pos.getX() >> 4;
        int centerChunkZ = pos.getZ() >> 4;

        for (int cx = centerChunkX - chunkRadius; cx <= centerChunkX + chunkRadius; cx++) {
            for (int cz = centerChunkZ - chunkRadius; cz <= centerChunkZ + chunkRadius; cz++) {
                if (!level.hasChunk(cx, cz)) continue;
                net.minecraft.world.level.chunk.LevelChunk levelChunk = (net.minecraft.world.level.chunk.LevelChunk) level.getChunk(cx, cz);
                if (levelChunk == null) continue;

                var blockEntities = levelChunk.getBlockEntities();
                if (blockEntities == null) continue;

                for (var entry : blockEntities.entrySet()) {
                    BlockEntity be = entry.getValue();
                    if (be instanceof SimpleSignalJammerBlockEntity jammer) {
                        if (!jammer.isWorking()) continue;
                        float jammerFreq = jammer.getFrequency();
                        if (Math.abs(jammerFreq - frequency) > 0.05f) continue;
                        BlockPos jammerWorldPos = be.getBlockPos();
                        double distSq = pos.distSqr(jammerWorldPos);
                        if (distSq > rangeSq) continue;
                        int interference = SimpleSignalJammerBlockEntity.calculateInterferenceAtDistance(distSq);
                        if (interference > maxInterference) {
                            maxInterference = interference;
                        }
                    }
                }
            }
        }

        return maxInterference;
    }

    public static boolean checkPlayerNearRadio(net.minecraft.world.entity.player.Player target, String senderName, float senderFreq, String senderPwd, String message) {
        return checkPlayerNearRadio(target, senderName, senderFreq, senderPwd, message, 0);
    }

    public static boolean checkPlayerNearRadio(net.minecraft.world.entity.player.Player target, String senderName, float senderFreq, String senderPwd, String message, int senderInterference) {
        Level level = target.level();

        BlockPos playerPos = target.blockPosition();
        for (int dx = -8; dx <= 8; dx++) {
            for (int dy = -8; dy <= 8; dy++) {
                for (int dz = -8; dz <= 8; dz++) {
                    BlockPos checkPos = playerPos.offset(dx, dy, dz);

                    net.minecraft.world.level.block.state.BlockState blockState = level.getBlockState(checkPos);
                    if (!blockState.isAir()) {
                        if (blockState.getBlock() instanceof bili.dongsz.broadcastradio.block.SimpleRadioBlock) {
                            BlockEntity blockEntity = level.getBlockEntity(checkPos);
                            if (blockEntity instanceof SimpleRadioBlockEntity radioEntity) {
                                float radioFreq = radioEntity.getFrequency();
                                if (Math.abs(radioFreq - senderFreq) < 0.01f) {
                                    double distance = target.distanceToSqr(checkPos.getX() + 0.5, checkPos.getY() + 0.5, checkPos.getZ() + 0.5);

                                    if (distance <= RADIO_RANGE * RADIO_RANGE) {
                                        int jammerAtRadio = getJammerInterference(level, checkPos, senderFreq);
                                        int jammerCombined = Math.max(jammerAtRadio, senderInterference);
                                        int radioBase = radioEntity.getInterference();
                                        int totalInterference = Math.max(radioBase, jammerCombined);
                                        String displayMessage;
                                        if (senderPwd.isEmpty()) {
                                            displayMessage = message;
                                        } else {
                                            displayMessage = generateGarbledText(message, 0.0);
                                        }
                                        displayMessage = applyInterference(displayMessage, totalInterference, level);
                                        sendMessageToPlayer(target, senderName, senderFreq, displayMessage);
                                        return true;
                                    }
                                }
                            } else {
                                float defaultFreq = SimpleRadioBlockEntity.DEFAULT_FREQUENCY;
                                if (Math.abs(defaultFreq - senderFreq) < 0.01f) {
                                    double distance = target.distanceToSqr(checkPos.getX() + 0.5, checkPos.getY() + 0.5, checkPos.getZ() + 0.5);

                                    if (distance <= RADIO_RANGE * RADIO_RANGE) {
                                        int jammerAtRadio = getJammerInterference(level, checkPos, senderFreq);
                                        int jammerCombined = Math.max(jammerAtRadio, senderInterference);
                                        int totalInterference = Math.max(SimpleRadioBlockEntity.DEFAULT_INTERFERENCE, jammerCombined);
                                        String displayMessage;
                                        if (senderPwd.isEmpty()) {
                                            displayMessage = message;
                                        } else {
                                            displayMessage = generateGarbledText(message, 0.0);
                                        }
                                        displayMessage = applyInterference(displayMessage, totalInterference, level);
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

    public static void sendMessageToPlayer(net.minecraft.world.entity.player.Player player, String senderName, float frequency, String message) {
        Component radioMessage = Component.translatable(
                "item.broadcast_radio.walkie_talkie.message",
                senderName,
                String.format("%.1f", frequency),
                message
        ).withStyle(net.minecraft.ChatFormatting.LIGHT_PURPLE);
        player.sendSystemMessage(radioMessage);
    }

    public static boolean isFrequencyMatch(float freq1, float freq2) {
        return Math.abs(freq1 - freq2) < 0.01f;
    }

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

    public static int getWeatherInterference(Level level) {
        if (level == null) return 0;
        int weather = 0;
        if (level.isRaining()) weather += RAIN_INTERFERENCE;
        if (level.isThundering()) weather += THUNDER_INTERFERENCE;
        return weather;
    }

    public static int clampInterference(int value) {
        if (value < 0) return 0;
        if (value > 100) return 100;
        return value;
    }

    public static int calculateCombinedInterference(Level level, BlockPos pos, float frequency, int extraBase) {
        int jammer = getJammerInterference(level, pos, frequency);
        int weather = getWeatherInterference(level);
        int combined = Math.max(extraBase, jammer) + weather;
        return clampInterference(combined);
    }

    public static String applyInterference(String message, int baseInterference, Level level) {
        int weatherInterference = getWeatherInterference(level);
        int totalInterference = baseInterference + weatherInterference;
        totalInterference = clampInterference(totalInterference);

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