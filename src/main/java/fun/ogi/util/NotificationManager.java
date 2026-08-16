package fun.ogi.util;

import fun.ogi.module.theme.ThemeManager;
import fun.ogi.util.render.Draggable;
import fun.ogi.util.render.ShadowUtil;
import fun.ogi.util.render.builders.Builder;
import fun.ogi.util.render.builders.states.QuadColorState;
import fun.ogi.util.render.builders.states.QuadRadiusState;
import fun.ogi.util.render.builders.states.SizeState;
import fun.ogi.util.render.msdf.MsdfFont;
import com.google.common.base.Supplier;
import com.google.common.base.Suppliers;
import net.minecraft.client.MinecraftClient;
import org.joml.Matrix4f;

import java.awt.Color;
import java.util.concurrent.CopyOnWriteArrayList;

public class NotificationManager {
    public static final int TYPE_INFO = 0;
    public static final int TYPE_SUCCESS = 1;
    public static final int TYPE_ERROR = 2;

    private static final long DURATION = 2500;
    private static final float BASE_WIDTH = 115;
    private static final float HEIGHT = 15;
    private static final float GAP = 3;
    private static final float ROUNDING = 4;
    private static final Supplier<MsdfFont> BIKO_FONT = Suppliers.memoize(() -> MsdfFont.builder().atlas("biko").data("biko").build());
    private static final Supplier<MsdfFont> NUR_FONT = Suppliers.memoize(() -> MsdfFont.builder().atlas("nur").data("nur").build());
    private static final CopyOnWriteArrayList<Notification> notifications = new CopyOnWriteArrayList<>();

    public static final Draggable draggable = new Draggable(5, 5, BASE_WIDTH, HEIGHT);

    public static void post(String text) {
        post(text, TYPE_INFO, DURATION, null);
    }

    public static void post(String text, int type) {
        post(text, type, DURATION, null);
    }

    public static void post(String text, int type, long durationMs) {
        post(text, type, durationMs, null);
    }

    public static void post(String text, int type, long durationMs, Character icon) {
        notifications.add(new Notification(text, type, System.currentTimeMillis(), durationMs, icon));
        if (notifications.size() > 10) {
            notifications.remove(0);
        }
    }

