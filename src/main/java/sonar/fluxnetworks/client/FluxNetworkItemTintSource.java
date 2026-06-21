package sonar.fluxnetworks.client;

import com.mojang.serialization.MapCodec;
import net.minecraft.client.Minecraft;
import net.minecraft.client.color.item.ItemTintSource;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.ARGB;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import org.jspecify.annotations.Nullable;
import sonar.fluxnetworks.api.FluxConstants;
import sonar.fluxnetworks.client.gui.basic.GuiFluxCore;

public record FluxNetworkItemTintSource() implements ItemTintSource {

    public static final FluxNetworkItemTintSource INSTANCE = new FluxNetworkItemTintSource();
    public static final MapCodec<FluxNetworkItemTintSource> MAP_CODEC = MapCodec.unit(INSTANCE);

    @Override
    public int calculate(ItemStack itemStack, @Nullable ClientLevel level, @Nullable LivingEntity owner) {
        return ARGB.opaque(getItemColor(itemStack));
    }

    @Override
    public MapCodec<FluxNetworkItemTintSource> type() {
        return MAP_CODEC;
    }

    private static int getItemColor(ItemStack stack) {
        var customData = stack.get(DataComponents.CUSTOM_DATA);
        if (customData != null && !customData.isEmpty()) {
            CompoundTag tag = customData.copyTag();

            if (tag.getBooleanOr(FluxConstants.FLUX_COLOR, false)) {
                var screen = Minecraft.getInstance().screen;
                if (screen instanceof GuiFluxCore gui && gui.getNetwork() != null) {
                    return gui.getNetwork().getNetworkColor();
                }
            }

            CompoundTag fluxData = tag.getCompoundOrEmpty(FluxConstants.TAG_FLUX_DATA);
            if (fluxData.contains(FluxConstants.NETWORK_ID)) {
                int networkId = fluxData.getIntOr(FluxConstants.NETWORK_ID, FluxConstants.INVALID_NETWORK_ID);
                var network = ClientCache.getNetwork(networkId);
                if (network != null) {
                    return network.getNetworkColor();
                }
            }

            CompoundTag fluxConfig = tag.getCompoundOrEmpty(FluxConstants.TAG_FLUX_CONFIG);
            if (fluxConfig.contains(FluxConstants.NETWORK_ID)) {
                int networkId = fluxConfig.getIntOr(FluxConstants.NETWORK_ID, FluxConstants.INVALID_NETWORK_ID);
                var network = ClientCache.getNetwork(networkId);
                if (network != null) {
                    return network.getNetworkColor();
                }
            }
        }

        return FluxConstants.INVALID_NETWORK_COLOR;
    }
}
