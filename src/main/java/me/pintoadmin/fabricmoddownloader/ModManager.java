package me.pintoadmin.fabricmoddownloader;

import com.fasterxml.jackson.core.type.*;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.json.*;
import net.fabricmc.loader.api.*;
import net.fabricmc.loader.api.metadata.*;

import java.io.*;
import java.net.*;
import java.net.http.*;
import java.nio.file.*;
import java.util.*;
import java.util.stream.*;

import static me.pintoadmin.fabricmoddownloader.ConfigManager.excludedMods;
import static me.pintoadmin.fabricmoddownloader.ConfigManager.modsDir;
import static me.pintoadmin.fabricmoddownloader.Fabricmoddownloader.LOGGER;

public class ModManager {
    private static volatile ModManager instance;

    public record ModData(String name, List<String> authors, String id, String version, ModEnvironment environment){}
    private final List<ModData> clientMods = new ArrayList<>();
    private final List<ModData> serverMods = new ArrayList<>();
    private final Map<String, String> foundMods = new HashMap<>(); // Namespace to ID

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
        if(foundMods.containsKey(namespace)) return foundMods.get(namespace);
        try {
            HttpClient client = HttpClient.newHttpClient();
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("https://api.modrinth.com/v2/project/"+namespace+"/check"))
                    .setHeader("User-Agent", "TheGreatPintoJ/fabricmoddownloader/1.0")
                    .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            String foundID = "";
            if(response.statusCode() != 200){
                if(namespace.contains("_"))
                    foundID = findMod(namespace.replace("_", "-"));
            } else {
                JsonMapper mapper = new JsonMapper();
                JsonNode json = mapper.readTree(response.body());
                foundID = json.get("id").asText();
            }
            if(foundID.isBlank()) LOGGER.warn("Could not find mod by id: "+namespace);
            else {
                LOGGER.warn("Found mod by id: "+namespace);
                foundMods.put(namespace, foundID);
            }
            return foundID;
        } catch (Exception e) {
            e.printStackTrace();
            return "";
        }
    }
    public String findModByName(String name){
        if(foundMods.containsKey(name)) return foundMods.get(name);
        try {
            HttpClient client = HttpClient.newHttpClient();
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("https://api.modrinth.com/v2/search?query="+urlEncode(name)))
                    .setHeader("User-Agent", "TheGreatPintoJ/fabricmoddownloader/1.0")
                    .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            String foundID = "";
            if(response.statusCode() != 200){
                if(name.contains("_"))
                    foundID = findMod(name.replace("_", "-"));
            } else {
                JsonMapper mapper = new JsonMapper();
                JsonNode json = mapper.readTree(response.body());
                int total_hits = json.get("total_hits").asInt();

                if (total_hits == 0 && name.contains("_")) foundID = findMod(name.replace("_", "-"));
                else if(total_hits > 0) foundID = json.get("hits").get(0).get("project_id").asText();
            }
            if(foundID.isBlank()) LOGGER.warn("Could not find mod by name: "+name);
            else {
                LOGGER.warn("Found mod by name: "+name);
                foundMods.put(name, foundID);
            }
            return foundID;
        } catch (Exception e) {
            e.printStackTrace();
            return "";
        }
    }
    public String findMod(ModData modData){
        String id = findMod(modData.id);
        if(!id.isBlank()) return id;
        else id = findModByName(modData.name);
        return id;
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
        try {
            // Get project details
            HttpClient client = HttpClient.newHttpClient();
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("https://api.modrinth.com/v2/project/"+id+"/version"))
                    .setHeader("User-Agent", "TheGreatPintoJ/fabricmoddownloader/1.0")
                    .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            if(response.statusCode() == 200) {
                JsonMapper mapper = new JsonMapper();
                JsonNode json = mapper.readTree(response.body());

                LOGGER.warn("Response: "+response);

                String clientGameVersion = FabricLoader.getInstance()
                        .getModContainer("minecraft")
                        .get()
                        .getMetadata()
                        .getVersion()
                        .getFriendlyString();

                List<JsonNode> validVersions;
                if(json != null && json.isArray()){
                    validVersions = StreamSupport.stream(json.spliterator(), false)
                            .filter(j ->
                                    mapper.convertValue(j.get("game_versions"), new TypeReference<List<String>>(){})
                                    .getFirst().equalsIgnoreCase(clientGameVersion)
                                            && mapper.convertValue(j.get("loaders"), new TypeReference<List<String>>(){})
                                            .getFirst().equalsIgnoreCase("fabric"))
                            .toList();
                    if(validVersions.isEmpty()) {
                        System.err.println("No valid mod version for game version "+clientGameVersion);
                        return false;
                    }
                    JsonNode version = mapper.convertValue(validVersions.getFirst().get("files"), new TypeReference<List<JsonNode>>(){}).getFirst();
                    System.out.println("Valid Version: "+version);
                    String filename = version.get("filename").asText();
                    HttpRequest fileRequest = HttpRequest.newBuilder()
                            .uri(URI.create(version.get("url").asText()))
                            .setHeader("User-Agent", "TheGreatPintoJ/fabricmoddownloader/1.0")
                            .build();

                    HttpResponse<InputStream> fileResponse = client.send(fileRequest, HttpResponse.BodyHandlers.ofInputStream());
                    System.out.println("Response: "+fileResponse);

                    if(fileResponse.statusCode() == 200){
                        try (InputStream inputStream = fileResponse.body()) {
                            Files.copy(inputStream, modsDir.resolve(filename), StandardCopyOption.REPLACE_EXISTING);
                            System.out.println("Download complete! Saved as " + filename);
                            return true;
                        } catch (Exception e){
                            e.printStackTrace();
                        }
                    } else {
                        System.err.println("Failed to download file. HTTP Code: " + fileResponse.statusCode());
                        return false;
                    }
                } else System.err.println("Versions response null or not array");
            }
            return false;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
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

    private String urlEncode(String input){
        return input.replace(" ", "%20")
                .replace("/", "%2F")
                .replace("'", "%27");
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
