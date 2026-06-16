package sonar.fluxnetworks.api;

import net.minecraft.resources.Identifier;
import net.neoforged.neoforge.capabilities.BlockCapability;
import net.neoforged.neoforge.capabilities.ItemCapability;
import sonar.fluxnetworks.FluxNetworks;
import sonar.fluxnetworks.api.energy.IFNEnergyStorage;

public final class FluxCapabilities {

    /**
     * Capability for {@link IFNEnergyStorage} attached to blocks (TileEntities).
     * Functions the same as Forge Energy but allows Long.MAX_VALUE.
     * <p>
     * Use with {@link net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent#registerBlock}
     * and {@link net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent#registerBlockEntity}
     */
    public static final BlockCapability<IFNEnergyStorage, Void> FN_ENERGY_STORAGE_BLOCK =
            BlockCapability.createVoid(
                    Identifier.fromNamespaceAndPath(FluxNetworks.MODID, "fn_energy_storage"),
                    IFNEnergyStorage.class
            );

    /**
     * Capability for {@link IFNEnergyStorage} attached to items.
     * <p>
     * Use with {@link net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent#registerItem}
     */
    public static final ItemCapability<IFNEnergyStorage, Void> FN_ENERGY_STORAGE_ITEM =
            ItemCapability.createVoid(
                    Identifier.fromNamespaceAndPath(FluxNetworks.MODID, "fn_energy_storage"),
                    IFNEnergyStorage.class
            );

    private FluxCapabilities() {}
}