package fun.ogi.screens;

import com.google.common.base.Supplier;
import com.google.common.base.Suppliers;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import fun.ogi.Cheap;
import fun.ogi.module.Module;
import fun.ogi.module.ModuleCategory;
import fun.ogi.module.settings.*;
import fun.ogi.module.theme.Theme;
import fun.ogi.module.theme.ThemeManager;
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
import fun.ogi.util.render.renderers.impl.BuiltBlur;
import fun.ogi.util.render.renderers.impl.BuiltText;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;
import org.joml.Matrix4f;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.opengl.GL11;

import java.awt.*;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.List;

import static fun.ogi.util.MinecraftUtil.mc;

public class DropDownGui extends Screen {
    private static final Supplier<MsdfFont> BIKO_FONT = Suppliers.memoize(() -> MsdfFont.builder().atlas("biko").data("biko").build());
    private static final Supplier<MsdfFont> ICONS_FONT = Suppliers.memoize(() -> MsdfFont.builder().atlas("icons").data("icons").build());

    private static final float CATEGORY_WIDTH = 112f;
    private static final float CATEGORY_HEIGHT = 20f;
    private static final float MODULE_HEIGHT = 16f;
    private static final float MODULE_PADDING = 3f;
    private static final float MODULE_TEXT_SIZE = 8f;
    private static final float MODULE_ROW_PAD = 5f;
    private static final float MODULE_CHECK_SLOT = 11f;
    private static final float VISIBLE_HEIGHT = 220f;
    private static final float SPACING = 4f;
    private static final float SCROLL_SPEED = 18f;
    private static final float SETTING_NAME_SIZE = 7.5f;
    private static final float SETTING_VALUE_SIZE = 7f;
    private static final float MODE_FONT_SIZE = 6.5f;
    private static final float MODE_PAD_X = 5f;
    private static final float MODE_PAD_Y = 3f;
    private static final float MODE_RADIUS = 3f;
    private static final float MODE_GAP_X = 3f;
    private static final float MODE_GAP_Y = 2f;

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final float[] savedPanelX = new float[ModuleCategory.values().length];
    private static final float[] savedPanelY = new float[ModuleCategory.values().length];
    private static final boolean[] hasSavedPos = new boolean[ModuleCategory.values().length];

    private float panelStartX;
    private float panelStartY;

    private final float[] panelX = new float[ModuleCategory.values().length];
    private final float[] panelY = new float[ModuleCategory.values().length];
    private final float[] defaultPanelX = new float[ModuleCategory.values().length];
    private final float[] defaultPanelY = new float[ModuleCategory.values().length];
    private final Animation[] collapseAnim = new Animation[ModuleCategory.values().length];
    private final boolean[] collapsed = new boolean[ModuleCategory.values().length];
    private final long[] lastHeaderClick = new long[ModuleCategory.values().length];
    private int draggingIndex = -1;
    private float dragDX;
    private float dragDY;

    private final float[] scrollOffsets;
    private final float[] targetScrolls;
    private final List<ModuleEntry> moduleEntries = new ArrayList<>();
    private Module bindingModule = null;
    private KeySetting bindingKeySetting = null;
    private StringSetting focusedStringSetting = null;
    private Module hoveredModule = null;
    private String searchText = "";
    private boolean searchFocused = false;

    private final float guiScale;
    private final Animation openAnim = new Animation();
    private boolean closing = false;

    public DropDownGui(float guiScale) {
        super(Text.literal("ClickGUI"));
        this.guiScale = guiScale;
        scrollOffsets = new float[ModuleCategory.values().length];
        targetScrolls = new float[ModuleCategory.values().length];
    }

    @Override
    protected void init() {
        openAnim.start(0f, 1f, 250, Easing.CUBIC_OUT);
        closing = false;
        moduleEntries.clear();
        ModuleCategory[] cats = ModuleCategory.values();
        int count = cats.length;
        float[] widths = new float[count];
        for (int i = 0; i < count; i++) widths[i] = computePanelWidth(i);
        float totalWidth = 0;
        for (int i = 0; i < count; i++) totalWidth += widths[i];
        totalWidth += (count - 1) * SPACING;
        float logicalWidth = this.width / guiScale;
        float logicalHeight = this.height / guiScale;
        panelStartX = (logicalWidth / 2f) - (totalWidth / 2f);
        panelStartY = (logicalHeight / 2f) - (VISIBLE_HEIGHT + CATEGORY_HEIGHT) / 2f;

        for (int i = 0; i < scrollOffsets.length; i++) {
            scrollOffsets[i] = 0;
            targetScrolls[i] = 0;
        }

        loadPanelPositions();
        float runningX = 0;
        for (int i = 0; i < cats.length; i++) {
            defaultPanelX[i] = panelStartX + runningX;
            defaultPanelY[i] = panelStartY;
            if (hasSavedPos[i]) {
                panelX[i] = savedPanelX[i];
                panelY[i] = savedPanelY[i];
            } else {
                panelX[i] = defaultPanelX[i];
                panelY[i] = defaultPanelY[i];
            }
            clampPanel(i);
            if (collapseAnim[i] == null) collapseAnim[i] = new Animation();
            collapseAnim[i].setValue(1f);
            collapsed[i] = false;
            lastHeaderClick[i] = 0;
            runningX += widths[i] + SPACING;
        }
        draggingIndex = -1;

        for (ModuleCategory cat : cats) {
            if (cat == ModuleCategory.THEMES) continue;
            List<Module> modules = Cheap.getInstance().getModuleStorage().getCategory(cat);
            for (Module module : modules) {
                moduleEntries.add(new ModuleEntry(module));
            }
        }
    }

    private ModuleEntry getEntry(Module module) {
        for (ModuleEntry entry : moduleEntries) {
            if (entry.module == module) return entry;
        }
        return null;
    }

    private Color themeColor(int argb) {
        return new Color(argb, true);
    }

    private float openAlpha = 1f;

    private Color applyAlpha(Color c, float alpha) {
        return new Color(c.getRed(), c.getGreen(), c.getBlue(),
                Math.max(0, Math.min(255, (int) (c.getAlpha() * alpha * openAlpha))));
    }

    private Color applyAlpha(int argb, float alpha) {
        Color c = new Color(argb, true);
        return new Color(c.getRed(), c.getGreen(), c.getBlue(),
                Math.max(0, Math.min(255, (int) (c.getAlpha() * alpha * openAlpha))));
    }

    private void clampPanel(int i) {
        float logicalW = this.width / guiScale;
        float logicalH = this.height / guiScale;
        panelX[i] = Math.max(0, Math.min(panelX[i], logicalW - computePanelWidth(i)));
        panelY[i] = Math.max(0, Math.min(panelY[i], logicalH - CATEGORY_HEIGHT - VISIBLE_HEIGHT));
    }

    private static void loadPanelPositions() {
        try (FileReader reader = new FileReader(new File(Cheap.getInstance().getCheapDir(), "dropdown_gui.json"))) {
            JsonObject root = JsonParser.parseReader(reader).getAsJsonObject();
            ModuleCategory[] cats = ModuleCategory.values();
            for (int i = 0; i < cats.length; i++) {
                if (root.has(cats[i].name())) {
                    JsonObject pos = root.getAsJsonObject(cats[i].name());
                    savedPanelX[i] = pos.get("x").getAsFloat();
                    savedPanelY[i] = pos.get("y").getAsFloat();
                    hasSavedPos[i] = true;
                }
            }
        } catch (Exception ignored) {
        }
    }

