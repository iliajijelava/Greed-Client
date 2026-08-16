package fun.ogi.command.impl;

import fun.ogi.Cheap;
import fun.ogi.command.Command;
import fun.ogi.module.Module;
import fun.ogi.util.chatutil.ChatUtil;
import net.minecraft.client.util.InputUtil;
import org.lwjgl.glfw.GLFW;

import java.util.List;

public class BindCommand extends Command {
    public BindCommand() {
        super("bind", "Manage module binds (add/rem/list/clear)", "b");
    }

    @Override
    public void execute(String[] args) {
        if (args.length == 0) {
            usage("<add/rem/list/clear> [module] [key]");
            return;
        }

        String sub = args[0].toLowerCase();

        switch (sub) {
            case "add" -> {
                if (args.length < 3) {
                    usage("add <module> <key>");
                    return;
                }
                fun.ogi.module.Module m = Cheap.getInstance().getModuleStorage().getModuleByName(args[1]);
                if (m == null) {
                    ChatUtil.sendMSG("§cModule not found: " + args[1]);
                    return;
                }
                int key = parseKey(args[2]);
                if (key == -1) {
                    ChatUtil.sendMSG("§cInvalid key: " + args[2]);
                    return;
                }
                m.setKeybind(key);
                ChatUtil.sendMSG("§b" + m.getName() + " §fbound to §b" + keyToName(key) + "§f.");
            }
            case "rem", "remove" -> {
                if (args.length < 2) {
                    usage("rem <module>");
                    return;
                }
                fun.ogi.module.Module m = Cheap.getInstance().getModuleStorage().getModuleByName(args[1]);
                if (m == null) {
                    ChatUtil.sendMSG("§cModule not found: " + args[1]);
                    return;
                }
                m.setKeybind(0);
                ChatUtil.sendMSG("§fBind removed for §b" + m.getName());
            }
            case "list" -> {
                List<fun.ogi.module.Module> bound = Cheap.getInstance().getModuleStorage().getModules().stream()
                        .filter(mod -> mod.getKeybind() != 0 && mod.getKeybind() != -1)
                        .toList();
                if (bound.isEmpty()) {
                    ChatUtil.sendMSG("§7No binds found.");
                    return;
                }
                ChatUtil.sendMSG("§bActive binds:");
                for (fun.ogi.module.Module mod : bound) {
                    ChatUtil.sendMSG(" §7- §b" + mod.getName() + " §f» §b" + keyToName(mod.getKeybind()));
                }
            }
            case "clear" -> {
                for (fun.ogi.module.Module mod : Cheap.getInstance().getModuleStorage().getModules()) {
                    mod.setKeybind(0);
                }
                ChatUtil.sendMSG("§fAll binds cleared.");
            }
            default -> {
                if (args.length >= 2) {
                    Module m = Cheap.getInstance().getModuleStorage().getModuleByName(args[0]);
                    if (m != null) {
                        int key = parseKey(args[1]);
                        if (key == -1) {
                            ChatUtil.sendMSG("§cInvalid key: " + args[1]);
                            return;
                        }
                        m.setKeybind(key);
                        ChatUtil.sendMSG("§b" + m.getName() + " §fbound to §b" + keyToName(key) + "§f.");
                        return;
                    }
                }
                ChatUtil.sendMSG("§cUnknown subcommand or module not found.");
            }
        }
    }

    public static int parseKey(String input) {
        String lower = input.toLowerCase();
        if (lower.equals("none") || lower.equals("0")) return 0;

        if (lower.startsWith("mouse")) {
            String numPart = lower.replace("mouse", "").trim();
            try {
                int btn = Integer.parseInt(numPart);
                if (btn >= 1 && btn <= 8) return 1000 + (btn - 1);
            } catch (NumberFormatException ignored) {}
            switch (lower) {
                case "left", "mouse1" -> { return 1000; }
                case "right", "mouse2" -> { return 1001; }
                case "middle", "mouse3" -> { return 1002; }
                case "mouse4" -> { return 1003; }
                case "mouse5" -> { return 1004; }
            }
        }

        if (lower.startsWith("m")) {
            String numPart = lower.substring(1).trim();
            try {
                int btn = Integer.parseInt(numPart);
                if (btn >= 1 && btn <= 8) return 1000 + (btn - 1);
            } catch (NumberFormatException ignored) {}
            switch (lower) {
                case "m1", "lmb" -> { return 1000; }
                case "m2", "rmb" -> { return 1001; }
                case "m3", "mmb" -> { return 1002; }
                case "m4" -> { return 1003; }
                case "m5" -> { return 1004; }
            }
        }

        try {
            return InputUtil.fromTranslationKey("key.keyboard." + lower).getCode();
        } catch (Exception ignored) {}

        switch (lower) {
            case "shift", "lshift" -> { return GLFW.GLFW_KEY_LEFT_SHIFT; }
            case "rshift" -> { return GLFW.GLFW_KEY_RIGHT_SHIFT; }
            case "ctrl", "control", "lctrl" -> { return GLFW.GLFW_KEY_LEFT_CONTROL; }
            case "rctrl" -> { return GLFW.GLFW_KEY_RIGHT_CONTROL; }
            case "alt", "lalt" -> { return GLFW.GLFW_KEY_LEFT_ALT; }
            case "ralt" -> { return GLFW.GLFW_KEY_RIGHT_ALT; }
            case "space" -> { return GLFW.GLFW_KEY_SPACE; }
            case "tab" -> { return GLFW.GLFW_KEY_TAB; }
            case "enter", "return" -> { return GLFW.GLFW_KEY_ENTER; }
            case "escape", "esc" -> { return GLFW.GLFW_KEY_ESCAPE; }
            case "delete", "del" -> { return GLFW.GLFW_KEY_DELETE; }
            case "up" -> { return GLFW.GLFW_KEY_UP; }
            case "down" -> { return GLFW.GLFW_KEY_DOWN; }
            case "left" -> { return GLFW.GLFW_KEY_LEFT; }
            case "right" -> { return GLFW.GLFW_KEY_RIGHT; }
            case "pageup" -> { return GLFW.GLFW_KEY_PAGE_UP; }
            case "pagedown" -> { return GLFW.GLFW_KEY_PAGE_DOWN; }
            case "home" -> { return GLFW.GLFW_KEY_HOME; }
            case "end" -> { return GLFW.GLFW_KEY_END; }
            case "insert", "ins" -> { return GLFW.GLFW_KEY_INSERT; }
            case "capslock", "caps" -> { return GLFW.GLFW_KEY_CAPS_LOCK; }
        }

        try {
            return GLFW.class.getField("GLFW_KEY_" + lower.toUpperCase()).getInt(null);
        } catch (Exception ignored) {}

        return -1;
    }

    public static String keyToName(int key) {
        if (key == 0 || key == -1) return "NONE";
        if (key >= 1000) {
            int btn = key - 1000;
            return switch (btn) {
                case 0 -> "LEFT";
                case 1 -> "RIGHT";
                case 2 -> "MIDDLE";
                default -> "MOUSE" + (btn + 1);
            };
        }
        try {
            String translationKey = InputUtil.fromKeyCode(key, 0).getTranslationKey();
            if (translationKey.startsWith("key.keyboard.")) {
                return translationKey.substring("key.keyboard.".length()).toUpperCase();
            }
            return translationKey.toUpperCase();
        } catch (Exception ignored) {}
        return "KEY_" + key;
    }
}

