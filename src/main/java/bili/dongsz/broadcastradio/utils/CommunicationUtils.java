package bili.dongsz.broadcastradio.utils;

import bili.dongsz.broadcastradio.BroadcastRadio;
import bili.dongsz.broadcastradio.block.entity.SimpleRadioBlockEntity;
import bili.dongsz.broadcastradio.block.entity.SimpleSignalJammerBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

public class CommunicationUtils {
    public static final int RADIO_RANGE = 8;
    public static final int RAIN_INTERFERENCE = 4;
    public static final int THUNDER_INTERFERENCE = 8;
    public static final double BASE_COMMUNICATION_RANGE = 200.0;
    public static final double RAY_TRACE_STEP_SIZE = 0.2;
    public static final double EYE_HEIGHT_OFFSET = 1.62;

    public static final boolean DEBUG_SIGNAL_ATTENUATION = false;//调试信息开关，true为开启，false为关闭

    private static final Map<Level, List<Vec3>> debugCandidatePoints = new java.util.concurrent.ConcurrentHashMap<>();
    private static final Map<Level, List<Vec3>> debugValidPoints = new java.util.concurrent.ConcurrentHashMap<>();
    private static final Map<Level, Vec3> debugBestPoint = new java.util.concurrent.ConcurrentHashMap<>();
    private static boolean debugVisualizationEnabled = false;

    public static void setDebugVisualizationEnabled(boolean enabled) {
        debugVisualizationEnabled = enabled;
    }

    public static void clearDebugVisualization(Level level) {
        debugCandidatePoints.remove(level);
        debugValidPoints.remove(level);
        debugBestPoint.remove(level);
    }

    public static java.util.Map<Level, List<Vec3>> getDebugCandidatePoints() {
        return debugCandidatePoints;
    }

    public static java.util.Map<Level, List<Vec3>> getDebugValidPoints() {
        return debugValidPoints;
    }

    public static java.util.Map<Level, Vec3> getDebugBestPoint() {
        return debugBestPoint;
    }

    public static final int SEARCH_RADIUS = 64;
    public static final int BOX_STEP = 3;
    public static int MAX_CANDIDATES = 512;
    public static final double REFLECTION_WEIGHT_REFLECTION = 0.5;
    public static final double REFLECTION_WEIGHT_DISTANCE = 0.3;
    public static final double REFLECTION_WEIGHT_ANGLE = 0.2;

    public static void setReflectionSearchConfig(int maxCandidates, int axialStep, int radialStep, int numRadialDirections) {
        if (maxCandidates > 0) MAX_CANDIDATES = maxCandidates;
    }

    public static double getBlockAbsorptionValue(BlockState state) {
        return (double) AbsorptionManager.getAbsorption(state);
    }

    public static double getBlockReflectionValue(BlockState state) {
        return (double) ReflectionManager.getReflection(state);
    }

    private static final Map<Level, BlockPos> lastSuccessfulReflectionPos = new WeakHashMap<>();

    public static class ReflectedPathResult {
        public final boolean found;
        public final BlockPos reflectionPos;
        public final Vec3 reflectionPoint;
        public final double totalPathLength;
        public final double d1;
        public final double d2;
        public final double absorption1;
        public final double absorption2;
        public final double totalAbsorption;
        public final double reflectionValue;
        public final double incidentAngle;
        public final double effectiveRangeAfterReflection;
        public final double weight;
        public final Direction normalDirection;
        public final AttenuationResult detailedSeg1;
        public final AttenuationResult detailedSeg2;
        public final String blockedReason;

        public ReflectedPathResult(boolean found, BlockPos reflectionPos, Vec3 reflectionPoint,
                                   double totalPathLength, double d1, double d2,
                                   double absorption1, double absorption2, double totalAbsorption,
                                   double reflectionValue, double incidentAngle,
                                   double effectiveRangeAfterReflection, double weight,
                                   Direction normalDirection, AttenuationResult detailedSeg1,
                                   AttenuationResult detailedSeg2, String blockedReason) {
            this.found = found;
            this.reflectionPos = reflectionPos;
            this.reflectionPoint = reflectionPoint;
            this.totalPathLength = totalPathLength;
            this.d1 = d1;
            this.d2 = d2;
            this.absorption1 = absorption1;
            this.absorption2 = absorption2;
            this.totalAbsorption = totalAbsorption;
            this.reflectionValue = reflectionValue;
            this.incidentAngle = incidentAngle;
            this.effectiveRangeAfterReflection = effectiveRangeAfterReflection;
            this.weight = weight;
            this.normalDirection = normalDirection;
            this.detailedSeg1 = detailedSeg1;
            this.detailedSeg2 = detailedSeg2;
            this.blockedReason = blockedReason;
        }
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

    private static boolean hasExposedFace(Level level, BlockPos pos) {
        if (level == null || pos == null) return false;
        for (Direction dir : Direction.values()) {
            BlockPos neighbor = pos.relative(dir);
            if (!level.hasChunkAt(neighbor)) continue;
            BlockState neighborState = level.getBlockState(neighbor);
            if (neighborState.isAir()) return true;
        }
        return false;
    }