    private void savePanelPositions() {
        ModuleCategory[] cats = ModuleCategory.values();
        for (int i = 0; i < cats.length; i++) {
            savedPanelX[i] = panelX[i];
            savedPanelY[i] = panelY[i];
            hasSavedPos[i] = true;
        }
        JsonObject root = new JsonObject();
        for (int i = 0; i < cats.length; i++) {
            JsonObject pos = new JsonObject();
            pos.addProperty("x", panelX[i]);
            pos.addProperty("y", panelY[i]);
            root.add(cats[i].name(), pos);
        }
        try (FileWriter writer = new FileWriter(new File(Cheap.getInstance().getCheapDir(), "dropdown_gui.json"))) {
            GSON.toJson(root, writer);
        } catch (Exception ignored) {
        }
    }

    private boolean matchesQuery(String name, String desc) {
        if (searchText.isEmpty()) return true;
        String q = searchText.toLowerCase();
        return name.toLowerCase().contains(q) || (desc != null && desc.toLowerCase().contains(q));
    }

    private List<Module> visibleModules(ModuleCategory category) {
        List<Module> all = Cheap.getInstance().getModuleStorage().getCategory(category);
        List<Module> result = new ArrayList<>();
        for (Module m : all) {
            if (matchesQuery(m.getName(), m.getDesc())) result.add(m);
        }
        result.sort((a, b) -> Float.compare(
                BIKO_FONT.get().getWidth(b.getName(), MODULE_TEXT_SIZE),
                BIKO_FONT.get().getWidth(a.getName(), MODULE_TEXT_SIZE)));
        return result;
    }

    private float moduleRowWidth(Module module) {
        return BIKO_FONT.get().getWidth(module.getName(), MODULE_TEXT_SIZE) + MODULE_ROW_PAD * 2f + MODULE_CHECK_SLOT;
    }

    private float computePanelWidth(int i) {
        ModuleCategory category = ModuleCategory.values()[i];
        if (category == ModuleCategory.THEMES) {
            float maxW = 0f;
            for (Theme t : ThemeManager.getInstance().getThemes()) {
                maxW = Math.max(maxW, BIKO_FONT.get().getWidth(t.getName(), MODULE_TEXT_SIZE) + 30f);
            }
            return Math.max(90f, maxW);
        }
        float maxW = 0f;
        for (Module m : visibleModules(category)) {
            maxW = Math.max(maxW, moduleRowWidth(m));
        }
        String title = category.name();
        title = title.charAt(0) + title.substring(1).toLowerCase();
        float titleW = BIKO_FONT.get().getWidth(title, 8f) + 14f;
        return Math.max(titleW, maxW);
    }

    private void drawGradientModuleText(Matrix4f mx, String text, float size, float thickness, float x, float y, float alpha) {
        int len = text.length();
        if (len == 0) return;
        ThemeManager tm = ThemeManager.getInstance();
        int start = tm.getPrimary();
        int end = tm.isRainbow() ? start : tm.getSecondary();
        float thicknessAdvance = (thickness + 0.0f) * 0.5f * size;
        int drawnCount = 0;
        double time = System.currentTimeMillis() * 0.005;
        MsdfFont font = BIKO_FONT.get();
        for (int i = 0; i < len; i++) {
            char c = text.charAt(i);
            if (!font.getGlyphs().containsKey((int) c)) continue;
            float wave = tm.isRainbow() ? 0f : (float) ((Math.sin(time - (double) i / len * Math.PI * 2.0) + 1.0) * 0.5);
            int col = ColorProvider.interpolateColor(start, end, wave);
            float posX = x + font.getWidth(text.substring(0, i), size) + thicknessAdvance * drawnCount;
            Color grad = new Color(withAlpha(col, alpha * openAlpha));
            Builder.text().text(String.valueOf(c)).font(font).size(size).thickness(thickness)
                    .color(grad).build().render(mx, posX, y);
            drawnCount++;
        }
    }

    private int withAlpha(int color, float alpha) {
        int a = (int) (((color >> 24) & 0xFF) * Math.max(0, Math.min(1, alpha)));
        return (a << 24) | (color & 0x00FFFFFF);
    }

    private Color darkenColor(Color c, float f) {
        return new Color(
                Math.max(0, (int) (c.getRed() * f)),
                Math.max(0, (int) (c.getGreen() * f)),
                Math.max(0, (int) (c.getBlue() * f)),
                c.getAlpha());
    }

