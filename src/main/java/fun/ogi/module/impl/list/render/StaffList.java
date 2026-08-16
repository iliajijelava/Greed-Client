package fun.ogi.module.impl.list.render;

import com.google.common.base.Suppliers;
import fun.ogi.Cheap;
import fun.ogi.module.theme.ThemeManager;
import fun.ogi.util.animation.Animation;
import fun.ogi.util.animation.Easing;
import fun.ogi.util.render.Draggable;
import fun.ogi.util.render.ShadowUtil;
import fun.ogi.util.render.builders.Builder;
import fun.ogi.util.render.builders.states.QuadColorState;
import fun.ogi.util.render.builders.states.QuadRadiusState;
import fun.ogi.util.render.builders.states.SizeState;
import fun.ogi.util.render.msdf.MsdfFont;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ChatScreen;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.client.texture.AbstractTexture;
import net.minecraft.client.util.DefaultSkinHelper;
import net.minecraft.scoreboard.Team;
import net.minecraft.util.Identifier;
import org.joml.Matrix4f;

import java.awt.*;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class StaffList {
    private final MinecraftClient mc = MinecraftClient.getInstance();
    public final Draggable draggable = new Draggable(50, 50, 100, 20);
    private static final Supplier<MsdfFont> BIKO_FONT = Suppliers.memoize(() -> MsdfFont.builder().atlas("biko").data("biko").build());
    private static final Supplier<MsdfFont> ICON_FONT = Suppliers.memoize(() -> MsdfFont.builder().atlas("nur").data("nur").build());

    private final List<Staff> staffPlayers = new ArrayList<>();
    private final Pattern namePattern = Pattern.compile("^\\w{3,16}$");
    private final Pattern prefixMatches = Pattern.compile(".*(mod|der|adm|help|wne|хелп|адм|поддержка|кура|own|staf|curat|dev|supp|yt|гл.мод|мл.мод|мл.сотруд|ст.сотруд|стажёр|стажер|сотруд).*");

    private final Animation sizeAnimation = new Animation();
    private final Animation youGameAlpha = new Animation();
    private final Map<String, Animation> youGameRows = new HashMap<>();
    private final Map<String, Identifier> youGameSkins = new HashMap<>();
    private long lastSkinClear;
    private boolean wasVisible;

    public StaffList() {
        sizeAnimation.setValue(0f);
    }

    public void render(DrawContext context, String style, String mode, int mouseX, int mouseY, int screenWidth, int screenHeight) {
        updateStaffList();

        boolean shouldBeVisible = !staffPlayers.isEmpty() || (mc.currentScreen instanceof ChatScreen);

        if (shouldBeVisible && !wasVisible) {
            sizeAnimation.start(sizeAnimation.getValue(), 1f, 250, Easing.QUART_OUT);
        } else if (!shouldBeVisible && wasVisible) {
            sizeAnimation.start(sizeAnimation.getValue(), 0f, 250, Easing.QUART_OUT);
        }

        wasVisible = shouldBeVisible;
        sizeAnimation.update();

        float animScale = sizeAnimation.getValue();
        if (animScale <= 0.01f) return;

        if (mode.equals("Macan")) {
            renderMacan(context, mouseX, mouseY, screenWidth, screenHeight, animScale);
            return;
        }

        if (mode.equals("YouGame")) {
            renderYouGameStaff(context, mouseX, mouseY, screenWidth, screenHeight);
            return;
        }

        context.getMatrices().push();
        float cx = draggable.getX() + draggable.getWidth() / 2f;
        float cy = draggable.getY() + draggable.getHeight() / 2f;
        context.getMatrices().translate(cx, cy, 0);
        context.getMatrices().scale(animScale, animScale, 1);
        context.getMatrices().translate(-cx, -cy, 0);

        Matrix4f matrix = context.getMatrices().peek().getPositionMatrix();

        Color accentColor = new Color(ThemeManager.getInstance().getPrimary());
        Color textColor = Color.WHITE;
        Color hudBg = new Color(ThemeManager.getInstance().getPalette().getHudBackground());
        hudBg = new Color(hudBg.getRed(), hudBg.getGreen(), hudBg.getBlue(), 255);
        Color sepColor = new Color(166, 166, 166, 255);
        float ts = 9f;
        float iconSz = 8f;
        float headerH = 16f;
        float itemH = 13f + 4;
        float gap = 2f;
        float rounding = 3f;
        float pad = 5f;
        float sepW = 0.5f;

        List<Staff> displayList = new ArrayList<>(staffPlayers);
        if (displayList.isEmpty() && mc.currentScreen instanceof ChatScreen) {
            displayList.add(new Staff("§cADMIN", "ExampleStaff", false));
        }

        float maxStatusWidth = 0f;
        for (Staff s : displayList) {
            String statusLabel = s.isVanish ? "Vanish" : "Online";
            float sw = BIKO_FONT.get().getWidth(statusLabel, 8f);
            if (sw > maxStatusWidth) maxStatusWidth = sw;
        }

        float minW = BIKO_FONT.get().getWidth("Staffs", ts) + 40f;
        float maxNameW = 0f;
        for (Staff s : displayList) {
            String fullText = (s.prefix.isEmpty() ? "" : s.prefix + " ") + s.name;
            float nw = BIKO_FONT.get().getWidth(fullText, ts);
            if (nw > maxNameW) maxNameW = nw;
        }
        float contentW = maxNameW + sepW + pad + maxStatusWidth + pad;
        float totalW = Math.max(minW, contentW) + 7;
        int rowCount = displayList.size();
        float totalH = headerH + (rowCount > 0 ? gap + rowCount * (itemH + gap) : 0);

        draggable.setWidth(totalW);
        draggable.setHeight(totalH);

        float x = draggable.getX();
        float y = draggable.getY();

        if (mode.equals("Macan")) {
            Color macanBg = new Color(22, 22, 22, 200);
            Builder.blur().size(new SizeState(totalW, totalH)).radius(new QuadRadiusState(rounding))
                    .color(new QuadColorState(macanBg)).blurRadius(10).smoothness(1f).build().render(matrix, x, y);
        } else if (mode.equals("Old")) {
            Color gradStart = new Color(ThemeManager.getInstance().getPrimary());
            Color gradEnd = new Color(ThemeManager.getInstance().getSecondary());
            ShadowUtil.gradient(gradStart, gradEnd, totalW, headerH, new QuadRadiusState(rounding)).render(matrix, x, y, 0);
            Builder.rectangle()
                    .size(new SizeState(totalW, headerH))
                    .radius(new QuadRadiusState(rounding))
                    .color(new QuadColorState(gradStart, gradEnd, gradStart, gradEnd))
                    .build().render(matrix, x, y);
        } else if (style.equals("Liquid Glass")) {
            Builder.liquid().size(new SizeState(totalW, totalH)).radius(new QuadRadiusState(4))
                    .color(new QuadColorState(new Color(255, 255, 255, 255))).build().render(matrix, x, y);
        } else if (style.equals("Colored Liquid")) {
            Builder.coloredLiquid().size(new SizeState(totalW, totalH)).radius(new QuadRadiusState(4))
                    .color(new QuadColorState(new Color(hudBg.getRed(), hudBg.getGreen(), hudBg.getBlue(), 150))).build().render(matrix, x, y);
        } else if (style.equals("Default")) {
            ShadowUtil.dark(totalW, totalH, new QuadRadiusState(4)).render(matrix, x, y, 0);
            Builder.rectangle().size(new SizeState(totalW, totalH)).radius(new QuadRadiusState(4)).color(new QuadColorState(new Color(20, 20, 20, 255))).build().render(matrix, x, y);
        } else {
            Builder.blur().size(new SizeState(totalW, totalH)).radius(new QuadRadiusState(rounding))
                    .color(new QuadColorState(hudBg)).blurRadius(10).smoothness(1f).build().render(matrix, x, y);
        }

        if (!mode.equals("Macan")) {
            float iconCharW = ICON_FONT.get().getWidth("W", iconSz);
            Builder.text().text("W").font(ICON_FONT.get()).size(iconSz).thickness(0.08f).color(textColor)
                    .build().render(matrix, x + pad, y + (headerH - iconSz) / 2f);
            float titleX = x + pad + iconCharW + 3f;
            Builder.text().text("Staffs").font(BIKO_FONT.get()).size(ts).thickness(0.06f).color(textColor)
                    .build().render(matrix, titleX, y + (headerH - ts) / 2f);
        }

        float curY = y + (mode.equals("Macan") ? 0f : headerH + gap);
        for (Staff staff : displayList) {
            String fullText = (staff.prefix.isEmpty() ? "" : staff.prefix + " ") + staff.name;
            String statusLabel = staff.isVanish ? "Vanish" : "Online";
            Color statusColor = staff.isVanish ? new Color(255, 85, 85) : new Color(85, 255, 85);
            if (staff.name.equals("ExampleStaff")) {
                statusColor = (System.currentTimeMillis() / 500 % 2 == 0) ? new Color(85, 255, 85) : new Color(255, 85, 85);
            }

            if (mode.equals("Macan")) {
                float nameSize = 7f;
                float labelSize = 6f;
                Builder.text().text(fullText).font(BIKO_FONT.get()).size(nameSize).thickness(0.06f).color(textColor)
                        .build().render(matrix, x + pad, curY + (itemH - nameSize) / 2f);
                float statusW = BIKO_FONT.get().getWidth(statusLabel, labelSize);
                float statusX = x + totalW - pad - statusW -3;
                Builder.text().text(statusLabel).font(BIKO_FONT.get()).size(labelSize).thickness(0.06f).color(statusColor)
                        .build().render(matrix, statusX, curY + (itemH - labelSize) / 2f);
            } else if (mode.equals("Old")) {
                Color gradStart = new Color(ThemeManager.getInstance().getPrimary());
                Color gradEnd = new Color(ThemeManager.getInstance().getSecondary());
                ShadowUtil.gradient(gradStart, gradEnd, totalW, itemH, new QuadRadiusState(rounding)).render(matrix, x, curY, 0);
                Builder.rectangle().radius(new QuadRadiusState(rounding))
                        .size(new SizeState(totalW, itemH))
                        .color(new QuadColorState(new Color(20, 20, 20, 155)))
                        .build().render(matrix, x, curY);

                Builder.text().text(fullText).font(BIKO_FONT.get()).size(ts).thickness(0.06f).color(textColor)
                        .build().render(matrix, x + pad, curY + (itemH - ts) / 2f);

                float statusW = BIKO_FONT.get().getWidth(statusLabel, 8f);
                float statusX = x + totalW - pad - statusW + 3;
                Builder.text().text(statusLabel).font(BIKO_FONT.get()).size(8f).thickness(0.06f).color(statusColor)
                        .build().render(matrix, statusX, curY + (itemH - 8f) / 2f);
            } else {
                Builder.text().text(fullText).font(BIKO_FONT.get()).size(ts).thickness(0.06f).color(textColor)
                        .build().render(matrix, x + pad, curY + (itemH - ts) / 2f);

                float statusW = BIKO_FONT.get().getWidth(statusLabel, 8f);
                float statusX = x + totalW - pad - statusW;
                Builder.text().text(statusLabel).font(BIKO_FONT.get()).size(8f).thickness(0.06f).color(statusColor)
                        .build().render(matrix, statusX, curY + (itemH - 8f) / 2f);
            }

            curY += itemH + gap;
        }

        if (mc.currentScreen instanceof ChatScreen) {
            draggable.onDraw(mouseX, mouseY, screenWidth, screenHeight);
        }

        context.getMatrices().pop();
    }

    private void renderYouGameStaff(DrawContext context, int mouseX, int mouseY, int screenWidth, int screenHeight) {
        boolean chat = mc.currentScreen instanceof ChatScreen;
        boolean hasAny = !staffPlayers.isEmpty();

        youGameAlpha.update();
        youGameAlpha.start(youGameAlpha.getValue(), hasAny || chat ? 1f : 0f, 200, Easing.CUBIC_OUT);
        float alpha = youGameAlpha.getValue();
        if (alpha <= 0.01f) return;

        long now = System.currentTimeMillis();
        if (now - lastSkinClear > 30000) {
            youGameSkins.clear();
            lastSkinClear = now;
        }

        List<Staff> displayList = new ArrayList<>(staffPlayers);
        boolean fakeRow = chat && displayList.isEmpty();

        Set<String> keys = new HashSet<>();
        for (Staff s : displayList) keys.add(s.name);
        updateRowAnimations(youGameRows, keys);

        float posX = draggable.getX();
        float posY = draggable.getY();

        float headerW = 60f;
        float headPanelWidth = 13f;
        float gap = 1f;

        float maxNameWidth = 0f;
        for (Staff s : displayList) {
            String text = youGameNameText(s);
            float w = BIKO_FONT.get().getWidth(text, 6f);
            if (w > maxNameWidth) maxNameWidth = w;
        }
        if (fakeRow) {
            float w = BIKO_FONT.get().getWidth("Admin", 6f);
            if (w > maxNameWidth) maxNameWidth = w;
        }
        float rightPanelWidth = Math.max(maxNameWidth + 16f, 40f);
        float totalW = Math.max(headerW, headPanelWidth + gap + rightPanelWidth);
        float totalH = 14.5f + (fakeRow ? 1 : displayList.size()) * 13f;
        draggable.setWidth(totalW);
        draggable.setHeight(totalH);

        Matrix4f matrix = context.getMatrices().peek().getPositionMatrix();

        youGameHeader(matrix, posX, posY, headerW, "W", "Staff", alpha);
        posY += 14.5f;

        for (Staff s : displayList) {
            Animation anim = youGameRows.get(s.name);
            float a = anim != null ? anim.getValue() : 1f;
            if (a <= 0.01f) continue;

            float rowY = posY + a * 3f - 3f;
            float rowAlpha = a * alpha;

            String text = youGameNameText(s);
            float textWidth = BIKO_FONT.get().getWidth(text, 6f);
            float infoPanelWidth = textWidth + 24f;
            float maxAllowed = 60f - headPanelWidth - gap;
            float actualInfoWidth = Math.min(infoPanelWidth, maxAllowed);

            youGamePanel(matrix, posX, rowY, headPanelWidth, 13f, rowAlpha * 0.55f);

            drawYouGameHead(matrix, getYouGameSkin(s.name), posX + 2.5f, rowY + 2.5f, 8f, rowAlpha);

            youGamePanel(matrix, posX + headPanelWidth + gap, rowY, actualInfoWidth, 13f, rowAlpha * 0.55f);

            float baseY = rowY + (13f - 6f) / 2f;
            float prefixLeftX = posX + headPanelWidth + gap + 6f;
            Builder.text().text(text).font(BIKO_FONT.get()).size(6f).thickness(0.06f)
                    .color(youGameAlpha(Color.WHITE, rowAlpha)).build().render(matrix, prefixLeftX, baseY);

            float dotSize = 5f;
            float dotX = prefixLeftX + textWidth + 4f;
            float dotY = rowY + (13f - dotSize) / 2f;
            Color statusColor = s.isVanish ? new Color(231, 76, 60) : new Color(46, 204, 113);
            Builder.rectangle().size(new SizeState(dotSize, dotSize)).radius(new QuadRadiusState(2.5f))
                    .color(new QuadColorState(youGameAlpha(statusColor, rowAlpha))).build().render(matrix, dotX, dotY);

            posY += 13f * a;
        }

        if (fakeRow) {
            float rowY = posY - 1f;
            String fakePrefix = "Admin";
            float fakePrefixWidth = BIKO_FONT.get().getWidth(fakePrefix, 6f);
            float fakeInfoWidth = Math.min(fakePrefixWidth + 24f, 60f - headPanelWidth - gap);

            youGamePanel(matrix, posX, rowY, headPanelWidth, 13f, alpha * 0.55f);
            drawYouGameHead(matrix, DefaultSkinHelper.getSteve().texture(), posX + 2.5f, rowY + 2.5f, 8f, alpha);
            youGamePanel(matrix, posX + headPanelWidth + gap, rowY, fakeInfoWidth, 13f, alpha * 0.55f);

            float baseY = rowY + (13f - 6f) / 2f;
            float prefixLeftX = posX + headPanelWidth + gap + 6f;
            Builder.text().text(fakePrefix).font(BIKO_FONT.get()).size(6f).thickness(0.06f)
                    .color(youGameAlpha(new Color(255, 85, 85), alpha)).build().render(matrix, prefixLeftX, baseY);

            float dotSize = 5f;
            float dotX = prefixLeftX + fakePrefixWidth + 4f;
            float dotY = rowY + (13f - dotSize) / 2f;
            Builder.rectangle().size(new SizeState(dotSize, dotSize)).radius(new QuadRadiusState(2.5f))
                    .color(new QuadColorState(youGameAlpha(new Color(46, 204, 113), alpha))).build().render(matrix, dotX, dotY);
        }

        if (chat) draggable.onDraw(mouseX, mouseY, screenWidth, screenHeight);
    }

    private void youGameHeader(Matrix4f matrix, float x, float y, float w, String icon, String title, float alpha) {
        youGamePanel(matrix, x, y, w, 13f, alpha);
        int accent = ThemeManager.getInstance().getPrimary();
        Builder.text().text(icon).font(ICON_FONT.get()).size(8f).thickness(0.08f)
                .color(youGameAlpha(new Color(accent), alpha)).build().render(matrix, x + 4f, y + (13f - 8f) / 2f);
        Builder.text().text(title).font(BIKO_FONT.get()).size(7f).thickness(0.06f)
                .color(youGameAlpha(Color.WHITE, alpha)).build().render(matrix, x + 16f, y + (13f - 7f) / 2f);
    }

    private void youGamePanel(Matrix4f matrix, float x, float y, float w, float h, float alpha) {
        float a = Math.max(0f, Math.min(1f, alpha));
        Builder.rectangle().size(new SizeState(w, h)).radius(new QuadRadiusState(3f))
                .color(new QuadColorState(new Color(20, 20, 20, (int) (255 * a)))).build().render(matrix, x, y);
        Builder.blur().size(new SizeState(w, h)).radius(new QuadRadiusState(3f))
                .color(new QuadColorState(new Color(140, 140, 140, (int) (255 * a)))).blurRadius(11f).smoothness(1f).build().render(matrix, x, y);
    }

    private Color youGameAlpha(Color c, float a) {
        a = Math.max(0f, Math.min(1f, a));
        return new Color(c.getRed(), c.getGreen(), c.getBlue(), (int) (255 * a));
    }

    private void updateRowAnimations(Map<String, Animation> map, Set<String> active) {
        for (Map.Entry<String, Animation> entry : new ArrayList<>(map.entrySet())) {
            Animation anim = entry.getValue();
            anim.update();
            anim.start(anim.getValue(), active.contains(entry.getKey()) ? 1f : 0f, 250, Easing.CUBIC_OUT);
        }
        for (String key : active) {
            if (!map.containsKey(key)) {
                Animation anim = new Animation();
                anim.setValue(0f);
                map.put(key, anim);
            }
        }
        map.entrySet().removeIf(e -> !active.contains(e.getKey()) && e.getValue().getValue() < 0.01f);
    }

    private String youGameNameText(Staff s) {
        String prefix = s.prefix == null ? "" : s.prefix.replaceAll("§[0-9a-fk-or]", "").trim();
        return prefix.isEmpty() ? s.name : prefix + " " + s.name;
    }

    private Identifier getYouGameSkin(String name) {
        Identifier cached = youGameSkins.get(name);
        if (cached != null) return cached;
        if (mc.getNetworkHandler() != null) {
            for (PlayerListEntry entry : mc.getNetworkHandler().getPlayerList()) {
                if (entry.getProfile() != null && name.equals(entry.getProfile().getName())) {
                    Identifier tex = entry.getSkinTextures().texture();
                    youGameSkins.put(name, tex);
                    return tex;
                }
            }
        }
        return DefaultSkinHelper.getSteve().texture();
    }

    private void drawYouGameHead(Matrix4f matrix, Identifier texture, float x, float y, float size, float alpha) {
        AbstractTexture tex = mc.getTextureManager().getTexture(texture);
        if (tex == null) return;
        Builder.texture().size(new SizeState(size, size)).radius(new QuadRadiusState(2f))
                .color(new QuadColorState(youGameAlpha(Color.WHITE, alpha)))
                .texture(8 / 64f, 8 / 64f, 8 / 64f, 8 / 64f, tex.getGlId()).build().render(matrix, x, y);
        Builder.texture().size(new SizeState(size, size)).radius(new QuadRadiusState(2f))
                .color(new QuadColorState(youGameAlpha(Color.WHITE, alpha)))
                .texture(40 / 64f, 8 / 64f, 8 / 64f, 8 / 64f, tex.getGlId()).build().render(matrix, x, y);
    }

    private void renderMacan(DrawContext context, int mouseX, int mouseY, int screenWidth, int screenHeight, float animScale) {
        int onlineCount = 0;
        for (Staff s : staffPlayers) {
            if (!s.isVanish) onlineCount++;
        }
        boolean showPlaceholder = staffPlayers.isEmpty() && mc.currentScreen instanceof ChatScreen;
        if (staffPlayers.isEmpty() && !showPlaceholder) {
            draggable.setWidth(100);
            draggable.setHeight(18);
            return;
        }
        if (showPlaceholder) onlineCount = 3;

        float backHeight = 18f;
        float pad = 7f;

        String title = "Staff";
        String suffix = "  online";
        String countText = String.valueOf(onlineCount);

        MsdfFont font = BIKO_FONT.get();
        float titleW = font.getWidth(title, 8f);
        float countW = font.getWidth(countText, 8f);
        float suffixW = font.getWidth(suffix, 8f);

        float back1X = draggable.getX();
        float back2X = back1X + pad + titleW + pad;
        float back1W = back2X - back1X;
        float back2W = pad + countW + suffixW + pad;

        float totalW = (back2X - back1X) + back2W - pad;

        float y = draggable.getY();
        float centerY = y + backHeight / 2f;

        context.getMatrices().push();
        float cx = draggable.getX() + totalW / 2f;
        float cy = y + backHeight / 2f;
        context.getMatrices().translate(cx, cy, 0);
        context.getMatrices().scale(animScale, animScale, 1);
        context.getMatrices().translate(-cx, -cy, 0);
        Matrix4f matrix = context.getMatrices().peek().getPositionMatrix();

        int back1Color = new Color(12, 12, 12, 204).getRGB();
        int back2Color = new Color(22, 22, 22, 153).getRGB();

        ShadowUtil.dark(back1W, backHeight, new QuadRadiusState(5, 5, 0, 0)).render(matrix, back1X, y, 0);
        ShadowUtil.dark(back2W, backHeight, new QuadRadiusState(0, 0, 5, 5)).render(matrix, back2X, y, 0);
        Builder.blur().size(new SizeState(back1W, backHeight)).radius(new QuadRadiusState(5, 5, 0, 0))
                .color(new QuadColorState(new Color(back1Color, true)))
                .blurRadius(10).smoothness(1f).build().render(matrix, back1X, y);
        Builder.blur().size(new SizeState(back2W, backHeight)).radius(new QuadRadiusState(0, 0, 5, 5))
                .color(new QuadColorState(new Color(back2Color, true)))
                .blurRadius(10).smoothness(1f).build().render(matrix, back2X, y);

        Color white = Color.WHITE;
        Builder.text().text(title).font(font).size(8f).thickness(0.06f).color(white)
                .build().render(matrix, back1X + pad, centerY - 4f);

        float countX = back2X + pad;
        float textY = centerY - 4f;
        Builder.text().text(countText).font(font).size(8f).thickness(0.06f).color(white)
                .build().render(matrix, countX, textY);
        Builder.text().text(suffix).font(font).size(8f).thickness(0.06f).color(white)
                .build().render(matrix, countX + countW, textY);

        draggable.setWidth(totalW);
        draggable.setHeight(backHeight);

        if (mc.currentScreen instanceof ChatScreen) {
            draggable.onDraw(mouseX, mouseY, screenWidth, screenHeight);
        }

        context.getMatrices().pop();
    }

    public void handleMouse(double mouseX, double mouseY, int button, int action) {
        if (mc.currentScreen instanceof ChatScreen) {
            if (action == 1) {
                draggable.onClick((int) mouseX, (int) mouseY, button);
            } else if (action == 0) {
                draggable.onRelease(button);
            }
        }
    }

    private void updateStaffList() {
        staffPlayers.clear();
        if (mc.world == null || mc.getNetworkHandler() == null) return;

        var manualStaff = Cheap.getInstance().getStaffManager().getStaff();

        for (Team team : mc.world.getScoreboard().getTeams()) {
            for (String playerName : team.getPlayerList()) {
                String prefix = team.getPrefix().getString();

                boolean isOnline = false;
                for (PlayerListEntry entry : mc.getNetworkHandler().getPlayerList()) {
                    if (entry.getProfile().getName().equalsIgnoreCase(playerName)) {
                        isOnline = true;
                        break;
                    }
                }

                boolean isManual = manualStaff.contains(playerName);
                boolean isDetected = namePattern.matcher(playerName).matches() &&
                        prefixMatches.matcher(prefix.toLowerCase(Locale.ROOT)).matches();

                if ((isDetected || isManual) && !playerName.equalsIgnoreCase(mc.player.getName().getString())) {
                    staffPlayers.add(new Staff(prefix, playerName, !isOnline));
                }
            }
        }

        var distinct = staffPlayers.stream()
                .collect(Collectors.toMap(s -> s.name.toLowerCase(), s -> s, (s1, s2) -> s1))
                .values().stream().toList();
        staffPlayers.clear();
        staffPlayers.addAll(distinct);
        staffPlayers.sort(Comparator.comparing(s -> s.name.toLowerCase()));
    }

    private static class Staff {
        String prefix;
        String name;
        boolean isVanish;

        public Staff(String prefix, String name, boolean isVanish) {
            this.prefix = prefix;
            this.name = name;
            this.isVanish = isVanish;
        }
    }
}