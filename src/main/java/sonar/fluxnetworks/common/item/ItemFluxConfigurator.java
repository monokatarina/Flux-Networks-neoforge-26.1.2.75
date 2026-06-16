package sonar.fluxnetworks.common.item;

import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.item.context.UseOnContext;
import sonar.fluxnetworks.api.FluxConstants;
import sonar.fluxnetworks.api.FluxTranslate;
import sonar.fluxnetworks.api.device.IFluxProvider;
import sonar.fluxnetworks.api.energy.EnergyType;
import sonar.fluxnetworks.api.misc.FluxConfigurationType;
import sonar.fluxnetworks.client.ClientCache;
import sonar.fluxnetworks.common.connection.FluxMenu;
import sonar.fluxnetworks.common.connection.FluxNetwork;
import sonar.fluxnetworks.common.device.TileFluxDevice;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.function.Consumer;

public class ItemFluxConfigurator extends Item {

    public ItemFluxConfigurator(Properties props) {
        super(props);
    }

    @Override
    public InteractionResult onItemUseFirst(ItemStack stack, UseOnContext context) {
        if (context.getLevel().isClientSide()) {
            return InteractionResult.SUCCESS;
        }
        Player player = context.getPlayer();
        if (player == null) {
            return InteractionResult.PASS;
        }
        if (context.getLevel().getBlockEntity(context.getClickedPos()) instanceof TileFluxDevice device) {
            if (!device.canPlayerAccess(player)) {
                player.sendSystemMessage(FluxTranslate.ACCESS_DENIED);
                return InteractionResult.FAIL;
            }
            if (player.isShiftKeyDown()) {
                CustomData.update(DataComponents.CUSTOM_DATA, stack, root -> {
                    CompoundTag tag = root.getCompoundOrEmpty(FluxConstants.TAG_FLUX_CONFIG);
                    for (FluxConfigurationType type : FluxConfigurationType.VALUES) {
                        type.copy(player, tag, device);
                    }
                    root.put(FluxConstants.TAG_FLUX_CONFIG, tag);
                });
                player.sendSystemMessage(FluxTranslate.CONFIG_COPIED);
            } else {
                CompoundTag tag = getConfigTag(stack);
                if (!tag.isEmpty()) {
                    for (FluxConfigurationType type : FluxConfigurationType.VALUES) {
                        type.paste(player, tag, device);
                    }
                    player.sendSystemMessage(FluxTranslate.CONFIG_PASTED);
                }
            }
            return InteractionResult.SUCCESS;
        }
        return InteractionResult.SUCCESS;
    }

    @Nonnull
    @Override
    public InteractionResult use(@Nonnull net.minecraft.world.level.Level level, @Nonnull Player player,
                                 @Nonnull InteractionHand hand) {
        return InteractionResult.SUCCESS;
    }

    @Override
    public void appendHoverText(@Nonnull ItemStack stack, @Nonnull Item.TooltipContext context,
                                @Nonnull TooltipDisplay display, @Nonnull Consumer<Component> tooltip,
                                @Nonnull TooltipFlag flag) {
        CompoundTag tag = getConfigTag(stack);
        if (tag.isEmpty()) {
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
    }

    public static class Provider implements IFluxProvider {

        public final ItemStack mStack;

        public Provider(@Nonnull ItemStack stack) {
            mStack = stack;
        }

        @Override
        public int getNetworkID() {
            return getConfigTag(mStack).getIntOr(FluxConstants.NETWORK_ID, FluxConstants.INVALID_NETWORK_ID);
        }

        @Override
        public void onPlayerOpened(@Nonnull Player player) {
        }

        @Override
        public void onPlayerClosed(@Nonnull Player player) {
        }

        @Nullable
        @Override
        public FluxMenu createMenu(int containerId, @Nonnull Inventory inventory, @Nonnull Player player) {
            return new FluxMenu(containerId, inventory, this);
        }
    }

    private static CompoundTag getConfigTag(@Nonnull ItemStack stack) {
        CustomData customData = stack.get(DataComponents.CUSTOM_DATA);
        if (customData == null || customData.isEmpty()) {
            return new CompoundTag();
        }
        return customData.copyTag().getCompoundOrEmpty(FluxConstants.TAG_FLUX_CONFIG);
    }
}
