package sonar.fluxnetworks.common.device;

import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import sonar.fluxnetworks.api.FluxCapabilities;
import sonar.fluxnetworks.register.RegistryBlockEntityTypes;

public class FluxCapabilityRegistrar {

    public static void registerCapabilities(RegisterCapabilitiesEvent event) {
        event.registerBlockEntity(
                FluxCapabilities.FN_ENERGY_STORAGE_BLOCK,
                RegistryBlockEntityTypes.FLUX_PLUG,
                (plug, context) -> plug.getFluxEnergyStorage(null)
        );

        event.registerBlockEntity(
                Capabilities.Energy.BLOCK,
                RegistryBlockEntityTypes.FLUX_PLUG,
                TileFluxPlug::getEnergyHandler
        );

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
}
