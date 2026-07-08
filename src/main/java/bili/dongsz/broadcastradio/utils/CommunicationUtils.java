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

        int jammerCount = 0;
        int activeCount = 0;
        int freqMatchCount = 0;
        int inRangeCount = 0;
        int totalChecked = 0;

        int minX = pos.getX() - range;
        int maxX = pos.getX() + range;
        int minZ = pos.getZ() - range;
        int maxZ = pos.getZ() + range;
        int yMin = Math.max(level.getMinBuildHeight(), pos.getY() - range);
        int yMax = Math.min(level.getMaxBuildHeight() - 1, pos.getY() + range);

        BroadcastRadio.LOGGER.info("[JammerScan] START: pos={}, freq={}, scanRange X[{}..{}], Y[{}..{}], Z[{}..{}]",
                pos, frequency, minX, maxX, yMin, yMax, minZ, maxZ);

        // 先用区块方式快速收集（用 LevelChunk 具体类型）
        int chunkRadius = (range >> 4) + 1;
        int centerChunkX = pos.getX() >> 4;
        int centerChunkZ = pos.getZ() >> 4;

        boolean foundAny = false;

        // 方式1：用 LevelChunk 的具体实现获取 BlockEntities
        for (int cx = centerChunkX - chunkRadius; cx <= centerChunkX + chunkRadius; cx++) {
            for (int cz = centerChunkZ - chunkRadius; cz <= centerChunkZ + chunkRadius; cz++) {
                if (!level.hasChunk(cx, cz)) continue;
                net.minecraft.world.level.chunk.LevelChunk levelChunk = (net.minecraft.world.level.chunk.LevelChunk) level.getChunk(cx, cz);
                if (levelChunk == null) continue;

                var blockEntities = levelChunk.getBlockEntities();
                if (blockEntities == null) continue;

                for (var entry : blockEntities.entrySet()) {
                    BlockEntity be = entry.getValue();
                    totalChecked++;
                    if (be instanceof SimpleSignalJammerBlockEntity jammer) {
                        foundAny = true;
                        jammerCount++;
                        BlockPos jammerWorldPos = be.getBlockPos();
                        if (!jammer.isWorking()) {
                            BroadcastRadio.LOGGER.info("[JammerDebug] 屏蔽器在{} 未激活(isWorking=false, source={})",
                                    jammerWorldPos, jammer.getCurrentEnergySource());
                            continue;
                        }
                        activeCount++;

                        float jammerFreq = jammer.getFrequency();
                        float freqDiff = Math.abs(jammerFreq - frequency);
                        if (freqDiff > 0.05f) {
                            BroadcastRadio.LOGGER.info("[JammerDebug] 屏蔽器在{} 频率{} 不匹配(目标{}, 差{})",
                                    jammerWorldPos, jammerFreq, frequency, freqDiff);
                            continue;
                        }
                        freqMatchCount++;

                        double distSq = pos.distSqr(jammerWorldPos);
                        if (distSq > rangeSq) {
                            BroadcastRadio.LOGGER.info("[JammerDebug] 屏蔽器在{} 距离{} 超出范围(限{})",
                                    jammerWorldPos, String.format("%.1f", Math.sqrt(distSq)), range);
                            continue;
                        }
                        inRangeCount++;

                        int interference = SimpleSignalJammerBlockEntity.calculateInterferenceAtDistance(distSq);
                        BroadcastRadio.LOGGER.info("[JammerDebug] 屏蔽器在{} 距离{} 产生干扰{}%",
                                jammerWorldPos, String.format("%.1f", Math.sqrt(distSq)), interference);
                        if (interference > maxInterference) {
                            maxInterference = interference;
                        }
                    }
                }
            }
        }

        // 方式2：如果区块遍历没找到，退而直接按坐标遍历确认
        if (!foundAny) {
            BroadcastRadio.LOGGER.info("[JammerScan] 区块遍历未找到屏蔽器，尝试直接坐标遍历方式...");
            int step = 1;
            for (int x = minX; x <= maxX; x += step) {
                for (int y = yMin; y <= yMax; y += step) {
                    for (int z = minZ; z <= maxZ; z += step) {
                        BlockPos checkPos = new BlockPos(x, y, z);
                        try {
                            BlockEntity be = level.getBlockEntity(checkPos);
                            if (be instanceof SimpleSignalJammerBlockEntity jammer) {
                                BroadcastRadio.LOGGER.info("[JammerScan-Fallback] 找到屏蔽器在{} (区块坐标 {},{})",
                                        checkPos, x >> 4, z >> 4);
                                jammerCount++;
                                BlockPos jammerWorldPos = be.getBlockPos();
                                if (!jammer.isWorking()) continue;
                                float jammerFreq = jammer.getFrequency();
                                if (Math.abs(jammerFreq - frequency) > 0.05f) continue;
                                double distSq = pos.distSqr(jammerWorldPos);
                                if (distSq > rangeSq) continue;
                                int interference = SimpleSignalJammerBlockEntity.calculateInterferenceAtDistance(distSq);
                                if (interference > maxInterference) maxInterference = interference;
                            }
                        } catch (Exception e) {
                            // 跳过
                        }
                    }
                }
            }
        }

        BroadcastRadio.LOGGER.info("[Jammer] END: 位置{} 频率{}: 扫描检查{}个BlockEntity, 屏蔽器={}, 激活={}, 频率匹配={}, 范围内={}, 最终干扰={}%",
                pos, frequency, totalChecked,
                jammerCount, activeCount, freqMatchCount, inRangeCount, maxInterference);

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
                                        int jammerAtTarget = getJammerInterference(level, playerPos, senderFreq);
                                        int jammerCombined = Math.max(Math.max(jammerAtRadio, jammerAtTarget), senderInterference);
                                        int totalInterference = Math.max(radioEntity.getInterference(), jammerCombined);
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
                                        int jammerAtTarget = getJammerInterference(level, playerPos, senderFreq);
                                        int jammerCombined = Math.max(Math.max(jammerAtRadio, jammerAtTarget), senderInterference);
                                        String displayMessage;
                                        if (senderPwd.isEmpty()) {
                                            displayMessage = message;
                                        } else {
                                            displayMessage = generateGarbledText(message, 0.0);
                                        }
                                        displayMessage = applyInterference(displayMessage, Math.max(SimpleRadioBlockEntity.DEFAULT_INTERFERENCE, jammerCombined), level);
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