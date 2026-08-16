package fun.ogi.screens;

import com.google.common.base.Supplier;
import com.google.common.base.Suppliers;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import fun.ogi.Cheap;
import fun.ogi.module.ModuleCategory;
import fun.ogi.module.impl.list.misc.ClientSounds;
import fun.ogi.module.settings.*;
import fun.ogi.module.theme.Theme;
import fun.ogi.module.theme.ThemeManager;
import fun.ogi.util.animation.Animation;
import fun.ogi.util.animation.Easing;
import fun.ogi.util.render.ShadowUtil;
import fun.ogi.util.render.builders.Builder;
import fun.ogi.util.render.builders.impl.BlurBuilder;
import fun.ogi.util.render.builders.impl.LiquidBuilder;
import fun.ogi.util.render.builders.impl.RectangleBuilder;
import fun.ogi.util.render.builders.states.QuadColorState;
import fun.ogi.util.render.builders.states.QuadRadiusState;
import fun.ogi.util.render.builders.states.SizeState;
import fun.ogi.util.render.helper.HoverUtil;
import fun.ogi.util.render.msdf.MsdfFont;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;
import org.joml.Matrix4f;
import org.lwjgl.glfw.GLFW;
import fun.ogi.module.Module;
import org.w3c.dom.css.Rect;

import java.awt.*;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.util.*;
import java.util.List;
import java.util.stream.Collectors;

import static fun.ogi.util.MinecraftUtil.mc;

public class ClickGuiScreen extends Screen {
    private static final Supplier<MsdfFont> BIKO_FONT = Suppliers.memoize(() -> MsdfFont.builder().atlas("biko").data("biko").build());
    private static final Supplier<MsdfFont> ICON_FONT  = Suppliers.memoize(() -> MsdfFont.builder().atlas("nur").data("nur").build());

    private float x, y;
    private static final float WINDOW_WIDTH    = 500f;
    private static final float WINDOW_HEIGHT   = 276f;
    private static final float TOPBAR_HEIGHT   = 22f;
    private static final float CATEGORY_WIDTH  = 108f;
    private static final float CATEGORY_HEIGHT = 24f;
    private static final float CATEGORY_GAP    = 3f;
    private static final float MODULE_HEIGHT   = 20f;
    private static final float MODULE_ROW_HEIGHT = 42f;
    private static final float MODULE_GAP      = 2f;
    private static final float COL_PADDING     = 5f;
    private static final float SCROLLBAR_W     = 2.5f;
    private static final float SCROLL_SPEED    = 22f;
    private static final float SCROLL_LERP     = 0.18f;

    private ModuleCategory activeCategory = ModuleCategory.values()[0];
    private final Map<ModuleCategory, List<ModuleComponent>> categoryModules = new EnumMap<>(ModuleCategory.class);
    private final SearchComponent search = new SearchComponent();
    private final Animation alphaAnimation = new Animation();
    private final Animation categoryFade = new Animation();
    private final Animation categorySlide = new Animation();
    private long openTime = 0;
    private Module bindingModule;
    private KeySetting bindingKeySetting;
    private SettingComponent focusedStringComponent;

    private static final String[] CATEGORY_ICONS = {"S", "E", "Y", "W", "A", "U"};

    private static final int NUM_COLS = 2;
    private float targetScroll;
    private float currentScroll;

    private final float guiScale;
    private boolean dragging;
    private float dragOffsetX, dragOffsetY;

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static float savedX = Float.NaN;
    private static float savedY = Float.NaN;

    public ClickGuiScreen(float guiScale) {
        super(Text.literal("ClickGUI"));
        this.guiScale = guiScale;
    }

    private void rebuild() {
        categoryModules.clear();
        List<Module> all = Cheap.getInstance().getModuleStorage().getModules();
        for (ModuleCategory c : ModuleCategory.values()) {
            List<ModuleComponent> list;
            if (c == ModuleCategory.THEMES) {
                list = new ArrayList<>();
                list.add(new ThemeHeaderComponent("Search themes…", true));
                list.add(new ThemeHeaderComponent("Favorites: OFF", false));
                list.add(new ThemeCreateComponent());
                for (Theme t : ThemeManager.getInstance().getThemes()) list.add(new ThemeComponent(t));
            } else {
                list = all.stream()
                        .filter(m -> m.getCategory() == c)
                        .map(ModuleComponent::new)
                        .collect(Collectors.toList());
            }
            categoryModules.put(c, list);
        }
        targetScroll = 0f;
        currentScroll = 0f;
    }

    @Override
    protected void init() {
        rebuild();
        centerWindow();
        reflow();
        categoryFade.setValue(1f);
        alphaAnimation.start(0f, 1f, 200, Easing.CUBIC_OUT);
        openTime = System.currentTimeMillis();
    }

    private void centerWindow() {
        loadPosition();
        if (Float.isNaN(savedX) || Float.isNaN(savedY)) {
            x = (this.width  / 2f) - (WINDOW_WIDTH  / 2f);
            y = (this.height / 2f) - (WINDOW_HEIGHT / 2f);
        } else {
            x = Math.max(-WINDOW_WIDTH + 80f, Math.min(this.width - 80f, savedX));
            y = Math.max(-TOPBAR_HEIGHT + 12f, Math.min(this.height - 60f, savedY));
        }
    }

    private static void loadPosition() {
        try {
            File f = new File(Cheap.getInstance().getCheapDir(), "clickgui.json");
            if (f.exists()) {
                JsonObject o = JsonParser.parseReader(new FileReader(f)).getAsJsonObject();
                if (o.has("x") && o.has("y")) {
                    savedX = o.get("x").getAsFloat();
                    savedY = o.get("y").getAsFloat();
                }
            }
        } catch (Exception ignored) {}
    }

    private static void savePosition(float x, float y) {
        savedX = x;
        savedY = y;
        try {
            JsonObject o = new JsonObject();
            o.addProperty("x", x);
            o.addProperty("y", y);
            File f = new File(Cheap.getInstance().getCheapDir(), "clickgui.json");
            try (FileWriter w = new FileWriter(f)) {
                GSON.toJson(o, w);
            }
        } catch (Exception ignored) {}
    }

    private void reflow() {
        search.setX(x + (WINDOW_WIDTH / 2f) - 90f);
        search.setY(y + WINDOW_HEIGHT + 12f);
        search.setWidth(180f);
    }

    private void updateScroll() {
        float diff = targetScroll - currentScroll;
        if (Math.abs(diff) < 0.05f) currentScroll = targetScroll;
        else                         currentScroll += diff * SCROLL_LERP;
    }

