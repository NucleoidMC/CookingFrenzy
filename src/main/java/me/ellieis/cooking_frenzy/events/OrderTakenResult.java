package me.ellieis.cooking_frenzy.events;

import me.ellieis.cooking_frenzy.gamestate.orders.BaseOrder;
import me.ellieis.cooking_frenzy.gamestate.orders.Order;
import net.minecraft.world.item.Items;
import xyz.nucleoid.stimuli.event.EventResult;

public final class OrderTakenResult {
    private static final OrderTakenResult DENY = new OrderTakenResult(EventResult.PASS, new BaseOrder(Items.AIR, 0, 1, 60));

    private final EventResult result;
    private final Order order;

    private OrderTakenResult(EventResult result, Order order) {
        this.result = result;
        this.order = order;
    }

    public EventResult result() {
        return this.result;
    }

    public Order order() {
        return this.order;
    }

    @Override
    public String toString() {
        return "OrderTakenResult{result=" + this.result + ", order=" + this.order + "}";
    }

    public static OrderTakenResult pass(Order order) {
        return new OrderTakenResult(EventResult.PASS, order);
    }

    public static OrderTakenResult allow(Order order) {
        return new OrderTakenResult(EventResult.ALLOW, order);
    }

    public static OrderTakenResult deny() {
        return DENY;
    }
}