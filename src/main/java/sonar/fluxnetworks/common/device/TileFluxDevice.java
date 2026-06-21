package sonar.fluxnetworks.common.device;

import net.minecraft.util.Util;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.Connection;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import sonar.fluxnetworks.FluxConfig;
import sonar.fluxnetworks.api.FluxConstants;
import sonar.fluxnetworks.api.FluxTranslate;
import sonar.fluxnetworks.api.device.IFluxDevice;
import sonar.fluxnetworks.client.ClientCache;
import sonar.fluxnetworks.common.connection.*;
import sonar.fluxnetworks.common.util.FluxUtils;
import sonar.fluxnetworks.register.Channel;
import sonar.fluxnetworks.register.Messages;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import java.util.UUID;

/**
 * Represents a network device entity (on server) and a block entity.
 */
@ParametersAreNonnullByDefault
public abstract class TileFluxDevice extends BlockEntity implements IFluxDevice {

    private static final BlockEntityTicker<? extends TileFluxDevice> sTickerServer =
            (level, pos, state, tile) -> tile.onServerTick();

    public static final int INVALID_CLIENT_COLOR =
            FluxUtils.getModifiedColor(FluxConstants.INVALID_NETWORK_COLOR, 1.1f);

    public static final int MAX_CUSTOM_NAME_LENGTH = 24;

    @Nullable
    private Player mPlayerUsing;

    @Nonnull
    private String mCustomName = "";
    @Nonnull
    private UUID mOwnerUUID = Util.NIL_UUID;

    private int mNetworkID = FluxConstants.INVALID_NETWORK_ID;

    public int mClientColor = INVALID_CLIENT_COLOR;

    protected static final int SIDES_CONNECTED_MASK = 0x3F;
    protected static final int FLAG_FORCED_LOADING = 0x40;
    protected static final int FLAG_FIRST_TICKED = 0x80;
    protected static final int FLAG_SETTING_CHANGED = 0x100;
    protected static final int FLAG_ENERGY_CHANGED = 0x200;
    private static final String DISPLAY_ITEM = "displayItem";
    private static final String FLUX_DATA = "fluxData";

    protected int mFlags;

    @Nullable
    private GlobalPos mGlobalPos;

    @Nonnull
    private FluxNetwork mNetwork = FluxNetwork.INVALID;

    protected TileFluxDevice(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    @Nullable
    @SuppressWarnings("unchecked")
    public static <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level) {
        return level.isClientSide() ? null : (BlockEntityTicker<T>) sTickerServer;
    }

    @Override
    public void setRemoved() {
        super.setRemoved();
        if (level != null && !level.isClientSide() && (mFlags & FLAG_FIRST_TICKED) != 0) {
            mNetwork.enqueueConnectionRemoval(this, false);
            if (isForcedLoading() && level instanceof ServerLevel serverLevel) {
                serverLevel.setChunkForced(worldPosition.getX() >> 4, worldPosition.getZ() >> 4, false);
            }
            getTransferHandler().onNetworkChanged();
            mFlags &= ~FLAG_FIRST_TICKED;
        }
    }

    @Override
    public void onChunkUnloaded() {
        super.onChunkUnloaded();
        if (level != null && !level.isClientSide() && (mFlags & FLAG_FIRST_TICKED) != 0) {
            mNetwork.enqueueConnectionRemoval(this, true);
            getTransferHandler().onNetworkChanged();
            mFlags &= ~FLAG_FIRST_TICKED;
        }
    }

    protected void onServerTick() {
        if ((mFlags & FLAG_FIRST_TICKED) == 0) {
            onFirstTick();
            mFlags |= FLAG_FIRST_TICKED;
        }
        if ((mFlags & FLAG_SETTING_CHANGED) != 0) {
            sendBlockUpdate();
            mFlags &= ~FLAG_SETTING_CHANGED;
        } else if (mPlayerUsing != null) {
            Channel.get().sendToPlayer(
                    Messages.makeDeviceBuffer(this, FluxConstants.DEVICE_S2C_GUI_SYNC), mPlayerUsing);
        }
    }

    protected void onFirstTick() {
        connect(FluxNetworkData.getNetwork(mNetworkID));
    }

    public void onPlayerInteract(Player player) {
        assert level != null && !level.isClientSide();
        if (mPlayerUsing != null) {
            player.sendSystemMessage(FluxTranslate.ACCESS_OCCUPY);
        } else if (canPlayerAccess(player) && player instanceof ServerPlayer serverPlayer) {
            serverPlayer.openMenu(this, buf -> writeScreenOpeningData(serverPlayer, buf));
        } else {
            player.sendSystemMessage(FluxTranslate.ACCESS_DENIED);
        }
    }

