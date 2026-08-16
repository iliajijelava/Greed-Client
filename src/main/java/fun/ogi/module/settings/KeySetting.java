package fun.ogi.module.settings;


import fun.ogi.module.Module;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.util.InputUtil;
import org.lwjgl.glfw.GLFW;

import java.util.function.Supplier;

public class KeySetting extends Setting {
    private int key;
    private long holdStartTime;
    private boolean wasDown;
    private static final long HOLD_THRESHOLD = 50;

    public KeySetting(String name, Module parent, int defaultKey) {
        super(name, parent);
        this.key = defaultKey;
        this.holdStartTime = 0;
        this.wasDown = false;
    }

    public int getKey() {
        return key;
    }

    public void setKey(int key) {
        this.key = key;
    }

    public boolean isKeyDown() {
        if (key == -1) return false;
        long window = MinecraftClient.getInstance().getWindow().getHandle();
        if (key >= 0 && key <= GLFW.GLFW_MOUSE_BUTTON_LAST) {
            return GLFW.glfwGetMouseButton(window, key) == GLFW.GLFW_PRESS;
        }
        if (key >= GLFW.GLFW_KEY_SPACE && key <= GLFW.GLFW_KEY_LAST) {
            return GLFW.glfwGetKey(window, key) == GLFW.GLFW_PRESS;
        }
        return InputUtil.isKeyPressed(window, key);
    }

    public boolean isPressed() {

        return getParent().isEnabled() && isKeyDown();
    }

    public boolean justPressed() {
        if (key == -1) return false;
        boolean down = isKeyDown();
        if (down && !wasDown) {
            wasDown = true;
            holdStartTime = System.currentTimeMillis();
            return true;
        }
        if (!down) {
            wasDown = false;
            holdStartTime = 0;
        }
        return false;
    }
    public KeySetting visible(Supplier<Boolean> state) {
        this.visible = state;
        return this;
    }
    public boolean isHeld() {
        if (key == -1) return false;
        if (!isKeyDown()) {
            wasDown = false;
            holdStartTime = 0;
            return false;
        }
        if (!wasDown) {
            wasDown = true;
            holdStartTime = System.currentTimeMillis();
            return false;
        }
        return wasDown && System.currentTimeMillis() - holdStartTime >= HOLD_THRESHOLD;
    }

    public static String mouseButtonName(int key) {
        return switch (key) {
            case 0 -> "LMB";
            case 1 -> "RMB";
            case 2 -> "MMB";
            case 3 -> "MB4";
            case 4 -> "MB5";
            default -> "MOUSE_" + key;
        };
    }

    @Override
    public String getValueAsString() {
        if (key == -1) return "NONE";
        if (key >= 0 && key <= GLFW.GLFW_MOUSE_BUTTON_LAST) {
            return mouseButtonName(key);
        }
        try {
            String name = InputUtil.fromKeyCode(key, 0).getTranslationKey();
            if (name.contains("key.keyboard.")) {
                return name.replace("key.keyboard.", "").toUpperCase();
            }
            return name;
        } catch (Exception e) {
            return "KEY_" + key;

        }
    }

    @Override
    public void setValueFromString(String value) {
        if (value.equalsIgnoreCase("NONE") || value.isEmpty()) {
            this.key = -1;
            return;
        }
        switch (value.toUpperCase()) {
            case "LMB" -> { this.key = 0; return; }
            case "RMB" -> { this.key = 1; return; }
            case "MMB" -> { this.key = 2; return; }
        }
        if (value.toUpperCase().startsWith("MB")) {
            try {
                int btn = Integer.parseInt(value.substring(2));
                if (btn >= 4 && btn <= 8) {
                    this.key = btn - 1;
                    return;
                }
            } catch (NumberFormatException ignored) {
            }
        }
        if (value.toUpperCase().startsWith("MOUSE_")) {
            try {
                this.key = Integer.parseInt(value.substring(6));
                return;
            } catch (NumberFormatException ignored) {
            }
        }
        try {
            int code = InputUtil.fromTranslationKey("key.keyboard." + value.toLowerCase()).getCode();
            if (code > 0) {
                this.key = code;
                return;

            }
        } catch (Exception ignored) {
        }
        try {
            int code = Integer.parseInt(value.replace("KEY_", ""));
            if (code > 0) this.key = code;
        } catch (NumberFormatException ignored) {
        }

    }
}

