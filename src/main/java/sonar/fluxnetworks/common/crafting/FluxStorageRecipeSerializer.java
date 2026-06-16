package sonar.fluxnetworks.common.crafting;

import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.ShapedRecipe;

public final class FluxStorageRecipeSerializer {

    public static final RecipeSerializer<FluxStorageRecipe> INSTANCE = new RecipeSerializer<>(
            ShapedRecipe.MAP_CODEC.xmap(FluxStorageRecipe::new, FluxStorageRecipe::shapedRecipe),
            ShapedRecipe.STREAM_CODEC.map(FluxStorageRecipe::new, FluxStorageRecipe::shapedRecipe)
    );

    private FluxStorageRecipeSerializer() {
    }
}
