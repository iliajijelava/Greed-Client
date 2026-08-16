package fun.ogi.command;

import fun.ogi.command.impl.*;
import fun.ogi.util.chatutil.ChatUtil;

import java.util.ArrayList;
import java.util.List;

public class CommandManager {
    private final List<Command> commands = new ArrayList<>();
    private final String prefix = ".";

    public CommandManager() {
        commands.add(new BindCommand());
        commands.add(new MacroCommand());
        commands.add(new ConfigCommand());
        commands.add(new FriendCommand());
        commands.add(new HelpCommand());
        commands.add(new StaffCommand());
        commands.add(new NeuroCommand());
        commands.add(new GPSCommand());
        commands.add(new WaypointCommand());
        commands.add(new AutoPilotCommand());
        commands.add(new VClipCommand());
    }

    public boolean handleCommand(String message) {
        if (!message.startsWith(prefix)) return false;

        String raw = message.substring(prefix.length());
        String[] split = raw.split(" ");
        String name = split[0];
        String[] args = new String[split.length - 1];
        System.arraycopy(split, 1, args, 0, split.length - 1);

        for (Command cmd : commands) {
            if (cmd.getName().equalsIgnoreCase(name)) {
                cmd.execute(args);
                return true;
            }
            for (String alias : cmd.getAliases()) {
                if (alias.equalsIgnoreCase(name)) {
                    cmd.execute(args);
                    return true;
                }
            }
        }

        ChatUtil.sendMSG("§cUnknown command. Type .help for a list of commands.");
        return true;
    }

    public List<Command> getCommands() {
        return commands;
    }
}

