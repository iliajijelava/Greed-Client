package fun.ogi.module.impl.list.render;

import com.google.common.eventbus.Subscribe;
import com.google.common.eventbus.Subscribe;
import fun.ogi.Cheap;
import fun.ogi.events.render.EventHud;
import fun.ogi.module.Module;
import fun.ogi.module.ModuleCategory;
import fun.ogi.module.ModuleInformation;
import fun.ogi.module.settings.BooleanSetting;
import fun.ogi.module.settings.NumberSetting;
import fun.ogi.module.theme.ThemeManager;
import fun.ogi.util.render.builders.Builder;
import fun.ogi.util.render.builders.states.QuadColorState;
import fun.ogi.util.render.builders.states.QuadRadiusState;
import fun.ogi.util.render.builders.states.SizeState;
import fun.ogi.util.render.renderers.impl.BuiltTexture;
import fun.ogi.util.rotation.FreeLookComponent;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.Identifier;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RotationAxis;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;
import org.joml.Matrix4f;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@ModuleInformation(moduleName = "Arrows", moduleDesc = "Renders arrow indicators pointing to entities", moduleCategory = ModuleCategory.RENDER)
public class Arrows extends Module {

    private static final Identifier ARROW_TEXTURE = Identifier.of("cheap", "textures/arrows/arrow.png");
    private static final Identifier FRIEND_ARROW_TEXTURE = Identifier.of("cheap", "textures/arrows/arrow2.png");
    private final NumberSetting radius = new NumberSetting("Radius", this, 58.0, 30.0, 120.0, 1.0);
    private final NumberSetting size = new NumberSetting("Size", this, 13.0, 8.0, 28.0, 0.5);
    private final BooleanSetting showPlayers = new BooleanSetting("Show Players", this, true);
    private final BooleanSetting showFriends = new BooleanSetting("Show Friends", this, true);
    private final BooleanSetting showItems = new BooleanSetting("Show Items", this, false);
    private final BooleanSetting onlyHidden = new BooleanSetting("Hidden Only", this, false);

    private final Map<UUID, ArrowState> states = new HashMap<>();
    private final Set<UUID> seenPlayers = new HashSet<>();

    public Arrows() {
        addSettings(radius, size, showPlayers, showFriends, showItems, onlyHidden);
    }

    @Subscribe
    public void onHud(EventHud event) {
        if (mc.player == null || mc.world == null) return;
        if (mc.options.hudHidden) {
            fadeAllStates();
            return;
        }

        float partialTicks = event.getRenderTickCounter().getTickDelta(false);
        float centerX = mc.getWindow().getScaledWidth() * 0.5f;
        float centerY = mc.getWindow().getScaledHeight() * 0.5f;
        float arrowSize = size.getFloatValue();
        float y = centerY - radius.getFloatValue();
        float playerYaw = getReferenceYaw(partialTicks);
        Vec3d selfPos = getReferencePos(partialTicks);

        seenPlayers.clear();

        for (PlayerEntity player : mc.world.getPlayers()) {
            if (player == mc.player || !player.isAlive() || player.isSpectator()) continue;

            String name = player.getName().getString();
            boolean isFriend = Cheap.getInstance().getFriendManager().contains(name);

            if (isFriend && !showFriends.getValue()) continue;
            if (!isFriend && !showPlayers.getValue()) continue;
            if (onlyHidden.getValue() && isEntityVisible(player)) continue;

            UUID uuid = player.getUuid();
            ArrowState state = states.computeIfAbsent(uuid, id -> new ArrowState());
            seenPlayers.add(uuid);

            int color = isFriend ? rgba(0, 224, 116, 255) : ThemeManager.getInstance().getPrimary();
            float targetYaw = getRelativeYaw(player, partialTicks, playerYaw, selfPos);
            state.rotation = interpolateAngle(state.rotation, targetYaw, 0.18f);
            state.alpha = approach(state.alpha, 1.0f, 0.12f);
            float alpha = MathHelper.clamp(state.alpha, 0.0f, 1.0f);
            if (alpha <= 0.01f) continue;

            int drawColor = applyAlpha(color, alpha);
            if(isFriend && showFriends.getValue()) {
                renderArrow(event,centerX,centerY,y,arrowSize,state.rotation,drawColor,ARROW_TEXTURE);
            }
            renderArrow(event, centerX, centerY, y, arrowSize, state.rotation, drawColor, ARROW_TEXTURE);
        }

        if (showItems.getValue()) {
            for (Entity entity : mc.world.getEntities()) {
                if (!(entity instanceof ItemEntity)) continue;
                if (!entity.isAlive()) continue;
                if (onlyHidden.getValue() && isEntityVisible(entity)) continue;

                UUID uuid = entity.getUuid();
                ArrowState state = states.computeIfAbsent(uuid, id -> new ArrowState());
                seenPlayers.add(uuid);

                int color = ThemeManager.getInstance().getPrimary();
                float targetYaw = getRelativeYaw(entity, partialTicks, playerYaw, selfPos);
                state.rotation = interpolateAngle(state.rotation, targetYaw, 0.18f);
                state.alpha = approach(state.alpha, 1.0f, 0.12f);
                float alpha = MathHelper.clamp(state.alpha, 0.0f, 1.0f);
                if (alpha <= 0.01f) continue;

                int drawColor = applyAlpha(color, alpha);
                renderArrow(event, centerX, centerY, y, arrowSize, state.rotation, drawColor, ARROW_TEXTURE);
            }
        }

        states.entrySet().removeIf(entry -> {
            if (seenPlayers.contains(entry.getKey())) return false;
            ArrowState state = entry.getValue();
            state.alpha = approach(state.alpha, 0.0f, 0.10f);
            return state.alpha <= 0.02f;
        });
    }

