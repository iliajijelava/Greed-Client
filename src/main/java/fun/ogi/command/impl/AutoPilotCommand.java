package fun.ogi.command.impl;

import com.google.common.eventbus.Subscribe;
import fun.ogi.Cheap;
import fun.ogi.command.Command;
import fun.ogi.events.render.EventUpdate;
import fun.ogi.util.chatutil.ChatUtil;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.block.BlockState;
import net.minecraft.network.packet.c2s.play.ClientCommandC2SPacket;
import net.minecraft.network.packet.c2s.play.CloseHandledScreenC2SPacket;
import net.minecraft.network.packet.c2s.play.UpdateSelectedSlotC2SPacket;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.util.Hand;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.BlockPos;

public class AutoPilotCommand extends Command {

    private static final int CHEST_CONTAINER_SLOT = 6;
    private static final long CLICK_COOLDOWN_MS = 200L;
    private static final long FIREWORK_DELAY_MS = 4800L;
    private static final long GLIDE_RETRY_MS = 400L;

    private static final double STOP_SPEED = 0.05;
    private static final double BRAKE_DECEL = 0.05;
    private static final double ARRIVE_DIST = 3.0;
    private static final double BRAKE_PITCH = -30.0;
    private static final float MIN_PITCH = -70.0f;
    private static final float MAX_PITCH = 35.0f;

    private static final double DEFAULT_ALTITUDE = 30.0;
    private static final double GROUND_CLEARANCE = 5.0;
    private static final double CLIMB_EPS = 1.5;
    private static final float CLIMB_PITCH = -70.0f;
    private static final double TERRAIN_SCAN_STEP = 8.0;

    private enum Mode { CLIMB, CRUISE, BRAKE, RETURN }

    private Vec3d target;
    private boolean active;
    private Mode mode = Mode.CRUISE;
    private long lastClickTime;
    private long lastFireworkUse;
    private long lastGlideTry;

    public AutoPilotCommand() {
        super("autopilot", "Fly to coordinates on elytra + fireworks", "ap", "autofly");
    }

    @Override
    public void execute(String[] args) {
        if (args.length == 0) {
            if (active) stop();
            else usage("<X> [Y] <Z>");
            return;
        }

        String first = args[0].toLowerCase();
        if (first.equals("stop") || first.equals("cancel") || first.equals("off")) {
            stop();
            return;
        }

        if (args.length < 2 || args.length > 3) {
            usage("<X> [Y] <Z>");
            return;
        }

        try {
            double x = Double.parseDouble(args[0]);
            double z = Double.parseDouble(args[args.length - 1]);
            double y = args.length == 3 ? Double.parseDouble(args[1]) : DEFAULT_ALTITUDE;
            target = new Vec3d(x, y, z);
        } catch (NumberFormatException e) {
            ChatUtil.sendMSG("§cНеверные координаты.");
            return;
        }

        if (!active) {
            active = true;
            Cheap.getInstance().getEventBus().register(this);
        }
        mode = mc.player != null && mc.player.getY() < target.y - CLIMB_EPS ? Mode.CLIMB : Mode.CRUISE;
        ChatUtil.sendMSG("§aАвтопилот: X: " + (int) target.x + " Y: " + (int) target.y + " Z: " + (int) target.z);
    }

    @Subscribe
    public void onTick(EventUpdate event) {
        if (mc.player == null || mc.world == null) {
            stop();
            return;
        }

        ClientPlayerEntity player = mc.player;
        boolean canClick = mc.currentScreen == null && canClick();

        if (!hasElytraEquipped()) {
            if (!canClick) return;
            int hotbar = findHotbarItem(Items.ELYTRA);
            if (hotbar != -1) {
                click(CHEST_CONTAINER_SLOT, hotbar, SlotActionType.SWAP);
                return;
            }
            int inventory = findInventoryItem(Items.ELYTRA);
            if (inventory != -1) {
                click(inventory, player.getInventory().selectedSlot, SlotActionType.SWAP);
                return;
            }
            fail("У вас нет элитр!");
            return;
        }

        int selected = player.getInventory().selectedSlot;
        if (!player.getInventory().getStack(selected).isOf(Items.FIREWORK_ROCKET)) {
            if (!canClick) return;
            int hotbar = findHotbarItem(Items.FIREWORK_ROCKET);
            if (hotbar != -1) {
                selectSlot(hotbar);
                return;
            }
            int inventory = findInventoryItem(Items.FIREWORK_ROCKET);
            if (inventory != -1) {
                click(inventory, selected, SlotActionType.SWAP);
                return;
            }
            fail("У вас нет фейерверков!");
            return;
        }

        fly(player);
    }

