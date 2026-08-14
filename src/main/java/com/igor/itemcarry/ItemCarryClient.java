package com.igor.itemcarry;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.entity.ItemEntity;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import org.lwjgl.glfw.GLFW;
import java.util.Optional;

public class ItemCarryClient implements ClientModInitializer {
    private static KeyBinding pickupKey;

    @Override
    public void onInitializeClient() {
        pickupKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
            "key.itemcarry.pickup",
            InputUtil.Type.KEYSYM,
            GLFW.GLFW_KEY_R,
            "key.categories.itemcarry"
        ));
    }

    public static void onAttackItem(ItemEntity item) {
        // Triggered by mixin when left-clicking item
        // Server-side pickup will be handled by ServerPlayNetworking
    }

    public static ItemEntity getTargetedItem(MinecraftClient client) {
        ClientPlayerEntity player = client.player;
        if (player == null || client.world == null) return null;

        Vec3d cameraPos = player.getCameraPosVec(1.0f);
        Vec3d look = player.getRotationVec(1.0f);
        Vec3d reachEnd = cameraPos.add(look.multiply(4.5));
        Box searchBox = player.getBoundingBox().stretch(look.multiply(4.5)).expand(1.0);

        ItemEntity closest = null;
        double closestDist = 4.5 * 4.5;

        for (ItemEntity item : client.world.getEntitiesByClass(ItemEntity.class, searchBox, e -> true)) {
            Optional<Vec3d> hit = item.getBoundingBox().expand(0.3).raycast(cameraPos, reachEnd);
            if (hit.isPresent()) {
                double dist = cameraPos.squaredDistanceTo(hit.get());
                if (dist < closestDist) {
                    closestDist = dist;
                    closest = item;
                }
            }
        }
        return closest;
    }
}
