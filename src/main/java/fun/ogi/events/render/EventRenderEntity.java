package fun.ogi.events.render;

import fun.ogi.events.Event;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.entity.Entity;

public class EventRenderEntity extends Event {
    private final Entity entity;
    private final MatrixStack matrices;
    private final VertexConsumerProvider vertexConsumers;
    private final int light;
    private double x, y, z;

    public EventRenderEntity(Entity entity, MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light) {
        this.entity = entity;
        this.matrices = matrices;
        this.vertexConsumers = vertexConsumers;
        this.light = light;
    }

    public Entity getEntity() {
        return entity;
    }

    public MatrixStack getMatrices() {
        return matrices;
    }

    public VertexConsumerProvider getVertexConsumers() {
        return vertexConsumers;
    }

    public int getLight() {
        return light;
    }

    public void setPosition(double x, double y, double z) {
        this.x = x;
        this.y = y;
        this.z = z;
    }

    public double getX() { return x; }
    public double getY() { return y; }
    public double getZ() { return z; }
}

