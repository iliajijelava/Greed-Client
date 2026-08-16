package fun.ogi.screens;

import com.google.common.base.Supplier;
import com.google.common.base.Suppliers;
import fun.ogi.Cheap;
import fun.ogi.module.theme.ThemeManager;
import fun.ogi.util.figura.FiguraAvatarInstaller;
import fun.ogi.util.render.builders.Builder;
import fun.ogi.util.render.builders.impl.BlurBuilder;
import fun.ogi.util.render.builders.states.QuadColorState;
import fun.ogi.util.render.builders.states.QuadRadiusState;
import fun.ogi.util.render.builders.states.SizeState;
import fun.ogi.util.render.helper.HoverUtil;
import fun.ogi.util.render.msdf.MsdfFont;
import org.figuramc.figura.FiguraMod;
import org.figuramc.figura.avatar.Avatar;
import org.figuramc.figura.avatar.AvatarManager;
import org.figuramc.figura.avatar.local.LocalAvatarLoader;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.ingame.InventoryScreen;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.texture.NativeImageBackedTexture;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;
import org.joml.Matrix4f;
import org.lwjgl.glfw.GLFW;

import java.awt.Color;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

import static fun.ogi.module.Module.mc;

public class FiguraModelsGuiComponent extends Screen {
    private static final Supplier<MsdfFont> BIKO_FONT = Suppliers.memoize(() -> MsdfFont.builder().atlas("biko").data("biko").build());
    private static final Supplier<MsdfFont> ICON_FONT  = Suppliers.memoize(() -> MsdfFont.builder().atlas("nur").data("nur").build());

    private float x, y;
    private static final float WINDOW_WIDTH   = 640f;
    private static final float WINDOW_HEIGHT  = 320f;
    private static final float TOPBAR_HEIGHT  = 24f;
    private static final float TAB_H          = 20f;
    private static final float SEARCH_H       = 20f;
    private static final float STATUS_H       = 16f;
    private static final float ROW_H          = 46f;
    private static final float ROW_GAP        = 5f;
    private static final float ACTION_H       = 18f;

    private final List<ModelEntry> models = new ArrayList<>();
    private final List<ModelEntry> visibleModels = new ArrayList<>();
    private final Map<String, Identifier> previewTextures = new ConcurrentHashMap<>();
    private final Set<String> previewTextureRequests = ConcurrentHashMap.newKeySet();
    private final Set<String> favoriteKeys = new HashSet<>();

    private Section selectedSection = Section.ALL;
    private String searchText = "";
    private boolean searchFocused;
    private float listScroll;
    private float smoothedListScroll;
    private long lastScan;

    private volatile String selectedKey = "";
    private static volatile String appliedKey = "";
    private volatile String selectedName = "Not choosen";
    private volatile String status = "Ready · Choose Model";
    private volatile boolean installing;
    private boolean installRefreshed;
    private volatile String pendingKey = "";
    private volatile String pendingName = "";
    private long pendingStart;
    private long lastPollLogMs;

    private float previewYaw = -24f;
    private float previewPitch = 3f;
    private float previewZoom = 1f;
    private boolean previewDragging;

    public FiguraModelsGuiComponent() {
        super(Text.literal("Figura models"));
    }

    @Override
    protected void init() {
        reflow();
        installRefreshed = false;
        FiguraAvatarInstaller.installAsync();
        refreshModels(true);
        updateVisibleModels();
    }

    @Override
    public boolean shouldPause() {
        return false;
    }

    private void reflow() {
        this.x = (this.width - WINDOW_WIDTH) / 2f;
        this.y = (this.height - WINDOW_HEIGHT) / 2f;
    }

    

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        reflow();
        if (!installRefreshed && FiguraAvatarInstaller.isFinished()) {
            installRefreshed = true;
            refreshModels(true);
            updateVisibleModels();
            Throwable error = FiguraAvatarInstaller.getLastError();
            if (error != null) {
                status = "Install error: " + error.getMessage();
            } else if (models.isEmpty()) {
                status = "No models found in run/figura/avatars";
        } else {
            status = "Ready · " + models.size() + " models";
        }
        }
        updatePendingApply();
        refreshModels(false);
        updateVisibleModels();

        int bg     = ThemeManager.getInstance().getPalette().getBackground();
        int accent = ThemeManager.getInstance().getPrimary();
        int textP  = ThemeManager.getInstance().getPalette().getTextPrimary();
        int textS  = ThemeManager.getInstance().getPalette().getTextSecondary();

//        context.fill(0, 0, this.width, this.height, new Color(0, 0, 0, 150).getRGB());

        Matrix4f mx = context.getMatrices().peek().getPositionMatrix();
        new BlurBuilder()
                .size(new SizeState(WINDOW_WIDTH, WINDOW_HEIGHT))
                .radius(new QuadRadiusState(5))
                .blurRadius(10f)
                .smoothness(1f)
                .color(new QuadColorState(new Color(bg)))
                .build().render(mx, x, y);

