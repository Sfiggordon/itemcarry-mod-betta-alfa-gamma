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
    private static final double MAX_REACH = 8.0;
    private static final Map<UUID, Integer> carrying = new HashMap<>();

    public static void onServerTick(MinecraftServer server) {
        Iterator<Map.Entry<UUID, Integer>> it = carrying.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<UUID, Integer> entry = it.next();
            ServerPlayerEntity player = server.getPlayerManager().getPlayer(entry.getKey());
            if (player == null) { it.remove(); continue; }
            ItemEntity item = findItem(server, entry.getValue());
            if (item == null || item.isRemoved()) { it.remove(); continue; }
            item.setVelocity(Vec3d.ZERO);
        }
    }

    public static void handlePickup(ServerPlayerEntity player, int entityId) {
        UUID uuid = player.getUuid();

        if (carrying.containsKey(uuid)) {
            int carriedId = carrying.get(uuid);
            ItemEntity carriedItem = findItem(player.getServer(), carriedId);
            if (carriedItem != null && !carriedItem.isRemoved()) {
                tryInsert(player, carriedItem);
            }
            carrying.remove(uuid);
            return;
        }

        if (entityId < 0) return;

        ItemEntity item = findItem(player.getServer(), entityId);
        if (item == null || item.isRemoved()) return;
        if (carrying.containsValue(entityId)) return;
        if (player.getPos().squaredDistanceTo(item.getPos()) > MAX_REACH * MAX_REACH) return;

        tryInsert(player, item);
    }

    public static void handleCarryToggle(ServerPlayerEntity player, int entityId, boolean gentle) {
        UUID uuid = player.getUuid();

        if (carrying.containsKey(uuid)) {
            int oldId = carrying.remove(uuid);
            ItemEntity oldItem = findItem(player.getServer(), oldId);
            if (oldItem != null && !oldItem.isRemoved()) {
                oldItem.setNoGravity(false);
                if (gentle) {
                    oldItem.setVelocity(Vec3d.ZERO);
                } else {
                    Vec3d throwDir = player.getRotationVec(1.0f).multiply(0.35).add(0, 0.15, 0);
                    oldItem.setVelocity(player.getVelocity().add(throwDir));
                }
                oldItem.velocityModified = true;
            }
            return;
        }

        ItemEntity item = findItem(player.getServer(), entityId);
        if (item == null || item.isRemoved() || carrying.containsValue(entityId)) return;
        if (player.getPos().squaredDistanceTo(item.getPos()) > MAX_REACH * MAX_REACH) return;

        item.setNoGravity(true);
        item.setVelocity(Vec3d.ZERO);
        carrying.put(uuid, entityId);
    }

    private static void tryInsert(ServerPlayerEntity player, ItemEntity item) {
        ItemStack stack = item.getStack();
        int before = stack.getCount();
        player.getInventory().insertStack(stack);
        int picked = before - stack.getCount();
        if (picked <= 0) return;
        if (stack.isEmpty()) {
            item.discard();
        } else {
            item.setStack(stack);
            item.setNoGravity(false);
        }
        player.sendPickup(item, picked);
    }

    private static ItemEntity findItem(MinecraftServer server, int entityId) {
        if (server == null) return null;
        for (var world : server.getWorlds()) {
            var entity = world.getEntityById(entityId);
            if (entity instanceof ItemEntity) return (ItemEntity) entity;
        }
        return null;
    }
}