    private List<ModuleComponent> getCurrentComps() {
        String q = search.getText().toLowerCase();
        if (q.isEmpty() || activeCategory == ModuleCategory.THEMES) {
            return categoryModules.getOrDefault(activeCategory, Collections.emptyList());
        }
        List<ModuleComponent> all = new ArrayList<>();
        for (ModuleCategory c : ModuleCategory.values()) {
            if (c == ModuleCategory.THEMES) continue;
            all.addAll(categoryModules.getOrDefault(c, Collections.emptyList()));
        }
        return all.stream()
                .filter(c -> c.getName().toLowerCase().contains(q))
                .collect(Collectors.toList());
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        alphaAnimation.update();
        categoryFade.update();
        categorySlide.update();
        updateScroll();

        float a = alphaAnimation.getValue();
        if (a <= 0f) return;

        int smX = (int) (mouseX / guiScale);
        int smY = (int) (mouseY / guiScale);

        context.getMatrices().push();
        context.getMatrices().scale(guiScale, guiScale, 1);

        reflow();
        Matrix4f mx = context.getMatrices().peek().getPositionMatrix();

        int surface = ThemeManager.getInstance().getPalette().getSurface();
        int accent  = ThemeManager.getInstance().getPrimary();
        int textP   = ThemeManager.getInstance().getPalette().getTextPrimary();
        int textS   = ThemeManager.getInstance().getPalette().getTextSecondary();
        int blurCol = ThemeManager.getInstance().getPalette().getHudBackground();
        Color blurBase = new Color(blurCol, true);
        Color darkBlur = new Color((int)(blurBase.getRed() * 0.12), (int)(blurBase.getGreen() * 0.12), (int)(blurBase.getBlue() * 0.12), blurBase.getAlpha());

        
        ShadowUtil.window(WINDOW_WIDTH, WINDOW_HEIGHT, new QuadRadiusState(6)).render(mx, x, y);

        Builder.rectangle()
                .size(new SizeState(WINDOW_WIDTH, WINDOW_HEIGHT))
                .radius(new QuadRadiusState(6))
                .smoothness(1f)
                .color(new QuadColorState(applyAlpha(new Color(4, 3, 3), a)))
                .build().render(mx, x, y);


        Builder.rectangle()
                .size(new SizeState(WINDOW_WIDTH, TOPBAR_HEIGHT))
                .radius(new QuadRadiusState(4, 4, 0, 0)).smoothness(1f)
                .color(new QuadColorState(applyAlpha(new Color(4, 3, 3), a)))
                .build().render(mx, x, y);
        Builder.rectangle().size(new SizeState(WINDOW_WIDTH/2.7f,0.8f))
                        .radius(new QuadRadiusState(2f)).color(new QuadColorState(Color.GRAY))
                        .build().render(mx,x+30,y);


        Builder.rectangle()
                .size(new SizeState(WINDOW_WIDTH, 2.5f))
                .radius(new QuadRadiusState(0)).smoothness(1f)
                .color(new QuadColorState(applyAlpha(new Color(25, 25, 25), a)))
                .build().render(mx, x, y + TOPBAR_HEIGHT);

        
        Builder.text()
                .text("ClickGUI")
                .font(BIKO_FONT.get())
                .size(11f)
                .color(applyAlpha(new Color(textP), a))
                .thickness(0.04f)
                .build().render(mx, x + 12f, y + (TOPBAR_HEIGHT - 11f) / 2f + 1f);

        
        float barW = 190f;
        float barH = 16f;
        float barX = x + (WINDOW_WIDTH - barW) / 2f;
        float barY = y + (TOPBAR_HEIGHT - barH) / 2f;

        new RectangleBuilder()
                .size(new SizeState(barW, barH))
                .radius(new QuadRadiusState(8))
                .color(new QuadColorState(applyAlpha(new Color(28, 27, 27), a * 0.98f)))
                .build().render(mx, barX, barY);

        String urlText = "GreedClient.fun";
        float uw = BIKO_FONT.get().getWidth(urlText, 8f);
        Builder.text()
                .text(urlText)
                .font(BIKO_FONT.get())
                .size(8f)
                .color(applyAlpha(Color.white, a * 0.9f))
                .thickness(0.05f)
                .build().render(mx, barX + (barW - uw) / 2f, barY + (barH - 8f) / 2f + 1f);

        
        ModuleCategory activeCat = activeCategory;
        Builder.text()
                .text(activeCat.name().charAt(0) + activeCat.name().substring(1).toLowerCase())
                .font(BIKO_FONT.get())
                .size(9f)
                .color(applyAlpha(new Color(textS), a * 0.8f))
                .thickness(0.05f)
                .build().render(mx, x + WINDOW_WIDTH - 150f, y + (TOPBAR_HEIGHT - 9f) / 2f + 1f);

        
        ModuleCategory[] cats = ModuleCategory.values();
        float sidebarX = x;
        float sidebarY = y + TOPBAR_HEIGHT;
        float sidebarW = CATEGORY_WIDTH;
        float sidebarH = WINDOW_HEIGHT - TOPBAR_HEIGHT;

        






        
        float step = CATEGORY_HEIGHT + CATEGORY_GAP;
        float slideY = sidebarY + 10f + categorySlide.getValue() * step;
        if (!categorySlide.isRunning() && Math.abs(categorySlide.getValue() - activeCategory.ordinal()) > 0.01f) {
            categorySlide.setValue(activeCategory.ordinal());
        }
        new RectangleBuilder()
                .size(new SizeState(sidebarW - 16f, CATEGORY_HEIGHT))
                .radius(new QuadRadiusState(4)).smoothness(1f)
                .color(new QuadColorState(applyAlpha(new Color(accent), a)))
                .build().render(mx, sidebarX + 8f, slideY);

        float catY = sidebarY + 10f;
        for (int ci = 0; ci < cats.length; ci++) {
            ModuleCategory c = cats[ci];
            boolean active = c == activeCategory;
            float catX = sidebarX + 8f;
            float catW = sidebarW - 16f;

            boolean catHovered = HoverUtil.isHovered(smX, smY, catX, catY, catW, CATEGORY_HEIGHT);
            if (!active && catHovered) {
                Builder.rectangle()
                        .size(new SizeState(catW, CATEGORY_HEIGHT))
                        .radius(new QuadRadiusState(4)).smoothness(1f)
                        .color(new QuadColorState(applyAlpha(lighter(new Color(surface), 0.06f), a * 0.9f)))
                        .build().render(mx, catX, catY);
            }

            
            Builder.text()
                    .text(CATEGORY_ICONS[ci])
                    .font(ICON_FONT.get())
                    .size(13f)
                    .color(applyAlpha(active ? Color.WHITE : new Color(textS), a))
                    .thickness(0.08f)
                    .build().render(mx, catX + 6f, catY + (CATEGORY_HEIGHT - 13f) / 2f + 1f);

            
            String catName = c.name().charAt(0) + c.name().substring(1).toLowerCase();
            Builder.text()
                    .text(catName)
                    .font(BIKO_FONT.get())
                    .size(9f)
                    .color(applyAlpha(active ? Color.WHITE : new Color(textS), a))
                    .thickness(active ? 0.03f : 0.06f)
                    .build().render(mx, catX + 24f, catY + (CATEGORY_HEIGHT - 9f) / 2f + 1f);

            catY += step;
        }

        
        new RectangleBuilder()
                .size(new SizeState(3f, 14f))
                .radius(new QuadRadiusState(1.5f)).smoothness(1f)
                .color(new QuadColorState(applyAlpha(new Color(accent), a)))
                .build().render(mx, sidebarX + sidebarW - 4f, slideY + (CATEGORY_HEIGHT - 14f) / 2f);

        Builder.rectangle()
                .size(new SizeState(2.5f, sidebarH))
                .radius(new QuadRadiusState(0)).smoothness(1f)
                .color(new QuadColorState(applyAlpha(new Color(25, 25, 25), a)))
                .build().render(mx, sidebarX + sidebarW, sidebarY);

        float contentX = x + CATEGORY_WIDTH + 10f;
        float listY      = y + TOPBAR_HEIGHT + 10f;
        float listHeight = WINDOW_HEIGHT - TOPBAR_HEIGHT - 20f;
        float contentW   = WINDOW_WIDTH - CATEGORY_WIDTH - 20f;
        float colW       = (contentW - COL_PADDING * (NUM_COLS + 1)) / NUM_COLS;

        renderColumnContent(context, mx, smX, smY, a * categoryFade.getValue(), contentX, listY, listHeight, colW, getCurrentComps(), 0);

        search.setX(x + (WINDOW_WIDTH / 2f) - 90f);
        search.setY(y + WINDOW_HEIGHT + 12f);
        search.draw(context, smX, smY, a);

        context.getMatrices().pop();
    }

