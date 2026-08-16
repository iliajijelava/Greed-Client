package fun.ogi.module.impl.list.misc;

import com.google.common.eventbus.Subscribe;
import fun.ogi.events.render.EventUpdate;
import fun.ogi.module.Module;
import fun.ogi.module.ModuleCategory;
import fun.ogi.module.ModuleInformation;
import fun.ogi.util.NotificationManager;
import fun.ogi.util.chatutil.ChatUtil;
import fun.ogi.util.player.InventoryUtils;
import fun.ogi.util.time.Timer;
import fun.ogi.util.world.ServerUtil;
import net.minecraft.item.Items;
import net.minecraft.network.packet.c2s.play.PlayerActionC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerInteractBlockC2SPacket;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;

import javax.management.Notification;

@ModuleInformation(moduleName = "Clan upgrade",moduleDesc = "Automatically upgrades clan on Funtime",moduleCategory = ModuleCategory.MISC)
public class ClanUpgrade extends Module {
    private final Timer timer = new Timer();
    @Subscribe
    private void onUpdate(EventUpdate e){
        if(mc.player==null||mc.world==null)return;
        if (ServerUtil.getWorldType().equals("lobby") && timer.finished(5000)) {
            NotificationManager.post("You can't upgrade clan in this world");
            return;
        }
        int slotId = searchItemHotbar(Items.REDSTONE);
        if (slotId == -1) {
            if (timer.finished(5000)) {
                ChatUtil.sendMSG("Нужен" + Formatting.RED + "факел/редстоун " + Formatting.RESET + "в хотбаре");
            }
            return;
        }
        if (mc.player.getInventory().selectedSlot != slotId) {
            mc.player.getInventory().selectedSlot = slotId;
            return;
        }

        BlockPos pos = mc.player.getBlockPos().down();
        if (mc.world.getBlockState(pos).isSolid()) {
            mc.player.setPitch(90);
            if (mc.player.getPitch() >= 89) {
                mc.doItemUse();
                mc.getNetworkHandler().sendPacket(new PlayerInteractBlockC2SPacket(Hand.MAIN_HAND, new BlockHitResult(mc.player.getPos(), Direction.UP,pos,false), 0));
                mc.doAttack();
            }
        }
    }
    private int searchItemHotbar(net.minecraft.item.Item item) {
        for (int i = 0; i < 9; i++) {
            if (mc.player.getInventory().getStack(i).isOf(item)) {
                return i;
            }
        }
        return -1;
    }
}