    public void writeScreenOpeningData(ServerPlayer player, FriendlyByteBuf buf) {
        buf.writeBoolean(true);
        buf.writeBlockPos(worldPosition);
        CompoundTag tag = new CompoundTag();
        writeCustomTag(tag, FluxConstants.NBT_TILE_UPDATE);
        buf.writeNbt(tag);
    }
    public final FluxMenu createMenu(int containerId, Inventory inventory, Player player) {
        return new FluxMenu(containerId, inventory, this);
    }

    public boolean connect(FluxNetwork network) {
        assert level != null && !level.isClientSide();
        if (mNetwork == network) {
            return true;
        }
        if (network.enqueueConnectionAddition(this)) {
            mNetwork.enqueueConnectionRemoval(this, false);
            mNetwork = network;
            mNetworkID = mNetwork.getNetworkID();
            getTransferHandler().onNetworkChanged();
            mFlags |= FLAG_SETTING_CHANGED;
            setChanged();
            return true;
        }
        return false;
    }

    public final void disconnect() {
        connect(FluxNetwork.INVALID);
    }

    @Override
    public final int getNetworkID() {
        return mNetworkID;
    }

    @Nonnull
    public final FluxNetwork getNetwork() {
        return mNetwork;
    }

    public final void setClientNetwork(@Nonnull FluxNetwork network) {
        if (level != null && !level.isClientSide()) {
            return;
        }
        mNetwork = network;
        mNetworkID = network.getNetworkID();
        setClientColor(network.getNetworkColor());
    }


    public abstract TransferHandler getTransferHandler();


