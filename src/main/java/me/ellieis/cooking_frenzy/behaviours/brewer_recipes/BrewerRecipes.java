package me.ellieis.cooking_frenzy.behaviours.brewer_recipes;



import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.Potion;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.item.alchemy.Potions;

import java.util.ArrayList;
import java.util.List;

public class BrewerRecipes {
    public static ArrayList<PotionRecipe> recipes = new ArrayList<>(List.of(
            new BaseRecipe(Potions.WATER, Items.SUGAR.getDefaultInstance(), Items.HONEY_BOTTLE.getDefaultInstance()),
            new BaseRecipe(Potions.WATER, Items.CARROT.getDefaultInstance(), Potions.FIRE_RESISTANCE),
            new BaseRecipe(Potions.WATER, Items.BEETROOT.getDefaultInstance(), Potions.HEALING),
            new SplashRecipe()
    ));

    public static boolean isValidRecipe(PotionContents potion, ItemStack item) {
        for (PotionRecipe recipe : recipes) {
            if (recipe.testRecipe(potion, item)) {
                return true;
            }
        }
        return false;
    }

    public static ItemStack getResult(PotionContents potion, ItemStack item) {
        if (isValidRecipe(potion, item)) {
            for (PotionRecipe recipe : recipes) {
                if (recipe.testRecipe(potion, item)) {
                    return recipe.getResult(potion, item);
                }
            }
        }
        return ItemStack.EMPTY;
    }
}
