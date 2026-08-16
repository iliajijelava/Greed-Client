package fun.ogi.screens;

import com.google.common.base.Supplier;
import com.google.common.base.Suppliers;
import fun.ogi.util.animation.Animation;
import fun.ogi.util.animation.Easing;
import fun.ogi.util.render.ShadowUtil;
import fun.ogi.util.render.builders.Builder;
import fun.ogi.util.render.builders.states.QuadColorState;
import fun.ogi.util.render.builders.states.QuadRadiusState;
import fun.ogi.util.render.builders.states.SizeState;
import fun.ogi.util.render.helper.HoverUtil;
import fun.ogi.util.render.msdf.MsdfFont;
import fun.ogi.util.render.providers.ColorProvider;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.multiplayer.MultiplayerScreen;
import net.minecraft.client.gui.screen.option.OptionsScreen;
import net.minecraft.client.gui.screen.world.SelectWorldScreen;
import net.minecraft.client.sound.PositionedSoundInstance;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import org.joml.Matrix4f;

import java.awt.Color;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;

public class MainMenuScreen extends Screen {
    private static final Supplier<MsdfFont> BIKO_FONT = Suppliers.memoize(() -> MsdfFont.builder().atlas("biko").data("biko").build());
    private static final Supplier<MsdfFont> MAINMENU_FONT = Suppliers.memoize(() -> MsdfFont.builder().atlas("mainmenu/font").data("mainmenu/font").build());
    private static final Supplier<MsdfFont> MENU_FONT = Suppliers.memoize(() -> MsdfFont.builder().atlas("menu/font").data("menu/font").build());
    private static final Supplier<MsdfFont> ICON_FONT = Suppliers.memoize(() -> MsdfFont.builder().atlas("nur").data("nur").build());
    private final List<MenuButton> buttons = new ArrayList<>();
    private final Animation[] linkAnimations = {new Animation(300, Easing.CUBIC_OUT), new Animation(300, Easing.CUBIC_OUT)};
    private final Animation appearAnimation = new Animation(450, Easing.CUBIC_OUT);
    private final Animation themeButtonAnimation = new Animation(200, Easing.CUBIC_OUT);
    private final boolean[] prevLinkHovered = {false, false};
    private boolean prevThemeButtonHovered = false;

    private static final float BASE_HEIGHT = 300f;

    private float responsiveScale = 1.0f;
    private float buttonWidth = 130f;
    private float buttonHeight = 18f;
    private float margin = 10f;

    
    private float themeButtonX, themeButtonY, themeButtonSize;

    public MainMenuScreen() {
        super(Text.empty());
    }

    @Override
    public boolean shouldPause() {
        return false;
    }

    @Override
    public boolean shouldCloseOnEsc() {
        return false;
    }

    @Override
    protected void init() {
        super.init();
        buttons.clear();

        responsiveScale = Math.min(1.5f, Math.max(0.5f, height / BASE_HEIGHT));

        buttonWidth = 150f * responsiveScale;
        buttonHeight = 18f * responsiveScale;
        margin = 5f * responsiveScale;

        float centerX = (width - buttonWidth) / 2f;

        int buttonCount = 5;
        float totalHeight = buttonHeight * buttonCount + margin * (buttonCount - 1);
        float baseY = (height - totalHeight) / 2f;

        buttons.add(new MenuButton(centerX, baseY, buttonWidth, buttonHeight, "D", "Singleplayer",
                () -> client.setScreen(new SelectWorldScreen(this)), false));
        buttons.add(new MenuButton(centerX, baseY + (buttonHeight + margin), buttonWidth, buttonHeight, "C", "Multiplayer",
                () -> client.setScreen(new MultiplayerScreen(this)), false));
        buttons.add(new MenuButton(centerX, baseY + (buttonHeight + margin) * 2f, buttonWidth, buttonHeight, "A", "Settings",
                () -> client.setScreen(new OptionsScreen(this, client.options)), false));
        buttons.add(new MenuButton(centerX, baseY + (buttonHeight + margin) * 3f, buttonWidth, buttonHeight, "B", "Alts",
                () -> client.setScreen(new AltManagerScreen(this)), false));
        buttons.add(new MenuButton(centerX, baseY + (buttonHeight + margin) * 4f, buttonWidth, buttonHeight, "X", "Exit",
                () -> client.scheduleStop(), true));

        themeButtonSize = 20f * responsiveScale;
        themeButtonX = 14f * responsiveScale;
        themeButtonY = 16f * responsiveScale;

        appearAnimation.start(0, 1);
    }

