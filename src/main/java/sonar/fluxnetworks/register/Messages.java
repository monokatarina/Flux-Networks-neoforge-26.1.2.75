package sonar.fluxnetworks.register;

import io.netty.handler.codec.DecoderException;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import sonar.fluxnetworks.FluxNetworks;
import sonar.fluxnetworks.api.FluxConstants;
import sonar.fluxnetworks.api.device.IFluxDevice;
import sonar.fluxnetworks.api.network.SecurityLevel;
import sonar.fluxnetworks.api.network.WirelessType;
import sonar.fluxnetworks.common.capability.FluxPlayer;
import sonar.fluxnetworks.common.connection.FluxMenu;
import sonar.fluxnetworks.common.connection.FluxNetwork;
import sonar.fluxnetworks.common.connection.FluxNetworkData;
import sonar.fluxnetworks.common.connection.ServerFluxNetwork;
import sonar.fluxnetworks.common.device.TileFluxDevice;
import sonar.fluxnetworks.common.item.ItemAdminConfigurator;
import sonar.fluxnetworks.common.item.ItemFluxConfigurator;
import sonar.fluxnetworks.common.util.FluxUtils;
import javax.annotation.Nonnull;
import javax.annotation.ParametersAreNonnullByDefault;
import java.util.*;
import java.util.function.Supplier;

import static sonar.fluxnetworks.register.Channel.sChannel;

@ParametersAreNonnullByDefault
@SuppressWarnings({"unused", "DuplicatedCode"})
public class Messages {

    static final int C2S_DEVICE_BUFFER = 0;
    static final int C2S_SUPER_ADMIN = 1;
    static final int C2S_CREATE_NETWORK = 2;
    static final int C2S_DELETE_NETWORK = 3;
    static final int C2S_EDIT_TILE = 4;
    static final int C2S_TILE_NETWORK = 5;
    static final int C2S_EDIT_ITEM = 6;
    static final int C2S_ITEM_NETWORK = 7;
    static final int C2S_EDIT_MEMBER = 8;
    static final int C2S_EDIT_NETWORK = 9;
    static final int C2S_EDIT_CONNECTION = 10;
    static final int C2S_UPDATE_NETWORK = 11;
    static final int C2S_WIRELESS_MODE = 12;
    static final int C2S_DISCONNECT = 13;
    static final int C2S_UPDATE_CONNECTIONS = 14;
    static final int C2S_TRACK_MEMBERS = 15;
    static final int C2S_TRACK_CONNECTIONS = 16;
    static final int C2S_TRACK_STATISTICS = 17;

    static final int S2C_DEVICE_BUFFER = 0;
    static final int S2C_RESPONSE = 1;
    static final int S2C_CAPABILITY = 2;
    static final int S2C_UPDATE_NETWORK = 3;
    static final int S2C_DELETE_NETWORK = 4;
    static final int S2C_UPDATE_CONNECTIONS = 5;
    static final int S2C_UPDATE_MEMBERS = 6;

    @Nonnull
    public static FriendlyByteBuf makeDeviceBuffer(TileFluxDevice device, byte type) {
        assert type < 0;
        var buf = Channel.buffer(S2C_DEVICE_BUFFER);
        buf.writeBlockPos(device.getBlockPos());
        buf.writeByte(type);
        device.writePacketBuffer(buf, type);
        return buf;
    }

    private static void response(int token, int key, int code, Player player) {
        var buf = Channel.buffer(S2C_RESPONSE);
        buf.writeByte(token);
        buf.writeShort(key);
        buf.writeByte(code);
        sChannel.sendToPlayer(buf, player);
    }

    public static void syncCapability(Player player) {
        var buf = Channel.buffer(S2C_CAPABILITY);
        FluxPlayer fluxPlayer = FluxPlayer.get(player);
        buf.writeBoolean(FluxPlayer.isPlayerSuperAdmin(player));
        buf.writeInt(fluxPlayer.getWirelessMode());
        buf.writeVarInt(fluxPlayer.getWirelessNetwork());
        sChannel.sendToPlayer(buf, player);
    }

