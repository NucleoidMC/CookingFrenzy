package me.ellieis.cooking_frenzy.behaviours.brewer_recipes;

import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.PotionContents;

public record SplashRecipe() implements PotionRecipe {
    public boolean testRecipe(PotionContents potion, ItemStack recipe) {
        return recipe.getItem().equals(Items.GUNPOWDER);
    }

    public ItemStack getResult(PotionContents potion, ItemStack recipe) {
        if (testRecipe(potion, recipe)) {
            ItemStack item = new ItemStack(Items.SPLASH_POTION);
            item.set(DataComponents.POTION_CONTENTS, potion);
            return item;
        } else {
            return ItemStack.EMPTY;
        }
    }
}
