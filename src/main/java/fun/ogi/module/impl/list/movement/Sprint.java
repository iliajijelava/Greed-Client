package fun.ogi.module.impl.list.movement;

import com.google.common.eventbus.Subscribe;
import fun.ogi.Cheap;
import fun.ogi.events.render.EventUpdate;
import fun.ogi.module.Module;
import fun.ogi.module.ModuleCategory;
import fun.ogi.module.ModuleInformation;
import fun.ogi.module.settings.BooleanSetting;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;

@ModuleInformation(moduleName = "Sprint", moduleDesc = "Auto sprint", moduleCategory = ModuleCategory.MOVEMENT)
public class Sprint extends Module {
    public static Sprint INSTANCE = new Sprint();
    private static final MinecraftClient CLIENT = MinecraftClient.getInstance();

    public final BooleanSetting keepInWater = new BooleanSetting("Keep in Water", this, false);

    private static boolean sprinting = true;
    private static long time = 0;
    private static int pauseDepth = 0;
    private static boolean restoreAfterPause = false;
    private ClientPlayerEntity lastPlayer;

    public Sprint() {
        addSettings(keepInWater);
    }

    @Override
    public void onEnable() {
        resetPauseState();
        sprinting = true;
        super.onEnable();
    }

    @Override
    public void onDisable() {
        resetPauseState();
        sprinting = false;
        lastPlayer = null;
        if (mc.options != null) {
            mc.options.sprintKey.setPressed(false);
        }
        if (mc.player != null) {
            mc.player.setSprinting(false);
        }
        super.onDisable();
    }

    @Subscribe
    public void onEvent(EventUpdate ignored) {
        if (mc.player == null) {
            lastPlayer = null;
            resetPauseState();
            if (mc.options != null) {
                mc.options.sprintKey.setPressed(false);
            }
            return;
        }

        if (lastPlayer != mc.player) {
            lastPlayer = mc.player;
            resetPauseState();
            sprinting = true;
        }

        boolean inWater = mc.player.isTouchingWater() || mc.player.isSubmergedInWater();
        boolean shouldSprint = !blockedByAura
                && pauseDepth == 0
                && System.currentTimeMillis() >= time
                && sprinting
                && (mc.player.input.movementForward != 0 || mc.player.input.movementSideways != 0)
                && mc.player.input.movementForward > 0.0F
                && !mc.player.isGliding();

        if (keepInWater.getValue() && inWater && mc.player.isSprinting()) {
            shouldSprint = true;
        }

        mc.options.sprintKey.setPressed(shouldSprint);
        mc.player.setSprinting(shouldSprint);
    }

    public boolean shouldKeepSprintInWater() {
        return isEnabled() && keepInWater.getValue();
    }

    public static void pushPause(long delayMs) {
        restoreAfterPause |= shouldRestoreAfterPause();
        pauseDepth++;
        time = Math.max(time, System.currentTimeMillis() + Math.max(0L, delayMs));
        sprinting = false;

        if (CLIENT.options != null) {
            CLIENT.options.sprintKey.setPressed(false);
        }

        if (CLIENT.player != null) {
            CLIENT.player.setSprinting(false);
        }
    }

    public static void popPause() {
        if (pauseDepth > 0) {
            pauseDepth--;
        }

        if (pauseDepth > 0) {
            return;
        }

        time = 0;
        sprinting = restoreAfterPause;
        restoreAfterPause = false;
    }

    private static boolean shouldRestoreAfterPause() {
        if (CLIENT.player != null && CLIENT.player.isSprinting()) {
            return true;
        }

        Sprint sprint = Cheap.getInstance().getModuleStorage().get(Sprint.class);
        return sprint != null
                && sprint.isEnabled()
                && sprinting;
    }

    private static void resetPauseState() {
        pauseDepth = 0;
        restoreAfterPause = false;
        time = 0;
    }
    public static boolean isPaused() {
        return pauseDepth > 0;
    }
    private static boolean blockedByAura = false;

    public static void setBlockedByAura(boolean blocked) {
        blockedByAura = blocked;
    }
}

