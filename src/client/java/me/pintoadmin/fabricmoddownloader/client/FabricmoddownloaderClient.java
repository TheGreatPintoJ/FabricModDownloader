package me.pintoadmin.fabricmoddownloader.client;

import com.fasterxml.jackson.core.*;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.type.*;
import com.mojang.authlib.minecraft.client.*;
import com.mojang.brigadier.context.*;
import me.pintoadmin.fabricmoddownloader.*;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.*;
import net.fabricmc.fabric.api.client.networking.v1.*;
import net.fabricmc.fabric.api.resource.v1.reloader.*;
import net.minecraft.client.*;
import net.minecraft.client.gui.*;
import net.minecraft.client.renderer.feature.*;
import net.minecraft.commands.*;
import net.minecraft.network.*;
import net.minecraft.network.chat.*;
import net.minecraft.network.chat.Component;

import java.awt.*;
import java.util.*;
import java.util.List;
import java.util.stream.*;

import static me.pintoadmin.fabricmoddownloader.Fabricmoddownloader.LOGGER;

public class FabricmoddownloaderClient implements ClientModInitializer {
    private static volatile FabricmoddownloaderClient instance;
    private final List<ModManager.ModData> missingMods = new ArrayList<>();
    private final List<String> missingModsNamespaces = new ArrayList<>();
    private ModManager modManager;
    private ConfigManager configManager;

    @Override
    public void onInitializeClient() {
        configManager = ConfigManager.getInstance();
        modManager = ModManager.getInstance();
        RequestHandler requestHandler = new RequestHandler(modManager);
        requestHandler.initClient();

        ClientPlayNetworking.registerGlobalReceiver(RequestHandler.ModListResponse.TYPE, (payload, context) -> {
            String feedbackMessage = payload.response();

            ObjectMapper jsonMapper = new ObjectMapper();
            try {
                List<ModManager.ModData> object = Arrays.asList(jsonMapper.readValue(feedbackMessage, TypeFactory.defaultInstance().constructArrayType(ModManager.ModData.class)));
                List<ModManager.ModData> clientMods = modManager.getClientModlist();

                List<ModManager.ModData> diffMods = new ArrayList<>();
                diffMods.addAll(object);
                diffMods.removeIf(clientMods::contains);

                if(diffMods.isEmpty()) return;

                LOGGER.warn("------ MISSING MODS ------");
                for(ModManager.ModData mod : diffMods){
                    LOGGER.warn("Name: "+mod.name());
                    LOGGER.warn("Authors: "+mod.authors());
                    LOGGER.warn("ID: "+mod.id());
                    LOGGER.warn("Version: "+mod.version());
                    LOGGER.warn("Env: "+mod.environment());
                    LOGGER.warn("--------------------------");
                }
                missingMods.clear();
                missingMods.addAll(diffMods);
                missingModsNamespaces.addAll(diffMods.stream().map(ModManager.ModData::id).toList());

                context.client().player.sendSystemMessage(Component.literal("This server has mods that can be installed clientside.").withColor(TextColor.GOLD));
                context.client().gui.hud.getChat().addClientSystemMessage(Component.literal("Click Here").setStyle(
                        Style.EMPTY.withClickEvent(new ClickEvent.RunCommand("/downloadmods"))
                                .withColor(TextColor.GREEN)
                                .withUnderlined(true)
                                .withBold(true))
                        .append(Component.literal(" to download them")
                                .withColor(TextColor.GOLD)));
            } catch (JsonProcessingException e) {
                throw new RuntimeException(e);
            }
        });

        ClientPlayConnectionEvents.JOIN.register((_, _, _) -> {
            if (ClientPlayNetworking.canSend(RequestHandler.ModListRequest.TYPE)) {
                RequestHandler.ModListRequest request = new RequestHandler.ModListRequest("");
                ClientPlayNetworking.send(request);
            }
        });

        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> {
            dispatcher.register(ClientCommands.literal("downloadmods").executes(this::downloadModsCommand));
        });
    }

    private int downloadModsCommand(CommandContext<FabricClientCommandSource> context){
        Minecraft client = context.getSource().getClient();
        client.execute(this::openOptionalGUI);
        return 1;
    }

    public void openDownloadGUI(){
        Minecraft.getInstance().gui.setScreen(
                new DownloadModsScreen(Component.literal(""), Minecraft.getInstance().gui.screen())
        );
    }
    public void openOptionalGUI(){
        Minecraft.getInstance().gui.setScreen(
                new OptionalModsScreen(missingMods)
        );
    }

    public void parseReason(String reason){
        List<String> reasons = new ArrayList<>(reason.lines().toList());
        reasons.removeIf(s -> s.endsWith("that are unknown to this client.")
        || s.startsWith("this is usually caused")
        || s.startsWith("the following registry")
        || s.isBlank());

        missingModsNamespaces.clear();
        missingModsNamespaces.addAll(reasons);
    }

    public static FabricmoddownloaderClient getInstance(){
        if (instance == null) { // First check (no logging/locking overhead)
            synchronized (FabricmoddownloaderClient.class) {
                if (instance == null) { // Second check (inside synchronization)
                    instance = new FabricmoddownloaderClient();
                }
            }
        }
        return instance;
    }

    public List<ModManager.ModData> getMissingMods(){
        return missingMods;
    }
    public List<String> getMissingModsNamespaces() {
        return missingModsNamespaces;
    }
}
