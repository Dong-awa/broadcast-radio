package bili.dongsz.broadcastradio.server;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class RadioServerData {
    private static final Map<UUID, Boolean> playerSignalStatus = new HashMap<>();
    
    public static void setPlayerSignalStatus(UUID playerId, boolean hasSignal) {
        playerSignalStatus.put(playerId, hasSignal);
    }
    
    public static boolean getPlayerSignalStatus(UUID playerId) {
        return playerSignalStatus.getOrDefault(playerId, false);
    }
    
    public static void removePlayer(UUID playerId) {
        playerSignalStatus.remove(playerId);
    }
}