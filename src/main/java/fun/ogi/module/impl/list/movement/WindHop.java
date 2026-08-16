package fun.ogi.module.impl.list.movement;

import com.google.common.eventbus.Subscribe;
import fun.ogi.events.render.EventUpdate;
import fun.ogi.module.Module;
import fun.ogi.module.ModuleCategory;
import fun.ogi.module.ModuleInformation;
import fun.ogi.module.settings.BooleanSetting;
import fun.ogi.util.inventory.InventoryUtility;
import fun.ogi.util.inventory.ItemSlot;
import fun.ogi.util.inventory.group.SlotGroup;
import fun.ogi.util.inventory.group.SlotGroups;
import fun.ogi.util.inventory.slots.HotbarSlot;
import fun.ogi.util.inventory.slots.OffhandSlot;
import fun.ogi.util.rotation.RotationUtil;
import net.minecraft.item.Items;
import net.minecraft.network.packet.c2s.play.PlayerInteractItemC2SPacket;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec2f;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;

import static fun.ogi.util.MinecraftUtil.mc;

@ModuleInformation(moduleName = "Wind Hop", moduleCategory = ModuleCategory.MOVEMENT, moduleDesc = "Uses wind charges near walls")
public class WindHop extends Module {

    private final BooleanSetting autoJump = new BooleanSetting("AutoJump", this, false);
    private final BooleanSetting swingArm = new BooleanSetting("Swing Arm", this, true);

    private SlotGroup<ItemSlot> searchSlots;
    private boolean wasJumping;
    private long nextUseTime;

    public WindHop() {
        addSettings(autoJump, swingArm);
    }

    @Override
    public void onEnable() {
        searchSlots = SlotGroups.offhand().and(SlotGroups.hotbar());
        wasJumping = false;
        nextUseTime = 0;
    }

    @Subscribe
    public void onUpdate(EventUpdate event) {
        if (mc.player == null || mc.world == null || mc.interactionManager == null) return;
        if (findWindCharge() == null) return;

        boolean jumpPressed = mc.options.jumpKey.isPressed();
        boolean wallAhead = isWallAhead();
        Vec3d hitPos = getWallHitPos();

        boolean wantUse = false;
        if (mc.player.isOnGround()) {
            if (autoJump.getValue() && wallAhead) wantUse = true;
            if (jumpPressed && wallAhead && !wasJumping) wantUse = true;
        } else {
            if (jumpPressed && mc.player.getVelocity().y > 0.4 && wallAhead) wantUse = true;
        }
        wasJumping = jumpPressed;

        if (!wantUse || hitPos == null) return;

        
        if (!rotateTowards(hitPos)) return;

        
        long now = System.currentTimeMillis();
        if (now < nextUseTime) return;

        if (mc.player.isOnGround()) mc.player.jump();
        useWindCharge();
        nextUseTime = now + 250 + (long) (Math.random() * 350);
    }

    
    private boolean rotateTowards(Vec3d hitPos) {
        Vec2f target = new RotationUtil().calculate(hitPos);
        float targetYaw = MathHelper.wrapDegrees(target.x);
        float targetPitch = MathHelper.clamp(target.y, -89.0f, 89.0f);

        
        targetYaw += (float) ((Math.random() - 0.5) * 1.6);
        targetPitch += (float) ((Math.random() - 0.5) * 1.0);

        float curYaw = MathHelper.wrapDegrees(mc.player.getYaw());
        float yawDelta = MathHelper.wrapDegrees(targetYaw - curYaw);
        float pitchDelta = targetPitch - mc.player.getPitch();

        if (Math.abs(yawDelta) > 1.2f || Math.abs(pitchDelta) > 0.8f) {
            float step = Math.min(5.0f, 1.8f + Math.abs(yawDelta) * 0.25f);
            mc.player.setYaw(curYaw + MathHelper.clamp(yawDelta, -step, step));
            float newPitch = mc.player.getPitch() + MathHelper.clamp(pitchDelta, -step * 0.5f, step * 0.5f);
            mc.player.setPitch(MathHelper.clamp(newPitch, -89.0f, 89.0f));
            return false;
        }
        return true;
    }

