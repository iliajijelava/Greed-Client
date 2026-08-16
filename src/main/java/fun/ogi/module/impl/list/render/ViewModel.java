package fun.ogi.module.impl.list.render;

import fun.ogi.module.Module;
import fun.ogi.module.ModuleCategory;
import fun.ogi.module.ModuleInformation;
import fun.ogi.module.settings.BooleanSetting;
import fun.ogi.module.settings.NumberSetting;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Arm;
import net.minecraft.util.Hand;

@ModuleInformation(moduleName = "ViewModel", moduleCategory = ModuleCategory.RENDER)
public class ViewModel extends Module {
    public static ViewModel INSTANCE;

    public final NumberSetting rightX = new NumberSetting("Right X", this, 0.0, -2.0, 2.0, 0.01);
    public final NumberSetting rightY = new NumberSetting("Right Y", this, 0.0, -2.0, 2.0, 0.01);
    public final NumberSetting rightZ = new NumberSetting("Right Z", this, 0.0, -2.0, 2.0, 0.01);

    public final NumberSetting leftX = new NumberSetting("Left X", this, 0.0, -2.0, 2.0, 0.01);
    public final NumberSetting leftY = new NumberSetting("Left Y", this, 0.0, -2.0, 2.0, 0.01);
    public final NumberSetting leftZ = new NumberSetting("Left Z", this, 0.0, -2.0, 2.0, 0.01);

    public final BooleanSetting onlyAura = new BooleanSetting("Aura Only", this, false);

    public ViewModel() {
        INSTANCE = this;
        addSetting(rightX);
        addSetting(rightY);
        addSetting(rightZ);
        addSetting(leftX);
        addSetting(leftY);
        addSetting(leftZ);
        addSetting(onlyAura);
    }

    public void applyHandPosition(MatrixStack matrices, Hand hand) {
        Arm arm = hand == Hand.MAIN_HAND ? mc.player.getMainArm() : mc.player.getMainArm().getOpposite();
        if (arm == Arm.RIGHT) {
            matrices.translate(rightX.getFloatValue(), rightY.getFloatValue(), rightZ.getFloatValue());
        } else {
            matrices.translate(leftX.getFloatValue(), leftY.getFloatValue(), leftZ.getFloatValue());
        }
    }
}

