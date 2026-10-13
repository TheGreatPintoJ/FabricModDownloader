package me.pintoadmin.fabricmoddownloader.client.mixin;

import me.pintoadmin.fabricmoddownloader.client.*;
import net.fabricmc.fabric.api.client.networking.v1.*;
import net.minecraft.client.*;
import net.minecraft.client.multiplayer.*;
import net.minecraft.network.*;
import net.minecraft.network.chat.*;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.*;

@Mixin(ClientCommonPacketListenerImpl.class)
public class MixinConnection {
    @Inject(method = "onDisconnect", at = @At("HEAD"))
    private void onDisconnect(DisconnectionDetails info, CallbackInfo ci) {
        if (info != null && info.reason() != null) {
            String message = info.reason().getString();

            if (message.contains("registry") || message.contains("mismatched mod") || message.contains("unknown to this client")) {
                System.out.println("Intercepted Mod/Registry Mismatch Kick! Reason given: " + message);
                FabricmoddownloaderClient.openDownloadGUI(message);
            }
        }
    }
}