    @Nonnull
    public static FriendlyByteBuf updateNetwork(FluxNetwork network, byte type) {
        var buf = Channel.buffer(S2C_UPDATE_NETWORK);
        buf.writeByte(type);
        buf.writeVarInt(1);
        buf.writeVarInt(network.getNetworkID());
        final var tag = new CompoundTag();
        network.writeCustomTag(tag, type);
        buf.writeNbt(tag);
        return buf;
    }

    @Nonnull
    private static FriendlyByteBuf updateConnections(FluxNetwork network, List<CompoundTag> tags) {
        var buf = Channel.buffer(S2C_UPDATE_CONNECTIONS);
        buf.writeVarInt(network.getNetworkID());
        buf.writeVarInt(tags.size());
        for (CompoundTag tag : tags) {
            buf.writeNbt(tag);
        }
        return buf;
    }

    @Nonnull
    private static List<CompoundTag> makeConnectionTags(FluxNetwork network) {
        List<CompoundTag> tags = new ArrayList<>();
        for (IFluxDevice device : network.getAllConnections()) {
            CompoundTag tag = new CompoundTag();
            device.writeCustomTag(tag, FluxConstants.NBT_PHANTOM_UPDATE);
            tags.add(tag);
        }
        return tags;
    }

    @Nonnull
    public static FriendlyByteBuf updateNetwork(Collection<FluxNetwork> networks, byte type) {
        var buf = Channel.buffer(S2C_UPDATE_NETWORK);
        buf.writeByte(type);
        buf.writeVarInt(networks.size());
        for (var network : networks) {
            buf.writeVarInt(network.getNetworkID());
            final var tag = new CompoundTag();
            network.writeCustomTag(tag, type);
            buf.writeNbt(tag);
        }
        return buf;
    }

    @Nonnull
    private static FriendlyByteBuf updateNetwork(int[] networkIDs, byte type) {
        var buf = Channel.buffer(S2C_UPDATE_NETWORK);
        buf.writeByte(type);
        buf.writeVarInt(networkIDs.length);
        for (var networkID : networkIDs) {
            buf.writeVarInt(networkID);
            final var tag = new CompoundTag();
            FluxNetworkData.getNetwork(networkID).writeCustomTag(tag, type);
            buf.writeNbt(tag);
        }
        return buf;
    }

    public static void deleteNetwork(int id) {
        var buf = Channel.buffer(S2C_DELETE_NETWORK);
        buf.writeVarInt(id);
        sChannel.sendToAll(buf);
    }

    static void msg(short index, FriendlyByteBuf payload, Supplier<ServerPlayer> player) {
        switch (index) {
            case C2S_DEVICE_BUFFER -> onDeviceBuffer(payload, player);
            case C2S_SUPER_ADMIN -> onSuperAdmin(payload, player);
            case C2S_CREATE_NETWORK -> onCreateNetwork(payload, player);
            case C2S_DELETE_NETWORK -> onDeleteNetwork(payload, player);
            case C2S_EDIT_TILE -> onEditTile(payload, player);
            case C2S_TILE_NETWORK -> onTileNetwork(payload, player);
            case C2S_ITEM_NETWORK -> onItemNetwork(payload, player);
            case C2S_EDIT_MEMBER -> onEditMember(payload, player);
            case C2S_EDIT_NETWORK -> onEditNetwork(payload, player);
            case C2S_EDIT_CONNECTION -> onEditConnection(payload, player);
            case C2S_UPDATE_NETWORK -> onUpdateNetwork(payload, player);
            case C2S_WIRELESS_MODE -> onWirelessMode(payload, player);
            case C2S_DISCONNECT -> onDisconnect(payload, player);
            case C2S_UPDATE_CONNECTIONS -> onUpdateConnections(payload, player);
            default -> kick(player.get(), new RuntimeException("Unidentified message index " + index));
        }
    }