    private boolean isInSearchBar(double mouseX, double mouseY) {
        float barW = 140f;
        float barH = 20f;
        float logicalWidth = this.width / guiScale;
        float barX = (logicalWidth / 2f) - (barW / 2f);
        float barY = panelStartY + VISIBLE_HEIGHT + CATEGORY_HEIGHT + 10f;
        return mouseX >= barX && mouseX <= barX + barW && mouseY >= barY && mouseY <= barY + barH;
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        openAnim.update();
        if (closing && openAnim.isFinished()) {
            super.close();
            return;
        }
        float openValue = openAnim.getValue();
        if (openValue <= 0.001f) return;
        openAlpha = openValue;

        ThemeManager tm = ThemeManager.getInstance();
        int bg = tm.getPalette().getBackground();
        int surface = tm.getPalette().getSurface();
        int textPrimary = Color.WHITE.getRGB();
        int textSecondary = new Color(255, 255, 255, 160).getRGB();
        int textAccent = Color.WHITE.getRGB();
        int accent = Color.WHITE.getRGB();
        int blurCol = tm.getPalette().getHudBackground();

        int smX = (int) (mouseX / guiScale);
        int smY = (int) (mouseY / guiScale);

        context.getMatrices().push();
        float scaleAnim = guiScale * (0.85f + 0.15f * openValue);
        float centerX = this.width / 2f;
        float centerY = this.height / 2f;
        context.getMatrices().translate(centerX, centerY, 0);
        context.getMatrices().scale(scaleAnim, scaleAnim, 1);
        context.getMatrices().translate(-centerX, -centerY, 0);
        Matrix4f mx = context.getMatrices().peek().getPositionMatrix();
        ModuleCategory[] cats = ModuleCategory.values();

        hoveredModule = null;

        for (ModuleEntry entry : moduleEntries) {
            entry.toggleAnim.update();
            entry.expandAnim.update();
        }

        for (int i = 0; i < cats.length; i++) {
            ModuleCategory category = cats[i];
            collapseAnim[i].update();
            float colX = panelX[i];
            float colY = panelY[i];
            float headerH = CATEGORY_HEIGHT;
            float collapse = collapseAnim[i].getValue();
            float contentH = VISIBLE_HEIGHT * collapse;
            float panelW = computePanelWidth(i);
            float totalH = headerH + contentH;

            List<Module> modules = visibleModules(category);
            updateScroll(i, modules);

            ShadowUtil.gradient(new Color(ThemeManager.getInstance().getPrimary()),
                    new Color(ThemeManager.getInstance().getSecondary()), panelW, totalH + 2f,
                    new QuadRadiusState(1f)).render(mx, colX, colY, 0);

            Builder.rectangle()
                    .size(new SizeState(panelW, totalH + 2f))
                    .radius(new QuadRadiusState(1f))
                    .color(new QuadColorState(new Color(14, 14, 18, 220)))
                    .build().render(mx, colX, colY);

            double hTime = System.currentTimeMillis() * 0.005;
            int startC = ThemeManager.getInstance().getPrimary();
            int endC = ThemeManager.getInstance().isRainbow() ? startC : ThemeManager.getInstance().getSecondary();
            float wTL = (float) ((Math.sin(hTime) + 1.0) * 0.5);
            float wTR = (float) ((Math.sin(hTime - Math.PI * 0.7) + 1.0) * 0.5);
            float wBR = (float) ((Math.sin(hTime - Math.PI * 1.4) + 1.0) * 0.5);
            float wBL = (float) ((Math.sin(hTime - Math.PI * 2.1) + 1.0) * 0.5);
            Color cTL = new Color(ColorProvider.interpolateColor(startC, endC, wTL));
            Color cTR = new Color(ColorProvider.interpolateColor(startC, endC, wTR));
            Color cBR = new Color(ColorProvider.interpolateColor(startC, endC, wBR));
            Color cBL = new Color(ColorProvider.interpolateColor(startC, endC, wBL));
            Builder.rectangle()
                    .size(new SizeState(panelW, headerH))
                    .radius(new QuadRadiusState(1, 0, 0, 1))
                    .color(new QuadColorState(cTL, cTR, cBR, cBL))
                    .build().render(mx, colX, colY);

            Builder.rectangle()
                    .size(new SizeState(panelW - 8, 1))
                    .radius(new QuadRadiusState(0))
                    .color(new QuadColorState(applyAlpha(textPrimary, 0.12f)))
                    .build().render(mx, colX + 4, colY + headerH);

            String catName = category.name();
            catName = catName.charAt(0) + catName.substring(1).toLowerCase();
            float titleW = BIKO_FONT.get().getWidth(catName, 8f);
            BuiltText headerText = Builder.text()
                    .text(catName)
                    .color(applyAlpha(accent, 1f))
                    .size(8f)
                    .font(BIKO_FONT.get())
                    .thickness(0.05f)
                    .build();
            headerText.render(mx, colX + (panelW - titleW) / 2f, colY + 6f);

            GL11.glEnable(GL11.GL_SCISSOR_TEST);
            int sf = (int) mc.getWindow().getScaleFactor();
            float clipX = colX;
            float clipY = colY + headerH;
            float clipW = panelW;
            float clipH = contentH;
            int gx = (int) (clipX * guiScale * sf);
            int gy = (int) (mc.getWindow().getHeight() - (clipY + clipH) * guiScale * sf);
            int gw = (int) (clipW * guiScale * sf);
            int gh = (int) (clipH * guiScale * sf);
            GL11.glScissor(gx, Math.max(0, gy), gw, gh);

            float renderY = clipY - scrollOffsets[i];

            if (category == ModuleCategory.THEMES) {
                List<Theme> themes = ThemeManager.getInstance().getThemes();
                Theme current = ThemeManager.getInstance().getCurrentTheme();
                float itemH = 20f;
                for (Theme theme : themes) {
                    if (renderY + itemH > clipY - 2 && renderY < clipY + clipH + 2) {
                        boolean hov = HoverUtil.isHovered(smX, smY, colX + MODULE_PADDING, renderY,
                                panelW - MODULE_PADDING * 2, itemH);
                        boolean isActive = theme == current;

                        Color rowBg = hov ? new Color(40, 40, 50, 200) : new Color(30, 30, 38, 180);
                        Color acC = new Color(accent, true);
                        if (isActive) rowBg = new Color(acC.getRed(), acC.getGreen(), acC.getBlue(), 40);
                        Builder.rectangle()
                                .size(new SizeState(panelW - MODULE_PADDING * 2, itemH))
                                .radius(new QuadRadiusState(3))
                                .color(new QuadColorState(rowBg))
                                .build().render(mx, colX + MODULE_PADDING, renderY);

                        int themePrimary = theme.getPalette().getPrimary();
                        Builder.rectangle()
                                .size(new SizeState(3f, itemH - 6f))
                                .radius(new QuadRadiusState(1.5f))
                                .color(new QuadColorState(applyAlpha(themePrimary, 1f)))
                                .build().render(mx, colX + MODULE_PADDING + 5, renderY + 3);

                        Color nameC = isActive ? new Color(accent, true) : new Color(textPrimary, true);
                        BuiltText themeName = Builder.text()
                                .text(theme.getName())
                                .color(applyAlpha(nameC, 1f))
                                .size(8f)
                                .font(BIKO_FONT.get())
                                .thickness(isActive ? 0.04f : 0.05f)
                                .build();
                        themeName.render(mx, colX + MODULE_PADDING + 14, renderY + 5.5f);

                        if (theme.isCustom()) {
                            BuiltText customTag = Builder.text()
                                    .text("C")
                                    .color(applyAlpha(textSecondary, 0.6f))
                                    .size(6f)
                                    .font(BIKO_FONT.get())
                                    .thickness(0.05f)
                                    .build();
                            customTag.render(mx, colX + panelW - MODULE_PADDING - 14, renderY + 6.5f);
                        }
                    }
                    renderY += itemH;
                }
            } else {
                for (Module module : modules) {
                    ModuleEntry entry = getEntry(module);
                    float moduleH = entry != null ? entry.getHeight() : MODULE_HEIGHT;

                    if (renderY + moduleH > clipY - 2 && renderY < clipY + clipH + 2) {
                        drawModule(context, mx, module, colX + MODULE_PADDING, renderY,
                                panelW - MODULE_PADDING * 2, smX, smY,
                                bg, surface, textPrimary, textSecondary, textAccent, accent);
                    }
                    renderY += moduleH;
                }
            }

            GL11.glDisable(GL11.GL_SCISSOR_TEST);

            float totalContentH = 0;
            if (category == ModuleCategory.THEMES) {
                totalContentH = ThemeManager.getInstance().getThemes().size() * 20f;
            } else {
                for (Module module : modules) {
                    ModuleEntry entry = getEntry(module);
                    totalContentH += entry != null ? entry.getHeight() : MODULE_HEIGHT;
                }
            }
            if (collapse > 0.05f && totalContentH > VISIBLE_HEIGHT) {
                float scrollRatio = totalContentH > VISIBLE_HEIGHT ? scrollOffsets[i] / (totalContentH - VISIBLE_HEIGHT) : 0;
                float sbH = Math.max(20, (VISIBLE_HEIGHT / totalContentH) * VISIBLE_HEIGHT);
                float sbY = clipY + (VISIBLE_HEIGHT - sbH) * scrollRatio;
                Builder.rectangle()
                        .size(new SizeState(1.5f, sbH - 4))
                        .radius(new QuadRadiusState(1f))
                        .color(new QuadColorState(applyAlpha(accent, 0.5f)))
                        .build().render(mx, colX + panelW - 3, sbY + 2);
            }
        }

        if (hoveredModule != null && !hoveredModule.getDesc().isEmpty()) {
            String desc = hoveredModule.getDesc();
            float descSize = 8f;
            float descW = BIKO_FONT.get().getWidth(desc, descSize);
            float descH = 18f;
            float logicalWidth = this.width / guiScale;
            float descX = (logicalWidth / 2f) - (descW / 2f) - 8f;
            float descY = panelStartY - descH - 6f;


            Builder.rectangle()
                    .size(new SizeState(descW + 16f, descH))
                    .radius(new QuadRadiusState(3))
                    .color(new QuadColorState(new Color(14, 14, 18, 220)))
                    .build().render(mx, descX, descY);


            BuiltText descText = Builder.text()
                    .text(desc)
                    .color(applyAlpha(textPrimary, 0.75f))
                    .size(descSize)
                    .font(BIKO_FONT.get())
                    .thickness(0.05f)
                    .build();
            descText.render(mx, descX + 8f, descY + 4.5f);
        }

        renderSearchBar(mx, smX, smY, textPrimary, accent, blurCol);
        context.getMatrices().pop();
    }

