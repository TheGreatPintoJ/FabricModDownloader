package me.pintoadmin.fabricmoddownloader;

import net.fabricmc.loader.api.*;
import net.fabricmc.loader.api.metadata.*;
import org.yaml.snakeyaml.*;

import java.io.*;
import java.nio.file.*;
import java.util.*;

import static me.pintoadmin.fabricmoddownloader.Fabricmoddownloader.LOGGER;

public class ConfigManager {
    private Path dataDir = FabricLoader.getInstance().getConfigDir().resolve("fabricmoddownloader");
    private Yaml yaml = new Yaml();
    private Map<String, Object> config = new HashMap<>();
    private Map<String, Object> modsConfig = new HashMap<>();
    public ConfigManager(){
        try {
            Files.createDirectories(dataDir);
            load();
        } catch (IOException e) {
            LOGGER.error("Could not load yaml: "+e.getMessage());
        }
    }

    private void load() throws IOException {
        config = yaml.load(Files.newInputStream(dataDir.resolve("config.yml")));
        modsConfig = yaml.load(Files.newInputStream(dataDir.resolve("mods.yml")));
    }

    public void loadMods(List<ModManager.ModData> allMods){
        Map<String, Object> allModsMap = new HashMap<>();
        for(ModManager.ModData modData : allMods){
            Map<String, Object> thisMod = new HashMap<>();
            thisMod.put("id", modData.id());
            thisMod.put("name", modData.name());
            thisMod.put("authors", modData.authors());
            thisMod.put("version", modData.version());
            thisMod.put("env", modData.environment().name());

            allModsMap.put(modData.id(), thisMod);
        }

        try (FileWriter writer = new FileWriter(dataDir.resolve("mods.yml").toFile())) {
            yaml.dump(allModsMap, writer);
        } catch (IOException e) {
            LOGGER.error("Could not save yaml: "+e.getMessage());
        }
    }
}
