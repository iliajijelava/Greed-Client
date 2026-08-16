package fun.ogi.command.impl;

import fun.ogi.command.Command;
import fun.ogi.screens.WaypointScreen;
import fun.ogi.util.chatutil.ChatUtil;
import fun.ogi.util.storages.WaypointStorage;
import fun.ogi.util.storages.waypoint.Waypoint;
import net.minecraft.client.MinecraftClient;

import java.util.Map;

public class WaypointCommand extends Command {

    public WaypointCommand() {
        super("way", "Manage waypoints (add/remove/clear/list/gui)", "w", "waypoint");
    }

    @Override
    public void execute(String[] args) {
        if (args.length == 0) {
            usage("add <name> <x> <y> <z> | remove <name> | clear | list | gui");
            return;
        }

        switch (args[0].toLowerCase()) {
            case "add" -> addWaypoint(args);
            case "remove" -> removeWaypoint(args);
            case "clear" -> clearWaypoints();
            case "list" -> listWaypoints();
            case "gui" -> openGui();
            default -> usage("add <name> <x> <y> <z> | remove <name> | clear | list | gui");
        }
    }

    private void openGui() {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.currentScreen instanceof WaypointScreen) return;
        mc.setScreen(new WaypointScreen(mc.currentScreen));
    }

    private void addWaypoint(String[] args) {
        if (args.length < 5) {
            usage("add <name> <x> <y> <z>");
            return;
        }

        try {
            String name = args[1];
            int x = Integer.parseInt(args[2]);
            int y = Integer.parseInt(args[3]);
            int z = Integer.parseInt(args[4]);

            Waypoint wp = new Waypoint(name, x, y, z);
            WaypointStorage.getInstance().add(wp);
            ChatUtil.sendMSG("§aWaypoint '" + name + "' added at X:" + x + " Y:" + y + " Z:" + z);
        } catch (NumberFormatException e) {
            ChatUtil.sendMSG("§cInvalid coordinates. Use integer numbers.");
        }
    }

    private void removeWaypoint(String[] args) {
        if (args.length < 2) {
            usage("remove <name>");
            return;
        }

        String name = args[1];
        if (WaypointStorage.getInstance().remove(name)) {
            ChatUtil.sendMSG("§aWaypoint '" + name + "' removed!");
        } else {
            ChatUtil.sendMSG("§cWaypoint '" + name + "' not found.");
        }
    }

    private void clearWaypoints() {
        if (WaypointStorage.getInstance().isEmpty()) {
            ChatUtil.sendMSG("§cNo waypoints to clear.");
        } else {
            WaypointStorage.getInstance().clear();
            ChatUtil.sendMSG("§aAll waypoints cleared!");
        }
    }

    private void listWaypoints() {
        Map<String, Waypoint> all = WaypointStorage.getInstance().getAll();
        if (all.isEmpty()) {
            ChatUtil.sendMSG("§cNo waypoints set.");
            return;
        }
        ChatUtil.sendMSG("§7Waypoints (" + all.size() + "):");
        for (Waypoint wp : all.values()) {
            ChatUtil.sendMSG("§f" + wp.getName() + " §7- X:" + wp.getX() + " Y:" + wp.getY() + " Z:" + wp.getZ());
        }
    }
}

