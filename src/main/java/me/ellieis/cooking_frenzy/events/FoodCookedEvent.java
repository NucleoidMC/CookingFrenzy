package me.ellieis.cooking_frenzy.events;

import net.minecraft.world.item.ItemStack;
import xyz.nucleoid.stimuli.event.StimulusEvent;

public interface FoodCookedEvent {
    StimulusEvent<FoodCookedEvent> EVENT = StimulusEvent.create(FoodCookedEvent.class, ctx -> (ItemStack item) -> {
        try {
            for (var listener : ctx.getListeners()) {
                listener.onFoodCooked(item);
            }
        } catch (Throwable t) {
            ctx.handleException(t);
        }
    });
    void onFoodCooked(ItemStack item);
}
