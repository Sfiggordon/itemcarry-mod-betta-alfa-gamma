package com.igor.itemcarry;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.entity.ItemEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.math.Vec3d;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;

public class ItemCarryMod implements ModInitializer {

    public static final String MOD_ID = "itemcarry";
    private static final double MAX_INTERACT_DISTANCE_SQ = 8.0 * 8.0;
    private static final Map<UUID, Integer> carrying = new HashMap<>();

    @Override
    public void onInitialize() {
        ServerEntityEvents.ENTITY_LOAD.register((entity, world) -> {
            if (entity instanceof ItemEntity item) {
                item.setNeverDespawn();
                item.setPickupDelayInfinite();
            }
        });

        ServerTickEvents.END_SERVER_TICK.register(this::updateCarriedItems);
    }

    public static void handlePickup(ServerPlayerEntity player, int entityId) {
        if (!(player.getWorld().getEntityById(entityId) instanceof ItemEntity item)) return;
        if (item.isRemoved()) return;
        if (carrying.containsValue(entityId)) return;
        if (player.getPos().squaredDistanceTo(item.getPos()) > MAX_INTERACT_DISTANCE_SQ) return;

        ItemStack stack = item.getStack();
        int before = stack.getCount();
        player.getInventory().insertStack(stack);
        int pickedUp = before - stack.getCount();

        if (pickedUp <= 0) return;

        if (stack.isEmpty()) {
            item.discard();
        } else {
            item.setStack(stack);
        }
        player.sendPickup(item, pickedUp);
        player.getInventory().markDirty();
    }

    public static void handleCarryToggle(ServerPlayerEntity player, int entityId) {
        UUID uuid = player.getUuid();

        if (carrying.containsKey(uuid)) {
            int carriedId = carrying.remove(uuid);
            if (player.getWorld().getEntityById(carriedId) instanceof ItemEntity item) {
                item.setNoGravity(false);
                Vec3d toss = player.getRotationVec(1.0f).multiply(0.35).add(0, 0.15, 0);
                item.setVelocity(toss);
                item.velocityModified = true;
            }
            return;
        }

        if (!(player.getWorld().getEntityById(entityId) instanceof ItemEntity item)) return;
        if (item.isRemoved()) return;
        if (carrying.containsValue(entityId)) return;
        if (player.getPos().squaredDistanceTo(item.getPos()) > MAX_INTERACT_DISTANCE_SQ) return;

        item.setNoGravity(true);
        item.setVelocity(Vec3d.ZERO);
        carrying.put(uuid, entityId);
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
                continue;
            }

            Vec3d target = player.getCameraPosVec(1.0f).add(player.getRotationVec(1.0f).multiply(2.2));
            item.setPosition(target.x, target.y, target.z);
            item.setVelocity(Vec3d.ZERO);
        }
    }

    public static void sendPickupRequest(int entityId) {
    }

    public static void sendCarryToggleRequest(int entityId) {
    }
    }