    private static void kick(ServerPlayer p, RuntimeException e) {
        if (!p.level().isClientSide()) {
            p.connection.disconnect(Component.translatable("multiplayer.disconnect.invalid_packet"));
            FluxNetworks.LOGGER.info("Received invalid packet from player {}", p.getGameProfile().name(), e);
        } else {
            FluxNetworks.LOGGER.info("Received invalid packet", e);
        }
    }

    private static void consume(FriendlyByteBuf payload) {
        if (payload.isReadable()) {
            throw new DecoderException("Payload is not fully consumed");
        }
    }

    private static void onDeviceBuffer(FriendlyByteBuf payload, Supplier<ServerPlayer> player) {
        payload.retain();
        ServerPlayer p = player.get();
        if (p != null && p.level().getBlockEntity(payload.readBlockPos()) instanceof TileFluxDevice e) {
            if (e.canPlayerAccess(p)) {
                byte id = payload.readByte();
                if (id > 0) {
                    e.readPacketBuffer(payload, id);
                }
                consume(payload);
            }
        }
        payload.release();
    }

    private static void onSuperAdmin(FriendlyByteBuf payload, Supplier<ServerPlayer> player) {
        final int token = payload.readByte();
        final boolean enable = payload.readBoolean();
        consume(payload);
        ServerPlayer p = player.get();
        if (p == null) return;
        FluxPlayer fp = FluxPlayer.get(p);
        if (fp.isSuperAdmin() || FluxPlayer.canActivateSuperAdmin(p)) {
            if (fp.setSuperAdmin(enable)) {
                syncCapability(p);
            }
        } else {
            response(token, 0, FluxConstants.RESPONSE_REJECT, p);
        }
    }

    private static void onCreateNetwork(FriendlyByteBuf payload, Supplier<ServerPlayer> player) {
        final int token = payload.readByte();
        final String name = payload.readUtf(256);
        final int color = payload.readInt();
        final SecurityLevel security = SecurityLevel.fromId(payload.readByte());
        final String password = security == SecurityLevel.ENCRYPTED ? payload.readUtf(256) : "";
        consume(payload);
        ServerPlayer p = player.get();
        if (p == null) return;
        boolean reject = p.containerMenu.containerId != token || !(p.containerMenu instanceof FluxMenu);
        if (reject) {
            response(token, FluxConstants.REQUEST_CREATE_NETWORK, FluxConstants.RESPONSE_REJECT, p);
            return;
        }
        if (FluxNetworkData.getInstance().createNetwork(p, name, color, security, password) != null) {
            response(token, FluxConstants.REQUEST_CREATE_NETWORK, FluxConstants.RESPONSE_SUCCESS, p);
        } else {
            response(token, FluxConstants.REQUEST_CREATE_NETWORK, FluxConstants.RESPONSE_NO_SPACE, p);
        }
    }

    private static void onDeleteNetwork(FriendlyByteBuf payload, Supplier<ServerPlayer> player) {
        final int token = payload.readByte();
        final int networkID = payload.readVarInt();
        consume(payload);
        ServerPlayer p = player.get();
        if (p == null) return;
        final FluxNetwork network = FluxNetworkData.getNetwork(networkID);
        if (network.isValid()) {
            if (network.getPlayerAccess(p).canDelete()) {
                FluxNetworkData.getInstance().deleteNetwork(network);
                response(token, FluxConstants.REQUEST_DELETE_NETWORK, FluxConstants.RESPONSE_SUCCESS, p);
            } else {
                response(token, FluxConstants.REQUEST_DELETE_NETWORK, FluxConstants.RESPONSE_NO_OWNER, p);
            }
        } else {
            response(token, FluxConstants.REQUEST_DELETE_NETWORK, FluxConstants.RESPONSE_REJECT, p);
        }
    }

