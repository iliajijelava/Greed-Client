package fun.ogi.util.combat;

import net.minecraft.block.Blocks;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.AxeItem;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ShieldItem;
import net.minecraft.network.packet.c2s.play.HandSwingC2SPacket;
import net.minecraft.network.packet.c2s.play.UpdateSelectedSlotC2SPacket;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;

import static fun.ogi.util.MinecraftUtil.mc;

public class CombatUtility {

    public static boolean canPerformCriticalHit(LivingEntity target, boolean ignoreSprint) {
        if (mc.world == null || mc.player == null) return false;
        return mc.player.isClimbing()
                || !mc.world.getBlockState(mc.player.getBlockPos().up(2)).isAir()
                || mc.player.isTouchingWater()
                || mc.player.isSwimming()
                || mc.world.getBlockState(mc.player.getBlockPos()).isOf(Blocks.COBWEB)
                || mc.player.isInLava()
                || mc.player.fallDistance > 0.0F
                || mc.player.hasVehicle();
    }

    public static boolean canBreakShield(LivingEntity target) {
        if (mc.player == null || mc.player.isDead() || target.isDead()) return false;
        for (int i = 0; i < 9; i++) {
            ItemStack stack = mc.player.getInventory().getStack(i);
            if (stack.getItem() instanceof AxeItem) {
                Vec3d delta = new Vec3d(target.getPos().getX() - mc.player.getPos().getX(), 0.0, target.getPos().getZ() - mc.player.getPos().getZ());
                return delta.dotProduct(target.getRotationVector()) < 0.0;
            }
        }
        return false;
    }

    public static boolean shouldBreakShield(LivingEntity target) {
        return target.isUsingItem() && target.getActiveItem().getItem() instanceof ShieldItem;
    }

    public static void tryBreakShield(LivingEntity target) {
        if (mc.player == null || mc.interactionManager == null) return;
        for (int i = 0; i < 9; i++) {
            ItemStack stack = mc.player.getInventory().getStack(i);
            if (stack.getItem() instanceof AxeItem) {
                if (target instanceof PlayerEntity && target.isUsingItem() && target.getActiveItem().getItem() instanceof ShieldItem) {
                    mc.player.networkHandler.sendPacket(new UpdateSelectedSlotC2SPacket(i));
                    mc.interactionManager.attackEntity(mc.player, target);
                    mc.player.networkHandler.sendPacket(new HandSwingC2SPacket(Hand.MAIN_HAND));
                    mc.player.networkHandler.sendPacket(new UpdateSelectedSlotC2SPacket(mc.player.getInventory().selectedSlot));
                }
                return;
            }
        }
    }

    public static boolean stalin(LivingEntity target) {
        Vec3d pos = target.getPos();
        var box = target.getBoundingBox();
        float off = 0.05F;
        return !isAir(box.minX - off, pos.y, box.minZ - off)
                || !isAir(box.maxX + off, pos.y, box.minZ - off)
                || !isAir(box.minX - off, pos.y, box.maxZ + off)
                || !isAir(box.maxX + off, pos.y, box.maxZ + off);
    }

    private static boolean isAir(double x, double y, double z) {
        return mc.world.getBlockState(new BlockPos((int) x, (int) y, (int) z)).getBlock() == Blocks.AIR;
    }
}

