package fun.ogi.command.impl;

import fun.ogi.Cheap;
import fun.ogi.command.Command;
import fun.ogi.util.chatutil.ChatUtil;
import net.minecraft.util.math.Vec3d;

public class VClipCommand extends Command {
    public VClipCommand(){super("VClip", "VClip [Y]", "V");}
//    private float playerY = mc.player.getY();
    @Override
    public void execute(String[] args){
        if (args.length == 0) {
            usage("[Y]");
            return;
        }
        double y = Double.parseDouble(args[0]);
        assert mc.player != null;
        mc.player.setPosition(mc.player.getX(), y, mc.player.getZ());
    };
}
