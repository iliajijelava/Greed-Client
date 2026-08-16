package fun.ogi.module.impl.list.movement;

import com.google.common.eventbus.Subscribe;
import com.mojang.authlib.GameProfile;
import fun.ogi.events.PacketEvent;
import fun.ogi.events.render.EventHud;
import fun.ogi.events.render.EventUpdate;
import fun.ogi.events.render.EventWorldRenderer;
import fun.ogi.module.Module;
import fun.ogi.module.ModuleCategory;
import fun.ogi.module.ModuleInformation;
import fun.ogi.module.settings.BooleanSetting;
import fun.ogi.module.settings.SliderSetting;
import net.minecraft.client.network.OtherClientPlayerEntity;
import net.minecraft.entity.Entity;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.GameMode;

import java.util.UUID;

@ModuleInformation(moduleName = "FreeCam", moduleDesc = "Свободная камера", moduleCategory = ModuleCategory.MOVEMENT)
public class FreeCam extends Module {
    private boolean wasFlyingAllowed;
    private boolean wasFlying;
    private float oldFlyingSpeed;
    private GameMode prevGameMode;
    private OtherClientPlayerEntity dummy;
    private Vec3d frozenPos;
    private BlockPos startPos;

    private final SliderSetting speed = new SliderSetting("Speed", this, 3.0, 0.1, 15.0, 0.1);
    private final BooleanSetting showCoords = new BooleanSetting("Show Coords", this, false);

    public FreeCam() {
        addSettings(speed, showCoords);
    }

    public OtherClientPlayerEntity getFakePlayer() {
        return dummy;
    }

    public void onEnable() {
        if (mc.player == null || mc.world == null) return;

        this.wasFlyingAllowed = mc.player.getAbilities().allowFlying;
        this.wasFlying = mc.player.getAbilities().flying;
        this.oldFlyingSpeed = mc.player.getAbilities().getFlySpeed();
        this.prevGameMode = mc.interactionManager.getCurrentGameMode();
        this.frozenPos = mc.player.getPos();
        this.startPos = mc.player.getBlockPos();

        GameProfile profile = new GameProfile(UUID.randomUUID(), mc.getSession().getUsername());
        this.dummy = new OtherClientPlayerEntity(mc.world, profile);
        this.dummy.copyFrom(mc.player);
        this.dummy.setPos(mc.player.getX(), mc.player.getY(), mc.player.getZ());
        mc.world.addEntity(this.dummy);

        mc.player.getAbilities().allowFlying = true;
        mc.player.getAbilities().flying = true;
        mc.player.getAbilities().setFlySpeed(speed.getFloatValue() / 10.0F);
        mc.interactionManager.setGameMode(GameMode.SPECTATOR);
    }

    @Override
    public void onDisable() {
        if (mc.player == null || mc.world == null || mc.getNetworkHandler() == null) return;

        if (this.dummy != null) {
            mc.player.setPos(this.dummy.getX(), this.dummy.getY(), this.dummy.getZ());
            mc.getNetworkHandler().sendPacket(
                    new PlayerMoveC2SPacket.PositionAndOnGround(
                            this.dummy.getX(), this.dummy.getY(), this.dummy.getZ(),
                            mc.player.isOnGround(), false
                    )
            );
            mc.world.removeEntity(this.dummy.getId(), Entity.RemovalReason.DISCARDED);
            this.dummy = null;
        }

        mc.player.getAbilities().allowFlying = this.wasFlyingAllowed;
        mc.player.getAbilities().flying = this.wasFlying;
        mc.player.getAbilities().setFlySpeed(this.oldFlyingSpeed);
        mc.interactionManager.setGameMode(this.prevGameMode);
        mc.player.setVelocity(0.0, 0.0, 0.0);
    }

    @Subscribe
    public void onPacket(PacketEvent event) {
        if (event.getPacket() instanceof PlayerMoveC2SPacket) {
            event.cancelEvent();
        }
    }

    @Subscribe
    public void onUpdate(EventUpdate event) {
        if (mc.player == null) return;

        mc.player.noClip = true;
        mc.player.getAbilities().flying = false;

        double s = speed.getValue();
        double forward = mc.player.input.movementForward;
        double strafe = mc.player.input.movementSideways;
        double yaw = Math.toRadians(mc.player.getYaw());

        double motionX = 0;
        double motionZ = 0;

        if (forward != 0 || strafe != 0) {
            double angle = yaw + Math.atan2(-strafe, forward);
            motionX = -Math.sin(angle) * s;
            motionZ = Math.cos(angle) * s;
        }

        double motionY = 0;
        if (mc.options.jumpKey.isPressed()) {
            motionY = s;
        } else if (mc.options.sneakKey.isPressed()) {
            motionY = -s;
        }

        mc.player.setVelocity(motionX, motionY, motionZ);
    }

    @Subscribe
    public void onHud(EventHud event) {
        if (!showCoords.getValue() || mc.player == null || startPos == null) return;

        BlockPos diff = mc.player.getBlockPos().subtract(startPos);
        String text = String.format("X: %d  Y: %d  Z: %d", diff.getX(), diff.getY(), diff.getZ());
        int width = mc.textRenderer.getWidth(text);
        int centerX = mc.getWindow().getScaledWidth() / 2;
        int centerY = mc.getWindow().getScaledHeight() / 2;

        event.getDrawContext().drawText(
                mc.textRenderer, text,
                centerX - width / 2 + 8, centerY - 20,
                0xFFFFFFFF, true
        );
    }
}