    private static List<BlockPos> getBoxCandidates(Level level, Vec3 senderEye, Vec3 targetPos, int[] sampleCounts) {
        List<BlockPos> candidates = new ArrayList<>();
        java.util.HashSet<BlockPos> seen = new java.util.HashSet<>();

        int minX = (int) Math.floor(Math.min(senderEye.x, targetPos.x) - SEARCH_RADIUS);
        int maxX = (int) Math.ceil(Math.max(senderEye.x, targetPos.x) + SEARCH_RADIUS);
        int minY = (int) Math.floor(Math.min(senderEye.y, targetPos.y) - SEARCH_RADIUS);
        int maxY = (int) Math.ceil(Math.max(senderEye.y, targetPos.y) + SEARCH_RADIUS);
        int minZ = (int) Math.floor(Math.min(senderEye.z, targetPos.z) - SEARCH_RADIUS);
        int maxZ = (int) Math.ceil(Math.max(senderEye.z, targetPos.z) + SEARCH_RADIUS);

        minY = Math.max(minY, level.getMinBuildHeight());
        maxY = Math.min(maxY, level.getMaxBuildHeight() - 1);

        int totalChecked = 0;
        int totalSurface = 0;
        int totalNearPath = 0;
        int totalFarFromPath = 0;

        Vec3 ab = targetPos.subtract(senderEye);
        double abLenSq = ab.x * ab.x + ab.y * ab.y + ab.z * ab.z;
        double maxDistFromPath = Math.max(20.0, Math.sqrt(abLenSq) * 1.5);

        for (int x = minX; x <= maxX; x += BOX_STEP) {
            for (int y = minY; y <= maxY; y += BOX_STEP) {
                for (int z = minZ; z <= maxZ; z += BOX_STEP) {
                    BlockPos pos = new BlockPos(x, y, z);
                    totalChecked++;

                    if (!level.hasChunkAt(pos)) continue;

                    BlockState state = level.getBlockState(pos);
                    if (state.isAir()) continue;

                    double reflection = getBlockReflectionValue(state);
                    if (reflection <= 0) continue;

                    if (!hasExposedFace(level, pos)) continue;
                    totalSurface++;

                    Vec3 blockCenter = new Vec3(x + 0.5, y + 0.5, z + 0.5);
                    double distFromPath = distanceFromLineSegment(senderEye, targetPos, blockCenter);

                    if (distFromPath > maxDistFromPath) {
                        totalFarFromPath++;
                        continue;
                    }
                    totalNearPath++;

                    if (!seen.add(pos)) continue;

                    candidates.add(pos);

                    if (candidates.size() >= MAX_CANDIDATES) {
                        sampleCounts[0] = totalChecked;
                        sampleCounts[1] = totalSurface;
                        sampleCounts[2] = candidates.size();
                        return candidates;
                    }
                }
            }
        }

        sampleCounts[0] = totalChecked;
        sampleCounts[1] = totalSurface;
        sampleCounts[2] = candidates.size();
        return candidates;
    }

    private static double distanceFromLineSegment(Vec3 a, Vec3 b, Vec3 point) {
        Vec3 ab = b.subtract(a);
        double abLenSq = ab.x * ab.x + ab.y * ab.y + ab.z * ab.z;

        if (abLenSq < 0.0001) {
            return point.distanceTo(a);
        }

        double t = Math.max(0.0, Math.min(1.0,
                (point.x - a.x) * ab.x + (point.y - a.y) * ab.y + (point.z - a.z) * ab.z) / abLenSq);

        Vec3 closest = new Vec3(
                a.x + t * ab.x,
                a.y + t * ab.y,
                a.z + t * ab.z
        );

        return point.distanceTo(closest);
    }

