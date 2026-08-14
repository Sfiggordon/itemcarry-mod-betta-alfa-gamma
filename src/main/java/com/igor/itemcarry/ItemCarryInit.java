package com.igor.itemcarry;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.entity.ItemEntity;
import net.minecraft.util.Identifier;

public class ItemCarryInit implements ModInitializer {
    public static final Identifier PICKUP_CHANNEL = new Identifier("itemcarry", "pickup");
    public static final Identifier CARRY_TOGGLE_CHANNEL = new Identifier("itemcarry", "carry_toggle");

    @Override
    public void onInitialize() {
        ServerEntityEvents.ENTITY_LOAD.register((entity, world) -> {
            if (entity instanceof ItemEntity item) {
                item.setNeverDespawn();
                item.setPickupDelayInfinite();
            }
        });

        ServerTickEvents.END_SERVER_TICK.register(server -> ItemCarryMod.onServerTick(server));

        ServerPlayNetworking.registerGlobalReceiver(PICKUP_CHANNEL, (server, player, handler, buf, responseSender) -> {
            int entityId = buf.readInt();
            server.execute(() -> ItemCarryMod.handlePickup(player, entityId));
        });

        ServerPlayNetworking.registerGlobalReceiver(CARRY_TOGGLE_CHANNEL, (server, player, handler, buf, responseSender) -> {
            int entityId = buf.readInt();
            server.execute(() -> ItemCarryMod.handleCarryToggle(player, entityId));
        });
    }
}
