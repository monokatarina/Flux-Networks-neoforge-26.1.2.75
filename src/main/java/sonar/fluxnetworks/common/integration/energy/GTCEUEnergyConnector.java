package sonar.fluxnetworks.common.integration.energy;

import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import sonar.fluxnetworks.api.energy.IBlockEnergyConnector;
import sonar.fluxnetworks.api.energy.IItemEnergyConnector;

import javax.annotation.Nonnull;

public class GTCEUEnergyConnector implements IBlockEnergyConnector, IItemEnergyConnector {

    public static final GTCEUEnergyConnector INSTANCE = new GTCEUEnergyConnector();

    @Override
    public boolean hasCapability(@Nonnull BlockEntity target, @Nonnull Direction side) {
        return false;
    }

    @Override
    public boolean canSendTo(@Nonnull BlockEntity target, @Nonnull Direction side) {
        return false;
    }

    @Override
    public boolean canReceiveFrom(@Nonnull BlockEntity target, @Nonnull Direction side) {
        return false;
    }

    @Override
    public long sendTo(long amount, @Nonnull BlockEntity target, @Nonnull Direction side, boolean simulate) {
        return 0L;
    }

    @Override
    public long receiveFrom(long amount, @Nonnull BlockEntity target, @Nonnull Direction side, boolean simulate) {
        return 0L;
    }

    @Override
    public boolean hasCapability(@Nonnull ItemStack stack) {
        return false;
    }

    @Override
    public boolean canSendTo(@Nonnull ItemStack stack) {
        return false;
    }

    @Override
    public boolean canReceiveFrom(@Nonnull ItemStack stack) {
        return false;
    }

    @Override
    public long sendTo(long amount, @Nonnull ItemStack stack, boolean simulate) {
        return 0L;
    }

    @Override
    public long receiveFrom(long amount, @Nonnull ItemStack stack, boolean simulate) {
        return 0L;
    }
}
