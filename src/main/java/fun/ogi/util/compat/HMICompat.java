
package fun.ogi.util.compat;

import fun.ogi.module.impl.list.render.SwingAnimations;
import net.fabricmc.loader.api.FabricLoader;

public final class HMICompat {

    private static final String MOD_ID = "hold-my-items";

    private HMICompat() {
    }

    


    public static boolean isLoaded() {
        return FabricLoader.getInstance().isModLoaded(MOD_ID);
    }

    












    public static boolean shouldUseHMI() {
        SwingAnimations swing = SwingAnimations.INSTANCE;

        return isLoaded()
                && swing != null
                && swing.isEnabled()
                && swing.swingEnabled.getValue()
                && "HMI".equals(swing.swingType.getValue());
    }
}

