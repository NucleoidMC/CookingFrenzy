package me.ellieis.cooking_frenzy.behaviours.brewer_recipes;

import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.Potion;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.item.alchemy.Potions;

public record BaseRecipe(PotionContents basePotion, ItemStack ingredient, ItemStack result) implements PotionRecipe {
    public BaseRecipe(Holder<Potion> potion, ItemStack ingredient, ItemStack result) {
        this(new PotionContents(potion), ingredient, result);
    }

    public ItemStack basePotionDisplay() {
        ItemStack item = Items.POTION.getDefaultInstance().copy();
        item.set(DataComponents.POTION_CONTENTS, basePotion);
        return item;
    }

    public ItemStack ingredientDisplay() {
        return ingredient;
    }

    public ItemStack resultDisplay() {
        return result;
    }

    public boolean testRecipe(PotionContents potion, ItemStack recipe) {
        //.is(potion.potion().orElse(Potions.WATER))
        return basePotion.equals(potion) && ingredient.getItem().equals(recipe.getItem());
    }

    public ItemStack getResult(PotionContents potion, ItemStack recipe) {
        if (testRecipe(potion, recipe)) {
            return result;
        } else {
            return ItemStack.EMPTY;
        }
    }
}
