package sonar.fluxnetworks.common.device;

import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import sonar.fluxnetworks.api.FluxConstants;
import sonar.fluxnetworks.api.device.FluxDeviceType;
import sonar.fluxnetworks.api.device.IFluxStorage;
import sonar.fluxnetworks.common.util.FluxGuiStack;
import sonar.fluxnetworks.register.Channel;
import sonar.fluxnetworks.register.Messages;
import sonar.fluxnetworks.register.RegistryBlockEntityTypes;

import javax.annotation.Nonnull;

public abstract class TileFluxStorage extends TileFluxDevice implements IFluxStorage {

    private final FluxStorageHandler mHandler;

    protected TileFluxStorage(@Nonnull BlockEntityType<?> type, @Nonnull BlockPos pos, @Nonnull BlockState state,
                              @Nonnull FluxStorageHandler handler) {
        super(type, pos, state);
        mHandler = handler;
    }

    public static class Basic extends TileFluxStorage {

        public Basic(@Nonnull BlockPos pos, @Nonnull BlockState state) {
            super(RegistryBlockEntityTypes.BASIC_FLUX_STORAGE, pos, state,
                    new FluxStorageHandler.Basic());
        }

        @Nonnull
        @Override
        public ItemStack getDisplayStack() {
            return writeToDisplayStack(FluxGuiStack.BASIC_STORAGE);
        }
    }

    public static class Herculean extends TileFluxStorage {

        public Herculean(@Nonnull BlockPos pos, @Nonnull BlockState state) {
            super(RegistryBlockEntityTypes.HERCULEAN_FLUX_STORAGE, pos, state,
                    new FluxStorageHandler.Herculean());
        }

        @Nonnull
        @Override
        public ItemStack getDisplayStack() {
            return writeToDisplayStack(FluxGuiStack.HERCULEAN_STORAGE);
        }
    }

    public static class Gargantuan extends TileFluxStorage {

        public Gargantuan(@Nonnull BlockPos pos, @Nonnull BlockState state) {
            super(RegistryBlockEntityTypes.GARGANTUAN_FLUX_STORAGE, pos, state,
                    new FluxStorageHandler.Gargantuan());
        }

        @Nonnull
        @Override
        public ItemStack getDisplayStack() {
            return writeToDisplayStack(FluxGuiStack.GARGANTUAN_STORAGE);
        }
    }

    @Override
    public long getMaxTransferLimit() {
        return mHandler.getMaxEnergyStorage();
    }

    @Override
    protected void onServerTick() {
        super.onServerTick();
        if ((mFlags & FLAG_ENERGY_CHANGED) != 0) {
            // CORRIGIDO: usar getGameTime() corretamente
            if (level != null && (level.getGameTime() & 0b111) == 0) {
                // update model data to players who can see it
                Channel.get().sendToTrackingChunk(
                        Messages.makeDeviceBuffer(this, FluxConstants.DEVICE_S2C_STORAGE_ENERGY),
                        level.getChunkAt(worldPosition));
                mFlags &= ~FLAG_ENERGY_CHANGED;
            }
        }
    }

    @Nonnull
    @Override
    public FluxDeviceType getDeviceType() {
        return FluxDeviceType.STORAGE;
    }

    @Nonnull
    @Override
    public FluxStorageHandler getTransferHandler() {
        return mHandler;
    }

    /**
     * Make this storage full of energy (debug or admin function).
     */
    public void fillUp() {
        mHandler.fillUp();
        // this may happen without a valid network, so force to sync
        mFlags |= FLAG_ENERGY_CHANGED;
    }

    /**
     * Write data for client
     *
     * @return item stack with NBT
     * @see sonar.fluxnetworks.client.render.FluxStorageItemRenderer
     */
    @Nonnull
    protected ItemStack writeToDisplayStack(@Nonnull ItemStack stack) {
        // CORRIGIDO: Usar o novo sistema de componentes do Minecraft
        CustomData customData = stack.get(DataComponents.CUSTOM_DATA);
        CompoundTag rootTag = customData != null ? customData.copyTag() : new CompoundTag();

        // CORRIGIDO: getCompound retorna Optional, precisa usar orElse
        CompoundTag subTag = rootTag.getCompound(FluxConstants.TAG_FLUX_DATA).orElse(new CompoundTag());

        if (level != null && level.isClientSide()) {
            rootTag.putBoolean(FluxConstants.FLUX_COLOR, true);
        } else if (level != null) {
            rootTag.putBoolean(FluxConstants.FLUX_COLOR, false);
            subTag.putInt(FluxConstants.CLIENT_COLOR, getNetwork().getNetworkColor());
        }

        subTag.putLong(FluxConstants.ENERGY, getTransferBuffer());
        rootTag.put(FluxConstants.TAG_FLUX_DATA, subTag);

        // Aplicar o CompoundTag de volta ao ItemStack
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(rootTag));

        return stack;
    }
}