        Layout l = layout();

        renderSearch(mx, l, mouseX, mouseY, accent, textP, textS);
        renderTabs(mx, l, mouseX, mouseY, accent, textP, textS);
        renderList(context, mx, l, mouseX, mouseY, accent, textP, textS);
        renderPreview(context, mx, l, mouseX, mouseY, accent, textP, textS);
        renderStatus(mx, l, textS);
    }

    private record Layout(float searchX, float searchY, float searchW,
                          float tabsX, float tabsY, float tabsW,
                          float listX, float listY, float listW, float listH,
                          float previewX, float previewY, float previewW, float previewH,
                          float statusX, float statusY, float statusW) {
    }

    private Layout layout() {
        float contentX = x + 10f;
        float contentW = WINDOW_WIDTH - 20f;
        float searchW = 160f;
        float searchX = contentX + contentW - searchW;
        float searchY = y + 8f;
        float tabsY = y + 8f + SEARCH_H + 6f;
        float statusY = y + WINDOW_HEIGHT - STATUS_H - 6f;
        float bodyY = tabsY + TAB_H + 8f;
        float bodyH = statusY - bodyY - 6f;
        float gap = 10f;
        float listW = contentW * 0.56f;
        float previewW = contentW - listW - gap;
        return new Layout(searchX, searchY, searchW,
                contentX, tabsY, contentW,
                contentX, bodyY, listW, bodyH,
                contentX + listW + gap, bodyY, previewW, bodyH,
                contentX, statusY, contentW);
    }

    private void renderSearch(Matrix4f mx, Layout l, int mouseX, int mouseY, int accent, int textP, int textS) {
        boolean hovered = HoverUtil.isHovered(mouseX, mouseY, l.searchX, l.searchY, l.searchW, SEARCH_H);
        Builder.rectangle()
                .size(new SizeState(l.searchW, SEARCH_H))
                .radius(new QuadRadiusState(5f))
                .color(new QuadColorState(searchFocused ? new Color(accent).darker().getRGB()
                        : (hovered ? 0x22FFFFFF : 0x14FFFFFF)))
                .build().render(mx, l.searchX, l.searchY);

        String shown = searchText.isEmpty() && !searchFocused ? "Search..." : searchText;
        Builder.text()
                .text(trimToWidth(shown, l.searchW - 16f, 10f))
                .font(BIKO_FONT.get())
                .size(10f)
                .color(searchText.isEmpty() && !searchFocused ? textS : textP)
                .thickness(0.06f)
                .build().render(mx, l.searchX + 8f, l.searchY + 6f);
    }

    private void renderTabs(Matrix4f mx, Layout l, int mouseX, int mouseY, int accent, int textP, int textS) {
        float gap = 4f;
        float tabW = (l.tabsW - gap * (Section.values().length - 1)) / Section.values().length;
        float tx = l.tabsX;
        for (Section section : Section.values()) {
            boolean selected = selectedSection == section;
            boolean hovered = HoverUtil.isHovered(mouseX, mouseY, tx, l.tabsY, tabW, TAB_H);
            Builder.rectangle()
                    .size(new SizeState(tabW, TAB_H))
                    .radius(new QuadRadiusState(5f)).smoothness(1f)
                    .color(new QuadColorState(selected ? applyAlpha(new Color(accent), 0.35f).getRGB()
                            : (hovered ? 0x1EFFFFFF : 0x0DFFFFFF)))
                    .build().render(mx, tx, l.tabsY);

            String text = trimToWidth(section.title, tabW - 6f, 10f);
            float tw = BIKO_FONT.get().getWidth(text, 10f);
            Builder.text()
                    .text(text)
                    .font(BIKO_FONT.get())
                    .size(10f)
                    .color(selected ? textP : textS)
                    .thickness(selected ? 0.07f : 0.05f)
                    .build().render(mx, tx + (tabW - tw) / 2f, l.tabsY + 6f);

            tx += tabW + gap;
        }
    }

    private void renderList(DrawContext context, Matrix4f mx, Layout l, int mouseX, int mouseY, int accent, int textP, int textS) {
        float contentH = visibleModels.size() * (ROW_H + ROW_GAP);
        float maxScroll = Math.max(0f, contentH - l.listH);
        listScroll = MathHelper.clamp(listScroll, -maxScroll, 0f);
        smoothedListScroll += (listScroll - smoothedListScroll) * 0.2f;

        context.enableScissor((int) l.listX, (int) l.listY, (int) (l.listX + l.listW), (int) (l.listY + l.listH));

        float ry = l.listY + smoothedListScroll;
        for (ModelEntry model : visibleModels) {
            if (ry + ROW_H >= l.listY && ry <= l.listY + l.listH) {
                renderRow(context, mx, model, l.listX, ry, l.listW, mouseX, mouseY, accent, textP, textS);
            }
            ry += ROW_H + ROW_GAP;
        }

        if (visibleModels.isEmpty()) {
            Builder.text().text("Nothing found...").font(BIKO_FONT.get()).size(10f)
                    .color(textS).thickness(0.05f).build().render(mx, l.listX + 8f, l.listY + 8f);
        }

        context.disableScissor();
    }

    private void renderRow(DrawContext context, Matrix4f mx, ModelEntry model, float rx, float ry, float rw,
                           int mouseX, int mouseY, int accent, int textP, int textS) {
        boolean hovered = HoverUtil.isHovered(mouseX, mouseY, rx, ry, rw, ROW_H);
        boolean selected = model.key().equals(selectedKey);

        Builder.rectangle()
                .size(new SizeState(rw, ROW_H))
                .radius(new QuadRadiusState(6f)).smoothness(1f)
                .color(new QuadColorState(selected ? applyAlpha(new Color(accent), 0.30f).getRGB()
                        : (hovered ? 0x1EFFFFFF : 0x0DFFFFFF)))
                .build().render(mx, rx, ry);

        float imgSize = ROW_H - 10f;
        float ix = rx + 5f;
        float iy = ry + 5f;
        Identifier texture = getOrRequestPreviewTexture(model);
        if (texture != null) {
            var tex = mc.getTextureManager().getTexture(texture);
            Builder.texture()
                    .size(new SizeState(imgSize, imgSize))
                    .radius(new QuadRadiusState(4f))
                    .color(QuadColorState.WHITE)
                    .texture(0f, 0f, 1f, 1f, tex)
                    .build().render(mx, ix, iy);
        } else {
            Builder.blur()
                    .size(new SizeState(imgSize, imgSize))
                    .radius(new QuadRadiusState(4f)).smoothness(1f).blurRadius(10f)
                    .color(new QuadColorState(0x22FFFFFF))
                    .build().render(mx, ix, iy);
        }

        float textX = ix + imgSize + 8f;
        boolean favorite = favoriteKeys.contains(model.key());
        String star = favorite ? "★" : "☆";
        float starW = ICON_FONT.get().getWidth(star, 10f);
        Builder.text().text(star).font(ICON_FONT.get()).size(10f)
                .color(favorite ? accent : textS).thickness(0.05f)
                .build().render(mx, rx + rw - 8f - starW, ry + 4f);

        Builder.text()
                .text(trimToWidth(model.name(), rw - (textX - rx) - 20f, 11f))
                .font(BIKO_FONT.get()).size(11f).color(textP).thickness(0.06f)
                .build().render(mx, textX, ry + 10f);

        String sub = model.key().equals(appliedKey) ? "Applied" : model.section().title;
        Builder.text()
                .text(trimToWidth(sub, rw - (textX - rx) - 10f, 9f))
                .font(BIKO_FONT.get()).size(9f)
                .color(model.key().equals(appliedKey) ? new Color(130, 210, 170).getRGB() : textS)
                .thickness(0.05f)
                .build().render(mx, textX, ry + 27f);
    }

    private void renderPreview(DrawContext context, Matrix4f mx, Layout l, int mouseX, int mouseY, int accent, int textP, int textS) {
        Builder.rectangle()
                .size(new SizeState(l.previewW, l.previewH))
                .radius(new QuadRadiusState(6f)).smoothness(1f)
                .color(new QuadColorState(0x18000000))
                .build().render(mx, l.previewX, l.previewY);

        Builder.text().text("preview").font(BIKO_FONT.get()).size(10f)
                .color(textP).thickness(0.06f).build().render(mx, l.previewX + 8f, l.previewY + 6f);
        Builder.text().text(trimToWidth(selectedName, l.previewW - 16f, 9f)).font(BIKO_FONT.get()).size(9f)
                .color(textS).thickness(0.05f).build().render(mx, l.previewX + 8f, l.previewY + 19f);

        float areaX = l.previewX + 4f;
        float areaY = l.previewY + 30f;
        float areaW = l.previewW - 8f;
        float areaH = l.previewH - 30f - ACTION_H * 2f - 12f;
        render3DArea(context, mx, areaX, areaY, areaW, areaH, textS);

        float gap = 4f;
        float topY = l.previewY + l.previewH - ACTION_H * 2f - gap;
        actionButton(mx, l.previewX + 4f, topY, areaW, installing ? "Applying..." : "Apply", accent, mouseX, mouseY);
        float bottomY = topY + ACTION_H + gap;
        float half = (areaW - gap) / 2f;
        actionButton(mx, l.previewX + 4f, bottomY, half, "Unequip", 0x33FFFFFF, mouseX, mouseY);
        actionButton(mx, l.previewX + 4f + half + gap, bottomY, half, "Un equip everything", 0x992A1B1B, mouseX, mouseY);
    }

    private void actionButton(Matrix4f mx, float bx, float by, float bw, String label, int fill, int mouseX, int mouseY) {
        boolean hover = HoverUtil.isHovered(mouseX, mouseY, bx, by, bw, ACTION_H);
        Builder.rectangle()
                .size(new SizeState(bw, ACTION_H))
                .radius(new QuadRadiusState(5f)).smoothness(1f)
                .color(new QuadColorState(hover ? blendWhite(fill, 0.10f) : fill))
                .build().render(mx, bx, by);
        float tw = BIKO_FONT.get().getWidth(label, 10f);
        Builder.text().text(label).font(BIKO_FONT.get()).size(10f)
                .color(0xFFFFFFFF).thickness(0.06f)
                .build().render(mx, bx + (bw - tw) / 2f, by + 5f);
    }

    private void render3DArea(DrawContext context, Matrix4f mx, float px, float py, float pw, float ph, int textS) {
        Builder.rectangle()
                .size(new SizeState(pw, ph))
                .radius(new QuadRadiusState(5f))
                .color(new QuadColorState(0x30000000))
                .build().render(mx, px, py);

        if (mc.player != null) {
            try {
                int left = (int) (px + 4f);
                int top = (int) (py + 4f);
                int right = (int) (px + pw - 4f);
                int bottom = (int) (py + ph - 4f);
                int size = (int) (MathHelper.clamp(Math.min(pw, ph) * 0.6f, 32f, 68f) * previewZoom);
                int cx = (int) ((left + right) * 0.5f - Math.tan(Math.toRadians(previewYaw)) * 30f);
                int cy = (int) ((top + bottom) * 0.5f - Math.tan(Math.toRadians(previewPitch)) * 30f);

                context.enableScissor((int) px, (int) py, (int) (px + pw), (int) (py + ph));
                InventoryScreen.drawEntity(context, left, top, right, bottom, size, 0.0625f, cx, cy, mc.player);
                context.disableScissor();
            } catch (Throwable ignored) {
                drawCentered(mx, "Preview error", px, py, pw, ph, textS);
            }
        } else {
            drawCentered(mx, "Choose model", px, py, pw, ph, textS);
        }
    }

    private void drawCentered(Matrix4f mx, String text, float px, float py, float pw, float ph, int color) {
        float tw = BIKO_FONT.get().getWidth(text, 10f);
        Builder.text().text(text).font(BIKO_FONT.get()).size(10f)
                .color(color).thickness(0.05f)
                .build().render(mx, px + (pw - tw) / 2f, py + ph / 2f - 5f);
    }

    private void renderStatus(Matrix4f mx, Layout l, int textS) {
        Builder.rectangle()
                .size(new SizeState(l.statusW, STATUS_H))
                .radius(new QuadRadiusState(4f))
                .color(new QuadColorState(0x14000000))
                .build().render(mx, l.statusX, l.statusY);
        Builder.text()
                .text(trimToWidth(status, l.statusW - 16f, 9f))
                .font(BIKO_FONT.get()).size(9f).color(textS).thickness(0.05f)
                .build().render(mx, l.statusX + 8f, l.statusY + 4f);
    }

    

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        Layout l = layout();

        if (button == 0 && HoverUtil.isHovered(mouseX, mouseY, l.searchX, l.searchY, l.searchW, SEARCH_H)) {
            searchFocused = true;
            return true;
        }

        if (button == 0 && HoverUtil.isHovered(mouseX, mouseY, l.tabsX, l.tabsY, l.tabsW, TAB_H)) {
            float gap = 4f;
            float tabW = (l.tabsW - gap * (Section.values().length - 1)) / Section.values().length;
            float tx = l.tabsX;
            for (Section section : Section.values()) {
                if (HoverUtil.isHovered(mouseX, mouseY, tx, l.tabsY, tabW, TAB_H)) {
                    selectedSection = section;
                    listScroll = 0f;
                    searchFocused = false;
                    return true;
                }
                tx += tabW + gap;
            }
        }

        ModelEntry clicked = findRowAt(mouseX, mouseY, l);
        if (clicked != null) {
            if (button == 0 && isFavoriteHit(mouseX, mouseY, clicked, l)) {
                toggleFavorite(clicked.key());
                return true;
            }
            if (button == 0 && !installing) {
                applyModel(clicked);
            }
            return true;
        }

        float gap = 4f;
        float topY = l.previewY + l.previewH - ACTION_H * 2f - gap;
        if (button == 0 && HoverUtil.isHovered(mouseX, mouseY, l.previewX + 4f, topY, l.previewW - 8f, ACTION_H)) {
            ModelEntry sel = findModelByKey(selectedKey);
            if (sel == null) status = "Firstly choose model";
            else applyModel(sel);
            return true;
        }
        float bottomY = topY + ACTION_H + gap;
        float half = (l.previewW - 8f - gap) / 2f;
        if (button == 0 && HoverUtil.isHovered(mouseX, mouseY, l.previewX + 4f, bottomY, half, ACTION_H)) {
            removeCurrentSelection();
            return true;
        }
        if (button == 0 && HoverUtil.isHovered(mouseX, mouseY, l.previewX + 4f + half + gap, bottomY, half, ACTION_H)) {
            removeAllSelections();
            return true;
        }

        float areaY = l.previewY + 30f;
        float areaH = l.previewH - 30f - ACTION_H * 2f - 12f;
        if (button == 0 && HoverUtil.isHovered(mouseX, mouseY, l.previewX + 4f, areaY, l.previewW - 8f, areaH)) {
            previewDragging = true;
            return true;
        }

        searchFocused = false;
        return super.mouseClicked(mouseX, mouseY, button);
    }

    private ModelEntry findRowAt(double mouseX, double mouseY, Layout l) {
        if (!HoverUtil.isHovered(mouseX, mouseY, l.listX, l.listY, l.listW, l.listH)) return null;
        float ry = l.listY + smoothedListScroll;
        for (ModelEntry model : visibleModels) {
            if (HoverUtil.isHovered(mouseX, mouseY, l.listX, ry, l.listW, ROW_H)) return model;
            ry += ROW_H + ROW_GAP;
        }
        return null;
    }

    private boolean isFavoriteHit(double mouseX, double mouseY, ModelEntry target, Layout l) {
        float ry = l.listY + smoothedListScroll;
        for (ModelEntry model : visibleModels) {
            if (model == target) {
                return HoverUtil.isHovered(mouseX, mouseY, l.listX + l.listW - 30f, ry, 26f, ROW_H);
            }
            ry += ROW_H + ROW_GAP;
        }
        return false;
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double deltaX, double deltaY) {
        if (previewDragging && button == 0) {
            previewYaw = MathHelper.clamp(previewYaw - (float) deltaX * 1.8f, -72f, 72f);
            previewPitch = MathHelper.clamp(previewPitch + (float) deltaY * 1.2f, -24f, 28f);
            return true;
        }
        return super.mouseDragged(mouseX, mouseY, button, deltaX, deltaY);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (button == 0 && previewDragging) {
            previewDragging = false;
            return true;
        }
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double amount) {
        Layout l = layout();
        if (HoverUtil.isHovered(mouseX, mouseY, l.listX, l.listY, l.listW, l.listH)) {
            listScroll += (float) amount * 24f;
            return true;
        }
        float areaY = l.previewY + 30f;
        float areaH = l.previewH - 30f - ACTION_H * 2f - 12f;
        if (HoverUtil.isHovered(mouseX, mouseY, l.previewX + 4f, areaY, l.previewW - 8f, areaH)) {
            previewZoom = MathHelper.clamp(previewZoom + (float) amount * 0.08f, 0.72f, 1.35f);
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, horizontalAmount, amount);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (searchFocused) {
            if (keyCode == GLFW.GLFW_KEY_BACKSPACE && !searchText.isEmpty()) {
                searchText = searchText.substring(0, searchText.length() - 1);
                listScroll = 0f;
                return true;
            }
            if (keyCode == GLFW.GLFW_KEY_ESCAPE || keyCode == GLFW.GLFW_KEY_ENTER) {
                searchFocused = false;
                return true;
            }
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean charTyped(char chr, int modifiers) {
        if (searchFocused && !Character.isISOControl(chr) && searchText.length() < 40) {
            searchText += chr;
            listScroll = 0f;
            return true;
        }
        return super.charTyped(chr, modifiers);
    }

    

    private void toggleFavorite(String key) {
        if (!favoriteKeys.add(key)) favoriteKeys.remove(key);
    }

    private void refreshModels(boolean force) {
        long now = System.currentTimeMillis();
        if (!force && now - lastScan < 10_000L && !models.isEmpty()) return;
        lastScan = now;

        List<ModelEntry> found = new ArrayList<>();
        if (mc.runDirectory != null) {
            Path avatarsDir = mc.runDirectory.toPath().resolve("figura").resolve("avatars").normalize();
            if (Files.isDirectory(avatarsDir)) {
                try (java.util.stream.Stream<Path> stream = Files.list(avatarsDir)) {
                    stream.filter(Files::isDirectory).forEach(path -> {
                        if (!Files.exists(path.resolve("avatar.json"))) return;
                        String folder = path.getFileName().toString();
                        String name = cleanName(folder);
                        Section section = detectSection(folder, name);
                        found.add(new ModelEntry(name, folder, section));
                    });
                } catch (Throwable ignored) {
                }
            }
        }
        found.sort(Comparator.comparing(m -> m.name().toLowerCase(Locale.ROOT)));
        models.clear();
        models.addAll(found);
    }

    private void updateVisibleModels() {
        visibleModels.clear();
        String query = searchText.trim().toLowerCase(Locale.ROOT);
        for (ModelEntry model : models) {
            if (selectedSection != Section.ALL && model.section() != selectedSection) continue;
            if (!query.isEmpty() && !model.name().toLowerCase(Locale.ROOT).contains(query)
                    && !model.folder().toLowerCase(Locale.ROOT).contains(query)) continue;
            visibleModels.add(model);
        }
    }

    private ModelEntry findModelByKey(String key) {
        if (key == null || key.isBlank()) return null;
        for (ModelEntry model : models) {
            if (model.key().equals(key)) return model;
        }
        return null;
    }

    private String cleanName(String folder) {
        String name = folder.replaceAll("([a-zа-яё])([A-ZА-ЯЁ])", "$1 $2")
                .replace('_', ' ').replace('-', ' ')
                .replaceAll("\\s+", " ").trim();
        if (name.isEmpty()) return folder;
        StringBuilder sb = new StringBuilder();
        for (String word : name.split(" ")) {
            if (word.isEmpty()) continue;
            if (sb.length() > 0) sb.append(' ');
            sb.append(Character.toUpperCase(word.charAt(0))).append(word.substring(1));
        }
        return sb.toString();
    }

    private Section detectSection(String folder, String name) {
        String words = (folder + " " + name).toLowerCase(Locale.ROOT);
        if (containsAny(words, "wing", "wings", "elytra", "cape", "cloak")) return Section.WINGS;
        if (containsAny(words, "script", "lua", "bends", "shader")) return Section.SCRIPTS;
        if (containsAny(words, "sword", "blade", "katana", "bow", "weapon", "dagger", "spear")) return Section.WEAPONS;
        if (containsAny(words, "pet", "companion", "axolotl", "cat", "dog", "fox", "wolf")) return Section.PETS;
        if (containsAny(words, "hat", "cap", "crown", "helmet", "hood", "mask", "glasses", "backpack", "tail", "ears", "horn")) return Section.ACCESSORIES;
        return Section.AVATARS;
    }

    private boolean containsAny(String haystack, String... needles) {
        for (String needle : needles) {
            if (haystack.contains(needle)) return true;
        }
        return false;
    }

    private String trimToWidth(String text, float maxWidth, float size) {
        if (text == null || text.isEmpty()) return "";
        MsdfFont font = BIKO_FONT.get();
        if (font.getWidth(text, size) <= maxWidth) return text;
        String suffix = "...";
        String trimmed = text;
        while (!trimmed.isEmpty() && font.getWidth(trimmed + suffix, size) > maxWidth) {
            trimmed = trimmed.substring(0, trimmed.length() - 1);
        }
        return trimmed.isEmpty() ? suffix : trimmed + suffix;
    }

    

    private Path getFiguraAvatarPath(ModelEntry model) {
        Path root = mc.runDirectory.toPath().resolve("figura").resolve("avatars").normalize();
        Path result = root.resolve(model.folder()).normalize();
        return result.startsWith(root) ? result : root;
    }

    private Identifier getOrRequestPreviewTexture(ModelEntry model) {
        if (model == null) return null;
        Identifier cached = previewTextures.get(model.key());
        if (cached != null || !previewTextureRequests.add(model.key())) return cached;

        Path previewPath = getFiguraAvatarPath(model).resolve("avatar.png");
        if (!Files.isRegularFile(previewPath)) return null;

        CompletableFuture.runAsync(() -> loadPreviewTexture(model.key(), previewPath));
        return null;
    }

    private void loadPreviewTexture(String key, Path path) {
        try (InputStream input = Files.newInputStream(path)) {
            NativeImage source = NativeImage.read(input);

            mc.execute(() -> {
                try {
                    Identifier id = Identifier.of(
                            "cheap",
                            "figura_preview/" + Integer.toHexString(key.hashCode())
                    );

                    mc.getTextureManager().registerTexture(
                            id,
                            new NativeImageBackedTexture(source)
                    );

                    previewTextures.put(key, id);
                } catch (Throwable t) {
                    source.close();
                    t.printStackTrace();
                }
            });
        } catch (Throwable t) {
            t.printStackTrace();
        }
    }

    

    public static String getAppliedKey() {
        return appliedKey;
    }

    
    public static void reapplyAppliedModel() {
        String key = appliedKey;
        if (key == null || key.isBlank()) return;
        if (mc.runDirectory == null) return;
        String folder = key.startsWith("avatar:") ? key.substring("avatar:".length()) : key;
        Path root = mc.runDirectory.toPath().resolve("figura").resolve("avatars").normalize();
        Path target = root.resolve(folder).normalize();
        if (!target.startsWith(root) || !Files.isDirectory(target)) return;
        try {
            AvatarManager.loadLocalAvatar(target);
            Cheap.LOGGER.info("[FiguraGUI] re-applied model on world join: {}", folder);
        } catch (Exception e) {
            Cheap.LOGGER.error("[FiguraGUI] reapply failed on world join", e);
        }
    }

    private void applyModel(ModelEntry model) {
        if (installing) return;
        if (model.key().equals(appliedKey)) {
            UUID localUuid = FiguraMod.getLocalPlayerUUID();
            if (localUuid != null && AvatarManager.getLoadedAvatar(localUuid) != null) {
                status = "Already applied: " + model.name();
                return;
            }
        }

        Path target = getFiguraAvatarPath(model).normalize();
        if (!Files.isDirectory(target)) {
            status = "Folder not found: " + model.folder();
            return;
        }

        selectedKey = model.key();
        selectedName = model.name();
        pendingKey = model.key();
        pendingName = model.name();
        pendingStart = System.currentTimeMillis();
        status = "Applying: " + model.folder() + " (loading...)";
        installing = true;

        try {
            Cheap.LOGGER.info("[FiguraGUI] apply start folder={} uuid={} world={} player={}",
                    model.folder(), FiguraMod.getLocalPlayerUUID(),
                    mc.world != null, mc.player != null);
            AvatarManager.loadLocalAvatar(target);
        } catch (Exception e) {
            Cheap.LOGGER.error("[FiguraGUI] apply threw", e);
            status = "Load error: " + e.getMessage();
            e.printStackTrace();
            installing = false;
            pendingStart = 0;
        }
    }

    private void updatePendingApply() {
        if (!installing || pendingStart == 0) return;

        long elapsed = System.currentTimeMillis() - pendingStart;
        UUID localUuid = FiguraMod.getLocalPlayerUUID();
        String loadState = LocalAvatarLoader.getLoadState();
        String loadError = LocalAvatarLoader.getLoadError();

        if (localUuid == null) {
            if (elapsed > 2000L) {
                Cheap.LOGGER.info("[FiguraGUI] poll no-uuid elapsed={}ms world={} player={}",
                        elapsed, mc.world != null, mc.player != null);
                installing = false;
                pendingStart = 0;
                status = "No local player session";
            }
            return;
        }

        Avatar avatar = AvatarManager.getLoadedAvatar(localUuid);
        if (lastPollLogMs == 0 || elapsed - lastPollLogMs >= 1000L) {
            lastPollLogMs = elapsed;
            Cheap.LOGGER.info("[FiguraGUI] poll elapsed={}ms uuid={} avatar={} loaded={} scriptError={} renderer={} loadState={} loadError={} world={}",
                    elapsed, localUuid, avatar != null,
                    avatar != null && avatar.loaded,
                    avatar != null && avatar.scriptError,
                    avatar != null && avatar.renderer != null,
                    loadState, loadError, mc.world != null);
        }

        if (avatar != null && avatar.loaded) {
            installing = false;
            pendingStart = 0;
            if (avatar.scriptError) {
                String err = avatar.errorText != null ? avatar.errorText.getString() : "unknown script error";
                appliedKey = "";
                Cheap.LOGGER.info("[FiguraGUI] done script-error: {}", err);
                status = "Script error: " + truncate(err, 160);
            } else if (avatar.renderer == null) {
                appliedKey = "";
                Cheap.LOGGER.info("[FiguraGUI] done no-renderer");
                status = "Model failed to load (no renderer)";
            } else {
                appliedKey = pendingKey;
                Cheap.LOGGER.info("[FiguraGUI] done applied: {}", pendingName);
                status = "Applied: " + pendingName;
            }
            return;
        }

        if (elapsed > 10_000L) {
            installing = false;
            pendingStart = 0;
            lastPollLogMs = 0;
            Cheap.LOGGER.info("[FiguraGUI] timeout uuid={} avatar={} loadState={} loadError={}",
                    localUuid, avatar != null, loadState, loadError);
            dumpThreads(localUuid);
            if (avatar != null) diagnoseAvatarFailure(avatar);
            status = (loadError != null && !loadError.isEmpty())
                    ? "Load error: " + truncate(loadError, 160)
                    : "Load timeout";
        }
    }

    private void dumpThreads(UUID localUuid) {
        Cheap.LOGGER.info("[FiguraGUI] ---- thread dump start (current={}) ----", Thread.currentThread().getName());
        Thread.getAllStackTraces().forEach((thread, stack) -> {
            StringBuilder sb = new StringBuilder();
            for (StackTraceElement el : stack) {
                sb.append("\n    at ").append(el);
            }
            Cheap.LOGGER.info("[FiguraGUI] thread [{}] state={}{}", thread.getName(), thread.getState(), sb);
        });
        Cheap.LOGGER.info("[FiguraGUI] ---- thread dump end ----");
    }

    private static String truncate(String s, int max) {
        if (s == null) return "null";
        return s.length() <= max ? s : s.substring(0, max) + "...";
    }

    private void diagnoseAvatarFailure(org.figuramc.figura.avatar.Avatar avatar) {
        Cheap.LOGGER.info("[FiguraGUI] ---- diagnoseAvatarFailure start ----");
        try {
            Field fLoaded = org.figuramc.figura.avatar.Avatar.class.getDeclaredField("loaded");
            fLoaded.setAccessible(true);
            Field fScriptError = org.figuramc.figura.avatar.Avatar.class.getDeclaredField("scriptError");
            fScriptError.setAccessible(true);
            Field fRenderer = org.figuramc.figura.avatar.Avatar.class.getDeclaredField("renderer");
            fRenderer.setAccessible(true);
            Field fNbt = org.figuramc.figura.avatar.Avatar.class.getDeclaredField("nbt");
            fNbt.setAccessible(true);
            Cheap.LOGGER.info("[FiguraGUI] diag fields loaded={} scriptError={} renderer={} nbt={}",
                    fLoaded.get(avatar), fScriptError.get(avatar), fRenderer.get(avatar), fNbt.get(avatar));
        } catch (ReflectiveOperationException e) {
            Cheap.LOGGER.error("[FiguraGUI] diag field read failed", e);
        }

        invokePrivate(avatar, "loadCustomSounds", "loadCustomSounds");
        invokePrivate(avatar, "createLuaRuntime", "createLuaRuntime");

        try {
            Field fLoaded = org.figuramc.figura.avatar.Avatar.class.getDeclaredField("loaded");
            fLoaded.setAccessible(true);
            Field fRenderer = org.figuramc.figura.avatar.Avatar.class.getDeclaredField("renderer");
            fRenderer.setAccessible(true);
            Cheap.LOGGER.info("[FiguraGUI] diag AFTER retry loaded={} renderer={}",
                    fLoaded.get(avatar), fRenderer.get(avatar));
        } catch (ReflectiveOperationException e) {
            Cheap.LOGGER.error("[FiguraGUI] diag field re-read failed", e);
        }
        Cheap.LOGGER.info("[FiguraGUI] ---- diagnoseAvatarFailure end ----");
    }

    private void invokePrivate(Object target, String methodName, String logName) {
        try {
            Method m = target.getClass().getDeclaredMethod(methodName);
            m.setAccessible(true);
            Cheap.LOGGER.info("[FiguraGUI] diag invoking {} ...", logName);
            Object res = m.invoke(target);
            Cheap.LOGGER.info("[FiguraGUI] diag {} returned: {}", logName, res);
        } catch (Throwable t) {
            Throwable cause = t;
            if (t instanceof InvocationTargetException && t.getCause() != null) {
                cause = t.getCause();
            }
            Cheap.LOGGER.error("[FiguraGUI] diag {} THREW", logName, cause);
        }
    }

    private void removeFiguraAvatar() {
        try {
            org.figuramc.figura.avatar.AvatarManager.clearAvatars(
                    org.figuramc.figura.FiguraMod.getLocalPlayerUUID()
            );
            appliedKey = "";
            status = "Model cleared";
        } catch (Exception e) {
            status = "Error clearing: " + e.getMessage();
        }
    }

    private void removeCurrentSelection() {
        ModelEntry model = findModelByKey(!appliedKey.isBlank() ? appliedKey : selectedKey);
        if (model == null) { status = "Nothing to remove"; return; }
        removeFiguraAvatar();
        if (model.key().equals(appliedKey)) appliedKey = "";
        if (model.key().equals(selectedKey)) { selectedKey = ""; selectedName = "Not choosen"; }
        status = "Removed: " + model.name();
    }

    private void removeAllSelections() {
        removeFiguraAvatar();
        appliedKey = "";
        selectedKey = "";
        selectedName = "Not choosen";
        status = "All models removed";
    }

    

    private Color applyAlpha(Color c, float alpha) {
        return new Color(c.getRed(), c.getGreen(), c.getBlue(), Math.max(0, Math.min(255, (int) (c.getAlpha() * alpha))));
    }

    private Color darker(Color c, float factor) {
        return new Color(Math.max(0, (int) (c.getRed() * factor)), Math.max(0, (int) (c.getGreen() * factor)),
                Math.max(0, (int) (c.getBlue() * factor)), c.getAlpha());
    }

    private int blendWhite(int color, float amt) {
        int a = (color >> 24) & 0xFF;
        int r = Math.min(255, ((color >> 16) & 0xFF) + (int) (255 * amt));
        int g = Math.min(255, ((color >> 8) & 0xFF) + (int) (255 * amt));
        int b = Math.min(255, (color & 0xFF) + (int) (255 * amt));
        return (a << 24) | (r << 16) | (g << 8) | b;
    }

    

    private enum Section {
        ALL("Everything"), AVATARS("Avatars"), PETS("Pets"), ACCESSORIES("Accessories"),
        WEAPONS("Weapons"), WINGS("Wings"), SCRIPTS("Scripts");

        final String title;

        Section(String title) {
            this.title = title;
        }
    }

    private record ModelEntry(String name, String folder, Section section) {
        private String key() {
            return "avatar:" + folder.toLowerCase(Locale.ROOT);
        }
    }
}