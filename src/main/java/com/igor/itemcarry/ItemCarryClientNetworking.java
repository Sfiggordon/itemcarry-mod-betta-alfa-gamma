package com.igor.itemcarry;

import net.fabricmc.fabric.api.networking.v1.ClientPlayNetworking;
import net.minecraft.util.Identifier;
import net.minecraft.network.PacketByteBuf;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;

public class ItemCarryClientNetworking {
    public static final Identifier PICKUP_CHANNEL = new Identifier("itemcarry", "pickup");
    public static final Identifier CARRY_TOGGLE_CHANNEL = new Identifier("itemcarry", "carry_toggle");

    public static void sendPickupRequest(int entityId) {
        PacketByteBuf buf = PacketByteBufs.create();
        buf.writeInt(entityId);
        ClientPlayNetworking.send(PICKUP_CHANNEL, buf);
    }

    public static void sendCarryToggle(int entityId) {
        PacketByteBuf buf = PacketByteBufs.create();
        buf.writeInt(entityId);
        ClientPlayNetworking.send(CARRY_TOGGLE_CHANNEL, buf);
    }
}
