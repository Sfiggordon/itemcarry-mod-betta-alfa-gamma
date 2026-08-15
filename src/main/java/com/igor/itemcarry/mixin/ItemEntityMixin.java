package com.igor.itemcarry.mixin;

import net.minecraft.entity.ItemEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ItemEntity.class)
public class ItemEntityMixin {
    @Inject(method = "canHit", at = @At("HEAD"), cancellable = true)
    private void itemcarry$canHit(CallbackInfoReturnable<Boolean> cir) {
        cir.setReturnValue(true);
    }
}
