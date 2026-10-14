package me.pintoadmin.fabricmoddownloader.client;

import com.google.gson.*;
import com.mojang.authlib.minecraft.client.*;
import me.pintoadmin.fabricmoddownloader.*;
import net.fabricmc.fabric.api.client.networking.v1.*;
import net.minecraft.client.*;
import net.minecraft.client.gui.*;
import net.minecraft.client.gui.components.*;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.*;
import net.minecraft.network.chat.*;
import net.minecraft.network.chat.Component;

import java.awt.*;
import java.net.*;
import java.net.http.*;
import java.util.*;
import java.util.List;

import static me.pintoadmin.fabricmoddownloader.Fabricmoddownloader.LOGGER;

public class DownloadModsScreen extends Screen {
    private final ModManager modManager;
    private final Screen disconnectScreen;
    private boolean hasDownloadedAny = false;
    private boolean downloadError = false;

    private final List<Button> downloadButtons = new ArrayList<>();
    private final Map<String, Integer> downloadEntries = new HashMap<>();

    public DownloadModsScreen(Component title, Screen disconnectScreen) {
        super(title);
        this.disconnectScreen = disconnectScreen;
        modManager = ModManager.getInstance();
    }

    @Override
    protected void init() {
        downloadButtons.forEach(this::addRenderableWidget);
        Button searchButton = Button.builder(Component.literal("Search for missing mod"), btn -> {
            downloadButtons.forEach(this::removeWidget);
            downloadButtons.clear();
            downloadEntries.clear();
            List<String> missingModsNamespaces = FabricmoddownloaderClient.getInstance().getMissingModsNamespaces();
            LOGGER.warn("Missing mods: "+missingModsNamespaces);
            int startingY = 40;
            int spacing = 25;
            for (String namespace : missingModsNamespaces){
                String modID = modManager.findMod(namespace);
                if(modID.isBlank()){
                    LOGGER.warn("Could not find mod on modrinth: "+namespace);
                    newFailedEntry(modID, startingY+=spacing);
                    continue;
                }
                ModManager.ModData mod = modManager.getModData(modID);
                newDownloadEntry(mod, startingY+=spacing);
            }
        }).bounds(this.width/2-75, 40, 150, 20).build();
        this.addRenderableWidget(searchButton);

        Button rawErrorButton = Button.builder(Component.literal("View disconnect error"), btn -> {
            this.minecraft.gui.setScreen(disconnectScreen);
        }).bounds(this.width-185, this.height-30, 175, 20).build();
        this.addRenderableWidget(rawErrorButton);

        Button backButton = Button.builder(Component.literal("Back"), btn -> {
            onClose();
        }).bounds(10, this.height-30, 40, 20).build();
        this.addRenderableWidget(backButton);
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
        super.extractRenderState(graphics, mouseX, mouseY, delta);

        String missingModsText = "Download Missing Mods";
        graphics.text(this.font, missingModsText, (this.width/2)-(this.font.width(missingModsText)/2), 40 - this.font.lineHeight - 10, 0xFFFFFFFF, true);

        for(Map.Entry<String, Integer> entry : downloadEntries.entrySet()){
            graphics.text(this.font, entry.getKey()+" (Modrinth)", this.width/8, entry.getValue()+5, Color.GREEN.getRGB(), true);
        }
        if(downloadError) graphics.text(this.font, "Error while downloading mods. Check the client logs for more details.",
                this.width*3/4, 40-this.font.lineHeight-10, Color.RED.getRGB(), true);
        else if(hasDownloadedAny) graphics.text(this.font, "You will need to restart for mods to take effect.",
                20, this.height-40-this.font.lineHeight-10, Color.GREEN.getRGB(), true);
    }

    private void newDownloadEntry(ModManager.ModData modData, int yOffset){
        Button downloadButton = Button.builder(Component.literal("Download"), btn -> {
            if(modManager.downloadMod(modManager.findMod(modData.id()))){
                hasDownloadedAny = true;
            } else {
                downloadError = true;
            }
        }).bounds(this.width*3/4, yOffset, 75, 20).build();
        this.addRenderableWidget(downloadButton);
        downloadEntries.put(modData.name(), yOffset);
        downloadButtons.add(downloadButton);
    }
    private void newFailedEntry(String modID, int yOffset){
        Button downloadButton = Button.builder(Component.literal("Error").withColor(TextColor.RED), btn -> {
        }).bounds(this.width*3/4, yOffset, 75, 20).tooltip(
                Tooltip.create(Component.literal("Couldn't find this mod on modrinth. Try downloading it manually.")
                        .withColor(TextColor.RED)))
                .build();
        this.addRenderableWidget(downloadButton);
        downloadEntries.put(modID, yOffset);
        downloadButtons.add(downloadButton);
    }
}