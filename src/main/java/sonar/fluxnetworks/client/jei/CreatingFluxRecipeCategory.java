package sonar.fluxnetworks.client.jei;

import mezz.jei.api.constants.VanillaTypes;
import mezz.jei.api.gui.ITickTimer;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.category.IRecipeCategory;
import mezz.jei.api.recipe.types.IRecipeType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.Blocks;
import sonar.fluxnetworks.FluxNetworks;
import sonar.fluxnetworks.api.FluxTranslate;
import sonar.fluxnetworks.register.RegistryBlocks;
import sonar.fluxnetworks.register.RegistryItems;
import mezz.jei.api.gui.builder.ITooltipBuilder;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.*;

public class CreatingFluxRecipeCategory implements IRecipeCategory<CreatingFluxRecipe> {

    // Textura do background da receita
    public static final Identifier TEXTURES = FluxNetworks.location(
            "textures/gui/gui_creating_flux_recipe.png");

    public static final Identifier UID = FluxNetworks.location("creating_flux");
    public static final IRecipeType<CreatingFluxRecipe> RECIPE_TYPE =
            IRecipeType.create(UID, CreatingFluxRecipe.class);

    private final IDrawable background;
    private final IDrawable icon;
    private final ITickTimer timer;

    // Texturas para os blocos (usando sprites 2D em vez de renderização 3D)
    private final IDrawable bedrockIcon;
    private final IDrawable obsidianIcon;
    private final IDrawable fluxBlockIcon;
    private final IDrawable redstoneIcon;
    private final IDrawable fluxDustIcon;

    public CreatingFluxRecipeCategory(@Nonnull IGuiHelper guiHelper) {
        // Background: posição (0, -20), tamanho 128x80
        this.background = guiHelper.createDrawable(TEXTURES, 0, -20, 128, 80);
        this.icon = guiHelper.createDrawableIngredient(VanillaTypes.ITEM_STACK,
                new ItemStack(RegistryItems.FLUX_DUST));
        this.timer = guiHelper.createTickTimer(60, 320, false);

        // Criar ícones 2D para os blocos e itens
        this.bedrockIcon = guiHelper.createDrawableIngredient(VanillaTypes.ITEM_STACK, new ItemStack(Blocks.BEDROCK));
        this.obsidianIcon = guiHelper.createDrawableIngredient(VanillaTypes.ITEM_STACK, new ItemStack(Blocks.OBSIDIAN));
        this.fluxBlockIcon = guiHelper.createDrawableIngredient(VanillaTypes.ITEM_STACK, new ItemStack(RegistryBlocks.FLUX_BLOCK));
        this.redstoneIcon = guiHelper.createDrawableIngredient(VanillaTypes.ITEM_STACK, new ItemStack(Items.REDSTONE));
        this.fluxDustIcon = guiHelper.createDrawableIngredient(VanillaTypes.ITEM_STACK, new ItemStack(RegistryItems.FLUX_DUST));
    }

    @Nonnull
    public static List<CreatingFluxRecipe> getRecipes() {
        List<CreatingFluxRecipe> recipes = new ArrayList<>();
        recipes.add(new CreatingFluxRecipe(Blocks.BEDROCK, Blocks.OBSIDIAN,
                new ItemStack(Items.REDSTONE), new ItemStack(RegistryItems.FLUX_DUST)));
        recipes.add(new CreatingFluxRecipe(RegistryBlocks.FLUX_BLOCK, Blocks.OBSIDIAN,
                new ItemStack(Items.REDSTONE), new ItemStack(RegistryItems.FLUX_DUST)));
        return recipes;
    }

    @Nonnull
    public static List<ItemStack> getCatalysts() {
        return List.of(new ItemStack(RegistryItems.FLUX_DUST));
    }

    @Nonnull
    @Override
    public IRecipeType<CreatingFluxRecipe> getRecipeType() {
        return RECIPE_TYPE;
    }

    @Nonnull
    @Override
    public Component getTitle() {
        return FluxTranslate.JEI_CREATING_FLUX.getComponent();
    }

    @Override
    public int getWidth() {
        return 128;
    }

    @Override
    public int getHeight() {
        return 80;
    }

    @Nullable
    @Override
    public IDrawable getIcon() {
        return icon;
    }

    @Override
    public void setRecipe(@Nonnull IRecipeLayoutBuilder builder, @Nonnull CreatingFluxRecipe recipe,
                          @Nonnull IFocusGroup focuses) {
        // Slot de input (ingrediente) - posição (8, 24)
        builder.addSlot(RecipeIngredientRole.INPUT, 8, 24)
                .add(recipe.input());  // MUDANÇA: addItemStack -> add
        // Slot de output (resultado) - posição (102, 24)
        builder.addSlot(RecipeIngredientRole.OUTPUT, 102, 24)
                .add(recipe.output()); // MUDANÇA: addItemStack -> add
    }

    @Nonnull
    @Override
    public void getTooltip(ITooltipBuilder tooltip, @Nonnull CreatingFluxRecipe recipe,
                           @Nonnull IRecipeSlotsView recipeSlotsView, double mouseX, double mouseY) {
        if (mouseX >= 40 && mouseX < 80 && mouseY >= 10 && mouseY < 64) {
            tooltip.add(Component.literal("Y+2 = ").append(recipe.crusher().getName()));
            tooltip.add(Component.literal("Y+1 = ").append(recipe.input().getHoverName()));
            tooltip.add(Component.literal("Y+0 = ").append(recipe.base().getName()));
        }
    }

    @Override
    public void draw(@Nonnull CreatingFluxRecipe recipe, @Nonnull IRecipeSlotsView recipeSlotsView,
                     @Nonnull GuiGraphicsExtractor guiGraphics, double mouseX, double mouseY) {
        int value = timer.getValue();
        double offset = (value > 160 ? 160 - (value - 160) : value) / 10F;

        // Desenha o bloco do meio (crusher) - animado subindo e descendo
        if (recipe.crusher() == Blocks.BEDROCK) {
            bedrockIcon.draw(guiGraphics, 52, (int)(10 + offset));
        } else {
            fluxBlockIcon.draw(guiGraphics, 52, (int)(10 + offset));
        }

        // Desenha o bloco base (sempre no mesmo lugar)
        if (recipe.base() == Blocks.OBSIDIAN) {
            obsidianIcon.draw(guiGraphics, 52, 40);
        } else {
            bedrockIcon.draw(guiGraphics, 52, 40);
        }

        // Desenha o item (alternando entre input e output) - USANDO guiGraphics.item()
        ItemStack toDisplay = value > 160 ? recipe.output() : recipe.input();
        guiGraphics.item(toDisplay, 63, 36);  // <-- MUDANÇA AQUI!

        // Desenha o texto de ajuda
        Font fontRenderer = Minecraft.getInstance().font;
        String help = FluxTranslate.JEI_LEFT_CLICK.format(recipe.crusher().getName().getString());
        guiGraphics.text(fontRenderer, help, 64 - fontRenderer.width(help) / 2, 68, 0xff404040, false);
    }
}