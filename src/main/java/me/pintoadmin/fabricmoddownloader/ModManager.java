package me.pintoadmin.fabricmoddownloader;

import net.fabricmc.loader.api.*;
import net.fabricmc.loader.api.metadata.*;

import java.util.*;
import java.util.stream.*;

public class ModManager {
    public record ModData(String name, List<String> authors, String id, String version, ModEnvironment environment){}
    private final List<ModData> clientMods = new ArrayList<>();
    private final List<ModData> serverMods = new ArrayList<>();

    public ModManager(){
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
        Collection<ModContainer> modList = FabricLoader.getInstance().getAllMods();
        return new ArrayList<>(modList);
    }
}
