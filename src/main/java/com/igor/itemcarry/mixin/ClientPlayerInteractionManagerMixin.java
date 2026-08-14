package com.igor.itemcarry.mixin;

import com.igor.itemcarry.ItemCarryClient;
import net.minecraft.client.network.ClientPlayerInteractionManager;
import net.minecraft.entity.Entity;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.player.PlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Vanilla normally kicks the player for "attacking" an entity type that isn't meant to be attacked
 * (ItemEntity is one of them). So instead of letting the vanilla attack packet get sent, we intercept
 * the click here, cancel it, and handle our own carry-toggle logic instead.
 */
@Mixin(ClientPlayerInteractionManager.class)
public class ClientPlayerInteractionManagerMixin {

    @Inject(method = "attackEntity", at = @At("HEAD"), cancellable = true)
    private void itemcarry$onAttackEntity(PlayerEntity player, Entity target, CallbackInfo ci) {
        if (target instanceof ItemEntity itemEntity) {
            ItemCarryClient.onAttackItemEntity(itemEntity);
            ci.cancel();
        }
    }
}
