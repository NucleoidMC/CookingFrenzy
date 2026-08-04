package me.ellieis.cooking_frenzy.behaviours.brewer_recipes;

import net.minecraft.core.Holder;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.alchemy.Potion;
import net.minecraft.world.item.alchemy.PotionContents;

public record BaseRecipe(PotionContents basePotion, ItemStack ingredient, ItemStack result) implements PotionRecipe {
    public BaseRecipe(Potion potion, ItemStack ingredient, ItemStack result) {
        this(new PotionContents(Holder.direct(potion)), ingredient, result);
    }

    public boolean testRecipe(PotionContents potion, ItemStack recipe) {
        return potion.equals(basePotion) && ingredient.equals(recipe);
    }

    public ItemStack getResult(PotionContents potion, ItemStack recipe) {
        if (testRecipe(potion, recipe)) {
            return result;
        } else {
            return ItemStack.EMPTY;
        }
    }
}
