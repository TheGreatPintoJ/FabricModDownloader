package me.pintoadmin.fabricmoddownloader;

import com.fasterxml.jackson.core.type.*;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.json.*;
import com.google.gson.*;
import net.fabricmc.loader.api.*;
import net.fabricmc.loader.api.metadata.*;

import java.net.*;
import java.net.http.*;
import java.util.*;
import java.util.stream.*;

import static me.pintoadmin.fabricmoddownloader.ConfigManager.excludedMods;
import static me.pintoadmin.fabricmoddownloader.Fabricmoddownloader.LOGGER;

public class ModManager {
    private static volatile ModManager instance;

    public record ModData(String name, List<String> authors, String id, String version, ModEnvironment environment){}
    private final List<ModData> clientMods = new ArrayList<>();
    private final List<ModData> serverMods = new ArrayList<>();

    private ModManager(){
        init();
    }

    private void init(){
        loadClientMods();
        loadServerMods();
    }

    private void loadClientMods(){
        for(ModContainer mod : getLoadedMods()){
            ModMetadata meta = mod.getMetadata();
            if(meta.getEnvironment() == ModEnvironment.CLIENT
            || meta.getEnvironment() == ModEnvironment.UNIVERSAL){
                List<String> authors = meta.getAuthors().stream().map(Person::getName).collect(Collectors.toList());
                clientMods.add(
                        new ModData(meta.getName(), authors, meta.getId(),
                                meta.getVersion().getFriendlyString(), meta.getEnvironment()));
            }
        }
    }
    private void loadServerMods(){
        for(ModContainer mod : getLoadedMods()){
            ModMetadata meta = mod.getMetadata();
            if(meta.getEnvironment() == ModEnvironment.SERVER
                    || meta.getEnvironment() == ModEnvironment.UNIVERSAL){
                List<String> authors = meta.getAuthors().stream().map(Person::getName).collect(Collectors.toList());
                serverMods.add(
                        new ModData(meta.getName(), authors, meta.getId(),
                                meta.getVersion().getFriendlyString(), meta.getEnvironment()));
            }
        }
    }

    public String findMod(String namespace){
        try {
            HttpClient client = HttpClient.newHttpClient();
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("https://api.modrinth.com/v2/search?query="+namespace))
                    .setHeader("User-Agent", "TheGreatPintoJ/fabricmoddownloader/1.0")
                    .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            if(response.statusCode() != 200) return findMod(namespace.replace("_", "-"));

            JsonMapper mapper = new JsonMapper();
            JsonNode json = mapper.readTree(response.body());
            int total_hits = json.get("total_hits").asInt();

            if(total_hits == 0) return findMod(namespace.replace("_", "-"));
            else return json.get("hits").get(0).get("project_id").asText();
        } catch (Exception e) {
            e.printStackTrace();
            return "";
        }
    }

    public ModData getModData(String id){
        try {
            HttpClient client = HttpClient.newHttpClient();
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("https://api.modrinth.com/v2/project/"+id))
                    .setHeader("User-Agent", "TheGreatPintoJ/fabricmoddownloader/1.0")
                    .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            if(response.statusCode() == 200) {
                JsonMapper mapper = new JsonMapper();
                JsonNode json = mapper.readTree(response.body());

                String slug = json.get("slug").asText();
                String environment = json.get("environment").asText();
                String name = json.get("title").asText();
                String newestVersion = mapper.convertValue(json.get("game_versions"), new TypeReference<List<String>>(){}).getLast();
                ModEnvironment modEnvironment;
                try {
                    modEnvironment = ModEnvironment.valueOf(environment);
                } catch (IllegalArgumentException e) {
                    modEnvironment = ModEnvironment.UNIVERSAL;
                }

                return new ModData(name, List.of(), slug, newestVersion, modEnvironment);
            } else return null;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public boolean downloadMod(String id){
        return false;
    }

    public List<ModData> getClientModlist(){
        return clientMods;
    }
    public List<ModData> getServerModlist(){
        return serverMods;
    }
    public List<ModData> getAllMods(){
        List<ModData> allMods = new ArrayList<>();
        allMods.addAll(clientMods);
        allMods.addAll(serverMods);
        return allMods;
    }

    private List<ModContainer> getLoadedMods(){
        Collection<ModContainer> modList = FabricLoader.getInstance().getAllMods().stream().filter(c -> c.getContainingMod().isEmpty() && !excludedMods.contains(c.getMetadata().getId())).toList();
        LOGGER.warn("Got loaded mods. Exclusions: "+excludedMods.toString());
        return new ArrayList<>(modList);
    }

    public static ModManager getInstance(){
        if (instance == null) { // First check (no logging/locking overhead)
            synchronized (ModManager.class) {
                if (instance == null) { // Second check (inside synchronization)
                    instance = new ModManager();
                }
            }
        }
        return instance;
    }
}
