package sonar.fluxnetworks.common.device;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.transfer.energy.EnergyHandler;
import net.neoforged.neoforge.transfer.transaction.TransactionContext;
import sonar.fluxnetworks.api.device.FluxDeviceType;
import sonar.fluxnetworks.api.device.IFluxPlug;
import sonar.fluxnetworks.api.energy.IFNEnergyStorage;
import sonar.fluxnetworks.common.util.FluxGuiStack;
import sonar.fluxnetworks.register.RegistryBlockEntityTypes;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class TileFluxPlug extends TileFluxConnector implements IFluxPlug {

    private final FluxPlugHandler mHandler = new FluxPlugHandler();

    public TileFluxPlug(@Nonnull BlockPos pos, @Nonnull BlockState state) {
        super(RegistryBlockEntityTypes.FLUX_PLUG, pos, state);
    }

    @Nonnull
    @Override
    public FluxDeviceType getDeviceType() {
        return FluxDeviceType.PLUG;
    }

    @Nonnull
    @Override
    public FluxPlugHandler getTransferHandler() {
        return mHandler;
    }

    @Nonnull
    @Override
    public ItemStack getDisplayStack() {
        return FluxGuiStack.FLUX_PLUG;
    }

    // Método para EnergyHandler (com Direction)
    @Nullable
    public EnergyHandler getEnergyHandler(@Nullable Direction side) {
        if (isRemoved() || !getNetwork().isValid()) return null;
        return new PlugEnergyHandler(side);
    }

    // Método para IFNEnergyStorage (com Direction - pode ser null)
    @Nullable
    public IFNEnergyStorage getFluxEnergyStorage(@Nullable Direction side) {
        if (isRemoved() || !getNetwork().isValid()) return null;
        return new PlugFluxEnergyHandler(side);
    }

    // EnergyHandler implementation - baseado na API correta
    private class PlugEnergyHandler implements EnergyHandler {

        @Nullable
        private final Direction mSide;

        public PlugEnergyHandler(@Nullable Direction side) {
            this.mSide = side;
        }

        @Override
        public long getAmountAsLong() {
            return mHandler.getBuffer();
        }

        @Override
        public long getCapacityAsLong() {
            return Math.max(mHandler.getBuffer(), mHandler.getLimit());
        }

        @Override
        public int insert(int amount, TransactionContext transaction) {
            if (!getNetwork().isValid()) return 0;
            return (int) mHandler.receive(amount, mSide, false, getNetwork().getBufferLimiter());
        }

        @Override
        public int extract(int amount, TransactionContext transaction) {
            return 0; // Plugs não extraem energia
        }
    }

    // IFNEnergyStorage implementation
    private class PlugFluxEnergyHandler implements IFNEnergyStorage {

        @Nullable
        private final Direction mSide;

        public PlugFluxEnergyHandler(@Nullable Direction side) {
            mSide = side;
        }

        @Override
        public long receiveEnergyL(long maxReceive, boolean simulate) {
            if (getNetwork().isValid()) {
                return mHandler.receive(maxReceive, mSide, simulate, getNetwork().getBufferLimiter());
            }
            return 0;
        }

        @Override
        public long extractEnergyL(long maxExtract, boolean simulate) {
            return 0;
        }

        @Override
        public long getEnergyStoredL() {
            return mHandler.getBuffer();
        }

        @Override
        public long getMaxEnergyStoredL() {
            return Math.max(mHandler.getBuffer(), mHandler.getLimit());
        }

        @Override
        public boolean canExtract() {
            return false;
        }

        @Override
        public boolean canReceive() {
            return getNetwork().isValid();
        }
    }
}