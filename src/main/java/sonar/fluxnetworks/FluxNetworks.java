package sonar.fluxnetworks;

import net.minecraft.resources.Identifier;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.ModList;
import net.neoforged.fml.common.Mod;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import sonar.fluxnetworks.common.capability.FluxPlayer;

import javax.annotation.Nonnull;

@Mod(FluxNetworks.MODID)
public class FluxNetworks {

    public static final String MODID = "fluxnetworks";
    public static final String NAME = "Flux Networks";
    public static final String NAME_CPT = "FluxNetworks";

    public static final Logger LOGGER = LogManager.getLogger(NAME_CPT);

    private static boolean sCuriosLoaded;
    private static boolean sModernUILoaded;

    public FluxNetworks(ModContainer modContainer) {
        sCuriosLoaded = ModList.get().isLoaded("curios");
        sModernUILoaded = ModList.get().isLoaded("modernui");

        FluxConfig.init(modContainer);

        // CORRIGIDO: registrar o DeferredRegister no event bus do mod
        FluxPlayer.ATTACHMENT_TYPES.register(modContainer.getEventBus());
    }

    public static boolean isCuriosLoaded() {
        return sCuriosLoaded;
    }

    public static boolean isModernUILoaded() {
        return sModernUILoaded;
    }

    @Nonnull
    public static Identifier location(String path) {
        return Identifier.fromNamespaceAndPath(MODID, path);
    }
}