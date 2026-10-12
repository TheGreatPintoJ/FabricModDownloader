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
        modManager = new ModManager();
        configManager = new ConfigManager();
        configManager.loadMods(modManager.getAllMods());

        Collection<ModContainer> modList = FabricLoader.getInstance().getAllMods();
        int index = 0;
        for(ModContainer mod : modList){
            ModMetadata meta = mod.getMetadata();
            LOGGER.warn("Name: "+meta.getName());
            LOGGER.warn("Authors: "+meta.getAuthors());
            LOGGER.warn("ID: "+meta.getId());
            LOGGER.warn("Version: "+meta.getVersion());
            LOGGER.warn("Env: "+meta.getEnvironment());
            LOGGER.warn("-------------------------");
            index++;
            if(index > 10) break;
        }
    }
}
