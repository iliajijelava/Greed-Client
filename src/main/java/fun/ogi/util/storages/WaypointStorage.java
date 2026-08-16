package fun.ogi.util.storages;

import com.google.common.base.Supplier;
import com.google.common.base.Suppliers;
import com.google.common.eventbus.Subscribe;
import fun.ogi.events.EventMouse;
import fun.ogi.events.render.EventWorldRenderer;
import fun.ogi.events.render.EventHud;
import fun.ogi.module.theme.ThemeManager;
import fun.ogi.util.render.Draggable;
import fun.ogi.util.render.builders.Builder;
import fun.ogi.util.render.builders.states.QuadColorState;
import fun.ogi.util.render.builders.states.QuadRadiusState;
import fun.ogi.util.render.builders.states.SizeState;
import fun.ogi.util.render.msdf.MsdfFont;
import fun.ogi.util.render.renderers.impl.BuiltTexture;
import fun.ogi.util.storages.waypoint.Waypoint;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.Camera;
import net.minecraft.client.gui.screen.ChatScreen;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RotationAxis;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.joml.Vector4f;

import java.awt.*;
import java.util.LinkedHashMap;
import java.util.Map;

public class WaypointStorage {

    private static final WaypointStorage INSTANCE = new WaypointStorage();
    private static final Identifier ARROW_TEXTURE = Identifier.of("cheap", "textures/arrows/arrow.png");
    private static final Supplier<MsdfFont> BIKO_FONT = Suppliers.memoize(() -> MsdfFont.builder().atlas("biko").data("biko").build());
    private static final Supplier<MsdfFont> REGULAR_FONT = Suppliers.memoize(() -> MsdfFont.builder().atlas("regular_semibold").data("regular_semibold").build());

    private static final float ARROW_SIZE = 20.0f;

    private static final float WP_FONT_SIZE = 8.0f;
    private static final float WP_DOT_SIZE = 4.0f;
    private static final float WP_H_PADDING = 7.0f;
    private static final float WP_V_PADDING = 4.5f;
    private static final float WP_DOT_GAP = 5.0f;
    private static final float WP_TEXT_GAP = 6.0f;
    private static final float WP_CORNER_RADIUS = 5.0f;
    private static final int WP_SCREEN_MARGIN = 12;
    private static final int WP_SPACING = 4;

    public static float waypointScale = 1.0f;

    private final LinkedHashMap<String, Waypoint> waypoints = new LinkedHashMap<>();
    private Waypoint gpsWaypoint = null;
    private float gpsAlpha = 0.0f;
    private float animatedYaw;

    private Draggable gpsDraggable;

    private static MinecraftClient mc() {
        return MinecraftClient.getInstance();
    }

    private final Matrix4f lastViewMatrix = new Matrix4f();
    private final Matrix4f lastProjectionMatrix = new Matrix4f();
    private boolean hasProjection;

    private WaypointStorage() {
    }

    public static WaypointStorage getInstance() {
        return INSTANCE;
    }

    public void setGps(Waypoint waypoint) {
        this.gpsWaypoint = waypoint;
    }

    public void clearGps() {
        this.gpsWaypoint = null;
    }

    public boolean isGpsEmpty() {
        return gpsWaypoint == null;
    }

    public Draggable getGpsDraggable() {
        if (gpsDraggable == null) {
            gpsDraggable = new Draggable(
                    mc().getWindow().getScaledWidth() * 0.5f - ARROW_SIZE * 0.5f,
                    mc().getWindow().getScaledHeight() * 0.25f - ARROW_SIZE * 0.5f,
                    ARROW_SIZE,
                    ARROW_SIZE + 10
            );
        }
        return gpsDraggable;
    }

    public void add(Waypoint waypoint) {
        waypoints.put(waypoint.getName().toLowerCase(), waypoint);
    }

    public boolean remove(String name) {
        return waypoints.remove(name.toLowerCase()) != null;
    }

    public void clear() {
        waypoints.clear();
    }

    public Waypoint get(String name) {
        return waypoints.get(name.toLowerCase());
    }

    public boolean contains(String name) {
        return waypoints.containsKey(name.toLowerCase());
    }

    public Map<String, Waypoint> getAll() {
        return waypoints;
    }

    public boolean isEmpty() {
        return waypoints.isEmpty();
    }

    public int size() {
        return waypoints.size();
    }

    @Subscribe
    public void onRender3D(EventWorldRenderer event) {
        hasProjection = true;
        lastViewMatrix.set(event.getModelViewMatrix());
        lastProjectionMatrix.set(event.getProjectionMatrix());
    }

