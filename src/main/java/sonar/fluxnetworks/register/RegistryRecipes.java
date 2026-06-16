package sonar.fluxnetworks.register;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.neoforged.neoforge.registries.RegisterEvent;
import sonar.fluxnetworks.FluxNetworks;
import sonar.fluxnetworks.common.crafting.FluxStorageRecipe;
import sonar.fluxnetworks.common.crafting.FluxStorageRecipeSerializer;
import sonar.fluxnetworks.common.crafting.NBTWipeRecipe;
import sonar.fluxnetworks.common.crafting.NBTWipeRecipeSerializer;

public class RegistryRecipes {
    public static final Identifier FLUX_STORAGE_RECIPE_KEY = Identifier.fromNamespaceAndPath(FluxNetworks.MODID, "flux_storage_recipe");
    public static final Identifier NBT_WIPE_RECIPE_KEY = Identifier.fromNamespaceAndPath(FluxNetworks.MODID, "nbt_wipe_recipe");

    public static RecipeSerializer<FluxStorageRecipe> FLUX_STORAGE_RECIPE;
    public static RecipeSerializer<NBTWipeRecipe> NBT_WIPE_RECIPE;

    static void register(RegisterEvent event) {
        event.register(Registries.RECIPE_SERIALIZER, helper -> {
            FLUX_STORAGE_RECIPE = FluxStorageRecipeSerializer.INSTANCE;
            helper.register(FLUX_STORAGE_RECIPE_KEY, FluxStorageRecipeSerializer.INSTANCE);

            NBT_WIPE_RECIPE = NBTWipeRecipeSerializer.INSTANCE;
            helper.register(NBT_WIPE_RECIPE_KEY, NBTWipeRecipeSerializer.INSTANCE);
        });
    }

    private RegistryRecipes() {}
}