    private void drawModule(DrawContext context, Matrix4f mx, Module module, float x, float y, float width,
                            int mouseX, int mouseY, int bg, int surface, int textPrimary, int textSecondary,
                            int textAccent, int accent) {
        ModuleEntry entry = getEntry(module);
        if (entry == null) return;

        float tv = entry.toggleAnim.getValue();
        float rowW = width;
        float rowX = x;
        boolean hovered = HoverUtil.isHovered(mouseX, mouseY, rowX, y, rowW, MODULE_HEIGHT);
        boolean isBinding = bindingModule == module;

        if (hovered && !module.getDesc().isEmpty()) {
            hoveredModule = module;
        }

        Builder.rectangle()
                .size(new SizeState(rowW, MODULE_HEIGHT))
                .radius(new QuadRadiusState(0))
                .color(new QuadColorState(new Color(20, 20, 20, 255)))
                .build().render(mx, rowX, y);

        if (tv > 0.01f) {
            int pCol = ThemeManager.getInstance().getPrimary();
            int sCol = ThemeManager.getInstance().isRainbow() ? pCol : ThemeManager.getInstance().getSecondary();
            Color gs = new Color(pCol);
            Color ge = new Color(sCol);
            Builder.rectangle()
                    .size(new SizeState(rowW, MODULE_HEIGHT))
                    .radius(new QuadRadiusState(0))
                    .color(new QuadColorState(applyAlpha(gs, tv), applyAlpha(ge, tv), applyAlpha(gs, tv), applyAlpha(ge, tv)))
                    .build().render(mx, rowX, y);
        }

        if (hovered) {
            Builder.rectangle()
                    .size(new SizeState(rowW, MODULE_HEIGHT))
                    .radius(new QuadRadiusState(0))
                    .color(new QuadColorState(new Color(255, 255, 255, 20)))
                    .build().render(mx, rowX, y);
        }

        String display = isBinding ? "Key: ..." : module.getName();
        float textW = BIKO_FONT.get().getWidth(display, MODULE_TEXT_SIZE);
        float textX = x + (width - textW) / 2f;
        float textY = y + (MODULE_HEIGHT - MODULE_TEXT_SIZE) / 2f;

        if (isBinding) {
            Builder.text().text(display).font(BIKO_FONT.get()).size(MODULE_TEXT_SIZE)
                    .color(applyAlpha(Color.WHITE, 1f)).thickness(0.06f).build().render(mx, textX, textY);
        } else if (module.isEnabled()) {
            Builder.text().text(display).font(BIKO_FONT.get()).size(MODULE_TEXT_SIZE)
                    .color(applyAlpha(Color.WHITE, 1f)).thickness(0.06f).build().render(mx, textX, textY);
        } else {
            drawGradientModuleText(mx, display, MODULE_TEXT_SIZE, 0.06f, textX, textY, 0.45f);
        }

        if (entry.expandAnim.getValue() > 0.01f && !entry.settingComponents.isEmpty()) {
            float ev = entry.expandAnim.getValue();
            float settingY = y + MODULE_HEIGHT + 2;
            for (SettingComponent sc : entry.settingComponents) {
                if (!sc.setting.visible()) continue;
                float sh = sc.getHeight();
                sc.x = x + 4;
                sc.y = settingY;
                sc.width = width - 8;
                sc.draw(context, mx, mouseX, mouseY, ev, bg, surface, textPrimary, textSecondary, textAccent, accent);
                settingY += sh + 2;
            }
        }
    }

