package fun.ogi.mixin;

import fun.ogi.Cheap;
import fun.ogi.events.render.EventFrame;
import fun.ogi.events.render.EventUpdate;
import fun.ogi.module.Module;
import fun.ogi.module.theme.ThemeManager;
import fun.ogi.screens.FiguraModelsGuiComponent;
import fun.ogi.util.macro.Macro;
import fun.ogi.util.macro.MacroManager;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.util.InputUtil;
import org.lwjgl.glfw.GLFW;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.HashMap;
import java.util.Map;

@Mixin(MinecraftClient.class)
public class MinecraftClientMixin {

    private static final Map<Integer, Boolean> previousKeyStates = new HashMap<>();
    private long lastFrameTime = System.nanoTime();
    private boolean hadWorld = false;

    @Inject(at = @At("HEAD"), method = "tick")
    private void onTick(CallbackInfo ci) {
        ThemeManager.getInstance().tick();

        MinecraftClient client = MinecraftClient.getInstance();

        boolean inWorld = client.world != null;
        if (inWorld && !hadWorld) {
            FiguraModelsGuiComponent.reapplyAppliedModel();
        }
        hadWorld = inWorld;

        if (client.player == null) return;

        
        long now = System.nanoTime();
        float deltaTime = (now - lastFrameTime) / 1_000_000_000.0f;
        lastFrameTime = now;
        if (deltaTime <= 0 || deltaTime > 0.1f) deltaTime = 0.05f;

        
        Cheap.getInstance().getEventBus().post(new EventFrame(deltaTime));

        long window = client.getWindow().getHandle();

        Map<Integer, Boolean> currentKeyStates = new HashMap<>();

        for (Module module : Cheap.getInstance().getModuleStorage().getModules()) {
            int key = module.getKeybind();
            if (key == 0 || key == -1) continue;
            if (currentKeyStates.containsKey(key)) continue;

            boolean isDown;
            if (key >= 0 && key <= GLFW.GLFW_MOUSE_BUTTON_LAST) {
                isDown = GLFW.glfwGetMouseButton(window, key) == GLFW.GLFW_PRESS;
            } else if (key >= GLFW.GLFW_KEY_SPACE && key <= GLFW.GLFW_KEY_LAST) {
                isDown = GLFW.glfwGetKey(window, key) == GLFW.GLFW_PRESS;
            } else {
                isDown = InputUtil.isKeyPressed(window, key);
            }
            currentKeyStates.put(key, isDown);
        }

        for (Macro macro : MacroManager.getMacros()) {
            int key = macro.getKey();
            if (key == 0 || key == -1) continue;
            if (currentKeyStates.containsKey(key)) continue;

            boolean isDown;
            if (key >= 0 && key <= GLFW.GLFW_MOUSE_BUTTON_LAST) {
                isDown = GLFW.glfwGetMouseButton(window, key) == GLFW.GLFW_PRESS;
            } else if (key >= GLFW.GLFW_KEY_SPACE && key <= GLFW.GLFW_KEY_LAST) {
                isDown = GLFW.glfwGetKey(window, key) == GLFW.GLFW_PRESS;
            } else {
                isDown = InputUtil.isKeyPressed(window, key);
            }
            currentKeyStates.put(key, isDown);
        }

        for (Module module : Cheap.getInstance().getModuleStorage().getModules()) {
            int key = module.getKeybind();
            if (key == 0 || key == -1) continue;

            boolean isDown = currentKeyStates.getOrDefault(key, false);
            boolean wasDown = previousKeyStates.getOrDefault(key, false);

            if (isDown && !wasDown && client.currentScreen == null) {
                module.toggle();
            }
        }

        for (Macro macro : MacroManager.getMacros()) {
            int key = macro.getKey();
            if (key == 0 || key == -1) continue;

            boolean isDown = currentKeyStates.getOrDefault(key, false);
            boolean wasDown = previousKeyStates.getOrDefault(key, false);

            if (isDown && !wasDown && client.currentScreen == null
                    && client.player != null && client.getNetworkHandler() != null) {
                client.getNetworkHandler().sendChatCommand(macro.getCommand());
            }
        }

        previousKeyStates.clear();
        previousKeyStates.putAll(currentKeyStates);

        if (client.player != null) {
            Cheap.getInstance().getEventBus().post(new EventUpdate());
        }
    }
}

