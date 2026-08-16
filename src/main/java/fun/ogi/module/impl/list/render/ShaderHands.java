package fun.ogi.module.impl.list.render;

import com.google.common.eventbus.Subscribe;
import fun.ogi.events.render.EventHud;
import fun.ogi.module.Module;
import fun.ogi.module.ModuleCategory;
import fun.ogi.module.ModuleInformation;
import fun.ogi.module.settings.BooleanSetting;
import fun.ogi.module.settings.ModeSetting;
import fun.ogi.module.settings.NumberSetting;
import fun.ogi.module.theme.ThemeManager;
import fun.ogi.util.render.hand.ShaderHandsRenderer;
import net.minecraft.client.MinecraftClient;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;

import java.awt.Color;

@ModuleInformation(moduleName = "ShaderHands", moduleDesc = "Hands shader: glow/outline, fire smoke trail and pretty fire", moduleCategory = ModuleCategory.RENDER)
public class ShaderHands extends Module {
    public static ShaderHands INSTANCE;

    public final ModeSetting mode = new ModeSetting("Mode", this, "Glow", "Glow", "Smoke", "Pretty");

    
    public final NumberSetting outline = new NumberSetting("Outline", this, 1.2, 0.1, 5.0, 0.1);
    public final NumberSetting glow = new NumberSetting("Glow", this, 1.0, 0.0, 5.0, 0.1);
    public final NumberSetting fill = new NumberSetting("Fill", this, 0.6, 0.0, 1.0, 0.01);
    public final NumberSetting alpha = new NumberSetting("Alpha", this, 1.0, 0.0, 1.0, 0.05);

    
    public final NumberSetting intensity = new NumberSetting("Intensity", this, 0.85, 0.1, 1.5, 0.01)
            .visible(() -> mode.is("Smoke") || mode.is("Pretty"));
    public final NumberSetting speed = new NumberSetting("Speed", this, 1.15, 0.2, 3.0, 0.05)
            .visible(() -> mode.is("Smoke") || mode.is("Pretty"));
    public final NumberSetting length = new NumberSetting("Length", this, 0.72, 0.1, 1.0, 0.05)
            .visible(() -> mode.is("Smoke"));

    
    public final NumberSetting prettyGlow = new NumberSetting("Pretty Glow", this, 2.0, 0.5, 5.0, 0.1)
            .visible(() -> mode.is("Pretty"));
    public final NumberSetting prettyHeight = new NumberSetting("Pretty Height", this, 0.06, 0.02, 0.6, 0.01)
            .visible(() -> mode.is("Pretty"));
    public final NumberSetting prettyWind = new NumberSetting("Pretty Wind", this, 1.0, 0.0, 3.0, 0.1)
            .visible(() -> mode.is("Pretty"));
    public final NumberSetting prettyWave = new NumberSetting("Pretty Wave", this, 1.0, 0.0, 3.0, 0.1)
            .visible(() -> mode.is("Pretty"));

    
    public final NumberSetting trailFade = new NumberSetting("Trail Fade", this, 0.91, 0.55, 0.98, 0.01)
            .visible(() -> mode.is("Smoke"));
    public final NumberSetting trailSoftness = new NumberSetting("Trail Softness", this, 1.35, 0.45, 2.5, 0.05)
            .visible(() -> mode.is("Smoke"));
    public final NumberSetting trailBlur = new NumberSetting("Trail Blur", this, 1.55, 0.2, 3.0, 0.05)
            .visible(() -> mode.is("Smoke"));
    public final NumberSetting handFade = new NumberSetting("Hand Fade", this, 0.68, 0.45, 0.9, 0.01)
            .visible(() -> mode.is("Smoke"));
    public final NumberSetting handSoftness = new NumberSetting("Hand Softness", this, 1.3, 0.45, 2.5, 0.05)
            .visible(() -> mode.is("Smoke"));
    public final NumberSetting handBlur = new NumberSetting("Hand Blur", this, 1.45, 0.2, 3.0, 0.05)
            .visible(() -> mode.is("Smoke"));
    public final NumberSetting smoke = new NumberSetting("Smoke", this, 0.55, 0.0, 0.8, 0.05)
            .visible(() -> mode.is("Smoke"));

    public final BooleanSetting useItemColor = new BooleanSetting("Item Color", this, true);
    public final BooleanSetting useThemeColor = new BooleanSetting("Theme Color", this, false);

    public ShaderHands() {
        INSTANCE = this;
        addSettings(mode, outline, glow, fill, alpha,
                intensity, speed, length,
                prettyGlow, prettyHeight, prettyWind, prettyWave,
                trailFade, trailSoftness, trailBlur, handFade, handSoftness, handBlur, smoke,
                useItemColor, useThemeColor);
    }

    @Subscribe
    public void onRenderHud(EventHud event) {
        if (!isEnabled()) return;
        ShaderHandsRenderer.getInstance().renderOverlayIfPending(event.getRenderTickCounter().getTickDelta(false));
    }

     
    public float[] glowColor() {
        if (useThemeColor.getValue()) {
            int primary = ThemeManager.getInstance().getPrimary();
            Color c = new Color(primary);
            return new float[]{c.getRed() / 255f, c.getGreen() / 255f, c.getBlue() / 255f};
        }
        Color color = useItemColor.getValue() ? heldItemColor() : new Color(0x6633FF);
        return new float[]{color.getRed() / 255f, color.getGreen() / 255f, color.getBlue() / 255f};
    }

    private Color heldItemColor() {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null) {
            return Color.WHITE;
        }

        ItemStack stack = mc.player.getMainHandStack();
        if (stack.isEmpty()) {
            stack = mc.player.getOffHandStack();
        }
        if (stack.isEmpty()) {
            return Color.WHITE;
        }

        String path = Registries.ITEM.getId(stack.getItem()).getPath();
        if (path.contains("diamond")) {
            return new Color(0x55DDE0);
        }
        if (path.contains("netherite")) {
            return new Color(0x5B4A67);
        }
        if (path.contains("gold")) {
            return new Color(0xFFD45A);
        }
        if (path.contains("iron")) {
            return new Color(0xD8DEE8);
        }
        if (path.contains("emerald")) {
            return new Color(0x35D06F);
        }
        if (path.contains("redstone")) {
            return new Color(0xE23B3B);
        }
        if (path.contains("lapis")) {
            return new Color(0x3156D4);
        }
        return new Color(0xE6E6E6);
    }
}