    @Override
    public void renderBackground(DrawContext context, int mouseX, int mouseY, float delta) {
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        appearAnimation.update();
        float appear = appearAnimation.getValue();

        MatrixStack matrices = context.getMatrices();


        BackgroundManager.render(context, width, height, mouseX, mouseY, delta, appear);
        context.draw();

        matrices.push();

        matrices.push();
        matrices.translate(width / 2f, height / 2f, 0);
        float scale = 0.9f + 0.1f * appear;
        matrices.scale(scale, scale, 1f);
        matrices.translate(-width / 2f, -height / 2f, 0);

        renderFooter(matrices);

        for (MenuButton button : buttons) {
            button.render(matrices, mouseX, mouseY);
        }

        renderThemeButton(matrices, mouseX, mouseY);
        renderLinkButtons(matrices, mouseX, mouseY);

        matrices.pop();

        super.render(context, mouseX, mouseY, delta);

        if (appear < 1f) {
            int fadeAlpha = (int) (180 * (1f - appear));
            Builder.rectangle()
                    .size(new SizeState(width, height))
                    .radius(new QuadRadiusState(0))
                    .color(new QuadColorState(ColorProvider.pack(0, 0, 0, fadeAlpha)))
                    .build()
                    .render(matrices.peek().getPositionMatrix(), 0, 0, 0);
        }
    }

    private void renderThemeButton(MatrixStack matrices, int mouseX, int mouseY) {
        MsdfFont font = MAINMENU_FONT.get();
        if (font == null) return;

        Matrix4f matrix = matrices.peek().getPositionMatrix();
        boolean hovered = HoverUtil.isHovered(mouseX, mouseY, themeButtonX, themeButtonY, themeButtonSize, themeButtonSize);
        if (hovered != prevThemeButtonHovered) {
            prevThemeButtonHovered = hovered;
            themeButtonAnimation.start(themeButtonAnimation.getValue(), hovered ? 1f : 0f);
        }
        themeButtonAnimation.update();
        float t = themeButtonAnimation.getValue();

        int bgAlpha = (int) (150 + 60 * t);
        Builder.rectangle()
                .size(new SizeState(themeButtonSize, themeButtonSize))
                .radius(new QuadRadiusState(6f))
                .color(new QuadColorState(ColorProvider.pack(17, 20, 38, bgAlpha)))
                .build()
                .render(matrix, themeButtonX, themeButtonY, 0);

        float glyphSize = 13f * responsiveScale;
        float glyphWidth = font.getWidth("B", glyphSize);
        float glyphX = themeButtonX + (themeButtonSize - glyphWidth) / 2f;
        float glyphY = themeButtonY + (themeButtonSize - glyphSize) / 2f;

        Builder.text()
                .text("B")
                .font(ICON_FONT.get())
                .size(glyphSize)
                .color(Color.WHITE.getRGB())
                .thickness(0.1f)
                .build()
                .render(matrix, glyphX, glyphY, 0);
    }

    private void renderFooter(MatrixStack matrices) {
        MsdfFont footerFont = BIKO_FONT.get();
        if (footerFont == null) return;

        String leftText = "GREED CLIENT | VER 1.0.0";
        String rightText = "#SELFKODER #NOVIBEECODING";
        float textSize = 5f * responsiveScale;

        float leftWidth = footerFont.getWidth(leftText, textSize);
        float rightWidth = footerFont.getWidth(rightText, textSize);

        float leftX = 10f * responsiveScale;
        float rightX = width - rightWidth - (10f * responsiveScale);
        float y = height - textSize - (6f * responsiveScale);

        Builder.text()
                .text(leftText)
                .font(footerFont)
                .size(textSize)
                .color(Color.WHITE.getRGB())
                .thickness(0.06f)
                .build()
                .render(matrices.peek().getPositionMatrix(), leftX, y, 0);

        Builder.text()
                .text(rightText)
                .font(footerFont)
                .size(textSize)
                .color(Color.WHITE.getRGB())
                .thickness(0.06f)
                .build()
                .render(matrices.peek().getPositionMatrix(), rightX, y, 0);
    }

