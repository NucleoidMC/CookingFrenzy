package me.ellieis.cooking_frenzy.gamestate.upgrades.modifier;

import me.ellieis.cooking_frenzy.gamestate.GameModifiers;
import me.ellieis.cooking_frenzy.gamestate.GameState;
import me.ellieis.cooking_frenzy.gamestate.ModifierType;
import net.minecraft.network.chat.Component;

public class IncrementModifierUpgrade<T extends Number> extends ModifierUpgrade<T> {
    public IncrementModifierUpgrade(Component name, Component desc, ModifierType<T> modifierType, T incrementBy, int price) {
        super(name, desc, modifierType, incrementBy, price);
    }

    @Override
    public void onBuy(GameState state) {
        GameModifiers currentModifiers = state.currentModifiers();
        Number val = this.newValue;
        Number oldVal = currentModifiers.getModifier(this.modifierType);
        if (this.modifierType.typeClass().isInstance(1f)) {
            float result = oldVal.floatValue() + val.floatValue();
            currentModifiers.setModifier(this.modifierType, this.modifierType.typeClass().cast(result));
        } else if (this.modifierType.typeClass().isInstance(1d)) {
            double result = oldVal.doubleValue() + val.doubleValue();
            currentModifiers.setModifier(this.modifierType, this.modifierType.typeClass().cast(result));
        } else if (this.modifierType.typeClass().isInstance(1)) {
            int result = oldVal.intValue() + val.intValue();
            currentModifiers.setModifier(this.modifierType, this.modifierType.typeClass().cast(result));
        } else if (this.modifierType.typeClass().isInstance(1L)) {
            long result = oldVal.longValue() + val.longValue();
            currentModifiers.setModifier(this.modifierType, this.modifierType.typeClass().cast(result));
        } else {
            currentModifiers.setModifier(this.modifierType, newValue);
        }
    }
}
