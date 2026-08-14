package com.igor.itemcarry;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.entity.ItemEntity;
import net.minecraft.network.PacketByteBuf;
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

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (client.player == null || client.world == null) return;
            while (pickupKey.wasPressed()) {
                ItemEntity target = getTargetedItem(client);
                if (target != null) {
                    PacketByteBuf buf = PacketByteBufs.create();
                    buf.writeInt(target.getId());
                    ClientPlayNetworking.send(ItemCarryInit.PICKUP_CHANNEL, buf);
                }
            }
        });
    }

    public static void onAttackItem(ItemEntity item) {
        PacketByteBuf buf = PacketByteBufs.create();
        buf.writeInt(item.getId());
        ClientPlayNetworking.send(ItemCarryInit.CARRY_TOGGLE_CHANNEL, buf);
    }

    private static ItemEntity getTargetedItem(MinecraftClient client) {
        ClientPlayerEntity player = client.player;
        if (player == null || client.world == null) return null;

        Vec3d pos = player.getCameraPosVec(1.0f);
        Vec3d dir = player.getRotationVec(1.0f);
        Vec3d end = pos.add(dir.multiply(4.5));
        Box box = player.getBoundingBox().stretch(dir.multiply(4.5)).expand(1.0);

        ItemEntity closest = null;
        double closestDist = 4.5 * 4.5;

        for (ItemEntity item : client.world.getEntitiesByClass(ItemEntity.class, box, e -> true)) {
            Optional<Vec3d> hit = item.getBoundingBox().expand(0.3).raycast(pos, end);
            if (hit.isPresent()) {
                double dist = pos.squaredDistanceTo(hit.get());
                if (dist < closestDist) {
                    closestDist = dist;
                    closest = item;
                }
            }
        }
        return closest;
    }
}
