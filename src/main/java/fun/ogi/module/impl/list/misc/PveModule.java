package fun.ogi.module.impl.list.misc;

import com.google.common.eventbus.Subscribe;
import fun.ogi.Cheap;
import fun.ogi.module.Module;
import fun.ogi.util.baritone.BaritoneHelper;
import fun.ogi.util.chatutil.ChatUtil;
import fun.ogi.util.time.Timer;
import net.minecraft.block.*;
import net.minecraft.block.entity.*;
import net.minecraft.item.*;
import net.minecraft.network.packet.c2s.play.PlayerActionC2SPacket;
import net.minecraft.network.packet.c2s.play.UpdateSelectedSlotC2SPacket;
import net.minecraft.potion.Potion;
import net.minecraft.potion.Potions;
import net.minecraft.screen.GenericContainerScreenHandler;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.slot.Slot;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.text.Text;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.PotionContentsComponent;

import java.util.ArrayList;
import java.util.Locale;
import java.util.List;
import java.util.function.Predicate;


public abstract class PveModule extends Module {
    protected final Timer actionTimer = new Timer();
    protected final Timer commandTimer = new Timer();
    protected final Timer stateTimer = new Timer();
    protected final Timer sellTimer = new Timer();
    protected final Timer warnTimer = new Timer();
    protected final Timer notifyTimer = new Timer();
    protected final Timer toolSwitchTimer = new Timer();
    protected final Timer breakTimer = new Timer();

    protected void lookAt(BlockPos p) {
        if (p == null || mc.player == null) return;
        Vec3d eye = mc.player.getEyePos();
        Vec3d target = p.toCenterPos();
        Vec3d delta = target.subtract(eye);
        double horiz = Math.sqrt(delta.x * delta.x + delta.z * delta.z);
        mc.player.setYaw((float) (Math.toDegrees(Math.atan2(delta.z, delta.x)) - 90.0));
        mc.player.setPitch((float) (-Math.toDegrees(Math.atan2(delta.y, horiz))));
    }

    protected void openContainer(BlockPos p, long delay) {
        if (p == null || mc.player == null || mc.world == null || mc.interactionManager == null || !actionTimer.finished(delay)) return;
        lookAt(p);
        BlockHitResult hit = visibleHit(p);
        if (hit == null) return;
        mc.interactionManager.interactBlock(mc.player, Hand.MAIN_HAND, hit);
        mc.player.swingHand(Hand.MAIN_HAND);
        actionTimer.reset();
    }

    protected void sendCommand(String cmd) {
        if (mc.getNetworkHandler() == null || cmd == null || cmd.isBlank()) return;
        commandTimer.reset();
        if (cmd.startsWith("/")) mc.getNetworkHandler().sendChatCommand(cmd.substring(1));
        else mc.getNetworkHandler().sendChatMessage(cmd);
    }

    protected boolean detectBaritone() {
        return BaritoneHelper.isAvailable();
    }

    protected void runBaritoneCommand(String cmd) {
        BaritoneHelper.sendCommand(cmd);
    }

    protected void stopBaritone() {
        BaritoneHelper.stop();
    }

    protected boolean isBaritonePathing() {
        return BaritoneHelper.isPathing();
    }

    protected boolean hasItem(Predicate<ItemStack> p) {
        for (int i = 0; i < 36; i++) {
            if (p.test(mc.player.getInventory().getStack(i))) return true;
        }
        return false;
    }

    protected int countItem(Item item) {
        int count = 0;
        for (int i = 0; i < 36; i++) {
            ItemStack stack = mc.player.getInventory().getStack(i);
            if (stack.getItem() == item) count += stack.getCount();
        }
        return count;
    }

    protected void throwInventorySlot(int slot) {
        if (mc.player == null || mc.interactionManager == null || slot < 0 || slot >= 36) return;
        ScreenHandler handler = mc.player.currentScreenHandler;
        int handlerSlot = findPlayerInventorySlot(handler, slot);
        if (handlerSlot != -1) {
            mc.interactionManager.clickSlot(handler.syncId, handlerSlot, 0, SlotActionType.THROW, mc.player);
        }
    }

    protected boolean isInventoryFull() {
        for (int i = 0; i < 36; i++) {
            if (mc.player.getInventory().getStack(i).isEmpty()) return false;
        }
        return true;
    }