    private void renderColumnContent(DrawContext context, Matrix4f mx, int mouseX, int mouseY, float a,
                                     float contentX, float listY, float listHeight, float colW,
                                     List<ModuleComponent> comps, float offsetY) {
        int accent = ThemeManager.getInstance().getPrimary();

        List<List<ModuleComponent>> cols = new ArrayList<>();
        for (int i = 0; i < NUM_COLS; i++) cols.add(new ArrayList<>());
        for (int i = 0; i < comps.size(); i++) cols.get(i % NUM_COLS).add(comps.get(i));

        float totalContentH = 0;
        for (int ci = 0; ci < NUM_COLS; ci++) {
            float h = 0;
            for (ModuleComponent c : cols.get(ci)) h += c.getHeight() + MODULE_GAP;
            totalContentH = Math.max(totalContentH, h);
        }

        float contentRight = contentX + COL_PADDING + NUM_COLS * (colW + COL_PADDING);
        context.enableScissor(
                (int) (contentX * guiScale),
                (int) (listY * guiScale),
                (int) (contentRight * guiScale),
                (int) ((listY + listHeight) * guiScale)
        );

        float scroll = currentScroll;
        for (int ci = 0; ci < NUM_COLS; ci++) {
            float colX = contentX + COL_PADDING + ci * (colW + COL_PADDING);
            float curY = listY + 8f + scroll + offsetY;
            for (ModuleComponent comp : cols.get(ci)) {
                comp.x     = colX;
                comp.y     = curY;
                comp.width = colW;
                comp.draw(context, mouseX, mouseY, a);
                curY += comp.getHeight() + MODULE_GAP;
            }
        }

        context.disableScissor();

        
        float visible = listHeight - 16f;
        float maxScr  = Math.max(0, totalContentH - visible);
        if (maxScr > 0) {
            float thumbH  = Math.max(16f, visible * (visible / totalContentH));
            float pct     = -scroll / maxScr;
            float thumbY  = listY + 8f + (visible - thumbH) * pct;
            new RectangleBuilder()
                    .size(new SizeState(SCROLLBAR_W, thumbH))
                    .radius(new QuadRadiusState(1f))
                    .color(new QuadColorState(applyAlpha(new Color(accent), a * 0.5f)))
                    .build().render(mx, contentRight - SCROLLBAR_W - 1f, thumbY);
        }
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double deltaX, double deltaY) {
        if (dragging && button == 0) {
            float nx = (float)(mouseX / guiScale) - dragOffsetX;
            float ny = (float)(mouseY / guiScale) - dragOffsetY;
            x = Math.max(-WINDOW_WIDTH + 80f, Math.min(this.width - 80f, nx));
            y = Math.max(-TOPBAR_HEIGHT + 12f, Math.min(this.height - 60f, ny));
            reflow();
            return true;
        }
        return super.mouseDragged(mouseX, mouseY, button, deltaX, deltaY);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (button == 0 && dragging) {
            dragging = false;
            savePosition(x, y);
            return true;
        }
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double hAmt, double vAmt) {
        double smX = mouseX / guiScale;
        double smY = mouseY / guiScale;
        float contentX = x + CATEGORY_WIDTH + 10f;
        float contentRight = x + WINDOW_WIDTH - 10f;
        float listY = y + TOPBAR_HEIGHT + 10f;
        float listHeight = WINDOW_HEIGHT - TOPBAR_HEIGHT - 20f;
        if (smX >= contentX && smX <= contentRight && smY >= listY && smY <= listY + listHeight) {
            List<ModuleComponent> allComps = getCurrentComps();
            List<List<ModuleComponent>> cols = new ArrayList<>();
            for (int i = 0; i < NUM_COLS; i++) cols.add(new ArrayList<>());
            for (int i = 0; i < allComps.size(); i++) cols.get(i % NUM_COLS).add(allComps.get(i));

            float totalContentH = 0;
            for (int ci = 0; ci < NUM_COLS; ci++) {
                float h = 0;
                for (ModuleComponent c : cols.get(ci)) h += c.getHeight() + MODULE_GAP;
                totalContentH = Math.max(totalContentH, h);
            }

            float visible = listHeight - 16f;
            float maxScr = Math.max(0, totalContentH - visible);
            targetScroll = Math.max(-maxScr, Math.min(0, targetScroll + (float) vAmt * SCROLL_SPEED));
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, hAmt, vAmt);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        double smX = mouseX / guiScale;
        double smY = mouseY / guiScale;
        focusedStringComponent = null;

        
        if (button == 0 && smY >= y && smY <= y + TOPBAR_HEIGHT) {
            dragging = true;
            dragOffsetX = (float)(smX - x);
            dragOffsetY = (float)(smY - y);
            return true;
        }

        if (bindingKeySetting != null) {
            bindingKeySetting.setKey(button);
            bindingKeySetting = null;
            return true;
        }

        ModuleCategory[] cats = ModuleCategory.values();
        float catY = y + TOPBAR_HEIGHT + 10f;
        for (int ci = 0; ci < cats.length; ci++) {
            float catX = x + 8f;
            float catW = CATEGORY_WIDTH - 16f;
            if (smX >= catX && smX <= catX + catW
                    && smY >= catY && smY <= catY + CATEGORY_HEIGHT) {
                ModuleCategory c = cats[ci];
                if (c != activeCategory) {
                    activeCategory = c;
                    categoryFade.start(0f, 1f, 150, Easing.CUBIC_OUT);
                    categorySlide.start(categorySlide.getValue(), (float) ci, 220, Easing.CUBIC_IN_OUT);
                    targetScroll = 0f;
                    currentScroll = 0f;
                }
                return true;
            }
            catY += CATEGORY_HEIGHT + CATEGORY_GAP;
        }

        if (search.mouseClicked(smX, smY, button)) return true;

        if (bindingModule != null) {
            boolean hitModule = false;
            for (ModuleComponent comp : getCurrentComps()) {
                if (comp.mouseClicked(smX, smY, button)) { hitModule = true; break; }
            }
            if (!hitModule) bindingModule = null;
            return hitModule || super.mouseClicked(smX, smY, button);
        }

        for (ModuleComponent comp : getCurrentComps())
            if (comp.mouseClicked(smX, smY, button)) return true;

        return super.mouseClicked(smX, smY, button);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
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
        if (focusedStringComponent != null) {
            if (keyCode == GLFW.GLFW_KEY_ESCAPE) { focusedStringComponent = null; return true; }
            if (focusedStringComponent.keyPressed(keyCode, scanCode, modifiers)) return true;
        }
        if (keyCode == GLFW.GLFW_KEY_ESCAPE) { this.close(); return true; }
        if (search.keyPressed(keyCode, scanCode, modifiers)) return true;
        for (ModuleComponent comp : getCurrentComps())
            if (comp.keyPressed(keyCode, scanCode, modifiers)) return true;
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean charTyped(char c, int modifiers) {
        if (search.charTyped(c, modifiers)) return true;
        if (focusedStringComponent != null && focusedStringComponent.charTyped(c, modifiers)) return true;
        return super.charTyped(c, modifiers);
    }

    @Override
    public void close() {
        if (alphaAnimation.isRunning() && alphaAnimation.getValue() < 0.1f) { super.close(); return; }
        ClientSounds.INSTANCE.playCloseGui();
        savePosition(x, y);
        alphaAnimation.start(alphaAnimation.getValue(), 0f, 150, Easing.LINEAR);
        new Thread(() -> {
            try { Thread.sleep(150); if (client != null) client.execute(super::close); }
            catch (InterruptedException ignored) {}
        }).start();
    }

    @Override public boolean shouldPause() { return false; }

    private Color applyAlpha(Color c, float alpha) {
        return new Color(c.getRed(), c.getGreen(), c.getBlue(), Math.max(0, Math.min(255, (int)(c.getAlpha() * alpha))));
    }

    private String getKeyName(int key) {
        String name = GLFW.glfwGetKeyName(key, 0);
        if (name != null) return name.toUpperCase();
        return switch (key) {
            case GLFW.GLFW_KEY_LEFT_SHIFT -> "L-SHIFT";
            case GLFW.GLFW_KEY_RIGHT_SHIFT -> "R-SHIFT";
            case GLFW.GLFW_KEY_LEFT_CONTROL -> "L-CTRL";
            case GLFW.GLFW_KEY_RIGHT_CONTROL -> "R-CTRL";
            case GLFW.GLFW_KEY_LEFT_ALT -> "L-ALT";
            case GLFW.GLFW_KEY_RIGHT_ALT -> "R-ALT";
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
            case GLFW.GLFW_KEY_LEFT_SUPER -> "L-WIN";
            case GLFW.GLFW_KEY_RIGHT_SUPER -> "R-WIN";
            case GLFW.GLFW_KEY_MENU -> "MENU";
            case GLFW.GLFW_KEY_GRAVE_ACCENT -> "`";
            case GLFW.GLFW_KEY_MINUS -> "-";
            case GLFW.GLFW_KEY_EQUAL -> "=";
            case GLFW.GLFW_KEY_LEFT_BRACKET -> "[";
            case GLFW.GLFW_KEY_RIGHT_BRACKET -> "]";
            case GLFW.GLFW_KEY_SEMICOLON -> ";";
            case GLFW.GLFW_KEY_APOSTROPHE -> "'";
            case GLFW.GLFW_KEY_COMMA -> ",";
            case GLFW.GLFW_KEY_PERIOD -> ".";
            case GLFW.GLFW_KEY_SLASH -> "/";
            case GLFW.GLFW_KEY_BACKSLASH -> "\\";
            default -> {
                if (key >= GLFW.GLFW_KEY_F1 && key <= GLFW.GLFW_KEY_F25) yield "F" + (key - GLFW.GLFW_KEY_F1 + 1);
                if (key >= GLFW.GLFW_KEY_0 && key <= GLFW.GLFW_KEY_9) yield String.valueOf((char) ('0' + key - GLFW.GLFW_KEY_0));
                if (key >= GLFW.GLFW_KEY_A && key <= GLFW.GLFW_KEY_Z) yield String.valueOf((char) ('A' + key - GLFW.GLFW_KEY_A));
                yield "KEY_" + key;
            }
        };
    }

    private Color darker(Color c, float factor) {
        return new Color(
                Math.max(0, (int)(c.getRed()   * factor)),
                Math.max(0, (int)(c.getGreen() * factor)),
                Math.max(0, (int)(c.getBlue()  * factor)),
                c.getAlpha()
        );
    }

    private Color lighter(Color c, float amount) {
        return new Color(
                Math.min(255, c.getRed()   + (int)(amount * 255)),
                Math.min(255, c.getGreen() + (int)(amount * 255)),
                Math.min(255, c.getBlue()  + (int)(amount * 255)),
                c.getAlpha()
        );
    }

    
    
    

    
    
    private class ModuleComponent {
        protected final Module module;
        protected float x, y, width;
        protected boolean expanded;
        protected final List<SettingComponent> settingComponents = new ArrayList<>();
        private final Animation toggleAnimation = new Animation();
        private final Animation expandAnimation = new Animation();

        public ModuleComponent(Module module) {
            this.module = module;
            if (module != null) { 
                for (Setting s : module.getSettings()) settingComponents.add(new SettingComponent(s));
                toggleAnimation.setValue(module.isEnabled() ? 1f : 0f);
            }
            expandAnimation.setValue(0f);
        }

        public String getName() { return module != null ? module.getName() : ""; }

        public void draw(DrawContext context, int mouseX, int mouseY, float alpha) {
            toggleAnimation.update();
            expandAnimation.update();

            Matrix4f mx = context.getMatrices().peek().getPositionMatrix();

            int bg     = ThemeManager.getInstance().getPalette().getBackground();
            int accent = ThemeManager.getInstance().getPrimary();
            int textP  = ThemeManager.getInstance().getPalette().getTextPrimary();
            int textS  = ThemeManager.getInstance().getPalette().getTextSecondary();

            float tv = toggleAnimation.getValue();

            Color ac = new Color(accent);
            Color moduleBg = new Color(24, 23, 23);

            float ev = expandAnimation.getValue();
            float rowH = getHeight();

            
            QuadRadiusState rowRadius = ev > 0.01f ? new QuadRadiusState(4, 4, 0, 0) : new QuadRadiusState(4);

            
            Builder.rectangle()
                    .size(new SizeState(width, MODULE_ROW_HEIGHT))
                    .radius(rowRadius)
                    .smoothness(1f)
                    .color(new QuadColorState(applyAlpha(moduleBg, alpha)))
                    .build().render(mx, x, y);
            if (ev > 0.01f) {
                Builder.rectangle()
                        .size(new SizeState(width, rowH - MODULE_ROW_HEIGHT))
                        .radius(new QuadRadiusState(0, 0, 4, 4))
                        .smoothness(1f)
                        .color(new QuadColorState(applyAlpha(moduleBg, alpha)))
                        .build().render(mx, x, y + MODULE_ROW_HEIGHT);
            }

            ShadowUtil.dark(width,MODULE_ROW_HEIGHT,rowRadius);
            Builder.border()
                    .size(new SizeState(width, rowH))
                    .radius(new QuadRadiusState(4))
                    .color(new QuadColorState(applyAlpha(new Color(66, 66, 66), alpha * (0.55f + 0.45f * tv))))
                    .thickness(0.3f)
                    .smoothness(1f, 1f)
                    .build().render(mx, x, y);

            
            Color nameColor = Color.WHITE;
            Builder.text()
                    .text(module.getName())
                    .font(BIKO_FONT.get())
                    .size(10f)
                    .color(applyAlpha(nameColor, alpha))
                    .thickness(0.04f)
                    .build().render(mx, x + 9f, y + 6f);

            
            String desc = module.getDesc();
            if (!desc.isEmpty()) {
                String[] lines = wrapText(desc, width - 18f, 7f, 2);
                for (int li = 0; li < lines.length; li++) {
                    Builder.text()
                            .text(lines[li])
                            .font(BIKO_FONT.get())
                            .size(7f)
                            .color(applyAlpha(new Color(textS), alpha * 0.7f))
                            .thickness(0.05f)
                            .build().render(mx, x + 9f, y + 18f + li * 9f);
                }
            }

            
            float trackX = x + width - 31f;
            float trackY = y + 6f;
            Color trackBg = tv > 0.5f ? ac : darker(new Color(bg), 0.6f);
            new RectangleBuilder()
                    .size(new SizeState(28f, 11f))
                    .radius(new QuadRadiusState(5.5f))
                    .color(new QuadColorState(applyAlpha(trackBg, alpha)))
                    .build().render(mx, trackX, trackY);

            float thumbX = trackX + 2f + tv * 12f;
            new RectangleBuilder()
                    .size(new SizeState(13f, 7f))
                    .radius(new QuadRadiusState(3.5f))
                    .color(new QuadColorState(applyAlpha(Color.WHITE, alpha * 0.95f)))
                    .build().render(mx, thumbX, trackY + 2f);

            
            boolean isBinding = ClickGuiScreen.this.bindingModule == module;
            String bindText = isBinding ? "..." : (module.getKeybind() > 0 ? getKeyName(module.getKeybind()) : "None");
            float btw = BIKO_FONT.get().getWidth(bindText, 7f);
            float btnH = 16f;
            float btnW = Math.max(18f, btw + 10f);

            String bindLabel = "bind:";
            float blw = BIKO_FONT.get().getWidth(bindLabel, 7f);
            float groupW = blw + 6f + btnW;
            float btnY = y + 20f;
            float btnX = x + width - 8f - groupW + blw + 6f;
            float labelX = x + width - 8f - groupW;

            boolean bindHovered = HoverUtil.isHovered(mouseX, mouseY, btnX, btnY, btnW, btnH);
            int btnA = isBinding ? 90 : bindHovered ? 60 : 32;
            Color btnBg = Color.BLACK;
            new RectangleBuilder()
                    .size(new SizeState(btnW, btnH))
                    .radius(new QuadRadiusState(2f)).smoothness(1f)
                    .color(new QuadColorState(applyAlpha(btnBg, alpha * 200f)))
                    .build().render(mx, btnX, btnY);

            Builder.text()
                    .text(bindText)
                    .font(BIKO_FONT.get())
                    .size(7f)
                    .color(applyAlpha(isBinding ? new Color(textP) : new Color(textS), alpha))
                    .thickness(0.06f)
                    .build().render(mx, btnX + (btnW - btw) / 2f, btnY + (btnH - 7f) / 2f + 1f);

            if (ev > 0.01f) {
                context.enableScissor(
                        (int) x,
                        (int)(y + MODULE_ROW_HEIGHT),
                        (int)(x + width),
                        (int)(y + getHeight())
                );
                float curY = y + MODULE_ROW_HEIGHT + 3f;
                for (SettingComponent sc : settingComponents) {
                    if (!sc.setting.visible()) continue;
                    sc.x     = x + 9f;
                    sc.y     = curY;
                    sc.width = width - 18f;
                    sc.draw(context, mouseX, mouseY, alpha * ev);
                    curY += sc.getHeight() + 3f;
                }
                context.disableScissor();
            }
        }

        public boolean mouseClicked(double mouseX, double mouseY, int button) {
            if (button == 0 && isOverBindButton(mouseX, mouseY)) {
                ClickGuiScreen.this.bindingModule = ClickGuiScreen.this.bindingModule == module ? null : module;
                return true;
            }
            if (HoverUtil.isHovered(mouseX, mouseY, x, y, width, MODULE_ROW_HEIGHT)) {
                if (button == 0 && module != null) {
                    module.toggle();
                    toggleAnimation.start(toggleAnimation.getValue(),
                            module.isEnabled() ? 1f : 0f, 180, Easing.CUBIC_OUT);
                    return true;
                } else if (button == 1) {
                    expanded = !expanded;
                    expandAnimation.start(expandAnimation.getValue(),
                            expanded ? 1f : 0f, 250, Easing.CUBIC_IN_OUT);
                    return true;
                } else if (button == 2) {
                    ClickGuiScreen.this.bindingModule = ClickGuiScreen.this.bindingModule == module ? null : module;
                    return true;
                }
            }
            if (expanded || expandAnimation.getValue() > 0.01f)
                for (SettingComponent sc : settingComponents)
                    if (sc.setting.visible() && sc.mouseClicked(mouseX, mouseY, button)) return true;
            return false;
        }

        private boolean isOverBindButton(double mouseX, double mouseY) {
            String bindText = ClickGuiScreen.this.bindingModule == module ? "..."
                    : (module.getKeybind() > 0 ? getKeyName(module.getKeybind()) : "None");
            float btw = BIKO_FONT.get().getWidth(bindText, 7f);
            float btnW = Math.max(18f, btw + 10f);
            float blw = BIKO_FONT.get().getWidth("bind:", 7f);
            float groupW = blw + 6f + btnW;
            float btnX = x + width - 8f - groupW + blw + 6f;
            return HoverUtil.isHovered(mouseX, mouseY, btnX, y + 20f, btnW, 16f);
        }

        private String[] wrapText(String text, float maxWidth, float size, int maxLines) {
            String[] words = text.split(" ");
            StringBuilder cur = new StringBuilder();
            List<String> lines = new ArrayList<>();
            for (String w : words) {
                String test = cur.length() == 0 ? w : cur + " " + w;
                if (cur.length() > 0 && BIKO_FONT.get().getWidth(test, size) > maxWidth) {
                    lines.add(cur.toString());
                    cur = new StringBuilder(w);
                } else {
                    cur = new StringBuilder(test);
                }
            }
            if (cur.length() > 0) lines.add(cur.toString());

            if (lines.size() > maxLines) {
                lines = new ArrayList<>(lines.subList(0, maxLines));
                String last = lines.get(maxLines - 1);
                String ell = "...";
                float ellW = BIKO_FONT.get().getWidth(ell, size);
                while (last.length() > 0 && BIKO_FONT.get().getWidth(last, size) > maxWidth - ellW) {
                    last = last.substring(0, last.length() - 1);
                }
                lines.set(maxLines - 1, last + ell);
            }
            return lines.toArray(new String[0]);
        }

        public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
            if (expanded)
                for (SettingComponent sc : settingComponents)
                    if (sc.keyPressed(keyCode, scanCode, modifiers)) return true;
            return false;
        }

        public float getHeight() {
            float h = MODULE_ROW_HEIGHT;
            if (!settingComponents.isEmpty()) {
                float sh = 3f;
                for (SettingComponent sc : settingComponents)
                    if (sc.setting.visible()) sh += sc.getHeight() + 3f;
                h += sh * expandAnimation.getValue();
            }
            return h;
        }
    }

    
    
