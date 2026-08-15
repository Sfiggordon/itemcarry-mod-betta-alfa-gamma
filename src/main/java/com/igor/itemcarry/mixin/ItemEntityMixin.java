package com.igor.itemcarry.mixin;

import net.minecraft.entity.Entity;
import net.minecraft.entity.ItemEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Entity.class)
public class ItemEntityMixin {
    @Inject(method = "canHit", at = @At("HEAD"), cancellable = true)
    private void itemcarry$canHit(CallbackInfoReturnable<Boolean> cir) {
        if ((Object) this instanceof ItemEntity) {
            cir.setReturnValue(true);
        }
    }
}
