package me.pintoadmin.fabricmoddownloader;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.*;
import net.fabricmc.loader.api.*;
import net.fabricmc.loader.api.metadata.*;
import net.minecraft.server.*;
import org.slf4j.*;

import java.util.*;

public class Fabricmoddownloader implements ModInitializer {
    public static final Logger LOGGER = LoggerFactory.getLogger("fabricmoddownloader");
    private ModManager modManager;
    private ConfigManager configManager;

    @Override
    public void onInitialize() {
        ServerLifecycleEvents.SERVER_STARTED.register(this::onServerReady);
    }

    private void onServerReady(MinecraftServer server) {
        configManager = new ConfigManager();
        modManager = ModManager.getInstance();
        new RequestHandler(modManager).initServer();
        configManager.loadMods(modManager.getAllMods());
    }
}
