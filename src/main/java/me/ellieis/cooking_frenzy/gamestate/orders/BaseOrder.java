package me.ellieis.cooking_frenzy.gamestate.orders;

import net.minecraft.SharedConstants;
import net.minecraft.world.item.Item;

public record BaseOrder(Item food, int timeLimit, int tier, int money) {
    public static BaseOrder inSeconds(Item food, int timeLimit, int tier) {
        return new BaseOrder(food, timeLimit * SharedConstants.TICKS_PER_SECOND, tier, 60);
    }
    public static BaseOrder inSeconds(Item food, int timeLimit, int tier, int money) {
        return new BaseOrder(food, timeLimit * SharedConstants.TICKS_PER_SECOND, tier, money);
    }
}
