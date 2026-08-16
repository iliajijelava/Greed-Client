package fun.ogi.command.impl;

import fun.ogi.command.Command;
import fun.ogi.util.chatutil.ChatUtil;
import fun.ogi.util.macro.Macro;
import fun.ogi.util.macro.MacroManager;

import java.util.Arrays;
import java.util.List;

public class MacroCommand extends Command {
    public MacroCommand() {
        super("macro", "Manage macros (bind minecraft commands to keys)", "macros", "m");
    }

    @Override
    public void execute(String[] args) {
        if (args.length == 0) {
            usage("<add/rem/list/clear> [command] [key]");
            return;
        }

        String sub = args[0].toLowerCase();

        switch (sub) {
            case "add" -> {
                if (args.length < 3) {
                    usage("add <command> <key>");
                    return;
                }
                String command = join(args, 1, args.length - 1);
                int key = BindCommand.parseKey(args[args.length - 1]);
                if (key == -1) {
                    ChatUtil.sendMSG("§cInvalid key: " + args[args.length - 1]);
                    return;
                }
                MacroManager.add(new Macro(stripSlash(command), key));
                ChatUtil.sendMSG("§b" + command + " §fbound to §b" + BindCommand.keyToName(key) + "§f.");
            }
            case "rem", "remove" -> {
                if (args.length < 2) {
                    usage("rem <command>");
                    return;
                }
                String command = stripSlash(join(args, 1, args.length));
                if (MacroManager.removeByCommand(command)) {
                    ChatUtil.sendMSG("§fMacro removed: §b" + command);
                } else {
                    ChatUtil.sendMSG("§cMacro not found: " + command);
                }
            }
            case "list" -> {
                List<Macro> macros = MacroManager.getMacros();
                if (macros.isEmpty()) {
                    ChatUtil.sendMSG("§7No macros found.");
                    return;
                }
                ChatUtil.sendMSG("§bActive macros (§f" + macros.size() + "§b):");
                for (Macro macro : macros) {
                    ChatUtil.sendMSG(" §7- §b/" + macro.getCommand() + " §f» §b" + BindCommand.keyToName(macro.getKey()));
                }
            }
            case "clear" -> {
                MacroManager.clear();
                ChatUtil.sendMSG("§fAll macros cleared.");
            }
            default -> ChatUtil.sendMSG("§cUnknown subcommand. Use: add, rem, list, clear");
        }
    }

    private static String stripSlash(String command) {
        if (command.startsWith("/")) return command.substring(1);
        return command;
    }

    private static String join(String[] args, int from, int to) {
        return String.join(" ", Arrays.copyOfRange(args, from, Math.min(to, args.length)));
    }
}

