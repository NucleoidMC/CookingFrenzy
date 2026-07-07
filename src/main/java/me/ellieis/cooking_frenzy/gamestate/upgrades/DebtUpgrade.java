package me.ellieis.cooking_frenzy.gamestate.upgrades;

import me.ellieis.cooking_frenzy.gamestate.GameState;
import net.minecraft.network.chat.Component;

public class DebtUpgrade extends BaseUpgrade {

    public DebtUpgrade() {
        super(Component.translatable("cooking_frenzy.upgrades.debt"), Component.translatable("cooking_frenzy.upgrades.debt.desc"), 75);
    }

    @Override
    public void onBuy(GameState state) {
        state.upgrades().add(this);
    }

    @Override
    public void setupEvents() {

    }

    @Override
    public boolean isAvailable(GameState state) {
        return state.upgrades().contains(this);
    }
}
