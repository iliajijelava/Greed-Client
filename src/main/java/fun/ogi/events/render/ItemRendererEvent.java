package fun.ogi.events.render;

import fun.ogi.events.Event;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Hand;

public class ItemRendererEvent extends Event {
    private AbstractClientPlayerEntity player;
    private ItemStack stack;
    private Hand hand;

    public ItemRendererEvent(AbstractClientPlayerEntity player, ItemStack stack, Hand hand) {
        this.player = player;
        this.stack = stack;
        this.hand = hand;
    }

    public AbstractClientPlayerEntity getPlayer() {
        return player;
    }

    public void setPlayer(AbstractClientPlayerEntity player) {
        this.player = player;
    }

    public ItemStack getStack() {
        return stack;
    }

    public void setStack(ItemStack stack) {
        this.stack = stack;
    }

    public Hand getHand() {
        return hand;
    }

    public void setHand(Hand hand) {
        this.hand = hand;
    }
}

