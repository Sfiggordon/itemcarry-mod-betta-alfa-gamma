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
import net.minecraft.entity.Entity;
import net.minecraft.entity.ItemEntity;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import org.lwjgl.glfw.GLFW;
import java.util.Optional;

public class ItemCarryClient implements ClientModInitializer {
    private static KeyBinding pickupKey;
    private static boolean carrying = false;
    private static int carriedEntityId = -1;
    private static double carryDistance = 2.2;
    private static final double MIN_DISTANCE = 1.0;
    private static final double MAX_DISTANCE = 5.0;

    @Override
    public void onInitializeClient() {
        pickupKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
            "key.itemcarry.pickup",
            InputUtil.Type.KEYSYM,
            GLFW.GLFW_KEY_R,
            "key.categories.itemcarry"
        ));

        // Сервер — единственный источник правды о том, держим мы что-то или нет
        ClientPlayNetworking.registerGlobalReceiver(ItemCarryInit.CARRY_STATE_CHANNEL, (client, handler, buf, responseSender) -> {
            boolean nowCarrying = buf.readBoolean();
            int entityId = buf.readInt();
            client.execute(() -> {
                carrying = nowCarrying;
                carriedEntityId = nowCarrying ? entityId : -1;
            });
        });

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (client.player == null || client.world == null) return;

            if (carrying && carriedEntityId != -1) {
                Entity carried = client.world.getEntityById(carriedEntityId);
                if (carried instanceof ItemEntity) {
                    Vec3d target = client.player.getCameraPosVec(1.0f)
                        .add(client.player.getRotationVec(1.0f).multiply(carryDistance));
                    carried.setPosition(target.x, target.y, target.z);
                    carried.setVelocity(Vec3d.ZERO);
                }
            }

            while (pickupKey.wasPressed()) {
                if (carrying) {
                    sendPickup(carriedEntityId);
                } else {
                    ItemEntity target = getTargetedItem(client);
                    sendPickup(target != null ? target.getId() : -1);
                }
            }
        });
    }

    public static boolean isCarrying() {
        return carrying;
    }

    public static void adjustCarryDistance(double scrollAmount) {
        carryDistance += scrollAmount * 0.3;
        if (carryDistance < MIN_DISTANCE) carryDistance = MIN_DISTANCE;
        if (carryDistance > MAX_DISTANCE) carryDistance = MAX_DISTANCE;
    }

    public static void onAttackItem(ItemEntity item) {
        // Просто отправляем запрос — сервер сам решит грабнуть или бросить,
        // и подтвердит через CARRY_STATE_CHANNEL. Локально ничего не меняем.
        PacketByteBuf buf = PacketByteBufs.create();
        buf.writeInt(item.getId());
        buf.writeBoolean(false);
        ClientPlayNetworking.send(ItemCarryInit.CARRY_TOGGLE_CHANNEL, buf);
    }

    public static void onRightClickPlace() {
        if (!carrying) return;
        PacketByteBuf buf = PacketByteBufs.create();
        buf.writeInt(carriedEntityId);
        buf.writeBoolean(true);
        ClientPlayNetworking.send(ItemCarryInit.CARRY_TOGGLE_CHANNEL, buf);
    }

    private static void sendPickup(int entityId) {
        PacketByteBuf buf = PacketByteBufs.create();
        buf.writeInt(entityId);
        ClientPlayNetworking.send(ItemCarryInit.PICKUP_CHANNEL, buf);
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
