package com.igor.itemcarry;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.entity.ItemEntity;

public class ItemCarryInit implements ModInitializer {
    @Override
    public void onInitialize() {
        // Feature 1 & 2: Items never despawn
        ServerEntityEvents.ENTITY_LOAD.register((entity, world) -> {
            if (entity instanceof ItemEntity item) {
                item.setNeverDespawn();
                item.setPickupDelayInfinite();
            }
        });

        // Feature 3: Update carried items
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            ItemCarryMod.onServerTick(server);
        });
    }
}