    private void fly(ClientPlayerEntity player) {
        double tx = target.x - player.getX();
        double tyRaw = target.y - player.getY();
        double tz = target.z - player.getZ();
        double hDist = Math.sqrt(tx * tx + tz * tz);

        double safeY = computeSafeAltitude(tx, tz);
        double ty = safeY - player.getY();

        Vec3d vel = player.getVelocity();
        double hSpeed = Math.hypot(vel.x, vel.z);

        boolean moving = hSpeed > 0.01;
        double travelX = moving ? vel.x / hSpeed : 0.0;
        double travelZ = moving ? vel.z / hSpeed : 0.0;
        double signed = tx * travelX + tz * travelZ;
        boolean passed = signed < 0.0;

        if (hSpeed < STOP_SPEED && hDist < ARRIVE_DIST && Math.abs(tyRaw) < ARRIVE_DIST) {
            ChatUtil.sendMSG("§aАвтопилот: прибыли!");
            stop();
            return;
        }

        double brakeDist = brakeDistance(hSpeed);

        switch (mode) {
            case CLIMB -> {
                if (ty < CLIMB_EPS) {
                    mode = Mode.CRUISE;
                }
            }
            case CRUISE -> {
                if (passed) {
                    mode = Mode.RETURN;
                } else if (hDist < brakeDist) {
                    mode = Mode.BRAKE;
                }
            }
            case BRAKE -> {
                if (passed) {
                    mode = Mode.RETURN;
                } else if (hSpeed < STOP_SPEED && hDist > ARRIVE_DIST * 2.0) {
                    mode = Mode.CRUISE;
                } else if (hSpeed > 0.8 && hDist > brakeDist * 1.5) {
                    mode = Mode.CRUISE;
                }
            }
            case RETURN -> {
                if (!passed) {
                    mode = hDist < brakeDist ? Mode.BRAKE : Mode.CRUISE;
                }
            }
        }

        float yaw = (float) Math.toDegrees(Math.atan2(tz, tx)) - 90.0f;
        float pitch;
        boolean useRocket;

        switch (mode) {
            case BRAKE -> {
                pitch = (float) BRAKE_PITCH;
                useRocket = false;
            }
            case CLIMB -> {
                pitch = CLIMB_PITCH;
                useRocket = true;
            }
            default -> {
                double pitchDist = Math.max(hDist, 10.0);
                pitch = (float) -Math.toDegrees(Math.atan2(ty, pitchDist));
                pitch = MathHelper.clamp(pitch, MIN_PITCH, MAX_PITCH);
                useRocket = ty > 0.5 && hDist > ARRIVE_DIST * 2.0;
            }
        }

        player.setYaw(yaw);
        player.headYaw = yaw;
        player.setPitch(pitch);

        if (player.isOnGround()) {
            mc.options.jumpKey.setPressed(true);
        } else {
            mc.options.jumpKey.setPressed(false);
            if (!player.isGliding() && !player.getAbilities().flying
                    && player.getVelocity().y < 0
                    && System.currentTimeMillis() - lastGlideTry >= GLIDE_RETRY_MS) {
                lastGlideTry = System.currentTimeMillis();
                if (!player.isTouchingWater() && !player.isInLava()) {
                    player.startGliding();
                    if (mc.getNetworkHandler() != null) {
                        mc.getNetworkHandler().sendPacket(
                                new ClientCommandC2SPacket(player, ClientCommandC2SPacket.Mode.START_FALL_FLYING)
                        );
                    }
                }
            }
        }

        if (player.isGliding() && useRocket && System.currentTimeMillis() - lastFireworkUse >= FIREWORK_DELAY_MS) {
            lastFireworkUse = System.currentTimeMillis();
            mc.interactionManager.interactItem(player, Hand.MAIN_HAND);
        }
    }

    private double brakeDistance(double hSpeed) {
        double v = Math.max(hSpeed, 0.0);
        return (v * v) / (2.0 * BRAKE_DECEL);
    }

    private double computeSafeAltitude(double tx, double tz) {
        double hDist = Math.sqrt(tx * tx + tz * tz);
        int samples = Math.max(1, (int) Math.ceil(hDist / TERRAIN_SCAN_STEP));
        double maxGround = mc.world.getBottomY();

        for (int i = 0; i <= samples; i++) {
            double f = (double) i / samples;
            int bx = MathHelper.floor(mc.player.getX() + tx * f);
            int bz = MathHelper.floor(mc.player.getZ() + tz * f);
            maxGround = Math.max(maxGround, groundHeight(bx, bz));
        }

        return Math.max(target.y, maxGround + GROUND_CLEARANCE);
    }

    private double groundHeight(int x, int z) {
        int bottom = mc.world.getBottomY();
        int top = (int) Math.max(160.0, target.y + 10.0);

        for (int y = top; y >= bottom; y--) {
            BlockState state = mc.world.getBlockState(new BlockPos(x, y, z));
            if (!state.isAir() && !state.isLiquid()) {
                return y++;
            }
        }

        return bottom;
    }

    private boolean hasElytraEquipped() {
        ItemStack chest = mc.player.getInventory().getArmorStack(2);
        return chest.isOf(Items.ELYTRA) && chest.getDamage() < chest.getMaxDamage() - 1;
    }

    private int findHotbarItem(Item item) {
        for (int i = 0; i < 9; i++) {
            if (mc.player.getInventory().getStack(i).isOf(item)) return i;
        }
        return -1;
    }

    private int findInventoryItem(Item item) {
        for (int i = 9; i < 36; i++) {
            if (mc.player.getInventory().getStack(i).isOf(item)) return i;
        }
        return -1;
    }

    private void selectSlot(int hotbarIndex) {
        mc.player.getInventory().selectedSlot = hotbarIndex;
        if (mc.getNetworkHandler() != null) {
            mc.getNetworkHandler().sendPacket(new UpdateSelectedSlotC2SPacket(hotbarIndex));
        }
    }

    private void click(int slot, int button, SlotActionType type) {
        lastClickTime = System.currentTimeMillis();
        mc.interactionManager.clickSlot(mc.player.playerScreenHandler.syncId, slot, button, type, mc.player);
        if (mc.getNetworkHandler() != null) {
            mc.getNetworkHandler().sendPacket(new CloseHandledScreenC2SPacket(0));
        }
    }

    private boolean canClick() {
        return System.currentTimeMillis() - lastClickTime >= CLICK_COOLDOWN_MS;
    }

    private void fail(String message) {
        ChatUtil.sendMSG("§c" + message);
        stop();
    }

    private void stop() {
        target = null;
        if (active) {
            active = false;
            Cheap.getInstance().getEventBus().unregister(this);
        }
        if (mc.options != null) {
            mc.options.jumpKey.setPressed(false);
        }
    }

    public boolean isActive() {
        return active;
    }
}

