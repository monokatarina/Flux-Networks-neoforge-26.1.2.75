package sonar.fluxnetworks.common.connection;

import net.minecraft.util.Util;
import net.minecraft.core.GlobalPos;
import net.minecraft.nbt.*;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.server.ServerLifecycleHooks;
import sonar.fluxnetworks.api.FluxConstants;
import sonar.fluxnetworks.api.device.IFluxDevice;
import sonar.fluxnetworks.api.network.*;
import sonar.fluxnetworks.common.capability.FluxPlayer;
import sonar.fluxnetworks.common.device.TileFluxDevice;
import sonar.fluxnetworks.common.util.FluxUtils;

import javax.annotation.*;
import java.util.*;

/**
 * The base class of a flux network.
 * <p>
 * There are two common types of implementation: client and server.
 * Client instances are cache values that updated from server, used for pre-checks in UI.
 * Server instances are logical networks and are responsible for energy transfer.
 * <p>
 * When the client operates the server-side network, it needs double side checks to ensure security.
 * The server-side data will be persistent stored with the game save.
 */
@ParametersAreNonnullByDefault
public class FluxNetwork {

    /**
     * An invalid network avoids nullability checks, any operation on this network is invalid.
     * You can check {@link #isValid()} to skip your operations. Even if the operation is performed,
     * there will be no error.
     * <p>
     * A disconnected device is considered connected to this network.
     */
    public static final FluxNetwork INVALID = new FluxNetwork();

    /**
     * Constant IDs used to identify logical devices.
     *
     * @see #getLogicalDevices(int)
     */
    public static final int
            ANY = 0,
            PLUG = 1,
            POINT = 2,
            STORAGE = 3,
            CONTROLLER = 4;

    /**
     * Some contracts.
     */
    public static final int MAX_NETWORK_NAME_LENGTH = 24;
    public static final int MAX_PASSWORD_LENGTH = 16;

    public static final String NETWORK_NAME = "name";
    public static final String NETWORK_COLOR = "color";
    public static final String OWNER_UUID = "owner";
    public static final String SECURITY_LEVEL = "security";
    public static final String MEMBERS = "members";
    public static final String CONNECTIONS = "connections";

    int mID;
    String mName;
    int mColor;
    UUID mOwnerUUID;
    SecurityLevel mSecurityLevel;

    final NetworkStatistics mStatistics = new NetworkStatistics(this);
    final HashMap<UUID, NetworkMember> mMemberMap = new HashMap<>();
    /**
     * Server: {@link TileFluxDevice} (loaded) and {@link PhantomFluxDevice} (unloaded)
     * <p>
     * Client: {@link PhantomFluxDevice} (data container)
     */
    final HashMap<GlobalPos, IFluxDevice> mConnectionMap = new HashMap<>();

    FluxNetwork() {
        this(FluxConstants.INVALID_NETWORK_ID, "", FluxConstants.INVALID_NETWORK_COLOR,
                SecurityLevel.PUBLIC, Util.NIL_UUID);
    }

    FluxNetwork(int id, String name, int color, @Nonnull SecurityLevel security, @Nonnull Player owner) {
        this(id, name, color, security, owner.getUUID());
        mMemberMap.put(mOwnerUUID, NetworkMember.create(owner, AccessLevel.OWNER));
    }

    private FluxNetwork(int id, String name, int color, @Nonnull SecurityLevel security, @Nonnull UUID owner) {
        mID = id;
        mName = name;
        mColor = color;
        mSecurityLevel = security;
        mOwnerUUID = owner;
    }

    /**
     * Returns the unique ID of this network.
     *
     * @return a positive integer or {@link FluxConstants#INVALID_NETWORK_ID}
     */
    public final int getNetworkID() {
        return mID;
    }

    /**
     * @return the owner UUID
     */
    @Nonnull
    public final UUID getOwnerUUID() {
        return mOwnerUUID;
    }

    /**
     * Returns the network name. For an invalid network this is empty,
     * and client should display an alternative text instead.
     *
     * @return the name of this network
     */
    @Nonnull
    public final String getNetworkName() {
        return mName;
    }

    public boolean setNetworkName(@Nonnull String name) {
        if (!name.equals(mName) && !FluxUtils.isBadNetworkName(name)) {
            mName = name;
            return true;
        }
        return false;
    }

    /**
     * Returns the network color in 0xRRGGBB format.
     *
     * @return the network color
     */
    public final int getNetworkColor() {
        return mColor;
    }

    public boolean setNetworkColor(int color) {
        color &= 0xFFFFFF;
        if (mColor != color) {
            mColor = color;
            return true;
        }
        return false;
    }

    /**
     * Returns the security level of this network.
     *
     * @return the security level of this network
     */
    @Nonnull
    public final SecurityLevel getSecurityLevel() {
        return mSecurityLevel;
    }

    public boolean setSecurityLevel(@Nonnull SecurityLevel level) {
        if (mSecurityLevel != level) {
            mSecurityLevel = level;
            return true;
        }
        return false;
    }

