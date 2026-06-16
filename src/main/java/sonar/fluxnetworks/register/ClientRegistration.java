package sonar.fluxnetworks.register;

import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;
import sonar.fluxnetworks.FluxNetworks;
import sonar.fluxnetworks.client.gui.GuiFluxAdminHome;
import sonar.fluxnetworks.client.gui.GuiFluxDeviceHome;
import sonar.fluxnetworks.client.render.FluxStorageEntityRenderer;
import sonar.fluxnetworks.common.connection.FluxMenu;
import sonar.fluxnetworks.common.device.TileFluxDevice;
import sonar.fluxnetworks.common.integration.MUIIntegration;

import javax.annotation.Nonnull;

@EventBusSubscriber(modid = FluxNetworks.MODID)
public class ClientRegistration {

    @SubscribeEvent
    public static void setup(FMLClientSetupEvent event) {
        event.enqueueWork(() -> {
        });
    }

    @SubscribeEvent
    public static void registerMenuScreens(RegisterMenuScreensEvent event) {
        var factory = getScreenFactory();
        if (FluxNetworks.isModernUILoaded()) {
            factory = MUIIntegration.upgradeScreenFactory(factory);
        }
        event.register(RegistryMenuTypes.FLUX_MENU, factory);
    }

    @Nonnull
    private static net.minecraft.client.gui.screens.MenuScreens.ScreenConstructor<FluxMenu, AbstractContainerScreen<FluxMenu>> getScreenFactory() {
        return (menu, inventory, title) -> {
            if (menu.mProvider instanceof TileFluxDevice) {
                return new GuiFluxDeviceHome(menu, inventory.player);
            }
            return new GuiFluxAdminHome(menu, inventory.player);
        };
    }

    @SubscribeEvent
    public static void registerEntityRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerBlockEntityRenderer(RegistryBlockEntityTypes.BASIC_FLUX_STORAGE, FluxStorageEntityRenderer.PROVIDER);
        event.registerBlockEntityRenderer(RegistryBlockEntityTypes.HERCULEAN_FLUX_STORAGE, FluxStorageEntityRenderer.PROVIDER);
        event.registerBlockEntityRenderer(RegistryBlockEntityTypes.GARGANTUAN_FLUX_STORAGE, FluxStorageEntityRenderer.PROVIDER);
    }

}
