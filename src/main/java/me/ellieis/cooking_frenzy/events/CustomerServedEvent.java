package me.ellieis.cooking_frenzy.events;

import me.ellieis.cooking_frenzy.behaviours.extra.Customer;
import me.ellieis.cooking_frenzy.gamestate.orders.BaseOrder;
import xyz.nucleoid.stimuli.event.StimulusEvent;

public interface CustomerServedEvent {
    StimulusEvent<CustomerServedEvent> EVENT = StimulusEvent.create(CustomerServedEvent.class, ctx -> (Customer customer) -> {
        try {
            for (var listener : ctx.getListeners()) {
                listener.onCustomerServed(customer);
            }
        } catch (Throwable t) {
            ctx.handleException(t);
        }
    });
    void onCustomerServed(Customer customer);
}
