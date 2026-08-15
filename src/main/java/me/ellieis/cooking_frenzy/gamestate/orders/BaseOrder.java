package me.ellieis.cooking_frenzy.gamestate.orders;

import net.minecraft.SharedConstants;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

public record BaseOrder(Item food, int timeLimit, int tier, int money) implements Order {
    public ItemStack displayStack() {
        return food.getDefaultInstance();
    }
    public Component name() {
        return food.getDefaultInstance().getItemName();
    }
    public boolean isCorrectOrder(ItemStack item) {
        return item.getItem().equals(food());
    }
    public static BaseOrder inSeconds(Item food, int timeLimit, int tier) {
        return new BaseOrder(food, timeLimit * SharedConstants.TICKS_PER_SECOND, tier, 60);
    }
    public static BaseOrder inSeconds(Item food, int timeLimit, int tier, int money) {
        return new BaseOrder(food, timeLimit * SharedConstants.TICKS_PER_SECOND, tier, money);
    }
}
