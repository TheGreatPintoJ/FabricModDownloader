package me.pintoadmin.fabricmoddownloader.client;

import net.minecraft.client.*;
import net.minecraft.client.gui.*;
import net.minecraft.client.gui.components.*;
import net.minecraft.client.gui.components.toasts.*;
import net.minecraft.client.gui.screens.*;
import net.minecraft.network.chat.*;

import java.util.*;

public class DownloadModsScreen extends Screen {
    private final Screen parent;
    private final int WIDTH;
    private final int HEIGHT;
    public DownloadModsScreen(Component title, Screen parent) {
        super(title);
        this.parent = parent;
        WIDTH = Minecraft.getInstance().getWindow().getScreenWidth();
        HEIGHT = Minecraft.getInstance().getWindow().getScreenHeight();
    }

    @Override
    protected void init() {
        Button downloadButton = Button.builder(Component.literal("Download"), btn -> {

        }).bounds(WIDTH/2, 80, 75, 20).build();

        Button searchButton = Button.builder(Component.literal("Search for missing mod"), btn -> {
            this.addRenderableWidget(downloadButton);
        }).bounds(WIDTH/2, 40, 100, 20).build();

        this.addRenderableWidget(searchButton);
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
        super.extractRenderState(graphics, mouseX, mouseY, delta);

        graphics.text(this.font, "Download Missing Mods", WIDTH / 2, 40 - this.font.lineHeight - 10, 0xFFFFFFFF, true);
    }

    @Override
    public void onClose(){
        this.minecraft.gui.setScreen(this.parent);
    }
}