    private class ThemeComponent extends ModuleComponent {
        private final Theme theme;

        public ThemeComponent(Theme t) { super(null); this.theme = t; }

        @Override public String getName() { return theme.getName(); }

        @Override
        public void draw(DrawContext context, int mouseX, int mouseY, float alpha) {
            Matrix4f mx = context.getMatrices().peek().getPositionMatrix();
            boolean hovered = HoverUtil.isHovered(mouseX, mouseY, x, y, width, MODULE_HEIGHT);
            boolean active  = ThemeManager.getInstance().getCurrentTheme() == theme;

            int bg     = ThemeManager.getInstance().getPalette().getBackground();
            int accent = theme.getPalette().getPrimary();
            int textP  = ThemeManager.getInstance().getPalette().getTextPrimary();
            int textS  = ThemeManager.getInstance().getPalette().getTextSecondary();

            Color rowBg = hovered ? lighter(new Color(bg), 0.04f) : new Color(bg);
            new RectangleBuilder()
                    .size(new SizeState(width, MODULE_HEIGHT))
                    .radius(new QuadRadiusState(2))
                    .color(new QuadColorState(applyAlpha(rowBg, alpha * 0.95f)))
                    .build().render(mx, x, y);

            new RectangleBuilder()
                    .size(new SizeState(3f, MODULE_HEIGHT - 6f))
                    .radius(new QuadRadiusState(1.5f))
                    .color(new QuadColorState(applyAlpha(new Color(accent), alpha)))
                    .build().render(mx, x + 4f, y + 3f);

            Color nameColor = active ? new Color(accent) : new Color(textS);
            Builder.text()
                    .text(theme.getName())
                    .font(BIKO_FONT.get())
                    .size(9f)
                    .color(applyAlpha(nameColor, alpha))
                    .thickness(active ? 0.04f : 0.06f)
                    .build().render(mx, x + 12f, y + 5.5f);
        }

