package fun.ogi.util.WayPoint;

import com.google.common.eventbus.Subscribe;
import fun.ogi.events.render.EventWorldRenderer;
import fun.ogi.util.storages.WaypointStorage;
import fun.ogi.util.storages.waypoint.Waypoint;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;
import org.joml.Vector4f;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static fun.ogi.util.MinecraftUtil.mc;

public class WayPointsRender {
    private final Map<String, Waypoint> all = WaypointStorage.getInstance().getAll();
    private final List<Waypoint> wpList = new ArrayList<>(all.values());
    private final Vec3d WAYPOINT_POS = new Vec3d(100, 64, 200);
    private final String WAYPOINT_LABEL = "Пример";
    private final Identifier ICON = Identifier.of("cheap", "textures/arrows/arrow.png");

    private static Matrix4f lastPositionMatrix;
    private static Matrix4f lastProjectionMatrix;
    private static Vec3d lastCameraPos = Vec3d.ZERO;
    @Subscribe
    public void onWorldRender(EventWorldRenderer event) {
        WayPointsRender.lastPositionMatrix = new Matrix4f(event.getMatrices().peek().getPositionMatrix());
        WayPointsRender.lastProjectionMatrix = new Matrix4f(event.getProjectionMatrix());
        WayPointsRender.lastCameraPos = mc.player.getEyePos();
    }

    private static String formatDistance(double distance) {
        if (distance >= 1000) {
            return String.format("%.1fK", distance / 1000.0);
        }
        return String.format("%.0fM", distance);
    }
}

