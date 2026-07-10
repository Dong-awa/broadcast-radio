package bili.dongsz.broadcastradio.utils;

import bili.dongsz.broadcastradio.BroadcastRadio;
import bili.dongsz.broadcastradio.block.entity.SimpleRadioBlockEntity;
import bili.dongsz.broadcastradio.block.entity.SimpleSignalJammerBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CommunicationUtils {
    public static final int RADIO_RANGE = 8;
    public static final int RAIN_INTERFERENCE = 4;
    public static final int THUNDER_INTERFERENCE = 8;
    public static final double BASE_COMMUNICATION_RANGE = 200.0;
    public static final double RAY_TRACE_STEP_SIZE = 0.2;
    public static final double EYE_HEIGHT_OFFSET = 1.62;

    public static final boolean DEBUG_SIGNAL_ATTENUATION = false; //信号衰减的调试开关，True为开启，False为关闭

    public static double getBlockAbsorptionValue(BlockState state) {
        return (double) AbsorptionManager.getAbsorption(state);
    }

    public static class AttenuationResult {
        public final double totalAbsorption;
        public final int totalSteps;
        public final int nonAirSteps;
        public final Map<String, BlockDebugInfo> blockDebugInfo;
        public final boolean blockedByImpenetrable;
        public final Vec3 from;
        public final Vec3 to;
        public final BlockPos impenetrablePos;

        public AttenuationResult(double totalAbsorption, int totalSteps, int nonAirSteps,
                                 Map<String, BlockDebugInfo> blockDebugInfo, boolean blockedByImpenetrable,
                                 Vec3 from, Vec3 to, BlockPos impenetrablePos) {
            this.totalAbsorption = totalAbsorption;
            this.totalSteps = totalSteps;
            this.nonAirSteps = nonAirSteps;
            this.blockDebugInfo = blockDebugInfo;
            this.blockedByImpenetrable = blockedByImpenetrable;
            this.from = from;
            this.to = to;
            this.impenetrablePos = impenetrablePos;
        }
    }

    public static class BlockDebugInfo {
        public final String blockName;
        public int count;
        public double absorptionPerBlock;
        public double totalSubAbsorption;

        public BlockDebugInfo(String blockName, double absorptionPerBlock) {
            this.blockName = blockName;
            this.count = 0;
            this.absorptionPerBlock = absorptionPerBlock;
            this.totalSubAbsorption = 0.0;
        }

        public void addStep(double absorptionPerBlock) {
            this.count++;
            this.totalSubAbsorption += absorptionPerBlock * RAY_TRACE_STEP_SIZE;
        }
    }

    public static AttenuationResult calculateSignalAttenuationDetailed(Level level, Vec3 from, Vec3 to, double baseRange) {
        if (level == null || from == null || to == null) {
            return new AttenuationResult(0.0, 0, 0, new HashMap<>(), false, from, to, null);
        }
        double totalDistance = from.distanceTo(to);
        if (totalDistance < 0.5) {
            return new AttenuationResult(0.0, 0, 0, new HashMap<>(), false, from, to, null);
        }

        double stepSize = RAY_TRACE_STEP_SIZE;
        int steps = (int) Math.ceil(totalDistance / stepSize);
        double totalAbsorption = 0.0;
        int nonAirSteps = 0;
        Map<String, BlockDebugInfo> debugInfo = new HashMap<>();
        BlockPos impenetrablePos = null;
        boolean blocked = false;

        Vec3 direction = to.subtract(from).normalize();
        Vec3 current = from.add(direction.scale(stepSize * 0.5));

        for (int i = 0; i < steps; i++) {
            BlockPos pos = BlockPos.containing(current);
            if (level.hasChunkAt(pos)) {
                BlockState state = level.getBlockState(pos);
                if (!state.isAir()) {
                    nonAirSteps++;
                    double absorption = getBlockAbsorptionValue(state);
                    if (absorption >= 999) {
                        impenetrablePos = pos;
                        blocked = true;
                        String key = state.getBlock().toString();
                        if (!debugInfo.containsKey(key)) {
                            debugInfo.put(key, new BlockDebugInfo(key, absorption));
                        }
                        debugInfo.get(key).addStep(absorption);
                        break;
                    }
                    totalAbsorption += absorption * stepSize;
                    String key = state.getBlock().toString();
                    if (!debugInfo.containsKey(key)) {
                        debugInfo.put(key, new BlockDebugInfo(key, absorption));
                    }
                    debugInfo.get(key).addStep(absorption);
                } else {
                    String key = "air";
                    if (!debugInfo.containsKey(key)) {
                        debugInfo.put(key, new BlockDebugInfo(key, 0.0));
                    }
                    debugInfo.get(key).addStep(0.0);
                }
            }
            current = current.add(direction.scale(stepSize));
        }

        double finalAbsorption = blocked ? baseRange + 1.0 : totalAbsorption;
        return new AttenuationResult(finalAbsorption, steps, nonAirSteps, debugInfo, blocked, from, to, impenetrablePos);
    }

    public static double calculateSignalAttenuation(Level level, Vec3 from, Vec3 to, double baseRange) {
        if (level == null || from == null || to == null) return 0.0;
        double totalDistance = from.distanceTo(to);
        if (totalDistance < 0.5) return 0.0;

        double stepSize = RAY_TRACE_STEP_SIZE;
        int steps = (int) Math.ceil(totalDistance / stepSize);
        double totalAbsorption = 0.0;

        Vec3 direction = to.subtract(from).normalize();
        Vec3 current = from.add(direction.scale(stepSize * 0.5));

        for (int i = 0; i < steps; i++) {
            BlockPos pos = BlockPos.containing(current);
            if (level.hasChunkAt(pos)) {
                BlockState state = level.getBlockState(pos);
                if (!state.isAir()) {
                    double absorption = getBlockAbsorptionValue(state);
                    if (absorption >= 999) {
                        return baseRange + 1.0;
                    }
                    totalAbsorption += absorption * stepSize;
                }
            }
            current = current.add(direction.scale(stepSize));
        }

        return totalAbsorption;
    }

    public static double calculateSignalAttenuation(Level level, BlockPos from, BlockPos to, double baseRange) {
        if (from == null || to == null) return 0.0;
        Vec3 fromVec = new Vec3(from.getX() + 0.5, from.getY() + 0.5, from.getZ() + 0.5);
        Vec3 toVec = new Vec3(to.getX() + 0.5, to.getY() + 0.5, to.getZ() + 0.5);
        return calculateSignalAttenuation(level, fromVec, toVec, baseRange);
    }

    public static double calculateSignalAttenuationEye(Level level, Entity sender, Entity target, double baseRange) {
        if (sender == null || target == null) return 0.0;
        Vec3 senderEye = sender.position().add(0.0, EYE_HEIGHT_OFFSET, 0.0);
        Vec3 targetEye = target.position().add(0.0, EYE_HEIGHT_OFFSET, 0.0);
        return calculateSignalAttenuation(level, senderEye, targetEye, baseRange);
    }

    public static boolean canSignalReach(Level level, BlockPos from, BlockPos to, double baseRange) {
        if (from == null || to == null) return false;
        double straightDistance = Math.sqrt(from.distSqr(to));
        if (straightDistance > baseRange) return false;
        double pathAbsorption = calculateSignalAttenuation(level, from, to, baseRange);
        double effectiveRange = baseRange - pathAbsorption;
        return effectiveRange >= straightDistance;
    }

    public static boolean canSignalReach(Level level, Entity sender, Entity target, double baseRange) {
        if (sender == null || target == null) return false;
        if (sender.level() != target.level()) return false;
        return canSignalReach(sender.level(), sender.blockPosition(), target.blockPosition(), baseRange);
    }

    public static boolean canSignalReachEye(Level level, Entity sender, Entity target, double baseRange) {
        if (sender == null || target == null) return false;
        if (sender.level() != target.level()) return false;
        Vec3 senderEye = sender.position().add(0.0, EYE_HEIGHT_OFFSET, 0.0);
        Vec3 targetEye = target.position().add(0.0, EYE_HEIGHT_OFFSET, 0.0);
        double straightDistance = senderEye.distanceTo(targetEye);
        if (straightDistance > baseRange) return false;
        double pathAbsorption = calculateSignalAttenuation(level, senderEye, targetEye, baseRange);
        double effectiveRange = baseRange - pathAbsorption;
        return effectiveRange >= straightDistance;
    }

    public static boolean canSignalReachEye(Level level, Entity sender, Entity target, double baseRange,
                                            String senderName, String targetName,
                                            Player chatOutputTarget) {
        if (sender == null || target == null) return false;
        if (sender.level() != target.level()) return false;

        Vec3 senderEye = sender.position().add(0.0, EYE_HEIGHT_OFFSET, 0.0);
        Vec3 targetEye = target.position().add(0.0, EYE_HEIGHT_OFFSET, 0.0);
        double straightDistance = senderEye.distanceTo(targetEye);

        AttenuationResult result = calculateSignalAttenuationDetailed(level, senderEye, targetEye, baseRange);
        double effectiveRange = baseRange - result.totalAbsorption;
        boolean reached = effectiveRange >= straightDistance && straightDistance <= baseRange;

        if (DEBUG_SIGNAL_ATTENUATION) {
            printAttenuationDebug(level, senderName, targetName, straightDistance, baseRange, result,
                    effectiveRange, reached, "玩家→玩家", chatOutputTarget);
        }

        return reached;
    }

    public static boolean canSignalReachEyeToBlock(Level level, Entity sender, BlockPos blockPos, double baseRange) {
        if (sender == null || blockPos == null) return false;
        Vec3 senderEye = sender.position().add(0.0, EYE_HEIGHT_OFFSET, 0.0);
        Vec3 blockCenter = new Vec3(blockPos.getX() + 0.5, blockPos.getY() + 0.5, blockPos.getZ() + 0.5);
        double straightDistance = senderEye.distanceTo(blockCenter);
        if (straightDistance > baseRange) return false;
        double pathAbsorption = calculateSignalAttenuation(level, senderEye, blockCenter, baseRange);
        double effectiveRange = baseRange - pathAbsorption;
        return effectiveRange >= straightDistance;
    }

    public static boolean canSignalReachEyeToBlock(Level level, Entity sender, BlockPos blockPos, double baseRange,
                                                   String senderName, String blockName,
                                                   Player chatOutputTarget) {
        if (sender == null || blockPos == null) return false;

        Vec3 senderEye = sender.position().add(0.0, EYE_HEIGHT_OFFSET, 0.0);
        Vec3 blockCenter = new Vec3(blockPos.getX() + 0.5, blockPos.getY() + 0.5, blockPos.getZ() + 0.5);
        double straightDistance = senderEye.distanceTo(blockCenter);

        AttenuationResult result = calculateSignalAttenuationDetailed(level, senderEye, blockCenter, baseRange);
        double effectiveRange = baseRange - result.totalAbsorption;
        boolean reached = effectiveRange >= straightDistance && straightDistance <= baseRange;

        if (DEBUG_SIGNAL_ATTENUATION) {
            printAttenuationDebug(level, senderName, blockName, straightDistance, baseRange, result,
                    effectiveRange, reached, "玩家→收音机", chatOutputTarget);
        }

        return reached;
    }

    private static void printAttenuationDebug(Level level, String senderName, String targetName,
                                              double straightDistance, double baseRange,
                                              AttenuationResult result, double effectiveRange,
                                              boolean reached, String mode, Player chatTarget) {
        StringBuilder logBuilder = new StringBuilder();
        logBuilder.append("[SignalAttenuation] 模式=").append(mode);
        logBuilder.append(", 发送者=").append(senderName);
        logBuilder.append(", 接收者=").append(targetName);
        logBuilder.append("\n  直线距离: ").append(String.format("%.1f", straightDistance)).append(" 格");
        logBuilder.append("\n  基础传播距离: ").append(String.format("%.0f", baseRange)).append(" 格");
        logBuilder.append("\n  射线追踪步数: ").append(result.totalSteps);
        logBuilder.append(" (非空气: ").append(result.nonAirSteps).append(")");
        logBuilder.append("\n  方块明细:");

        List<String> blockLines = new ArrayList<>();
        for (BlockDebugInfo info : result.blockDebugInfo.values()) {
            double perBlock = info.absorptionPerBlock;
            if (info.count > 0) {
                perBlock = info.totalSubAbsorption / info.count / RAY_TRACE_STEP_SIZE;
            }
            String line = String.format("    %s x%d (吸收%.0f) → 小计%.2f",
                    info.blockName, info.count, perBlock, info.totalSubAbsorption);
            blockLines.add(line);
        }

        for (String line : blockLines) {
            logBuilder.append("\n").append(line);
        }

        if (result.blockedByImpenetrable) {
            logBuilder.append("\n  ⚠ 遇到不可穿透方块 (如基岩/屏障)，信号被完全阻挡！");
            if (result.impenetrablePos != null) {
                logBuilder.append(" 位置: (").append(result.impenetrablePos.getX())
                        .append(", ").append(result.impenetrablePos.getY())
                        .append(", ").append(result.impenetrablePos.getZ()).append(")");
            }
        }

        logBuilder.append("\n  总吸收值: ").append(String.format("%.2f", result.totalAbsorption));
        logBuilder.append("\n  有效传播距离: ").append(String.format("%.2f", Math.max(0, effectiveRange))).append(" 格");
        logBuilder.append("\n  结果: ").append(reached ? "信号到达 ✓" : "信号未到达 ✗");
        logBuilder.append(" (").append(String.format("%.2f", Math.max(0, effectiveRange)));
        logBuilder.append(" >= ").append(String.format("%.2f", straightDistance)).append(" ? ")
                .append((effectiveRange >= straightDistance) ? "是" : "否").append(")");
        if (straightDistance > baseRange) {
            logBuilder.append(" [已超出基础传播距离]");
        }

        BroadcastRadio.LOGGER.info(logBuilder.toString());

        if (chatTarget != null) {
            sendAttenuationChatMessage(chatTarget, senderName, targetName, straightDistance, baseRange,
                    result, effectiveRange, reached, mode);
        }
    }

    private static void sendAttenuationChatMessage(Player player, String senderName, String targetName,
                                                   double straightDistance, double baseRange,
                                                   AttenuationResult result, double effectiveRange,
                                                   boolean reached, String mode) {
        String color = reached ? "§a" : "§c";
        String marker = reached ? "✓" : "✗";

        player.sendSystemMessage(Component.literal("§6======== [信号衰减调试] ========"));
        player.sendSystemMessage(Component.literal("§7模式: §f" + mode + "  §7| 发送: §f" + senderName + " → 接收: §f" + targetName));
        player.sendSystemMessage(Component.literal("§7直线距离: §f" + String.format("%.1f", straightDistance) + " 格  §7| 基础距离: §f" + String.format("%.0f", baseRange) + " 格"));
        player.sendSystemMessage(Component.literal("§7总吸收: §e" + String.format("%.2f", result.totalAbsorption) + "  §7| 有效距离: §f" + String.format("%.2f", Math.max(0, effectiveRange)) + " 格"));

        player.sendSystemMessage(Component.literal("§7--- 方块明细 (" + result.blockDebugInfo.size() + " 种) ---"));
        for (BlockDebugInfo info : result.blockDebugInfo.values()) {
            if (info.count > 0) {
                double perBlock = info.totalSubAbsorption / info.count / RAY_TRACE_STEP_SIZE;
                player.sendSystemMessage(Component.literal(
                        String.format("§7  %s x%d (吸收%.0f) → §e%.2f",
                                info.blockName, info.count, perBlock, info.totalSubAbsorption)
                ));
            }
        }

        if (result.blockedByImpenetrable) {
            String posStr = result.impenetrablePos != null
                    ? "(" + result.impenetrablePos.getX() + ", " + result.impenetrablePos.getY() + ", " + result.impenetrablePos.getZ() + ")"
                    : "未知";
            player.sendSystemMessage(Component.literal("§c⚠ 信号被不可穿透方块阻挡！位置: " + posStr));
        }

        player.sendSystemMessage(Component.literal(color + "结果: " + marker + " " + (reached ? "信号成功到达" : "信号未能到达")));
        player.sendSystemMessage(Component.literal("§6================================"));
    }

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

    public static boolean checkPlayerNearRadio(Player target, String senderName, float senderFreq, String senderPwd, String message) {
        return checkPlayerNearRadio(target, senderName, senderFreq, senderPwd, message, 0);
    }

    public static boolean checkPlayerNearRadio(Player target, String senderName, float senderFreq, String senderPwd, String message, int senderInterference) {
        Level level = target.level();

        BlockPos playerPos = target.blockPosition();
        for (int dx = -8; dx <= 8; dx++) {
            for (int dy = -8; dy <= 8; dy++) {
                for (int dz = -8; dz <= 8; dz++) {
                    BlockPos checkPos = playerPos.offset(dx, dy, dz);

                    BlockState blockState = level.getBlockState(checkPos);
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

    public static boolean checkPlayerNearRadio(Player target, Entity sender, double baseRange,
                                               String senderName, float senderFreq, String senderPwd,
                                               String message, int senderInterference) {
        return checkPlayerNearRadio(target, sender, baseRange, senderName, senderFreq, senderPwd, message,
                senderInterference, null);
    }

    public static boolean checkPlayerNearRadio(Player target, Entity sender, double baseRange,
                                               String senderName, float senderFreq, String senderPwd,
                                               String message, int senderInterference,
                                               Player chatOutputTarget) {
        Level level = target.level();

        BlockPos playerPos = target.blockPosition();
        for (int dx = -8; dx <= 8; dx++) {
            for (int dy = -8; dy <= 8; dy++) {
                for (int dz = -8; dz <= 8; dz++) {
                    BlockPos checkPos = playerPos.offset(dx, dy, dz);

                    BlockState blockState = level.getBlockState(checkPos);
                    if (!blockState.isAir()) {
                        if (blockState.getBlock() instanceof bili.dongsz.broadcastradio.block.SimpleRadioBlock) {
                            if (!canSignalReachEyeToBlock(level, sender, checkPos, baseRange,
                                    senderName, "收音机@(" + checkPos.getX() + "," + checkPos.getY() + "," + checkPos.getZ() + ")",
                                    chatOutputTarget)) continue;

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

    public static void sendMessageToPlayer(Player player, String senderName, float frequency, String message) {
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