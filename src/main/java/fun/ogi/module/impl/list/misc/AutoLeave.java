package fun.ogi.module.impl.list.misc;

import com.google.common.eventbus.Subscribe;
import fun.ogi.Cheap;
import fun.ogi.events.render.EventUpdate;
import fun.ogi.module.Module;
import fun.ogi.module.ModuleCategory;
import fun.ogi.module.ModuleInformation;
import fun.ogi.module.settings.BooleanSetting;
import fun.ogi.module.settings.ListSetting;
import fun.ogi.module.settings.ModeSetting;
import fun.ogi.module.settings.SliderSetting;
import fun.ogi.util.NotificationManager;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.scoreboard.Team;
import net.minecraft.text.Text;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

@ModuleInformation(moduleName = "Auto leave", moduleDesc = "Automatically leaves from server when player is near.", moduleCategory = ModuleCategory.MISC)
public class AutoLeave extends Module {

    public static final AutoLeave INSTANCE = new AutoLeave();

    private static final Set<String> STAFF_PREFIXES = new HashSet<>(Arrays.asList(
            "supp", "mod", "der", "adm", "wne", "curat", "dev", "yt",
            "мод", "помо", "адм", "владе", "курато", "сапп", "ютуб", "стажер", "сотрудник"
    ));

    private final SliderSetting leaveDistance = new SliderSetting("Distance",this, 5.0f, 3.0f, 50.0f, 1.0f);
    private final ListSetting leaveIfSeen = new ListSetting("Leave if seen",this, "Player", "Moderator");
    private final ModeSetting leaveType = new ModeSetting("Leave mode", this,"Into Title screen", "Into Title screen", "/hub", "/home", "/spawn");
    private final BooleanSetting stopBaritone = new BooleanSetting("Turn off Baritone",this, false);
    private final BooleanSetting leaveDisable = new BooleanSetting("Disable after leave",this, true);

    private int cooldownTicks;

    public AutoLeave() {
        addSettings(leaveDistance, leaveIfSeen, leaveType, stopBaritone, leaveDisable);
    }

    @Override
    public void onEnable() {
        cooldownTicks = 0;
        super.onEnable();
    }

    @Override
    public void onDisable() {
        cooldownTicks = 0;
        super.onDisable();
    }

    @Subscribe
    public void onUpdate(EventUpdate event) {
        if (mc.player == null || mc.world == null) {
            return;
        }

        if (cooldownTicks > 0) {
            cooldownTicks--;
            return;
        }

        float maxDistance = leaveDistance.getFloatValue();
        for (PlayerEntity player : mc.world.getPlayers()) {
            if (player == null || player == mc.player) {
                continue;
            }

            if (mc.player.distanceTo(player) <= maxDistance && shouldLeaveFor(player)) {
                triggerLeave();
                break;
            }
        }
    }

    private boolean shouldLeaveFor(PlayerEntity player) {
        if (isModerator(player)) {
            return leaveIfSeen.isSelected("Moderator");
        }
        return leaveIfSeen.isSelected("Player");
    }

    private boolean isModerator(PlayerEntity player) {
        if (player == null) {
            return false;
        }

        String name = player.getName().getString();
        if (Cheap.getInstance() != null && Cheap.getInstance().getStaffManager() != null && Cheap.getInstance().getStaffManager().contains(name)) {
            return true;
        }

        Team team = player.getScoreboardTeam();
        if (team == null) {
            return false;
        }

        String prefix = team.getPrefix().getString().toLowerCase(Locale.ROOT);
        for (String candidate : STAFF_PREFIXES) {
            if (prefix.contains(candidate)) {
                return true;
            }
        }

        return false;
    }

    private void triggerLeave() {
        tryStopBaritone();

        switch (leaveType.getValueAsString()) {
            case "Into Title  screen" -> disconnectLeave();
            case "/hub" -> commandLeave("hub");
            case "/home" -> commandLeave("home home");
            case "/spawn" -> commandLeave("spawn");
        }
    }

    private void tryStopBaritone() {
        if (!stopBaritone.getValue() || mc.getNetworkHandler() == null) {
            return;
        }
        mc.getNetworkHandler().sendChatMessage("#stop");
    }

    private void disconnectLeave() {
        if (mc.getNetworkHandler() == null) {
            NotificationManager.post("Module doesn't working in single player");
            return;
        }

        mc.getNetworkHandler().getConnection().disconnect(Text.literal("AutoLeave"));
        if (leaveDisable.getValue()) {
            toggle();
        }
    }

    private void commandLeave(String command) {
        if (mc.getNetworkHandler() == null) {
            NotificationManager.post("You cant use AutoLeave in single player!");
            return;
        }

        mc.getNetworkHandler().sendChatCommand(command);
        cooldownTicks = leaveDisable.getValue() ? 10 : 30;

        if (leaveDisable.getValue()) {
            toggle();
        }
    }
}

