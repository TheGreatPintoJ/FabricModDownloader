package me.pintoadmin.fabricmoddownloader;

import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

public class RequestHandler {
    public record ModListRequest(String message) implements CustomPacketPayload {
        public static final CustomPacketPayload.Type<ModListRequest> TYPE = new CustomPacketPayload.Type<>(
                Identifier.fromNamespaceAndPath("fabricmoddownloader", "modlist-request"));
        public static final StreamCodec<RegistryFriendlyByteBuf, ModListRequest> CODEC =
                StreamCodec.composite(ByteBufCodecs.STRING_UTF8, ModListRequest::message, ModListRequest::new);

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }
    public record ModListResponse(String response) implements CustomPacketPayload {
        public static final CustomPacketPayload.Type<ModListResponse> TYPE = new CustomPacketPayload.Type<>(
                Identifier.fromNamespaceAndPath("fabricmoddownloader", "modlist-response"));
        public static final StreamCodec<RegistryFriendlyByteBuf, ModListResponse> CODEC =
                StreamCodec.composite(ByteBufCodecs.STRING_UTF8, ModListResponse::response, ModListResponse::new);

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    public void init(){
        ServerPlayNetworking.registerGlobalReceiver(ModListRequest.TYPE, (payload, context) -> {

        });
    }
}
