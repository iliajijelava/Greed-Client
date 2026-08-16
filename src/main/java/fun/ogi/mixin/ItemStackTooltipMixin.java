
package fun.ogi.mixin;

import fun.ogi.module.impl.list.misc.AucHelper;

import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.text.Text;

import org.jetbrains.annotations.Nullable;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;

@Mixin(ItemStack.class)
public abstract class ItemStackTooltipMixin {

    @Inject(
            method = "getTooltip",
            at = @At("RETURN")
    )
    private void ogi$addPerItemPrice(
            Item.TooltipContext context, PlayerEntity player, TooltipType type, CallbackInfoReturnable<List<Text>> cir
    ) {
        AucHelper.process(
                (ItemStack) (Object) this,
                cir.getReturnValue()
        );
    }
}

