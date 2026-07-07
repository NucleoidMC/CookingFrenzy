package me.ellieis.cooking_frenzy.gamestate.upgrades.modifier;

import me.ellieis.cooking_frenzy.gamestate.GameState;
import me.ellieis.cooking_frenzy.gamestate.ModifierType;
import me.ellieis.cooking_frenzy.gamestate.upgrades.BaseUpgrade;
import net.minecraft.network.chat.Component;

public class ModifierUpgrade<T> extends BaseUpgrade {
    T newValue;
    ModifierType<T> modifierType;
    public ModifierUpgrade(Component name, Component desc, ModifierType<T> modifierType, T newValue, int price) {
        super(name, desc, price);
        this.newValue = newValue;
        this.modifierType = modifierType;
    }

    public void onBuy(GameState state) {
        state.currentModifiers().setModifier(this.modifierType, newValue);
    }

    public void setupEvents() {

    }
}
