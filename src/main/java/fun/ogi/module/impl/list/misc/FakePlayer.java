package fun.ogi.module.impl.list.misc;

import com.google.common.eventbus.Subscribe;
import com.mojang.authlib.GameProfile;
import fun.ogi.events.render.EventUpdate;
import fun.ogi.module.Module;
import fun.ogi.module.ModuleCategory;
import fun.ogi.module.ModuleInformation;
import net.minecraft.client.network.OtherClientPlayerEntity;
import net.minecraft.entity.Entity;

import java.util.UUID;

@ModuleInformation(moduleName = "FakePlayer", moduleCategory = ModuleCategory.MISC)
public class FakePlayer extends Module {
    private static OtherClientPlayerEntity fakePlayer;

    public static OtherClientPlayerEntity getFakePlayer() {
        return fakePlayer;
    }

    @Override
    public void onEnable() {
        if (mc.player == null || mc.world == null) return;

        GameProfile profile = new GameProfile(UUID.randomUUID(), mc.player.getName().getString());
        fakePlayer = new OtherClientPlayerEntity(mc.world, profile);
        fakePlayer.copyFrom(mc.player);

        
        fakePlayer.refreshPositionAndAngles(
                mc.player.getX(), mc.player.getY(), mc.player.getZ(),
                mc.player.getYaw(), mc.player.getPitch()
        );
        fakePlayer.prevX = fakePlayer.getX();
        fakePlayer.prevY = fakePlayer.getY();
        fakePlayer.prevZ = fakePlayer.getZ();
        fakePlayer.prevYaw = fakePlayer.getYaw();
        fakePlayer.prevPitch = fakePlayer.getPitch();
        fakePlayer.headYaw = fakePlayer.getYaw();
        fakePlayer.bodyYaw = fakePlayer.getYaw();

        mc.world.addEntity(fakePlayer);
    }

    @Subscribe
    public void onUpdate(EventUpdate event) {
        if (mc.player == null || mc.world == null || fakePlayer == null) return;

        
        if (fakePlayer.getHealth() < fakePlayer.getMaxHealth()) {
            fakePlayer.setHealth(fakePlayer.getMaxHealth());
            fakePlayer.deathTime = 0;
        }
    }

    @Override
    public void onDisable() {
        if (mc.world != null && fakePlayer != null) {
            mc.world.removeEntity(fakePlayer.getId(), Entity.RemovalReason.DISCARDED);
            fakePlayer = null;
        }
    }
}