    @Override
    public final ClientboundBlockEntityDataPacket getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }


    @Override
    public final void onDataPacket(Connection net, ValueInput input) {
        readUpdateInput(input);
        if (level != null) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_ALL_IMMEDIATE);
        }
    }

    @Nonnull
    @Override
    public final CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        CompoundTag tag = new CompoundTag();
        writeCustomTag(tag, FluxConstants.NBT_TILE_UPDATE);
        return tag;
    }

    @Override
    public final void handleUpdateTag(ValueInput input) {
        readUpdateInput(input);
    }

    @Override
    protected final void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        CompoundTag tag = new CompoundTag();
        writeCustomTag(tag, FluxConstants.NBT_SAVE_ALL);
        output.store(FLUX_DATA, CompoundTag.CODEC, tag);
    }

    @Override
    protected final void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        input.read(FLUX_DATA, CompoundTag.CODEC)
                .ifPresent(tag -> readCustomTag(tag, FluxConstants.NBT_SAVE_ALL));
    }

    @Override
    public void setLevel(@Nonnull Level level) {
        super.setLevel(level);
        mGlobalPos = GlobalPos.of(level.dimension(), worldPosition);
    }

    public void writeCustomTag(@Nonnull CompoundTag tag, byte type) {
        tag.putInt(FluxConstants.NETWORK_ID, mNetworkID);
        tag.putString(FluxConstants.CUSTOM_NAME, mCustomName);
        getTransferHandler().writeCustomTag(tag, type);
        switch (type) {
            case FluxConstants.NBT_SAVE_ALL -> {
                tag.putString(FluxConstants.PLAYER_UUID, mOwnerUUID.toString());
                tag.putInt(FluxConstants.CLIENT_COLOR, getNetworkColorForClient());
            }
            case FluxConstants.NBT_TILE_UPDATE -> {
                tag.putString(FluxConstants.PLAYER_UUID, mOwnerUUID.toString());
                tag.putInt(FluxConstants.CLIENT_COLOR, getNetworkColorForClient());
                tag.putInt(FluxConstants.FLAGS, mFlags);
            }
            case FluxConstants.NBT_PHANTOM_UPDATE -> {
                FluxUtils.writeGlobalPos(tag, getGlobalPos());
                tag.putByte(FluxConstants.DEVICE_TYPE, getDeviceType().getId());
                tag.putString(FluxConstants.PLAYER_UUID, mOwnerUUID.toString());
                tag.putBoolean(FluxConstants.FORCED_LOADING, isForcedLoading());
                tag.putBoolean(FluxConstants.CHUNK_LOADED, isChunkLoaded());
                writeDisplayStack(tag);
            }
        }
    }

    private void writeDisplayStack(@Nonnull CompoundTag tag) {
        ItemStack displayStack = getDisplayStack();
        if (!displayStack.isEmpty()) {
            Identifier id = BuiltInRegistries.ITEM.getKey(displayStack.getItem());
            tag.putString(DISPLAY_ITEM, id.toString());
        }
    }

    public void readCustomTag(@Nonnull CompoundTag tag, byte type) {
        if (type == FluxConstants.NBT_TILE_SETTINGS) {
            assert level != null && !level.isClientSide();
            if (tag.isEmpty()) {
                return;
            }
            if (tag.contains(FluxConstants.CUSTOM_NAME)) {
                String name = tag.getString(FluxConstants.CUSTOM_NAME).orElse("");
                if (name.length() <= MAX_CUSTOM_NAME_LENGTH) {
                    mCustomName = name;
                }
            }
            boolean sort = getTransferHandler().changeSettings(tag);
            if (sort && mNetwork.isValid()) {
                ((ServerFluxNetwork) mNetwork).markSortConnections();
            }
            if (tag.contains(FluxConstants.FORCED_LOADING) && level instanceof ServerLevel serverLevel) {
                boolean load = tag.getBoolean(FluxConstants.FORCED_LOADING).orElse(false) &&
                        FluxConfig.enableChunkLoading && !getDeviceType().isStorage();
                serverLevel.setChunkForced(worldPosition.getX() >> 4, worldPosition.getZ() >> 4, load);
                setForcedLoading(load);
            }
            mFlags |= FLAG_SETTING_CHANGED;
            setChanged();
            return;
        }
        mNetworkID = tag.getInt(FluxConstants.NETWORK_ID).orElse(FluxConstants.INVALID_NETWORK_ID);
        mCustomName = tag.getString(FluxConstants.CUSTOM_NAME).orElse("");
        getTransferHandler().readCustomTag(tag, type);
        switch (type) {
            case FluxConstants.NBT_SAVE_ALL -> {
                String uuidString = tag.getString(FluxConstants.PLAYER_UUID).orElse(Util.NIL_UUID.toString());
                try {
                    mOwnerUUID = UUID.fromString(uuidString);
                } catch (IllegalArgumentException e) {
                    mOwnerUUID = Util.NIL_UUID;
                }
                if (tag.contains(FluxConstants.CLIENT_COLOR)) {
                    setClientColor(tag.getInt(FluxConstants.CLIENT_COLOR).orElse(0));
                }
            }
            case FluxConstants.NBT_TILE_UPDATE -> {
                String uuidString = tag.getString(FluxConstants.PLAYER_UUID).orElse(Util.NIL_UUID.toString());
                try {
                    mOwnerUUID = UUID.fromString(uuidString);
                } catch (IllegalArgumentException e) {
                    mOwnerUUID = Util.NIL_UUID;
                }
                if (tag.contains(FluxConstants.CLIENT_COLOR)) {
                    setClientColor(tag.getInt(FluxConstants.CLIENT_COLOR).orElse(0));
                }
                mFlags = tag.getInt(FluxConstants.FLAGS).orElse(0);
            }
            case FluxConstants.NBT_TILE_DROP -> {
                if (level != null && level.isClientSide()) {
                    setClientColor(ClientCache.getNetwork(mNetworkID).getNetworkColor());
                }
            }
        }
    }

    private void setClientColor(int color) {
        int previousColor = mClientColor;
        mClientColor = FluxUtils.getModifiedColor(color, 1.1f);
        if (level != null && level.isClientSide() && previousColor != mClientColor) {
            BlockState state = getBlockState();
            level.sendBlockUpdated(worldPosition, state, state, Block.UPDATE_ALL_IMMEDIATE);
        }
    }

    private int getNetworkColorForClient() {
        if (mNetwork.isValid()) {
            return mNetwork.getNetworkColor();
        }
        return FluxNetworkData.getNetwork(mNetworkID).getNetworkColor();
    }

    private void readUpdateInput(ValueInput input) {
        CompoundTag tag = new CompoundTag();
        tag.putInt(FluxConstants.NETWORK_ID, input.getIntOr(
                FluxConstants.NETWORK_ID, FluxConstants.INVALID_NETWORK_ID));
        tag.putString(FluxConstants.CUSTOM_NAME, input.getStringOr(FluxConstants.CUSTOM_NAME, ""));
        input.getString(FluxConstants.PLAYER_UUID).ifPresent(value -> tag.putString(FluxConstants.PLAYER_UUID, value));
        input.getInt(FluxConstants.CLIENT_COLOR).ifPresent(value -> tag.putInt(FluxConstants.CLIENT_COLOR, value));
        input.getInt(FluxConstants.FLAGS).ifPresent(value -> tag.putInt(FluxConstants.FLAGS, value));
        input.getLong(FluxConstants.CHANGE).ifPresent(value -> tag.putLong(FluxConstants.CHANGE, value));
        input.getInt(FluxConstants.PRIORITY).ifPresent(value -> tag.putInt(FluxConstants.PRIORITY, value));
        tag.putBoolean(FluxConstants.SURGE_MODE, input.getBooleanOr(FluxConstants.SURGE_MODE, false));
        tag.putLong(FluxConstants.LIMIT, input.getLongOr(FluxConstants.LIMIT, 0));
        tag.putBoolean(FluxConstants.DISABLE_LIMIT, input.getBooleanOr(FluxConstants.DISABLE_LIMIT, false));
        readCustomTag(tag, FluxConstants.NBT_TILE_UPDATE);
    }

    public boolean canPlayerAccess(@Nonnull Player player) {
        assert level != null && !level.isClientSide();
        if (mNetwork.isValid()) {
            if (player.getUUID().equals(mOwnerUUID)) {
                return true;
            }
            return mNetwork.canPlayerAccess(player);
        }
        return true;
    }

    public void sendBlockUpdate() {
        assert level != null && !level.isClientSide();
        level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_ALL_IMMEDIATE);
    }

    public void writePacketBuffer(FriendlyByteBuf buf, byte type) {
        getTransferHandler().writePacketBuffer(buf, type);
    }

    public void readPacketBuffer(FriendlyByteBuf buf, byte type) {
        getTransferHandler().readPacketBuffer(buf, type);
    }

    public void markChunkUnsaved() {
        setChanged();
    }

    public void markEnergyChanged() {
        mFlags |= FLAG_ENERGY_CHANGED;
        setChanged();
    }

    @Nonnull
    @Override
    public final UUID getOwnerUUID() {
        return mOwnerUUID;
    }

    public final void setOwnerUUID(@Nonnull UUID uuid) {
        if (!mOwnerUUID.equals(uuid)) {
            mOwnerUUID = uuid;
            mFlags |= FLAG_SETTING_CHANGED;
            setChanged();
        }
    }

    @Override
    public void onPlayerOpened(Player player) {
        assert mPlayerUsing == null;
        mPlayerUsing = player;
    }

    @Override
    public void onPlayerClosed(Player player) {
        assert level != null && (level.isClientSide() || mPlayerUsing == player);
        mPlayerUsing = null;
    }

    @Nonnull
    @Override
    public final String getCustomName() {
        return mCustomName;
    }

    @Override
    public boolean isChunkLoaded() {
        return !isRemoved();
    }

    @Override
    public boolean isForcedLoading() {
        return (mFlags & FLAG_FORCED_LOADING) != 0;
    }

    public void setForcedLoading(boolean forcedLoading) {
        if (forcedLoading) {
            mFlags |= FLAG_FORCED_LOADING;
        } else {
            mFlags &= ~FLAG_FORCED_LOADING;
        }
    }

    @Override
    public final int getRawPriority() {
        return getTransferHandler().getRawPriority();
    }

    @Override
    public final long getRawLimit() {
        return getTransferHandler().getRawLimit();
    }

    @Override
    public long getMaxTransferLimit() {
        return Long.MAX_VALUE;
    }

    @Override
    public final long getTransferBuffer() {
        return getTransferHandler().getBuffer();
    }

    @Override
    public final long getTransferChange() {
        return getTransferHandler().getChange();
    }

    @Nonnull
    @Override
    public final GlobalPos getGlobalPos() {
        if (mGlobalPos == null) {
            if (level == null) {
                // Fallback - retorna algo não-nulo
                return GlobalPos.of(Level.OVERWORLD, BlockPos.ZERO);
            }
            mGlobalPos = GlobalPos.of(level.dimension(), worldPosition);
        }
        return mGlobalPos;
    }

    @Override
    public final boolean getDisableLimit() {
        return getTransferHandler().getDisableLimit();
    }

    @Override
    public final boolean getSurgeMode() {
        return getTransferHandler().getSurgeMode();
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() +
                '{' +
                "mNetworkID=" + mNetworkID +
                ", mGlobalPos=" + mGlobalPos +
                '}';
    }
}
