package fun.ogi.module.impl.list.player;

import com.google.common.eventbus.Subscribe;
import fun.ogi.events.render.EventUpdate;
import fun.ogi.module.Module;
import fun.ogi.module.ModuleCategory;
import fun.ogi.module.ModuleInformation;
import net.minecraft.network.packet.c2s.play.PlayerInteractItemC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import net.minecraft.util.Hand;
import net.minecraft.util.math.Vec3d;

@ModuleInformation(moduleName = "No Fall",moduleDesc = "Removes damage from fall",moduleCategory = ModuleCategory.PLAYER)
public class NoFall extends Module {
    @Subscribe
    private void onEventUpdate(EventUpdate e){
        if (mc.player.fallDistance > 2.5) {
            Vec3d pos = mc.player.getPos();
            mc.player.networkHandler.sendPacket(new PlayerMoveC2SPacket.Full(pos.x, pos.y, pos.z, mc.player.getYaw(), mc.player.getPitch(), true, true));
            mc.player.networkHandler.sendPacket(new PlayerInteractItemC2SPacket(Hand.MAIN_HAND, 0, mc.player.getYaw(), mc.player.getPitch()));
            mc.player.fallDistance = 0.0F;
        }

    }
}