    @Nonnull
    public NetworkStatistics getStatistics() {
        return mStatistics;
    }

    @Nullable
    public NetworkMember getMemberByUUID(@Nonnull UUID uuid) {
        return mMemberMap.get(uuid);
    }

    /**
     * Returns a collection view that contains all network members
     *
     * @return all members
     */
    @Nonnull
    public Collection<NetworkMember> getAllMembers() {
        return mMemberMap.values();
    }

    /**
     * Get connection by global pos from all connections collection
     *
     * @param pos global pos
     * @return possible device
     * @see #getAllConnections()
     */
    @Nullable
    public IFluxDevice getConnectionByPos(@Nonnull GlobalPos pos) {
        return mConnectionMap.get(pos);
    }

    /**
     * Returns a collection view that contains all loaded entities and unloaded devices.
     *
     * @return the list of all connections
     * @see #getLogicalDevices(int)
     */
    @Nonnull
    public Collection<IFluxDevice> getAllConnections() {
        return mConnectionMap.values();
    }

    /**
     * Ticks the server. Server only.
     */
    public void onEndServerTick() {
    }

    /**
     * Called when this network is deleted from its manager.
     */
    public void onDelete() {
        mMemberMap.clear();
        mConnectionMap.clear();
    }

    /**
     * Helper method to get player's access level for this network including super admin,
     * even if the player in not a member in the network.
     *
     * @param player the server player
     * @return access level
     */
    @Nonnull
    public AccessLevel getPlayerAccess(@Nonnull Player player) {
        final UUID uuid = player.getUUID();
        if (mOwnerUUID.equals(uuid)) {
            return AccessLevel.OWNER;
        }
        final NetworkMember member = getMemberByUUID(uuid);
        if (member != null) {
            return member.getAccessLevel();
        }
        return mSecurityLevel == SecurityLevel.PUBLIC ? AccessLevel.USER : AccessLevel.BLOCKED;
    }

    /**
     * Can player access this network logically, without password?
     *
     * @param player the player
     * @return has permission or not
     */
    public final boolean canPlayerAccess(@Nonnull Player player) {
        return canPlayerAccess(player, "");
    }

    /**
     * Can player access this network logically, with or without password?
     *
     * @param player   the player
     * @param password the password
     * @return has permission or not
     */
    public boolean canPlayerAccess(@Nonnull Player player, @Nonnull String password) {
        return getPlayerAccess(player).canUse();
    }

    /**
     * Get all network device entities with given logical type,
     * this method should be only invoked on the server side.
     *
     * @param logic the logical type
     * @return a list of devices
     */
    @Nonnull
    public List<TileFluxDevice> getLogicalDevices(int logic) {
        return Collections.emptyList();
    }

    /**
     * A sum value that limits energy going to device's buffer. Server only.
     *
     * @return buffer limit
     */
    public long getBufferLimiter() {
        return 0;
    }

    /**
     * Add a logical device to this network. Called by {@link TileFluxDevice}.
     *
     * @param device the logical device
     * @return success or not
     */
    public boolean enqueueConnectionAddition(@Nonnull TileFluxDevice device) {
        return true;
    }

    /**
     * Remove a logical device from this network. Called by {@link TileFluxDevice}.
     *
     * @param device the logical device
     * @param unload true if just chunk unload, false if it no longer belongs to this network
     */
    public void enqueueConnectionRemoval(@Nonnull TileFluxDevice device, boolean unload) {
    }

    /**
     * Change the membership of a target. Check valid first.
     *
     * @param player     the player performing this action
     * @param targetUUID the UUID of the player to change
     * @param type       the operation type, e.g. {@link FluxConstants#MEMBERSHIP_SET_USER}
     * @return a response code
     */
    public int changeMembership(Player player, UUID targetUUID, byte type) {
        throw new IllegalStateException();
    }

    /**
     * Returns whether this network is a valid network.
     * An invalid network is actually a null network, but we use a singleton to avoid nullability checks.
     *
     * @return {@code true} if it is valid, {@code false} otherwise
     * @see FluxConstants#INVALID_NETWORK_ID
     */
    public boolean isValid() {
        return false;
    }

