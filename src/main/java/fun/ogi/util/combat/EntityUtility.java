package fun.ogi.util.combat;

import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.AxeItem;
import net.minecraft.item.MaceItem;
import net.minecraft.item.SwordItem;
import net.minecraft.item.TridentItem;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;

import static fun.ogi.util.MinecraftUtil.mc;

public class EntityUtility {

    public static Block getBlock(double x, double y, double z) {
        if (mc.player == null || mc.world == null) return Blocks.AIR;
        return mc.world.getBlockState(BlockPos.ofFloored(mc.player.getPos().add(x, y, z))).getBlock();
    }

    public static boolean collideWith(LivingEntity entity) {
        return collideWith(entity, 0.0F);
    }

    public static boolean collideWith(LivingEntity entity, float grow) {
        if (mc.player == null) return false;
        Box box = mc.player.getBoundingBox();
        Box targetBox = entity.getBoundingBox().expand(grow, 0.0, grow);
        return box.intersects(targetBox);
    }

    public static boolean isHoldingWeapon() {
        if (mc.player == null) return false;
        var item = mc.player.getMainHandStack().getItem();
        return item instanceof SwordItem || item instanceof AxeItem || item instanceof TridentItem || item instanceof MaceItem;
    }

    public static boolean isInGame() {
        return mc.player != null && mc.world != null;
    }
}

