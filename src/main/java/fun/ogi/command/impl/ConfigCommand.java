package fun.ogi.command.impl;

import fun.ogi.Cheap;
import fun.ogi.command.Command;
import fun.ogi.util.chatutil.ChatUtil;
import net.minecraft.util.Util;

public class ConfigCommand extends Command {
    public ConfigCommand() {
        super("config", "Manage configs (save/rem/list/clear)", "cfg");
    }

    @Override
    public void execute(String[] args) {
        if (args.length == 0) {
            usage("<save/load/rem/list/clear/dir> [name]");
            return;
        }

        var cm = Cheap.getInstance().getConfigManager();
        String sub = args[0].toLowerCase();

        switch (sub) {
            case "load" -> {
                if (args.length < 2) {
                    usage("load <name>");
                    return;
                }
                cm.loadConfig(args[1]);
                ChatUtil.sendMSG("Config §b" + args[1] + " §floaded.");
            }
            case "save" -> {
                if (args.length < 2) {
                    usage("save <name>");
                    return;
                }
                cm.saveConfig(args[1]);
                ChatUtil.sendMSG("Config §b" + args[1] + " §fsaved.");
            }
            case "rem", "remove", "delete" -> {
                if (args.length < 2) {
                    usage("rem <name>");
                    return;
                }
                cm.deleteConfig(args[1]);
                ChatUtil.sendMSG("Config §b" + args[1] + " §fdeleted.");
            }
            case "list" -> {
                String[] names = cm.listConfigs();
                if (names.length == 0) {
                    ChatUtil.sendMSG("§7No configs found.");
                    return;
                }
                ChatUtil.sendMSG("§bConfigs (§f" + names.length + "§b):");
                for (String n : names) {
                    ChatUtil.sendMSG(" §7- §b" + n);
                }
            }
            case "clear" -> {
                cm.clearConfigs();
                ChatUtil.sendMSG("§fAll configs except default cleared.");
            }
            case "dir", "folder", "open" -> {
                Util.getOperatingSystem().open(cm.getConfigDir());
                ChatUtil.sendMSG("§fOpened config directory.");
            }
            default -> ChatUtil.sendMSG("§cUnknown subcommand. Use: save, load, rem, list, clear, dir");
        }
    }
}

