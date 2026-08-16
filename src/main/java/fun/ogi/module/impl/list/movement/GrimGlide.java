package fun.ogi.module.impl.list.movement;

import com.google.common.eventbus.Subscribe;
import fun.ogi.events.render.EventTickPre;
import fun.ogi.events.render.EventUpdate;
import fun.ogi.module.Module;
import fun.ogi.module.ModuleCategory;
import fun.ogi.module.ModuleInformation;
import fun.ogi.module.settings.ModeSetting;
import net.minecraft.util.math.Vec3d;


import java.util.concurrent.ThreadLocalRandom;
@ModuleInformation(moduleName = "Grim Glide", moduleDesc = "Elytra speed without fireworks", moduleCategory = ModuleCategory.MOVEMENT)
public class GrimGlide extends Module {

    public static GrimGlide INSTANCE = new GrimGlide();

    private final ModeSetting mode = new ModeSetting("Mode",this, "Standart", "Standart", "RW");

    private long lastTickTime = 0L;
    private int ticksTwo = 0;

    public GrimGlide() {
        addSettings(mode);
    }

    @Subscribe
    public void onUpdate(EventUpdate event) {
        if (!isGliding()) return;

        if (mode.getValue().equals("Standart")) {
            handleGlide();
        }
    }

    @Subscribe
    public void onTick(EventTickPre event) {
        if (!isGliding()) return;

        if (mode.getValue().equals("RW")) {
            handleGlide();
        }
    }

    private void handleGlide() {
        ticksTwo++;

        Vec3d pos = mc.player.getPos();
        float yaw = mc.player.getYaw();
        double forward = mc.player.age % 2 == 0 ? 0.087D : 0.09D;

        double dx = -Math.sin(Math.toRadians(yaw)) * forward;
        double dz = Math.cos(Math.toRadians(yaw)) * forward;

        if (System.currentTimeMillis() - lastTickTime >= 40L) {
            mc.player.setPosition(pos.getX() + dx, pos.getY(), pos.getZ() + dz);
            lastTickTime = System.currentTimeMillis();
        }

        if (ticksTwo % 40 == 0) {
            mc.player.setVelocity(
                    dx * ThreadLocalRandom.current().nextFloat(1.001F, 1.0021F),
                    mc.player.getVelocity().y + 0.00600000075995922D,
                    dz * ThreadLocalRandom.current().nextFloat(1.001F, 1.0021F)
            );
        }
    }

    private boolean isGliding() {
        return mc.player != null && mc.world != null && mc.player.isGliding();
    }

    @Override
    public void onEnable() {
        super.onEnable();
        ticksTwo = 0;
        lastTickTime = System.currentTimeMillis();
    }

    @Override
    public void onDisable() {
        ticksTwo = 0;
        super.onDisable();
    }
}

