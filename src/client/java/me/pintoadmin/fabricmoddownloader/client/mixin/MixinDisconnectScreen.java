package me.pintoadmin.fabricmoddownloader.client.mixin;

import me.pintoadmin.fabricmoddownloader.client.*;
import net.minecraft.client.gui.screens.*;
import net.minecraft.network.*;
import net.minecraft.network.chat.*;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.*;

@Mixin(DisconnectedScreen.class)
public class MixinDisconnectScreen {
    @Final
    @Shadow private DisconnectionDetails details;

    @Inject(method = "init", at = @At("HEAD"))
    private void onInit(CallbackInfo ci) {
        String message = this.details.reason().getString().toLowerCase();

        if (message.contains("registry") || message.contains("mismatched mod") || message.contains("unknown to this client")) {
            FabricmoddownloaderClient.getInstance().parseReason(message);
            FabricmoddownloaderClient.getInstance().openDownloadGUI();
        }
    }
}