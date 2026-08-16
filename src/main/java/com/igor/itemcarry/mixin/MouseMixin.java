package com.igor.itemcarry.mixin;

import com.igor.itemcarry.ItemCarryClient;
import net.minecraft.client.Mouse;
import org.lwjgl.glfw.GLFW;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Mouse.class)
public class MouseMixin {
    @Inject(method = "onMouseScroll", at = @At("HEAD"), cancellable = true)
    private void itemcarry$onScroll(long window, double horizontal, double vertical, CallbackInfo ci) {
        if (ItemCarryClient.isCarrying()) {
            ItemCarryClient.adjustCarryDistance(vertical);
            ci.cancel();
        }
    }

    @Inject(method = "onMouseButton", at = @At("HEAD"), cancellable = true)
    private void itemcarry$onButton(long window, int button, int action, int mods, CallbackInfo ci) {
        if (ItemCarryClient.isCarrying() && button == GLFW.GLFW_MOUSE_BUTTON_RIGHT && action == GLFW.GLFW_PRESS) {
            ItemCarryClient.onRightClickPlace();
            ci.cancel();
        }
    }
}
