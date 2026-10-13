package me.pintoadmin.fabricmoddownloader.client;

import com.fasterxml.jackson.core.*;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.type.*;
import com.mojang.authlib.minecraft.client.*;
import me.pintoadmin.fabricmoddownloader.*;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.networking.v1.*;
import net.minecraft.client.*;
import net.minecraft.client.renderer.feature.*;
import net.minecraft.network.*;
import net.minecraft.network.chat.*;
import net.minecraft.network.chat.Component;

import java.awt.*;
import java.util.*;
import java.util.List;

import static me.pintoadmin.fabricmoddownloader.Fabricmoddownloader.LOGGER;

public class FabricmoddownloaderClient implements ClientModInitializer {
    private final Map<String, List<ModManager.ModData>> missingMods = new HashMap<>();

    @Override
    public void onInitializeClient() {
        ModManager modManager = new ModManager();
        RequestHandler requestHandler = new RequestHandler(modManager);
        requestHandler.initClient();

        ClientConfigurationNetworking.registerGlobalReceiver(RequestHandler.ModListResponse.TYPE, (payload, context) -> {
            String feedbackMessage = payload.response();

            ObjectMapper jsonMapper = new ObjectMapper();
            try {
                List<ModManager.ModData> object = Arrays.asList(jsonMapper.readValue(feedbackMessage, TypeFactory.defaultInstance().constructArrayType(ModManager.ModData.class)));
                List<ModManager.ModData> clientMods = modManager.getClientModlist();

                List<ModManager.ModData> diffMods = new ArrayList<>();
                diffMods.addAll(object);
                diffMods.removeIf(clientMods::contains);


                LOGGER.warn("------ MISSING MODS ------");
                for(ModManager.ModData mod : diffMods){
                    LOGGER.warn("Name: "+mod.name());
                    LOGGER.warn("Authors: "+mod.authors());
                    LOGGER.warn("ID: "+mod.id());
                    LOGGER.warn("Version: "+mod.version());
                    LOGGER.warn("Env: "+mod.environment());
                    LOGGER.warn("--------------------------");
                }
                if(context.client().getCurrentServer() != null)
                    missingMods.put(context.client().getCurrentServer().ip, diffMods);

            } catch (JsonProcessingException e) {
                throw new RuntimeException(e);
            }
        });

        ClientConfigurationConnectionEvents.INIT.register((_, _) -> {
            if (ClientPlayNetworking.canSend(RequestHandler.ModListRequest.TYPE)) {
                ClientPlayNetworking.send(new RequestHandler.ModListRequest("gimme"));
            }
        });
    }

    public void openDownloadGUI(String kickReason){
        Minecraft.getInstance().gui.setScreen(
                new DownloadModsScreen(Component.literal(kickReason))
        );
    }
}
