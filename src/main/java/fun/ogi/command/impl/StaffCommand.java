package fun.ogi.command.impl;

import fun.ogi.Cheap;
import fun.ogi.command.Command;
import fun.ogi.util.chatutil.ChatUtil;

public class StaffCommand extends Command {
    public StaffCommand() {
        super("staff", "Manage your staff list", "staf");
    }

    @Override
    public void execute(String[] args) {
        if (args.length == 0) {
            usage("<add/rem/list/clear> [name]");
            return;
        }

        var sm = Cheap.getInstance().getStaffManager();
        String sub = args[0].toLowerCase();

        switch (sub) {
            case "add" -> {
                if (args.length < 2) {
                    usage("add <name>");
                    return;
                }
                sm.add(args[1]);
                ChatUtil.sendMSG("Added §c" + args[1] + " §fto staff list.");
            }
            case "rem", "remove" -> {
                if (args.length < 2) {
                    usage("rem <name>");
                    return;
                }
                if (!sm.contains(args[1])) {
                    ChatUtil.sendMSG("§c" + args[1] + " §fis not in your staff list.");
                    return;
                }
                sm.remove(args[1]);
                ChatUtil.sendMSG("Removed §c" + args[1] + " §ffrom staff list.");
            }
            case "list" -> {
                if (sm.getStaff().isEmpty()) {
                    ChatUtil.sendMSG("§7No staff players.");
                    return;
                }
                ChatUtil.sendMSG("§cStaff Players (§f" + sm.getStaff().size() + "§c):");
                for (String name : sm.getStaff()) {
                    ChatUtil.sendMSG(" §7- §c" + name);
                }
            }
            case "clear" -> {
                sm.clear();
                ChatUtil.sendMSG("§fStaff list cleared.");
            }
            default -> ChatUtil.sendMSG("§cUnknown subcommand. Use: add, rem, list, clear");
        }
    }
}

