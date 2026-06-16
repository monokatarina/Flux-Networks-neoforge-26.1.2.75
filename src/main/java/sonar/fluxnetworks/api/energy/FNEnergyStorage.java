package sonar.fluxnetworks.api.energy;

import net.minecraft.util.Mth;
import sonar.fluxnetworks.api.FluxCapabilities;

/**
 * Reference implementation of {@link IFNEnergyStorage} that allows Long.MAX_VALUE.
 * Use the cap in {@link FluxCapabilities} to add support to your blocks and items.
 *
 * <p>For NeoForge's modern energy system, this class can be adapted to work with
 * {@link net.neoforged.neoforge.transfer.energy.EnergyHandler} and
 * {@link net.neoforged.neoforge.transfer.energy.SimpleEnergyHandler}.
 */
public class FNEnergyStorage implements IFNEnergyStorage {

    protected long energy;
    protected long capacity;
    protected long maxReceive;
    protected long maxExtract;

    public FNEnergyStorage(long capacity) {
        this(capacity, capacity, capacity, 0);
    }

    public FNEnergyStorage(long capacity, long maxTransfer) {
        this(capacity, maxTransfer, maxTransfer, 0);
    }

    public FNEnergyStorage(long capacity, long maxReceive, long maxExtract) {
        this(capacity, maxReceive, maxExtract, 0);
    }

    public FNEnergyStorage(long capacity, long maxReceive, long maxExtract, long energy) {
        this.capacity = capacity;
        this.maxReceive = maxReceive;
        this.maxExtract = maxExtract;
        this.energy = Mth.clamp(energy, 0, capacity);
    }

    @Override
    public long receiveEnergyL(long maxReceive, boolean simulate) {
        if (!canReceive() || maxReceive <= 0) {
            return 0;
        }

        long energyReceived = Math.min(capacity - energy, Math.min(this.maxReceive, maxReceive));
        if (!simulate) {
            energy += energyReceived;
        }
        return energyReceived;
    }

    @Override
    public long extractEnergyL(long maxExtract, boolean simulate) {
        if (!canExtract() || maxExtract <= 0) {
            return 0;
        }

        long energyExtracted = Math.min(energy, Math.min(this.maxExtract, maxExtract));
        if (!simulate) {
            energy -= energyExtracted;
        }
        return energyExtracted;
    }

    @Override
    public long getEnergyStoredL() {
        return energy;
    }

    @Override
    public long getMaxEnergyStoredL() {
        return capacity;
    }

    @Override
    public boolean canExtract() {
        return this.maxExtract > 0;
    }

    @Override
    public boolean canReceive() {
        return this.maxReceive > 0;
    }

    /**
     * Sets the current energy stored.
     *
     * @param energy New energy amount
     */
    public void setEnergyStored(long energy) {
        this.energy = Mth.clamp(energy, 0, capacity);
    }

    /**
     * Adds energy to the storage without checking receive limits.
     *
     * @param energy Amount to add
     */
    public void addEnergy(long energy) {
        this.energy = Mth.clamp(this.energy + energy, 0, capacity);
    }

    /**
     * Removes energy from the storage without checking extract limits.
     *
     * @param energy Amount to remove
     */
    public void removeEnergy(long energy) {
        this.energy = Mth.clamp(this.energy - energy, 0, capacity);
    }

    /**
     * Creates a copy of this energy storage.
     */
    public FNEnergyStorage copy() {
        return new FNEnergyStorage(capacity, maxReceive, maxExtract, energy);
    }

    @Override
    public String toString() {
        return String.format("FNEnergyStorage [%d/%d, maxReceive=%d, maxExtract=%d]",
                energy, capacity, maxReceive, maxExtract);
    }
}