    private static Vec3 calculateReflectionPointForFace(Level level, BlockPos blockPos, Direction faceDir,
                                                        Vec3 senderEye, Vec3 targetPos) {
        Vec3 blockCenter = new Vec3(blockPos.getX() + 0.5, blockPos.getY() + 0.5, blockPos.getZ() + 0.5);
        Vec3 normalVec = new Vec3(faceDir.getStepX(), faceDir.getStepY(), faceDir.getStepZ());
        Vec3 faceCenter = blockCenter.add(normalVec.scale(0.5));

        Vec3 toSender = senderEye.subtract(faceCenter);
        double d = normalVec.dot(toSender);
        Vec3 reflectedSender = senderEye.subtract(normalVec.scale(2.0 * d));

        Vec3 lineDir = targetPos.subtract(reflectedSender);
        double denominator = normalVec.dot(lineDir);

        if (Math.abs(denominator) < 0.0001) {
            return faceCenter;
        }

        double t = normalVec.dot(faceCenter.subtract(reflectedSender)) / denominator;

        Vec3 intersection = reflectedSender.add(lineDir.scale(t));

        double minX = blockPos.getX();
        double maxX = blockPos.getX() + 1.0;
        double minY = blockPos.getY();
        double maxY = blockPos.getY() + 1.0;
        double minZ = blockPos.getZ();
        double maxZ = blockPos.getZ() + 1.0;

        final double EPS = 0.01;
        boolean inRange = true;

        switch (faceDir) {
            case DOWN:
                if (Math.abs(intersection.y - minY) > EPS) inRange = false;
                else if (intersection.x < minX - EPS || intersection.x > maxX + EPS) inRange = false;
                else if (intersection.z < minZ - EPS || intersection.z > maxZ + EPS) inRange = false;
                break;
            case UP:
                if (Math.abs(intersection.y - maxY) > EPS) inRange = false;
                else if (intersection.x < minX - EPS || intersection.x > maxX + EPS) inRange = false;
                else if (intersection.z < minZ - EPS || intersection.z > maxZ + EPS) inRange = false;
                break;
            case NORTH:
                if (Math.abs(intersection.z - minZ) > EPS) inRange = false;
                else if (intersection.x < minX - EPS || intersection.x > maxX + EPS) inRange = false;
                else if (intersection.y < minY - EPS || intersection.y > maxY + EPS) inRange = false;
                break;
            case SOUTH:
                if (Math.abs(intersection.z - maxZ) > EPS) inRange = false;
                else if (intersection.x < minX - EPS || intersection.x > maxX + EPS) inRange = false;
                else if (intersection.y < minY - EPS || intersection.y > maxY + EPS) inRange = false;
                break;
            case WEST:
                if (Math.abs(intersection.x - minX) > EPS) inRange = false;
                else if (intersection.y < minY - EPS || intersection.y > maxY + EPS) inRange = false;
                else if (intersection.z < minZ - EPS || intersection.z > maxZ + EPS) inRange = false;
                break;
            case EAST:
                if (Math.abs(intersection.x - maxX) > EPS) inRange = false;
                else if (intersection.y < minY - EPS || intersection.y > maxY + EPS) inRange = false;
                else if (intersection.z < minZ - EPS || intersection.z > maxZ + EPS) inRange = false;
                break;
        }

        if (inRange) {
            return intersection;
        }

        double bestDist = Double.MAX_VALUE;
        Vec3 bestPoint = faceCenter;

        double cx = Math.max(minX, Math.min(maxX, intersection.x));
        double cy = Math.max(minY, Math.min(maxY, intersection.y));
        double cz = Math.max(minZ, Math.min(maxZ, intersection.z));

        switch (faceDir) {
            case DOWN:
            case UP:
                bestPoint = new Vec3(cx, faceCenter.y, cz);
                break;
            case NORTH:
            case SOUTH:
                bestPoint = new Vec3(cx, Math.max(minY, Math.min(maxY, intersection.y)), faceCenter.z);
                break;
            case WEST:
            case EAST:
                bestPoint = new Vec3(faceCenter.x, Math.max(minY, Math.min(maxY, intersection.y)), cz);
                break;
        }

        double dist = reflectedSender.distanceTo(bestPoint);
        if (dist < bestDist) {
            bestDist = dist;
        }

        Vec3 cornerCandidate = faceCenter;
        switch (faceDir) {
            case DOWN:
            case UP:
                cornerCandidate = new Vec3(
                        Math.abs(intersection.x - minX) < Math.abs(intersection.x - maxX) ? minX + 0.5 : maxX - 0.5,
                        faceCenter.y,
                        Math.abs(intersection.z - minZ) < Math.abs(intersection.z - maxZ) ? minZ + 0.5 : maxZ - 0.5
                );
                break;
            case NORTH:
            case SOUTH:
                cornerCandidate = new Vec3(
                        Math.abs(intersection.x - minX) < Math.abs(intersection.x - maxX) ? minX + 0.5 : maxX - 0.5,
                        Math.max(minY, Math.min(maxY, intersection.y)),
                        faceCenter.z
                );
                break;
            case WEST:
            case EAST:
                cornerCandidate = new Vec3(
                        faceCenter.x,
                        Math.max(minY, Math.min(maxY, intersection.y)),
                        Math.abs(intersection.z - minZ) < Math.abs(intersection.z - maxZ) ? minZ + 0.5 : maxZ - 0.5
                );
                break;
        }

        double cornerDist = reflectedSender.distanceTo(cornerCandidate);
        if (cornerDist < bestDist) {
            bestDist = cornerDist;
            bestPoint = cornerCandidate;
        }

        if (bestDist < Double.MAX_VALUE) {
            return bestPoint;
        }

        return faceCenter;
    }

    private static List<Direction> getExposedFaces(Level level, BlockPos blockPos) {
        List<Direction> faces = new ArrayList<>();
        for (Direction dir : Direction.values()) {
            BlockPos neighbor = blockPos.relative(dir);
            if (!level.hasChunkAt(neighbor)) continue;
            if (level.getBlockState(neighbor).isAir()) {
                faces.add(dir);
            }
        }
        return faces;
    }

