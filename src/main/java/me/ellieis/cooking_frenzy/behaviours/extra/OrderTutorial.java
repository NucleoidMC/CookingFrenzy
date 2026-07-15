package me.ellieis.cooking_frenzy.behaviours.extra;

import me.ellieis.cooking_frenzy.behaviours.CustomerBehaviour;
import me.ellieis.cooking_frenzy.behaviours.TutorialBehaviour;
import me.ellieis.cooking_frenzy.events.CustomerOrderTakenEvent;
import me.ellieis.cooking_frenzy.events.CustomerSitEvent;
import me.ellieis.cooking_frenzy.events.OrderTakenResult;
import me.ellieis.cooking_frenzy.gamestate.orders.BaseOrder;
import me.ellieis.cooking_frenzy.map.Active;
import me.ellieis.cooking_frenzy.phases.CookingFrenzyActive;
import me.ellieis.cooking_frenzy.scheduler.Scheduler;
import me.ellieis.cooking_frenzy.scheduler.Task;
import net.minecraft.SharedConstants;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Items;
import xyz.nucleoid.plasmid.api.game.GameActivity;
import xyz.nucleoid.plasmid.api.game.GameSpace;
import xyz.nucleoid.plasmid.api.game.event.GameActivityEvents;

public class OrderTutorial extends BaseTutorial {
    CustomerBehaviour<Active> behaviour;
    boolean customerSat = false;
    boolean orderTaken = false;
    boolean stallingCamera = false;
    public OrderTutorial(TutorialBehaviour tutorial, GameActivity activity, GameSpace gameSpace, Active map, Scheduler scheduler, CookingFrenzyActive game, CustomerBehaviour<Active> behaviour) {
        super(tutorial, activity, gameSpace, map, scheduler, game);
        this.behaviour = behaviour;
        activity.listen(GameActivityEvents.TICK, this::onTick);
    }

    private void onTick() {
        if (stallingCamera) {
            tutorial.timeUntilAngleEnds = 999;
        }
    }

    public void startTutorial() {
        stallingCamera = true;
        tutorial.setCameraAngle(map.tutorialCameraPositions.customer(), 999);
        behaviour.spawnCustomer(false);
        tutorial.sendTutorialMessage(Component.translatable("cooking_frenzy.tutorial.customer_arrived"));
        activity.listen(CustomerSitEvent.EVENT, (_customer) -> {
            if (customerSat) return;
            _customer.timeout = 10 * SharedConstants.TICKS_PER_MINUTE;
            customerSat = true;
            tutorial.sendTutorialMessage(Component.translatable("cooking_frenzy.tutorial.customer_arrived.take_order"));
            stallingCamera = false;
            tutorial.timeUntilAngleEnds = 5 * SharedConstants.TICKS_PER_SECOND;
            activity.listen(CustomerOrderTakenEvent.EVENT, (customer, order) -> {
                if (orderTaken) {
                    return OrderTakenResult.pass(order);
                }
                orderTaken = true;
                tutorial.sendTutorialMessage(Component.translatable("cooking_frenzy.tutorial.customer_arrived.order_taken", Items.BREAD.getDefaultInstance().getItemName()));
                scheduler.addTask(new Task(game.time + (SharedConstants.TICKS_PER_SECOND * 4), () -> {
                    tutorial.sendTutorialMessage(Component.translatable("cooking_frenzy.tutorial.customer_arrived.timeout_bar"));
                    scheduler.addTask(new Task(game.time + (SharedConstants.TICKS_PER_SECOND * 6), () -> {
                        tutorial.sendTutorialMessage(Component.translatable("cooking_frenzy.tutorial.customer_arrived.timeout_bar.extended"));
                        scheduler.addTask(new Task(game.time + (SharedConstants.TICKS_PER_SECOND * 8), () -> {
                            tutorial.endCurrentTutorial();
                        }));
                    }));
                }));
                return OrderTakenResult.allow(BaseOrder.inSeconds(Items.BREAD, 10 * SharedConstants.TICKS_PER_MINUTE, 1));
            });
        });
    }
}
