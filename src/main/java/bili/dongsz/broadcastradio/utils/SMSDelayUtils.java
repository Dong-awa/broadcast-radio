package bili.dongsz.broadcastradio.utils;

import bili.dongsz.broadcastradio.block.entity.RadioBaseStationBlockEntity;
import net.minecraftforge.fml.DistExecutor;

public class SMSDelayUtils {
    private static final RadioBaseStationBlockEntity.NetworkType DEFAULT_NETWORK_TYPE = RadioBaseStationBlockEntity.NetworkType.FOUR_G;
    
    public static RadioBaseStationBlockEntity.NetworkType getPlayerCurrentNetworkType() {
        final RadioBaseStationBlockEntity.NetworkType[] result = {DEFAULT_NETWORK_TYPE};
        DistExecutor.unsafeRunWhenOn(net.minecraftforge.api.distmarker.Dist.CLIENT, () -> () -> {
            result[0] = bili.dongsz.broadcastradio.client.ClientProxy.getPlayerCurrentNetworkType();
        });
        return result[0];
    }
    
    public static int getNetworkDelay(RadioBaseStationBlockEntity.NetworkType networkType) {
        switch (networkType) {
            case TWO_G:
                return 2500;
            case THREE_G:
                return 1500;
            case FOUR_G:
            default:
                return 500;
        }
    }
    
    public static int getPlayerCurrentNetworkDelay() {
        return getNetworkDelay(getPlayerCurrentNetworkType());
    }
}