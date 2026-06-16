package sonar.fluxnetworks.client;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.api.distmarker.Dist;
import org.jspecify.annotations.Nullable;
import sonar.fluxnetworks.api.FluxConstants;
import sonar.fluxnetworks.client.gui.basic.GuiFluxCore;
import sonar.fluxnetworks.common.device.TileFluxDevice;

/**
 * Render network color on blocks and items.
 */
public class FluxColorHandler {

    public static final FluxColorHandler INSTANCE = new FluxColorHandler();

    /**
     * Get color for block (used in RegisterColorHandlersEvent.Block)
     * Esta parte não mudou, pois não envolve o sistema de Data Components.
     */
    public int getBlockColor(BlockState state, @Nullable BlockGetter world, @Nullable BlockPos pos, int tintIndex) {
        if (tintIndex == 1 && pos != null && world != null) {
            BlockEntity tile = world.getBlockEntity(pos);
            if (tile instanceof TileFluxDevice device) {
                return device.mClientColor;
            }
        }
        return -1;
    }

    /**
     * Get color for item (used in RegisterColorHandlersEvent.Item)
     * Versão corrigida para usar Data Components.
     */
    public int getItemColor(ItemStack stack, int tintIndex) {
        if (tintIndex != 1) {
            return -1;
        }

        // 1. Obtém o componente CUSTOM_DATA. Ele nunca é 'null', mas pode estar "vazio".
        var customData = stack.get(DataComponents.CUSTOM_DATA);

        // 2. Verifica se o componente existe e contém dados.
        //    OBS: O método correto é `customData != null && !customData.isEmpty()`.
        //    O `isEmpty()` verifica se o NBT interno está vazio.
        if (customData != null && !customData.isEmpty()) {
            // 3. Obtém o CompoundTag subjacente.
            //    O método 'copyTag()' pode ter nomes diferentes dependendo da versão.
            //    Experimente 'nbt()' ou 'getNbt()' se 'copyTag()' não existir.
            var tag = customData.copyTag(); // OU customData.nbt() / customData.getNbt()

            // Lógica para cor da GUI
            if (tag.getBooleanOr(FluxConstants.FLUX_COLOR, false)) {
                var screen = Minecraft.getInstance().screen;
                if (screen instanceof GuiFluxCore gui && gui.getNetwork() != null) {
                    return gui.getNetwork().getNetworkColor();
                }
            }

            // Lógica para cor via TAG_FLUX_DATA
            if (tag.contains(FluxConstants.TAG_FLUX_DATA)) {
                var fluxTag = tag.getCompoundOrEmpty(FluxConstants.TAG_FLUX_DATA);
                if (fluxTag.contains(FluxConstants.NETWORK_ID)) {
                    int networkId = fluxTag.getIntOr(FluxConstants.NETWORK_ID, FluxConstants.INVALID_NETWORK_ID);
                    var network = ClientCache.getNetwork(networkId);
                    if (network != null) {
                        return network.getNetworkColor();
                    }
                }
            }
        }

        // Se nenhuma cor for encontrada, retorna a cor padrão para "sem rede"
        return FluxConstants.INVALID_NETWORK_COLOR;
    }

    /**
     * Special color multiplier for configurator items
     */
    public static int colorMultiplierForConfigurator(ItemStack stack, int tintIndex) {
        if (tintIndex != 1) {
            return -1;
        }

        var customData = stack.get(DataComponents.CUSTOM_DATA);
        if (customData != null && !customData.isEmpty()) {
            var tag = customData.copyTag(); // OU customData.nbt() / customData.getNbt()

            if (tag.contains(FluxConstants.TAG_FLUX_CONFIG)) {
                var configTag = tag.getCompoundOrEmpty(FluxConstants.TAG_FLUX_CONFIG);
                if (configTag.contains(FluxConstants.NETWORK_ID)) {
                    int networkId = configTag.getIntOr(FluxConstants.NETWORK_ID, FluxConstants.INVALID_NETWORK_ID);
                    var network = ClientCache.getNetwork(networkId);
                    if (network != null) {
                        return network.getNetworkColor();
                    }
                }
            }
        }

        return FluxConstants.INVALID_NETWORK_COLOR;
    }
}
