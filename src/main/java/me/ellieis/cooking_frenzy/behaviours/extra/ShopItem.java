package me.ellieis.cooking_frenzy.behaviours.extra;

import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemLore;

import java.util.List;

public record ShopItem(int price, ItemStack item) {
    public ShopItem {
        item.set(DataComponents.LORE, new ItemLore(List.of(Component.literal("$" + price).withStyle(ChatFormatting.GREEN))));
    }
    static ShopItem EMPTY() {
        return new ShopItem(-1, ItemStack.EMPTY);
    }
}
