package sonar.fluxnetworks.client;

import it.unimi.dsi.fastutil.ints.*;
import net.minecraft.client.Minecraft;
import net.minecraft.core.GlobalPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.api.distmarker.Dist;
import sonar.fluxnetworks.FluxNetworks;
import sonar.fluxnetworks.api.FluxConstants;
import sonar.fluxnetworks.api.device.IFluxDevice;
import sonar.fluxnetworks.common.connection.ClientFluxNetwork;
import sonar.fluxnetworks.common.connection.FluxNetwork;
import sonar.fluxnetworks.common.device.TileFluxDevice;
import sonar.fluxnetworks.common.util.FluxUtils;

import javax.annotation.Nonnull;
import java.util.Collection;
import java.util.List;

/**
 * Main thread only.
 */
public final class ClientCache {

    private static final int MAX_RECENT_PASSWORD_COUNT = 5;

    private static final Int2ObjectOpenHashMap<FluxNetwork> sNetworks =
            new Int2ObjectOpenHashMap<>();
    private static final Int2ObjectLinkedOpenHashMap<String> sRecentPasswords =
            new Int2ObjectLinkedOpenHashMap<>(MAX_RECENT_PASSWORD_COUNT); // LRU cache

    public static boolean sSuperAdmin = false;
    public static int sWirelessMode = 0;
    public static int sWirelessNetwork = FluxConstants.INVALID_NETWORK_ID;
    public static boolean sDetailedNetworkView = false;

    public static int sAdminViewingNetwork = FluxConstants.INVALID_NETWORK_ID;

    private ClientCache() {
    }

    /**
     * Release buffers and view models.
     */
    public static void release() {
        sNetworks.clear();
        sNetworks.trim(); // rehash
        sRecentPasswords.clear(); // preserved memory, no need to rehash
        sAdminViewingNetwork = FluxConstants.INVALID_NETWORK_ID;
        FluxNetworks.LOGGER.info("Released client Flux Networks cache");
    }

    /**
     * Cleanup members and connections cache.
     */
    public static void cleanup() {
        sNetworks.values().forEach(FluxNetwork::onDelete);
    }

    public static void updateNetwork(@Nonnull Int2ObjectMap<CompoundTag> map, byte type) {
        for (var e : map.int2ObjectEntrySet()) {
            sNetworks.computeIfAbsent(e.getIntKey(), ClientFluxNetwork::new)
                    .readCustomTag(e.getValue(), type);
            if (type == FluxConstants.NBT_NET_BASIC) {
                refreshLoadedNetworkBlocks(e.getIntKey());
            }
        }
    }

    public static void updateConnections(int networkID, @Nonnull List<CompoundTag> tags) {
        final FluxNetwork network = sNetworks.get(networkID);
        if (network != null) {
            for (var tag : tags) {
                network.updateClientConnection(tag);
            }
            refreshLoadedNetworkBlocks(networkID);
        }
    }

    public static void refreshLoadedNetworkBlocks(int networkID) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null) {
            return;
        }
        FluxNetwork network = getNetwork(networkID);
        if (!network.isValid()) {
            return;
        }
        for (IFluxDevice connection : network.getAllConnections()) {
            GlobalPos pos = connection.getGlobalPos();
            if (!pos.dimension().equals(minecraft.level.dimension()) || !minecraft.level.isLoaded(pos.pos())) {
                continue;
            }
            if (minecraft.level.getBlockEntity(pos.pos()) instanceof TileFluxDevice device &&
                    device.getNetworkID() == networkID) {
                device.setClientNetwork(network);
            } else {
                BlockState state = minecraft.level.getBlockState(pos.pos());
                minecraft.level.sendBlockUpdated(pos.pos(), state, state, Block.UPDATE_ALL_IMMEDIATE);
            }
        }
    }

    @Nonnull
    public static FluxNetwork getNetwork(int id) {
        return sNetworks.getOrDefault(id, FluxNetwork.INVALID);
    }

    @Nonnull
    public static Collection<FluxNetwork> getAllNetworks() {
        return sNetworks.values();
    }

    public static void deleteNetwork(int id) {
        sNetworks.remove(id);
    }

    @Nonnull
    public static String getRecentPassword(int id) {
        return sRecentPasswords.getOrDefault(id, "");
    }

    public static void updateRecentPassword(int id, String password) {
        // remember last 5 passwords so that no need to enter password again
        if (sRecentPasswords.size() >= MAX_RECENT_PASSWORD_COUNT) {
            // Remove the oldest entry (first)
            int firstKey = sRecentPasswords.keySet().iterator().next();
            sRecentPasswords.remove(firstKey);
        }
        sRecentPasswords.put(id, password);
    }
}