        @Override
        public boolean mouseClicked(double mouseX, double mouseY, int button) {
            if (HoverUtil.isHovered(mouseX, mouseY, x, y, width, MODULE_HEIGHT) && button == 0) {
                ThemeManager.getInstance().setTheme(theme);
                return true;
            }
            return false;
        }

        @Override public float getHeight() { return MODULE_HEIGHT; }
    }

    
    
    private class ThemeHeaderComponent extends ModuleComponent {
        private final String text;
        private final boolean isSearch;

        public ThemeHeaderComponent(String text, boolean isSearch) {
            super(null); this.text = text; this.isSearch = isSearch;
        }

        @Override
        public void draw(DrawContext context, int mouseX, int mouseY, float alpha) {
            Matrix4f mx = context.getMatrices().peek().getPositionMatrix();
            int textS = ThemeManager.getInstance().getPalette().getTextSecondary();
            Builder.text()
                    .text(text)
                    .font(BIKO_FONT.get())
                    .size(8f)
                    .color(applyAlpha(new Color(textS), alpha * 0.6f))
                    .thickness(0.07f)
                    .build().render(mx, x + 4f, y + (isSearch ? 3f : 2f));
        }

        @Override public float getHeight() { return isSearch ? 16f : 13f; }
    }

    
    