    public static ReflectedPathResult searchReflectionPath(Level level, Vec3 senderEye, Vec3 targetPos,
                                                           double baseRange, boolean detailedDebug) {
        if (level == null || senderEye == null || targetPos == null) {
            return new ReflectedPathResult(false, null, null, 0, 0, 0, 0, 0,
                    0, 0, 0, 0, 0, null, null, null, "无效参数");
        }

        double straightDistance = senderEye.distanceTo(targetPos);
        if (straightDistance < 0.5) {
            return new ReflectedPathResult(false, null, null, 0, 0, 0, 0, 0,
                    0, 0, 0, 0, 0, null, null, null, "距离过近");
        }

        int[] sampleCounts = new int[3];
        List<BlockPos> candidates = getBoxCandidates(level, senderEye, targetPos, sampleCounts);

        if (debugVisualizationEnabled) {
            List<Vec3> candidatePoints = new ArrayList<>();
            for (BlockPos pos : candidates) {
                candidatePoints.add(new Vec3(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5));
            }
            debugCandidatePoints.put(level, candidatePoints);
            debugValidPoints.put(level, new ArrayList<>());
            debugBestPoint.remove(level);
        }

        ReflectedPathResult bestResult = null;
        double bestWeight = -1.0;
        int maxPossibleDistance = (int) (baseRange * 2);
        int totalBlocksEvaluated = 0;
        int totalFacesEvaluated = 0;
        int totalValidReflections = 0;
        int totalPathLengthFailed = 0;
        int totalAbsorptionFailed = 0;
        int totalBlockedFailed = 0;
        int totalEffectiveFailed = 0;
        int totalPassedValidation = 0;

        for (BlockPos candidate : candidates) {
            totalBlocksEvaluated++;

            if (!level.hasChunkAt(candidate)) continue;
            BlockState state = level.getBlockState(candidate);
            if (state.isAir()) continue;

            double reflection = getBlockReflectionValue(state);
            if (reflection <= 0) continue;

            List<Direction> exposedFaces = getExposedFaces(level, candidate);
            if (exposedFaces.isEmpty()) continue;

            for (Direction faceDir : exposedFaces) {
                totalFacesEvaluated++;

                Vec3 reflectionPoint = calculateReflectionPointForFace(level, candidate, faceDir, senderEye, targetPos);

                if (reflectionPoint == null) continue;
                totalValidReflections++;

                double d1 = senderEye.distanceTo(reflectionPoint);
                double d2 = targetPos.distanceTo(reflectionPoint);
                double totalPathLength = d1 + d2;

                if (totalPathLength > baseRange * 2) {
                    totalPathLengthFailed++;
                    continue;
                }

                double absorption1 = calculateSignalAttenuation(level, senderEye, reflectionPoint, baseRange);
                double absorption2 = calculateSignalAttenuation(level, reflectionPoint, targetPos, baseRange);

                if (absorption1 > baseRange || absorption2 > baseRange) {
                    totalAbsorptionFailed++;
                    continue;
                }

                double totalAbsorption = absorption1 + absorption2;
                if (totalAbsorption >= baseRange) {
                    totalAbsorptionFailed++;
                    continue;
                }

                double effectiveRangeAfterReflection = (baseRange - totalAbsorption) * (reflection / 100.0);
                boolean pathValid = effectiveRangeAfterReflection >= totalPathLength;

                if (!pathValid) {
                    totalEffectiveFailed++;
                    continue;
                }
                totalPassedValidation++;

                if (debugVisualizationEnabled) {
                    List<Vec3> valid = debugValidPoints.get(level);
                    if (valid != null) valid.add(reflectionPoint);
                }

                double distanceFactor = 1.0 - Math.min(1.0, totalPathLength / maxPossibleDistance);
                double weight = reflection * REFLECTION_WEIGHT_REFLECTION
                        + distanceFactor * 100.0 * REFLECTION_WEIGHT_DISTANCE;

                if (weight > bestWeight) {
                    bestWeight = weight;
                    bestResult = new ReflectedPathResult(
                            true, candidate, reflectionPoint,
                            totalPathLength, d1, d2,
                            absorption1, absorption2, totalAbsorption,
                            reflection, 0.0,
                            effectiveRangeAfterReflection, weight,
                            faceDir, null, null, null
                    );
                }
            }
        }

        if (bestResult != null) {
            lastSuccessfulReflectionPos.put(level, bestResult.reflectionPos);
            if (debugVisualizationEnabled) {
                debugBestPoint.put(level, bestResult.reflectionPoint);
            }
        }

        if (bestResult == null) {
            return new ReflectedPathResult(false, null, null, 0, 0, 0, 0, 0,
                    0, 0, 0, 0, 0, null, null, null, "未找到有效的反射点");
        }
        return bestResult;
    }

    public static boolean canSignalReachEyeWithReflection(Level level, Entity sender, Entity target, double baseRange,
                                                          String senderName, String targetName,
                                                          Player chatOutputTarget) {
        if (sender == null || target == null) return false;
        if (sender.level() != target.level()) return false;

        Vec3 senderEye = sender.position().add(0.0, EYE_HEIGHT_OFFSET, 0.0);
        Vec3 targetEye = target.position().add(0.0, EYE_HEIGHT_OFFSET, 0.0);
        double straightDistance = senderEye.distanceTo(targetEye);

        AttenuationResult straightResult = calculateSignalAttenuationDetailed(level, senderEye, targetEye, baseRange);
        double straightEffective = baseRange - straightResult.totalAbsorption;
        boolean straightReached = straightEffective >= straightDistance && straightDistance <= baseRange;

        if (straightReached) {
            if (DEBUG_SIGNAL_ATTENUATION) {
                printAttenuationDebug(level, senderName, targetName, straightDistance, baseRange,
                        straightResult, straightEffective, true, "玩家→玩家 (直线)", chatOutputTarget);
            }
            return true;
        }

        ReflectedPathResult reflectionResult = searchReflectionPath(level, senderEye, targetEye, baseRange, DEBUG_SIGNAL_ATTENUATION);

        if (DEBUG_SIGNAL_ATTENUATION) {
            printReflectionDebug(level, senderName, targetName, straightDistance, baseRange,
                    straightResult, reflectionResult, "玩家→玩家 (反射)", chatOutputTarget);
        }

        return reflectionResult.found;
    }

