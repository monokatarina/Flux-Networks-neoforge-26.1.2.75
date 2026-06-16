package sonar.fluxnetworks.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.block.Block;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import sonar.fluxnetworks.api.FluxConstants;
import sonar.fluxnetworks.client.ClientCache;
import sonar.fluxnetworks.client.gui.basic.GuiFluxCore;
import sonar.fluxnetworks.common.block.FluxStorageBlock;

import javax.annotation.Nonnull;

@OnlyIn(Dist.CLIENT)
public class FluxStorageItemRenderer {

    @SuppressWarnings({"unused", "UnusedParameters"})
    public static void renderByItem(@Nonnull ItemStack stack, @Nonnull ItemDisplayContext transformType,
                                    @Nonnull PoseStack poseStack, @Nonnull MultiBufferSource bufferSource,
                                    int packedLight, int packedOverlay) {
        int color;
        long energy;

        CompoundTag rootTag = getCustomData(stack);
        if (rootTag != null && !rootTag.isEmpty()) {
            if (rootTag.getBooleanOr(FluxConstants.FLUX_COLOR, false)) {
                Screen screen = Minecraft.getInstance().screen;
                if (screen instanceof GuiFluxCore gui) {
                    color = gui.getNetwork().getNetworkColor();
                } else {
                    color = FluxConstants.INVALID_NETWORK_COLOR;
                }
                energy = rootTag.getCompoundOrEmpty(FluxConstants.TAG_FLUX_DATA)
                        .getLongOr(FluxConstants.ENERGY, 0L);
            } else {
                CompoundTag tag = rootTag.getCompoundOrEmpty(FluxConstants.TAG_FLUX_DATA);
                if (!tag.isEmpty()) {
                    if (tag.contains(FluxConstants.CLIENT_COLOR)) {
                        color = tag.getIntOr(FluxConstants.CLIENT_COLOR, FluxConstants.INVALID_NETWORK_COLOR);
                    } else {
                        int networkId = tag.getIntOr(FluxConstants.NETWORK_ID, FluxConstants.INVALID_NETWORK_ID);
                        color = ClientCache.getNetwork(networkId).getNetworkColor();
                    }
                    energy = tag.getLongOr(FluxConstants.ENERGY, 0L);
                } else {
                    color = FluxConstants.INVALID_NETWORK_COLOR;
                    energy = 0L;
                }
            }
        } else {
            color = FluxConstants.INVALID_NETWORK_COLOR;
            energy = 0L;
        }

        FluxStorageBlock block = (FluxStorageBlock) Block.byItem(stack.getItem());
        FluxStorageEntityRenderer.render(poseStack, bufferSource.getBuffer(FluxStorageRenderType.getType()),
                color, energy, block.getEnergyCapacity());
    }

    private static CompoundTag getCustomData(@Nonnull ItemStack stack) {
        CustomData customData = stack.get(DataComponents.CUSTOM_DATA);
        return customData == null ? null : customData.copyTag();
    }
}
