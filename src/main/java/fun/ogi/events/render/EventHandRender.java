package fun.ogi.events.render;

import fun.ogi.events.Event;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Arm;

public class EventHandRender extends Event {
    private final MatrixStack matrices;
    private final Arm arm;

    public EventHandRender(MatrixStack matrices, Arm arm) {
        this.matrices = matrices;
        this.arm = arm;
    }

    public MatrixStack getMatrices() {
        return matrices;
    }

    public Arm getArm() {
        return arm;
    }
}

