package me.ellieis.cooking_frenzy.events;

import net.minecraft.world.item.ItemStack;
import xyz.nucleoid.stimuli.event.StimulusEvent;

public interface ItemCraftedEvent {
    StimulusEvent<ItemCraftedEvent> EVENT = StimulusEvent.create(ItemCraftedEvent.class, ctx -> (ItemStack item) -> {
        try {
            for (var listener : ctx.getListeners()) {
                listener.onItemCrafted(item);
            }
        } catch (Throwable t) {
            ctx.handleException(t);
        }
    });
    void onItemCrafted(ItemStack item);
}
