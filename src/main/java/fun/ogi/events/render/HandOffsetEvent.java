package fun.ogi.events.render;

import fun.ogi.events.Event;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Hand;

public class HandOffsetEvent extends Event {
    private final MatrixStack matrices;
    private final ItemStack stack;
    private final Hand hand;

    public HandOffsetEvent(MatrixStack matrices, ItemStack stack, Hand hand) {
        this.matrices = matrices;
        this.stack = stack;
        this.hand = hand;
    }

    public MatrixStack getMatrices() {
        return matrices;
    }

    public ItemStack getStack() {
        return stack;
    }

    public Hand getHand() {
        return hand;
    }
}

