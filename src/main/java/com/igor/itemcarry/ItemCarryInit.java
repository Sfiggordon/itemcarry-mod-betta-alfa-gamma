package com.igor.itemcarry;

import net.fabricmc.api.ModInitializer;
import net.minecraft.entity.ItemEntity;

public class ItemCarryInit implements ModInitializer {
    @Override
    public void onInitialize() {
        // Используем event hook из Minecraft для обработки спаун айтемов
        net.fabricmc.fabric.api.event.lifecycle.v1.ServerEntityEvents.ENTITY_LOAD.register((entity, world) -> {
            if (entity instanceof ItemEntity item) {
                item.setNeverDespawn();
                item.setPickupDelayInfinite();
            }
        });
    }
}
