package fun.ogi.command.impl;

import fun.ogi.Cheap;
import fun.ogi.command.Command;
import fun.ogi.util.chatutil.ChatUtil;

public class FriendCommand extends Command {
    public FriendCommand() {
        super("friend", "Manage your friends list", "f");
    }

    @Override
    public void execute(String[] args) {
        if (args.length == 0) {
            usage("<add/rem/list/clear> [name]");
            return;
        }

        var fm = Cheap.getInstance().getFriendManager();
        String sub = args[0].toLowerCase();

        switch (sub) {
            case "add" -> {
                if (args.length < 2) {
                    usage("add <name>");
                    return;
                }
                fm.add(args[1]);
                ChatUtil.sendMSG("Added §b" + args[1] + " §fto friends.");
            }
            case "rem", "remove" -> {
                if (args.length < 2) {
                    usage("rem <name>");
                    return;
                }
                if (!fm.contains(args[1])) {
                    ChatUtil.sendMSG("§c" + args[1] + " §fis not in your friends.");
                    return;
                }
                fm.remove(args[1]);
                ChatUtil.sendMSG("Removed §b" + args[1] + " §ffrom friends.");
            }
            case "list" -> {
                if (fm.getFriends().isEmpty()) {
                    ChatUtil.sendMSG("§7No friends.");
                    return;
                }
                ChatUtil.sendMSG("§bFriends (§f" + fm.getFriends().size() + "§b):");
                for (String name : fm.getFriends()) {
                    ChatUtil.sendMSG(" §7- §b" + name);
                }
            }
            case "clear" -> {
                fm.clear();
                ChatUtil.sendMSG("§fFriend list cleared.");
            }
            default -> ChatUtil.sendMSG("§cUnknown subcommand. Use: add, rem, list, clear");
        }
    }
}

