package me.ellieis.cooking_frenzy.events;

import me.ellieis.cooking_frenzy.behaviours.extra.Customer;
import xyz.nucleoid.stimuli.event.EventResult;
import xyz.nucleoid.stimuli.event.StimulusEvent;

public interface CustomerSpawnEvent {
    StimulusEvent<CustomerSpawnEvent> EVENT = StimulusEvent.create(CustomerSpawnEvent.class, ctx -> (Customer customer) -> {
        try {
            for (var listener : ctx.getListeners()) {
                var result = listener.onCustomerSpawn(customer);
                if (result != EventResult.PASS) {
                    return result;
                }
            }
        } catch (Throwable t) {
            ctx.handleException(t);
        }
        return EventResult.PASS;
    });
    EventResult onCustomerSpawn(Customer customer);
}
