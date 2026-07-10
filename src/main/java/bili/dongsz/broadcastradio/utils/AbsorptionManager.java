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

public class AbsorptionManager {

    private static final Map<Block, Integer> BLOCK_ABSORPTION_MAP = new HashMap<>();
    private static int defaultAbsorption = 5;
    private static final ResourceLocation DATA_PATH = ResourceLocation.fromNamespaceAndPath(
            BroadcastRadio.MOD_ID, "config/absorption_values.json");

    private static boolean initialized = false;

    public static void loadFromResources(Object resourceSource) {
        BLOCK_ABSORPTION_MAP.clear();
        defaultAbsorption = 5;

        ResourceManager resourceManager = resolveResourceManager(resourceSource);
        if (resourceManager == null) {
            BroadcastRadio.LOGGER.warn("[BroadcastRadio] Unable to obtain ResourceManager, using default absorption values");
            loadFallback();
            return;
        }

        try {
            List<Resource> resources = resourceManager.getResourceStack(DATA_PATH);
            if (resources.isEmpty()) {
                BroadcastRadio.LOGGER.warn("[BroadcastRadio] Absorption data file not found: {}, using defaults", DATA_PATH);
                initialized = true;
                return;
            }

            for (Resource resource : resources) {
                try (InputStream is = resource.open();
                     InputStreamReader reader = new InputStreamReader(is, StandardCharsets.UTF_8)) {
                    JsonElement element = JsonParser.parseReader(reader);
                    if (!element.isJsonObject()) continue;
                    JsonObject root = element.getAsJsonObject();

                    if (root.has("defaultAbsorption")) {
                        defaultAbsorption = root.get("defaultAbsorption").getAsInt();
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
                            BLOCK_ABSORPTION_MAP.put(block, value);
                        }
                    }
                } catch (Exception e) {
                    BroadcastRadio.LOGGER.error("[BroadcastRadio] Failed to parse absorption data file: {}", e.getMessage());
                }
            }

            initialized = true;
            BroadcastRadio.LOGGER.info("[BroadcastRadio] Loaded {} block absorption values (default={})",
                    BLOCK_ABSORPTION_MAP.size(), defaultAbsorption);
        } catch (Exception e) {
            BroadcastRadio.LOGGER.error("[BroadcastRadio] Failed to load absorption data: {}", e.getMessage());
            loadFallback();
        }
    }

    private static ResourceManager resolveResourceManager(Object source) {
        if (source == null) return null;
        if (source instanceof ResourceManager rm) return rm;
        if (source instanceof MinecraftServer server) {
            return server.getResourceManager();
        }
        // Try reflection for ReloadableServerResources
        try {
            java.lang.reflect.Method method = source.getClass().getMethod("getResourceManager");
            if (ResourceManager.class.isAssignableFrom(method.getReturnType())) {
                return (ResourceManager) method.invoke(source);
            }
        } catch (NoSuchMethodException ignored) {
            // Fall through
        } catch (Exception e) {
            BroadcastRadio.LOGGER.warn("[BroadcastRadio] Failed to resolve ResourceManager via reflection: {}", e.getMessage());
        }
        // Maybe the object itself is a ResourceManager in newer versions
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
        BLOCK_ABSORPTION_MAP.clear();
        defaultAbsorption = 5;
        initialized = true;
    }

    public static int getAbsorption(Block block) {
        if (!initialized) return defaultAbsorption;
        if (block == null) return defaultAbsorption;
        return BLOCK_ABSORPTION_MAP.getOrDefault(block, defaultAbsorption);
    }

    public static int getAbsorption(BlockState state) {
        if (state == null) return defaultAbsorption;
        return getAbsorption(state.getBlock());
    }

    public static int getDefaultAbsorption() {
        return defaultAbsorption;
    }

    public static boolean isInitialized() {
        return initialized;
    }

    public static int getAbsorptionMapSize() {
        return BLOCK_ABSORPTION_MAP.size();
    }
}