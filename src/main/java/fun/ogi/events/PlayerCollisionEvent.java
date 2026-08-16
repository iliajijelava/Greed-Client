package fun.ogi.events;

import net.minecraft.block.Block;

public class PlayerCollisionEvent extends Event {
    private final Block block;

    public PlayerCollisionEvent(Block block) {
        this.block = block;
    }

    public Block getBlock() {
        return block;
    }
}