    public static boolean canSignalReachEyeWithReflection(Level level, Entity sender, BlockPos blockPos, double baseRange,
                                                          String senderName, String blockName,
                                                          Player chatOutputTarget) {
        if (sender == null || blockPos == null) return false;

        Vec3 senderEye = sender.position().add(0.0, EYE_HEIGHT_OFFSET, 0.0);
        Vec3 blockCenter = new Vec3(blockPos.getX() + 0.5, blockPos.getY() + 0.5, blockPos.getZ() + 0.5);
        double straightDistance = senderEye.distanceTo(blockCenter);

        AttenuationResult straightResult = calculateSignalAttenuationDetailed(level, senderEye, blockCenter, baseRange);
        double straightEffective = baseRange - straightResult.totalAbsorption;
        boolean straightReached = straightEffective >= straightDistance && straightDistance <= baseRange;

        if (straightReached) {
            if (DEBUG_SIGNAL_ATTENUATION) {
                printAttenuationDebug(level, senderName, blockName, straightDistance, baseRange,
                        straightResult, straightEffective, true, "玩家→收音机 (直线)", chatOutputTarget);
            }
            return true;
        }

        ReflectedPathResult reflectionResult = searchReflectionPath(level, senderEye, blockCenter, baseRange, DEBUG_SIGNAL_ATTENUATION);

        if (DEBUG_SIGNAL_ATTENUATION) {
            printReflectionDebug(level, senderName, blockName, straightDistance, baseRange,
                    straightResult, reflectionResult, "玩家→收音机 (反射)", chatOutputTarget);
        }

        return reflectionResult.found;
    }

