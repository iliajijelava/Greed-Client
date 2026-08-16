package fun.ogi.module.impl.list.movement;

import com.google.common.eventbus.Subscribe;
import fun.ogi.events.PacketEvent;
import fun.ogi.events.render.EventUpdate;
import fun.ogi.module.Module;
import fun.ogi.module.ModuleCategory;
import fun.ogi.module.ModuleInformation;
import fun.ogi.module.settings.BooleanSetting;
import fun.ogi.util.StopWatch;
import fun.ogi.util.time.Timer;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import net.minecraft.text.Text;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

@ModuleInformation(moduleName = "No Fall", moduleDesc = "NoFall FunTime and polar BYPA$$ BLAD", moduleCategory = ModuleCategory.MOVEMENT)
public class NoFall extends Module {
    private final BooleanSetting turnOff = new BooleanSetting("Auto Turn off", this, true);
    private boolean frozen;
    private Vec3d savedVelocity = Vec3d.ZERO;
    private final Queue<Packet<?>> packets = new ConcurrentLinkedQueue<>();
    private final Timer timer = new Timer();

    public NoFall() {
        addSetting(turnOff);
    }

    @Override
    public void onEnable() {
        super.onEnable();
        frozen = false;
        timer.reset();
        packets.clear();
        if (mc.player == null || mc.world == null) return;

        if (mc.player.isOnGround()) {
            setEnabled(true);
        } else {
            mc.player.setNoGravity(true);
            savedVelocity = mc.player.getVelocity();
        }
    }

    @Override
    public void onDisable() {
        super.onDisable();
        frozen = false;
        if (mc.player == null) return;
        if (mc.player.isOnGround()) return;

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

        if (timer.finished(25000)) {
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
        double distance = getDistanceToGround();

        boolean falling =
                mc.player != null &&
                        !mc.player.isOnGround() &&
                        mc.player.getVelocity().y < 0;
        if (falling && distance <= 2) {
            if (event.isSend()) {
                if (event.getPacket() instanceof PlayerMoveC2SPacket) {
                    event.cancelEvent();
                } else {
                    packets.add(event.getPacket());
                    event.cancelEvent();
                }
                if(timer.finished(900)){
                    setEnabled(false);
                }
            }
        }
    }

    public static double getDistanceToGround() {

        if (mc.player == null || mc.world == null)
            return -1;

        Vec3d start = mc.player.getPos();
        Vec3d end = start.subtract(0, 100, 0);

        BlockHitResult hit = mc.world.raycast(new RaycastContext(
                start,
                end,
                RaycastContext.ShapeType.COLLIDER,
                RaycastContext.FluidHandling.NONE,
                mc.player
        ));

        if (hit.getType() != HitResult.Type.BLOCK)
            return -1;

        return start.y - hit.getPos().y;
    }
}