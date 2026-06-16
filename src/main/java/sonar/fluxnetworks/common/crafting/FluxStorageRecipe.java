package sonar.fluxnetworks.common.crafting;

import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.PlacementInfo;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.ShapedRecipe;
import net.minecraft.world.level.Level;
import sonar.fluxnetworks.api.FluxConstants;
import sonar.fluxnetworks.register.RegistryRecipes;

import javax.annotation.Nonnull;
import java.util.List;
import java.util.Optional;

public class FluxStorageRecipe implements net.minecraft.world.item.crafting.CraftingRecipe {

    private final ShapedRecipe recipe;

    public FluxStorageRecipe(@Nonnull ShapedRecipe recipe) {
        this.recipe = recipe;
    }

    @Nonnull
    public ShapedRecipe shapedRecipe() {
        return recipe;
    }

    @Override
    public boolean matches(@Nonnull CraftingInput input, @Nonnull Level level) {
        return recipe.matches(input, level);
    }

    @Nonnull
    @Override
    public ItemStack assemble(@Nonnull CraftingInput input) {
        long totalEnergy = 0L;
        int networkID = FluxConstants.INVALID_NETWORK_ID;

        for (int i = 0; i < input.size(); i++) {
            CompoundTag subTag = getFluxData(input.getItem(i));
            if (subTag != null && !subTag.isEmpty()) {
                if (networkID == FluxConstants.INVALID_NETWORK_ID) {
                    networkID = subTag.getIntOr(FluxConstants.NETWORK_ID, FluxConstants.INVALID_NETWORK_ID);
                }
                totalEnergy += subTag.getLongOr(FluxConstants.ENERGY, 0L);
            }
        }

        ItemStack stack = recipe.assemble(input);
        if (totalEnergy > 0L || networkID != FluxConstants.INVALID_NETWORK_ID) {
            final long finalTotalEnergy = totalEnergy;
            final int finalNetworkID = networkID;
            CustomData.update(DataComponents.CUSTOM_DATA, stack, tag -> {
                CompoundTag subTag = tag.getCompoundOrEmpty(FluxConstants.TAG_FLUX_DATA);
                if (finalNetworkID != FluxConstants.INVALID_NETWORK_ID) {
                    subTag.putInt(FluxConstants.NETWORK_ID, finalNetworkID);
                }
                if (finalTotalEnergy > 0L) {
                    subTag.putLong(FluxConstants.ENERGY, finalTotalEnergy);
                }
                tag.put(FluxConstants.TAG_FLUX_DATA, subTag);
            });
        }
        return stack;
    }

    @Override
    public boolean isSpecial() {
        return recipe.isSpecial();
    }

    @Override
    public boolean showNotification() {
        return recipe.showNotification();
    }

    @Override
    public String group() {
        return recipe.group();
    }

    @Override
    public RecipeSerializer<? extends net.minecraft.world.item.crafting.CraftingRecipe> getSerializer() {
        return RegistryRecipes.FLUX_STORAGE_RECIPE;
    }

    @Override
    public PlacementInfo placementInfo() {
        return recipe.placementInfo();
    }

    @Override
    public List<net.minecraft.world.item.crafting.display.RecipeDisplay> display() {
        return recipe.display();
    }

    @Override
    public CraftingBookCategory category() {
        return recipe.category();
    }

    private static CompoundTag getFluxData(@Nonnull ItemStack stack) {
        CustomData customData = stack.get(DataComponents.CUSTOM_DATA);
        if (customData == null || customData.isEmpty()) {
            return null;
        }
        Optional<CompoundTag> tag = customData.copyTag().getCompound(FluxConstants.TAG_FLUX_DATA);
        return tag.orElse(null);
    }
}
