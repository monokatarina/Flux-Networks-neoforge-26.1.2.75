package sonar.fluxnetworks.common.connection;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.server.ServerLifecycleHooks;
import sonar.fluxnetworks.api.FluxConstants;
import sonar.fluxnetworks.api.device.*;
import sonar.fluxnetworks.api.network.*;
import sonar.fluxnetworks.common.capability.FluxPlayer;
import sonar.fluxnetworks.common.device.TileFluxDevice;

import javax.annotation.Nonnull;
import java.lang.reflect.Array;
import java.util.*;

/**
 * The class represents a flux Network on the logical server side.
 */
public class ServerFluxNetwork extends FluxNetwork {

    private static final Comparator<TileFluxDevice> sDescendingOrder =
            (lhs, rhs) -> Integer.compare(rhs.getTransferHandler().getPriority(),
                    lhs.getTransferHandler().getPriority());

    /**
     * See {@link #ANY}
     */
    private static final Class<?>[] sLogicalTypes =
            {IFluxDevice.class, IFluxPlug.class, IFluxPoint.class, IFluxStorage.class, IFluxController.class};

    private final ArrayList<TileFluxDevice>[] mDevices;

    // LinkedList doesn't create large arrays, should be better
    private final LinkedList<TileFluxDevice> mToAdd = new LinkedList<>();
    private final LinkedList<TileFluxDevice> mToRemove = new LinkedList<>();

    private boolean mSortConnections = true;

    private final TransferIterator mPlugTransferIterator = new TransferIterator(false);
    private final TransferIterator mPointTransferIterator = new TransferIterator(true);

    private long mBufferLimiter = 0;

    private String mPassword;

    {
        @SuppressWarnings("unchecked") final ArrayList<TileFluxDevice>[] devices =
                (ArrayList<TileFluxDevice>[]) Array.newInstance(ArrayList.class, sLogicalTypes.length);
        Arrays.setAll(devices, type -> new ArrayList<>());
        mDevices = devices;
    }

    ServerFluxNetwork() {
    }

    ServerFluxNetwork(int id, String name, int color, @Nonnull SecurityLevel security, @Nonnull Player owner,
                      @Nonnull String password) {
        super(id, name, color, security, owner);
        mPassword = password;
    }

    /*public void addConnections() {
        if (toAdd.isEmpty()) {
            return;
        }
        Iterator<IFluxConnector> iterator = toAdd.iterator();
        while (iterator.hasNext()) {
            IFluxConnector flux = iterator.next();
            FluxCacheType.getValidTypes(flux).forEach(t -> FluxUtils.addWithCheck(getConnections(t), flux));
            NeoForge.EVENT_BUS.post(new FluxConnectionEvent.Connected(flux, this));
            iterator.remove();
            sortConnections = true;
        }
    }

    public void removeConnections() {
        if (toRemove.isEmpty()) {
            return;
        }
        Iterator<IFluxConnector> iterator = toRemove.iterator();
        while (iterator.hasNext()) {
            IFluxConnector flux = iterator.next();
            FluxCacheType.getValidTypes(flux).forEach(t -> getConnections(t).removeIf(f -> f == flux));
            iterator.remove();
            sortConnections = true;
        }
    }*/

    private void handleConnectionQueue() {
        TileFluxDevice device;
        while ((device = mToAdd.poll()) != null) {
            for (int type = 0; type < sLogicalTypes.length; type++) {
                if (sLogicalTypes[type].isInstance(device)) {
                    var list = getLogicalDevices(type);
                    assert list != null && !list.contains(device);
                    mSortConnections |= list.add(device);
                }
            }
        }
        while ((device = mToRemove.poll()) != null) {
            for (int type = 0; type < sLogicalTypes.length; type++) {
                if (sLogicalTypes[type].isInstance(device)) {
                    var list = getLogicalDevices(type);
                    assert list != null && list.contains(device);
                    mSortConnections |= list.remove(device);
                }
            }
        }
        if (mSortConnections) {
            ArrayList<TileFluxDevice> plugs = getLogicalDevices(PLUG);
            ArrayList<TileFluxDevice> points = getLogicalDevices(POINT);
            if (plugs != null) plugs.sort(sDescendingOrder);
            if (points != null) points.sort(sDescendingOrder);
            mSortConnections = false;
        }
    }

    @Nonnull
    @Override
    public ArrayList<TileFluxDevice> getLogicalDevices(int logic) {
        if (logic >= 0 && logic < mDevices.length) {
            return mDevices[logic];
        }
        return new ArrayList<>();
    }