    private boolean isWallAhead() {
        Vec3d eyePos = mc.player.getEyePos();
        Vec3d look = mc.player.getRotationVec(1.0F);
        Vec3d end = eyePos.add(look.x * 1.5, look.y * 1.5, look.z * 1.5);

        BlockHitResult result = mc.world.raycast(new RaycastContext(
                eyePos, end,
                RaycastContext.ShapeType.COLLIDER,
                RaycastContext.FluidHandling.NONE,
                mc.player
        ));

        return result.getType() == HitResult.Type.BLOCK;
    }

    private Vec3d getWallHitPos() {
        Vec3d eyePos = mc.player.getEyePos();
        Vec3d look = mc.player.getRotationVec(1.0F);
        Vec3d end = eyePos.add(look.x * 1.5, look.y * 1.5, look.z * 1.5);

        BlockHitResult result = mc.world.raycast(new RaycastContext(
                eyePos, end,
                RaycastContext.ShapeType.COLLIDER,
                RaycastContext.FluidHandling.NONE,
                mc.player
        ));

        return result.getType() == HitResult.Type.BLOCK ? result.getPos() : null;
    }

    private ItemSlot findWindCharge() {
        if (searchSlots == null) {
            searchSlots = SlotGroups.offhand().and(SlotGroups.hotbar());
        }
        return searchSlots.findItem(Items.WIND_CHARGE);
    }

    private void useWindCharge() {
        if (mc.player == null || mc.world == null || mc.interactionManager == null) return;

        ItemSlot slot = findWindCharge();
        if (slot == null) return;

        int oldSlot = mc.player.getInventory().selectedSlot;

        if (slot instanceof HotbarSlot hotbarSlot) {
            if (mc.player.getInventory().selectedSlot != hotbarSlot.getSlotId()) {
                InventoryUtility.selectHotbarSlot(hotbarSlot);
            }
        }

        Hand hand = (slot instanceof OffhandSlot) ? Hand.OFF_HAND : Hand.MAIN_HAND;

        Vec3d eyePos = mc.player.getEyePos();
        Vec3d look = mc.player.getRotationVec(1.0F);
        Vec3d end = eyePos.add(look.x * 1.5, look.y * 1.5, look.z * 1.5);

        BlockHitResult result = mc.world.raycast(new RaycastContext(
                eyePos, end,
                RaycastContext.ShapeType.COLLIDER,
                RaycastContext.FluidHandling.NONE,
                mc.player
        ));

        
        float yaw = mc.player.getYaw();
        float pitch = mc.player.getPitch();

        
        if (result.getType() == HitResult.Type.BLOCK) {
            Vec2f rot = new RotationUtil().calculate(result.getPos());
            float yawDelta = MathHelper.wrapDegrees(rot.x - yaw);
            if (Math.abs(yawDelta) < 25.0f) yaw = rot.x;
            pitch = MathHelper.clamp(rot.y, -89.0f, 89.0f);
        }

        
        float gcd = getGCD();
        yaw = yaw - (yaw % gcd) + (float) ((Math.random() - 0.5) * gcd);
        pitch = pitch - (pitch % gcd) + (float) ((Math.random() - 0.5) * gcd);
        pitch = MathHelper.clamp(pitch, -89.0f, 89.0f);

        float finalYaw = yaw;
        float finalPitch = pitch;
        mc.interactionManager.sendSequencedPacket(mc.world,
                sequence -> new PlayerInteractItemC2SPacket(hand, sequence, finalYaw, finalPitch));

        if (swingArm.getValue()) {
            mc.player.swingHand(hand);
        }

        if (slot instanceof HotbarSlot) {
            mc.player.getInventory().selectedSlot = oldSlot;
        }
    }

    private float getGCD() {
        double sensitivity = mc.options.getMouseSensitivity().getValue() * 0.6 + 0.2;
        double pow3 = sensitivity * sensitivity * sensitivity;
        return (float) (pow3 * 1.2);
    }
}

