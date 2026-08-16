package fun.ogi.mixin;

import net.minecraft.entity.player.ItemCooldownManager;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Identifier;

import java.util.ArrayList;
import java.util.List;

public class CooldownUtil {

    public static List<String[]> getActiveCooldowns(ItemCooldownManager manager, PlayerEntity player, float tickDelta) {
        List<String[]> result = new ArrayList<>();
        java.util.Set<String> seen = new java.util.HashSet<>();

        for (int i = 0; i < player.getInventory().size(); i++) {
            ItemStack stack = player.getInventory().getStack(i);
            if (stack.isEmpty()) continue;
            if (!manager.isCoolingDown(stack)) continue;

            float progress = manager.getCooldownProgress(stack, tickDelta);
            if (progress <= 0.0f) continue;

            Item item = stack.getItem();
            String name = item.getName().getString();
            if (seen.contains(name)) continue;
            seen.add(name);

            int remaining = getRemainingFromProgress(manager, stack, tickDelta);
            String time = String.format("%d:%02d", remaining / 20 / 60, (remaining / 20) % 60);
            result.add(new String[]{name, time});
        }
        return result;
    }

    public static int getRemainingTicks(ItemCooldownManager manager, ItemStack stack, float tickDelta) {
        Identifier groupId = manager.getGroup(stack);
        if (groupId == null) return 0;

        ItemCooldownManagerAccessor accessor = (ItemCooldownManagerAccessor) manager;
        Object entry = accessor.getEntries().get(groupId);
        if (entry == null) return 0;

        int endTick = ((ItemCooldownManagerEntryAccessor) entry).getEndTick();
        int currentTick = accessor.getTick();
        return Math.max(0, endTick - (currentTick + (int) tickDelta));
    }

    private static int getRemainingFromProgress(ItemCooldownManager manager, ItemStack stack, float tickDelta) {
        return getRemainingTicks(manager, stack, tickDelta);
    }

    public record CooldownEntry(ItemStack stack, String name, String time, float progress) {}

    public static List<CooldownEntry> getActiveCooldownsFull(ItemCooldownManager manager, PlayerEntity player, float tickDelta) {
        List<CooldownEntry> result = new ArrayList<>();
        java.util.Set<String> seen = new java.util.HashSet<>();

        for (int i = 0; i < player.getInventory().size(); i++) {
            ItemStack stack = player.getInventory().getStack(i);
            if (stack.isEmpty()) continue;
            Item item = stack.getItem();
            if (!manager.isCoolingDown(stack)) continue;

            float progress = manager.getCooldownProgress(stack, tickDelta);
            if (progress <= 0.0f) continue;

            String name = item.getName().getString();
            if (seen.contains(name)) continue;
            seen.add(name);

            int remaining = getRemainingTicks(manager, stack, tickDelta);
            String time = String.format("%d:%02d", remaining / 20 / 60, (remaining / 20) % 60);
            result.add(new CooldownEntry(stack, name, time, progress));
        }
        return result;
    }
}

