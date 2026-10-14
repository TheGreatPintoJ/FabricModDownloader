package me.pintoadmin.fabricmoddownloader.client;

import me.pintoadmin.fabricmoddownloader.*;
import net.minecraft.client.gui.*;
import net.minecraft.client.gui.components.*;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.*;
import net.minecraft.network.chat.*;
import net.minecraft.network.chat.Component;

import java.awt.*;
import java.util.*;
import java.util.List;

import static me.pintoadmin.fabricmoddownloader.Fabricmoddownloader.*;

public class OptionalModsScreen extends Screen {
    private final ModManager modManager;
    private final List<ModManager.ModData> mods;
    private boolean hasDownloadedAny = false;
    private boolean downloadError = false;

    private final List<Button> downloadButtons = new ArrayList<>();
    private final Map<String, Integer> downloadEntries = new HashMap<>();

    public OptionalModsScreen(List<ModManager.ModData> mods) {
        super(Component.literal(""));
        this.mods = mods;
        modManager = ModManager.getInstance();
    }

    @Override
    protected void init() {
        downloadButtons.forEach(this::removeWidget);
        downloadButtons.clear();
        downloadEntries.clear();
        LOGGER.warn("Optional mods: "+mods);
        int startingY = 30;
        int spacing = 25;
        for (ModManager.ModData data : mods){
            String modID = modManager.findMod(data);
            if(modID.isBlank()){
                LOGGER.warn("Could not find mod on modrinth: "+data.id());
                newFailedEntry(data.id(), startingY+=spacing);
                continue;
            }
            ModManager.ModData mod = modManager.getModData(modID);
            if(mod != null) newDownloadEntry(mod, startingY+=spacing);
            else {
                ModManager.ModData modData;
                String id = modManager.findModByName(data.name());
                modData = modManager.getModData(id);
                if(modData != null) newDownloadEntry(modData, startingY+=spacing);
            }
        }

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