    private void renderArrow(EventHud event, float centerX, float centerY, float y, float arrowSize, float rotation, int color, Identifier arrowTexture) {
        MatrixStack ms = event.getDrawContext().getMatrices();
        ms.push();
        ms.translate(centerX, centerY, 0.0f);
        ms.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(rotation));
        ms.translate(-centerX, -centerY, 0.0f);

        Matrix4f mat = ms.peek().getPositionMatrix();
        float x = centerX - arrowSize * 0.5f;
        BuiltTexture arrow = Builder.texture()
                .size(new SizeState(arrowSize, arrowSize))
                .radius(new QuadRadiusState(0))
                .color(new QuadColorState(color))
                .texture(0, 0, 1, 1, mc.getTextureManager().getTexture(arrowTexture).getGlId())
                .build();
        arrow.render(mat, x, y, 0);

        ms.pop();
    }

    private boolean isEntityVisible(Entity entity) {
        Vec3d start = mc.player.getCameraPosVec(1.0f);
        Vec3d end = entity.getPos().add(0, entity.getHeight() * 0.5, 0);
        var result = mc.world.raycast(new RaycastContext(start, end, RaycastContext.ShapeType.COLLIDER, RaycastContext.FluidHandling.NONE, mc.player));
        return result.getType() == HitResult.Type.MISS;
    }

    private void fadeAllStates() {
        states.entrySet().removeIf(entry -> {
            ArrowState state = entry.getValue();
            state.alpha = approach(state.alpha, 0.0f, 0.10f);
            return state.alpha <= 0.02f;
        });
    }

    private float approach(float current, float target, float factor) {
        return MathHelper.lerp(MathHelper.clamp(factor, 0.0f, 1.0f), current, target);
    }

    private float getRelativeYaw(Entity entity, float partialTicks, float playerYaw, Vec3d selfPos) {
        Vec3d entityPos = interpolateEntity(entity, partialTicks);
        double dx = entityPos.x - selfPos.x;
        double dz = entityPos.z - selfPos.z;
        float yaw = (float) -Math.toDegrees(Math.atan2(dx, dz));
        return MathHelper.wrapDegrees(yaw - playerYaw);
    }

    private float getReferenceYaw(float partialTicks) {
        if (FreeLookComponent.isActive()) return FreeLookComponent.getFreeYaw();
        return MathHelper.lerp(partialTicks, mc.player.prevYaw, mc.player.getYaw());
    }

    private Vec3d getReferencePos(float partialTicks) {
        if (FreeLookComponent.isActive() && mc.gameRenderer != null && mc.gameRenderer.getCamera() != null)
            return mc.gameRenderer.getCamera().getPos();
        return interpolateEntity(mc.player, partialTicks);
    }

    private Vec3d interpolateEntity(Entity entity, float pt) {
        return new Vec3d(
                MathHelper.lerp(pt, entity.prevX, entity.getX()),
                MathHelper.lerp(pt, entity.prevY, entity.getY()),
                MathHelper.lerp(pt, entity.prevZ, entity.getZ())
        );
    }

    private float interpolateAngle(float current, float target, float factor) {
        float delta = MathHelper.wrapDegrees(target - current);
        return current + delta * factor;
    }

    private static int rgba(int r, int g, int b, int a) {
        return (a << 24) | (r << 16) | (g << 8) | b;
    }

    private static int applyAlpha(int color, float alpha) {
        int a = MathHelper.clamp((int) (alpha * (color >> 24 & 0xFF)), 0, 255);
        return (a << 24) | (color & 0x00FFFFFF);
    }

    private static final class ArrowState {
        private float alpha;
        private float rotation;
    }
}

