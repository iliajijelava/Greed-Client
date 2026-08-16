package fun.ogi.module.impl.list.render;

import fun.ogi.module.Module;
import fun.ogi.module.ModuleCategory;
import fun.ogi.module.ModuleInformation;
import fun.ogi.module.settings.ListSetting;
import fun.ogi.module.settings.ModeSetting;
import fun.ogi.module.settings.SliderSetting;
import net.fabricmc.fabric.api.client.rendering.v1.LivingEntityFeatureRendererRegistrationCallback;
import net.minecraft.client.render.entity.feature.FeatureRendererContext;
import net.minecraft.client.render.entity.model.PlayerEntityModel;
import net.minecraft.client.render.entity.state.PlayerEntityRenderState;
import net.minecraft.entity.EntityType;


@ModuleInformation(moduleName = "TrollfaceMask",moduleDesc = "Trollface NAXUI", moduleCategory = ModuleCategory.RENDER)
public class TrollfaceMask extends Module {

    private static TrollfaceMask instance;

    public final ListSetting target = new ListSetting("target", this,
            "Self", "Friends", "Everyone");

    public final ModeSetting maskType = new ModeSetting("Style",this,
            "Style 1",
            "Style 1", "Style 2", "Style 3",
            "Style 4", "Style 5", "Style 6");


    public final SliderSetting size = new SliderSetting(
            "Size", this,1.0f, 0.3f, 3.0f, 0.05f);

    public final SliderSetting alpha = new SliderSetting(
            "Opacity",this, 1.0f, 0.1f, 1.0f, 0.05f);

    @SuppressWarnings("unchecked")
    public TrollfaceMask() {
        instance = this;
        addSettings(target,maskType,size,alpha);
        LivingEntityFeatureRendererRegistrationCallback.EVENT.register(
            (entityType, entityRenderer, registrationHelper, context) -> {
                if (entityType == EntityType.PLAYER) {
                    registrationHelper.register(
                        new TrollfaceMaskRenderer(
                            (FeatureRendererContext<PlayerEntityRenderState, PlayerEntityModel>) entityRenderer
                        )
                    );
                }
            }
        );
    }

    public static TrollfaceMask getInstance() { return instance; }
}