    @Override
    public void onEndServerTick() {
        mStatistics.startProfiling();

        handleConnectionQueue();

        mBufferLimiter = 0;

        List<TileFluxDevice> devices = getLogicalDevices(ANY);
        if (devices != null) {
            for (var d : devices) {
                d.getTransferHandler().onCycleStart();
            }
        }

        List<TileFluxDevice> plugs = getLogicalDevices(PLUG);
        List<TileFluxDevice> points = getLogicalDevices(POINT);
        if (points != null && plugs != null && !points.isEmpty() && !plugs.isEmpty()) {
            // push into stack because they called too many times below
            final TransferIterator plugIterator = mPlugTransferIterator.reset(plugs);
            final TransferIterator pointIterator = mPointTransferIterator.reset(points);
            CYCLE:
            while (pointIterator.hasNext()) {
                while (plugIterator.hasNext()) {
                    TileFluxDevice plug = plugIterator.next();
                    TileFluxDevice point = pointIterator.next();
                    if (plug.getDeviceType() == point.getDeviceType()) {
                        break CYCLE; // Storage always have the lowest priority, the cycle can be broken here.
                    }
                    // we don't need to simulate this action
                    long actual = plug.getTransferHandler().removeFromBuffer(point.getTransferHandler().getRequest());
                    if (actual > 0) {
                        point.getTransferHandler().addToBuffer(actual);
                        continue CYCLE;
                    } else {
                        // although the plug still need transfer (buffer > 0)
                        // but it reached max transfer limit, so we use next plug
                        plugIterator.increment();
                    }
                }
                break; // all plugs have been used
            }
        }

        long limiter = 0;
        if (devices != null) {
            for (var d : devices) {
                TransferHandler h = d.getTransferHandler();
                h.onCycleEnd();
                limiter += h.getRequest();
                if (h.getChange() != 0) {
                    d.markEnergyChanged();
                }
            }
        }
        mBufferLimiter = limiter;

        mStatistics.stopProfiling();
    }

    @Override
    public long getBufferLimiter() {
        return mBufferLimiter;
    }

    @Nonnull
    @Override
    public AccessLevel getPlayerAccess(@Nonnull Player player) {
        if (FluxPlayer.isPlayerSuperAdmin(player)) {
            return AccessLevel.SUPER_ADMIN;
        }
        return super.getPlayerAccess(player);
    }

    @Override
    public boolean canPlayerAccess(@Nonnull Player player, @Nonnull String password) {
        if (super.canPlayerAccess(player, password)) {
            return true;
        }
        // password is required if non-public network and non-network member
        return !password.isEmpty() && password.equals(mPassword);
    }

    @Override
    public void onDelete() {
        super.onDelete();
        List<TileFluxDevice> devices = getLogicalDevices(ANY);
        if (devices != null) {
            devices.forEach(TileFluxDevice::disconnect);
        }
        Arrays.fill(mDevices, null);
        mToAdd.clear();
        mToRemove.clear();
    }

    @Override
    public boolean isValid() {
        return true;
    }

    @Override
    public boolean enqueueConnectionAddition(@Nonnull TileFluxDevice device) {
        ArrayList<TileFluxDevice> controllers = getLogicalDevices(CONTROLLER);
        if (device.getDeviceType().isController() && controllers != null && controllers.size() > 0) {
            return false;
        }
        if (!mToAdd.contains(device) && !getLogicalDevices(ANY).contains(device)) {
            mToAdd.offer(device);
            mToRemove.remove(device);
            if (mConnectionMap != null) {
                mConnectionMap.put(device.getGlobalPos(), device);
            }
            return true;
        }
        return false;
    }

    @Override
    public void enqueueConnectionRemoval(@Nonnull TileFluxDevice device, boolean unload) {
        if (!mToRemove.contains(device) && getLogicalDevices(ANY).contains(device)) {
            mToRemove.offer(device);
            mToAdd.remove(device);
            if (unload && mConnectionMap != null) {
                // create a fake device on server side, representing it has ever connected to
                // this network but currently unloaded
                mConnectionMap.put(device.getGlobalPos(), PhantomFluxDevice.makeUnloaded(device));
            } else if (mConnectionMap != null) {
                // remove the tile entity
                mConnectionMap.remove(device.getGlobalPos());
            }
        }
    }

    public void setPassword(@Nonnull String password) {
        mPassword = password;
    }

    public void markSortConnections() {
        mSortConnections = true;
    }

