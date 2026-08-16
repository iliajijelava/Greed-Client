package fun.ogi.events.render;

import fun.ogi.events.Event;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Hand;

public class HandAnimationEvent extends Event {
    private final MatrixStack matrices;
    private final Hand hand;
    private final float swingProgress;

    public HandAnimationEvent(MatrixStack matrices, Hand hand, float swingProgress) {
        this.matrices = matrices;
        this.hand = hand;
        this.swingProgress = swingProgress;
    }

    public MatrixStack getMatrices() {
        return matrices;
    }

    public Hand getHand() {
        return hand;
    }

    public float getSwingProgress() {
        return swingProgress;
    }
}

