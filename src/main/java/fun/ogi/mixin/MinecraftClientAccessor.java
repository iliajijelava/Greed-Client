package fun.ogi.mixin;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.session.Session;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(MinecraftClient.class)
public interface MinecraftClientAccessor {
    @Accessor("session")
    @Mutable
    void setSession(Session session);

    @Accessor("session")
    Session getSessionAccessor();

    @Accessor("itemUseCooldown")
    int getItemUseCooldown();

    @Accessor("itemUseCooldown")
    void setItemUseCooldown(int cooldown);
}

