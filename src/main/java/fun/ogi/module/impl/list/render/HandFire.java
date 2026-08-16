package fun.ogi.module.impl.list.render;

import com.google.common.eventbus.Subscribe;
import fun.ogi.events.render.EventHudPre;
import fun.ogi.module.Module;
import fun.ogi.module.ModuleCategory;
import fun.ogi.module.ModuleInformation;
import fun.ogi.module.settings.BooleanSetting;
import fun.ogi.module.settings.ModeSetting;
import fun.ogi.module.settings.SliderSetting;
import fun.ogi.module.theme.ThemeManager;
import net.minecraft.client.MinecraftClient;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;

import java.awt.Color;

@ModuleInformation(moduleName = "HandFire", moduleDesc = "Fire and smoke trail on hands and items", moduleCategory = ModuleCategory.RENDER)
public class HandFire extends Module {
    public static HandFire INSTANCE;

    public final ModeSetting mode = new ModeSetting("Mode", this, "Smoke", "Smoke", "Pretty");

    
    public final SliderSetting intensity = new SliderSetting("Intensity", this, 0.85f, 0.1f, 1.5f, 0.01f);
    public final SliderSetting speed = new SliderSetting("Speed", this, 1.15f, 0.2f, 3.0f, 0.05f);
    public final SliderSetting length = new SliderSetting("Length", this, 0.72f, 0.1f, 1.0f, 0.05f).visible(() -> mode.is("Smoke"));

    public final SliderSetting prettyGlow = new SliderSetting("Glow", this, 2.0f, 0.5f, 5.0f, 0.1f).visible(() -> mode.is("Pretty"));
    public final SliderSetting prettyHeight = new SliderSetting("Height", this, 0.06f, 0.02f, 0.6f, 0.01f).visible(() -> mode.is("Pretty"));
    public final SliderSetting prettyWind = new SliderSetting("Wind", this, 1.0f, 0.0f, 3.0f, 0.1f).visible(() -> mode.is("Pretty"));
    public final SliderSetting prettyWave = new SliderSetting("Waves", this, 1.0f, 0.0f, 3.0f, 0.1f).visible(() -> mode.is("Pretty"));

    
    public final SliderSetting trailFade = new SliderSetting("Trail Fade", this, 0.91f, 0.55f, 0.98f, 0.01f).visible(() -> mode.is("Smoke"));
    public final SliderSetting trailSoftness = new SliderSetting("Trail Softness", this, 1.35f, 0.45f, 2.5f, 0.05f).visible(() -> mode.is("Smoke"));
    public final SliderSetting trailBlur = new SliderSetting("Trail Blur", this, 1.55f, 0.2f, 3.0f, 0.05f).visible(() -> mode.is("Smoke"));
    public final SliderSetting handFade = new SliderSetting("Hand Fade", this, 0.68f, 0.45f, 0.9f, 0.01f).visible(() -> mode.is("Smoke"));
    public final SliderSetting handSoftness = new SliderSetting("Hand Softness", this, 1.3f, 0.45f, 2.5f, 0.05f).visible(() -> mode.is("Smoke"));
    public final SliderSetting handBlur = new SliderSetting("Hand Blur", this, 1.45f, 0.2f, 3.0f, 0.05f).visible(() -> mode.is("Smoke"));
    public final SliderSetting smoke = new SliderSetting("Smoke", this, 0.55f, 0.0f, 0.8f, 0.05f).visible(() -> mode.is("Smoke"));

    public final BooleanSetting useItemColor = new BooleanSetting("Item Color", this, true);
    public final BooleanSetting useThemeColor = new BooleanSetting("Theme Color", this, false);

    public HandFire() {
        INSTANCE = this;
        addSettings(mode, intensity, speed, length, prettyGlow, prettyHeight, prettyWind, prettyWave,
                trailFade, trailSoftness, trailBlur, handFade, handSoftness, handBlur, smoke,
                useItemColor, useThemeColor);
    }

    @Override
    public void onEnable() {
        super.onEnable();
        HandFireRenderer.getInstance().setEnabled(true);
    }

    @Override
    public void onDisable() {
        HandFireRenderer.getInstance().setEnabled(false);
        super.onDisable();
    }

    
    @Subscribe
    public void onRenderHudPre(EventHudPre event) {
        HandFireRenderer.getInstance().renderFireEffect();
    }

     
    public float[] glowColor() {
        if (useThemeColor.getValue()) {
            int color1 = ThemeManager.getInstance().getPrimary();
            return new float[]{
                    ((color1 >> 16) & 0xFF) / 255f,
                    ((color1 >> 8) & 0xFF) / 255f,
                    (color1 & 0xFF) / 255f
            };
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

