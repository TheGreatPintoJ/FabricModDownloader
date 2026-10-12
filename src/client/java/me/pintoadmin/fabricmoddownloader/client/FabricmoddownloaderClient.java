package me.pintoadmin.fabricmoddownloader.client;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.networking.v1.*;
import net.minecraft.world.*;

public class FabricmoddownloaderClient implements ClientModInitializer {

    @Override
    public void onInitializeClient() {
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> {

        });
    }
}