    private class ThemeCreateComponent extends ModuleComponent {
        public ThemeCreateComponent() { super(null); }

        @Override
        public void draw(DrawContext context, int mouseX, int mouseY, float alpha) {
            Matrix4f mx = context.getMatrices().peek().getPositionMatrix();
            boolean hovered = HoverUtil.isHovered(mouseX, mouseY, x, y, width, MODULE_HEIGHT);
            int accent = ThemeManager.getInstance().getPrimary();
            Color ac = new Color(accent);

            Color bg = new Color(ac.getRed(), ac.getGreen(), ac.getBlue(), hovered ? 35 : 20);
            new RectangleBuilder()
                    .size(new SizeState(width, MODULE_HEIGHT))
                    .radius(new QuadRadiusState(2))
                    .color(new QuadColorState(applyAlpha(bg, alpha)))
                    .build().render(mx, x, y);

            Builder.text()
                    .text("+ Create (dev)")
                    .font(BIKO_FONT.get())
                    .size(9f)
                    .color(applyAlpha(ac, alpha * 0.85f))
                    .thickness(0.06f)
                    .build().render(mx, x + 7f, y + 5.5f);
        }

        @Override
        public boolean mouseClicked(double mouseX, double mouseY, int button) {
            return HoverUtil.isHovered(mouseX, mouseY, x, y, width, MODULE_HEIGHT) && button == 0;
        }

        @Override public float getHeight() { return MODULE_HEIGHT; }
    }

    
    
    private class SettingComponent {
        private final Setting setting;
        private float x, y, width;
        private boolean sliding;

        public SettingComponent(Setting setting) { this.setting = setting; }

