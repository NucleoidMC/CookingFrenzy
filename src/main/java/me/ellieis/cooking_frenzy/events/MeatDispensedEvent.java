package me.ellieis.cooking_frenzy.events;

import net.minecraft.world.item.Item;
import xyz.nucleoid.stimuli.event.StimulusEvent;

public interface MeatDispensedEvent {
    StimulusEvent<MeatDispensedEvent> EVENT = StimulusEvent.create(MeatDispensedEvent.class, ctx -> (Item meat) -> {
        try {
            for (var listener : ctx.getListeners()) {
                listener.onMeatDispensed(meat);
            }
        } catch (Throwable t) {
            ctx.handleException(t);
        }
    });
    void onMeatDispensed(Item meat);
}
