package sonar.fluxnetworks.common.integration.energy;

import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import sonar.fluxnetworks.api.FluxCapabilities;
import sonar.fluxnetworks.api.energy.*;
import javax.annotation.Nonnull;

public class FNEnergyConnector implements IBlockEnergyConnector, IItemEnergyConnector {

    public static final FNEnergyConnector INSTANCE = new FNEnergyConnector();

    private FNEnergyConnector() {
    }

    private IFNEnergyStorage getBlockStorage(BlockEntity target, Direction side) {
        if (target == null || target.isRemoved() || target.getLevel() == null) return null;
        // NOTA: FN_ENERGY_STORAGE_BLOCK é do tipo Void, então passamos null como contexto
        return target.getLevel().getCapability(
                FluxCapabilities.FN_ENERGY_STORAGE_BLOCK,
                target.getBlockPos(),
                target.getBlockState(),
                target,
                null  // Void context - não usa Direction
        );
    }

    private IFNEnergyStorage getItemStorage(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return null;
        return stack.getCapability(FluxCapabilities.FN_ENERGY_STORAGE_ITEM);
    }

    @Override
    public boolean hasCapability(@Nonnull BlockEntity target, @Nonnull Direction side) {
        return getBlockStorage(target, side) != null;
    }

    @Override
    public boolean canSendTo(@Nonnull BlockEntity target, @Nonnull Direction side) {
        IFNEnergyStorage storage = getBlockStorage(target, side);
        return storage != null && storage.canReceive();
    }

    @Override
    public boolean canReceiveFrom(@Nonnull BlockEntity target, @Nonnull Direction side) {
        IFNEnergyStorage storage = getBlockStorage(target, side);
        return storage != null && storage.canExtract();
    }

    @Override
    public long sendTo(long amount, @Nonnull BlockEntity target, @Nonnull Direction side, boolean simulate) {
        IFNEnergyStorage storage = getBlockStorage(target, side);
        if (storage == null) return 0;
        return storage.receiveEnergyL(amount, simulate);
    }

    @Override
    public long receiveFrom(long amount, @Nonnull BlockEntity target, @Nonnull Direction side, boolean simulate) {
        IFNEnergyStorage storage = getBlockStorage(target, side);
        if (storage == null) return 0;
        return storage.extractEnergyL(amount, simulate);
    }

    @Override
    public boolean hasCapability(@Nonnull ItemStack stack) {
        return getItemStorage(stack) != null;
    }

    @Override
    public boolean canSendTo(@Nonnull ItemStack stack) {
        IFNEnergyStorage storage = getItemStorage(stack);
        return storage != null && storage.canReceive();
    }

    @Override
    public boolean canReceiveFrom(@Nonnull ItemStack stack) {
        IFNEnergyStorage storage = getItemStorage(stack);
        return storage != null && storage.canExtract();
    }

    @Override
    public long sendTo(long amount, @Nonnull ItemStack stack, boolean simulate) {
        IFNEnergyStorage storage = getItemStorage(stack);
        if (storage == null) return 0;
        return storage.receiveEnergyL(amount, simulate);
    }

    @Override
    public long receiveFrom(long amount, @Nonnull ItemStack stack, boolean simulate) {
        IFNEnergyStorage storage = getItemStorage(stack);
        if (storage == null) return 0;
        return storage.extractEnergyL(amount, simulate);
    }
}