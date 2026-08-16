package fun.ogi.module.impl.list.movement;

import com.google.common.eventbus.Subscribe;
import fun.ogi.events.PacketEvent;
import fun.ogi.events.render.EventUpdate;
import fun.ogi.module.Module;
import fun.ogi.module.ModuleCategory;
import fun.ogi.module.ModuleInformation;
import fun.ogi.module.settings.BooleanSetting;
import fun.ogi.module.settings.ModeSetting;
import fun.ogi.module.settings.SliderSetting;
import fun.ogi.util.StopWatch;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;

import java.util.ArrayList;
import java.util.List;

@ModuleInformation(moduleName = "Blink", moduleDesc = "Holds packets and releases them in bursts", moduleCategory = ModuleCategory.MOVEMENT)
public class Blink extends Module {

    private final ModeSetting mode = new ModeSetting("Mode", this, "Blink", "Blink", "Pulse");
    private final SliderSetting delay = new SliderSetting("Delay", this, 500, 50, 2000, 50);
    private final BooleanSetting onlyMovement = new BooleanSetting("OnlyMovement", this, true);

    private final List<Packet<?>> packets = new ArrayList<>();
    private final StopWatch timer = new StopWatch();
    private boolean releasing = false;

    public Blink() {
        addSettings(mode, delay, onlyMovement);
    }

    @Override
    public void onEnable() {
        super.onEnable();
        packets.clear();
        timer.reset();
        releasing = false;
    }

    @Override
    public void onDisable() {
        super.onDisable();
        releasePackets();
    }

    @Subscribe
    public void onUpdate(EventUpdate event) {
        if (mc.player == null) return;

        if (mode.getValue().equals("Pulse") && timer.finished((long) delay.getValue())) {
            releasePackets();
            timer.reset();
        }
    }

    @Subscribe
    public void onPacket(PacketEvent event) {
        if (mc.player == null || releasing) return;

        if (event.isSend()) {
            boolean shouldHold;
            if (onlyMovement.getValue()) {
                shouldHold = event.getPacket() instanceof PlayerMoveC2SPacket;
            } else {
                shouldHold = true;
            }

            if (shouldHold) {
                event.cancelEvent();
                packets.add(event.getPacket());
            }
        }
    }

    private void releasePackets() {
        if (packets.isEmpty()) return;

        releasing = true;
        for (Packet<?> packet : packets) {
            mc.player.networkHandler.sendPacket(packet);
        }
        packets.clear();
        releasing = false;
    }
}

