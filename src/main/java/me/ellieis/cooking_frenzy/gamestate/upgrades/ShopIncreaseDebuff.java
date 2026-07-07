package me.ellieis.cooking_frenzy.gamestate.upgrades;

import me.ellieis.cooking_frenzy.gamestate.GameState;
import net.minecraft.network.chat.Component;

public class ShopIncreaseDebuff extends BaseUpgrade implements Debuff {
    public ShopIncreaseDebuff() {
        super(Component.translatable("cooking_frenzy.debuffs.random_shop_item_expensive"), Component.translatable("cooking_frenzy.debuffs.random_shop_item_expensive.desc"), 0);
    }

    @Override
    public void onBuy(GameState state) {
        state.upgrades().add(this);
    }

    @Override
    public void setupEvents() {

    }
}
