package fun.ogi.module.impl.list.player;

import com.google.common.eventbus.Subscribe;
import fun.ogi.events.PlayerCollisionEvent;
import fun.ogi.events.PushEvent;
import fun.ogi.module.Module;
import fun.ogi.module.ModuleCategory;
import fun.ogi.module.ModuleInformation;
import fun.ogi.module.settings.ListSetting;
import net.minecraft.block.Blocks;

@ModuleInformation(moduleName = "NoPush", moduleDesc = "Prevents various push forces", moduleCategory = ModuleCategory.PLAYER)
public class NoPush extends Module {

    public final ListSetting ignoreSetting = new ListSetting("Ignore", this, "Water", "Block", "Entity Collision", "Powder Snow", "Berry");

    public NoPush() {
        addSetting(ignoreSetting);
    }

    @Subscribe
    public void onPush(PushEvent e) {
        switch (e.getType()) {
            case COLLISION -> e.setCancelled(ignoreSetting.isSelected("Entity Collision"));
            case WATER -> e.setCancelled(ignoreSetting.isSelected("Water"));
            case BLOCK -> e.setCancelled(ignoreSetting.isSelected("Block"));
        }
    }

    @Subscribe
    public void onPlayerCollision(PlayerCollisionEvent e) {
        if (e.getBlock().equals(Blocks.POWDER_SNOW)) e.setCancelled(ignoreSetting.isSelected("Powder Snow"));
        else if (e.getBlock().equals(Blocks.SWEET_BERRY_BUSH)) e.setCancelled(ignoreSetting.isSelected("Berry"));
    }
}

