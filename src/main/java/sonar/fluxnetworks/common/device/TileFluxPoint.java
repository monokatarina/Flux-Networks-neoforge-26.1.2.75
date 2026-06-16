package sonar.fluxnetworks.common.device;

import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.transfer.energy.EnergyHandler;
import net.neoforged.neoforge.transfer.transaction.TransactionContext;
import sonar.fluxnetworks.api.FluxCapabilities;
import sonar.fluxnetworks.api.device.FluxDeviceType;
import sonar.fluxnetworks.api.device.IFluxPoint;
import sonar.fluxnetworks.api.energy.IFNEnergyStorage;
import sonar.fluxnetworks.common.util.FluxGuiStack;
import sonar.fluxnetworks.register.RegistryBlockEntityTypes;

import javax.annotation.Nonnull;

public class TileFluxPoint extends TileFluxConnector implements IFluxPoint {

    private final FluxPointHandler mHandler = new FluxPointHandler();

    public TileFluxPoint(@Nonnull BlockPos pos, @Nonnull BlockState state) {
        super(RegistryBlockEntityTypes.FLUX_POINT, pos, state);
    }

    @Nonnull
    @Override
    public FluxDeviceType getDeviceType() {
        return FluxDeviceType.POINT;
    }

    @Nonnull
    @Override
    public FluxPointHandler getTransferHandler() {
        return mHandler;
    }

    @Nonnull
    @Override
    public ItemStack getDisplayStack() {
        return FluxGuiStack.FLUX_POINT;
    }

    public static void registerCapabilities(RegisterCapabilitiesEvent event) {
        event.registerBlockEntity(
                FluxCapabilities.FN_ENERGY_STORAGE_BLOCK,
                RegistryBlockEntityTypes.FLUX_POINT,
                (point, context) -> point.getEnergyStorage()
        );
        event.registerBlockEntity(
                Capabilities.Energy.BLOCK,
                RegistryBlockEntityTypes.FLUX_POINT,
                (point, side) -> point.getEnergyHandler()
        );
    }

    public EnergyStorage getEnergyStorage() {
        return new EnergyStorage();
    }

    public PointEnergyHandler getEnergyHandler() {
        return new PointEnergyHandler();
    }

    public class PointEnergyHandler implements EnergyHandler {

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
            return 0;
        }

        @Override
        public int extract(int amount, TransactionContext transaction) {
            return (int) Math.min(mHandler.removeFromBuffer(amount), Integer.MAX_VALUE);
        }
    }

    public class EnergyStorage implements IFNEnergyStorage {

        public EnergyStorage() {
        }

        ///// IFNEnergyStorage (Flux Networks) - Versão Long \\\\\

        @Override
        public long receiveEnergyL(long maxReceive, boolean simulate) {
            return 0;
        }

        @Override
        public long extractEnergyL(long maxExtract, boolean simulate) {
            long amount = Math.min(maxExtract, mHandler.getBuffer());
            if (!simulate) {
                return mHandler.removeFromBuffer(amount);
            }
            return amount;
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
            return true;
        }

        @Override
        public boolean canReceive() {
            return false;
        }

        ///// Compatibilidade com Forge/NeoForge - Versão Int \\\\\

        public int receiveEnergy(int maxReceive, boolean simulate) {
            return (int) Math.min(receiveEnergyL(maxReceive, simulate), Integer.MAX_VALUE);
        }

        public int extractEnergy(int maxExtract, boolean simulate) {
            return (int) Math.min(extractEnergyL(maxExtract, simulate), Integer.MAX_VALUE);
        }

        public int getEnergyStored() {
            return (int) Math.min(getEnergyStoredL(), Integer.MAX_VALUE);
        }

        public int getMaxEnergyStored() {
            return (int) Math.min(getMaxEnergyStoredL(), Integer.MAX_VALUE);
        }
    }
}
