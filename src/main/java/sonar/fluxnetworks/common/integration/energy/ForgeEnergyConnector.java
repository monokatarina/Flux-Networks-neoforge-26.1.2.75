package sonar.fluxnetworks.common.integration.energy;

import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.transfer.access.ItemAccess;
import net.neoforged.neoforge.transfer.energy.EnergyHandler;
import net.neoforged.neoforge.transfer.transaction.Transaction;
import sonar.fluxnetworks.api.energy.IBlockEnergyConnector;
import sonar.fluxnetworks.api.energy.IItemEnergyConnector;

import javax.annotation.Nonnull;

public class ForgeEnergyConnector implements IBlockEnergyConnector, IItemEnergyConnector {

    public static final ForgeEnergyConnector INSTANCE = new ForgeEnergyConnector();

    private ForgeEnergyConnector() {
    }

    private EnergyHandler getBlockHandler(BlockEntity target, Direction side) {
        if (target == null || target.isRemoved() || target.getLevel() == null) return null;
        return target.getLevel().getCapability(Capabilities.Energy.BLOCK, target.getBlockPos(), target.getBlockState(), target, side);
    }

    private EnergyHandler getItemHandler(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return null;
        return stack.getCapability(Capabilities.Energy.ITEM, ItemAccess.forStack(stack));
    }

    @Override
    public boolean hasCapability(@Nonnull BlockEntity target, @Nonnull Direction side) {
        return getBlockHandler(target, side) != null;
    }

    @Override
    public boolean canSendTo(@Nonnull BlockEntity target, @Nonnull Direction side) {
        return getBlockHandler(target, side) != null;
    }

    @Override
    public boolean canReceiveFrom(@Nonnull BlockEntity target, @Nonnull Direction side) {
        return getBlockHandler(target, side) != null;
    }

    @Override
    public long sendTo(long amount, @Nonnull BlockEntity target, @Nonnull Direction side, boolean simulate) {
        EnergyHandler handler = getBlockHandler(target, side);
        if (handler == null) return 0;

        try (Transaction transaction = Transaction.open(null)) {
            int inserted = handler.insert((int) Math.min(amount, Integer.MAX_VALUE), transaction);
            if (!simulate) {
                transaction.commit();
            }
            return inserted;
        }
    }

    @Override
    public long receiveFrom(long amount, @Nonnull BlockEntity target, @Nonnull Direction side, boolean simulate) {
        EnergyHandler handler = getBlockHandler(target, side);
        if (handler == null) return 0;

        try (Transaction transaction = Transaction.open(null)) {
            int extracted = handler.extract((int) Math.min(amount, Integer.MAX_VALUE), transaction);
            if (!simulate) {
                transaction.commit();
            }
            return extracted;
        }
    }

    @Override
    public boolean hasCapability(@Nonnull ItemStack stack) {
        return getItemHandler(stack) != null;
    }

    @Override
    public boolean canSendTo(@Nonnull ItemStack stack) {
        return getItemHandler(stack) != null;
    }

    @Override
    public boolean canReceiveFrom(@Nonnull ItemStack stack) {
        return getItemHandler(stack) != null;
    }

    @Override
    public long sendTo(long amount, @Nonnull ItemStack stack, boolean simulate) {
        EnergyHandler handler = getItemHandler(stack);
        if (handler == null) return 0;

        try (Transaction transaction = Transaction.open(null)) {
            int inserted = handler.insert((int) Math.min(amount, Integer.MAX_VALUE), transaction);
            if (!simulate) {
                transaction.commit();
            }
            return inserted;
        }
    }

    @Override
    public long receiveFrom(long amount, @Nonnull ItemStack stack, boolean simulate) {
        EnergyHandler handler = getItemHandler(stack);
        if (handler == null) return 0;

        try (Transaction transaction = Transaction.open(null)) {
            int extracted = handler.extract((int) Math.min(amount, Integer.MAX_VALUE), transaction);
            if (!simulate) {
                transaction.commit();
            }
            return extracted;
        }
    }
}
