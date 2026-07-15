package me.ellieis.cooking_frenzy.events;

import me.ellieis.cooking_frenzy.behaviours.extra.Customer;
import me.ellieis.cooking_frenzy.gamestate.orders.BaseOrder;
import xyz.nucleoid.stimuli.event.EventResult;
import xyz.nucleoid.stimuli.event.StimulusEvent;

public interface CustomerOrderTakenEvent {
    StimulusEvent<CustomerOrderTakenEvent> EVENT = StimulusEvent.create(CustomerOrderTakenEvent.class, ctx -> (Customer customer, BaseOrder order) -> {
        try {
            for (var listener : ctx.getListeners()) {
                var result = listener.onCustomerOrderTaken(customer, order);
                if (result.result() != EventResult.PASS) {
                    return result;
                }
            }
        } catch (Throwable t) {
            ctx.handleException(t);
        }
        return OrderTakenResult.pass(order);
    });
    OrderTakenResult onCustomerOrderTaken(Customer customer, BaseOrder order);
}
