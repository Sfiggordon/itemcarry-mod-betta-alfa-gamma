package com.igor.itemcarry;

import net.fabricmc.api.ModInitializer;
import net.minecraft.server.MinecraftServer;

public class ItemCarryInit implements ModInitializer {
    @Override
    public void onInitialize() {
        // Mod Zagruhen Yabani v rot
    }

    public static void onServerStart(MinecraftServer server) {
        ItemCarryMod.setCurrentServer(server);
    }

    public static void onServerTick() {
        ItemCarryMod.onServerTick();
    }
}
