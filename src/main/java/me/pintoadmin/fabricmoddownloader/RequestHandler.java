package me.pintoadmin.fabricmoddownloader;

import com.fasterxml.jackson.core.*;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.node.*;
import com.fasterxml.jackson.datatype.jdk8.*;
import net.fabricmc.fabric.api.networking.v1.*;
import net.minecraft.network.*;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import org.jetbrains.annotations.*;

import java.util.*;

public class RequestHandler {
    private final ModManager modManager;
    public RequestHandler(ModManager modManager){
        this.modManager = modManager;
    }

    public record ModListRequest(String message) implements CustomPacketPayload {
        public static final CustomPacketPayload.Type<@NotNull ModListRequest> TYPE = new CustomPacketPayload.Type<>(
                Identifier.fromNamespaceAndPath("fabricmoddownloader", "modlist-request"));
        public static final StreamCodec<@NotNull FriendlyByteBuf, @NotNull ModListRequest> CODEC =
                StreamCodec.composite(ByteBufCodecs.STRING_UTF8, ModListRequest::message, ModListRequest::new);

        @Override
        public @NotNull Type<? extends @NotNull CustomPacketPayload> type() {
            return TYPE;
        }
    }
    public record ModListResponse(String response) implements CustomPacketPayload {
        public static final CustomPacketPayload.Type<@NotNull ModListResponse> TYPE = new CustomPacketPayload.Type<>(
                Identifier.fromNamespaceAndPath("fabricmoddownloader", "modlist-response"));
        public static final StreamCodec<@NotNull FriendlyByteBuf, @NotNull ModListResponse> CODEC =
                StreamCodec.composite(ByteBufCodecs.STRING_UTF8, ModListResponse::response, ModListResponse::new);

        @Override
        public @NotNull Type<? extends @NotNull CustomPacketPayload> type() {
            return TYPE;
        }
    }

    public void initServer(){
        PayloadTypeRegistry.serverboundPlay().register(ModListRequest.TYPE, ModListRequest.CODEC);
        PayloadTypeRegistry.clientboundPlay().register(ModListResponse.TYPE, ModListResponse.CODEC);

        ServerPlayNetworking.registerGlobalReceiver(ModListRequest.TYPE, (_, context) -> {
            List<ModManager.ModData> clientMods = modManager.getClientModlist();
            ObjectMapper jsonMapper = new ObjectMapper();
            jsonMapper.registerModule(new Jdk8Module());

            try {
                String clientModsJson = jsonMapper.writeValueAsString(clientMods/*mods*/);
                ModListResponse payload = new ModListResponse(clientModsJson);
                ServerPlayNetworking.send(context.player(), payload);
            } catch (JsonProcessingException e) {
                throw new RuntimeException(e);
            }
        });
    }
    public void initClient(){
        PayloadTypeRegistry.serverboundPlay().register(ModListRequest.TYPE, ModListRequest.CODEC);
        PayloadTypeRegistry.clientboundPlay().register(ModListResponse.TYPE, ModListResponse.CODEC);
    }
}
