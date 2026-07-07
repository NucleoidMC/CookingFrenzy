package me.ellieis.cooking_frenzy.gamestate.upgrades;

import me.ellieis.cooking_frenzy.gamestate.GameState;
import me.ellieis.cooking_frenzy.phases.PhaseWithState;
import net.minecraft.network.chat.Component;

public abstract class BaseUpgrade {
    public int price;
    public Component name;
    public Component desc;
    public BaseUpgrade(Component name, Component desc, int price) {
        this.name = name;
        this.desc = desc;
        this.price = price;
    }

    public abstract void onBuy(GameState state);
    public void onBuy(PhaseWithState game) {
        this.onBuy(game.getState());
    }
    public abstract void setupEvents();

    public boolean isAvailable(GameState state) {
        return true;
    }
}