    private static void onEditTile(FriendlyByteBuf payload, Supplier<ServerPlayer> player) {
        final int token = payload.readByte();
        final BlockPos pos = payload.readBlockPos();
        final CompoundTag tag = payload.readNbt();
        consume(payload);
        Objects.requireNonNull(tag);
        ServerPlayer p = player.get();
        if (p == null) return;
        boolean reject = p.containerMenu.containerId != token || !(p.containerMenu instanceof FluxMenu);
        if (reject) {
            response(token, FluxConstants.REQUEST_EDIT_TILE, FluxConstants.RESPONSE_REJECT, p);
            return;
        }
        if (p.level().isLoaded(pos) && p.level().getBlockEntity(pos) instanceof TileFluxDevice e && e.canPlayerAccess(p)) {
            e.readCustomTag(tag, FluxConstants.NBT_TILE_SETTINGS);
        } else {
            response(token, FluxConstants.REQUEST_EDIT_TILE, FluxConstants.RESPONSE_REJECT, p);
        }
    }

    private static void onTileNetwork(FriendlyByteBuf payload, Supplier<ServerPlayer> player) {
        final int token = payload.readByte();
        final BlockPos pos = payload.readBlockPos();
        final int networkID = payload.readVarInt();
        final String password = payload.readUtf(256);
        consume(payload);
        ServerPlayer p = player.get();
        if (p == null) return;
        if (p.level().getBlockEntity(pos) instanceof TileFluxDevice e) {
            if (e.getNetworkID() == networkID) return;
            if (!e.canPlayerAccess(p)) {
                response(token, FluxConstants.REQUEST_TILE_NETWORK, FluxConstants.RESPONSE_REJECT, p);
                return;
            }
            final FluxNetwork network = FluxNetworkData.getNetwork(networkID);
            if (e.getDeviceType().isController() && !network.getLogicalDevices(FluxNetwork.CONTROLLER).isEmpty()) {
                response(token, FluxConstants.REQUEST_TILE_NETWORK, FluxConstants.RESPONSE_HAS_CONTROLLER, p);
                return;
            }
            if (!network.isValid() || network.canPlayerAccess(p, password)) {
                if (network.isValid()) {
                    e.setOwnerUUID(p.getUUID());
                }
                e.connect(network);
                response(token, FluxConstants.REQUEST_TILE_NETWORK, FluxConstants.RESPONSE_SUCCESS, p);
                return;
            }
            if (password.isEmpty()) {
                response(token, FluxConstants.REQUEST_TILE_NETWORK, FluxConstants.RESPONSE_REQUIRE_PASSWORD, p);
            } else {
                response(token, FluxConstants.REQUEST_TILE_NETWORK, FluxConstants.RESPONSE_INVALID_PASSWORD, p);
            }
        } else {
            response(token, FluxConstants.REQUEST_TILE_NETWORK, FluxConstants.RESPONSE_REJECT, p);
        }
    }

