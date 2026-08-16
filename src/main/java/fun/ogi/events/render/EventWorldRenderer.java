package fun.ogi.events.render;

import fun.ogi.events.Event;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.client.util.math.MatrixStack;
import org.joml.Matrix4f;

public class EventWorldRenderer extends Event {
    private final RenderTickCounter renderTickCounter;
    private final boolean tick;
    private final MatrixStack matrices;
    private final Matrix4f modelViewMatrix;
    private final Matrix4f projectionMatrix;
    private final Camera camera;

    public EventWorldRenderer(RenderTickCounter renderTickCounter, boolean tick, MatrixStack matrices, Matrix4f modelViewMatrix, Matrix4f projectionMatrix, Camera camera) {
        this.renderTickCounter = renderTickCounter;
        this.tick = tick;
        this.matrices = matrices;
        this.modelViewMatrix = modelViewMatrix;
        this.projectionMatrix = projectionMatrix;
        this.camera = camera;

    }

    public RenderTickCounter getRenderTickCounter() {
        return renderTickCounter;
    }

    public boolean isTick() {
        return tick;
    }

    public MatrixStack getMatrices() {
        return matrices;
    }

    public Matrix4f getModelViewMatrix() {
        return modelViewMatrix;
    }

    public Matrix4f getProjectionMatrix() {
        return projectionMatrix;

    }

    public Camera getCamera() {
        return camera;
    }

}

