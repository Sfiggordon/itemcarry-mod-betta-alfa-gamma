package com.igor.itemcarry;

import net.minecraft.entity.ItemEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.math.Vec3d;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class ItemCarryMod {
    private static final double MAX_REACH = 8.0;
    private static final Map<UUID, Integer> carrying = new HashMap<>();

    public static void onServerTick(MinecraftServer server) {
        // Update carried items
        carrying.forEach((uuid, entityId) -> {
            ServerPlayerEntity player = server.getPlayerManager().getPlayer(uuid);
            if (player == null) return;
            
            ItemEntity item = null;
            for (var world : server.getWorlds()) {
                var entity = world.getEntityById(entityId);
                if (entity instanceof ItemEntity) {
                    item = (ItemEntity) entity;
                    break;
                }
            }
            
            if (item == null || item.isRemoved()) {
                carrying.remove(uuid);
                return;
            }
            
            Vec3d target = player.getCameraPosVec(1.0f).add(player.getRotationVec(1.0f).multiply(2.2));
            item.setPosition(target.x, target.y, target.z);
            item.setVelocity(Vec3d.ZERO);
        });
    }

    public static void handlePickup(ServerPlayerEntity player, int entityId) {
        ItemEntity item = findItem(player.getServer(), entityId);
        if (item == null || item.isRemoved()) return;
        if (carrying.containsValue(entityId)) return;
        if (player.getPos().squaredDistanceTo(item.getPos()) > MAX_REACH * MAX_REACH) return;

        ItemStack stack = item.getStack();
        int before = stack.getCount();
        player.getInventory().insertStack(stack);
        int picked = before - stack.getCount();

        if (picked > 0) {
            if (stack.isEmpty()) item.discard();
            else item.setStack(stack);
            player.sendPickup(item, picked);
        }
    }

    public static void handleCarryToggle(ServerPlayerEntity player, int entityId) {
        UUID uuid = player.getUuid();
        
        if (carrying.containsKey(uuid)) {
            int oldId = carrying.remove(uuid);
            ItemEntity oldItem = findItem(player.getServer(), oldId);
            if (oldItem != null && !oldItem.isRemoved()) {
                oldItem.setNoGravity(false);
                Vec3d toss = player.getRotationVec(1.0f).multiply(0.35).add(0, 0.15, 0);
                oldItem.setVelocity(toss);
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

    private static ItemEntity findItem(MinecraftServer server, int entityId) {
        for (var world : server.getWorlds()) {
            var entity = world.getEntityById(entityId);
            if (entity instanceof ItemEntity) return (ItemEntity) entity;
        }
        return null;
    }
}