    @Subscribe
    public void onEventHud(EventHud event) {
        if (mc().player == null || mc().world == null) return;

        renderWaypoints(event);
        renderGps(event);
    }

    private void renderWaypoints(EventHud event) {
        if (waypoints.isEmpty() || !hasProjection) return;
        if (mc().player == null) return;

        float scale = waypointScale;
        Matrix4f matrix = event.getDrawContext().getMatrices().peek().getPositionMatrix();
        float screenW = mc().getWindow().getScaledWidth();
        float screenH = mc().getWindow().getScaledHeight();
        int themeColor = ThemeManager.getInstance().getPrimary();

        float fontSize = WP_FONT_SIZE * scale;
        float dotSize = WP_DOT_SIZE * scale;
        float hPad = WP_H_PADDING * scale;
        float vPad = WP_V_PADDING * scale;
        float dotGap = WP_DOT_GAP * scale;
        float textGap = WP_TEXT_GAP * scale;
        float cornerRadius = WP_CORNER_RADIUS * scale;
        int screenMargin = (int) (WP_SCREEN_MARGIN * scale);
        int spacing = (int) (WP_SPACING * scale);

        int index = 0;
        for (Waypoint wp : waypoints.values()) {
            Vec3d worldPos = new Vec3d(wp.getX() + 0.5, wp.getY() + 0.5, wp.getZ() + 0.5);
            float[] screen = worldToScreen(worldPos);
            if (screen == null) continue;

            int distance = (int) mc().player.getPos().distanceTo(worldPos);
            String distText = "\u0414\u043E \u0442\u043E\u0447\u043A\u0438: " + distance + "M";

            float nameWidth = REGULAR_FONT.get().getWidth(wp.getName(), fontSize);
            float distWidth = REGULAR_FONT.get().getWidth(distText, fontSize);
            float totalTextWidth = nameWidth + textGap + distWidth;

            float totalWidth = hPad + dotSize + dotGap + totalTextWidth + hPad;
            float totalHeight = fontSize + vPad * 2;

            float bgX = screen[0] - totalWidth / 2f;
            float bgY = screen[1] - totalHeight / 2f + index * (totalHeight + spacing);

            bgX = MathHelper.clamp(bgX, screenMargin, screenW - totalWidth - screenMargin);
            bgY = MathHelper.clamp(bgY, screenMargin, screenH - totalHeight - screenMargin);

            Builder.rectangle()
                    .size(new SizeState(totalWidth, totalHeight))
                    .radius(new QuadRadiusState(cornerRadius))
                    .color(new QuadColorState(new Color(18, 18, 18, 190)))
                    .build()
                    .render(matrix, bgX, bgY, 0);

            float dotX = bgX + hPad;
            float dotY = bgY + (totalHeight - dotSize) / 2f;
            Builder.rectangle()
                    .size(new SizeState(dotSize, dotSize))
                    .radius(new QuadRadiusState(dotSize / 2f))
                    .color(new QuadColorState(applyAlpha(themeColor, 1.0f)))
                    .build()
                    .render(matrix, dotX, dotY, 0);

            float contentWidth = dotSize + dotGap + totalTextWidth;
            float contentStartX = bgX + (totalWidth - contentWidth) / 2f;
            float textY = bgY + vPad;
            float nameX = contentStartX + dotSize + dotGap;

            Builder.text()
                    .text(wp.getName())
                    .font(REGULAR_FONT.get())
                    .size(fontSize)
                    .thickness(0.06f)
                    .color(new Color(-1, true))
                    .build()
                    .render(matrix, nameX, textY, 0);

            float distX = nameX + nameWidth + textGap;
            Builder.text()
                    .text(distText)
                    .font(REGULAR_FONT.get())
                    .size(fontSize)
                    .thickness(0.06f)
                    .color(new Color(applyAlpha(themeColor, 0.85f), true))
                    .build()
                    .render(matrix, distX, textY, 0);

            index++;
        }
    }