    private void renderSearchBar(Matrix4f mx, int mouseX, int mouseY, int textPrimary, int accent, int blurCol) {
        float barW = 140f;
        float barH = 20f;
        float logicalWidth = this.width / guiScale;
        float barX = (logicalWidth / 2f) - (barW / 2f);
        float barY = panelStartY + VISIBLE_HEIGHT + CATEGORY_HEIGHT + 10f;

        Builder.rectangle()
                .size(new SizeState(barW, barH))
                .radius(new QuadRadiusState(2))
                .color(new QuadColorState(new Color(14, 14, 18, 220)))
                .build().render(mx, barX, barY);


        boolean searchHovered = HoverUtil.isHovered(mouseX, mouseY, barX, barY, barW, barH);
        if (searchHovered || searchFocused) {
            Builder.rectangle()
                    .size(new SizeState(barW, barH))
                    .radius(new QuadRadiusState(2))
                    .color(new QuadColorState(applyAlpha(textPrimary, searchFocused ? 0.12f : 0.08f)))
                    .build().render(mx, barX, barY);
        }

        if (searchFocused) {
            Builder.rectangle()
                    .size(new SizeState(barW - 16, 1))
                    .radius(new QuadRadiusState(0))
                    .color(new QuadColorState(applyAlpha(textPrimary, 0.7f)))
                    .build().render(mx, barX + 8, barY + barH - 1);
        }

        String display = searchText.isEmpty() ? (searchFocused ? "" : "Search...") : searchText;
        Color txtColor = searchText.isEmpty() && !searchFocused
                ? applyAlpha(textPrimary, 0.4f)
                : applyAlpha(textPrimary, 0.9f);
        Builder.text()
                .text(display)
                .color(txtColor)
                .size(8f)
                .font(BIKO_FONT.get())
                .thickness(0.05f)
                .build().render(mx, barX + 10, barY + 6);

        if (searchFocused && (System.currentTimeMillis() / 500) % 2 == 0) {
            float tw = BIKO_FONT.get().getWidth(searchText, 8f);
            Builder.rectangle()
                    .size(new SizeState(1f, 8f))
                    .radius(new QuadRadiusState(0))
                    .color(new QuadColorState(applyAlpha(textPrimary, 0.8f)))
                    .build().render(mx, barX + 10 + tw, barY + 6);
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        double smX = mouseX / guiScale;
        double smY = mouseY / guiScale;
        if (isInSearchBar(smX, smY)) {
            searchFocused = true;
            return true;
        }
        searchFocused = false;
        if (bindingKeySetting != null) {
            if (button >= 0 && button <= GLFW.GLFW_MOUSE_BUTTON_LAST) {
                bindingKeySetting.setKey(button);
            }
            bindingKeySetting = null;
            return true;
        }

        if (bindingModule != null) {
            boolean hit = false;
            for (int ci = 0; ci < ModuleCategory.values().length; ci++) {
                float colX = panelX[ci];
                float colY = panelY[ci];
                float headerH = CATEGORY_HEIGHT;
                float contentH = VISIBLE_HEIGHT * collapseAnim[ci].getValue();
                if (contentH < 1f) continue;
                float contentY = colY + headerH;
                float panelW = computePanelWidth(ci);

                if (!HoverUtil.isHovered(smX, smY, colX, colY, panelW, headerH + contentH)) continue;
                if (smY < contentY || smY > contentY + contentH) continue;

                List<Module> modules = visibleModules(ModuleCategory.values()[ci]);
                float renderY = contentY - scrollOffsets[ci];
                for (Module module : modules) {
                    ModuleEntry entry = getEntry(module);
                    float moduleH = entry != null ? entry.getHeight() : MODULE_HEIGHT;
                    float rowW = panelW - MODULE_PADDING * 2;
                    float rowX = colX + MODULE_PADDING;
                    if (HoverUtil.isHovered(smX, smY, rowX, renderY, rowW, MODULE_HEIGHT)) {
                        hit = true;
                        break;
                    }
                    renderY += moduleH;
                }
                if (hit) break;
            }
            if (!hit) bindingModule = null;
            return hit;
        }

        ModuleCategory[] cats = ModuleCategory.values();
        for (int i = 0; i < cats.length; i++) {
            float colX = panelX[i];
            float colY = panelY[i];

            if (!HoverUtil.isHovered(smX, smY, colX, colY, computePanelWidth(i), CATEGORY_HEIGHT)) continue;

            if (button == 1) {
                collapsed[i] = !collapsed[i];
                collapseAnim[i].start(collapseAnim[i].getValue(), collapsed[i] ? 0f : 1f, 300, Easing.CUBIC_IN_OUT);
                return true;
            }

            if (button == 0) {
                long now = System.currentTimeMillis();
                if (now - lastHeaderClick[i] < 250L) {
                    panelX[i] = defaultPanelX[i];
                    panelY[i] = defaultPanelY[i];
                    draggingIndex = -1;
                    savePanelPositions();
                    lastHeaderClick[i] = 0;
                    return true;
                }
                lastHeaderClick[i] = now;
                draggingIndex = i;
                dragDX = (float) (smX - colX);
                dragDY = (float) (smY - colY);
                return true;
            }
            return true;
        }

        for (int i = 0; i < cats.length; i++) {
            float colX = panelX[i];
            float colY = panelY[i];
            float headerH = CATEGORY_HEIGHT;
            float contentH = VISIBLE_HEIGHT * collapseAnim[i].getValue();
            if (contentH < 1f) continue;
            float contentY = colY + headerH;
            float panelW = computePanelWidth(i);

            if (!HoverUtil.isHovered(smX, smY, colX, colY, panelW, headerH + contentH)) continue;
            if (smY < contentY || smY > contentY + contentH) continue;

            if (cats[i] == ModuleCategory.THEMES) {
                List<Theme> themes = ThemeManager.getInstance().getThemes();
                float renderY = contentY - scrollOffsets[i];
                float itemH = 20f;
                for (Theme theme : themes) {
                    if (HoverUtil.isHovered(smX, smY, colX + MODULE_PADDING, renderY,
                            panelW - MODULE_PADDING * 2, itemH)) {
                        if (button == 0) {
                            ThemeManager.getInstance().setTheme(theme);
                            return true;
                        }
                    }
                    renderY += itemH;
                }
            } else {
                List<Module> modules = visibleModules(cats[i]);
                float renderY = contentY - scrollOffsets[i];

                for (Module module : modules) {
                    ModuleEntry entry = getEntry(module);
                    float moduleH = entry != null ? entry.getHeight() : MODULE_HEIGHT;
                    float rowW = panelW - MODULE_PADDING * 2;
                    float rowX = colX + MODULE_PADDING;

                    if (HoverUtil.isHovered(smX, smY, rowX, renderY, rowW, MODULE_HEIGHT)) {
                        if (button == 0) {
                            module.toggle();
                            entry.toggleAnim.start(entry.toggleAnim.getValue(),
                                    module.isEnabled() ? 1f : 0f, 180, Easing.CUBIC_OUT);
                            return true;
                        } else if (button == 1) {
                            entry.expanded = !entry.expanded;
                            entry.expandAnim.start(entry.expandAnim.getValue(),
                                    entry.expanded ? 1f : 0f, 250, Easing.CUBIC_IN_OUT);
                            return true;
                        } else if (button == 2) {
                            bindingModule = bindingModule == module ? null : module;
                            return true;
                        }
                    }

                    if (entry.expanded || entry.expandAnim.getValue() > 0.01f) {
                        float settingY = renderY + MODULE_HEIGHT + 2;
                        for (SettingComponent sc : entry.settingComponents) {
                            if (!sc.setting.visible()) continue;
                            float sh = sc.getHeight();
                            if (HoverUtil.isHovered(smX, smY, colX + MODULE_PADDING + 4, settingY, panelW - MODULE_PADDING * 2 - 8, sh)) {
                                if (sc.mouseClicked(smX, smY, button)) return true;
                            }
                            settingY += sh + 2;
                        }
                    }

                    renderY += moduleH;
                }
            }
        }

        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double deltaX, double deltaY) {
        if (button == 0 && draggingIndex >= 0) {
            double smX = mouseX / guiScale;
            double smY = mouseY / guiScale;
            panelX[draggingIndex] = (float) (smX - dragDX);
            panelY[draggingIndex] = (float) (smY - dragDY);
            clampPanel(draggingIndex);
            return true;
        }
        return super.mouseDragged(mouseX, mouseY, button, deltaX, deltaY);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (button == 0 && draggingIndex >= 0) {
            draggingIndex = -1;
            savePanelPositions();
            return true;
        }
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        double smX = mouseX / guiScale;
        double smY = mouseY / guiScale;
        ModuleCategory[] cats = ModuleCategory.values();
        for (int i = 0; i < cats.length; i++) {
            float colX = panelX[i];
            float colY = panelY[i];
            float headerH = CATEGORY_HEIGHT;
            float contentH = VISIBLE_HEIGHT * collapseAnim[i].getValue();
            if (contentH < 1f) continue;

            if (HoverUtil.isHovered(smX, smY, colX, colY, computePanelWidth(i), headerH + contentH)) {
                targetScrolls[i] -= (float) verticalAmount * SCROLL_SPEED;
                return true;
            }
        }
        return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (searchFocused) {
            if (keyCode == GLFW.GLFW_KEY_ESCAPE || keyCode == GLFW.GLFW_KEY_ENTER || keyCode == GLFW.GLFW_KEY_TAB) {
                searchFocused = false;
                return true;
            }
            if (keyCode == GLFW.GLFW_KEY_BACKSPACE) {
                if (!searchText.isEmpty()) searchText = searchText.substring(0, searchText.length() - 1);
                return true;
            }
            return true;
        }
        if (bindingKeySetting != null) {
            if (keyCode == GLFW.GLFW_KEY_ESCAPE || keyCode == GLFW.GLFW_KEY_DELETE) {
                bindingKeySetting.setKey(-1);
            } else if (keyCode != GLFW.GLFW_KEY_UNKNOWN) {
                bindingKeySetting.setKey(keyCode);
            }
            bindingKeySetting = null;
            return true;
        }
        if (bindingModule != null) {
            if (keyCode == GLFW.GLFW_KEY_ESCAPE || keyCode == GLFW.GLFW_KEY_DELETE) {
                bindingModule.setKeybind(0);
            } else if (keyCode != GLFW.GLFW_KEY_UNKNOWN) {
                bindingModule.setKeybind(keyCode);
            }
            bindingModule = null;
            return true;
        }
        if (focusedStringSetting != null) {
            if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
                focusedStringSetting = null;
                return true;
            }
            if (keyCode == GLFW.GLFW_KEY_BACKSPACE) {
                String text = focusedStringSetting.getText();
                if (!text.isEmpty()) focusedStringSetting.setText(text.substring(0, text.length() - 1));
                return true;
            }
        }
        for (ModuleEntry entry : moduleEntries) {
            if (entry.expanded) {
                for (SettingComponent sc : entry.settingComponents) {
                    sc.keyPressed(keyCode, scanCode, modifiers);
                }
            }
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean charTyped(char c, int modifiers) {
        if (searchFocused) {
            searchText += c;
            return true;
        }
        if (focusedStringSetting != null) {
            focusedStringSetting.setText(focusedStringSetting.getText() + c);
            return true;
        }
        return super.charTyped(c, modifiers);
    }

    private void updateScroll(int index, List<Module> modules) {
        float totalH = 0;
        if (ModuleCategory.values()[index] == ModuleCategory.THEMES) {
            totalH = ThemeManager.getInstance().getThemes().size() * 20f;
        } else {
            for (Module module : modules) {
                ModuleEntry entry = getEntry(module);
                totalH += entry != null ? entry.getHeight() : MODULE_HEIGHT;
            }
        }
        float maxScroll = Math.max(0, totalH - VISIBLE_HEIGHT);
        targetScrolls[index] = Math.max(0, Math.min(targetScrolls[index], maxScroll));
        float diff = targetScrolls[index] - scrollOffsets[index];
        if (Math.abs(diff) < 0.1f) {
            scrollOffsets[index] = targetScrolls[index];
        } else {
            scrollOffsets[index] += diff * 0.2f;
        }
    }

    private Color lighter(Color c, float amount) {
        return new Color(
                Math.min(255, c.getRed() + (int) (amount * 255)),
                Math.min(255, c.getGreen() + (int) (amount * 255)),
                Math.min(255, c.getBlue() + (int) (amount * 255)),
                c.getAlpha()
        );
    }

    private Color darker(Color c, float factor) {
        return new Color(
                Math.max(0, (int) (c.getRed() * factor)),
                Math.max(0, (int) (c.getGreen() * factor)),
                Math.max(0, (int) (c.getBlue() * factor)),
                c.getAlpha()
        );
    }

    private String getKeyName(int key) {
        if (key >= 1000 && key <= 1007) {
            return switch (key) {
                case 1000 -> "LMB";
                case 1001 -> "RMB";
                case 1002 -> "MMB";
                default -> "M" + (key - 1000 + 1);
            };
        }
        String name = GLFW.glfwGetKeyName(key, 0);
        if (name != null) return name.toUpperCase();
        return switch (key) {
            case GLFW.GLFW_KEY_LEFT_SHIFT -> "LSHIFT";
            case GLFW.GLFW_KEY_RIGHT_SHIFT -> "RSHIFT";
            case GLFW.GLFW_KEY_LEFT_CONTROL -> "LCTRL";
            case GLFW.GLFW_KEY_RIGHT_CONTROL -> "RCTRL";
            case GLFW.GLFW_KEY_LEFT_ALT -> "LALT";
            case GLFW.GLFW_KEY_RIGHT_ALT -> "RALT";
            case GLFW.GLFW_KEY_TAB -> "TAB";
            case GLFW.GLFW_KEY_CAPS_LOCK -> "CAPS";
            case GLFW.GLFW_KEY_ENTER -> "ENTER";
            case GLFW.GLFW_KEY_SPACE -> "SPACE";
            case GLFW.GLFW_KEY_BACKSPACE -> "BSPACE";
            case GLFW.GLFW_KEY_DELETE -> "DEL";
            case GLFW.GLFW_KEY_ESCAPE -> "ESC";
            case GLFW.GLFW_KEY_INSERT -> "INS";
            case GLFW.GLFW_KEY_HOME -> "HOME";
            case GLFW.GLFW_KEY_END -> "END";
            case GLFW.GLFW_KEY_PAGE_UP -> "PGUP";
            case GLFW.GLFW_KEY_PAGE_DOWN -> "PGDN";
            case GLFW.GLFW_KEY_UP -> "UP";
            case GLFW.GLFW_KEY_DOWN -> "DOWN";
            case GLFW.GLFW_KEY_LEFT -> "LEFT";
            case GLFW.GLFW_KEY_RIGHT -> "RIGHT";
            case GLFW.GLFW_KEY_MINUS -> "-";
            case GLFW.GLFW_KEY_EQUAL -> "=";
            case GLFW.GLFW_KEY_WORLD_1 -> "W1";
            case GLFW.GLFW_KEY_WORLD_2 -> "W2";
            default -> {
                if (key >= GLFW.GLFW_KEY_F1 && key <= GLFW.GLFW_KEY_F25) yield "F" + (key - GLFW.GLFW_KEY_F1 + 1);
                yield "KEY_" + key;
            }
        };
    }

    @Override
    public boolean shouldPause() {
        return false;
    }

    @Override
    public void close() {
        if (closing) return;
        closing = true;
        openAnim.start(openAnim.getValue(), 0f, 200, Easing.CUBIC_IN);
    }

    private class ModuleEntry {
        final Module module;
        final List<SettingComponent> settingComponents = new ArrayList<>();
        final Animation toggleAnim = new Animation();
        final Animation expandAnim = new Animation();
        boolean expanded = false;

        ModuleEntry(Module module) {
            this.module = module;
            toggleAnim.setValue(module.isEnabled() ? 1f : 0f);
            for (Setting s : module.getSettings()) {
                settingComponents.add(new SettingComponent(s));
            }
        }

        float getHeight() {
            float h = MODULE_HEIGHT;
            if (!settingComponents.isEmpty()) {
                float sh = 0;
                for (SettingComponent sc : settingComponents)
                    if (sc.setting.visible()) sh += sc.getHeight() + 2;
                h += (2 + sh) * expandAnim.getValue();
            }
            return h;
        }
    }

    private class SettingComponent {
        final Setting setting;
        final Animation checkAnim = new Animation();
        float x, y, width;
        boolean sliding = false;

        SettingComponent(Setting setting) {
            this.setting = setting;
            if (setting instanceof BooleanSetting bs) {
                checkAnim.setValue(bs.getValue() ? 1f : 0f);
            }
        }

        void draw(DrawContext context, Matrix4f mx, int mouseX, int mouseY, float alpha,
                  int bg, int surface, int textPrimary, int textSecondary, int textAccent, int accent) {
            Color tCol = new Color(textPrimary, true);
            Color sCol = new Color(textSecondary, true);
            Color ac = new Color(accent, true);

            if (setting instanceof BooleanSetting bs) {
                drawBooleanSetting(mx, bs, alpha, tCol, ac, bg);
            } else if (setting instanceof ModeSetting ms) {
                drawModeSetting(mx, ms, mouseX, mouseY, alpha, tCol, ac);
            } else if (setting instanceof SliderSetting ss) {
                drawSliderSetting(mx, ss, mouseX, alpha, tCol, sCol, ac, bg);
            } else if (setting instanceof NumberSetting ns) {
                drawNumberSetting(mx, ns, mouseX, alpha, tCol, sCol, ac, bg);
            } else if (setting instanceof KeySetting ks) {
                drawKeySetting(mx, ks, alpha, tCol, ac);
            } else if (setting instanceof ListSetting ls) {
                drawListSetting(mx, ls, mouseX, mouseY, alpha, tCol, sCol, ac, bg);
            } else if (setting instanceof StringSetting ss) {
                drawStringSetting(mx, ss, alpha, tCol, sCol, ac);
            }
        }

        private void drawBooleanSetting(Matrix4f mx, BooleanSetting bs, float alpha, Color tCol, Color ac, int bg) {
            checkAnim.update();
            float checkVal = checkAnim.getValue();

            drawGradientModuleText(mx, bs.getName(), SETTING_NAME_SIZE, 0.05f, x, y + 1f, alpha);

            if (checkVal > 0.01f) {
                float checkSize = 8f;
                float iconW = ICONS_FONT.get().getWidth("S", checkSize);
                float iconX = x + width - iconW;
                float iconY = y + (11f - checkSize) / 2f;
                int pCol = ThemeManager.getInstance().getPrimary();
                int sCol = ThemeManager.getInstance().isRainbow() ? pCol : ThemeManager.getInstance().getSecondary();
                Builder.text()
                        .text("S")
                        .font(ICONS_FONT.get())
                        .size(checkSize)
                        .color(applyAlpha(new Color(sCol), alpha * checkVal))
                        .thickness(0.05f)
                        .build().render(mx, iconX + 0.5f, iconY + 0.5f);
                Builder.text()
                        .text("S")
                        .font(ICONS_FONT.get())
                        .size(checkSize)
                        .color(applyAlpha(new Color(pCol), alpha * checkVal))
                        .thickness(0.05f)
                        .build().render(mx, iconX, iconY);
            }
        }

        private void drawModeSetting(Matrix4f mx, ModeSetting ms, int mouseX, int mouseY, float alpha, Color tCol, Color ac) {
            drawGradientModuleText(mx, ms.getName(), SETTING_NAME_SIZE, 0.05f, x, y + 1f, alpha);

            int n = ms.getModes().size();
            float[] cx = new float[n], cy = new float[n], cw = new float[n], ch = new float[n];
            layoutModes(ms, cx, cy, cw, ch);
            int i = 0;
            for (String opt : ms.getModes()) {
                boolean hov = HoverUtil.isHovered(mouseX, mouseY, cx[i], cy[i], cw[i], ch[i]);
                boolean selected = ms.getValue().equals(opt);

                Color gradStart = new Color(ThemeManager.getInstance().getPrimary());
                Color gradEnd = new Color(ThemeManager.getInstance().getSecondary());
                float chipAlpha = selected ? 210f : (hov ? 130f : 70f);
                Color gs = new Color(gradStart.getRed(), gradStart.getGreen(), gradStart.getBlue(), (int) chipAlpha);
                Color ge = new Color(gradEnd.getRed(), gradEnd.getGreen(), gradEnd.getBlue(), (int) chipAlpha);
                Builder.rectangle().size(new SizeState(cw[i], ch[i])).radius(new QuadRadiusState(MODE_RADIUS))
                        .color(new QuadColorState(applyAlpha(gs, alpha), applyAlpha(ge, alpha), applyAlpha(gs, alpha), applyAlpha(ge, alpha))).build().render(mx, cx[i], cy[i]);

                float textY = cy[i] + (ch[i] - MODE_FONT_SIZE) / 2f;
                if (selected) {
                    Builder.text().text(opt).font(BIKO_FONT.get()).size(MODE_FONT_SIZE)
                            .color(applyAlpha(Color.WHITE, alpha)).thickness(0.05f).build().render(mx, cx[i] + MODE_PAD_X, textY);
                } else {
                    drawGradientModuleText(mx, opt, MODE_FONT_SIZE, 0.05f, cx[i] + MODE_PAD_X, textY, 0.7f * alpha);
                }
                i++;
            }
        }

        private float layoutModes(ModeSetting ms, float[] chipX, float[] chipY, float[] chipW, float[] chipH) {
            float curX = x;
            float curY = y + 13f;
            float bottom = curY;
            int i = 0;
            for (String opt : ms.getModes()) {
                float labelW = BIKO_FONT.get().getWidth(opt, MODE_FONT_SIZE);
                float cw = labelW + MODE_PAD_X * 2f;
                float ch = MODE_FONT_SIZE + MODE_PAD_Y * 2f;
                if (i > 0 && curX + cw > x + width) {
                    curX = x;
                    curY += ch + MODE_GAP_Y;
                }
                chipX[i] = curX;
                chipY[i] = curY;
                chipW[i] = cw;
                chipH[i] = ch;
                bottom = Math.max(bottom, curY + ch);
                curX += cw + MODE_GAP_X;
                i++;
            }
            return bottom - y;
        }

        private void drawSliderSetting(Matrix4f mx, SliderSetting ss, int mouseX, float alpha, Color tCol, Color sCol, Color ac, int bg) {
            drawGradientModuleText(mx, ss.getName(), SETTING_NAME_SIZE, 0.05f, x, y, alpha);
            String val = ss.getValueAsString();
            float vw = BIKO_FONT.get().getWidth(val, SETTING_VALUE_SIZE);
            drawGradientModuleText(mx, val, SETTING_VALUE_SIZE, 0.05f, x + width - vw, y, alpha * 0.85f);

            float barY = y + 10f;
            Builder.rectangle().size(new SizeState(width, 2f)).radius(new QuadRadiusState(1f))
                    .color(new QuadColorState(applyAlpha(Color.WHITE, 0.15f * alpha))).build().render(mx, x, barY);

            double pct = Math.max(0, Math.min(1, (ss.getValue() - ss.getMin()) / (ss.getMax() - ss.getMin())));
            Builder.rectangle().size(new SizeState((float) (width * pct), 2f)).radius(new QuadRadiusState(1f))
                    .color(new QuadColorState(applyAlpha(Color.WHITE, 0.9f * alpha))).build().render(mx, x, barY);

            int pCol = ThemeManager.getInstance().getPrimary();
            int sCol2 = ThemeManager.getInstance().isRainbow() ? pCol : ThemeManager.getInstance().getSecondary();
            Color gs = new Color(pCol);
            Color ge = new Color(sCol2);
            float thumbX = x + (float) (width * pct) - 3f;
            Builder.rectangle().size(new SizeState(6f, 6f)).radius(new QuadRadiusState(3f))
                    .color(new QuadColorState(applyAlpha(gs, alpha), applyAlpha(ge, alpha), applyAlpha(gs, alpha), applyAlpha(ge, alpha)))
                    .build().render(mx, thumbX, barY - 2f);

            if (sliding && GLFW.glfwGetMouseButton(mc.getWindow().getHandle(), GLFW.GLFW_MOUSE_BUTTON_LEFT) == GLFW.GLFW_PRESS) {
                ss.setValue(ss.getMin() + Math.max(0, Math.min(1, (mouseX - x) / width)) * (ss.getMax() - ss.getMin()));
            } else sliding = false;
        }

        private void drawNumberSetting(Matrix4f mx, NumberSetting ns, int mouseX, float alpha, Color tCol, Color sCol, Color ac, int bg) {
            drawGradientModuleText(mx, ns.getName(), SETTING_NAME_SIZE, 0.05f, x, y, alpha);
            String val = ns.getValueAsString();
            float vw = BIKO_FONT.get().getWidth(val, SETTING_VALUE_SIZE);
            drawGradientModuleText(mx, val, SETTING_VALUE_SIZE, 0.05f, x + width - vw, y, alpha * 0.85f);

            float barY = y + 10f;
            Builder.rectangle().size(new SizeState(width, 2f)).radius(new QuadRadiusState(1f))
                    .color(new QuadColorState(applyAlpha(Color.WHITE, 0.15f * alpha))).build().render(mx, x, barY);

            double pct = Math.max(0, Math.min(1, (ns.getValue() - ns.getMin()) / (ns.getMax() - ns.getMin())));
            Builder.rectangle().size(new SizeState((float) (width * pct), 2f)).radius(new QuadRadiusState(1f))
                    .color(new QuadColorState(applyAlpha(Color.WHITE, 0.9f * alpha))).build().render(mx, x, barY);

            int pCol = ThemeManager.getInstance().getPrimary();
            int sCol2 = ThemeManager.getInstance().isRainbow() ? pCol : ThemeManager.getInstance().getSecondary();
            Color gs = new Color(pCol);
            Color ge = new Color(sCol2);
            float thumbX = x + (float) (width * pct) - 3f;
            Builder.rectangle().size(new SizeState(6f, 6f)).radius(new QuadRadiusState(3f))
                    .color(new QuadColorState(applyAlpha(gs, alpha), applyAlpha(ge, alpha), applyAlpha(gs, alpha), applyAlpha(ge, alpha)))
                    .build().render(mx, thumbX, barY - 2f);

            if (sliding && GLFW.glfwGetMouseButton(mc.getWindow().getHandle(), GLFW.GLFW_MOUSE_BUTTON_LEFT) == GLFW.GLFW_PRESS) {
                ns.setValue(ns.getMin() + Math.max(0, Math.min(1, (mouseX - x) / width)) * (ns.getMax() - ns.getMin()));
            } else sliding = false;
        }

        private void drawKeySetting(Matrix4f mx, KeySetting ks, float alpha, Color tCol, Color ac) {
            drawGradientModuleText(mx, ks.getName(), SETTING_NAME_SIZE, 0.05f, x, y + 1f, alpha);

            String keyName;
            if (bindingKeySetting == ks) {
                keyName = "...";
            } else {
                int k = ks.getKey();
                if (k == -1) {
                    keyName = "NONE";
                } else {
                    keyName = ks.getValueAsString();
                }
            }
            float vw = BIKO_FONT.get().getWidth(keyName, SETTING_VALUE_SIZE);
            drawGradientModuleText(mx, keyName, SETTING_VALUE_SIZE, 0.05f, x + width - vw, y + 1f, alpha * 0.85f);
        }

        private void drawListSetting(Matrix4f mx, ListSetting ls, int mouseX, int mouseY, float alpha, Color tCol, Color sCol, Color ac, int bg) {
            drawGradientModuleText(mx, ls.getName(), SETTING_NAME_SIZE, 0.05f, x, y + 1f, alpha);

            float optY = y + 12f;
            for (String opt : ls.getOptions()) {
                boolean hov = HoverUtil.isHovered(mouseX, mouseY, x, optY - 0.5f, width, 10f);
                Color checkboxBg = ls.isSelected(opt) ? new Color(255, 255, 255, (int) (200 * alpha)) : new Color(255, 255, 255, (int) (40 * alpha));
                if (hov) {
                    checkboxBg = ls.isSelected(opt)
                            ? new Color(255, 255, 255, (int) (230 * alpha))
                            : new Color(255, 255, 255, (int) (60 * alpha));
                }

                Builder.rectangle().size(new SizeState(7f, 7f)).radius(new QuadRadiusState(1.5f))
                        .color(new QuadColorState(checkboxBg)).build().render(mx, x, optY);

                if (ls.isSelected(opt)) {
                    Builder.rectangle().size(new SizeState(2.5f, 2.5f)).radius(new QuadRadiusState(1f))
                            .color(new QuadColorState(applyAlpha(Color.WHITE, alpha))).build().render(mx, x + 2.25f, optY + 2.25f);
                }

                drawGradientModuleText(mx, opt, SETTING_VALUE_SIZE, 0.05f, x + 11f, optY + 0.5f,
                        (ls.isSelected(opt) ? 1f : 0.65f) * alpha);
                optY += 10f;
            }
        }

        private void drawStringSetting(Matrix4f mx, StringSetting ss, float alpha, Color tCol, Color sCol, Color ac) {
            drawGradientModuleText(mx, ss.getName(), SETTING_NAME_SIZE, 0.05f, x, y + 1f, alpha);

            float boxY = y + 11f;
            float boxH = 11f;

            Builder.rectangle()
                    .size(new SizeState(width, boxH))
                    .radius(new QuadRadiusState(1.5f))
                    .color(new QuadColorState(applyAlpha(Color.WHITE, alpha * 0.1f)))
                    .build().render(mx, x, boxY);

            if (focusedStringSetting == ss) {
                Builder.rectangle()
                        .size(new SizeState(width, 1f))
                        .radius(new QuadRadiusState(0))
                        .color(new QuadColorState(applyAlpha(Color.WHITE, alpha * 0.7f)))
                        .build().render(mx, x, boxY + boxH);
            }

            String display = ss.getText().isEmpty() ? "..." : ss.getText();
            if (ss.getText().isEmpty()) {
                Builder.text().text(display).font(BIKO_FONT.get()).size(SETTING_VALUE_SIZE)
                        .color(applyAlpha(new Color(255, 255, 255, 100), alpha))
                        .thickness(0.05f).build().render(mx, x + 4f, boxY + 2f);
            } else {
                drawGradientModuleText(mx, display, SETTING_VALUE_SIZE, 0.05f, x + 4f, boxY + 2f, alpha);
            }

            if (focusedStringSetting == ss && (System.currentTimeMillis() / 530) % 2 == 0) {
                float tw = BIKO_FONT.get().getWidth(ss.getText(), SETTING_VALUE_SIZE);
                Builder.rectangle()
                        .size(new SizeState(1f, 7f))
                        .color(new QuadColorState(applyAlpha(Color.WHITE, alpha * 0.8f)))
                        .build().render(mx, x + 4f + tw, boxY + 2f);
            }
        }

        boolean mouseClicked(double mouseX, double mouseY, int button) {
            if (button != 0) return false;
            if (setting instanceof BooleanSetting bs) {
                if (HoverUtil.isHovered(mouseX, mouseY, x, y, width, getHeight())) {
                    bs.setValue(!bs.getValue());
                    checkAnim.start(checkAnim.getValue(), bs.getValue() ? 1f : 0f, 200, Easing.CUBIC_OUT);
                    return true;
                }
            } else if (setting instanceof ModeSetting ms) {
                int n = ms.getModes().size();
                float[] cx = new float[n], cy = new float[n], cw = new float[n], ch = new float[n];
                layoutModes(ms, cx, cy, cw, ch);
                int i = 0;
                for (String opt : ms.getModes()) {
                    if (mouseX >= cx[i] && mouseX < cx[i] + cw[i] && mouseY >= cy[i] && mouseY < cy[i] + ch[i]) {
                        ms.setValue(opt);
                        return true;
                    }
                    i++;
                }
            } else if (setting instanceof SliderSetting) {
                if (HoverUtil.isHovered(mouseX, mouseY, x, y, width, getHeight())) {
                    sliding = true;
                    return true;
                }
            } else if (setting instanceof NumberSetting) {
                if (HoverUtil.isHovered(mouseX, mouseY, x, y, width, getHeight())) {
                    sliding = true;
                    return true;
                }
            } else if (setting instanceof KeySetting ks) {
                if (HoverUtil.isHovered(mouseX, mouseY, x, y, width, getHeight())) {
                    bindingKeySetting = ks;
                    return true;
                }
            } else if (setting instanceof ListSetting ls) {
                if (HoverUtil.isHovered(mouseX, mouseY, x, y, width, getHeight())) {
                    float optY = y + 12f;
                    for (String opt : ls.getOptions()) {
                        if (mouseY >= optY - 0.5f && mouseY < optY + 10f) {
                            ls.toggle(opt);
                            return true;
                        }
                        optY += 10f;
                    }
                }
            } else if (setting instanceof StringSetting ss) {
                if (HoverUtil.isHovered(mouseX, mouseY, x, y, width, getHeight())) {
                    focusedStringSetting = ss;
                    return true;
                }
            }
            return false;
        }

        void keyPressed(int keyCode, int scanCode, int modifiers) {
            if (setting instanceof KeySetting ks && bindingKeySetting == ks) {
                if (keyCode == GLFW.GLFW_KEY_ESCAPE || keyCode == GLFW.GLFW_KEY_DELETE) {
                    ks.setKey(-1);
                } else if (keyCode != GLFW.GLFW_KEY_UNKNOWN) {
                    ks.setKey(keyCode);
                }
                bindingKeySetting = null;
            }
        }

        float getHeight() {
            if (setting instanceof SliderSetting || setting instanceof NumberSetting) return 14f;
            if (setting instanceof ListSetting ls) return 12f + ls.getOptions().size() * 10f;
            if (setting instanceof StringSetting) return 24f;
            if (setting instanceof ModeSetting ms) {
                int n = ms.getModes().size();
                return layoutModes(ms, new float[n], new float[n], new float[n], new float[n]);
            }
            return 11f;
        }
    }
}

