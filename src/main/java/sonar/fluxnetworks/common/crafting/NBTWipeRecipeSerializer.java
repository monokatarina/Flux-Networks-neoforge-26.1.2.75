package sonar.fluxnetworks.common.crafting;

import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.ShapelessRecipe;

public final class NBTWipeRecipeSerializer {

    public static final RecipeSerializer<NBTWipeRecipe> INSTANCE = new RecipeSerializer<>(
            ShapelessRecipe.MAP_CODEC.xmap(NBTWipeRecipe::new, NBTWipeRecipe::shapelessRecipe),
            ShapelessRecipe.STREAM_CODEC.map(NBTWipeRecipe::new, NBTWipeRecipe::shapelessRecipe)
    );

    private NBTWipeRecipeSerializer() {
    }
}
