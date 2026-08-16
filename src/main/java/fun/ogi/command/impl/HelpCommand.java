package fun.ogi.command.impl;

import fun.ogi.Cheap;
import fun.ogi.command.Command;
import fun.ogi.util.chatutil.ChatUtil;

public class HelpCommand extends Command {
    public HelpCommand() {
        super("help", "List all commands", "h", "?");
    }

    @Override
    public void execute(String[] args) {
        ChatUtil.sendMSG("§bAvailable commands:");
        for (Command cmd : Cheap.getInstance().getCommandManager().getCommands()) {
            ChatUtil.sendMSG("§f." + cmd.getName() + " §7- " + cmd.getDescription());
        }
    }
}


