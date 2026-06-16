package sonar.fluxnetworks.common.device;

import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import sonar.fluxnetworks.FluxNetworks;
import sonar.fluxnetworks.api.FluxCapabilities;
import sonar.fluxnetworks.register.RegistryBlockEntityTypes;

@EventBusSubscriber(modid = FluxNetworks.MODID)
public class FluxCapabilityRegistrar {

    @SubscribeEvent
    public static void registerCapabilities(RegisterCapabilitiesEvent event) {

        // Registrar para FluxCapabilities customizada (IFNEnergyStorage) - Plug
        // Como FN_ENERGY_STORAGE_BLOCK é do tipo Void, o side é null
        event.registerBlockEntity(
                FluxCapabilities.FN_ENERGY_STORAGE_BLOCK,
                RegistryBlockEntityTypes.FLUX_PLUG,
                (be, side) -> {
                    if (be instanceof TileFluxPlug plug) {
                        // side é Void (null), passa null como Direction
                        return plug.getFluxEnergyStorage(null);
                    }
                    return null;
                }
        );

        // Registrar para EnergyHandler (Forge Energy compatível) - Plug
        // Capabilities.Energy.BLOCK é do tipo Direction, então side é Direction
        event.registerBlockEntity(
                Capabilities.Energy.BLOCK,
                RegistryBlockEntityTypes.FLUX_PLUG,
                (be, side) -> {
                    if (be instanceof TileFluxPlug plug) {
                        // side é Direction, passa diretamente
                        return plug.getEnergyHandler(side);
                    }
                    return null;
                }
        );
    }
}