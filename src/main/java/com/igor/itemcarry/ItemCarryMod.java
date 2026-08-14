package com.igor.itemcarry;

import net.minecraft.entity.ItemEntity;

public class ItemCarryMod {
    public static void setItemNeverDespawn(ItemEntity item) {
        item.setNeverDespawn();
        item.setPickupDelayInfinite();
    }
}
