package sonar.fluxnetworks.common.connection;

import com.mojang.serialization.Codec;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;
import net.neoforged.neoforge.server.ServerLifecycleHooks;
import sonar.fluxnetworks.FluxConfig;
import sonar.fluxnetworks.FluxNetworks;
import sonar.fluxnetworks.api.FluxConstants;
import sonar.fluxnetworks.api.network.SecurityLevel;
import sonar.fluxnetworks.common.capability.FluxPlayer;
import sonar.fluxnetworks.register.Channel;
import sonar.fluxnetworks.register.Messages;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.NotThreadSafe;
import java.util.Collection;
import java.util.UUID;

/**
 * Manage all logical flux networks and save their data to the world.
 * <p>
 * Only on logical server side. Only on server thread.
 */
@NotThreadSafe
public final class FluxNetworkData extends SavedData {

    // Usar Identifier.fromNamespaceAndPath em vez de Identifier.of
    private static final Identifier NETWORK_DATA_ID = Identifier.fromNamespaceAndPath(FluxNetworks.MODID, "data");

    // Criar o Codec para FluxNetworkData - usando CompoundTag.CODEC diretamente
    public static final Codec<FluxNetworkData> CODEC = Codec.lazyInitialized(() ->
            CompoundTag.CODEC.xmap(
                    tag -> {
                        // Deserialização
                        FluxNetworkData data = new FluxNetworkData();
                        data.loadData(tag);
                        return data;
                    },
                    data -> {
                        // Serialização
                        CompoundTag tag = new CompoundTag();
                        data.save(tag);
                        return tag;
                    }
            )
    );

    // Criar o SavedDataType para FluxNetworkData
    public static final SavedDataType<FluxNetworkData> TYPE = new SavedDataType<>(
            NETWORK_DATA_ID,      // Identifier
            FluxNetworkData::new, // Supplier<T> constructor
            CODEC                 // Codec<T> codec
    );

    private static volatile FluxNetworkData data;

    private static final String NETWORKS = "networks";
    private static final String UNIQUE_ID = "uniqueID";

    private final Int2ObjectMap<FluxNetwork> mNetworks = new Int2ObjectOpenHashMap<>();
    private int mUniqueID = 0;

    public FluxNetworkData() {
    }

    @Nonnull
    public static FluxNetworkData getInstance() {
        if (data == null) {
            var server = ServerLifecycleHooks.getCurrentServer();
            if (server == null) {
                throw new IllegalStateException("FluxNetworkData cannot be accessed without a running server!");
            }
            ServerLevel level = server.overworld();
            data = level.getDataStorage().computeIfAbsent(TYPE);
            FluxNetworks.LOGGER.debug("FluxNetworkData has been successfully loaded");
        }
        return data;
    }

    // called when the server instance changed, e.g. switching single player saves
    public static void release() {
        if (data != null) {
            data = null;
            FluxNetworks.LOGGER.debug("FluxNetworkData has been unloaded");
        }
    }

    @Nonnull
    public static FluxNetwork getNetwork(int id) {
        return getInstance().mNetworks.getOrDefault(id, FluxNetwork.INVALID);
    }

    @Nonnull
    public static Collection<FluxNetwork> getAllNetworks() {
        return getInstance().mNetworks.values();
    }

    @Nullable
    public FluxNetwork createNetwork(@Nonnull Player creator, @Nonnull String name, int color,
                                     @Nonnull SecurityLevel security, @Nonnull String password) {
        final int max = FluxConfig.maximumPerPlayer;
        if (max != -1 && !FluxPlayer.isPlayerSuperAdmin(creator)) {
            if (max <= 0) {
                return null;
            }
            final UUID uuid = creator.getUUID();
            int i = 0;
            for (var n : mNetworks.values()) {
                if (n.getOwnerUUID().equals(uuid) && ++i >= max) {
                    return null;
                }
            }
        }
        do {
            mUniqueID++;
        } while (mNetworks.containsKey(mUniqueID));

        final ServerFluxNetwork network = new ServerFluxNetwork(mUniqueID, name, color, security, creator, password);

        mNetworks.put(network.getNetworkID(), network);
        setDirty();
        Channel.get().sendToAll(Messages.updateNetwork(network, FluxConstants.NBT_NET_BASIC));
        return network;
    }

    public void deleteNetwork(@Nonnull FluxNetwork network) {
        if (mNetworks.remove(network.getNetworkID()) == network) {
            network.onDelete();
            setDirty();
            Messages.deleteNetwork(network.getNetworkID());
        }
    }

    @Override
    public boolean isDirty() {
        return true;
    }

    private void loadData(@Nonnull CompoundTag compound) {
        mUniqueID = compound.getInt(UNIQUE_ID).orElse(0);

        compound.getList(NETWORKS).ifPresent(list -> {
            for (int i = 0; i < list.size(); i++) {
                list.getCompound(i).ifPresent(networkTag -> {
                    ServerFluxNetwork network = new ServerFluxNetwork();
                    network.readCustomTag(networkTag, FluxConstants.NBT_SAVE_ALL);
                    if (network.getNetworkID() > 0) {
                        mNetworks.put(network.getNetworkID(), network);
                    }
                });
            }
        });
    }

    @Nonnull
    public CompoundTag save(@Nonnull CompoundTag compound) {
        compound.putInt(UNIQUE_ID, mUniqueID);

        ListTag list = new ListTag();
        for (FluxNetwork network : mNetworks.values()) {
            CompoundTag tag = new CompoundTag();
            network.writeCustomTag(tag, FluxConstants.NBT_SAVE_ALL);
            list.add(tag);
        }
        compound.put(NETWORKS, list);

        return compound;
    }
}
