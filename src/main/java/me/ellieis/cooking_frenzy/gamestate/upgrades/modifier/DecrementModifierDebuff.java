package me.ellieis.cooking_frenzy.gamestate.upgrades.modifier;

import me.ellieis.cooking_frenzy.gamestate.ModifierType;
import net.minecraft.network.chat.Component;

public class DecrementModifierDebuff<T extends Number> extends IncrementModifierUpgrade<T>{
    public DecrementModifierDebuff(Component name, Component desc, ModifierType<T> modifierType, T decrementBy) {
        Number val = decrementBy;
        if (modifierType.typeClass().isInstance(1f)) {
            float result = -val.floatValue();
            val = modifierType.typeClass().cast(result);
        } else if (modifierType.typeClass().isInstance(1d)) {
            double result = -val.doubleValue();
            val = modifierType.typeClass().cast(result);
        } else if (modifierType.typeClass().isInstance(1)) {
            int result = -val.intValue();
            val = modifierType.typeClass().cast(result);
        } else if (modifierType.typeClass().isInstance(1L)) {
            long result = -val.longValue();
            val = modifierType.typeClass().cast(result);
        }
        super(name, desc, modifierType, (T) val, 0);
    }
}