    public static void render(Matrix4f matrix, String style, String mode) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null) return;

        boolean showPlaceholder = notifications.isEmpty() && mc.currentScreen instanceof net.minecraft.client.gui.screen.ChatScreen;

        if (notifications.isEmpty() && !showPlaceholder) return;

        float totalHeight = 0;
        float maxTextWidth = 0;
        for (Notification n : notifications) {
            long elapsed = System.currentTimeMillis() - n.startTime;
            if ((float) elapsed / n.duration >= 1f) continue;
            totalHeight += HEIGHT + GAP;
            float textW = BIKO_FONT.get().getWidth(n.text, 7);
            float requiredW = textW + 8 + (n.icon != null ? 13 : 5);
            if (requiredW > maxTextWidth) maxTextWidth = requiredW;
        }
        if (showPlaceholder) {
            totalHeight = HEIGHT;
            float textW = BIKO_FONT.get().getWidth("Notifications", 7);
            maxTextWidth = textW + 8 + 5;
        }
        totalHeight = Math.max(0, totalHeight - (showPlaceholder ? 0 : GAP));

        float currentWidth = Math.max(BASE_WIDTH, maxTextWidth);

        draggable.setWidth(currentWidth);
        draggable.setHeight(totalHeight);

        float x = draggable.getX();
        float y = draggable.getY() + totalHeight;

        if (showPlaceholder) {
            y -= HEIGHT;

            int bgColorInt = ThemeManager.getInstance().getPalette().getHudBackground();
            Color bgColor = new Color(bgColorInt);
            bgColor = new Color(bgColor.getRed(), bgColor.getGreen(), bgColor.getBlue(), 255);

            if (mode.equals("Macan")) {
                ShadowUtil.dark(currentWidth, HEIGHT, new QuadRadiusState(ROUNDING)).render(matrix, x, y, 0);
                Color macanBg = new Color(22, 22, 22, 200);
                Builder.blur()
                        .size(new SizeState(currentWidth, HEIGHT))
                        .color(new QuadColorState(macanBg))
                        .radius(new QuadRadiusState(ROUNDING))
                        .blurRadius(10).smoothness(1f)
                        .build()
                        .render(matrix, x, y);
            }else if(mode.equals("Old")){
                Color gradStart = new Color(ThemeManager.getInstance().getPrimary());
                Color gradEnd = new Color(ThemeManager.getInstance().getSecondary());
                ShadowUtil.gradient(gradStart, gradEnd, currentWidth, HEIGHT, new QuadRadiusState(ROUNDING)).render(matrix, x, y, 0);
                Builder.rectangle()
                        .size(new SizeState(currentWidth, HEIGHT))
                        .radius(new QuadRadiusState(ROUNDING))
                        .color(new QuadColorState(gradStart, gradEnd, gradStart, gradEnd))
                        .build().render(matrix, x, y);
            } else if (mode.equals("YouGame")) {
                youGamePanel(matrix, x, y, currentWidth, HEIGHT, 1f);
                Builder.rectangle().size(new SizeState(2, HEIGHT - 4))
                        .color(new QuadColorState(new Color(ThemeManager.getInstance().getPrimary())))
                        .radius(new QuadRadiusState(1)).build().render(matrix, x + 2, y + 2, 0);
            } else if (style.equals("Liquid Glass")) {
                Builder.liquid()
                        .size(new SizeState(currentWidth, HEIGHT))
                        .radius(new QuadRadiusState(ROUNDING, ROUNDING, ROUNDING, ROUNDING))
                        .color(new QuadColorState(new Color(255, 255, 255, 255)))
                        .build()
                        .render(matrix, x, y);
            } else if (style.equals("Colored Liquid")) {
                Builder.coloredLiquid()
                        .size(new SizeState(currentWidth, HEIGHT))
                        .radius(new QuadRadiusState(ROUNDING, ROUNDING, ROUNDING, ROUNDING))
                        .color(new QuadColorState(new Color(bgColor.getRed(), bgColor.getGreen(), bgColor.getBlue(), 150)))
                        .build()
                        .render(matrix, x, y);
            } else if (style.equals("Default")) {
                ShadowUtil.dark(currentWidth, HEIGHT, new QuadRadiusState(ROUNDING)).render(matrix, x, y, 0);
                Builder.rectangle()
                        .size(new SizeState(currentWidth, HEIGHT))
                        .radius(new QuadRadiusState(ROUNDING))
                        .color(new QuadColorState(new Color(20, 20, 20, 255)))
                        .build()
                        .render(matrix, x, y);
            } else {
                Builder.blur()
                        .size(new SizeState(currentWidth, HEIGHT))
                        .color(new QuadColorState(bgColor))
                        .radius(new QuadRadiusState(ROUNDING))
                        .blurRadius(10).smoothness(1f)
                        .build()
                        .render(matrix, x, y);
            }

            float textSz = mode.equals("Macan") ? 6f : 7f;
            float textY = y + (HEIGHT - textSz) / 2f;
            Builder.text()
                    .font(BIKO_FONT.get())
                    .size(textSz)
                    .text("Notifications")
                    .thickness(0.06f)
                    .color(new Color(255, 255, 255, 255))
                    .build()
                    .render(matrix, x + 8, textY);
            return;
        }

        for (Notification n : notifications) {
            long elapsed = System.currentTimeMillis() - n.startTime;
            long duration = n.duration;
            float progress = Math.min(1f, (float) elapsed / duration);

            if (progress >= 1f) {
                continue;
            }

            y -= HEIGHT + GAP;

            int bgColorInt = ThemeManager.getInstance().getPalette().getHudBackground();
            Color bgColor = new Color(bgColorInt);
            bgColor = new Color(bgColor.getRed(), bgColor.getGreen(), bgColor.getBlue(), 255);

            int accentColorInt = switch (n.type) {
                case TYPE_SUCCESS -> ThemeManager.getInstance().getPalette().getNotificationSuccess();
                case TYPE_ERROR -> ThemeManager.getInstance().getPalette().getNotificationError();
                default -> ThemeManager.getInstance().getPalette().getNotificationInfo();
            };

            float alpha = 1f;
            if (progress > 0.85f) {
                alpha = 1f - (progress - 0.85f) / 0.15f;
            }
            if (progress < 0.1f) {
                alpha = progress / 0.1f;
            }

            Color accent = new Color(accentColorInt);
            accent = new Color(accent.getRed(), accent.getGreen(), accent.getBlue(), (int) (255 * alpha));

            Color bg = new Color(bgColor.getRed(), bgColor.getGreen(), bgColor.getBlue(), (int) (255 * alpha));

            if (mode.equals("Macan")) {
                ShadowUtil.dark(currentWidth, HEIGHT, new QuadRadiusState(ROUNDING)).render(matrix, x, y, 0);
                Color macanBg = new Color(22, 22, 22, (int)(200 * alpha));
                Builder.blur()
                        .size(new SizeState(currentWidth, HEIGHT))
                        .color(new QuadColorState(macanBg))
                        .radius(new QuadRadiusState(ROUNDING))
                        .blurRadius(10).smoothness(1f)
                        .build()
                        .render(matrix, x, y);
            } else if (mode.equals("Old")) {
                Color gradStart = new Color(ThemeManager.getInstance().getPrimary());
                Color gradEnd = new Color(ThemeManager.getInstance().getSecondary());
                ShadowUtil.gradient(gradStart, gradEnd, currentWidth, HEIGHT, new QuadRadiusState(ROUNDING)).render(matrix, x, y, 0);
                Builder.rectangle()
                        .size(new SizeState(currentWidth, HEIGHT))
                        .radius(new QuadRadiusState(ROUNDING))
                        .color(new QuadColorState(gradStart, gradEnd, gradStart, gradEnd))
                        .build().render(matrix, x, y);
            } else if (mode.equals("YouGame")) {
                youGamePanel(matrix, x, y, currentWidth, HEIGHT, alpha);
            } else if (style.equals("Liquid Glass")) {
                Builder.liquid()
                        .size(new SizeState(currentWidth, HEIGHT))
                        .radius(new QuadRadiusState(ROUNDING, ROUNDING, ROUNDING, ROUNDING))
                        .color(new QuadColorState(new Color(255, 255, 255, 255)))
                        .build()
                        .render(matrix, x, y);
            } else if (style.equals("Colored Liquid")) {
                Builder.coloredLiquid()
                        .size(new SizeState(currentWidth, HEIGHT))
                        .radius(new QuadRadiusState(ROUNDING, ROUNDING, ROUNDING, ROUNDING))
                        .color(new QuadColorState(new Color(bgColor.getRed(), bgColor.getGreen(), bgColor.getBlue(), 150)))
                        .build()
                        .render(matrix, x, y);
            } else if (style.equals("Default")) {
                ShadowUtil.dark(currentWidth, HEIGHT, new QuadRadiusState(ROUNDING)).render(matrix, x, y, 0);
                Builder.rectangle()
                        .size(new SizeState(currentWidth, HEIGHT))
                        .radius(new QuadRadiusState(ROUNDING))
                        .color(new QuadColorState(new Color(20, 20, 20, (int) (255 * alpha))))
                        .build()
                        .render(matrix, x, y);
            } else {
                Builder.blur()
                        .size(new SizeState(currentWidth, HEIGHT))
                        .color(new QuadColorState(bg))
                        .radius(new QuadRadiusState(ROUNDING))
                        .blurRadius(10).smoothness(1f)
                        .build()
                        .render(matrix, x, y);
            }

            if (!mode.equals("Macan")) {
                Builder.rectangle()
                        .size(new SizeState(2, HEIGHT - 4))
                        .color(new QuadColorState(accent))
                        .radius(new QuadRadiusState(1))
                        .build()
                        .render(matrix, x + 2, y + 2, 0);
            }

            float textSz = mode.equals("Macan") ? 6f : 7f;
            float textYOff = (HEIGHT - textSz) / 2f;
            Builder.text()
                    .font(BIKO_FONT.get())
                    .size(textSz)
                    .text(n.text)
                    .thickness(0.06f)
                    .color(new Color(255, 255, 255, (int) (255 * alpha)))
                    .build()
                    .render(matrix, x + 8, y + textYOff);

            if (n.icon != null) {
                float iconX = x + currentWidth - 13;
                float iconY = y + 3;
                Color iconColor = mode.equals("YouGame")
                        ? youGameAlpha(new Color(accentColorInt), alpha)
                        : new Color(255, 255, 255, (int) (255 * alpha));
                Builder.text()
                        .font(NUR_FONT.get())
                        .size(8)
                        .text(String.valueOf(n.icon))
                        .thickness(0.06f)
                        .color(iconColor)
                        .build()
                        .render(matrix, iconX, iconY);
            }
        }

        notifications.removeIf(n -> System.currentTimeMillis() - n.startTime >= n.duration);
    }

    private static void youGamePanel(Matrix4f matrix, float x, float y, float w, float h, float alpha) {
        float a = Math.max(0f, Math.min(1f, alpha));
        Builder.rectangle().size(new SizeState(w, h)).radius(new QuadRadiusState(3))
                .color(new QuadColorState(new Color(20, 20, 20, (int) (255 * a)))).build().render(matrix, x, y);
        Builder.blur().size(new SizeState(w, h)).radius(new QuadRadiusState(3))
                .color(new QuadColorState(new Color(140, 140, 140, (int) (255 * a)))).blurRadius(11).smoothness(1f).build().render(matrix, x, y);
    }

    private static Color youGameAlpha(Color c, float a) {
        a = Math.max(0f, Math.min(1f, a));
        return new Color(c.getRed(), c.getGreen(), c.getBlue(), (int) (255 * a));
    }

    private record Notification(String text, int type, long startTime, long duration, Character icon) {}
}

