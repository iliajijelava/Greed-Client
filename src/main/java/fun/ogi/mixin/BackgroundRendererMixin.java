package fun.ogi.mixin;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.llamalad7.mixinextras.sugar.Local;
import fun.ogi.Cheap;
import fun.ogi.module.impl.list.render.Removals;
import fun.ogi.module.impl.list.render.WorldTweaks;
import net.minecraft.block.enums.CameraSubmersionType;
import net.minecraft.client.render.BackgroundRenderer;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.Fog;
import net.minecraft.client.render.FogShape;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffects;
import org.joml.Vector4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(BackgroundRenderer.class)
public class BackgroundRendererMixin {

    @Unique
    private static WorldTweaks phaze$getWorldTweaks() {
        if (Cheap.getInstance() == null || Cheap.getInstance().getModuleStorage() == null) return null;
        return Cheap.getInstance().getModuleStorage().get(WorldTweaks.class);
    }

    @Unique
    private static boolean phaze$shouldntApplyCustomFog(Camera camera, BackgroundRenderer.FogType fogType) {
        WorldTweaks wt = phaze$getWorldTweaks();
        if (wt == null || !wt.isFogEnabled()) return true;
        if (fogType == BackgroundRenderer.FogType.FOG_SKY && !wt.isAffectSky()) return true;
        CameraSubmersionType submersion = camera.getSubmersionType();
        if (submersion != CameraSubmersionType.NONE) return true;
        Entity entity = camera.getFocusedEntity();
        if (entity instanceof LivingEntity living) {
            if (living.hasStatusEffect(StatusEffects.BLINDNESS)
                    || living.hasStatusEffect(StatusEffects.DARKNESS)) {
                return true;
            }
        }
        return false;
    }

    @ModifyReturnValue(method = "getFogColor", at = @At("RETURN"))
    private static Vector4f phaze$onGetFogColor(Vector4f original, @Local(argsOnly = true) Camera camera) {
        if (phaze$shouldntApplyCustomFog(camera, BackgroundRenderer.FogType.FOG_TERRAIN)) {
            return original;
        }
        WorldTweaks wt = phaze$getWorldTweaks();
        int rgb = wt.getFogColor();
        float r = ((rgb >> 16) & 0xFF) / 255.0F;
        float g = ((rgb >> 8) & 0xFF) / 255.0F;
        float b = (rgb & 0xFF) / 255.0F;
        return new Vector4f(r, g, b, 1.0F);
    }

    @ModifyReturnValue(method = "applyFog", at = @At("RETURN"))
    private static Fog phaze$onApplyFog(Fog original,
                                         @Local(argsOnly = true) Camera camera,
                                         @Local(argsOnly = true) BackgroundRenderer.FogType fogType) {
        if (phaze$shouldntApplyCustomFog(camera, fogType)) {
            return original;
        }
        WorldTweaks wt = phaze$getWorldTweaks();
        float distance = wt.getFogDistance();
        float density = phaze$clamp01(wt.getFogDensity());
        float start = distance * (1.0F - density);
        float end = distance;

        int rgb = wt.getFogColor();
        float r = ((rgb >> 16) & 0xFF) / 255.0F;
        float g = ((rgb >> 8) & 0xFF) / 255.0F;
        float b = (rgb & 0xFF) / 255.0F;

        return new Fog(start, end, FogShape.CYLINDER, r, g, b, 1.0F);
    }

    @Unique
    private static float phaze$clamp01(float v) {
        if (v < 0.0F) return 0.0F;
        if (v > 1.0F) return 1.0F;
        return v;
    }

    @Inject(method = "getFogModifier(Lnet/minecraft/entity/Entity;F)Lnet/minecraft/client/render/BackgroundRenderer$StatusEffectFogModifier;", at = @At("HEAD"), cancellable = true)
    private static void greed$getFogModifier(Entity entity, float tickDelta, CallbackInfoReturnable<Object> cir) {
        if (Cheap.getInstance().getModuleStorage() == null) return;

        Removals removals = Cheap.getInstance().getModuleStorage().get(Removals.class);
        if (removals != null && removals.isEnabled("Bad Effects")) {
            cir.setReturnValue(null);
        }
    }
}

