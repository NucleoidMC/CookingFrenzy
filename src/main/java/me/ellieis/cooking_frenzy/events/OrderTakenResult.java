package me.ellieis.cooking_frenzy.events;

import me.ellieis.cooking_frenzy.gamestate.orders.BaseOrder;
import net.minecraft.world.item.Items;
import xyz.nucleoid.stimuli.event.EventResult;

public final class OrderTakenResult {
    private static final OrderTakenResult DENY = new OrderTakenResult(EventResult.PASS, new BaseOrder(Items.AIR, 0, 1));

    private final EventResult result;
    private final BaseOrder order;

    private OrderTakenResult(EventResult result, BaseOrder order) {
        this.result = result;
        this.order = order;
    }

    public EventResult result() {
        return this.result;
    }

    public BaseOrder order() {
        return this.order;
    }

    @Override
    public String toString() {
        return "OrderTakenResult{result=" + this.result + ", order=" + this.order + "}";
    }

    public static OrderTakenResult pass(BaseOrder order) {
        return new OrderTakenResult(EventResult.PASS, order);
    }

    public static OrderTakenResult allow(BaseOrder order) {
        return new OrderTakenResult(EventResult.ALLOW, order);
    }

    public static OrderTakenResult deny() {
        return DENY;
    }
}