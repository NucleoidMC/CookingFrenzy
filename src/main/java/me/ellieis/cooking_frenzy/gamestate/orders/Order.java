package me.ellieis.cooking_frenzy.gamestate.orders;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

public interface Order {
    Item food();
    ItemStack displayStack();
    Component name();
    boolean isCorrectOrder(ItemStack item);
    int timeLimit();
    int tier();
    int money();
}