    private void renderGps(EventHud event) {
        float targetAlpha = (gpsWaypoint == null) ? 0.0f : 1.0f;
        gpsAlpha = approach(gpsAlpha, targetAlpha, 0.12f);
        float clampedAlpha = MathHelper.clamp(gpsAlpha, 0.0f, 1.0f);

        if (gpsWaypoint == null && clampedAlpha <= 0.02f) return;
        if (gpsWaypoint == null) return;

        double deltaX = gpsWaypoint.getX() - mc().player.getX();
        double deltaZ = gpsWaypoint.getZ() - mc().player.getZ();
        int distance = (int) Math.round(Math.sqrt(deltaX * deltaX + deltaZ * deltaZ));

        float targetYaw = (float) -Math.toDegrees(Math.atan2(deltaX, deltaZ)) - mc().gameRenderer.getCamera().getYaw();
        animatedYaw = interpolateAngle(animatedYaw, targetYaw, 0.18f);

        int themeColor = ThemeManager.getInstance().getPrimary();
        int color = applyAlpha(themeColor, clampedAlpha);

        int currentMouseX = (int) mc().mouse.getX() * mc().getWindow().getScaledWidth() / mc().getWindow().getWidth();
        int currentMouseY = (int) mc().mouse.getY() * mc().getWindow().getScaledHeight() / mc().getWindow().getHeight();
        getGpsDraggable().onDraw(currentMouseX, currentMouseY, mc().getWindow().getScaledWidth(), mc().getWindow().getScaledHeight());

        float arrowCenterX = getGpsDraggable().getX() + ARROW_SIZE * 0.5f;
        float arrowCenterY = getGpsDraggable().getY() + ARROW_SIZE * 0.5f;

        MatrixStack ms = event.getDrawContext().getMatrices();

        ms.push();
        ms.translate(arrowCenterX, arrowCenterY, 0.0f);
        ms.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(animatedYaw));
        ms.translate(-arrowCenterX, -arrowCenterY, 0.0f);

        Matrix4f matrix = ms.peek().getPositionMatrix();
        float drawX = arrowCenterX - ARROW_SIZE * 0.5f;
        float drawY = arrowCenterY - ARROW_SIZE * 0.5f;

        BuiltTexture arrow = Builder.texture()
                .size(new SizeState(ARROW_SIZE, ARROW_SIZE))
                .radius(new QuadRadiusState(0))
                .color(new QuadColorState(color))
                .texture(0, 0, 1, 1, mc().getTextureManager().getTexture(ARROW_TEXTURE).getGlId())
                .build();
        arrow.render(matrix, drawX, drawY, 0);

        ms.pop();

        matrix = ms.peek().getPositionMatrix();
        String distanceText = distance + "m.";
        int textColor = applyAlpha(0xFFFFFFFF, clampedAlpha);
        float textWidth = BIKO_FONT.get().getWidth(distanceText, 12f);
        Builder.text()
                .text(distanceText)
                .font(BIKO_FONT.get())
                .size(12f)
                .thickness(0.06f)
                .color(new Color(textColor, true))
                .build()
                .render(matrix, arrowCenterX - textWidth * 0.5f, arrowCenterY + ARROW_SIZE * 0.5f + 2.0f);
    }

    @Subscribe
    public void onMouse(EventMouse event) {
        if (!(mc().currentScreen instanceof ChatScreen)) return;

        if (event.getAction() == 1) {
            getGpsDraggable().onClick(event.getMouseX(), event.getMouseY(), event.getButton());
        } else if (event.getAction() == 0) {
            getGpsDraggable().onRelease(event.getButton());
        }
    }

    private float[] worldToScreen(Vec3d worldPos) {
        if (mc() == null || mc().getWindow() == null) return null;

        Matrix4f viewProjection = new Matrix4f(lastProjectionMatrix).mul(lastViewMatrix);

        Vector4f clip = new Vector4f(
                (float) worldPos.x,
                (float) worldPos.y,
                (float) worldPos.z,
                1.0f
        );
        viewProjection.transform(clip);

        float w = clip.w;
        if (w <= 0.0f) return null;

        float ndcX = clip.x / w;
        float ndcY = clip.y / w;

        float screenX = (ndcX * 0.5f + 0.5f) * mc().getWindow().getScaledWidth();
        float screenY = (1.0f - (ndcY * 0.5f + 0.5f)) * mc().getWindow().getScaledHeight();

        if (Float.isNaN(screenX) || Float.isNaN(screenY) || Float.isInfinite(screenX) || Float.isInfinite(screenY)) {
            return null;
        }

        return new float[]{screenX, screenY};
    }

    private float approach(float current, float target, float factor) {
        return MathHelper.lerp(MathHelper.clamp(factor, 0.0f, 1.0f), current, target);
    }

    private float interpolateAngle(float current, float target, float factor) {
        float delta = MathHelper.wrapDegrees(target - current);
        return current + delta * factor;
    }

    private static int applyAlpha(int color, float alpha) {
        int a = MathHelper.clamp((int) (alpha * ((color >> 24) & 0xFF)), 0, 255);
        return (a << 24) | (color & 0x00FFFFFF);
    }
}

