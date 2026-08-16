package fun.ogi.command.impl;

import fun.ogi.command.Command;
import fun.ogi.util.chatutil.ChatUtil;
import fun.ogi.util.storages.WaypointStorage;
import fun.ogi.util.storages.waypoint.Waypoint;

public class GPSCommand extends Command {

    public GPSCommand() {
        super("gps", "Set a GPS arrow to coordinates", "navigate", "nav");
    }

    @Override
    public void execute(String[] args) {
        if (args.length == 0) {
            usage("<X> <Z> | remove");
            return;
        }

        if (args[0].equalsIgnoreCase("remove")) {
            WaypointStorage.getInstance().clearGps();
            ChatUtil.sendMSG("§aGPS removed!");
            return;
        }

        if (args.length < 2) {
            usage("<X> <Z>");
            return;
        }

        try {
            int x = Integer.parseInt(args[0]);
            int z = Integer.parseInt(args[1]);

            WaypointStorage.getInstance().setGps(new Waypoint("gps", x, 0, z));
            ChatUtil.sendMSG("§aGPS set to X: " + x + " Z: " + z);
        } catch (NumberFormatException e) {
            ChatUtil.sendMSG("§cInvalid coordinates.");
        }
    }
}

