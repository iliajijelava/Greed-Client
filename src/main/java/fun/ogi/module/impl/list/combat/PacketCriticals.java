package fun.ogi.module.impl.list.combat;

import com.google.common.eventbus.Subscribe;
import fun.ogi.events.AttackEvent;
import fun.ogi.module.Module;
import fun.ogi.module.ModuleCategory;
import fun.ogi.module.ModuleInformation;
import fun.ogi.util.combat.IdealHitUtils;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;

@ModuleInformation(
        moduleName = "Packet Criticals",
        moduleDesc = "Critical hits under slow falling / in cobweb",
        moduleCategory = ModuleCategory.COMBAT
)
public class PacketCriticals extends Module {

    public PacketCriticals() {
        addSettings();
    }

    @Subscribe
    public void onAttack(AttackEvent event) {
        if (mc.player == null || mc.world == null) return;

        boolean inWeb = IdealHitUtils.isInCobweb();

        double x = mc.player.getX();
        double y = mc.player.getY();
        double z = mc.player.getZ();
        if (inWeb) {
            mc.player.networkHandler.sendPacket(new PlayerMoveC2SPacket.PositionAndOnGround(x, y + 0.00300, z, false, false));
            mc.player.networkHandler.sendPacket(new PlayerMoveC2SPacket.PositionAndOnGround(x, y, z, false, false));
        }
    }
}

