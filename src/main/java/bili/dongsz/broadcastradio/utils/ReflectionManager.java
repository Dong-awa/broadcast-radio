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
            BroadcastRadio.MOD_ID, "reflection/reflection_values.json");

    private static boolean initialized = false;

    public static void loadFromResources(Object resourceSource) {
        BLOCK_REFLECTION_MAP.clear();
        defaultReflection = 15;

        ResourceManager resourceManager = resolveResourceManager(resourceSource);
        if (resourceManager == null) {
            BroadcastRadio.LOGGER.warn("[BroadcastRadio] Unable to obtain ResourceManager for reflection, using default reflection values");
            loadFallback();
            return;
        }

        try {
            List<Resource> resources = resourceManager.getResourceStack(DATA_PATH);
            if (resources.isEmpty()) {
                BroadcastRadio.LOGGER.warn("[BroadcastRadio] Reflection data file not found: {}, using defaults", DATA_PATH);
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
                    BroadcastRadio.LOGGER.error("[BroadcastRadio] Failed to parse reflection data file: {}", e.getMessage());
                }
            }

            initialized = true;
            BroadcastRadio.LOGGER.info("[BroadcastRadio] Loaded {} block reflection values (default={})",
                    BLOCK_REFLECTION_MAP.size(), defaultReflection);
        } catch (Exception e) {
            BroadcastRadio.LOGGER.error("[BroadcastRadio] Failed to load reflection data: {}", e.getMessage());
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
            BroadcastRadio.LOGGER.warn("[BroadcastRadio] Failed to resolve ResourceManager via reflection: {}", e.getMessage());
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
        initialized = true;
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

    public static boolean isInitialized() {
        return initialized;
    }

    public static int getReflectionMapSize() {
        return BLOCK_REFLECTION_MAP.size();
    }
}