    protected void click(int syncId, int slot, SlotActionType action) {
        mc.interactionManager.clickSlot(syncId, slot, 0, action, mc.player);
    }

    protected void click(int syncId, int slot, int button, SlotActionType action) {
        mc.interactionManager.clickSlot(syncId, slot, button, action, mc.player);
    }

    protected boolean isChest(BlockPos p) {
        BlockState state = mc.world.getBlockState(p);
        return state.isOf(Blocks.CHEST) || state.isOf(Blocks.TRAPPED_CHEST) || state.isOf(Blocks.BARREL);
    }

    protected boolean isChestOpen() {
        return mc.currentScreen instanceof net.minecraft.client.gui.screen.ingame.GenericContainerScreen;
    }

    protected BlockPos findStorageChest(int r, String sign) {
        String t = sign.trim().toLowerCase(Locale.ROOT);
        if (t.isEmpty()) return null;
        BlockPos best = null;
        double bestD = Double.MAX_VALUE;
        for (BlockPos p : BlockPos.iterate(mc.player.getBlockPos().add(-r,-r,-r), mc.player.getBlockPos().add(r,r,r))) {
            BlockEntity be = mc.world.getBlockEntity(p);
            if (be instanceof SignBlockEntity s) {
                boolean m = false;
                for (Text line : s.getFrontText().getMessages(false)) if (line.getString().toLowerCase(Locale.ROOT).contains(t)) m = true;
                for (Text line : s.getBackText().getMessages(false)) if (line.getString().toLowerCase(Locale.ROOT).contains(t)) m = true;
                if (m) {
                    for (Direction d : Direction.Type.HORIZONTAL) {
                        BlockPos c = p.offset(d);
                        if (isChest(c)) {
                            double dist = mc.player.squaredDistanceTo(c.toCenterPos());
                            if (dist < bestD) {
                                bestD = dist;
                                best = c.toImmutable();
                            }
                        }
                    }
                }
            }
        }
        return best;
    }

    protected <T extends BlockEntity> T findBlockEntity(Class<T> clazz, double range) {
        if (mc.world == null) return null;
        BlockPos pos = mc.player.getBlockPos();
        int r = (int) Math.ceil(range);
        for (int x = -r; x <= r; x++) {
            for (int y = -r; y <= r; y++) {
                for (int z = -r; z <= r; z++) {
                    BlockPos p = pos.add(x, y, z);
                    BlockEntity be = mc.world.getBlockEntity(p);
                    if (clazz.isInstance(be) && mc.player.squaredDistanceTo(p.toCenterPos()) <= range * range) {
                        return clazz.cast(be);
                    }
                }
            }
        }
        return null;
    }

    protected String sanitized(String s) {
        return s.replaceAll("[^0-9]", "");
    }

    protected void warn(String modulePrefix, String m) {
        if (warnTimer.finished(3000L)) {
            ChatUtil.sendMSG("§c[" + modulePrefix + "] " + m);
            warnTimer.reset();
        }
    }

    protected boolean ensureHotbarItem(Predicate<ItemStack> p, double toolSwitchDelayValue) {
        if (mc.player == null || mc.interactionManager == null || p == null) return false;
        if (!this.toolSwitchTimer.finished((long) toolSwitchDelayValue)) {
            return p.test(mc.player.getMainHandStack());
        }
        for (int i = 0; i < 9; i++) {
            if (p.test(mc.player.getInventory().getStack(i))) {
                if (mc.player.getInventory().selectedSlot != i) {
                    mc.player.getInventory().selectedSlot = i;
                    mc.interactionManager.syncSelectedSlot();
                    this.toolSwitchTimer.reset();
                }
                return true;
            }
        }
        for (int i = 9; i < 36; i++) {
            if (p.test(mc.player.getInventory().getStack(i))) {
                ScreenHandler handler = mc.player.currentScreenHandler;
                int handlerSlot = findPlayerInventorySlot(handler, i);
                if (handlerSlot == -1) return false;
                mc.interactionManager.clickSlot(handler.syncId, handlerSlot, mc.player.getInventory().selectedSlot, SlotActionType.SWAP, mc.player);
                mc.interactionManager.syncSelectedSlot();
                this.toolSwitchTimer.reset();
                return true;
            }
        }
        return false;
    }

