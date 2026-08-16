package fun.ogi.screens;

import com.google.common.base.Supplier;
import com.google.common.base.Suppliers;
import fun.ogi.Cheap;
import fun.ogi.module.theme.ThemeManager;
import fun.ogi.util.animation.Animation;
import fun.ogi.util.animation.Easing;
import fun.ogi.util.render.builders.Builder;
import fun.ogi.util.render.builders.states.QuadColorState;
import fun.ogi.util.render.builders.states.QuadRadiusState;
import fun.ogi.util.render.builders.states.SizeState;
import fun.ogi.util.render.helper.HoverUtil;
import fun.ogi.util.render.helper.ScissorUtils;
import fun.ogi.util.render.msdf.MsdfFont;
import fun.ogi.util.storages.WaypointStorage;
import fun.ogi.util.storages.waypoint.Waypoint;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.sound.PositionedSoundInstance;
import net.minecraft.client.util.InputUtil;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.math.MathHelper;
import org.joml.Matrix4f;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class WaypointScreen extends Screen {

    private static final Supplier<MsdfFont> BIKO_FONT = Suppliers.memoize(() -> MsdfFont.builder().atlas("biko").data("biko").build());
    private static final Supplier<MsdfFont> ICON_FONT = Suppliers.memoize(() -> MsdfFont.builder().atlas("nur").data("nur").build());

    private final Screen parent;
    private final Animation anim = new Animation(400, Easing.CUBIC_OUT);

    private final TextField nameField = new TextField("Name");
    private final TextField xField = new TextField("X");
    private final TextField yField = new TextField("Y");
    private final TextField zField = new TextField("Z");

    private float scroll;
    private float targetScroll;
    private float maxScroll;

    public WaypointScreen(Screen parent) {
        super(Text.literal("Waypoints"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        anim.start(0, 1);
        client.getSoundManager().play(PositionedSoundInstance.master(SoundEvents.UI_BUTTON_CLICK, 1.0f));
    }

    @Override
    public boolean shouldPause() {
        return false;
    }

    @Override
    public void renderBackground(DrawContext context, int mouseX, int mouseY, float delta) {
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        anim.update();
        float alpha = anim.getValue();

        int sw = client.getWindow().getScaledWidth();
        int sh = client.getWindow().getScaledHeight();
        Matrix4f matrix = context.getMatrices().peek().getPositionMatrix();

        int bgAlpha = (int) (155 * alpha);
        Builder.rectangle()
                .size(new SizeState(sw, sh))
                .radius(new QuadRadiusState(0))
                .color(new QuadColorState(new Color(0, 0, 0, bgAlpha)))
                .build()
                .render(matrix, 0, 0, 0);

        int themeColor = ThemeManager.getInstance().getPrimary();
        int panelColor = (int) (255 * alpha);

        float panelW = 220f;
        float panelH = 170f;
        float panelX = sw / 2f + 20;
        float panelY = (sh - panelH) / 2f;

        float listW = 180f;
        float listH = 170f;
        float listX = panelX - listW - 20;
        float listY = panelY;
        ThemeManager tm = ThemeManager.getInstance();
        int blurCol = tm.getPalette().getHudBackground();
        Builder.blur()
                .size(new SizeState(panelW, panelH))
                .radius(new QuadRadiusState(4))
                .blurRadius(30f)
                .smoothness(1f)
                .color(new QuadColorState(new Color(22,22,22 ,alpha*0.55f)))
                .build().render(matrix, panelX, panelY);

        Builder.blur()
                .size(new SizeState(listW, listH))
                .radius(new QuadRadiusState(4))
                .blurRadius(10f)
                .smoothness(1f)
                .color(new QuadColorState(blurCol)).build()
                .render(matrix, listX, listY, 0);

        int titleColor = new Color(255, 255, 255, panelColor).getRGB();
        Builder.text()
                .text("Waypoints")
                .font(BIKO_FONT.get())
                .size(10f)
                .thickness(0.06f)
                .color(new Color(titleColor, true))
                .build()
                .render(matrix, panelX + (panelW - BIKO_FONT.get().getWidth("Waypoints", 10f)) / 2f, panelY - 16, 0);
        int textPrimary = Color.WHITE.getRGB();
        Builder.text()
                .text("Saved")
                .color(applyAlpha(textPrimary, 0.4f))
                .size(8f)
                .font(BIKO_FONT.get())
                .thickness(0.05f)
                .build()
                .render(matrix, listX + (listW - BIKO_FONT.get().getWidth("Saved", 10f)) / 2f, listY - 16, 0);

        float fieldW = panelW - 30;
        float fieldH = 18;
        float fieldX = panelX + 15;
        float fieldSpacing = 30;

        Builder.text()
                .text("Name")
                .color(applyAlpha(textPrimary, 0.4f))
                .size(8f)
                .font(BIKO_FONT.get())
                .thickness(0.05f)
                .build()
                .render(matrix, fieldX, panelY + 8, 0);
        nameField.setBounds(fieldX, panelY + 18, fieldW, fieldH);
        nameField.draw(matrix, panelColor);

        float coordW = (fieldW - 10) / 3f;
        Builder.text()
                .text("X")
                .font(BIKO_FONT.get())
                .size(7.5f)
                .thickness(0.06f)
                .color(new Color(160, 160, 160, panelColor))
                .build()
                .render(matrix, fieldX, panelY + 8 + fieldSpacing, 0);
        xField.setBounds(fieldX, panelY + 18 + fieldSpacing, coordW, fieldH);
        xField.draw(matrix, panelColor);

        Builder.text()
                .text("Y")
                .color(applyAlpha(textPrimary, 0.4f))
                .size(8f)
                .font(BIKO_FONT.get())
                .thickness(0.05f)
                .build()
                .render(matrix, fieldX + coordW + 5, panelY + 8 + fieldSpacing, 0);
        yField.setBounds(fieldX + coordW + 5, panelY + 18 + fieldSpacing, coordW, fieldH);
        yField.draw(matrix, panelColor);

        Builder.text()
                .text("Z")
                .font(BIKO_FONT.get())
                .size(7.5f)
                .thickness(0.06f)
                .color(new Color(160, 160, 160, panelColor))
                .build()
                .render(matrix, fieldX + (coordW + 5) * 2, panelY + 8 + fieldSpacing, 0);
        zField.setBounds(fieldX + (coordW + 5) * 2, panelY + 18 + fieldSpacing, coordW, fieldH);
        zField.draw(matrix, panelColor);

        float btnW = fieldW;
        float btnH = 18;
        float btnX = fieldX;
        float btnY = panelY + 18 + fieldSpacing * 2 + 12;
        boolean btnHovered = HoverUtil.isHovered(mouseX, mouseY, btnX, btnY, btnW, btnH);
        int btnBgAlpha = btnHovered ? 45 : 25;
        Builder.rectangle()
                .size(new SizeState(btnW, btnH))
                .radius(new QuadRadiusState(4))
                .color(new QuadColorState(new Color(25, 25, 25, (int) (btnBgAlpha * alpha))))
                .build()
                .render(matrix, btnX, btnY, 0);
        Builder.border()
                .size(new SizeState(btnW, btnH))
                .radius(new QuadRadiusState(4))
                .color(new QuadColorState(new Color(45, 45, 45, (int) (255 * alpha))))
                .thickness(1.0f)
                .smoothness(1.0f, 1.0f)
                .build()
                .render(matrix, btnX, btnY, 0);
        String btnText = "Add Waypoint";
        Builder.text()
                .text(btnText)
                .font(BIKO_FONT.get())
                .size(8f)
                .thickness(0.06f)
                .color(new Color(textPrimary, true))
                .build()
                .render(matrix, btnX + (btnW - BIKO_FONT.get().getWidth(btnText, 8f)) / 2f, btnY + (btnH - 8f) / 2f, 0);

        smoothScroll(delta);
        Map<String, Waypoint> all = WaypointStorage.getInstance().getAll();
        List<Waypoint> wpList = new ArrayList<>(all.values());

        float entryH = 30f;
        float listPadX = listX + 8;
        float listPadY = listY + 10;
        float listInnerH = listH - 20;

        ScissorUtils.push();
        ScissorUtils.setFromComponentCoordinates((int) listX, (int) (listY + 10), (int) listW, (int) listInnerH);

        float entryY = listPadY - scroll;
        for (int i = 0; i < wpList.size(); i++) {
            Waypoint wp = wpList.get(i);
            if (entryY + entryH >= listY + 10 && entryY <= listY + listH - 10) {
                Builder.rectangle()
                        .size(new SizeState(listW - 16, 25))
                        .radius(new QuadRadiusState(4))
                        .color(new QuadColorState(new Color(20, 20, 20, (int) (255 * alpha))))
                        .build()
                        .render(matrix, listPadX, entryY, 0);
                Builder.border()
                        .size(new SizeState(listW - 16, 25))
                        .radius(new QuadRadiusState(4))
                        .color(new QuadColorState(new Color(40, 40, 40, (int) (255 * alpha))))
                        .thickness(1.0f)
                        .smoothness(1.0f, 1.0f)
                        .build()
                        .render(matrix, listPadX, entryY, 0);

                Builder.text()
                        .text(wp.getName())
                        .font(BIKO_FONT.get())
                        .size(8f)
                        .thickness(0.06f)
                        .color(new Color(-1, true))
                        .build()
                        .render(matrix, listPadX + 4, entryY + 2, 0);

                String coords = "X:" + wp.getX() + " Y:" + wp.getY() + " Z:" + wp.getZ();
                Builder.text()
                        .text(coords)
                        .font(BIKO_FONT.get())
                        .size(7f)
                        .thickness(0.06f)
                        .color(new Color(125, 125, 125, (int) (255 * alpha)))
                        .build()
                        .render(matrix, listPadX + 4, entryY + 13, 0);
            }
            entryY += entryH;
        }
        ScissorUtils.pop();

        super.render(context, mouseX, mouseY, delta);
    }

    private void smoothScroll(float delta) {
        Map<String, Waypoint> all = WaypointStorage.getInstance().getAll();
        int totalH = all.size() * 30;
        maxScroll = Math.max(0, totalH - 150);
        targetScroll = MathHelper.clamp(targetScroll, 0, maxScroll);
        scroll = MathHelper.lerp(0.18f * delta, scroll, targetScroll);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        nameField.mouseClicked(mouseX, mouseY, button);
        xField.mouseClicked(mouseX, mouseY, button);
        yField.mouseClicked(mouseX, mouseY, button);
        zField.mouseClicked(mouseX, mouseY, button);

        float sw = client.getWindow().getScaledWidth();
        float sh = client.getWindow().getScaledHeight();

        float panelW = 220f;
        float panelH = 170f;
        float panelX = sw / 2f + 20;
        float panelY = (sh - panelH) / 2f;
        float fieldW = panelW - 30;
        float fieldX = panelX + 15;
        float fieldSpacing = 30;
        float btnW = fieldW;
        float btnH = 18;
        float btnX = fieldX;
        float btnY = panelY + 18 + fieldSpacing * 2 + 12;

        if (button == 0 && HoverUtil.isHovered(mouseX, mouseY, btnX, btnY, btnW, btnH)) {
            onAddWaypoint();
            return true;
        }

        float listW = 180f;
        float listH = 170f;
        float listX = panelX - listW - 20;
        float listY = panelY;
        float entryH = 30f;
        float listPadX = listX + 8;
        float listPadY = listY + 10;

        if (button == 1) {
            List<Waypoint> wpList = new ArrayList<>(WaypointStorage.getInstance().getAll().values());
            float entryY = listPadY - scroll;
            for (int i = 0; i < wpList.size(); i++) {
                if (mouseX >= listPadX && mouseX <= listPadX + listW - 16
                        && mouseY >= entryY && mouseY <= entryY + 25) {
                    WaypointStorage.getInstance().remove(wpList.get(i).getName());
                    int totalH = wpList.size() * 30;
                    maxScroll = Math.max(0, totalH - 150);
                    targetScroll = Math.min(targetScroll, maxScroll);
                    return true;
                }
                entryY += entryH;
            }
        }

        return super.mouseClicked(mouseX, mouseY, button);
    }

    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount,double verticalAmount) {
        float listW = 180f;
        float listH = 170f;
        float sw = client.getWindow().getScaledWidth();
        float sh = client.getWindow().getScaledHeight();
        float panelX = sw / 2f + 20;
        float panelY = (sh - listH) / 2f;
        float listX = panelX - listW - 20;
        float listY = panelY;

        if (mouseX >= listX && mouseX <= listX + listW
                && mouseY >= listY && mouseY <= listY + listH) {
            targetScroll -= verticalAmount * 17.5f;
            targetScroll = MathHelper.clamp(targetScroll, 0, maxScroll);
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, horizontalAmount,verticalAmount);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (nameField.isFocused() && nameField.keyPressed(keyCode)) return true;
        if (xField.isFocused() && xField.keyPressed(keyCode)) return true;
        if (yField.isFocused() && yField.keyPressed(keyCode)) return true;
        if (zField.isFocused() && zField.keyPressed(keyCode)) return true;
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean charTyped(char codePoint, int modifiers) {
        if (nameField.isFocused() && nameField.charTyped(codePoint)) return true;
        if (xField.isFocused() && xField.charTyped(codePoint)) return true;
        if (yField.isFocused() && yField.charTyped(codePoint)) return true;
        if (zField.isFocused() && zField.charTyped(codePoint)) return true;
        return super.charTyped(codePoint, modifiers);
    }

    private void onAddWaypoint() {
        String name = nameField.getText().trim();
        String xStr = xField.getText().trim();
        String yStr = yField.getText().trim();
        String zStr = zField.getText().trim();

        if (name.isEmpty() || xStr.isEmpty() || yStr.isEmpty() || zStr.isEmpty()) {
            return;
        }

        try {
            int x = Integer.parseInt(xStr);
            int y = Integer.parseInt(yStr);
            int z = Integer.parseInt(zStr);
            WaypointStorage.getInstance().add(new Waypoint(name, x, y, z));
            nameField.clear();
            xField.clear();
            yField.clear();
            zField.clear();
        } catch (NumberFormatException ignored) {
        }
    }

    @Override
    public void close() {
        client.setScreen(this.parent);
    }

    private static int applyAlpha(int color, float alpha) {
        int a = MathHelper.clamp((int) (alpha * ((color >> 24) & 0xFF)), 0, 255);
        return (a << 24) | (color & 0x00FFFFFF);
    }

    private final class TextField {
        private final String placeholder;
        private String text = "";
        private boolean selected;
        private int cursor;
        private float animatedCursorX;
        private float x, y, width, height;

        private TextField(String placeholder) {
            this.placeholder = placeholder;
        }

        private void setBounds(float x, float y, float width, float height) {
            this.x = x;
            this.y = y;
            this.width = width;
            this.height = height;
        }

        private void draw(Matrix4f matrix, int panelAlpha) {
            Builder.rectangle()
                    .size(new SizeState(width, height))
                    .radius(new QuadRadiusState(4))
                    .color(new QuadColorState(new Color(20, 20, 20, panelAlpha)))
                    .build()
                    .render(matrix, x, y, 0);

            float drawX = this.x + 5f;
            boolean showPlaceholder = this.text.isEmpty() && !this.selected;
            String rendered = showPlaceholder ? this.placeholder : this.text;
            if (!rendered.isEmpty()) {
                int color = showPlaceholder ? new Color(100, 100, 100, panelAlpha).getRGB() : new Color(220, 220, 220, panelAlpha).getRGB();
                Builder.text()
                        .text(rendered)
                        .font(BIKO_FONT.get())
                        .size(8f)
                        .thickness(0.06f)
                        .color(new Color(color, true))
                        .build()
                        .render(matrix, drawX, this.y + (this.height - 8f) / 2f, 0);
            }

            if (this.selected) {
                String before = this.text.substring(0, MathHelper.clamp(this.cursor, 0, this.text.length()));
                float cursorTargetX = drawX + BIKO_FONT.get().getWidth(before, 8f) + 1f;
                if (this.animatedCursorX == 0f) this.animatedCursorX = cursorTargetX;
                this.animatedCursorX = MathHelper.lerp(0.35f, this.animatedCursorX, cursorTargetX);
                float blink = (float) ((Math.sin(System.currentTimeMillis() * 0.006) + 1.0) * 0.5);
                int themeColor = applyAlpha(ThemeManager.getInstance().getPrimary(), 0.6f + 0.4f * blink);
                Builder.rectangle()
                        .size(new SizeState(0.8f, this.height - 6f))
                        .radius(new QuadRadiusState(0))
                        .color(new QuadColorState(new Color(themeColor, true)))
                        .build()
                        .render(matrix, this.animatedCursorX, this.y + 3f, 0);
            }
        }

        private void mouseClicked(double mouseX, double mouseY, int button) {
            if (button == 0) {
                this.selected = HoverUtil.isHovered(mouseX, mouseY, this.x, this.y, this.width, this.height);
                if (this.selected) this.cursor = this.text.length();
            }
        }

        private boolean keyPressed(int keyCode) {
            if (!this.selected) return false;
            boolean ctrl = InputUtil.isKeyPressed(client.getWindow().getHandle(), 341)
                    || InputUtil.isKeyPressed(client.getWindow().getHandle(), 345);
            if (ctrl && keyCode == 86) {
                insert(client.keyboard.getClipboard());
            } else if (keyCode == 259) {
                if (this.cursor > 0) {
                    this.text = this.text.substring(0, this.cursor - 1) + this.text.substring(this.cursor);
                    this.cursor--;
                }
            } else if (keyCode == 261) {
                if (this.cursor < this.text.length()) {
                    this.text = this.text.substring(0, this.cursor) + this.text.substring(this.cursor + 1);
                }
            } else if (keyCode == 263) {
                this.cursor = Math.max(0, this.cursor - 1);
            } else if (keyCode == 262) {
                this.cursor = Math.min(this.text.length(), this.cursor + 1);
            } else if (keyCode == 256) {
                this.selected = false;
            } else if (keyCode == 257 || keyCode == 335) {
                this.selected = false;
            }
            return true;
        }

        private boolean charTyped(char chr) {
            if (!this.selected || this.text.length() >= 16) return false;
            if (!Character.isLetterOrDigit(chr) && chr != '_' && chr != '-' && chr != ' ') return false;
            this.text = this.text.substring(0, this.cursor) + chr + this.text.substring(this.cursor);
            this.cursor++;
            return true;
        }

        private void insert(String value) {
            if (value == null || value.isEmpty()) return;
            String sanitized = value.replaceAll("[^a-zA-Z0-9_\\- ]", "");
            if (sanitized.isEmpty()) return;
            int free = Math.max(0, 16 - this.text.length());
            if (sanitized.length() > free) sanitized = sanitized.substring(0, free);
            this.text = this.text.substring(0, this.cursor) + sanitized + this.text.substring(this.cursor);
            this.cursor += sanitized.length();
        }

        private String getText() {
            return this.text;
        }

        private boolean isFocused() {
            return this.selected;
        }

        private void clear() {
            this.text = "";
            this.cursor = 0;
            this.animatedCursorX = 0;
        }
    }
}

