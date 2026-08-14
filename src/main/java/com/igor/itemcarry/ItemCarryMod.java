package com.igor.itemcarry;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.entity.ItemEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Vec3d;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;

/**
 * Item Carry Mod
 * <p>
 * 1. Dropped items never despawn (ItemEntity#setNeverDespawn).
 * 2. Dropped items can't be picked up by walking over them (ItemEntity#setPickupDelayInfinite),
 *    they can only be picked up via a dedicated key while looking at them.
 * 3. Left-click (attack) on a dropped item grabs it and carries it in front of the camera,
 *    like Teardown. Clicking again drops it.
 */
public class ItemCarryMod implements ModInitializer {

    public static final String MOD_ID = "itemcarry";

    // How close the carried/targeted item must remain to the player for the server to accept it (anti-cheat safety margin).
    private static final double MAX_INTERACT_DISTANCE_SQ = 8.0 * 8.0;

    // Networking channels
    public static final Identifier PICKUP_CHANNEL = new Identifier(MOD_ID, "pickup");
    public static final Identifier CARRY_TOGGLE_CHANNEL = new Identifier(MOD_ID, "carry_toggle");
    public static final Identifier CARRY_STATE_CHANNEL = new Identifier(MOD_ID, "carry_state");

    // player UUID -> id of the ItemEntity they are currently carrying
    private static final Map<UUID, Integer> carrying = new HashMap<>();

    @Override
    public void onInitialize() {

        // --- Feature 1 & 2: whenever an item entity spawns/loads on the server, make it permanent and un-pickup-able ---
        ServerEntityEvents.ENTITY_LOAD.register((entity, world) -> {
            if (entity instanceof ItemEntity item) {
                item.setNeverDespawn();
                item.setPickupDelayInfinite();
            }
        });

        // --- Feature 2: handle the custom "pick up looked-at item" key ---
        ServerPlayNetworking.registerGlobalReceiver(PICKUP_CHANNEL, (server, player, handler, buf, responseSender) -> {
            int entityId = buf.readInt();
            server.execute(() -> handlePickup(player, entityId));
        });

        // --- Feature 3: handle grab/drop toggle from left-click ---
        ServerPlayNetworking.registerGlobalReceiver(CARRY_TOGGLE_CHANNEL, (server, player, handler, buf, responseSender) -> {
            int entityId = buf.readInt();
            server.execute(() -> handleCarryToggle(player, entityId));
        });

        // --- Feature 3: every tick, move carried items in front of the carrying player's camera ---
        ServerTickEvents.END_SERVER_TICK.register(this::updateCarriedItems);

        // Clean up if a player disconnects while carrying something
        net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> {
            carrying.remove(handler.getPlayer().getUuid());
        });
    }

    private void handlePickup(ServerPlayerEntity player, int entityId) {
        if (!(player.getWorld().getEntityById(entityId) instanceof ItemEntity item)) return;
        if (item.isRemoved()) return;
        if (carrying.containsValue(entityId)) return; // being carried right now, can't grab it this way
        if (player.getPos().squaredDistanceTo(item.getPos()) > MAX_INTERACT_DISTANCE_SQ) return;

        ItemStack stack = item.getStack();
        int before = stack.getCount();
        player.getInventory().insertStack(stack);
        int pickedUp = before - stack.getCount();

        if (pickedUp <= 0) return; // inventory full, nothing happened

        if (stack.isEmpty()) {
            item.discard();
        } else {
            item.setStack(stack);
        }
        player.sendPickup(item, pickedUp);
        player.getInventory().markDirty();
    }

    private void handleCarryToggle(ServerPlayerEntity player, int entityId) {
        UUID uuid = player.getUuid();

        // Already carrying something -> this click means "drop it"
        if (carrying.containsKey(uuid)) {
            int carriedId = carrying.remove(uuid);
            if (player.getWorld().getEntityById(carriedId) instanceof ItemEntity item) {
                item.setNoGravity(false);
                Vec3d toss = player.getRotationVec(1.0f).multiply(0.35).add(0, 0.15, 0);
                item.setVelocity(toss);
                item.velocityModified = true;
            }
            sendCarryState(player, false, -1);
            return;
        }

        // Not carrying anything -> try to grab entityId
        if (!(player.getWorld().getEntityById(entityId) instanceof ItemEntity item)) return;
        if (item.isRemoved()) return;
        if (carrying.containsValue(entityId)) return; // someone else is already carrying it
        if (player.getPos().squaredDistanceTo(item.getPos()) > MAX_INTERACT_DISTANCE_SQ) return;

        item.setNoGravity(true);
        item.setVelocity(Vec3d.ZERO);
        carrying.put(uuid, entityId);
        sendCarryState(player, true, entityId);
    }

    private void updateCarriedItems(MinecraftServer server) {
        Iterator<Map.Entry<UUID, Integer>> it = carrying.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<UUID, Integer> entry = it.next();
            ServerPlayerEntity player = server.getPlayerManager().getPlayer(entry.getKey());
            if (player == null) {
                it.remove();
                continue;
            }

            if (!(player.getWorld().getEntityById(entry.getValue()) instanceof ItemEntity item) || item.isRemoved()) {
                it.remove();
                sendCarryState(player, false, -1);
                continue;
            }

            Vec3d target = player.getCameraPosVec(1.0f).add(player.getRotationVec(1.0f).multiply(2.2));
            item.setPosition(target.x, target.y, target.z);
            item.setVelocity(Vec3d.ZERO);
        }
    }

    private void sendCarryState(ServerPlayerEntity player, boolean isCarrying, int entityId) {
        PacketByteBuf buf = PacketByteBufs.create();
        buf.writeBoolean(isCarrying);
        buf.writeInt(entityId);
        ServerPlayNetworking.send(player, CARRY_STATE_CHANNEL, buf);
    }
    }
                  