        public void draw(DrawContext context, int mouseX, int mouseY, float alpha) {
            Matrix4f mx = context.getMatrices().peek().getPositionMatrix();

            int accent = ThemeManager.getInstance().getPrimary();
            int textP  = ThemeManager.getInstance().getPalette().getTextPrimary();
            int textS  = ThemeManager.getInstance().getPalette().getTextSecondary();
            int bg     = ThemeManager.getInstance().getPalette().getBackground();

            Color ac   = new Color(accent);
            Color tCol = new Color(textP);
            Color sCol = new Color(textS);

            if (setting instanceof BooleanSetting bs) {
                Builder.text().text(bs.getName()).font(BIKO_FONT.get()).size(8.5f)
                        .color(applyAlpha(tCol, alpha)).thickness(0.06f).build().render(mx, x, y + 2f);
                Color boxBg = bs.getValue() ? ac : darker(new Color(bg), 0.6f);
                new RectangleBuilder().size(new SizeState(9f, 9f)).radius(new QuadRadiusState(1.5f))
                        .color(new QuadColorState(applyAlpha(boxBg, alpha))).build().render(mx, x + width - 9f, y + 1f);

            } else if (setting instanceof ModeSetting ms) {
                Builder.text().text(ms.getName()).font(BIKO_FONT.get()).size(8.5f)
                        .color(applyAlpha(tCol, alpha)).thickness(0.06f).build().render(mx, x, y + 2f);
                String val = ms.getValue();
                float vw = BIKO_FONT.get().getWidth(val, 8f);
                Builder.text().text(val).font(BIKO_FONT.get()).size(8f)
                        .color(applyAlpha(ac, alpha)).thickness(0.05f).build().render(mx, x + width - vw, y + 2f);

            } else if (setting instanceof SliderSetting ss) {
                Builder.text().text(ss.getName()).font(BIKO_FONT.get()).size(8.5f)
                        .color(applyAlpha(tCol, alpha)).thickness(0.06f).build().render(mx, x, y);
                String val = ss.getValueAsString();
                float vw = BIKO_FONT.get().getWidth(val, 8f);
                Builder.text().text(val).font(BIKO_FONT.get()).size(8f)
                        .color(applyAlpha(sCol, alpha)).thickness(0.05f).build().render(mx, x + width - vw, y);

                float barY = y + 9f;
                new RectangleBuilder().size(new SizeState(width, 4f)).radius(new QuadRadiusState(1f))
                        .color(new QuadColorState(applyAlpha(darker(new Color(bg), 0.5f), alpha))).build().render(mx, x, barY);

                double pct = (ss.getValue() - ss.getMin()) / (ss.getMax() - ss.getMin());
                new RectangleBuilder().size(new SizeState((float)(width * pct), 4f)).radius(new QuadRadiusState(1f))
                        .color(new QuadColorState(applyAlpha(ac, alpha))).build().render(mx, x, barY);

                float thumbX = x + (float)(width * pct) - 4.5f;
                new RectangleBuilder().size(new SizeState(9f, 6f)).radius(new QuadRadiusState(3f))
                        .color(new QuadColorState(applyAlpha(ac, alpha))).build().render(mx, thumbX, barY - 1f);

                if (sliding && GLFW.glfwGetMouseButton(mc.getWindow().getHandle(), GLFW.GLFW_MOUSE_BUTTON_LEFT) == GLFW.GLFW_PRESS) {
                    ss.setValue(ss.getMin() + Math.max(0, Math.min(1, (mouseX - x) / width)) * (ss.getMax() - ss.getMin()));
                } else sliding = false;

            } else if (setting instanceof NumberSetting ns) {
                Builder.text().text(ns.getName()).font(BIKO_FONT.get()).size(8.5f)
                        .color(applyAlpha(tCol, alpha)).thickness(0.06f).build().render(mx, x, y);
                String val = ns.getValueAsString();
                float vw = BIKO_FONT.get().getWidth(val, 8f);
                Builder.text().text(val).font(BIKO_FONT.get()).size(8f)
                        .color(applyAlpha(sCol, alpha)).thickness(0.05f).build().render(mx, x + width - vw, y);

                float barY = y + 9f;
                new RectangleBuilder().size(new SizeState(width, 4f)).radius(new QuadRadiusState(1f))
                        .color(new QuadColorState(applyAlpha(darker(new Color(bg), 0.5f), alpha))).build().render(mx, x, barY);
                double pct = (ns.getValue() - ns.getMin()) / (ns.getMax() - ns.getMin());
                new RectangleBuilder().size(new SizeState((float)(width * pct), 4f)).radius(new QuadRadiusState(1f))
                        .color(new QuadColorState(applyAlpha(ac, alpha))).build().render(mx, x, barY);
                float thumbX = x + (float)(width * pct) - 4.5f;
                new RectangleBuilder().size(new SizeState(9f, 6f)).radius(new QuadRadiusState(3f))
                        .color(new QuadColorState(applyAlpha(ac, alpha))).build().render(mx, thumbX, barY - 1f);

                if (sliding && GLFW.glfwGetMouseButton(mc.getWindow().getHandle(), GLFW.GLFW_MOUSE_BUTTON_LEFT) == GLFW.GLFW_PRESS) {
                    ns.setValue(ns.getMin() + Math.max(0, Math.min(1, (mouseX - x) / width)) * (ns.getMax() - ns.getMin()));
                } else sliding = false;

            } else if (setting instanceof KeySetting ks) {
                Builder.text().text(ks.getName()).font(BIKO_FONT.get()).size(8.5f)
                        .color(applyAlpha(tCol, alpha)).thickness(0.06f).build().render(mx, x, y + 2f);
                String keyName;
                if (ClickGuiScreen.this.bindingKeySetting == ks) {
                    keyName = "...";
                } else {
                int k = ks.getKey();
                if (k == -1) {
                    keyName = "NONE";
                } else if (k >= 0 && k <= GLFW.GLFW_MOUSE_BUTTON_LAST) {
                    keyName = KeySetting.mouseButtonName(k);
                } else if (k >= GLFW.GLFW_KEY_SPACE) {
                    keyName = GLFW.glfwGetKeyName(k, 0);
                    if (keyName == null) keyName = "KEY_" + k;
                } else {
                    keyName = "KEY_" + k;
                }
                }
                float vw = BIKO_FONT.get().getWidth(keyName, 8f);
                Builder.text().text(keyName).font(BIKO_FONT.get()).size(8f)
                        .color(applyAlpha(ac, alpha)).thickness(0.05f).build().render(mx, x + width - vw, y + 2f);

            } else if (setting instanceof ListSetting ls) {
                Builder.text().text(ls.getName()).font(BIKO_FONT.get()).size(8.5f)
                        .color(applyAlpha(tCol, alpha)).thickness(0.06f).build().render(mx, x, y + 2f);

                float optY = y + 12f;
                for (String opt : ls.getOptions()) {
                    boolean hovered = HoverUtil.isHovered(mouseX, mouseY, x, optY - 0.5f, width, 10f);
                    Color checkboxBg = ls.isSelected(opt) ? ac : darker(new Color(bg), 0.6f);
                    if (hovered) checkboxBg = ls.isSelected(opt)
                            ? new Color(ac.getRed(), ac.getGreen(), ac.getBlue(), 220)
                            : lighter(darker(new Color(bg), 0.6f), 0.1f);

                    new RectangleBuilder().size(new SizeState(8f, 8f)).radius(new QuadRadiusState(1.5f))
                            .color(new QuadColorState(applyAlpha(checkboxBg, alpha))).build().render(mx, x, optY);

                    if (ls.isSelected(opt)) {
                        new RectangleBuilder().size(new SizeState(3f, 3f)).radius(new QuadRadiusState(1f))
                                .color(new QuadColorState(applyAlpha(Color.WHITE, alpha * 0.9f))).build().render(mx, x + 2.5f, optY + 2.5f);
                    }

                    Color optColor = ls.isSelected(opt) ? tCol : sCol;
                    Builder.text().text(opt).font(BIKO_FONT.get()).size(8f)
                            .color(applyAlpha(optColor, alpha * 0.9f)).thickness(0.06f).build().render(mx, x + 12f, optY + 0.5f);

                    optY += 10f;
                }

            } else if (setting instanceof StringSetting ss) {
                Builder.text().text(ss.getName()).font(BIKO_FONT.get()).size(8.5f)
                        .color(applyAlpha(tCol, alpha)).thickness(0.06f).build().render(mx, x, y + 2f);

                float boxY = y + 12f;
                float boxH = 11f;

                new RectangleBuilder()
                        .size(new SizeState(width, boxH))
                        .radius(new QuadRadiusState(1.5f))
                        .color(new QuadColorState(applyAlpha(new Color(0, 0, 0), alpha * 0.9f)))
                        .build().render(mx, x, boxY);

                if (ClickGuiScreen.this.focusedStringComponent == this) {
                    new RectangleBuilder()
                            .size(new SizeState(width, 1f))
                            .radius(new QuadRadiusState(0))
                            .color(new QuadColorState(applyAlpha(ac, alpha * 0.7f)))
                            .build().render(mx, x, boxY + boxH);
                }

                String display = ss.getText().isEmpty() ? "..." : ss.getText();
                Color dispColor = ss.getText().isEmpty() ? sCol : tCol;
                Builder.text()
                        .text(display)
                        .font(BIKO_FONT.get())
                        .size(8f)
                        .color(applyAlpha(dispColor, alpha * (ss.getText().isEmpty() ? 0.4f : 1f)))
                        .thickness(0.05f)
                        .build().render(mx, x + 5f, boxY + 2f);

                if (ClickGuiScreen.this.focusedStringComponent == this && (System.currentTimeMillis() / 530) % 2 == 0) {
                    float tw = BIKO_FONT.get().getWidth(ss.getText(), 8f);
                    new RectangleBuilder()
                            .size(new SizeState(1f, 8f))
                            .color(new QuadColorState(applyAlpha(Color.WHITE, alpha * 0.8f)))
                            .build().render(mx, x + 5f + tw, boxY + 2f);
                }
            }
        }

