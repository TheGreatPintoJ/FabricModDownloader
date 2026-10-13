package me.pintoadmin.fabricmoddownloader.client;

import com.google.gson.*;
import com.mojang.authlib.minecraft.client.*;
import me.pintoadmin.fabricmoddownloader.*;
import net.fabricmc.fabric.api.client.networking.v1.*;
import net.minecraft.client.*;
import net.minecraft.client.gui.*;
import net.minecraft.client.gui.components.*;
import net.minecraft.client.gui.screens.*;
import net.minecraft.network.chat.*;

import java.net.*;
import java.net.http.*;
import java.util.*;

import static me.pintoadmin.fabricmoddownloader.Fabricmoddownloader.LOGGER;

public class DownloadModsScreen extends Screen {
    private final Screen disconnectScreen;
    private GuiGraphicsExtractor graphics;

    public DownloadModsScreen(Component title, Screen disconnectScreen) {
        super(title);
        this.disconnectScreen = disconnectScreen;
    }

    @Override
    protected void init() {
        Button searchButton = Button.builder(Component.literal("Search for missing mod"), btn -> {
            List<String> missingModsNamespaces = FabricmoddownloaderClient.getInstance().getMissingModsNamespaces();
            LOGGER.warn("Missing mods: "+missingModsNamespaces);
            int startingY = 80;
            for (String namespace : missingModsNamespaces){
                String modID = ModManager.getInstance().findMod(namespace);
                if(modID.isBlank()){
                    LOGGER.warn("Could not find mod on modrinth: "+namespace);
                    continue;
                }
                ModManager.ModData mod = ModManager.getInstance().getModData(modID);
                newDownloadEntry(mod, startingY+=40);
            }
        }).bounds(this.width/2, 40, 150, 20).build();
        this.addRenderableWidget(searchButton);


        Button rawErrorButton = Button.builder(Component.literal("View raw disconnect error"), btn -> {
            this.minecraft.gui.setScreen(disconnectScreen);
        }).bounds(this.width-185, this.height-30, 175, 20).build();
        this.addRenderableWidget(rawErrorButton);
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
        super.extractRenderState(graphics, mouseX, mouseY, delta);
        this.graphics = graphics;

        graphics.text(this.font, "Download Missing Mods", this.width / 2, 40 - this.font.lineHeight - 10, 0xFFFFFFFF, true);
    }

    private void newDownloadEntry(ModManager.ModData modData, int yOffset){
        Button downloadButton = Button.builder(Component.literal("Download"), btn -> {}).bounds(this.width*3/4, yOffset, 75, 20).build();
        this.addRenderableWidget(downloadButton);
        graphics.text(this.font, modData.name(), this.width/4, yOffset - this.font.lineHeight-10, 0xFFFFFFFF, true);
    }
}
