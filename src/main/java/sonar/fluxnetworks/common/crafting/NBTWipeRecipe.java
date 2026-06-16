package sonar.fluxnetworks.common.crafting;

import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.PlacementInfo;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.ShapelessRecipe;
import net.minecraft.world.level.block.Block;
import sonar.fluxnetworks.api.FluxConstants;
import sonar.fluxnetworks.common.block.FluxStorageBlock;
import sonar.fluxnetworks.register.RegistryRecipes;

import javax.annotation.Nonnull;
import java.util.List;

public class NBTWipeRecipe implements net.minecraft.world.item.crafting.CraftingRecipe {

    private final ShapelessRecipe recipe;

    public NBTWipeRecipe(@Nonnull ShapelessRecipe recipe) {
        this.recipe = recipe;
    }

    @Nonnull
    public ShapelessRecipe shapelessRecipe() {
        return recipe;
    }

    @Override
    public boolean matches(@Nonnull CraftingInput input, @Nonnull net.minecraft.world.level.Level level) {
        return recipe.matches(input, level);
    }

    @Nonnull
    @Override
    public ItemStack assemble(@Nonnull CraftingInput input) {
        ItemStack originalStack = ItemStack.EMPTY;

        for (int i = 0; i < input.size(); i++) {
            ItemStack stack = input.getItem(i);
            if (!stack.isEmpty()) {
                originalStack = stack;
                break;
            }
        }
        if (!originalStack.isEmpty()) {
            ItemStack output = recipe.assemble(input);
            if (Block.byItem(output.getItem()) instanceof FluxStorageBlock) {
                long energy = getFluxData(originalStack).getLongOr(FluxConstants.ENERGY, 0L);
                if (energy != 0L) {
                    CustomData.update(DataComponents.CUSTOM_DATA, output, tag -> {
                        CompoundTag fluxData = tag.getCompoundOrEmpty(FluxConstants.TAG_FLUX_DATA);
                        fluxData.putLong(FluxConstants.ENERGY, energy);
                        tag.put(FluxConstants.TAG_FLUX_DATA, fluxData);
                    });
                }
            }
            return output;
        }
        return recipe.assemble(input);
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
        return RegistryRecipes.NBT_WIPE_RECIPE;
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
            return new CompoundTag();
        }
        return customData.copyTag().getCompoundOrEmpty(FluxConstants.TAG_FLUX_DATA);
    }
}
