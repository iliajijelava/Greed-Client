package fun.ogi.util.server;

import java.util.Arrays;

import com.google.common.eventbus.Subscribe;
import fun.ogi.Cheap;
import fun.ogi.events.PacketEvent;
import net.minecraft.network.packet.s2c.play.WorldTimeUpdateS2CPacket;
import net.minecraft.util.math.MathHelper;

public class TPSHandler {
    private final float[] tickRates = new float[20];
    private int nextIndex = 0;
    private long timeLastTimeUpdate;

    public TPSHandler() {
        this.nextIndex = 0;
        this.timeLastTimeUpdate = -1L;
        Arrays.fill(this.tickRates, 0.0F);
        Cheap.getInstance().getEventBus().register(this);
    }

    @Subscribe
    public void onReceivePacket(PacketEvent event) {
        if (event.isSend()) return;
        if (event.getPacket() instanceof WorldTimeUpdateS2CPacket) {
            if (this.timeLastTimeUpdate != -1L) {
                float timeElapsed = (float) (System.nanoTime() - this.timeLastTimeUpdate) / 1.0E9F;
                this.tickRates[this.nextIndex % this.tickRates.length] = MathHelper.clamp(20.0F / timeElapsed, 0.0F, 20.0F);
                this.nextIndex++;
            }
            this.timeLastTimeUpdate = System.nanoTime();
        }
    }

    public float getTPS() {
        float numTicks = 0.0F;
        float sumTickRates = 0.0F;

        for (float tickRate : this.tickRates) {
            if (tickRate > 0.0F) {
                sumTickRates += tickRate;
                numTicks++;
            }
        }

        return numTicks > 0 ? MathHelper.clamp(sumTickRates / numTicks, 0.0F, 20.0F) : 20.0F;
    }
}

