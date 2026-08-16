package fun.ogi.module.impl.list.render;

import com.google.common.eventbus.Subscribe;
import fun.ogi.events.render.AspectRatioEvent;
import fun.ogi.module.Module;
import fun.ogi.module.ModuleCategory;
import fun.ogi.module.ModuleInformation;
import fun.ogi.module.settings.SliderSetting;

@ModuleInformation(moduleName = "Aspect Ratio",moduleDesc = "Aspect ratio blad",moduleCategory = ModuleCategory.RENDER)
public class AspectRatio extends Module {
    private final SliderSetting ratio = new SliderSetting("Ratio",this,1.0f,0.1f,2.0f,0.1f);
    public AspectRatio(){
        addSetting(ratio);
    }
    @Subscribe
    private void onAspectRatio(AspectRatioEvent e){
        e.setRatio(ratio.getFloatValue());
        e.cancel();
    }
}