    private static void printReflectionDebug(Level level, String senderName, String targetName,
                                             double straightDistance, double baseRange,
                                             AttenuationResult straightResult, ReflectedPathResult reflectionResult,
                                             String mode, Player chatTarget) {
        StringBuilder logBuilder = new StringBuilder();
        logBuilder.append("[SignalReflection] 模式=").append(mode);
        logBuilder.append(", 发送者=").append(senderName);
        logBuilder.append(", 接收者=").append(targetName);
        logBuilder.append("\n  直线距离: ").append(String.format("%.1f", straightDistance)).append(" 格");
        logBuilder.append("\n  基础传播距离: ").append(String.format("%.0f", baseRange)).append(" 格");
        logBuilder.append("\n  直线总吸收: ").append(String.format("%.2f", straightResult.totalAbsorption));
        logBuilder.append("  直线有效距离: ").append(String.format("%.2f", baseRange - straightResult.totalAbsorption));

        if (reflectionResult == null || !reflectionResult.found) {
            logBuilder.append("\n  ★ 直线被阻挡，未找到有效的反射路径 → 信号丢失 ✗");
            if (reflectionResult != null && reflectionResult.blockedReason != null) {
                logBuilder.append(" (原因: ").append(reflectionResult.blockedReason).append(")");
            }
            BroadcastRadio.LOGGER.info(logBuilder.toString());
            if (chatTarget != null) {
                chatTarget.sendSystemMessage(Component.literal("§6======== [信号反射调试] ========"));
                chatTarget.sendSystemMessage(Component.literal("§7模式: §f" + mode));
                chatTarget.sendSystemMessage(Component.literal("§7直线距离: §f" + String.format("%.1f", straightDistance) + "  §7| 直线有效: §c" + String.format("%.2f", baseRange - straightResult.totalAbsorption)));
                chatTarget.sendSystemMessage(Component.literal("§c✗ 直线被阻挡，未找到有效的反射路径"));
                if (reflectionResult != null && reflectionResult.blockedReason != null) {
                    chatTarget.sendSystemMessage(Component.literal("§7  原因: " + reflectionResult.blockedReason));
                }
                chatTarget.sendSystemMessage(Component.literal("§6================================"));
            }
            return;
        }

        ReflectedPathResult r = reflectionResult;
        logBuilder.append("\n  ★ 直线被阻挡，尝试反射路径:");
        logBuilder.append("\n    反射点: (").append(r.reflectionPos.getX())
                .append(", ").append(r.reflectionPos.getY())
                .append(", ").append(r.reflectionPos.getZ()).append(")");
        logBuilder.append("\n    反射能力: ").append(String.format("%.0f%%", r.reflectionValue));
        logBuilder.append("\n    入射角: ").append(String.format("%.1f°", r.incidentAngle));
        logBuilder.append("  法线方向: ").append(r.normalDirection != null ? r.normalDirection.name() : "未知");
        logBuilder.append("\n    路径1 (发送→反射): ").append(String.format("%.1f", r.d1))
                .append(" 格, 吸收: ").append(String.format("%.2f", r.absorption1));
        logBuilder.append("\n    路径2 (反射→接收): ").append(String.format("%.1f", r.d2))
                .append(" 格, 吸收: ").append(String.format("%.2f", r.absorption2));
        logBuilder.append("\n    总路径长度: ").append(String.format("%.1f", r.totalPathLength)).append(" 格");
        logBuilder.append("\n    反射后有效距离: ").append(String.format("%.2f", r.effectiveRangeAfterReflection))
                .append(" 格 (权重: ").append(String.format("%.2f", r.weight)).append(")");
        logBuilder.append("\n  结果: 信号通过反射到达 ✓");

        BroadcastRadio.LOGGER.info(logBuilder.toString());

        if (chatTarget != null) {
            chatTarget.sendSystemMessage(Component.literal("§6======== [信号反射调试] ========"));
            chatTarget.sendSystemMessage(Component.literal("§7模式: §f" + mode));
            chatTarget.sendSystemMessage(Component.literal("§7直线距离: §f" + String.format("%.1f", straightDistance)
                    + "  §7| 直线有效: §c" + String.format("%.2f", baseRange - straightResult.totalAbsorption)));
            chatTarget.sendSystemMessage(Component.literal("§e★ 直线被阻挡，通过反射到达！"));
            chatTarget.sendSystemMessage(Component.literal("§7反射点: §f(" + r.reflectionPos.getX() + ", " + r.reflectionPos.getY() + ", " + r.reflectionPos.getZ() + ")"));
            chatTarget.sendSystemMessage(Component.literal("§7反射能力: §f" + String.format("%.0f%%", r.reflectionValue)
                    + "  §7| 入射角: §f" + String.format("%.1f°", r.incidentAngle)
                    + "  §7| 法线: §f" + (r.normalDirection != null ? r.normalDirection.name() : "未知")));
            chatTarget.sendSystemMessage(Component.literal(String.format("§7发送→反射: §f%.1f格 (吸收%.2f)  §7| 反射→接收: §f%.1f格 (吸收%.2f)",
                    r.d1, r.absorption1, r.d2, r.absorption2)));
            chatTarget.sendSystemMessage(Component.literal(String.format("§7总路径: §f%.1f格  §7| 反射后有效: §a%.2f格  §7| 权重: §f%.2f",
                    r.totalPathLength, r.effectiveRangeAfterReflection, r.weight)));
            chatTarget.sendSystemMessage(Component.literal("§a结果: ✓ 信号通过反射成功到达"));
            chatTarget.sendSystemMessage(Component.literal("§6================================"));
        }
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
                                        // 信号指示（无发送者实体信息 → 使用无线电实体位置处的简化强度）
                                        if (target instanceof ServerPlayer serverPlayer) {
                                            int strength = Math.max(0, 100 - totalInterference);
                                            sendSignalIndicator(serverPlayer, strength, totalInterference);
                                        }
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
                                        // 信号指示
                                        if (target instanceof ServerPlayer serverPlayer) {
                                            int strength = Math.max(0, 100 - totalInterference);
                                            sendSignalIndicator(serverPlayer, strength, totalInterference);
                                        }
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
                            if (!canSignalReachEyeWithReflection(level, sender, checkPos, baseRange,
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
                                        // 信号指示
                                        if (target instanceof ServerPlayer serverPlayer && sender != null && target != sender) {
                                            int strength = calculateSignalStrengthBlock(sender, checkPos, baseRange, level);
                                            sendSignalIndicator(serverPlayer, strength, totalInterference);
                                        }
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
                                        // 信号指示
                                        if (target instanceof ServerPlayer serverPlayer && sender != null && target != sender) {
                                            int strength = calculateSignalStrengthBlock(sender, checkPos, baseRange, level);
                                            sendSignalIndicator(serverPlayer, strength, totalInterference);
                                        }
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

    public static float getHarmonicFrequency(float baseFreq, int harmonicOrder) {
        return baseFreq * (float) harmonicOrder;
    }

    public static double getHarmonicRange(double baseRange, int harmonicOrder) {
        if (harmonicOrder <= 1) return baseRange;
        return baseRange / (double) harmonicOrder;
    }

    public static int getHarmonicStrengthFactor(int harmonicOrder) {
        if (harmonicOrder <= 1) return 1;
        return harmonicOrder;
    }

    public static void sendHarmonicMessageToPlayer(Player player, String senderName, float frequency,
                                                    String message, int harmonicOrder) {
        Component prefixComponent = Component.translatable("item.broadcast_radio.harmonic.prefix", String.valueOf(harmonicOrder));
        Component messageComponent = Component.literal(message);
        Component combinedMessage = prefixComponent.copy().append(messageComponent);
        Component radioMessage = Component.translatable(
                "item.broadcast_radio.walkie_talkie.message",
                senderName,
                String.format("%.1f", frequency),
                combinedMessage
        ).withStyle(net.minecraft.ChatFormatting.GRAY);
        player.sendSystemMessage(radioMessage);
    }

    public static boolean checkHarmonicReception(float senderFreq, float receiverFreq) {
        if (!ReflectionManager.isHarmonicEnabled()) return false;
        int maxOrder = ReflectionManager.getMaxHarmonicOrder();
        for (int n = 2; n <= maxOrder; n++) {
            if (isFrequencyMatch(receiverFreq, getHarmonicFrequency(senderFreq, n))) {
                return true;
            }
        }
        return false;
    }

    public static int getMatchingHarmonicOrder(float senderFreq, float receiverFreq) {
        if (!ReflectionManager.isHarmonicEnabled()) return -1;
        int maxOrder = ReflectionManager.getMaxHarmonicOrder();
        for (int n = 2; n <= maxOrder; n++) {
            if (isFrequencyMatch(receiverFreq, getHarmonicFrequency(senderFreq, n))) {
                return n;
            }
        }
        return -1;
    }

    public static void checkPlayerNearRadioHarmonic(Player target, Entity sender, double baseRange,
                                                     String senderName, float senderFreq, String senderPwd,
                                                     String message, int senderInterference,
                                                     Player chatOutputTarget, int harmonicOrder) {
        Level level = target.level();
        BlockPos playerPos = target.blockPosition();
        float expectedFreq = senderFreq * (float) harmonicOrder;
        for (int dx = -8; dx <= 8; dx++) {
            for (int dy = -8; dy <= 8; dy++) {
                for (int dz = -8; dz <= 8; dz++) {
                    BlockPos checkPos = playerPos.offset(dx, dy, dz);
                    BlockState blockState = level.getBlockState(checkPos);
                    if (!blockState.isAir()) {
                        if (blockState.getBlock() instanceof bili.dongsz.broadcastradio.block.SimpleRadioBlock) {
                            if (!canSignalReachEyeWithReflection(level, sender, checkPos, baseRange,
                                    senderName, "收音机@(" + checkPos.getX() + "," + checkPos.getY() + "," + checkPos.getZ() + ")",
                                    chatOutputTarget)) continue;

                            BlockEntity blockEntity = level.getBlockEntity(checkPos);
                            if (blockEntity instanceof SimpleRadioBlockEntity radioEntity) {
                                float radioFreq = radioEntity.getFrequency();
                                if (isFrequencyMatch(radioFreq, expectedFreq)) {
                                    double distance = target.distanceToSqr(checkPos.getX() + 0.5, checkPos.getY() + 0.5, checkPos.getZ() + 0.5);
                                    if (distance <= RADIO_RANGE * RADIO_RANGE) {
                                        int jammerAtRadio = getJammerInterference(level, checkPos, radioFreq);
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
                                        sendHarmonicMessageToPlayer(target, senderName, radioFreq, displayMessage, harmonicOrder);
                                        // 信号指示
                                        if (target instanceof ServerPlayer serverPlayer && sender != null && target != sender) {
                                            int strength = calculateSignalStrengthBlock(sender, checkPos, baseRange, level);
                                            sendSignalIndicator(serverPlayer, strength, totalInterference);
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
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

    public static boolean checkAnyWalkieTalkieFrequency(
            ItemStack stack, ServerPlayer target, String senderName,
            float senderFreq, String senderPwd, String messageContent,
            int senderInterference, Level level, int harmonicOrder, Entity sender,
            double baseRange, boolean isEncryptedSender) {

        if (stack == null || stack.isEmpty()) return false;

        boolean isPortable = stack.getItem() instanceof bili.dongsz.broadcastradio.item.PortableWalkieTalkieItem;
        boolean isEncrypted = stack.getItem() instanceof bili.dongsz.broadcastradio.item.EncryptedWalkieTalkieItem;

        if (!isPortable && !isEncrypted) return false;

        CompoundTag targetTag;
        float targetFreq;
        String targetPwd;
        int targetNBTInterference;

        if (isPortable) {
            bili.dongsz.broadcastradio.item.PortableWalkieTalkieItem.initNBT(stack);
            targetTag = stack.getTag();
            targetFreq = targetTag.getFloat(bili.dongsz.broadcastradio.item.PortableWalkieTalkieItem.TAG_FREQUENCY);
            targetPwd = targetTag.getString(bili.dongsz.broadcastradio.item.PortableWalkieTalkieItem.TAG_PASSWORD);
            targetNBTInterference = targetTag.getInt(bili.dongsz.broadcastradio.item.PortableWalkieTalkieItem.TAG_INTERFERENCE);
        } else {
            bili.dongsz.broadcastradio.item.EncryptedWalkieTalkieItem.initNBT(stack);
            targetTag = stack.getTag();
            targetFreq = targetTag.getFloat(bili.dongsz.broadcastradio.item.EncryptedWalkieTalkieItem.TAG_FREQUENCY);
            targetPwd = targetTag.getString(bili.dongsz.broadcastradio.item.EncryptedWalkieTalkieItem.TAG_PASSWORD);
            targetNBTInterference = targetTag.getInt(bili.dongsz.broadcastradio.item.EncryptedWalkieTalkieItem.TAG_INTERFERENCE);
        }

        boolean freqMatch;
        if (harmonicOrder <= 1) {
            freqMatch = isFrequencyMatch(targetFreq, senderFreq);
        } else {
            float expectedFreq = senderFreq * (float) harmonicOrder;
            freqMatch = isFrequencyMatch(targetFreq, expectedFreq);
        }

        if (!freqMatch) return false;

        String displayMessage;
        int totalInterference = Math.max(senderInterference, targetNBTInterference);

        if (isEncrypted) {
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
                    displayMessage = generateGarbledText(messageContent, 1.0 / 6.0);
                } else {
                    displayMessage = generateGarbledText(messageContent, 0.0);
                }
            }
        } else {
            if (targetPwd.equals(senderPwd)) {
                displayMessage = messageContent;
            } else {
                return false;
            }
        }

        displayMessage = applyInterference(displayMessage, totalInterference, level);

        // 显示信号指示（仅接收端显示，发送端不显示）
        if (target != null && sender != null && target != sender) {
            int strength = calculateSignalStrength(sender, target, baseRange, level);
            sendSignalIndicator(target, strength, totalInterference);
        }

        if (harmonicOrder <= 1) {
            sendMessageToPlayer(target, senderName, senderFreq, displayMessage);
        } else {
            sendHarmonicMessageToPlayer(target, senderName, targetFreq, displayMessage, harmonicOrder);
        }
        return true;
    }

    public static boolean checkPlayerAnyWalkieTalkie(
            ServerPlayer target, String senderName, float senderFreq,
            String senderPwd, String messageContent, int senderInterference,
            Level level, int harmonicOrder, Entity sender, double baseRange, boolean isEncryptedSender) {

        ItemStack mainHandStack = target.getMainHandItem();
        if (checkAnyWalkieTalkieFrequency(mainHandStack, target, senderName, senderFreq, senderPwd,
                messageContent, senderInterference, level, harmonicOrder, sender, baseRange, isEncryptedSender)) {
            return true;
        }
        ItemStack offHandStack = target.getOffhandItem();
        if (checkAnyWalkieTalkieFrequency(offHandStack, target, senderName, senderFreq, senderPwd,
                messageContent, senderInterference, level, harmonicOrder, sender, baseRange, isEncryptedSender)) {
            return true;
        }
        for (ItemStack targetStack : target.getInventory().items) {
            if (checkAnyWalkieTalkieFrequency(targetStack, target, senderName, senderFreq, senderPwd,
                    messageContent, senderInterference, level, harmonicOrder, sender, baseRange, isEncryptedSender)) {
                return true;
            }
        }

        return false;
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

    /**
     * 根据发送者与接收者的距离、基础传播距离以及沿途吸收值，
     * 计算信号强度 (0-100)。
     */
    public static int calculateSignalStrength(Entity sender, Entity target, double baseRange, Level level) {
        if (sender == null || target == null || level == null || baseRange <= 0.0) return 0;

        Vec3 senderEye = sender.position().add(0.0, EYE_HEIGHT_OFFSET, 0.0);
        Vec3 targetEye = target.position().add(0.0, EYE_HEIGHT_OFFSET, 0.0);
        double straightDistance = senderEye.distanceTo(targetEye);
        if (straightDistance <= 0.0) return 100;

        double totalAbsorption;
        try {
            totalAbsorption = calculateSignalAttenuation(level, senderEye, targetEye, baseRange);
        } catch (Throwable ignored) {
            totalAbsorption = 0.0;
        }
        if (totalAbsorption < 0.0) totalAbsorption = 0.0;

        double maxEffectiveDistance = baseRange - totalAbsorption;
        if (maxEffectiveDistance <= 0.0) return 0;

        double rawStrength = (1.0 - straightDistance / maxEffectiveDistance) * 100.0;
        int strength = (int) Math.round(rawStrength);
        if (strength < 0) strength = 0;
        if (strength > 100) strength = 100;
        return strength;
    }

    // 发送者为方块位置（例如收音机）时的信号强度计算。

    public static int calculateSignalStrengthBlock(Entity sender, BlockPos blockPos, double baseRange, Level level) {
        if (sender == null || blockPos == null || level == null || baseRange <= 0.0) return 0;

        Vec3 senderEye = sender.position().add(0.0, EYE_HEIGHT_OFFSET, 0.0);
        Vec3 blockCenter = new Vec3(blockPos.getX() + 0.5, blockPos.getY() + 0.5, blockPos.getZ() + 0.5);
        double straightDistance = senderEye.distanceTo(blockCenter);
        if (straightDistance <= 0.0) return 100;

        double totalAbsorption;
        try {
            totalAbsorption = calculateSignalAttenuation(level, senderEye, blockCenter, baseRange);
        } catch (Throwable ignored) {
            totalAbsorption = 0.0;
        }
        if (totalAbsorption < 0.0) totalAbsorption = 0.0;

        double maxEffectiveDistance = baseRange - totalAbsorption;
        if (maxEffectiveDistance <= 0.0) return 0;

        double rawStrength = (1.0 - straightDistance / maxEffectiveDistance) * 100.0;
        int strength = (int) Math.round(rawStrength);
        if (strength < 0) strength = 0;
        if (strength > 100) strength = 100;
        return strength;
    }


     //向目标玩家发送信号指示包（仅在服务端可用时发送）

    public static void sendSignalIndicator(ServerPlayer target, int signalStrength, int interference) {
        if (target == null) return;
        int s = Math.max(0, Math.min(100, signalStrength));
        int i = Math.max(0, Math.min(100, interference));
        bili.dongsz.broadcastradio.BroadcastRadio.NETWORK.send(
                net.minecraftforge.network.PacketDistributor.PLAYER.with(() -> target),
                new bili.dongsz.broadcastradio.network.SignalStrengthIndicatorPacket(s, i));
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