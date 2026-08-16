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
import net.minecraft.text.Text;
import net.minecraft.util.math.Vec3d;

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

@ModuleInformation(
        moduleName = "AirStuck",
        moduleDesc = "Freezes you in mid-air",
        moduleCategory = ModuleCategory.MOVEMENT
)
public class AirStuck extends Module {

    private final ModeSetting mode = new ModeSetting("Mode", this, "Universal", "Universal", "Grim", "Funtime (Polar)");
    private final BooleanSetting fallCheck = new BooleanSetting("Fall Check", this, true);
    private final BooleanSetting freezeOnFall = new BooleanSetting("FreezeOnFall", this, false);
    private final BooleanSetting extraRangeEnabled = new BooleanSetting("ExtraRange", this, false);
    private final SliderSetting extraRange = new SliderSetting("Range", this, 1.0, 1.0, 5.0, 0.1);

    private Vec3d freezePos;
    private boolean frozen;
    private Vec3d savedVelocity = Vec3d.ZERO;
    private final Queue<Packet<?>> packets = new ConcurrentLinkedQueue<>();
    private final StopWatch stopWatch = new StopWatch();

    public AirStuck() {
        addSettings(
                mode,
                fallCheck,
                freezeOnFall,
                extraRangeEnabled,
                extraRange
        );
    }

    @Override
    public void onEnable() {
        super.onEnable();
        frozen = false;
        stopWatch.reset();
        packets.clear();
        if (mc.player == null || mc.world == null) return;

        if (mc.player.isOnGround() && fallCheck.getValue()) {
            mc.player.sendMessage(Text.literal("Вам нужно находиться в воздухе"), false);
            setEnabled(false);
            return;
        }

        mc.player.setNoGravity(true);
        savedVelocity = mc.player.getVelocity();

        if (mode.is("Universal") && !freezeOnFall.getValue()) {
            freezePos = mc.player.getPos();
            frozen = true;
        }
    }

    @Override
    public void onDisable() {
        super.onDisable();
        frozen = false;
        freezePos = null;
        if (mc.player == null) return;
        if (mc.player.isOnGround() && fallCheck.getValue()) return;

        if (!packets.isEmpty()) {
            for (Packet<?> packet : packets) {
                if (mc.getNetworkHandler() != null) {
                    mc.getNetworkHandler().sendPacket(packet);
                }
            }
            packets.clear();
        }

        if (savedVelocity != null) {
            mc.player.setVelocity(savedVelocity);
        }
        mc.player.setNoGravity(false);
    }

    @Subscribe
    public void onUpdate(EventUpdate event) {
        if (mc.player == null)
            return;

        if (mode.is("Universal")) {
            if (freezeOnFall.getValue() && !frozen) {
                if (!mc.player.isOnGround()
                        && mc.player.getVelocity().y < 0) {
                    freezePos = mc.player.getPos();
                    frozen = true;
                }
            }
            if (frozen && freezePos != null) {
                mc.player.setVelocity(0, 0, 0);
                mc.player.setPos(freezePos.x, freezePos.y, freezePos.z);
                mc.player.setOnGround(false);
            }
            return;
        }

        if (mode.is("Funtime (Polar)") && stopWatch.finished(28000)) {
            mc.player.sendMessage(Text.literal("§c[AirStuck] Автоматически выключен (защита от кика)"), false);
            setEnabled(false);
            return;
        }

        mc.player.setVelocity(0, 0, 0);
        mc.player.setNoGravity(true);
    }

    @Subscribe
    public void onPacket(PacketEvent event) {
        if (!isEnabled() || mc.player == null)
            return;

        if (mode.is("Funtime (Polar)")) {
            if (event.isSend()) {
                if (event.getPacket() instanceof PlayerMoveC2SPacket) {
                    event.cancelEvent();
                } else {
                    packets.add(event.getPacket());
                    event.cancelEvent();
                }
            }
        } else if (mode.is("Grim")) {
            if (event.getPacket() instanceof PlayerMoveC2SPacket) {
                event.cancelEvent();
            }
        } else if (frozen && event.getPacket() instanceof PlayerMoveC2SPacket) {
            event.cancelEvent();
        }
    }

    public float getExtraRange() {
        if (isEnabled()
                && extraRangeEnabled.getValue()) {
            return extraRange.getFloatValue();
        }
        return 0;
    }
}

