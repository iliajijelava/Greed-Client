package fun.ogi.command;

import fun.ogi.util.chatutil.ChatUtil;
import net.minecraft.client.MinecraftClient;

public abstract class Command {
    private final String name;
    private final String description;
    private final String[] aliases;
    protected final MinecraftClient mc = MinecraftClient.getInstance();

    public Command(String name, String description, String... aliases) {
        this.name = name;
        this.description = description;
        this.aliases = aliases;
    }

    public abstract void execute(String[] args);

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public String[] getAliases() {
        return aliases;
    }

    protected void usage(String usage) {
        ChatUtil.sendMSG("§cUsage: ." + name + " " + usage);
    }
}

