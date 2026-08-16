package fun.ogi.screens;

import com.google.common.base.Suppliers;
import fun.ogi.util.animation.Animation;
import fun.ogi.util.animation.Easing;
import fun.ogi.util.render.builders.Builder;
import fun.ogi.util.render.builders.states.QuadColorState;
import fun.ogi.util.render.builders.states.QuadRadiusState;
import fun.ogi.util.render.builders.states.SizeState;
import fun.ogi.util.render.helper.HoverUtil;
import fun.ogi.util.render.msdf.MsdfFont;
import fun.ogi.util.render.providers.ColorProvider;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.sound.PositionedSoundInstance;
import net.minecraft.client.texture.AbstractTexture;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import org.joml.Matrix4f;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;





public class ThemeSelectScreen extends Screen {

    private static final java.util.function.Supplier<MsdfFont> TITLE_FONT =
            Suppliers.memoize(() -> MsdfFont.builder().atlas("biko").data("biko").build());
    private static final java.util.function.Supplier<MsdfFont> LABEL_FONT =
            Suppliers.memoize(() -> MsdfFont.builder().atlas("mainmenu/font").data("mainmenu/font").build());

    private final Screen parent;
    private final List<ThemeCard> cards = new ArrayList<>();
    private final Animation appearAnimation = new Animation(220, Easing.CUBIC_OUT);

    private float panelX, panelY, panelWidth, panelHeight;
    private float closeX, closeY, closeSize;

    public ThemeSelectScreen(Screen parent) {
        super(Text.empty());
        this.parent = parent;
    }

    @Override
    public boolean shouldPause() {
        return false;
    }

    @Override
    protected void init() {
        super.init();
        cards.clear();

        panelWidth = 460f;
        panelHeight = 220f;
        panelX = (width - panelWidth) / 2f;
        panelY = (height - panelHeight) / 2f;

        closeSize = 16f;
        closeX = panelX + panelWidth - 24f;
        closeY = panelY + 18f;

        float cardSize = 110f;
        float cardGap = 14f;
        float cardsStartX = panelX + 24f;
        float cardsY = panelY + 56f;

        BackgroundManager.BackgroundType[] types = BackgroundManager.BackgroundType.values();
        for (int i = 0; i < types.length; i++) {
            float cx = cardsStartX + i * (cardSize + cardGap);
            cards.add(new ThemeCard(types[i], cx, cardsY, cardSize, cardSize));
        }

        appearAnimation.start(0, 1);
    }

