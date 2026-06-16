package sonar.fluxnetworks.common.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import sonar.fluxnetworks.api.FluxConstants;
import sonar.fluxnetworks.common.device.TileFluxDevice;
import sonar.fluxnetworks.register.RegistryItems;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;

/**
 * Defines the block base class for any flux device.
 */
@ParametersAreNonnullByDefault
public abstract class FluxDeviceBlock extends Block implements EntityBlock {

    public FluxDeviceBlock(Properties props) {
        super(props);
    }

    // CORREÇÃO: usar useItemOn em vez de use
    @Nonnull
    @Override
    public InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos,
                                       Player player, InteractionHand hand, BlockHitResult hit) {
        if (level.isClientSide()) {
            return InteractionResult.SUCCESS;
        }

        // Verificar se o item é o configurador
        if (stack.is(RegistryItems.FLUX_CONFIGURATOR)) {
            return InteractionResult.PASS;
        }

        if (level.getBlockEntity(pos) instanceof TileFluxDevice device) {
            device.onPlayerInteract(player);
        }

        return InteractionResult.CONSUME;
    }

    /**
     * Called by BlockItem after this block has been placed.
     */
    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity placer,
                            ItemStack stack) {
        super.setPlacedBy(level, pos, state, placer, stack);
        if (level.getBlockEntity(pos) instanceof TileFluxDevice device) {
            CustomData customData = stack.get(DataComponents.CUSTOM_DATA);
            if (customData != null) {
                CompoundTag tag = customData.copyTag();
                if (tag.contains(FluxConstants.TAG_FLUX_DATA)) {
                    CompoundTag fluxTag = tag.getCompound(FluxConstants.TAG_FLUX_DATA)
                            .orElse(new CompoundTag());
                    device.readCustomTag(fluxTag, FluxConstants.NBT_TILE_DROP);
                }
            }
            if (placer instanceof Player) {
                device.setOwnerUUID(placer.getUUID());
            }
        }
    }
}