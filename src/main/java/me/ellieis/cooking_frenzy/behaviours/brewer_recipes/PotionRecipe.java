package me.ellieis.cooking_frenzy.behaviours.brewer_recipes;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.alchemy.PotionContents;

public interface PotionRecipe {
    boolean testRecipe(PotionContents potion, ItemStack recipe);
    ItemStack getResult(PotionContents potion, ItemStack recipe);
    ItemStack basePotionDisplay();
    ItemStack ingredientDisplay();
    ItemStack resultDisplay();
}
