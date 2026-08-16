package fun.ogi.screens;

import com.google.common.base.Supplier;
import com.google.common.base.Suppliers;
import fun.ogi.mixin.MinecraftClientAccessor;
import fun.ogi.module.theme.ThemeManager;
import fun.ogi.util.altmanager.Account;
import fun.ogi.util.altmanager.AltManager;
import fun.ogi.util.altmanager.MainGenerator;
import fun.ogi.util.render.builders.Builder;
import fun.ogi.util.render.builders.states.QuadColorState;
import fun.ogi.util.render.builders.states.QuadRadiusState;
import fun.ogi.util.render.builders.states.SizeState;
import fun.ogi.util.animation.Animation;
import fun.ogi.util.animation.Easing;
import fun.ogi.util.render.helper.HoverUtil;
import fun.ogi.util.render.helper.ScissorUtils;
import fun.ogi.util.render.msdf.MsdfFont;
import fun.ogi.util.render.providers.ColorProvider;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.session.Session;
import net.minecraft.client.sound.PositionedSoundInstance;
import net.minecraft.client.util.InputUtil;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.math.MathHelper;
import org.joml.Matrix4f;

import java.awt.Color;
import java.lang.reflect.Constructor;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public class AltManagerScreen extends Screen {
    private static final Supplier<MsdfFont> BIKO_FONT = Suppliers.memoize(() -> MsdfFont.builder().atlas("biko").data("biko").build());
    private static final Supplier<MsdfFont> ALTMANAGER_FONT = Suppliers.memoize(() -> MsdfFont.builder().atlas("altmanager/font").data("altmanager/font").build());
    private static final float PANEL_WIDTH = 400.0f;
    private static final float PANEL_HEIGHT = 160.0f;

    private final Map<String, Float> textScrollPhase = new HashMap<>();
    private final Screen parent;
    private final TextField nicknameField = new TextField("Nickname", "A");
    private final TextField searchField = new TextField("Search", "B");
    private final Animation leftPanelAnim = new Animation(600, Easing.CUBIC_OUT);
    private final Animation rightPanelAnim = new Animation(600, Easing.CUBIC_OUT);
    private final Animation appearAnimation = new Animation(450, Easing.CUBIC_OUT);
    private Account selected;
    private float scroll;
    private float targetScroll;
    private float maxScroll;

    public AltManagerScreen(Screen parent) {
        super(Text.empty());
        this.parent = parent;
        this.selectInitialAccount();
    }

    private void setSelected(Account account) {
        this.selected = account;
        AltManager.MANAGER.saveLastSelected(account != null ? account.name() : null);
    }

    @Override
    protected void init() {
        super.init();
        this.selectInitialAccount();
        leftPanelAnim.start(0, 1);
        rightPanelAnim.start(0, 1);
        appearAnimation.start(0, 1);
    }

    @Override
    public boolean shouldPause() {
        return false;
    }

    @Override
    public boolean shouldCloseOnEsc() {
        return true;
    }

    @Override
    public void renderBackground(DrawContext context, int mouseX, int mouseY, float delta) {
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        leftPanelAnim.update();
        rightPanelAnim.update();
        appearAnimation.update();
        float appear = appearAnimation.getValue();

        MatrixStack matrices = context.getMatrices();

        BackgroundManager.render(context, width, height, mouseX, mouseY, delta, appear);
        context.draw();

        float panelX = (float) this.width / 2.0f - 200.0f + leftOffset();
        float panelY = (float) this.height / 2.0f - 80.0f + 40.0f;
        float leftWidth = 150.94339f;
        float listX = panelX + leftWidth + 10.0f + rightOffset();
        float listWidth = PANEL_WIDTH - leftWidth;
        float panelPart = 75.0f;

        this.scroll = MathHelper.lerp(0.18f * delta, this.scroll, this.targetScroll);

        matrices.push();
        matrices.translate(width / 2f, height / 2f, 0);
        float scale = 0.9f + 0.1f * appear;
        matrices.scale(scale, scale, 1f);
        matrices.translate(-width / 2f, -height / 2f, 0);
        Matrix4f matrix = matrices.peek().getPositionMatrix();

        this.drawPanel(matrix, panelX, panelY, leftWidth, panelPart);
        this.drawPanel(matrix, panelX, panelY + panelPart + 10.0f, leftWidth, panelPart);
        this.drawPanel(matrix, listX, panelY, listWidth, PANEL_HEIGHT);
        this.drawAccountList(context, matrix, mouseX, mouseY, listX, panelY, listWidth, PANEL_HEIGHT);
        this.drawFieldsAndButtons(matrix, mouseX, mouseY, panelX, panelY, leftWidth, panelPart);

        matrices.pop();

        renderFooter(matrices);

        super.render(context, mouseX, mouseY, delta);
    }

    private void renderFooter(MatrixStack matrices) {
        MsdfFont footerFont = BIKO_FONT.get();
        if (footerFont == null) return;

        String leftText = "GREED CLIENT | VER 1.0.0";
        String rightText = "#SELFKODER #NOVIBEECODING";
        float textSize = 5f;

        float leftWidth = footerFont.getWidth(leftText, textSize);
        float rightWidth = footerFont.getWidth(rightText, textSize);

        float leftX = 10f;
        float rightX = width - rightWidth - 10f;
        float y = height - textSize - 6f;

        Matrix4f matrix = matrices.peek().getPositionMatrix();

        Builder.text()
                .text(leftText)
                .font(footerFont)
                .size(textSize)
                .color(Color.WHITE.getRGB())
                .thickness(0.06f)
                .build()
                .render(matrix, leftX, y, 0);

        Builder.text()
                .text(rightText)
                .font(footerFont)
                .size(textSize)
                .color(Color.WHITE.getRGB())
                .thickness(0.06f)
                .build()
                .render(matrix, rightX, y, 0);
    }

    private void drawFieldsAndButtons(Matrix4f matrix, int mouseX, int mouseY, float panelX, float panelY, float leftWidth, float panelPart) {
        float fieldX = panelX + 10.0f;
        float fieldWidth = leftWidth - 20.0f;
        float fieldHeight = 27.0f;
        float fieldGap = 8.0f;
        float verticalPadding = (panelPart - fieldHeight * 2.0f - fieldGap) / 2.0f;
        float nicknameY = panelY + verticalPadding;
        float searchY = nicknameY + fieldHeight + fieldGap;

        this.drawInputBackground(matrix, fieldX, nicknameY, fieldWidth, fieldHeight, mouseX, mouseY);
        this.drawInputBackground(matrix, fieldX, searchY, fieldWidth, fieldHeight, mouseX, mouseY);
        this.nicknameField.setBounds(fieldX + 5.0f, nicknameY, fieldWidth - 10.0f, fieldHeight);
        this.searchField.setBounds(fieldX + 5.0f, searchY, fieldWidth - 10.0f, fieldHeight);
        this.nicknameField.draw(matrix);
        this.searchField.draw(matrix);

        float bottomY = panelY + panelPart + 20.0f;
        float buttonWidth = leftWidth - 20.0f;
        float smallWidth = buttonWidth / 2.0f - 6.0f;

        this.drawButton(matrix, fieldX, bottomY, buttonWidth, 25.0f, "Add", false, mouseX, mouseY);
        this.drawButton(matrix, fieldX, bottomY + 25.0f + 5.0f, smallWidth, 25.0f, "Generate", false, mouseX, mouseY);
        this.drawButton(matrix, fieldX + smallWidth + 12.0f, bottomY + 25.0f + 5.0f, smallWidth, 25.0f, "Clear all", true, mouseX, mouseY);
    }

    private float leftOffset() {
        return (1f - leftPanelAnim.getValue()) * -180f;
    }

    private float rightOffset() {
        return (1f - rightPanelAnim.getValue()) * 200f;
    }

    private void drawAccountList(DrawContext context, Matrix4f matrix, int mouseX, int mouseY, float x, float y, float width, float height) {
        List<Account> accounts = this.filteredAccounts();
        float cardWidth = (width - 30.0f) / 2.0f;
        float currentOffset = 0.0f;
        int column = 0;

        ScissorUtils.push();
        ScissorUtils.setFromComponentCoordinates((int) x, (int) (y + 10.0f), (int) width, (int) (height - 20.0f));

        for (Account account : accounts) {
            float accountX = x + 10.0f + (cardWidth + 10.0f) * (float) column;
            float accountY = y + 10.0f + this.scroll + currentOffset;
            this.drawAccount(context, matrix, account, accountX, accountY, cardWidth, 40.0f, mouseX, mouseY);
            if (++column <= 1) continue;
            column = 0;
            currentOffset += 50.0f;
        }

        if (accounts.isEmpty()) {
            String noAccounts = "No accounts";
            float ns = 13f;
            float nw = BIKO_FONT.get().getWidth(noAccounts, ns);
            Builder.text()
                    .text(noAccounts)
                    .font(BIKO_FONT.get())
                    .size(ns)
                    .color(Color.WHITE.getRGB())
                    .thickness(0.07f)
                    .build()
                    .render(matrix, x + width / 2.0f - nw / 2.0f, y + height / 2.0f - ns / 2.0f, 0);
        }

        ScissorUtils.pop();

        if (column != 0) {
            currentOffset += 50.0f;
        }
        float contentHeight = currentOffset > 0.0f ? currentOffset - 10.0f : 0.0f;
        this.maxScroll = Math.min(0.0f, height - contentHeight - 20.0f);
        this.targetScroll = MathHelper.clamp(this.targetScroll, this.maxScroll, 0.0f);
        this.scroll = MathHelper.clamp(this.scroll, this.maxScroll, 0.0f);
    }

    private void drawAccount(DrawContext context, Matrix4f matrix, Account account, float x, float y, float width, float height, int mouseX, int mouseY) {
        boolean hovered = HoverUtil.isHovered(mouseX, mouseY, x, y, width, height);
        boolean current = this.isCurrent(account);
        boolean selectedAccount = this.selected == account;

        int fill = rgba(17, 20, 38, 145);
        if (hovered || selectedAccount) {
            fill = rgba(24, 28, 52, 210);
        }
        if (current) {
            fill = rgba(46, 54, 100, 230);
        }

        Builder.blur()
                .size(new SizeState(width, height))
                .radius(new QuadRadiusState(4))
                .color(new QuadColorState(fill))
                .blurRadius(30f)
                .smoothness(1f)
                .build()
                .render(matrix, x, y, 0);

        float headSize = height - 10.0f;

        Builder.rectangle()
                .size(new SizeState(headSize, headSize))
                .radius(new QuadRadiusState(headSize / 2f))
                .color(new QuadColorState(withAlpha(themeColor(45), 80)))
                .build()
                .render(matrix, x + 5.0f, y + 5.0f, 0);

        float textX = x + height;
        float maxNameWidth = width - height - 26.0f;
        this.drawTextTruncated(matrix, account.name(), textX, y + 9.0f, maxNameWidth, Color.WHITE.getRGB());

        String date = account.creationDate().format(DateTimeFormatter.ofPattern("dd MMMM HH:mm", Locale.ENGLISH));
        Builder.text()
                .text(date)
                .font(BIKO_FONT.get())
                .size(8f)
                .color(withAlpha(-1, 76))
                .thickness(0.04f)
                .build()
                .render(matrix, textX, y + 25.0f, 0);

    }

    private void drawPanel(Matrix4f matrix, float x, float y, float width, float height) {
        Builder.blur()
                .size(new SizeState(width, height))
                .radius(new QuadRadiusState(8))
                .color(new QuadColorState(rgba(17, 20, 38, 155)))
                .blurRadius(30f)
                .smoothness(1f)
                .build()
                .render(matrix, x, y, 0);
    }

    private void drawInputBackground(Matrix4f matrix, float x, float y, float width, float height, int mouseX, int mouseY) {
        boolean hovered = HoverUtil.isHovered(mouseX, mouseY, x, y, width, height);
        int fill = hovered ? rgba(24, 28, 52, 205) : rgba(17, 20, 38, 145);

        Builder.blur()
                .size(new SizeState(width, height))
                .radius(new QuadRadiusState(4))
                .color(new QuadColorState(fill))
                .blurRadius(30f)
                .smoothness(1f)
                .build()
                .render(matrix, x, y, 0);
    }

    private void drawButton(Matrix4f matrix, float x, float y, float width, float height, String text, boolean danger, int mouseX, int mouseY) {
        boolean hovered = HoverUtil.isHovered(mouseX, mouseY, x, y, width, height);
        int base = danger ? rgba(60, 12, 18, 135) : rgba(17, 20, 38, 135);
        int hoverFill = danger ? rgba(80, 16, 24, 245) : rgba(24, 28, 52, 245);
        int fill = hovered ? hoverFill : base;

        Builder.blur()
                .size(new SizeState(width, height))
                .radius(new QuadRadiusState(4))
                .color(new QuadColorState(fill))
                .blurRadius(30f)
                .smoothness(1f)
                .build()
                .render(matrix, x, y, 0);

        float ts = 10f;
        float tw = BIKO_FONT.get().getWidth(text, ts);
        Builder.text()
                .text(text)
                .font(BIKO_FONT.get())
                .size(ts)
                .color(Color.WHITE.getRGB())
                .thickness(0.07f)
                .build()
                .render(matrix, x + width / 2.0f - tw / 2.0f, y + height / 2.0f - ts / 2.0f, 0);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        float panelX = (float) this.width / 2.0f - 200.0f + leftOffset();
        float panelY = (float) this.height / 2.0f - 80.0f + 40.0f;
        float leftWidth = 150.94339f;
        float listX = panelX + leftWidth + 10.0f + rightOffset();
        float listWidth = PANEL_WIDTH - leftWidth;
        float panelPart = 75.0f;
        float bottomY = panelY + panelPart + 20.0f;
        float buttonWidth = leftWidth - 20.0f;
        float smallWidth = buttonWidth / 2.0f - 6.0f;

        this.nicknameField.mouseClicked(mouseX, mouseY, button);
        this.searchField.mouseClicked(mouseX, mouseY, button);

        if (button == 0 && HoverUtil.isHovered(mouseX, mouseY, panelX + 10.0f, bottomY, buttonWidth, 25.0)) {
            playClickSound();
            this.addAccount(this.nicknameField.text());
            return true;
        }
        if (button == 0 && HoverUtil.isHovered(mouseX, mouseY, panelX + 10.0f, bottomY + 25.0f + 5.0f, smallWidth, 25.0)) {
            playClickSound();
            this.addAccount(MainGenerator.generate(), false);
            return true;
        }
        if (button == 0 && HoverUtil.isHovered(mouseX, mouseY, panelX + 10.0f + smallWidth + 12.0f, bottomY + 25.0f + 5.0f, smallWidth, 25.0)) {
            playClickSound();
            AltManager.MANAGER.clearAccounts();
            this.setSelected(null);
            return true;
        }
        if (HoverUtil.isHovered(mouseX, mouseY, listX, panelY, listWidth, PANEL_HEIGHT) && this.clickAccount(mouseX, mouseY, button, listX, panelY, listWidth)) {
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    private boolean clickAccount(double mouseX, double mouseY, int button, float listX, float panelY, float listWidth) {
        List<Account> accounts = this.filteredAccounts();
        float cardWidth = (listWidth - 30.0f) / 2.0f;
        float currentOffset = 0.0f;
        int column = 0;

        for (Account account : accounts) {
            float ax = listX + 10.0f + (cardWidth + 10.0f) * (float) column;
            float ay = panelY + 10.0f + this.scroll + currentOffset;

            if (HoverUtil.isHovered(mouseX, mouseY, ax + cardWidth - 29.0f, ay + 7.0f, 18.0, 16.0)) {
                account.toggleFavorite();
                AltManager.MANAGER.save();
                playClickSound();
                return true;
            }
            if (HoverUtil.isHovered(mouseX, mouseY, ax + cardWidth - 20.0f, ay + 24.0f, 16.0, 16.0) || (button == 1 && HoverUtil.isHovered(mouseX, mouseY, ax, ay, cardWidth, 40.0))) {
                AltManager.MANAGER.removeAccount(account.name());
                if (this.selected == account) {
                    this.setSelected(AltManager.MANAGER.stream().findFirst().orElse(null));
                }
                playClickSound();
                return true;
            }

            if (HoverUtil.isHovered(mouseX, mouseY, ax, ay, cardWidth, 40.0)) {
                if (button == 0) {
                    setSession(account.name());
                    this.setSelected(account);
                    AltManager.MANAGER.save();
                    playClickSound();
                }
                return true;
            }

            if (++column <= 1) continue;
            column = 0;
            currentOffset += 50.0f;
        }
        return false;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        this.targetScroll = MathHelper.clamp(this.targetScroll + (float) verticalAmount * 17.5f, this.maxScroll, 0.0f);
        return true;
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (this.nicknameField.keyPressed(keyCode)) {
            if (keyCode == 257) {
                this.addAccount(this.nicknameField.text());
                this.nicknameField.selected(false);
            }
            return true;
        }
        if (this.searchField.keyPressed(keyCode)) {
            if (keyCode == 257) {
                this.searchField.selected(false);
            }
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean charTyped(char chr, int modifiers) {
        boolean handled = this.nicknameField.charTyped(chr);
        handled = this.searchField.charTyped(chr) || handled;
        return handled || super.charTyped(chr, modifiers);
    }

    @Override
    public void close() {
        AltManager.MANAGER.save();
        if (client != null) {
            client.setScreen(this.parent);
        }
    }

    private void addAccount(String name) {
        this.addAccount(name, true);
    }

    private void addAccount(String name, boolean switchTo) {
        if (name == null) return;
        String normalized = name.trim();
        if (normalized.length() < 3 || normalized.length() > 16) return;
        Account account = AltManager.MANAGER.getAccount(normalized).orElseGet(() -> {
            Account created = new Account(LocalDateTime.now(), normalized);
            AltManager.MANAGER.addAccount(created);
            return created;
        });
        if (switchTo || this.selected == null) {
            this.setSelected(account);
            setSession(account.name());
        }
        this.nicknameField.cursorToEnd();
        AltManager.MANAGER.save();
    }

    private void selectInitialAccount() {
        if (this.selected != null) return;
        String last = AltManager.MANAGER.file().getLast();
        Optional<Account> lastAccount = AltManager.MANAGER.getAccount(last);
        this.selected = lastAccount.orElseGet(() -> AltManager.MANAGER.stream().findFirst().orElse(null));
        if (this.selected != null && !last.isEmpty()) {
            setSession(this.selected.name());
        }
    }

    private List<Account> filteredAccounts() {
        String search = this.searchField.text().trim().toLowerCase(Locale.ROOT);
        Comparator<Account> comparator = Comparator.<Account, Boolean>comparing(a -> !a.favorite())
                .thenComparing(Account::creationDate, Comparator.reverseOrder());
        return AltManager.MANAGER.stream()
                .filter(a -> search.isEmpty() || a.name().toLowerCase(Locale.ROOT).contains(search))
                .sorted(comparator)
                .toList();
    }

    private boolean isCurrent(Account account) {
        return client.getSession() != null && client.getSession().getUsername().equalsIgnoreCase(account.name());
    }

    private static void setSession(String name) {
        try {
            Constructor<Session> constructor = Session.class.getDeclaredConstructor(String.class, UUID.class, String.class, Optional.class, Optional.class, Session.AccountType.class);
            constructor.setAccessible(true);
            Session session = constructor.newInstance(name, UUID.nameUUIDFromBytes(("OfflinePlayer:" + name).getBytes()),
                    MinecraftClient.getInstance().getSession() == null ? "" : MinecraftClient.getInstance().getSession().getAccessToken(),
                    Optional.empty(), Optional.empty(), Session.AccountType.MOJANG);
            ((MinecraftClientAccessor) MinecraftClient.getInstance()).setSession(session);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static void playClickSound() {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client != null) {
            client.getSoundManager().play(PositionedSoundInstance.master(SoundEvents.UI_BUTTON_CLICK, 1.0f));
        }
    }

    private static int themeColor() {
        try {
            return ThemeManager.getInstance().getPrimary();
        } catch (Exception ignored) {
            return 0xFF6E8F9B;
        }
    }

    private static int themeColor(int index) {
        try {
            return index <= 50 ? ThemeManager.getInstance().getPrimary() : ThemeManager.getInstance().getSecondary();
        } catch (Exception ignored) {
            return themeColor();
        }
    }

    private static int withAlpha(int color, int alpha) {
        int r = (color >> 16) & 0xFF;
        int g = (color >> 8) & 0xFF;
        int b = color & 0xFF;
        return ColorProvider.pack(r, g, b, MathHelper.clamp(alpha, 1, 255));
    }

    private static int rgba(int r, int g, int b, int a) {
        return ColorProvider.pack(r, g, b, a);
    }

    private void drawTextTruncated(Matrix4f matrix, String text, float x, float y, float maxWidth, int color) {
        MsdfFont font = BIKO_FONT.get();
        if (font == null || text == null || text.isEmpty() || maxWidth <= 0.0f) return;
        float totalWidth = font.getWidth(text, 14f);
        if (totalWidth <= maxWidth) {
            Builder.text()
                    .text(text)
                    .font(font)
                    .size(14f)
                    .color(color)
                    .thickness(0.07f)
                    .build()
                    .render(matrix, x, y, 0);
            return;
        }
        float overflow = totalWidth - maxWidth;
        float phase = this.textScrollPhase.getOrDefault(text, 0.0f);
        phase += 0.003f;
        if (phase > 1.0f) phase -= 1.0f;
        this.textScrollPhase.put(text, phase);
        float pingPong = phase < 0.5f ? phase * 2.0f : 2.0f - phase * 2.0f;
        float eased = pingPong * pingPong * (3.0f - 2.0f * pingPong);
        float scrollOffset = overflow * eased;
        ScissorUtils.push();
        ScissorUtils.setFromComponentCoordinates((int) x, (int) (y - 2.0f), (int) maxWidth, 20);
        Builder.text()
                .text(text)
                .font(font)
                .size(14f)
                .color(color)
                .thickness(0.07f)
                .build()
                .render(matrix, x - scrollOffset, y, 0);
        ScissorUtils.pop();
    }

    private final class TextField {
        private final String placeholder;
        private final String icon;
        private String text = "";
        private boolean selected;
        private int cursor;
        private float animatedCursorX;
        private float x;
        private float y;
        private float width;
        private float height;

        private TextField(String placeholder, String icon) {
            this.placeholder = placeholder;
            this.icon = icon;
        }

        private void setBounds(float x, float y, float width, float height) {
            this.x = x;
            this.y = y;
            this.width = width;
            this.height = height;
        }

        private void draw(Matrix4f matrix) {
            MsdfFont textFont = BIKO_FONT.get();
            MsdfFont iconFont = ALTMANAGER_FONT.get();

            boolean drawPlaceholder = this.text.isEmpty() && !this.selected;
            String rendered = drawPlaceholder ? this.placeholder : this.text;

            float iconWidth = 0.0f;
            if (iconFont != null && !this.icon.isEmpty()) {
                iconWidth = iconFont.getWidth(this.icon, 8f);
            }
            float textWidth = 0.0f;
            if (textFont != null && !rendered.isEmpty()) {
                textWidth = textFont.getWidth(rendered, 10f);
            }

            float gap = iconFont != null && !this.icon.isEmpty() && !rendered.isEmpty() ? 6.0f : 0.0f;
            float drawX = this.x + (this.width - (iconWidth + gap + textWidth)) / 2.0f;

            if (iconFont != null && !this.icon.isEmpty()) {
                Builder.text()
                        .text(this.icon)
                        .font(iconFont)
                        .size(8f)
                        .color(Color.WHITE.getRGB())
                        .thickness(0.07f)
                        .build()
                        .render(matrix, drawX, this.y + this.height / 2.0f - 5.5f + 5.0f, 0);
                drawX += iconWidth + 6.0f;
            }

            if (!rendered.isEmpty()) {
                int color = drawPlaceholder ? withAlpha(Color.WHITE.getRGB(), 125) : Color.WHITE.getRGB();
                Builder.text()
                        .text(rendered)
                        .font(textFont)
                        .size(10f)
                        .color(color)
                        .thickness(0.08f)
                        .build()
                        .render(matrix, drawX, this.y + this.height / 2.0f - 8f + 5.0f, 0);
            }

            if (this.selected && textFont != null) {
                String beforeCursor = this.text.substring(0, MathHelper.clamp(this.cursor, 0, this.text.length()));
                float cursorTargetX = drawX + textFont.getWidth(beforeCursor, 10f) + 1.0f;
                if (this.animatedCursorX == 0.0f) {
                    this.animatedCursorX = cursorTargetX;
                }
                this.animatedCursorX = MathHelper.lerp(0.35f, this.animatedCursorX, cursorTargetX);
                float blink = (float) ((Math.sin(System.currentTimeMillis() * 0.006) + 1.0) * 0.5);
                float cursorHeight = (this.height - 14.0f) * (0.88f + blink * 0.12f);
                float cursorY = this.y + (this.height - cursorHeight) / 2.0f;
                Builder.rectangle()
                        .size(new SizeState(0.8f, cursorHeight))
                        .radius(new QuadRadiusState(0))
                        .color(new QuadColorState(withAlpha(Color.WHITE.getRGB(), (int) (95.0f + 160.0f * blink))))
                        .build()
                        .render(matrix, this.animatedCursorX, cursorY, 0);
            }
        }

        private void mouseClicked(double mouseX, double mouseY, int button) {
            if (button == 0) {
                this.selected = HoverUtil.isHovered(mouseX, mouseY, this.x - 5.0f, this.y, this.width + 10.0f, this.height);
                if (this.selected) {
                    this.cursorToEnd();
                }
            }
        }

        private boolean keyPressed(int keyCode) {
            if (!this.selected) return false;
            boolean control = InputUtil.isKeyPressed(client.getWindow().getHandle(), 341) || InputUtil.isKeyPressed(client.getWindow().getHandle(), 345);
            if (control && keyCode == 86) {
                this.insert(client.keyboard.getClipboard());
            } else if (keyCode == 259) {
                if (this.cursor > 0) {
                    this.text = this.text.substring(0, this.cursor - 1) + this.text.substring(this.cursor);
                    --this.cursor;
                }
            } else if (keyCode == 261) {
                if (this.cursor < this.text.length()) {
                    this.text = this.text.substring(0, this.cursor) + this.text.substring(this.cursor + 1);
                }
            } else if (keyCode == 263) {
                this.cursor = Math.max(0, this.cursor - 1);
            } else if (keyCode == 262) {
                this.cursor = Math.min(this.text.length(), this.cursor + 1);
            } else if (keyCode == 268) {
                this.cursor = 0;
            } else if (keyCode == 269) {
                this.cursorToEnd();
            } else if (keyCode == 256) {
                this.selected = false;
            }
            return true;
        }

        private boolean charTyped(char chr) {
            if (!this.selected || this.text.length() >= 16 || !isAllowed(chr)) {
                return false;
            }
            this.insert(String.valueOf(chr));
            return true;
        }

        private void insert(String value) {
            if (value == null || value.isEmpty()) return;
            String sanitized = value.chars().filter(code -> isAllowed((char) code))
                    .collect(StringBuilder::new, StringBuilder::appendCodePoint, StringBuilder::append).toString();
            if (sanitized.isEmpty()) return;
            int free = Math.max(0, 16 - this.text.length());
            if (sanitized.length() > free) {
                sanitized = sanitized.substring(0, free);
            }
            this.text = this.text.substring(0, this.cursor) + sanitized + this.text.substring(this.cursor);
            this.cursor += sanitized.length();
        }

        private static boolean isAllowed(char chr) {
            return Character.isLetterOrDigit(chr) || chr == '_' || chr == '-';
        }

        private String text() {
            return this.text;
        }

        private void selected(boolean selected) {
            this.selected = selected;
        }

        private void cursorToEnd() {
            this.cursor = this.text.length();
            this.animatedCursorX = 0.0f;
        }
    }
}