    private static void onItemNetwork(FriendlyByteBuf payload, Supplier<ServerPlayer> player) {
        final int token = payload.readByte();
        final int networkID = payload.readVarInt();
        final String password = payload.readUtf(256);
        consume(payload);
        ServerPlayer p = player.get();
        if (p == null) return;
        boolean reject = p.containerMenu.containerId != token ||
                !(p.containerMenu instanceof FluxMenu menu) ||
                !(menu.mProvider instanceof ItemFluxConfigurator.Provider) ||
                !p.getMainHandItem().is(RegistryItems.FLUX_CONFIGURATOR);
        if (reject) {
            response(token, FluxConstants.REQUEST_TILE_NETWORK, FluxConstants.RESPONSE_REJECT, p);
            return;
        }
        final FluxNetwork network = FluxNetworkData.getNetwork(networkID);
        if (!network.isValid() || network.canPlayerAccess(p, password)) {
            ItemStack stack = p.getMainHandItem();
            CustomData.update(DataComponents.CUSTOM_DATA, stack, root -> {
                CompoundTag tag = root.getCompoundOrEmpty(FluxConstants.TAG_FLUX_CONFIG);
                tag.putInt(FluxConstants.NETWORK_ID, network.getNetworkID());
                root.put(FluxConstants.TAG_FLUX_CONFIG, tag);
            });
            response(token, FluxConstants.REQUEST_TILE_NETWORK, FluxConstants.RESPONSE_SUCCESS, p);
            return;
        }
        if (password.isEmpty()) {
            response(token, FluxConstants.REQUEST_TILE_NETWORK, FluxConstants.RESPONSE_REQUIRE_PASSWORD, p);
        } else {
            response(token, FluxConstants.REQUEST_TILE_NETWORK, FluxConstants.RESPONSE_INVALID_PASSWORD, p);
        }
    }

    private static void onEditNetwork(FriendlyByteBuf payload, Supplier<ServerPlayer> player) {
        final int token = payload.readByte();
        final int networkID = payload.readVarInt();
        final String name = payload.readUtf(256);
        final int color = payload.readInt();
        final SecurityLevel security = SecurityLevel.fromId(payload.readByte());
        final String password = security == SecurityLevel.ENCRYPTED ? payload.readUtf(256) : "";
        consume(payload);
        ServerPlayer p = player.get();
        if (p == null) return;
        final FluxNetwork network = FluxNetworkData.getNetwork(networkID);
        boolean reject = checkTokenFailed(token, p, network);
        if (reject) {
            response(token, FluxConstants.REQUEST_EDIT_NETWORK, FluxConstants.RESPONSE_REJECT, p);
            return;
        }
        assert network.isValid();
        if (network.getPlayerAccess(p).canEdit()) {
            boolean changed = network.setNetworkName(name);
            boolean colorChanged = network.setNetworkColor(color);
            if (colorChanged) {
                network.getLogicalDevices(FluxNetwork.ANY).forEach(device -> {
                    device.sendBlockUpdate();
                    device.markChunkUnsaved();
                });
                changed = true;
            }
            changed |= network.setSecurityLevel(security);
            if (!password.isEmpty()) {
                ((ServerFluxNetwork) network).setPassword(password);
            }
            if (changed) {
                sChannel.sendToAll(updateNetwork(network, FluxConstants.NBT_NET_BASIC));
                if (colorChanged) {
                    sChannel.sendToAll(updateConnections(network, makeConnectionTags(network)));
                }
            }
            response(token, FluxConstants.REQUEST_EDIT_NETWORK, FluxConstants.RESPONSE_SUCCESS, p);
        } else {
            response(token, FluxConstants.REQUEST_EDIT_NETWORK, FluxConstants.RESPONSE_NO_ADMIN, p);
        }
    }

    private static void onUpdateNetwork(FriendlyByteBuf payload, Supplier<ServerPlayer> player) {
        final int token = payload.readByte();
        final int size = payload.readVarInt();
        if (size <= 0) throw new IllegalArgumentException();
        final int[] networkIDs = new int[size];
        for (int i = 0; i < size; i++) {
            networkIDs[i] = payload.readVarInt();
        }
        final byte type = payload.readByte();
        consume(payload);
        ServerPlayer p = player.get();
        if (p == null) return;
        boolean reject = true;
        if (p.containerMenu.containerId == token && p.containerMenu instanceof FluxMenu menu) {
            if (FluxPlayer.isPlayerSuperAdmin(p)) {
                reject = false;
            } else if (networkIDs.length == 1) {
                int networkID = networkIDs[0];
                final FluxNetwork network = FluxNetworkData.getNetwork(networkID);
                if (!(menu.mProvider instanceof ItemAdminConfigurator.Provider)) {
                    if (network.isValid() && menu.mProvider.getNetworkID() == networkID) {
                        reject = false;
                    }
                } else {
                    if (network.isValid() && network.canPlayerAccess(p)) {
                        reject = false;
                    }
                }
            }
        }
        if (reject) {
            response(token, FluxConstants.REQUEST_UPDATE_NETWORK, FluxConstants.RESPONSE_REJECT, p);
        } else {
            sChannel.sendToPlayer(updateNetwork(networkIDs, type), p);
        }
    }