        public boolean mouseClicked(double mouseX, double mouseY, int button) {
            if (HoverUtil.isHovered(mouseX, mouseY, x, y, width, getHeight())) {
                if (setting instanceof BooleanSetting bs) { bs.setValue(!bs.getValue()); return true; }
                if (setting instanceof ModeSetting ms)    { ms.cycle(); return true; }
                if (setting instanceof SliderSetting)     { sliding = true; return true; }
                if (setting instanceof NumberSetting)     { sliding = true; return true; }
                if (setting instanceof KeySetting) {
                    ClickGuiScreen.this.bindingKeySetting = (KeySetting) setting;
                    return true;
                }
                if (setting instanceof ListSetting ls) {
                    float optY = y + 12f;
                    for (String opt : ls.getOptions()) {
                        if (mouseY >= optY - 0.5f && mouseY < optY + 10f) {
                            ls.toggle(opt);
                            return true;
                        }
                        optY += 10f;
                    }
                }
                if (setting instanceof StringSetting) {
                    ClickGuiScreen.this.focusedStringComponent = SettingComponent.this;
                    return true;
                }
            }
            return false;
        }

        public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
            if (setting instanceof StringSetting && ClickGuiScreen.this.focusedStringComponent == this) {
                if (keyCode == GLFW.GLFW_KEY_BACKSPACE) {
                    StringSetting ss = (StringSetting) setting;
                    String text = ss.getText();
                    if (!text.isEmpty()) ss.setText(text.substring(0, text.length() - 1));
                    return true;
                }
                return true;
            }
            return false;
        }

        public boolean charTyped(char c, int modifiers) {
            if (setting instanceof StringSetting && ClickGuiScreen.this.focusedStringComponent == this) {
                ((StringSetting) setting).setText(((StringSetting) setting).getText() + c);
                return true;
            }
            return false;
        }

        public float getHeight() {
            if (setting instanceof SliderSetting || setting instanceof NumberSetting) return 14f;
            if (setting instanceof ListSetting ls) return 12f + ls.getOptions().size() * 10f;
            if (setting instanceof StringSetting) return 25f;
            return 11f;
        }
    }

    
    
    private class SearchComponent {
        private float x, y, width;
        private String text = "";
        private boolean focused;

        public void draw(DrawContext context, int mouseX, int mouseY, float alpha) {
            Matrix4f mx = context.getMatrices().peek().getPositionMatrix();
            int bg     = ThemeManager.getInstance().getPalette().getBackground();
            int accent = ThemeManager.getInstance().getPrimary();
            int textP  = ThemeManager.getInstance().getPalette().getTextPrimary();
            int textS  = ThemeManager.getInstance().getPalette().getTextSecondary();

            new LiquidBuilder()
                    .size(new SizeState(width, 20f))
                    .radius(new QuadRadiusState(2))
                    .color(new QuadColorState(applyAlpha(darker(new Color(bg), 0.8f), alpha * 0.97f)))
                    .build().render(mx, x, y);

            if (focused) {
                new LiquidBuilder()
                        .size(new SizeState(width, 1f))
                        .radius(new QuadRadiusState(0))
                        .color(new QuadColorState(applyAlpha(new Color(accent), alpha * 0.7f)))
                        .build().render(mx, x, y + 19f);
            }

            String display = text.isEmpty() ? "Search…" : text;
            Color  color   = text.isEmpty() ? new Color(textS) : new Color(textP);
            Builder.text()
                    .text(display)
                    .font(BIKO_FONT.get())
                    .size(9f)
                    .color(applyAlpha(color, alpha * (text.isEmpty() ? 0.4f : 1f)))
                    .thickness(0.06f)
                    .build().render(mx, x + 7f, y + 5.5f);

            if (focused && (System.currentTimeMillis() / 530) % 2 == 0) {
                float tw = BIKO_FONT.get().getWidth(text, 9f);
                new LiquidBuilder()
                        .size(new SizeState(1f, 9f))
                        .color(new QuadColorState(applyAlpha(Color.WHITE, alpha * 0.8f)))
                        .build().render(mx, x + 7f + tw, y + 5.5f);
            }
        }

        public boolean mouseClicked(double mouseX, double mouseY, int button) {
            focused = HoverUtil.isHovered(mouseX, mouseY, x, y, width, 20f);
            return focused;
        }

        public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
            if (!focused) return false;
            if (keyCode == GLFW.GLFW_KEY_BACKSPACE && !text.isEmpty()) {
                text = text.substring(0, text.length() - 1);
                return true;
            }
            return false;
        }

        public boolean charTyped(char c, int modifiers) {
            if (!focused) return false;
            text += c;
            return true;
        }

        public String getText()          { return text; }
        public void setX(float x)       { this.x = x; }
        public void setY(float y)       { this.y = y; }
        public void setWidth(float w)   { this.width = w; }
    }
}