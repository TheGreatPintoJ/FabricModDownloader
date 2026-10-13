package me.pintoadmin.fabricmoddownloader;

import net.fabricmc.api.*;
import net.fabricmc.loader.api.*;
import org.yaml.snakeyaml.*;

import java.io.*;
import java.nio.file.*;
import java.util.*;

import static me.pintoadmin.fabricmoddownloader.Fabricmoddownloader.LOGGER;

public class ConfigManager {
    private static volatile ConfigManager instance;
    public static Path dataDir = FabricLoader.getInstance().getConfigDir().resolve("fabricmoddownloader");
    public static Path modsDir = FabricLoader.getInstance().getGameDir().resolve("mods");
    private final Yaml yaml = new Yaml();
    private Map<String, Object> config = new HashMap<>();
    private Map<String, Object> modsConfig = new HashMap<>();
    public static List<String> excludedMods = new ArrayList<>();
    public ConfigManager(){
        try {
            Files.createDirectories(dataDir);
            if(FabricLoader.getInstance().getEnvironmentType() == EnvType.SERVER){
                saveDefaultResourceToFile(dataDir.resolve("config.yml"), "server-config.yml");
                loadServer();
                loadExclusions();
            } else if(FabricLoader.getInstance().getEnvironmentType() == EnvType.CLIENT){
                saveDefaultResourceToFile(dataDir.resolve("config.yml"), "client-config.yml");
                loadClient();
                modsDir = FabricLoader.getInstance().getGameDir()
                        .resolve((String) config.getOrDefault("mods-directory", "mods"));
                LOGGER.warn("Loaded mods dir: "+modsDir.toAbsolutePath());
            }
        } catch (IOException e) {
            LOGGER.error("Could not load yaml: "+e.getMessage());
        }
    }

    private void loadClient() throws IOException {
        config = yaml.load(Files.newInputStream(dataDir.resolve("config.yml")));
        LOGGER.warn("Loaded yaml: "+config);
    }
    private void loadServer() throws IOException {
        config = yaml.load(Files.newInputStream(dataDir.resolve("config.yml")));
        LOGGER.warn("Loaded yaml: "+config);
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

    public void loadExclusions(){
        try {
            excludedMods.clear();
            List<String> excluded = (List<String>) config.get("excluded-mods");
            excludedMods.addAll(excluded);
            LOGGER.warn("Loaded exclusions: "+excludedMods.toString());
        } catch (ClassCastException e){
            LOGGER.error("Could not load excluded mods: "+e.getMessage());
            e.printStackTrace();
        }
    }

    private void saveDefaultResourceToFile(Path file, String path){
        if (Files.notExists(file)) {
            try (InputStream in = Fabricmoddownloader.class.getClassLoader().getResourceAsStream(path)) {
                if (in != null) {
                    Files.copy(in, file);
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public static ConfigManager getInstance(){
        if (instance == null) { // First check (no logging/locking overhead)
            synchronized (ConfigManager.class) {
                if (instance == null) { // Second check (inside synchronization)
                    instance = new ConfigManager();
                }
            }
        }
        return instance;
    }
}
