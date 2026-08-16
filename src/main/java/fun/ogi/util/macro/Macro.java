package fun.ogi.util.macro;

public class Macro {
    private final String command;
    private int key;

    public Macro(String command, int key) {
        this.command = command;
        this.key = key;
    }

    public String getCommand() {
        return command;
    }

    public int getKey() {
        return key;
    }

    public void setKey(int key) {
        this.key = key;
    }
}

