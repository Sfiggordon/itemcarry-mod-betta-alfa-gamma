package com.igor.itemcarry;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.networking.v1.ClientPlayNetworking;
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

    // How far (in blocks) the player can reach to grab an item with the pickup key.
    private static final double PICKUP_REACH = 4.5;

    private static KeyBinding pickupKey;

    @Override
    public void onInitializeClient() {

        pickupKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.itemcarry.pickup",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_R,
                "key.categories.itemcarry"
        ));

        // Server just confirms carry state; we don't strictly need to act on it client-side,
        // but registering the receiver keeps the channel valid and leaves room for HUD feedback later.
        ClientPlayNetworking.registerGlobalReceiver(ItemCarryMod.CARRY_STATE_CHANNEL, (client, handler, buf, responseSender) -> {
            boolean isCarrying = buf.readBoolean();
            int entityId = buf.readInt();
            // no-op for now - reserved for future HUD/UI hooks
        });

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (client.player == null || client.world == null) return;

            while (pickupKey.wasPressed()) {
                ItemEntity target = getTargetedItemEntity(client, PICKUP_REACH);
                if (target != null) {
                    PacketByteBuf buf = PacketByteBufs.create();
                    buf.writeInt(target.getId());
                    ClientPlayNetworking.send(ItemCarryMod.PICKUP_CHANNEL, buf);
                }
            }
        });
    }

    /**
     * Called from the mixin when the player left-clicks (attacks) an ItemEntity.
     * Sends a toggle request to the server: grab it if nothing is being carried, drop it otherwise.
     */
    public static void onAttackItemEntity(ItemEntity target) {
        PacketByteBuf buf = PacketByteBufs.create();
        buf.writeInt(target.getId());
        ClientPlayNetworking.send(ItemCarryMod.CARRY_TOGGLE_CHANNEL, buf);
    }

    /**
     * Simple entity raycast limited to ItemEntity, since vanilla's crosshair targeting
     * doesn't expose a public way to query "what item am I looking at" directly.
     */
    private static ItemEntity getTargetedItemEntity(MinecraftClient client, double reach) {
        ClientPlayerEntity player = client.player;
        if (player == null || client.world == null) return null;

        Vec3d cameraPos = player.getCameraPosVec(1.0f);
        Vec3d look = player.getRotationVec(1.0f);
        Vec3d reachEnd = cameraPos.add(look.multiply(reach));

        Box searchBox = player.getBoundingBox().stretch(look.multiply(reach)).expand(1.0);

        ItemEntity closest = null;
        double closestDistSq = reach * reach;

        for (ItemEntity item : client.world.getEntitiesByClass(ItemEntity.class, searchBox, e -> true)) {
            Box box = item.getBoundingBox().expand(0.3);
            Optional<Vec3d> hit = box.raycast(cameraPos, reachEnd);
            if (hit.isPresent()) {
                double distSq = cameraPos.squaredDistanceTo(hit.get());
                if (distSq < closestDistSq) {
                    closestDistSq = distSq;
                    closest = item;
                }
            }
        }

        return closest;
    }
  }
