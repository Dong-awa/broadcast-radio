package bili.dongsz.broadcastradio.utils;

import bili.dongsz.broadcastradio.BroadcastRadio;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.registries.ForgeRegistries;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ReflectionManager {

    private static final Map<Block, Integer> BLOCK_REFLECTION_MAP = new HashMap<>();
    private static int defaultReflection = 15;
    private static final ResourceLocation DATA_PATH = ResourceLocation.fromNamespaceAndPath(
            BroadcastRadio.MOD_ID, "config/reflection_values.json");

    private static int maxCandidates = 512;
    private static int axialStep = 3;
    private static int radialStep = 3;
    private static int angleStepDegrees = 30;
    private static int numRadialDirections = 12;

    private static boolean enableHarmonic = true;
    private static int maxHarmonicOrder = 3;

    private static boolean initialized = false;

    public static void loadFromResources(Object resourceSource) {
        BLOCK_REFLECTION_MAP.clear();
        defaultReflection = 15;
        maxCandidates = 512;
        axialStep = 3;
        radialStep = 3;
        angleStepDegrees = 30;
        numRadialDirections = 12;
        enableHarmonic = true;
        maxHarmonicOrder = 3;

        ResourceManager resourceManager = resolveResourceManager(resourceSource);
        if (resourceManager == null) {
            BroadcastRadio.LOGGER.warn("[Broadcast Radio] Unable to obtain ResourceManager for reflection, using default reflection values");
            loadFallback();
            return;
        }

        try {
            List<Resource> resources = resourceManager.getResourceStack(DATA_PATH);
            if (resources.isEmpty()) {
                BroadcastRadio.LOGGER.warn("[Broadcast Radio] Reflection data file not found: {}, using defaults", DATA_PATH);
                initialized = true;
                return;
            }

            for (Resource resource : resources) {
                try (InputStream is = resource.open();
                     InputStreamReader reader = new InputStreamReader(is, StandardCharsets.UTF_8)) {
                    JsonElement element = JsonParser.parseReader(reader);
                    if (!element.isJsonObject()) continue;
                    JsonObject root = element.getAsJsonObject();

                    if (root.has("defaultReflection")) {
                        defaultReflection = root.get("defaultReflection").getAsInt();
                    }

                    if (root.has("searchConfig") && root.get("searchConfig").isJsonObject()) {
                        JsonObject searchConfig = root.getAsJsonObject("searchConfig");
                        if (searchConfig.has("maxCandidates")) {
                            maxCandidates = searchConfig.get("maxCandidates").getAsInt();
                        }
                        if (searchConfig.has("axialStep")) {
                            axialStep = searchConfig.get("axialStep").getAsInt();
                        }
                        if (searchConfig.has("radialStep")) {
                            radialStep = searchConfig.get("radialStep").getAsInt();
                        }
                        if (searchConfig.has("angleStepDegrees")) {
                            angleStepDegrees = searchConfig.get("angleStepDegrees").getAsInt();
                            if (angleStepDegrees > 0) {
                                numRadialDirections = Math.max(1, 360 / angleStepDegrees);
                            }
                        }
                        if (searchConfig.has("numRadialDirections")) {
                            numRadialDirections = searchConfig.get("numRadialDirections").getAsInt();
                        }
                    }

                    if (root.has("harmonic") && root.get("harmonic").isJsonObject()) {
                        JsonObject harmonicConfig = root.getAsJsonObject("harmonic");
                        if (harmonicConfig.has("enableHarmonic")) {
                            enableHarmonic = harmonicConfig.get("enableHarmonic").getAsBoolean();
                        }
                        if (harmonicConfig.has("maxHarmonicOrder")) {
                            maxHarmonicOrder = harmonicConfig.get("maxHarmonicOrder").getAsInt();
                        }
                    }

                    if (root.has("blocks") && root.get("blocks").isJsonObject()) {
                        JsonObject blocksObj = root.getAsJsonObject("blocks");
                        for (Map.Entry<String, JsonElement> entry : blocksObj.entrySet()) {
                            String blockId = entry.getKey();
                            int value = entry.getValue().getAsInt();
                            ResourceLocation rl = ResourceLocation.tryParse(blockId);
                            if (rl == null) continue;
                            Block block = ForgeRegistries.BLOCKS.getValue(rl);
                            if (block == null || ForgeRegistries.BLOCKS.getKey(block) == null) {
                                continue;
                            }
                            BLOCK_REFLECTION_MAP.put(block, value);
                        }
                    }
                } catch (Exception e) {
                    BroadcastRadio.LOGGER.error("[Broadcast Radio] Failed to parse reflection data file: {}", e.getMessage());
                }
            }

            initialized = true;
            CommunicationUtils.setReflectionSearchConfig(maxCandidates, axialStep, radialStep, numRadialDirections);
            BroadcastRadio.LOGGER.info("[Broadcast Radio] Loaded {} block reflection values (default={}), "
                    + "search: maxCandidates={}, axialStep={}, radialStep={}, numRadialDirections={}; "
                    + "harmonic: enabled={}, maxOrder={}",
                    BLOCK_REFLECTION_MAP.size(), defaultReflection,
                    maxCandidates, axialStep, radialStep, numRadialDirections,
                    enableHarmonic, maxHarmonicOrder);
        } catch (Exception e) {
            BroadcastRadio.LOGGER.error("[Broadcast Radio] Failed to load reflection data: {}", e.getMessage());
            loadFallback();
        }
    }

    private static ResourceManager resolveResourceManager(Object source) {
        if (source == null) return null;
        if (source instanceof ResourceManager rm) return rm;
        if (source instanceof MinecraftServer server) {
            return server.getResourceManager();
        }
        try {
            java.lang.reflect.Method method = source.getClass().getMethod("getResourceManager");
            if (ResourceManager.class.isAssignableFrom(method.getReturnType())) {
                return (ResourceManager) method.invoke(source);
            }
        } catch (NoSuchMethodException ignored) {
        } catch (Exception e) {
            BroadcastRadio.LOGGER.warn("[Broadcast Radio] Failed to resolve ResourceManager via reflection: {}", e.getMessage());
        }
        try {
            Class<?> rmClass = Class.forName("net.minecraft.server.packs.resources.ResourceManager");
            if (rmClass.isInstance(source)) {
                return (ResourceManager) source;
            }
        } catch (ClassNotFoundException ignored) {
        }
        return null;
    }

    private static void loadFallback() {
        BLOCK_REFLECTION_MAP.clear();
        defaultReflection = 15;
        maxCandidates = 512;
        axialStep = 3;
        radialStep = 3;
        angleStepDegrees = 30;
        numRadialDirections = 12;
        enableHarmonic = true;
        maxHarmonicOrder = 4;
        initialized = true;
        CommunicationUtils.setReflectionSearchConfig(maxCandidates, axialStep, radialStep, numRadialDirections);
    }

    public static int getMaxCandidates() {
        return maxCandidates;
    }

    public static int getAxialStep() {
        return axialStep;
    }

    public static int getRadialStep() {
        return radialStep;
    }

    public static int getNumRadialDirections() {
        return numRadialDirections;
    }

    public static int getAngleStepDegrees() {
        return angleStepDegrees;
    }

    public static int getReflection(Block block) {
        if (!initialized) return defaultReflection;
        if (block == null) return defaultReflection;
        return BLOCK_REFLECTION_MAP.getOrDefault(block, defaultReflection);
    }

    public static int getReflection(BlockState state) {
        if (state == null) return defaultReflection;
        return getReflection(state.getBlock());
    }

    public static int getDefaultReflection() {
        return defaultReflection;
    }

    public static boolean isHarmonicEnabled() {
        return enableHarmonic;
    }

    public static int getMaxHarmonicOrder() {
        return maxHarmonicOrder;
    }

    public static boolean isInitialized() {
        return initialized;
    }

    public static int getReflectionMapSize() {
        return BLOCK_REFLECTION_MAP.size();
    }
}