    @Override
    public int changeMembership(@Nonnull Player player, @Nonnull UUID targetUUID, byte type) {
        final AccessLevel access = getPlayerAccess(player);
        boolean editPermission = access != null && access.canEdit();
        boolean ownerPermission = access != null && access.canDelete();
        // check permission
        if (!editPermission) {
            return FluxConstants.RESPONSE_NO_ADMIN;
        }

        // editing yourself
        final boolean self = player.getUUID().equals(targetUUID);
        // current member in the network
        final NetworkMember current = getMemberByUUID(targetUUID);

        // create new member
        if (type == FluxConstants.MEMBERSHIP_SET_USER && current == null) {
            final Player target = ServerLifecycleHooks.getCurrentServer()
                    .getPlayerList().getPlayer(targetUUID);
            if (target != null) {
                NetworkMember m = NetworkMember.create(target, AccessLevel.USER);
                if (mMemberMap != null) {
                    mMemberMap.put(m.getPlayerUUID(), m);
                }
                return FluxConstants.RESPONSE_SUCCESS;
            } else {
                // the player is offline now
                return FluxConstants.RESPONSE_INVALID_USER;
            }
        } else if (current != null) {
            // super admin can still transfer ownership to self
            if (self && current.getAccessLevel() == AccessLevel.OWNER) {
                return FluxConstants.RESPONSE_INVALID_USER;
            }
            boolean changed = false;
            if (type == FluxConstants.MEMBERSHIP_SET_ADMIN) {
                // we are not owner or super admin
                if (!ownerPermission) {
                    return FluxConstants.RESPONSE_NO_OWNER;
                }
                changed = current.setAccessLevel(AccessLevel.ADMIN);
            } else if (type == FluxConstants.MEMBERSHIP_SET_USER) {
                changed = current.setAccessLevel(AccessLevel.USER);
            } else if (type == FluxConstants.MEMBERSHIP_CANCEL_MEMBERSHIP) {
                changed = mMemberMap != null && mMemberMap.remove(targetUUID) != null;
            } else if (type == FluxConstants.MEMBERSHIP_TRANSFER_OWNERSHIP) {
                if (!ownerPermission) {
                    return FluxConstants.RESPONSE_NO_OWNER;
                }
                Collection<NetworkMember> members = getAllMembers();
                if (members != null) {
                    members.forEach(f -> {
                        if (f.getAccessLevel().canDelete()) {
                            f.setAccessLevel(AccessLevel.USER);
                        }
                    });
                }
                mOwnerUUID = targetUUID;
                current.setAccessLevel(AccessLevel.OWNER);
                changed = true;
            }
            return changed ? FluxConstants.RESPONSE_SUCCESS : FluxConstants.RESPONSE_INVALID_USER;
        } else if (type == FluxConstants.MEMBERSHIP_TRANSFER_OWNERSHIP) {
            if (!ownerPermission) {
                return FluxConstants.RESPONSE_NO_OWNER;
            }
            // super admin can still transfer ownership to self
            if (self && access == AccessLevel.OWNER) {
                return FluxConstants.RESPONSE_INVALID_USER;
            }
            Player target = ServerLifecycleHooks.getCurrentServer().getPlayerList().getPlayer(targetUUID);
            // is online
            if (target != null) {
                Collection<NetworkMember> members = getAllMembers();
                if (members != null) {
                    members.forEach(f -> {
                        if (f.getAccessLevel().canDelete()) {
                            f.setAccessLevel(AccessLevel.USER);
                        }
                    });
                }
                NetworkMember m = NetworkMember.create(target, AccessLevel.OWNER);
                if (mMemberMap != null) {
                    mMemberMap.put(m.getPlayerUUID(), m);
                }
                mOwnerUUID = targetUUID;
                return FluxConstants.RESPONSE_SUCCESS;
            } else {
                return FluxConstants.RESPONSE_INVALID_USER;
            }
        } else {
            return FluxConstants.RESPONSE_INVALID_USER;
        }
    }

    @Override
    public void writeCustomTag(@Nonnull CompoundTag tag, byte type) {
        super.writeCustomTag(tag, type);
        if (type == FluxConstants.NBT_SAVE_ALL && mPassword != null) {
            tag.putString("password", mPassword);
        }
    }

    @Override
    public void readCustomTag(@Nonnull CompoundTag tag, byte type) {
        super.readCustomTag(tag, type);
        if (tag.contains("password")) {
            // O método getString retorna Optional<String> agora
            mPassword = tag.getString("password").orElse("");
        }
    }
}