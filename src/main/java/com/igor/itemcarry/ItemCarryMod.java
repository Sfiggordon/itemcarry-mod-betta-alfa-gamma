package com.igor.itemcarry;

import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.entity.ItemEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.network.PacketByteBuf;
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
            int carriedId = carrying.remove(uuid);
            ItemEntity carriedItem = findItem(player.getServer(), carriedId);
            if (carriedItem != null && !carriedItem.isRemoved()) {
                boolean fullyPicked = tryInsert(player, carriedItem);
                if (!fullyPicked) {
                    carriedItem.setNoGravity(false);
                    carriedItem.setVelocity(Vec3d.ZERO);
                    carriedItem.velocityModified = true;
                }
            }
            sendCarryState(player, false, -1);
            return;
        }

        if (entityId < 0) return;

        ItemEntity item = findItem(player.getServer(), entityId);
        if (item == null || item.isRemoved()) return;
        if (carrying.containsValue(entityId)) return;
        if (player.getPos().squaredDistanceTo(item.getPos()) > MAX_REACH * MAX_REACH) return;

        tryInsert(player, item);
    }

    public static void handleCarryToggle(ServerPlayerEntity player, int entityId, boolean gentle, double x, double y, double z) {
        UUID uuid = player.getUuid();

        if (carrying.containsKey(uuid)) {
            int oldId = carrying.remove(uuid);
            ItemEntity oldItem = findItem(player.getServer(), oldId);
            if (oldItem != null && !oldItem.isRemoved()) {
                oldItem.setPosition(x, y, z);
                oldItem.setNoGravity(false);
                if (gentle) {
                    oldItem.setVelocity(Vec3d.ZERO);
                } else {
                    Vec3d throwDir = player.getRotationVec(1.0f).multiply(0.35).add(0, 0.15, 0);
                    oldItem.setVelocity(player.getVelocity().add(throwDir));
                }
                oldItem.velocityModified = true;
            }
            sendCarryState(player, false, -1);
            return;
        }

        ItemEntity item = findItem(player.getServer(), entityId);
        if (item == null || item.isRemoved() || carrying.containsValue(entityId)) return;
        if (player.getPos().squaredDistanceTo(item.getPos()) > MAX_REACH * MAX_REACH) return;

        item.setNoGravity(true);
        item.setVelocity(Vec3d.ZERO);
        carrying.put(uuid, entityId);
        sendCarryState(player, true, entityId);
    }

    private static boolean tryInsert(ServerPlayerEntity player, ItemEntity item) {
        ItemStack stack = item.getStack();
        int before = stack.getCount();
        player.getInventory().insertStack(stack);
        int picked = before - stack.getCount();
        if (picked <= 0) return false;
        if (stack.isEmpty()) {
            item.discard();
        } else {
            item.setStack(stack);
        }
        player.sendPickup(item, picked);
        return stack.isEmpty();
    }

    private static void sendCarryState(ServerPlayerEntity player, boolean isCarrying, int entityId) {
        PacketByteBuf buf = PacketByteBufs.create();
        buf.writeBoolean(isCarrying);
        buf.writeInt(entityId);
        ServerPlayNetworking.send(player, ItemCarryInit.CARRY_STATE_CHANNEL, buf);
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
