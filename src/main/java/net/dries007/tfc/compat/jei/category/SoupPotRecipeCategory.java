/*
 * Licensed under the EUPL, Version 1.2.
 * You may obtain a copy of the Licence at:
 * https://joinup.ec.europa.eu/collection/eupl/eupl-text-eupl-12
 */

package net.dries007.tfc.compat.jei.category;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;

import com.mojang.blaze3d.vertex.PoseStack;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.builder.IRecipeSlotBuilder;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.RecipeType;
import net.dries007.tfc.common.items.TFCItems;
import net.dries007.tfc.common.recipes.PotRecipe;
import net.dries007.tfc.compat.jei.JEIIntegration;

public class SoupPotRecipeCategory extends PotRecipeCategory<PotRecipe>
{
    public SoupPotRecipeCategory(RecipeType<PotRecipe> type, IGuiHelper helper)
    {
        super(type, helper, helper.createBlankDrawable(175, 80));
    }

    @Override
    public void setRecipe(IRecipeLayoutBuilder builder, PotRecipe recipe, IFocusGroup focuses)
    {
        int i = 0;
        for (Ingredient ingredient : recipe.getItemIngredients())
        {
            if (!ingredient.isEmpty())
            {
                IRecipeSlotBuilder inputSlot = builder.addSlot(RecipeIngredientRole.INPUT, 6 + 20 * i, 6);
                inputSlot.addIngredients(ingredient);
                inputSlot.setBackground(slot, -1, -1);
                i++;
            }
        }

        IRecipeSlotBuilder inputFluid = builder.addSlot(RecipeIngredientRole.INPUT, 46, 26);
        inputFluid.addIngredients(JEIIntegration.FLUID_STACK, collapse(recipe.getFluidIngredient()));
        inputFluid.setFluidRenderer(1, false, 16, 16);
        inputFluid.setBackground(slot, -1, -1);

        IRecipeSlotBuilder outputItem = builder.addSlot(RecipeIngredientRole.OUTPUT, 126, 6);
        outputItem.addItemStacks(TFCItems.SOUPS.values().stream().map(reg -> new ItemStack(reg.get())).toList());
        outputItem.setBackground(slot, -1, -1);
    }

    @Override
    public void draw(PotRecipe recipe, IRecipeSlotsView recipeSlots, PoseStack stack, double mouseX, double mouseY)
    {
        // fire
        fire.draw(stack, 47, 45);
        fireAnimated.draw(stack, 47, 45);
        // arrow
        arrow.draw(stack, 103, 26);
        arrowAnimated.draw(stack, 103, 26);
    }
}
