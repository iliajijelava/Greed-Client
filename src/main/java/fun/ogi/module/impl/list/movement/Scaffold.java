package fun.ogi.module.impl.list.movement;

import com.google.common.eventbus.Subscribe;
import fun.ogi.events.render.EventUpdate;
import fun.ogi.module.Module;
import fun.ogi.module.ModuleCategory;
import fun.ogi.module.ModuleInformation;
import fun.ogi.module.settings.ModeSetting;
import fun.ogi.util.rotation.Rotation;
import fun.ogi.util.rotation.RotationComponent;
import net.minecraft.block.BlockState;
import net.minecraft.item.BlockItem;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;

@ModuleInformation(
        moduleName = "Scaffold",
        moduleDesc = "Automatically sets block under you when you are falling",
        moduleCategory = ModuleCategory.MOVEMENT
)
public class Scaffold extends Module {

    private final ModeSetting mode =
            new ModeSetting("Mode:", this, "Legit","Legit");

    private int placeCooldown = 0;
    private BlockPos lockedSupport = null;

    public Scaffold() {
        addSetting(mode);
    }

    @Override
    public void onDisable() {
        lockedSupport = null;
    }

    @Subscribe
    private void onEventUpdate(EventUpdate e) {

        if (mc.player == null || mc.world == null || mc.interactionManager == null)
            return;

        if (placeCooldown > 0) {
            placeCooldown--;
            return;
        }

        ItemStack stack = mc.player.getMainHandStack();

        if (!(stack.getItem() instanceof BlockItem))
            return;

        BlockPos targetPos = getTargetPos();

        if (targetPos == null) {
            lockedSupport = null;
            return;
        }

        BlockPos supportPos = findSupportBlock(targetPos);

        if (supportPos == null)
            return;

        Direction direction = Direction.fromVector(
                targetPos.getX() - supportPos.getX(),
                targetPos.getY() - supportPos.getY(),
                targetPos.getZ() - supportPos.getZ(),
                Direction.UP
        );

        Vec3d hitPos = Vec3d.ofCenter(supportPos).add(
                direction.getOffsetX() * 0.5,
                direction.getOffsetY() * 0.5,
                direction.getOffsetZ() * 0.5
        );

        BlockHitResult hitResult = new BlockHitResult(
                hitPos,
                direction,
                supportPos,
                false
        );

        Rotation rotation = getRotationTo(hitPos, direction);

        RotationComponent.update(
                rotation,
                180,
                180,
                60,
                60,
                1,
                1,
                false
        );

        mc.interactionManager.interactBlock(
                mc.player,
                Hand.MAIN_HAND,
                hitResult
        );

        placeCooldown = 2;
    }

    private BlockPos getTargetPos() {
        BlockPos feet = mc.player.getBlockPos();
        BlockPos target = feet.down();

        if (isAirish(target))
            return target;

        return null;
    }

    private boolean isAirish(BlockPos pos) {
        BlockState state = mc.world.getBlockState(pos);
        return state.isAir() || state.isReplaceable();
    }

    private boolean isSolid(BlockPos pos) {
        BlockState state = mc.world.getBlockState(pos);
        return !state.isAir() && !state.isReplaceable();
    }

    private BlockPos findSupportBlock(BlockPos target) {

        BlockPos down = target.down();

        if (isSolid(down)) {
            lockedSupport = null;
            return down;
        }

        if (lockedSupport != null && isSolid(lockedSupport))
            return lockedSupport;

        BlockPos[] neighbors = {
                target.north(),
                target.south(),
                target.east(),
                target.west()
        };

        Vec3d feet = mc.player.getPos();
        BlockPos best = null;
        double bestDist = Double.MAX_VALUE;

        for (BlockPos pos : neighbors) {
            if (!isSolid(pos))
                continue;

            double dist = Vec3d.ofCenter(pos).squaredDistanceTo(feet);

            if (dist < bestDist) {
                bestDist = dist;
                best = pos;
            }
        }

        lockedSupport = best;
        return best;
    }

    private Rotation getRotationTo(Vec3d target, Direction direction) {

        if (direction == Direction.UP) {
            return new Rotation(mc.player.getYaw(), 90f);
        }

        Vec3d eyes = mc.player.getEyePos();

        double dx = target.x - eyes.x;
        double dy = target.y - eyes.y;
        double dz = target.z - eyes.z;

        double horizontal = Math.sqrt(dx * dx + dz * dz);

        float yaw = (float) Math.toDegrees(
                Math.atan2(dz, dx)
        ) - 90.0F;

        float pitch = (float) -Math.toDegrees(
                Math.atan2(dy, horizontal)
        );

        return new Rotation(yaw, pitch);
    }
}

