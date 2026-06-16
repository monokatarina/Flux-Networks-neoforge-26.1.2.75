package sonar.fluxnetworks.client.render;

import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.resources.Identifier;
import sonar.fluxnetworks.FluxNetworks;

import javax.annotation.Nonnull;

/**
 * Render energy sides.
 */
public class FluxStorageRenderType {

    private static final Identifier ENERGY_TEXTURE = FluxNetworks.location(
            "textures/block/flux_storage_energy.png");

    private static final RenderType INSTANCE;

    static {
        // Usar entityTranslucent do RenderTypes (existe!)
        INSTANCE = RenderTypes.entityTranslucent(ENERGY_TEXTURE);
    }

    @Nonnull
    public static RenderType getType() {
        return INSTANCE;
    }
}