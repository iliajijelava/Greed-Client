package fun.ogi.screens;

import com.google.common.base.Supplier;
import com.google.common.base.Suppliers;
import fun.ogi.util.altmanager.AltManager;
import fun.ogi.util.render.builders.Builder;
import fun.ogi.util.render.builders.states.QuadColorState;
import fun.ogi.util.render.builders.states.QuadRadiusState;
import fun.ogi.util.render.builders.states.SizeState;
import fun.ogi.util.render.helper.HoverUtil;
import fun.ogi.util.render.msdf.MsdfFont;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.texture.AbstractTexture;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import org.joml.Matrix4f;

import java.awt.*;

public class AddAccountScreen extends Screen {
    private static final Supplier<MsdfFont> BIKO_FONT = Suppliers.memoize(() -> MsdfFont.builder().atlas("biko").data("biko").build());
    private final Screen parent;
    private TextFieldWidget usernameField;
    private AddButton addButton;
    private AddButton randomButton;
    private static final Identifier BACKGROUND = Identifier.of("cheap", "textures/mainmenu/menu.png");
    public AddAccountScreen(Screen parent) {
        super(Text.literal("Add Account"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        float fieldWidth = 200;
        float fieldHeight = 20;
        float centerX = width / 2f - fieldWidth / 2f;
        float centerY = height / 2f - fieldHeight / 2f;

        usernameField = new TextFieldWidget(textRenderer, (int) centerX, (int) centerY, (int) fieldWidth, (int) fieldHeight, Text.literal("Username"));
        usernameField.setMaxLength(16);
        addSelectableChild(usernameField);
        setInitialFocus(usernameField);

        float buttonWidth = 100;
        float buttonHeight = 15;
        addButton = new AddButton("Add", width / 2f - buttonWidth / 2f, centerY + 30, buttonWidth, buttonHeight, () -> {
            String username = usernameField.getText().trim();
            if (!username.isEmpty()) {
                AltManager.addAccount(username);
                if (client != null) client.setScreen(parent);
            }
        });
        randomButton = new AddButton(
                "Random",
                width / 2f - buttonWidth / 2f,
                centerY + 50,
                buttonWidth,
                buttonHeight,
                () -> {
                    String randomName = generateRandomName();
                    AltManager.addAccount(randomName);

                    if (client != null) {
                        client.setScreen(parent);
                    }
                }
        );
    }

    @Override
    public void renderBackground(DrawContext context, int mouseX, int mouseY, float delta) {
        super.renderBackground(context, mouseX, mouseY, delta);
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        super.render(context, mouseX, mouseY, delta);

        Matrix4f matrix = context.getMatrices().peek().getPositionMatrix();

        AbstractTexture texture = client.getTextureManager().getTexture(BACKGROUND);
        Builder.texture()
                .size(new SizeState(width, height))
                .color(new QuadColorState(Color.WHITE))
                .texture(0, 0, 1, 1, texture)
                .build()
                .render(matrix, 0, 0, 0);

        
        String titleText = "ADD ACCOUNT";
        float titleSize = 20f;
        float titleWidth = BIKO_FONT.get().getWidth(titleText, titleSize);
        Builder.text()
                .text(titleText)
                .color(Color.WHITE)
                .size(titleSize)
                .font(BIKO_FONT.get())
                .thickness(0.1f)
                .build()
                .render(matrix, width / 2f - titleWidth / 2f, height / 4f);

        
        Builder.liquid()
                .size(new SizeState(usernameField.getWidth() + 4, usernameField.getHeight() + 4))
                .radius(new QuadRadiusState(4, 4, 4, 4))
                .color(new QuadColorState(new Color(255, 255, 255, 50)))
                .build()
                .render(matrix, usernameField.getX() - 2, usernameField.getY() - 2);

        usernameField.render(context, mouseX, mouseY, delta);
        addButton.render(matrix, mouseX, mouseY);
        randomButton.render(matrix, mouseX, mouseY);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (addButton.isHovered(mouseX, mouseY) && button == 0) {
            addButton.action.run();
            return true;
        }
        if (randomButton.isHovered(mouseX, mouseY) && button == 0) {
            randomButton.action.run();
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);

    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == 257) { 
            addButton.action.run();
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public void close() {
        if (client != null) client.setScreen(parent);
    }

    private static class AddButton {
        private final String text;
        private final float x, y, width, height;
        private final Runnable action;

        public AddButton(String text, float x, float y, float width, float height, Runnable action) {
            this.text = text;
            this.x = x;
            this.y = y;
            this.width = width;
            this.height = height;
            this.action = action;
        }

        public void render(Matrix4f matrix, int mouseX, int mouseY) {
            boolean hovered = isHovered(mouseX, mouseY);
            Color textColor = hovered ? Color.GRAY : Color.WHITE;

            Builder.liquid()
                    .size(new SizeState(width, height))
                    .radius(new QuadRadiusState(4, 4, 4, 4))
                    .color(hovered ? new QuadColorState(new Color(255, 255, 255, 150)) : new QuadColorState(new Color(255, 255, 255, 100)))
                    .build()
                    .render(matrix, x, y);

            float textSize = 10f;
            float textWidth = BIKO_FONT.get().getWidth(text, textSize);
            Builder.text()
                    .text(text)
                    .color(textColor)
                    .size(textSize)
                    .font(BIKO_FONT.get())
                    .thickness(0.05f)
                    .build()
                    .render(matrix, x + width / 2f - textWidth / 2f, y + height / 2f - 4);
        }

        public boolean isHovered(double mouseX, double mouseY) {
            return HoverUtil.isHovered(mouseX, mouseY, x, y, width, height);
        }
    }

    private String generateRandomName() {
        String chars = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
        int length = 8 + (int) (Math.random() * 5);

        StringBuilder name = new StringBuilder();

        for (int i = 0; i < length; i++) {
            name.append(chars.charAt((int) (Math.random() * chars.length())));
        }

        return name.toString();
    }
}