    private static void onEditMember(FriendlyByteBuf payload, Supplier<ServerPlayer> player) {
        final int token = payload.readByte();
        final int networkID = payload.readVarInt();
        final UUID targetUUID = payload.readUUID();
        final byte type = payload.readByte();
        consume(payload);
        ServerPlayer p = player.get();
        if (p == null) return;
        final FluxNetwork network = FluxNetworkData.getNetwork(networkID);
        boolean reject = checkTokenFailed(token, p, network);
        if (reject) {
            response(token, FluxConstants.REQUEST_EDIT_MEMBER, FluxConstants.RESPONSE_REJECT, p);
            return;
        }
        assert network.isValid();
        int code = network.changeMembership(p, targetUUID, type);
        if (code == FluxConstants.RESPONSE_SUCCESS) {
            sChannel.sendToPlayer(updateNetwork(network, FluxConstants.NBT_NET_MEMBERS), p);
        }
        response(token, FluxConstants.REQUEST_EDIT_MEMBER, code, p);
    }

    private static void onEditConnection(FriendlyByteBuf payload, Supplier<ServerPlayer> player) {
        final int token = payload.readByte();
        final int networkID = payload.readVarInt();
        final int size = payload.readVarInt();
        if (size <= 0) throw new IllegalArgumentException();
        final List<GlobalPos> list = new ArrayList<>(size);
        for (int i = 0; i < size; i++) {
            list.add(FluxUtils.readGlobalPos(payload));
        }
        final CompoundTag tag = payload.readNbt();
        consume(payload);
        Objects.requireNonNull(tag);
        ServerPlayer p = player.get();
        if (p == null) return;
        final FluxNetwork network = FluxNetworkData.getNetwork(networkID);
        boolean reject = checkTokenFailed(token, p, network);
        if (reject) {
            response(token, FluxConstants.REQUEST_EDIT_CONNECTION, FluxConstants.RESPONSE_REJECT, p);
            return;
        }
        assert network.isValid();
        if (network.getPlayerAccess(p).canEdit()) {
            for (GlobalPos pos : list) {
                IFluxDevice f = network.getConnectionByPos(pos);
                if (f instanceof TileFluxDevice e) {
                    e.readCustomTag(tag, FluxConstants.NBT_TILE_SETTINGS);
                }
            }
            response(token, FluxConstants.REQUEST_EDIT_CONNECTION, FluxConstants.RESPONSE_SUCCESS, p);
        } else {
            response(token, FluxConstants.REQUEST_EDIT_CONNECTION, FluxConstants.RESPONSE_NO_ADMIN, p);
        }
    }

    private static boolean checkTokenFailed(int token, Player p, FluxNetwork network) {
        if (!network.isValid()) return true;
        if (p.containerMenu.containerId == token && p.containerMenu instanceof FluxMenu menu) {
            if (FluxPlayer.isPlayerSuperAdmin(p)) {
                return false;
            } else {
                if (!(menu.mProvider instanceof ItemAdminConfigurator.Provider)) {
                    return menu.mProvider.getNetworkID() != network.getNetworkID() && !network.canPlayerAccess(p);
                } else {
                    return !network.canPlayerAccess(p);
                }
            }
        }
        return true;
    }

