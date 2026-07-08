package bili.dongsz.broadcastradio.api;

import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

public interface IRadioDevice {
    float getFrequency();
    void setInterference(int interference);
    int getInterference();
    Vec3 getPosition();
    Level getDeviceLevel();
}