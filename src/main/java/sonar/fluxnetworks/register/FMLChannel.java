package sonar.fluxnetworks.register;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.ClientboundCustomPayloadPacket;
import net.minecraft.network.protocol.common.ServerboundCustomPayloadPacket;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.chunk.LevelChunk;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.neoforged.neoforge.server.ServerLifecycleHooks;
import sonar.fluxnetworks.FluxNetworks;

import javax.annotation.Nonnull;
import java.util.function.Supplier;

public class FMLChannel extends Channel {

    private static final Identifier CHANNEL_NAME = FluxNetworks.location("network");
    private static final CustomPacketPayload.Type<ByteBufPayload> TYPE = new CustomPacketPayload.Type<>(CHANNEL_NAME);
    private static final StreamCodec<RegistryFriendlyByteBuf, ByteBufPayload> CODEC = StreamCodec.of(
            FMLChannel::encode,
            FMLChannel::decode
    );

    FMLChannel() {
        Channel.sChannel = this;
    }

    public static void registerPayloads(RegisterPayloadHandlersEvent event) {
        event.registrar(FluxNetworks.MODID)
                .versioned(Channel.PROTOCOL)
                .playBidirectional(TYPE, CODEC, FMLChannel::handleServerboundPayload, FMLChannel::handleClientboundPayload);
    }

    @Override
    public void sendToServer(@Nonnull FriendlyByteBuf payload) {
        ClientPacketListener connection = Minecraft.getInstance().getConnection();
        if (connection != null) {
            // CORRIGIDO: agora usa CustomPacketPayload
            connection.send(new ServerboundCustomPayloadPacket(new ByteBufPayload(CHANNEL_NAME, payload)));
        } else {
            payload.release();
        }
    }

    @Override
    public void sendToPlayer(@Nonnull FriendlyByteBuf payload, @Nonnull ServerPlayer player) {
        player.connection.send(new ClientboundCustomPayloadPacket(new ByteBufPayload(CHANNEL_NAME, payload)));
    }

    @Override
    public void sendToAll(@Nonnull FriendlyByteBuf payload) {
        var server = ServerLifecycleHooks.getCurrentServer();
        if (server != null) {
            server.getPlayerList()
                    .broadcastAll(new ClientboundCustomPayloadPacket(new ByteBufPayload(CHANNEL_NAME, payload)));
        }
    }

    @Override
    public void sendToTrackingChunk(@Nonnull FriendlyByteBuf payload, @Nonnull LevelChunk chunk) {
        final ClientboundCustomPayloadPacket packet = new ClientboundCustomPayloadPacket(new ByteBufPayload(CHANNEL_NAME, payload));
        ((ServerLevel) chunk.getLevel()).getChunkSource().chunkMap.getPlayers(
                chunk.getPos(), false).forEach(p -> p.connection.send(packet));
    }

    private static void encode(RegistryFriendlyByteBuf buf, ByteBufPayload payload) {
        buf.writeBytes(payload.data, payload.data.readerIndex(), payload.data.readableBytes());
    }

    private static ByteBufPayload decode(RegistryFriendlyByteBuf buf) {
        FriendlyByteBuf payload = new FriendlyByteBuf(buf.readBytes(buf.readableBytes()));
        return new ByteBufPayload(CHANNEL_NAME, payload);
    }

    private static void handleServerboundPayload(ByteBufPayload payload, IPayloadContext context) {
        FriendlyByteBuf data = payload.getData();
        short index = data.readShort();
        Supplier<ServerPlayer> serverPlayer = () -> context.player() instanceof ServerPlayer player ? player : null;
        Messages.msg(index, data, serverPlayer);
    }

    private static void handleClientboundPayload(ByteBufPayload payload, IPayloadContext context) {
        FriendlyByteBuf data = payload.getData();
        short index = data.readShort();
        ClientMessages.msg(index, data, () -> Minecraft.getInstance().player);
    }

    // Classe auxiliar para criar CustomPacketPayload
    public static class ByteBufPayload implements CustomPacketPayload {
        private final Identifier id;
        private final FriendlyByteBuf data;

        public ByteBufPayload(Identifier id, FriendlyByteBuf data) {
            this.id = id;
            this.data = data;
        }

        @Override
        public Type<ByteBufPayload> type() {
            return TYPE;
        }

        public FriendlyByteBuf getData() {
            return data;
        }
    }
}
