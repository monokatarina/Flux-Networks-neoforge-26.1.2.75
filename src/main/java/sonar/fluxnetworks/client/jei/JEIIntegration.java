package sonar.fluxnetworks.client.jei;

import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.registration.*;
import net.minecraft.resources.Identifier;
import sonar.fluxnetworks.FluxConfig;
import sonar.fluxnetworks.FluxNetworks;

import javax.annotation.Nonnull;

@JeiPlugin
public class JEIIntegration implements IModPlugin {

    public static final Identifier UID = FluxNetworks.location("jei");

    @Nonnull
    @Override
    public Identifier getPluginUid() {
        return UID;
    }

    @Override
    public void registerCategories(@Nonnull IRecipeCategoryRegistration registration) {
        if (FluxConfig.enableFluxRecipe) {
            registration.addRecipeCategories(new CreatingFluxRecipeCategory(registration.getJeiHelpers().getGuiHelper()));
        }
    }

    @Override
    public void registerRecipes(@Nonnull IRecipeRegistration registration) {
        if (FluxConfig.enableFluxRecipe) {
            registration.addRecipes(CreatingFluxRecipeCategory.RECIPE_TYPE, CreatingFluxRecipeCategory.getRecipes());
        }
    }

    @Override
    public void registerRecipeCatalysts(@Nonnull IRecipeCatalystRegistration registration) {
        if (FluxConfig.enableFluxRecipe) {
            // MUDANÇA: addRecipeCatalyst -> addCraftingStation
            CreatingFluxRecipeCategory.getCatalysts().forEach(itemStack ->
                    registration.addCraftingStation(CreatingFluxRecipeCategory.RECIPE_TYPE, itemStack));
        }
    }
}