package com.igor.itemcarry;

import net.minecraft.entity.ItemEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.math.Vec3d;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;

public class ItemCarryMod {
    public static final String MOD_ID = "itemcarry";
    private static final double MAX_INTERACT_DISTANCE_SQ = 8.0 * 8.0;
    private static final Map<UUID, Integer> carrying = new HashMap<>();
    private static MinecraftServer currentServer;

    public static void setCurrentServer(MinecraftServer server) {
        currentServer = server;
    }

    public static void onServerTick() {
        if (currentServer == null) return;
        currentServer.getWorlds().forEach(world -> {
            world.getEntities().forEach(entity -> {
                if (entity instanceof ItemEntity) {
                    ItemEntity item = (ItemEntity) entity;
                    item.setNeverDespawn();
                    item.setPickupDelayInfinite();
                }
            });
        });
        updateCarriedItems(currentServer);
    }

    public static void handlePickup(ServerPlayerEntity player, int entityId) {
        if (currentServer == null) return;
        ItemEntity item = findItem(entityId);
        if (item == null || item.isRemoved() || carrying.containsValue(entityId)) return;
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
        if (currentServer == null) return;
        UUID uuid = player.getUuid();
        if (carrying.containsKey(uuid)) {
            int carriedId = carrying.remove(uuid);
            ItemEntity item = findItem(carriedId);
            if (item != null && !item.isRemoved()) {
                item.setNoGravity(false);
                Vec3d toss = player.getRotationVec(1.0f).multiply(0.35).add(0, 0.15, 0);
                item.setVelocity(toss);
                item.velocityModified = true;
            }
            return;
        }
        ItemEntity item = findItem(entityId);
        if (item == null || item.isRemoved() || carrying.containsValue(entityId)) return;
        if (player.getPos().squaredDistanceTo(item.getPos()) > MAX_INTERACT_DISTANCE_SQ) return;
        item.setNoGravity(true);
        item.setVelocity(Vec3d.ZERO);
        carrying.put(uuid, entityId);
    }

    private static void updateCarriedItems(MinecraftServer server) {
        Iterator<Map.Entry<UUID, Integer>> it = carrying.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<UUID, Integer> entry = it.next();
            ServerPlayerEntity player = server.getPlayerManager().getPlayer(entry.getKey());
            if (player == null) {
                it.remove();
                continue;
            }
            ItemEntity item = findItem(entry.getValue());
            if (item == null || item.isRemoved()) {
                it.remove();
                continue;
            }
            Vec3d target = player.getCameraPosVec(1.0f).add(player.getRotationVec(1.0f).multiply(2.2));
            item.setPosition(target.x, target.y, target.z);
            item.setVelocity(Vec3d.ZERO);
        }
    }

    private static ItemEntity findItem(int entityId) {
        if (currentServer == null) return null;
        for (var world : currentServer.getWorlds()) {
            var entity = world.getEntityById(entityId);
            if (entity instanceof ItemEntity) return (ItemEntity) entity;
        }
        return null;
    }
    }