    private static void onWirelessMode(FriendlyByteBuf payload, Supplier<ServerPlayer> player) {
        final int token = payload.readByte();
        final int wirelessMode = payload.readInt();
        final int wirelessNetwork = payload.readVarInt();
        consume(payload);
        ServerPlayer p = player.get();
        if (p == null) return;
        FluxPlayer fp = FluxPlayer.get(p);
        final FluxNetwork network = FluxNetworkData.getNetwork(wirelessNetwork);
        boolean reject = network.isValid() && (checkTokenFailed(token, p, network) || network.getMemberByUUID(p.getUUID()) == null);
        if (reject) {
            if (WirelessType.ENABLE_WIRELESS.isActivated(wirelessMode)) {
                response(token, 0, FluxConstants.RESPONSE_REJECT, p);
            } else {
                fp.setWirelessMode(wirelessMode);
                syncCapability(p);
            }
        } else {
            fp.setWirelessMode(wirelessMode);
            fp.setWirelessNetwork(wirelessNetwork);
            syncCapability(p);
        }
    }

    private static void onDisconnect(FriendlyByteBuf payload, Supplier<ServerPlayer> player) {
        final int token = payload.readByte();
        final int networkID = payload.readVarInt();
        final int size = payload.readVarInt();
        if (size <= 0) throw new IllegalArgumentException();
        final List<GlobalPos> list = new ArrayList<>(size);
        for (int i = 0; i < size; i++) {
            list.add(FluxUtils.readGlobalPos(payload));
        }
        consume(payload);
        ServerPlayer p = player.get();
        if (p == null) return;
        final FluxNetwork network = FluxNetworkData.getNetwork(networkID);
        boolean reject = checkTokenFailed(token, p, network);
        if (reject) {
            response(token, FluxConstants.REQUEST_DISCONNECT, FluxConstants.RESPONSE_REJECT, p);
            return;
        }
        assert network.isValid();
        if (network.getPlayerAccess(p).canEdit()) {
            for (GlobalPos pos : list) {
                IFluxDevice f = network.getConnectionByPos(pos);
                if (f instanceof TileFluxDevice e) {
                    e.disconnect();
                }
            }
            response(token, FluxConstants.REQUEST_DISCONNECT, FluxConstants.RESPONSE_SUCCESS, p);
        } else {
            response(token, FluxConstants.REQUEST_DISCONNECT, FluxConstants.RESPONSE_NO_ADMIN, p);
        }
    }

    private static void onUpdateConnections(FriendlyByteBuf payload, Supplier<ServerPlayer> player) {
        final int token = payload.readByte();
        final int networkID = payload.readVarInt();
        final int size = payload.readVarInt();
        if (size <= 0 || size > 7) throw new IllegalArgumentException();
        final List<GlobalPos> list = new ArrayList<>(size);
        for (int i = 0; i < size; i++) {
            list.add(FluxUtils.readGlobalPos(payload));
        }
        consume(payload);
        ServerPlayer p = player.get();
        if (p == null) return;
        final FluxNetwork network = FluxNetworkData.getNetwork(networkID);
        boolean reject = checkTokenFailed(token, p, network);
        if (reject) {
            response(token, FluxConstants.REQUEST_UPDATE_CONNECTION, FluxConstants.RESPONSE_REJECT, p);
            return;
        }
        assert network.isValid();
        if (network.canPlayerAccess(p)) {
            List<CompoundTag> tags = new ArrayList<>();
            for (GlobalPos pos : list) {
                IFluxDevice f = network.getConnectionByPos(pos);
                if (f != null) {
                    CompoundTag subTag = new CompoundTag();
                    f.writeCustomTag(subTag, FluxConstants.NBT_PHANTOM_UPDATE);
                    tags.add(subTag);
                }
            }
            sChannel.sendToPlayer(updateConnections(network, tags), p);
        } else {
            response(token, FluxConstants.REQUEST_UPDATE_CONNECTION, FluxConstants.RESPONSE_REJECT, p);
        }
    }
}
