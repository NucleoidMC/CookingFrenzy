package me.ellieis.cooking_frenzy.events;

import net.minecraft.world.item.ItemStack;
import xyz.nucleoid.stimuli.event.StimulusEvent;

public interface ItemBuyEvent {
    StimulusEvent<ItemBuyEvent> EVENT = StimulusEvent.create(ItemBuyEvent.class, ctx -> (ItemStack item) -> {
        try {
            for (var listener : ctx.getListeners()) {
                listener.onItemBought(item);
            }
        } catch (Throwable t) {
            ctx.handleException(t);
        }
    });
    void onItemBought(ItemStack item);
}