    protected void autoFixPickaxe(double minDur, Timer mineFixTimer) {
        if (!mineFixTimer.finished(15000L)) return;
        ItemStack p = findBestPickaxe();
        if (!p.isEmpty() && (p.getMaxDamage() - p.getDamage()) <= minDur) {
            runBaritoneCommand("/fix all");
            mineFixTimer.reset();
        }
    }

    protected void autoFeed(Timer mineFeedTimer) {
        if (!mineFeedTimer.finished(180000L)) return;
        runBaritoneCommand("/feed");
        mineFeedTimer.reset();
    }

    protected ItemStack findBestPickaxe() {
        ItemStack best = ItemStack.EMPTY;
        int bestD = -1;
        for (int i = 0; i < 36; i++) {
            ItemStack s = mc.player.getInventory().getStack(i);
            if (s.getItem() instanceof PickaxeItem) {
                int d = s.getMaxDamage() - s.getDamage();
                if (d > bestD) {
                    bestD = d;
                    best = s;
                }
            }
        }
        return best;
    }

    protected boolean shouldEscapeFromPlayer(double radius) {
        double rSq = radius * radius;
        for (PlayerEntity p : mc.world.getPlayers()) {
            if (p == mc.player || p.isSpectator() || Cheap.getInstance().getFriendManager().getFriends().contains(p.getNameForScoreboard())) continue;
            if (mc.player.squaredDistanceTo(p) <= rSq) return true;
        }
        return false;
    }

    protected void useBlock(BlockPos p, Direction side, Hand h) {
        if (p == null || side == null || h == null || mc.player == null || mc.interactionManager == null) return;
        Vec3d hitVec = Vec3d.ofCenter(p).add(Vec3d.of(side.getVector()).multiply(0.5));
        this.lookAt(p);
        mc.interactionManager.interactBlock(mc.player, h, new BlockHitResult(hitVec, side, p, false));
        mc.player.swingHand(h);
    }

    protected void breakBlock(BlockPos p) {
        if (p == null || mc.player == null || mc.interactionManager == null) return;
        this.lookAt(p);
        mc.interactionManager.attackBlock(p, Direction.UP);
        mc.player.swingHand(Hand.MAIN_HAND);
    }

    protected boolean isLog(BlockPos p) {
        String n = mc.world.getBlockState(p).getBlock().getTranslationKey().toLowerCase();
        return n.contains("log") || n.contains("stem") || mc.world.getBlockState(p).isIn(BlockTags.LOGS);
    }

    protected boolean isLeaves(BlockPos p) {
        return mc.world != null && mc.world.getBlockState(p).isIn(BlockTags.LEAVES);
    }

    protected boolean isAxeOrHoe(ItemStack s) {
        return s.getItem() instanceof AxeItem || s.getItem() instanceof HoeItem;
    }

    protected boolean isHoe(ItemStack s) {
        return s.getItem() instanceof HoeItem;
    }

    protected int findPlayerInventorySlot(ScreenHandler handler, int inventoryIndex) {
        if (handler == null || mc.player == null) return -1;
        for (Slot slot : handler.slots) {
            if (slot.inventory == mc.player.getInventory() && slot.getIndex() == inventoryIndex) {
                return slot.id;
            }
        }
        return -1;
    }

    protected BlockHitResult visibleHit(BlockPos pos) {
        if (pos == null || mc.player == null || mc.world == null) return null;
        Vec3d eye = mc.player.getEyePos();
        BlockHitResult best = null;
        double bestDistance = Double.MAX_VALUE;
        for (Direction side : Direction.values()) {
            Vec3d hitPos = Vec3d.ofCenter(pos).add(Vec3d.of(side.getVector()).multiply(0.5));
            BlockHitResult ray = mc.world.raycast(new RaycastContext(eye, hitPos, RaycastContext.ShapeType.OUTLINE, RaycastContext.FluidHandling.NONE, mc.player));
            if (ray.getType() == net.minecraft.util.hit.HitResult.Type.BLOCK && ray.getBlockPos().equals(pos)) {
                double distance = eye.squaredDistanceTo(ray.getPos());
                if (distance < bestDistance) {
                    best = ray;
                    bestDistance = distance;
                }
            }
        }
        return best;
    }
}

