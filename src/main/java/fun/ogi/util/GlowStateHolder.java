package fun.ogi.util;

import net.minecraft.entity.Entity;

import java.util.IdentityHashMap;
import java.util.Map;

public final class GlowStateHolder {
    private static final Map<Entity, GlowState> glowStateMap = new IdentityHashMap<>();

    public static void put(Entity entity, boolean wasGlowing) {
        glowStateMap.put(entity, new GlowState(wasGlowing));
    }

    public static GlowState remove(Entity entity) {
        return glowStateMap.remove(entity);
    }

    public static boolean isManaged(Entity entity) {
        return glowStateMap.containsKey(entity);
    }

    public static class GlowState {
        public final boolean wasGlowing;

        GlowState(boolean wasGlowing) {
            this.wasGlowing = wasGlowing;
        }
    }

    private GlowStateHolder() {}
}