    @Override
    public void renderBackground(DrawContext context, int mouseX, int mouseY, float delta) {
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        
        if (parent != null) {
            parent.render(context, -1, -1, delta);
        }

        appearAnimation.update();
        float t = appearAnimation.getValue();

        MatrixStack matrices = context.getMatrices();
        Matrix4f matrix = matrices.peek().getPositionMatrix();

        
        Builder.rectangle()
                .size(new SizeState(width, height))
                .radius(new QuadRadiusState(0))
                .color(new QuadColorState(ColorProvider.pack(0, 0, 0, (int) (140 * t))))
                .build()
                .render(matrix, 0, 0, 0);

        int panelAlpha = (int) (245 * t);

        
        Builder.rectangle()
                .size(new SizeState(panelWidth, panelHeight))
                .radius(new QuadRadiusState(10f))
                .color(new QuadColorState(ColorProvider.pack(18, 20, 30, panelAlpha)))
                .build()
                .render(matrix, panelX, panelY, 0);

        
        MsdfFont titleFont = TITLE_FONT.get();
        if (titleFont != null) {
            Builder.text()
                    .text("Choose Theme")
                    .font(titleFont)
                    .size(16f)
                    .color(Color.WHITE.getRGB())
                    .thickness(0.07f)
                    .build()
                    .render(matrix, panelX + 20f, panelY + 18f, 0);
        }

        
        boolean closeHovered = HoverUtil.isHovered(mouseX, mouseY, closeX, closeY, closeSize, closeSize);
        Builder.text()
                .text("x")
                .font(titleFont)
                .size(closeHovered ? 17f : 15f)
                .color(closeHovered ? Color.WHITE.getRGB() : new Color(180, 180, 190).getRGB())
                .thickness(0.08f)
                .build()
                .render(matrix, closeX, closeY, 0);

        for (ThemeCard card : cards) {
            card.render(matrices, mouseX, mouseY);
        }

        
        MsdfFont labelFont = LABEL_FONT.get();
        if (labelFont != null) {
            String activeText = "Active: " + BackgroundManager.getCurrent().displayName;
            Builder.text()
                    .text(activeText)
                    .font(labelFont)
                    .size(11f)
                    .color(new Color(190, 190, 200).getRGB())
                    .thickness(0.06f)
                    .build()
                    .render(matrix, panelX + 20f, panelY + panelHeight - 26f, 0);
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0) {
            if (HoverUtil.isHovered(mouseX, mouseY, closeX, closeY, closeSize, closeSize)) {
                playClick();
                close();
                return true;
            }
            for (ThemeCard card : cards) {
                if (card.mouseClicked(mouseX, mouseY)) {
                    playClick();
                    return true;
                }
            }
            
            if (!HoverUtil.isHovered(mouseX, mouseY, panelX, panelY, panelWidth, panelHeight)) {
                close();
                return true;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public void close() {
        if (client != null) {
            client.setScreen(parent);
        }
    }

    private void playClick() {
        if (client != null) {
            client.getSoundManager().play(PositionedSoundInstance.master(SoundEvents.UI_BUTTON_CLICK, 1f));
        }
    }

    private class ThemeCard {
        private final BackgroundManager.BackgroundType type;
        private final float x, y, width, height;
        private final Animation hoverAnimation = new Animation(200, Easing.CUBIC_OUT);
        private boolean prevHovered;

        ThemeCard(BackgroundManager.BackgroundType type, float x, float y, float width, float height) {
            this.type = type;
            this.x = x;
            this.y = y;
            this.width = width;
            this.height = height;
        }

        void render(MatrixStack matrices, int mouseX, int mouseY) {
            Matrix4f matrix = matrices.peek().getPositionMatrix();
            boolean hovered = HoverUtil.isHovered(mouseX, mouseY, x, y, width, height + 20f);
            if (hovered != prevHovered) {
                prevHovered = hovered;
                hoverAnimation.start(hoverAnimation.getValue(), hovered ? 1f : 0f);
            }
            hoverAnimation.update();
            float h = hoverAnimation.getValue();

            boolean active = BackgroundManager.getCurrent() == type;

            
            
            AbstractTexture thumb = client != null
                    ? client.getTextureManager().getTexture(previewTextureFor(type))
                    : null;

            if (thumb != null) {
                Builder.texture()
                        .size(new SizeState(width, height))
                        .color(new QuadColorState(new Color(255, 255, 255, 255)))
                        .texture(0, 0, 1, 1, thumb)
                        .build()
                        .render(matrix, x, y, 0);
            } else {
                Builder.rectangle()
                        .size(new SizeState(width, height))
                        .radius(new QuadRadiusState(6f))
                        .color(new QuadColorState(ColorProvider.pack(40, 42, 55, 255)))
                        .build()
                        .render(matrix, x, y, 0);
            }

            
            if (h > 0.01f || active) {
                int borderAlpha = active ? 255 : (int) (150 * h);
                Builder.rectangle()
                        .size(new SizeState(width, 2f))
                        .radius(new QuadRadiusState(0))
                        .color(new QuadColorState(ColorProvider.pack(90, 160, 255, borderAlpha)))
                        .build()
                        .render(matrix, x, y - 2f, 0);
            }

            
            MsdfFont labelFont = LABEL_FONT.get();
            if (labelFont != null) {
                Builder.text()
                        .text(type.displayName)
                        .font(labelFont)
                        .size(10f)
                        .color(Color.WHITE.getRGB())
                        .thickness(0.06f)
                        .build()
                        .render(matrix, x, y + height + 6f, 0);
            }

            
            float dotSize = 8f;
            int dotColor = active ? ColorProvider.pack(90, 220, 130, 255) : ColorProvider.pack(90, 92, 105, 200);
            Builder.rectangle()
                    .size(new SizeState(dotSize, dotSize))
                    .radius(new QuadRadiusState(dotSize / 2f))
                    .color(new QuadColorState(dotColor))
                    .build()
                    .render(matrix, x + width + 6f, y + height / 2f - dotSize / 2f, 0);
        }

        boolean mouseClicked(double mouseX, double mouseY) {
            if (HoverUtil.isHovered(mouseX, mouseY, x, y, width, height)) {
                BackgroundManager.setCurrent(type);
                return true;
            }
            return false;
        }

        private net.minecraft.util.Identifier previewTextureFor(BackgroundManager.BackgroundType type) {
            
            
            if (type == BackgroundManager.BackgroundType.VANILLA) {
                return net.minecraft.util.Identifier.ofVanilla("textures/gui/title/background/panorama_0.png");
            }
            if (type == BackgroundManager.BackgroundType.CUSTOM_PANORAMA) {
                return net.minecraft.util.Identifier.of("cheap", "mainmenu/panorama/panorama_0.png");
            }
            return type.textureId;
        }
    }
}