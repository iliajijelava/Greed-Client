package fun.ogi.mixin;

import com.mojang.blaze3d.systems.RenderSystem;
import fun.ogi.Cheap;
import fun.ogi.events.render.EventWorldRenderer;
import fun.ogi.module.impl.list.render.WorldTweaks;
import net.minecraft.client.option.CloudRenderMode;
import net.minecraft.client.render.*;
import net.minecraft.client.util.ObjectAllocator;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.Vec3d;

import org.joml.Matrix4f;
import org.joml.Matrix4fStack;
import org.joml.Quaternionf;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(WorldRenderer.class)
public class WorldRendererMixin {

    @Shadow
    @Final
    private DefaultFramebufferSet framebufferSet;

    @Inject(method = "renderSky", at = @At("TAIL"))
    private void onRenderSky(FrameGraphBuilder frameGraphBuilder, Camera camera, float tickDelta, Fog fog, CallbackInfo ci) {
        if (Cheap.getInstance() == null || Cheap.getInstance().getModuleStorage() == null) return;
        WorldTweaks tweaks = Cheap.getInstance().getModuleStorage().get(WorldTweaks.class);
        if (tweaks == null || !tweaks.isSkyEnabled()) return;

        RenderPass pass = frameGraphBuilder.createPass("skyshader");
        this.framebufferSet.mainFramebuffer = pass.transfer(this.framebufferSet.mainFramebuffer);
        pass.setRenderer(() -> tweaks.renderSky(camera));
    }

    @Inject(method = "renderClouds", at = @At("HEAD"), cancellable = true)
    private void onRenderClouds(FrameGraphBuilder frameGraphBuilder, Matrix4f positionMatrix, Matrix4f projectionMatrix, CloudRenderMode renderMode, Vec3d cameraPos, float ticks, int color, float cloudHeight, CallbackInfo ci) {
        if (Cheap.getInstance() == null || Cheap.getInstance().getModuleStorage() == null) return;
        WorldTweaks tweaks = Cheap.getInstance().getModuleStorage().get(WorldTweaks.class);
        if (tweaks != null && tweaks.shouldCancelClouds()) {
            ci.cancel();
        }
    }

    @Inject(method = "render", at = @At("TAIL"))
    private void onWorldRender(ObjectAllocator allocator, RenderTickCounter tickCounter, boolean renderBlockOutline, Camera camera, GameRenderer gameRenderer, Matrix4f positionMatrix, Matrix4f projectionMatrix, CallbackInfo ci) {
        Matrix4fStack mvStack = RenderSystem.getModelViewStack();
        mvStack.pushMatrix();
        mvStack.rotate(new Quaternionf(camera.getRotation()).conjugate());

        Cheap.getInstance().getEventBus().post(new EventWorldRenderer(tickCounter, renderBlockOutline, new MatrixStack(), positionMatrix, projectionMatrix, camera));

        mvStack.popMatrix();
    }
}

