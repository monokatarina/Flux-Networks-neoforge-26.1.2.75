package sonar.fluxnetworks.register;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import sonar.fluxnetworks.FluxNetworks;
import sonar.fluxnetworks.client.ClientCache;

@EventBusSubscriber(modid = FluxNetworks.MODID, value = Dist.CLIENT)
public class ClientEventHandler {

    @SubscribeEvent
    public static void onPlayerLoggedOut(ClientPlayerNetworkEvent.LoggingOut event) {
        ClientCache.release();
    }
}