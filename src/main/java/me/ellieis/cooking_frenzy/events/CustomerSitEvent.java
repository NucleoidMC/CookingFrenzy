package me.ellieis.cooking_frenzy.events;

import me.ellieis.cooking_frenzy.behaviours.extra.Customer;
import xyz.nucleoid.stimuli.event.EventResult;
import xyz.nucleoid.stimuli.event.StimulusEvent;

public interface CustomerSitEvent {
    StimulusEvent<CustomerSitEvent> EVENT = StimulusEvent.create(CustomerSitEvent.class, ctx -> (Customer customer) -> {
        try {
            for (var listener : ctx.getListeners()) {
                listener.onCustomerSit(customer);
            }
        } catch (Throwable t) {
            ctx.handleException(t);
        }
    });
    void onCustomerSit(Customer customer);
}