    private void renderLinkButtons(MatrixStack matrices, int mouseX, int mouseY) {
        MsdfFont iconFont = MENU_FONT.get();
        if (iconFont == null) return;

        float iconSize = 20f * responsiveScale;
        float iconWidth = iconFont.getWidth("D", iconSize);
        float baseX = width - iconWidth - (20f * responsiveScale);
        float yPos = 20f * responsiveScale;
        Matrix4f matrix = matrices.peek().getPositionMatrix();

        for (int i = 0; i < 2; i++) {
            String icon = i == 0 ? "D" : "E";
            float xPos = baseX - i * (7.5f * responsiveScale + iconWidth);

            boolean hovered = HoverUtil.isHovered(mouseX, mouseY, xPos, yPos, iconWidth, iconSize + 1f);
            if (hovered != prevLinkHovered[i]) {
                prevLinkHovered[i] = hovered;
                linkAnimations[i].start(linkAnimations[i].getValue(), hovered ? 1f : 0f);
            }
            linkAnimations[i].update();

            float progress = linkAnimations[i].getValue();
            int blurColor = ColorProvider.pack(255, 255, 255, (int) (85 * (1f - progress * 0.3f)));

            Builder.blur()
                    .size(new SizeState(iconWidth + 8f, iconSize + 7f))
                    .radius(new QuadRadiusState(5))
                    .color(new QuadColorState(blurColor))
                    .blurRadius(7f)
                    .smoothness(0.5f)
                    .build()
                    .render(matrix, xPos - 4f, yPos - 3f, 0);

            Builder.text()
                    .text(icon)
                    .font(iconFont)
                    .size(iconSize)
                    .color(Color.WHITE.getRGB())
                    .thickness(0.1f)
                    .build()
                    .render(matrix, xPos, yPos, 0);
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        for (MenuButton menuButton : buttons) {
            if (menuButton.mouseClicked(mouseX, mouseY, button)) return true;
        }

        if (button == 0) {
            if (HoverUtil.isHovered(mouseX, mouseY, themeButtonX, themeButtonY, themeButtonSize, themeButtonSize)) {
                playClickSound();
                client.setScreen(new ThemeSelectScreen(this));
                return true;
            }

            MsdfFont iconFont = MENU_FONT.get();
            if (iconFont != null) {
                float iconSize = 20f * responsiveScale;
                float iconWidth = iconFont.getWidth("D", iconSize);
                float baseX = width - iconWidth - (20f * responsiveScale);
                float yPos = 20f * responsiveScale;

                float telX = baseX;
                float discX = baseX - (7.5f * responsiveScale) - iconWidth;

                if (HoverUtil.isHovered(mouseX, mouseY, telX, yPos, iconWidth, iconSize + 1f)) {
                    playClickSound();
                    openLink("https://t.me/greedvlog");
                    return true;
                }
                if (HoverUtil.isHovered(mouseX, mouseY, discX, yPos, iconWidth, iconSize + 1f)) {
                    playClickSound();
                    openLink("https://discord.gg/bU463pWpR");
                    return true;
                }
            }
        }

        return super.mouseClicked(mouseX, mouseY, button);
    }

    private void openLink(String link) {
        try {
            net.minecraft.util.Util.getOperatingSystem().open(URI.create(link));
        } catch (IllegalArgumentException ignored) {
        }
    }

    private void playClickSound() {
        client.getSoundManager().play(PositionedSoundInstance.master(SoundEvents.UI_BUTTON_CLICK, 1f));
    }

    private class MenuButton {
        private final float x, y, width, height;
        private final String icon;
        private final String text;
        private final Runnable action;
        private final boolean danger;
        private final Animation colorAnimation = new Animation(300, Easing.CUBIC_OUT);
        private boolean prevHovered;

        MenuButton(float x, float y, float width, float height, String icon, String text, Runnable action, boolean danger) {
            this.x = x;
            this.y = y;
            this.width = width;
            this.height = height;
            this.icon = icon;
            this.text = text;
            this.action = action;
            this.danger = danger;
        }

        void render(MatrixStack matrices, int mouseX, int mouseY) {
            Matrix4f matrix = matrices.peek().getPositionMatrix();
            boolean hovered = HoverUtil.isHovered(mouseX, mouseY, x, y, width, height);

            if (hovered != prevHovered) {
                prevHovered = hovered;
                colorAnimation.start(colorAnimation.getValue(), hovered ? 1f : 0f);
            }
            colorAnimation.update();
            float t = colorAnimation.getValue();

            int baseColor = danger
                    ? ColorProvider.pack(60, 12, 18, 235)
                    : ColorProvider.pack(17, 20, 38, 235);
            int hoverColor = danger
                    ? ColorProvider.pack(80, 16, 24, 245)
                    : ColorProvider.pack(24, 28, 52, 245);

            int r = (int) lerp((baseColor >> 16) & 0xFF, (hoverColor >> 16) & 0xFF, t);
            int g = (int) lerp((baseColor >> 8) & 0xFF, (hoverColor >> 8) & 0xFF, t);
            int b = (int) lerp(baseColor & 0xFF, hoverColor & 0xFF, t);
            int a = (int) lerp((baseColor >> 24) & 0xFF, (hoverColor >> 24) & 0xFF, t);
            ShadowUtil.dark(width,height,new QuadRadiusState(2f));
            Builder.rectangle()
                    .size(new SizeState(width, height))
                    .radius(new QuadRadiusState(2f))
                    .color(new QuadColorState(ColorProvider.pack(r, g, b, a)))
                    .build()
                    .render(matrix, x, y, 0);

            float centerY = y + height / 2f;
            float gap = 8f * responsiveScale;

            MsdfFont iconFont = MAINMENU_FONT.get();
            MsdfFont labelFont = BIKO_FONT.get();
            if (iconFont == null || labelFont == null) return;

            float iconSize = 9f * responsiveScale;
            float textSize = 7f * responsiveScale;

            float iconWidth = icon.isEmpty() ? 0 : iconFont.getWidth(icon, iconSize);
            float textWidth = text.isEmpty() ? 0 : labelFont.getWidth(text, textSize);

            float groupWidth = iconWidth + (icon.isEmpty() || text.isEmpty() ? 0 : gap) + textWidth;
            float groupX = x + (width - groupWidth) / 2f;

            if (!icon.isEmpty()) {
                Builder.text()
                        .text(icon)
                        .font(iconFont)
                        .size(iconSize)
                        .color(Color.WHITE.getRGB())
                        .thickness(0.1f)
                        .build()
                        .render(matrix, groupX + 2, centerY - iconSize / 2f, 0);
            }

            if (!text.isEmpty()) {
                float textX = groupX + iconWidth + (icon.isEmpty() ? 0 : gap) - 3f;
                Builder.text()
                        .text(text)
                        .font(labelFont)
                        .size(textSize)
                        .color(Color.WHITE.getRGB())
                        .thickness(0.07f)
                        .build()
                        .render(matrix, textX, centerY - textSize / 2f, 0);
            }
        }

        private float lerp(float a, float b, float t) {
            return a + (b - a) * t;
        }

        boolean mouseClicked(double mouseX, double mouseY, int button) {
            if (button == 0 && HoverUtil.isHovered(mouseX, mouseY, x, y, width, height)) {
                if (MinecraftClient.getInstance() != null) {
                    MinecraftClient.getInstance().getSoundManager().play(
                            PositionedSoundInstance.master(SoundEvents.UI_BUTTON_CLICK, 1f));
                }
                action.run();
                return true;
            }
            return false;
        }
    }
}