package sonar.fluxnetworks.common.item;

import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.block.Block;
import sonar.fluxnetworks.api.FluxConstants;
import sonar.fluxnetworks.api.FluxTranslate;
import sonar.fluxnetworks.api.energy.EnergyType;
import sonar.fluxnetworks.client.ClientCache;
import sonar.fluxnetworks.common.block.FluxStorageBlock;
import sonar.fluxnetworks.common.connection.FluxNetwork;

import javax.annotation.Nonnull;
import java.util.function.Consumer;

public class FluxDeviceItem extends BlockItem {

    public FluxDeviceItem(Block block, Properties props) {
        super(block, props);
    }

    @Nonnull
    @Override
    public Component getName(@Nonnull ItemStack stack) {
        CompoundTag tag = getFluxData(stack);
        if (!tag.isEmpty()) {
            String value = tag.getStringOr(FluxConstants.CUSTOM_NAME, "");
            if (!value.isEmpty()) {
                return Component.literal(value);
            }
        }
        return super.getName(stack);
    }

    @Override
    public void appendHoverText(@Nonnull ItemStack stack, @Nonnull Item.TooltipContext context,
                                @Nonnull TooltipDisplay display, @Nonnull Consumer<Component> tooltip,
                                @Nonnull TooltipFlag flag) {
        CompoundTag tag = getFluxData(stack);
        if (tag.isEmpty()) {
            super.appendHoverText(stack, context, display, tooltip, flag);
            return;
        }

        FluxNetwork network = ClientCache.getNetwork(tag.getIntOr(FluxConstants.NETWORK_ID, FluxConstants.INVALID_NETWORK_ID));
        if (network.isValid()) {
            tooltip.accept(Component.literal(ChatFormatting.BLUE + FluxTranslate.NETWORK_FULL_NAME.get() + ": " +
                    ChatFormatting.RESET + network.getNetworkName()));
        }

        if (tag.contains(FluxConstants.LIMIT)) {
            tooltip.accept(Component.literal(ChatFormatting.BLUE + FluxTranslate.TRANSFER_LIMIT.get() + ": " +
                    ChatFormatting.RESET + EnergyType.FE.getStorage(tag.getLongOr(FluxConstants.LIMIT, 0L))));
        }

        if (tag.contains(FluxConstants.PRIORITY)) {
            tooltip.accept(Component.literal(ChatFormatting.BLUE + FluxTranslate.PRIORITY.get() + ": " +
                    ChatFormatting.RESET + tag.getIntOr(FluxConstants.PRIORITY, 0)));
        }

        if (tag.contains(FluxConstants.BUFFER)) {
            tooltip.accept(Component.literal(ChatFormatting.BLUE + FluxTranslate.INTERNAL_BUFFER.get() + ": " +
                    ChatFormatting.RESET + EnergyType.FE.getStorage(tag.getLongOr(FluxConstants.BUFFER, 0L))));
        } else if (tag.contains(FluxConstants.ENERGY)) {
            long energy = tag.getLongOr(FluxConstants.ENERGY, 0L);
            Block block = getBlock();
            double percentage = block instanceof FluxStorageBlock storage
                    ? Math.min((double) energy / storage.getEnergyCapacity(), 1.0)
                    : 0.0;
            tooltip.accept(Component.literal(ChatFormatting.BLUE + FluxTranslate.ENERGY_STORED.get() + ": " +
                    ChatFormatting.RESET + EnergyType.FE.getStorage(energy) + String.format(" (%.1f%%)",
                    percentage * 100)));
        }
    }

    private static CompoundTag getFluxData(@Nonnull ItemStack stack) {
        CustomData customData = stack.get(DataComponents.CUSTOM_DATA);
        if (customData == null || customData.isEmpty()) {
            return new CompoundTag();
        }
        return customData.copyTag().getCompoundOrEmpty(FluxConstants.TAG_FLUX_DATA);
    }
}