    public void writeCustomTag(@Nonnull CompoundTag tag, byte type) {
        if (type == FluxConstants.NBT_NET_BASIC || type == FluxConstants.NBT_SAVE_ALL) {
            tag.putInt(FluxConstants.NETWORK_ID, mID);
            tag.putString(NETWORK_NAME, mName);
            tag.putInt(NETWORK_COLOR, mColor);
            tag.putString(OWNER_UUID, mOwnerUUID.toString());
            tag.putByte(SECURITY_LEVEL, mSecurityLevel.getId());
        }
        if (type == FluxConstants.NBT_SAVE_ALL) {
            Collection<NetworkMember> members = getAllMembers();
            if (!members.isEmpty()) {
                ListTag list = new ListTag();
                for (NetworkMember m : members) {
                    CompoundTag subTag = new CompoundTag();
                    m.writeNBT(subTag);
                    list.add(subTag);
                }
                tag.put(MEMBERS, list);
            }

            Collection<IFluxDevice> connections = getAllConnections();
            if (!connections.isEmpty()) {
                ListTag list = new ListTag();
                for (IFluxDevice d : connections) {
                    CompoundTag subTag = new CompoundTag();
                    d.writeCustomTag(subTag, d.isChunkLoaded()
                            ? FluxConstants.NBT_PHANTOM_UPDATE
                            : FluxConstants.NBT_SAVE_ALL);
                    list.add(subTag);
                }
                tag.put(CONNECTIONS, list);
            }
        }
        if (type == FluxConstants.NBT_NET_MEMBERS) {
            Collection<NetworkMember> members = getAllMembers();
            ListTag list = new ListTag();
            if (!members.isEmpty()) {
                for (NetworkMember m : members) {
                    CompoundTag subTag = new CompoundTag();
                    m.writeNBT(subTag);
                    list.add(subTag);
                }
            }
            List<ServerPlayer> players = ServerLifecycleHooks.getCurrentServer().getPlayerList().getPlayers();
            for (ServerPlayer p : players) {
                if (getMemberByUUID(p.getUUID()) == null) {
                    CompoundTag subTag = new CompoundTag();
                    NetworkMember.create(p, FluxPlayer.isPlayerSuperAdmin(p) ?
                                    AccessLevel.SUPER_ADMIN : AccessLevel.BLOCKED)
                            .writeNBT(subTag);
                    list.add(subTag);
                }
            }
            tag.put(MEMBERS, list);
        }
        if (type == FluxConstants.NBT_NET_ALL_CONNECTIONS) {
            Collection<IFluxDevice> connections = getAllConnections();
            if (!connections.isEmpty()) {
                ListTag list = new ListTag();
                for (IFluxDevice d : connections) {
                    CompoundTag subTag = new CompoundTag();
                    d.writeCustomTag(subTag, FluxConstants.NBT_PHANTOM_UPDATE);
                    list.add(subTag);
                }
                tag.put(CONNECTIONS, list);
            }
        }
        if (type == FluxConstants.NBT_NET_STATISTICS) {
            mStatistics.writeNBT(tag);
        }
    }

    public void readCustomTag(@Nonnull CompoundTag tag, byte type) {
        if (type == FluxConstants.NBT_NET_BASIC || type == FluxConstants.NBT_SAVE_ALL) {
            mID = tag.getIntOr(FluxConstants.NETWORK_ID, FluxConstants.INVALID_NETWORK_ID);
            mName = tag.getStringOr(NETWORK_NAME, "");
            mColor = tag.getIntOr(NETWORK_COLOR, FluxConstants.INVALID_NETWORK_COLOR);
            mOwnerUUID = UUID.fromString(tag.getStringOr(OWNER_UUID, Util.NIL_UUID.toString()));
            mSecurityLevel = SecurityLevel.fromId(tag.getByteOr(SECURITY_LEVEL, SecurityLevel.PUBLIC.getId()));
        }
        if (type == FluxConstants.NBT_SAVE_ALL) {
            ListTag list = tag.getListOrEmpty(MEMBERS);
            for (int i = 0; i < list.size(); i++) {
                CompoundTag c = list.getCompoundOrEmpty(i);  // MUDANÇA AQUI
                NetworkMember m = new NetworkMember(c);
                mMemberMap.put(m.getPlayerUUID(), m);
            }
            list = tag.getListOrEmpty(CONNECTIONS);
            for (int i = 0; i < list.size(); i++) {
                CompoundTag c = list.getCompoundOrEmpty(i);  // MUDANÇA AQUI
                PhantomFluxDevice f = PhantomFluxDevice.make(c);
                mConnectionMap.put(f.getGlobalPos(), f);
            }
        }
        if (type == FluxConstants.NBT_NET_MEMBERS) {
            mMemberMap.clear();
            ListTag list = tag.getListOrEmpty(MEMBERS);
            for (int i = 0; i < list.size(); i++) {
                CompoundTag c = list.getCompoundOrEmpty(i);  // MUDANÇA AQUI
                NetworkMember m = new NetworkMember(c);
                mMemberMap.put(m.getPlayerUUID(), m);
            }
        }
        if (type == FluxConstants.NBT_NET_ALL_CONNECTIONS) {
            mConnectionMap.clear();

            ListTag list = tag.getListOrEmpty(CONNECTIONS);
            for (int i = 0; i < list.size(); i++) {
                CompoundTag c = list.getCompoundOrEmpty(i);  // MUDANÇA AQUI
                GlobalPos pos = FluxUtils.readGlobalPos(c);
                mConnectionMap.put(pos, PhantomFluxDevice.makeUpdated(pos, c));
            }
        }
        if (type == FluxConstants.NBT_NET_STATISTICS) {
            mStatistics.readNBT(tag);
        }
    }

    @Override
    public String toString() {
        return "FluxNetwork{" +
                "id=" + mID +
                ", name='" + mName + '\'' +
                ", owner=" + mOwnerUUID +
                '}';
    }
}
